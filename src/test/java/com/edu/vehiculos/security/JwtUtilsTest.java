package com.edu.vehiculos.security;

import com.edu.vehiculos.domain.Role;
import com.edu.vehiculos.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expiration = 3600000L;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expiration);
    }

    @Test
    void testGenerateAndValidateToken() {
        User user = User.builder()
                .id(10L)
                .fullName("Carlos Conductor")
                .email("conductor@test.com")
                .password("pass123")
                .role(Role.ROLE_DRIVER)
                .build();

        String token = jwtUtils.generateToken(user);
        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));

        assertEquals("conductor@test.com", jwtUtils.extractUsername(token));
        assertEquals("ROLE_DRIVER", jwtUtils.extractRole(token));
        assertEquals("Carlos Conductor", jwtUtils.extractFullName(token));
        assertEquals(10L, jwtUtils.extractUserId(token));
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        assertFalse(jwtUtils.validateToken("invalid.jwt.token"));
    }
}
