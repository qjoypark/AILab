package com.lab.approval.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Resolve current user context from request headers/JWT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestUserContextResolver {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public CurrentUser resolve(HttpServletRequest request) {
        Long userId = parseLongHeader(request, "X-UserId");
        if (userId == null) {
            userId = parseLongHeader(request, "x-user-id");
        }

        String username = firstNonBlank(
                request.getHeader("X-Username"),
                request.getHeader("x-username"),
                request.getHeader("X-UserName"),
                request.getHeader("x-user-name")
        );
        String department = firstNonBlank(
                request.getHeader("X-Department"),
                request.getHeader("x-department"),
                request.getHeader("X-Dept"),
                request.getHeader("x-dept")
        );

        String token = extractBearerToken(request);
        if (token != null && (userId == null || isBlank(username) || isBlank(department))) {
            Map<String, Object> claims = decodeJwtPayload(token);
            if (claims != null) {
                if (userId == null) {
                    userId = parseLong(claims.get("sub"));
                }
                if (isBlank(username)) {
                    username = firstNonBlank(
                            toStringValue(claims.get("username")),
                            toStringValue(claims.get("user_name")),
                            toStringValue(claims.get("realName"))
                    );
                }
                if (isBlank(department)) {
                    department = firstNonBlank(
                            toStringValue(claims.get("department")),
                            toStringValue(claims.get("dept"))
                    );
                }
            }
        }

        if (userId == null) {
            throw new BusinessException("登录状态已失效，请重新登录");
        }

        if (isBlank(username)) {
            username = "用户" + userId;
        }
        if (isBlank(department)) {
            department = "未设置部门";
        }
        return new CurrentUser(userId, username, department);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorization.startsWith(prefix) || authorization.length() <= prefix.length()) {
            return null;
        }
        return authorization.substring(prefix.length()).trim();
    }

    private Map<String, Object> decodeJwtPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("Failed to decode JWT payload", ex);
            return null;
        }
    }

    private Long parseLongHeader(HttpServletRequest request, String headerName) {
        return parseLong(request.getHeader(headerName));
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public record CurrentUser(Long userId, String username, String department) {
    }
}
