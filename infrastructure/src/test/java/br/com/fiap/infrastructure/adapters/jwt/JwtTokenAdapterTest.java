package br.com.fiap.infrastructure.adapters.jwt;

import br.com.fiap.domain.model.Role;
import br.com.fiap.domain.model.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante o comportamento principal do adapter de JWT.
 */
class JwtTokenAdapterTest {

    @Test
    @DisplayName("Deve gerar e validar token JWT")
    void shouldGenerateAndValidateToken() {
        JwtTokenAdapter adapter = new JwtTokenAdapter("dev-secret-change-in-production-min-32-chars", 86400000L, "auth-service");
        UserAccount user = new UserAccount(1L, "admin", "hash", Role.ADMIN, true);

        String token = adapter.generate(user);

        assertThat(adapter.validate(token)).isTrue();
        assertThat(adapter.extractUsername(token)).isEqualTo("admin");
        assertThat(adapter.extractRole(token)).isEqualTo("ADMIN");
    }
}
