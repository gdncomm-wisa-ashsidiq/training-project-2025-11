package com.gdn.training.api_gateway.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gdn.training.api_gateway.config.RateLimiterProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String ANON_COOKIE_NAME = "ANON_CLIENT_ID";

    private final RateLimiterProperties properties;
    private final RateLimiterService rateLimiterService;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!properties.isEnabled()
                || properties.getRequestsPerMinute() <= 0
                || isIgnoredPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request, response);

        RateLimitResult result = rateLimiterService.consume(key);
        addHeaders(response, result);

        if (!result.allowed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(Math.max(0, result.nanosToReset()));
            if (retryAfterSeconds > 0) {
                response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, retryAfterSeconds)));
            }
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Rate limit exceeded\"}");
            response.getWriter().flush();
            log.warn("Rate limit exceeded for key={} path={}", key, request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void addHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(HEADER_LIMIT, String.valueOf(rateLimiterService.getConfiguredLimit()));
        long remaining = Math.max(0, result.remainingTokens());
        response.setHeader(HEADER_REMAINING, String.valueOf(remaining));
    }

    private boolean isIgnoredPath(String path) {
        if (properties.getIgnoredPaths() == null || properties.getIgnoredPaths().isEmpty()) {
            return false;
        }
        return properties.getIgnoredPaths().stream()
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveKey(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String stringPrincipal && StringUtils.hasText(stringPrincipal)) {
                String key = "user:" + stringPrincipal;
                log.debug("RateLimiter using authenticated key={}", key);
                return key;
            }
        }

        String clientId = getCookieValue(request, ANON_COOKIE_NAME);

        if (!StringUtils.hasText(clientId)) {
            clientId = java.util.UUID.randomUUID().toString();

            Cookie cookie = new Cookie(ANON_COOKIE_NAME, clientId);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setMaxAge(60 * 60 * 24 * 7);

            response.addCookie(cookie);
            log.debug("RateLimiter generated new anonymous clientId={} and set cookie", clientId);
        } else {
            log.debug("RateLimiter found existing anonymous clientId={} from cookie", clientId);
        }

        String key = "anon:" + clientId;
        log.debug("RateLimiter using anonymous key={}", key);
        return key;
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}

