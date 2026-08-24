package br.com.fiap.domain.ports.out;

/**
 * Define a porta de saída para comparação segura de senhas.
 */
public interface PasswordVerifierPort {

    /**
     * Compara uma senha em texto puro com sua representação codificada.
     *
     * @param rawPassword senha informada pelo usuário
     * @param encodedPassword hash persistido
     * @return verdadeiro quando a senha é compatível
     */
    boolean matches(String rawPassword, String encodedPassword);
}
