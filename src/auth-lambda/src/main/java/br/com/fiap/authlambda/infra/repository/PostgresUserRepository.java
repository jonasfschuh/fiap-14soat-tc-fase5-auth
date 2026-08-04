package br.com.fiap.authlambda.infra.repository;

import br.com.fiap.authlambda.domain.model.Role;
import br.com.fiap.authlambda.domain.model.UserAccount;
import br.com.fiap.authlambda.domain.port.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Objects;

public class PostgresUserRepository implements UserRepository {

    private static final String FIND_BY_USERNAME = """
            SELECT id, username, password, email, role, enabled, created_at, updated_at, last_login
            FROM users
            WHERE username = ?
            LIMIT 1
            """;

    private static final String UPDATE_LAST_LOGIN = """
            UPDATE users
            SET last_login = NOW(), updated_at = NOW()
            WHERE id = ?
            """;

    private static final String FIND_CUSTOMER_CPF_IN_SERVICE_ORDER = """
            SELECT customer_cpf
            FROM service_order
            WHERE customer_cpf = ?
            LIMIT 1
            """;

    private final DataSource dataSource;

    public PostgresUserRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_USERNAME)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao consultar usuario", ex);
        }
    }

    @Override
    public Optional<UserAccount> findByCustomerCpfInServiceOrder(String cpf) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_CUSTOMER_CPF_IN_SERVICE_ORDER)) {
            statement.setString(1, cpf);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String resolvedCpf = rs.getString("customer_cpf");
                String fallbackPassword = resolvedCpf != null && resolvedCpf.length() >= 3 ? resolvedCpf.substring(0, 3) : "";

                // Fallback account synthesized from service_order for CPF-only authentication flow.
                return Optional.of(new UserAccount(
                        -1L,
                        resolvedCpf,
                        fallbackPassword,
                        resolvedCpf + "@local",
                        Role.USER,
                        true,
                        null,
                        null,
                        null
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao consultar CPF em service_order", ex);
        }
    }

    @Override
    public void updateLastLogin(long userId, Instant lastLogin) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_LAST_LOGIN)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao atualizar last_login", ex);
        }
    }

    private UserAccount mapRow(ResultSet rs) throws SQLException {
        return new UserAccount(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email"),
                Role.fromValue(rs.getString("role")),
                rs.getBoolean("enabled"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                toInstant(rs.getTimestamp("last_login"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}

