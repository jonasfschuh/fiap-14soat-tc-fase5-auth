package br.com.fiap.infrastructure.adapters.jwt;

import br.com.fiap.domain.model.UserAccount;
import br.com.fiap.domain.ports.out.TokenServicePort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Implementa a emissão e a validação de tokens JWT utilizando JJWT.
 */
public class JwtTokenAdapter implements TokenServicePort {

    private final long expirationMs;
    private final String issuer;
    private final SecretKey secretKey;

    public JwtTokenAdapter(String secret, long expirationMs, String issuer) {
        this.expirationMs = expirationMs;
        this.issuer = Objects.requireNonNull(issuer, "issuer");
        this.secretKey = deriveKey(secret);
    }

    @Override
    public String generate(UserAccount user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.username())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claim("role", user.role().name())
                .claim("active", user.active())
                .signWith(secretKey)
                .compact();
    }

    @Override
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey deriveKey(String secret) {
        Objects.requireNonNull(secret, "secret");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Keys.hmacShaKeyFor(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível derivar a chave JWT.", exception);
        }
    }
}
