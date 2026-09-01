package br.com.evento.adapters.in.web;

import br.com.evento.adapters.in.web.dto.CadastroRequest;
import br.com.evento.adapters.in.web.dto.LoginRequest;
import br.com.evento.adapters.in.web.dto.LoginResponse;
import br.com.evento.adapters.in.web.security.JwtTokenProvider;
import br.com.evento.application.usecase.AutenticarUsuarioUseCase;
import br.com.evento.application.usecase.CadastrarUsuarioUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthRestController {

    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final CadastrarUsuarioUseCase cadastrarUsuarioUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthRestController(
            AutenticarUsuarioUseCase autenticarUsuarioUseCase,
            CadastrarUsuarioUseCase cadastrarUsuarioUseCase,
            JwtTokenProvider jwtTokenProvider) {
        this.autenticarUsuarioUseCase = autenticarUsuarioUseCase;
        this.cadastrarUsuarioUseCase = cadastrarUsuarioUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Usuario usuario = autenticarUsuarioUseCase.executar(request.email(), request.senha());

            String token = jwtTokenProvider.createToken(
                usuario.getEmail().getEndereco(),
                usuario.getId()
            );

            LoginResponse response = new LoginResponse(
                token,
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail().getEndereco()
            );

            return ResponseEntity.ok(response);
        } catch (DomainException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastro(@Valid @RequestBody CadastroRequest request) {
        try {
            Usuario usuario = cadastrarUsuarioUseCase.executar(
                request.nome(),
                request.email(),
                request.senha(),
                request.tipo()
            );

            String token = jwtTokenProvider.createToken(
                usuario.getEmail().getEndereco(),
                usuario.getId()
            );

            LoginResponse response = new LoginResponse(
                token,
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail().getEndereco()
            );

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
        } catch (DomainException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", e.getMessage()));
        }
    }
}
