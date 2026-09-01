package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticarUsuarioUseCase(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Usuario executar(String emailStr, String senhaTexto) {
        Email email = new Email(emailStr);

        Usuario usuario = usuarioRepository.buscarPorEmail(email)
            .orElseThrow(() -> new DomainException("Credenciais inválidas."));

        if (!passwordEncoder.matches(senhaTexto, usuario.getSenha().getHash())) {
            throw new DomainException("Credenciais inválidas.");
        }

        return usuario;
    }
}
