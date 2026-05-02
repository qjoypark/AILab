package com.lab.approval.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
        String displayName = decodeHeaderValue(firstNonBlank(
                request.getHeader("X-RealName"),
                request.getHeader("x-realname"),
                request.getHeader("X-Real-Name"),
                request.getHeader("x-real-name"),
                request.getHeader("X-Name"),
                request.getHeader("x-name")
        ));
        String department = decodeHeaderValue(firstNonBlank(
                request.getHeader("X-Department"),
                request.getHeader("x-department"),
                request.getHeader("X-Dept"),
                request.getHeader("x-dept")
        ));
        List<String> roles = parseStringList(firstNonBlank(
                request.getHeader("X-Roles"),
                request.getHeader("x-roles")
        ));
        List<String> permissions = parseStringList(firstNonBlank(
                request.getHeader("X-Permissions"),
                request.getHeader("x-permissions")
        ));

        String token = extractBearerToken(request);
        if (token != null && (userId == null || isBlank(username) || isBlank(displayName)
                || isBlank(department) || roles.isEmpty() || permissions.isEmpty())) {
            Map<String, Object> claims = decodeJwtPayload(token);
            if (claims != null) {
                if (userId == null) {
                    userId = parseLong(claims.get("sub"));
                }
                if (isBlank(username)) {
                    username = firstNonBlank(
                            toStringValue(claims.get("username")),
                            toStringValue(claims.get("user_name")),
                            toStringValue(claims.get("preferred_username"))
                    );
                }
                if (isBlank(displayName)) {
                    displayName = firstNonBlank(
                            toStringValue(claims.get("realName")),
                            toStringValue(claims.get("real_name")),
                            toStringValue(claims.get("name")),
                            toStringValue(claims.get("nickname"))
                    );
                }
                if (isBlank(department)) {
                    department = firstNonBlank(
                            toStringValue(claims.get("department")),
                            toStringValue(claims.get("dept"))
                    );
                }
                if (roles.isEmpty()) {
                    roles = parseStringList(claims.get("roles"));
                }
                if (permissions.isEmpty()) {
                    permissions = parseStringList(claims.get("permissions"));
                }
            }
        }

        if (userId == null) {
            throw new BusinessException("Login status is invalid, please sign in again.");
        }

        if (isBlank(username)) {
            username = "user" + userId;
        }
        if (isBlank(displayName)) {
            displayName = username;
        }
        if (isBlank(department)) {
            department = "UNKNOWN_DEPARTMENT";
        }

        return new CurrentUser(userId, username, displayName, department, roles, permissions);
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

    private List<String> parseStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        if (value instanceof List<?> rawList) {
            for (Object item : rawList) {
                String text = toStringValue(item);
                if (!isBlank(text)) {
                    result.add(text.trim());
                }
            }
        } else if (value instanceof String text) {
            String[] segments = text.split(",");
            for (String segment : segments) {
                if (!isBlank(segment)) {
                    result.add(segment.trim());
                }
            }
        } else {
            String text = toStringValue(value);
            if (!isBlank(text)) {
                result.add(text.trim());
            }
        }
        return result;
    }

    private String decodeHeaderValue(String value) {
        if (isBlank(value)) {
            return value;
        }
        if (!value.contains("%") && !value.contains("+")) {
            return value;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
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

    public record CurrentUser(Long userId,
                              String username,
                              String displayName,
                              String department,
                              List<String> roles,
                              List<String> permissions) {
        public CurrentUser {
            roles = normalizeStringList(roles, true);
            permissions = normalizeStringList(permissions, false);
        }

        private static List<String> normalizeStringList(List<String> values, boolean upperCase) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            List<String> normalized = new ArrayList<>();
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                String trimmed = value.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                normalized.add(upperCase ? trimmed.toUpperCase(Locale.ROOT) : trimmed);
            }
            return List.copyOf(normalized);
        }

        public boolean hasRole(String roleCode) {
            if (roleCode == null || roleCode.trim().isEmpty()) {
                return false;
            }
            return roles.contains(roleCode.trim().toUpperCase(Locale.ROOT));
        }

        public boolean hasAnyRole(String... roleCodes) {
            if (roleCodes == null || roleCodes.length == 0) {
                return false;
            }
            for (String roleCode : roleCodes) {
                if (hasRole(roleCode)) {
                    return true;
                }
            }
            return false;
        }
    }
}
