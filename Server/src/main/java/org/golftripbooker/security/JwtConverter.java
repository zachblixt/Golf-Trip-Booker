package org.golftripbooker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Mints and reads the bearer tokens the app authenticates with.
 *
 * The API is stateless because the client is an Expo app: React Native has no
 * dependable cookie jar, so a JSESSIONID would not survive the app being reopened.
 * A token in expo-secure-store does, which is what makes "I stay logged in when I
 * reopen the app" work.
 */
@Component
public class JwtConverter {

    private final SecretKey key;
    private final String issuer;
    private final int expirationMinutes;

    public JwtConverter(@Value("${jwt.secret}") String secret,
                        @Value("${jwt.issuer}") String issuer,
                        @Value("${jwt.expiration-minutes}") int expirationMinutes) {

        // HS256 refuses anything shorter than 32 bytes, which is a feature: a short
        // secret would fail here at startup rather than quietly weaken every token.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * The role is a claim so the client can hide host-only screens without another
     * round trip. The server never trusts it -- it re-reads the role from the
     * database on every request.
     */
    public String makeToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                // Named explicitly rather than inferred, so the algorithm is visible
                // to anyone reading this and cannot drift with a library upgrade.
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Returns null for anything it does not fully trust -- a missing header, the
     * wrong scheme, a bad signature, a foreign issuer, an expired token. The filter
     * treats null as "not logged in" rather than as an error, so a stale token on a
     * public endpoint still works.
     */
    public UsernamePasswordAuthenticationToken getAuthentication(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (username == null || role == null) {
                return null;
            }

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(Role.valueOf(role).authority()));

            // No credentials: the token has already been verified, and there is no
            // password to carry around.
            return new UsernamePasswordAuthenticationToken(username, null, authorities);

        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
