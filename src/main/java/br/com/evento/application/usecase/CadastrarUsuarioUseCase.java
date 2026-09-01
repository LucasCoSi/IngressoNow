package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CadastrarUsuarioUseCase(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario executar(String nome, String emailStr, String senhaTexto, String tipo) {
        Email email = new Email(emailStr);

        if (usuarioRepository.existePorEmail(email)) {
            throw new DomainException("Já existe um usuário cadastrado com este e-mail.");
        }

        String senhaHashStr = passwordEncoder.encode(senhaTexto);
        SenhaHash senhaHash = new SenhaHash(senhaHashStr);

        Usuario usuario;
        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {
            usuario = new Organizador(null, nome, email, senhaHash);
        } else if ("PARTICIPANTE".equalsIgnoreCase(tipo)) {
            usuario = new Participante(null, nome, email, senhaHash);
        } else {
            throw new DomainException("Tipo de usuário inválido. Use 'ORGANIZADOR' ou 'PARTICIPANTE'.");
        }

        return usuarioRepository.salvar(usuario);
    }
}
