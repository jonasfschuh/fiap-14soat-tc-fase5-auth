package br.com.fiap.authlambda.config;

public record AppConfig(DbSettings dbSettings, JwtSettings jwtSettings) {

    public static AppConfig fromEnv() {
        String jwtSecret = required("JWT_SECRET");
        String jwtIssuer = required("JWT_ISSUER");
        long jwtExpirationMs = requiredLong("JWT_EXPIRATION_MS");
        String dbUrl = required("DB_URL");
        String dbUser = required("DB_USER");
        String dbPassword = required("DB_PASSWORD");
        int dbPoolSize = integerOrDefault("DB_POOL_SIZE", 2);

        return new AppConfig(
                new DbSettings(dbUrl, dbUser, dbPassword, dbPoolSize),
                new JwtSettings(jwtSecret, jwtIssuer, jwtExpirationMs)
        );
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variavel de ambiente obrigatoria ausente: " + name);
        }
        return value.trim();
    }

    private static long requiredLong(String name) {
        try {
            return Long.parseLong(required(name));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Variavel de ambiente invalida: " + name, ex);
        }
    }

    private static int integerOrDefault(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Variavel de ambiente invalida: " + name, ex);
        }
    }
}

