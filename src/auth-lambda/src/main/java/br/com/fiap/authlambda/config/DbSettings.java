package br.com.fiap.authlambda.config;

public record DbSettings(String url, String user, String password, int poolSize) {
}

