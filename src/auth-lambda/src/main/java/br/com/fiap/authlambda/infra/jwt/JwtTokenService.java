package br.com.fiap.authlambda.infra.jwt;

import br.com.fiap.authlambda.config.JwtSettings;
import br.com.fiap.authlambda.domain.model.AuthenticatedPrincipal;
import br.com.fiap.authlambda.domain.model.UserAccount;
import br.com.fiap.authlambda.domain.port.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class JwtTokenService implements TokenService {

    private final JwtSettings settings;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtTokenService(JwtSettings settings, Clock clock) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secretKey = deriveKey(settings.secret());
    }

    @Override
    public String generateToken(UserAccount user) {
        Instant now = clock.instant();
        Date issuedAt = Date.from(now);
        Date expiration = Date.from(now.plusMillis(settings.expirationMs()));

        return Jwts.builder()
                .subject(user.username())
                .issuer(settings.issuer())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .claim("role", user.role().name())
                .claim("email", user.email())
                .signWith(secretKey)
                .compact();
    }

    @Override
    public AuthenticatedPrincipal validateToken(String token) {
        Jws<Claims> claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(settings.issuer())
                .build()
                .parseSignedClaims(token);

        Claims body = claims.getPayload();
        return new AuthenticatedPrincipal(
                body.getSubject(),
                body.get("role", String.class),
                body.get("email", String.class)
        );
    }

    private SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Nao foi possivel derivar a chave JWT", ex);
        }
    }
}

