package br.com.fiap.domain.ports.in;

import br.com.fiap.domain.model.LoginRequest;
import br.com.fiap.domain.model.LoginResponse;

/**
 * Define a porta de entrada responsável pela autenticação de usuários.
 */
public interface AuthenticationInputPort {

    /**
     * Realiza o login de um usuário com base nas credenciais informadas.
     *
     * @param request requisição contendo usuário e senha
     * @return resposta com o token JWT emitido
     */
    LoginResponse login(LoginRequest request);
}
