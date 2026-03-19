package com.lab.inventory.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StockOutDeleteAdminInterceptor implements HandlerInterceptor {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (isAdminRequest(request)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"Only admin can delete stock-out orders\",\"data\":null}");
        return false;
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        if (containsAdminRole(request.getHeader("X-Roles"))
                || containsAdminRole(request.getHeader("x-roles"))
                || containsAdminRole(request.getHeader("X-Role"))
                || containsAdminRole(request.getHeader("x-role"))) {
            return true;
        }

        String token = extractBearerToken(request);
        if (token == null) {
            return false;
        }

        Map<String, Object> claims = decodeJwtPayload(token);
        if (claims == null) {
            return false;
        }

        return containsAdminRole(claims.get("roles"))
                || containsAdminRole(claims.get("role"))
                || containsAdminRole(claims.get("authorities"));
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
            return null;
        }
    }

    private boolean containsAdminRole(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (containsAdminRole(item)) {
                    return true;
                }
            }
            return false;
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return false;
        }

        String normalized = text
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "");
        String[] parts = normalized.split("[,\\s]+");
        for (String part : parts) {
            if ("ADMIN".equalsIgnoreCase(part) || "ROLE_ADMIN".equalsIgnoreCase(part)) {
                return true;
            }
        }
        return false;
    }
}
