package com.dga.access.security;

import com.dga.access.entity.User;
import com.dga.access.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dga.jwt.secret:dga-development-secret-change-me}")
    private String secret;

    @Value("${dga.jwt.expiration-seconds:28800}")
    private long expirationSeconds;

    @Autowired
    private UserRepository userRepository;

    public String createToken(User user) {
        return createToken(user.getUsername(), Integer.valueOf(1).equals(user.getIsAdmin()));
    }

    public String createToken(String username, boolean admin) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = new HashMap<>();
        header.put("typ", "JWT");
        header.put("alg", "HS256");

        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", username);
        payload.put("username", username);
        payload.put("isAdmin", admin ? 1 : 0);
        payload.put("rootAdmin", "admin".equals(username));
        payload.put("iat", now);
        payload.put("exp", now + expirationSeconds);

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public AuthenticatedUser parse(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                return null;
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<Map<String, Object>>() {});
            if (isExpired(payload.get("exp"))) {
                return null;
            }

            String username = stringValue(firstNonNull(payload.get("username"), payload.get("sub")));
            if (username == null || username.trim().isEmpty()) {
                return null;
            }
            username = username.trim();

            boolean rootAdmin = "admin".equals(username);
            boolean admin = rootAdmin || numericBoolean(payload.get("isAdmin"));
            User user = userRepository.findByUsername(username);
            if (user != null) {
                if (Integer.valueOf(0).equals(user.getStatus())) {
                    return null;
                }
                admin = rootAdmin || Integer.valueOf(1).equals(user.getIsAdmin());
            } else if (!rootAdmin) {
                return null;
            }

            return new AuthenticatedUser(username, admin, rootAdmin);
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 序列化失败", e);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private boolean isExpired(Object exp) {
        Long expiration = longValue(exp);
        return expiration == null || expiration <= Instant.now().getEpochSecond();
    }

    private boolean numericBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private Long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
