package br.com.fiap.application.adapters;

import br.com.fiap.application.dtos.LoginRequest;
import br.com.fiap.application.dtos.LoginResponse;
import br.com.fiap.application.dtos.TokenValidationResponse;
import br.com.fiap.domain.ports.in.AuthenticationInputPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe os endpoints REST do fluxo de autenticação.
 */
@RestController
public class AuthController {

    private final AuthenticationInputPort authenticationInputPort;

    public AuthController(AuthenticationInputPort authenticationInputPort) {
        this.authenticationInputPort = authenticationInputPort;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        br.com.fiap.domain.model.LoginResponse response = authenticationInputPort.login(
                new br.com.fiap.domain.model.LoginRequest(request.username(), request.password())
        );

        return ResponseEntity.ok(new LoginResponse(response.token(), response.username(), response.role()));
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(authority -> authority.replace("ROLE_", ""))
                .orElse("USER");

        return ResponseEntity.ok(new TokenValidationResponse(true, authentication.getName(), role));
    }
}
