package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.port.out.PasswordHasher;
import br.com.evento.domain.port.out.UsuarioRepository;

public class AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;

    public AutenticarUsuarioUseCase(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
    }
    public Usuario executar(String emailStr, String senhaTexto) {
        Email email = new Email(emailStr);

        Usuario usuario = usuarioRepository.buscarPorEmail(email)
            .orElseThrow(() -> new DomainException("Credenciais inválidas."));

        if (!passwordHasher.matches(senhaTexto, usuario.getSenha().getHash())) {
            throw new DomainException("Credenciais inválidas.");
        }

        return usuario;
    }
}
