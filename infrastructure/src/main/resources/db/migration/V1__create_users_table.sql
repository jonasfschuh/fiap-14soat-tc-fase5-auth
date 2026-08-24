CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO users (username, password_hash, role)
VALUES
    ('admin', '$2b$12$iLk5Y4NuCe95qn.veHzN5OUy9r.WWxewTIRITXqjIZA8g1q8DMci.', 'ADMIN'),
    ('user', '$2b$12$psMRyaK9wKbBaYf8QYWVl.b6GrFYCb4aArhlLTqpgHBS.JndV4fge', 'USER')
ON CONFLICT (username) DO NOTHING;

-- Credenciais padrão:
-- admin / admin123
-- user / user123
