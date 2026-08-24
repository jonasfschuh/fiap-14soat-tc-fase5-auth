package br.com.fiap.domain.ports.out;

import br.com.fiap.domain.model.UserAccount;

/**
 * Define a porta de saída para emissão e validação de tokens JWT.
 */
public interface TokenServicePort {

    /**
     * Gera um token para o usuário autenticado.
     *
     * @param user usuário autenticado
     * @return token JWT assinado
     */
    String generate(UserAccount user);

    /**
     * Valida se o token informado é autêntico e está ativo.
     *
     * @param token token JWT a ser validado
     * @return verdadeiro quando o token é válido
     */
    boolean validate(String token);
}
