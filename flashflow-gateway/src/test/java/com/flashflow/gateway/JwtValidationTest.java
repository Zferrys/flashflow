package com.flashflow.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 验证逻辑测试（模拟 Gateway 鉴权）
 */
@DisplayName("Gateway JWT 验证测试")
class JwtValidationTest {

    private static final String SECRET = "test-jwt-secret-for-unit-tests-only-256-bits-long!!";
    private static SecretKey key;

    @BeforeAll
    static void setUp() {
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("有效 Token 应该通过验证")
    void validTokenShouldPass() {
        String token = Jwts.builder()
                .subject("1")
                .claim("username", "admin")
                .claim("role", "ROLE_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertDoesNotThrow(() -> {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            assertEquals("1", claims.getSubject());
            assertEquals("admin", claims.get("username"));
        });
    }

    @Test
    @DisplayName("过期 Token 应该验证失败")
    void expiredTokenShouldFail() {
        String token = Jwts.builder()
                .subject("1")
                .issuedAt(new Date(System.currentTimeMillis() - 3600000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        assertThrows(Exception.class, () -> {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        });
    }

    @Test
    @DisplayName("签名错误的 Token 应该验证失败")
    void wrongSignatureShouldFail() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("different-secret-key-that-is-also-256-bits-long-for-testing".getBytes());
        String token = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        assertThrows(Exception.class, () -> {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        });
    }

    @Test
    @DisplayName("Token 携带用户信息应该正确")
    void tokenShouldCarryUserInfo() {
        String token = Jwts.builder()
                .subject("42")
                .claim("username", "testuser")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertAll(
                () -> assertEquals("42", claims.getSubject()),
                () -> assertEquals("testuser", claims.get("username")),
                () -> assertEquals("ROLE_USER", claims.get("role"))
        );
    }
}
