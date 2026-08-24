package br.com.fiap.domain.exceptions;

/**
 * Representa falhas de autenticação ou autorização durante o login.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
