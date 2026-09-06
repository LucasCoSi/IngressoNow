package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.PasswordHasher;
import br.com.evento.domain.port.out.UsuarioRepository;

/**
 * RF-03: participante consulta/atualiza dados basicos (nome, senha).
 * E-mail e tipo sao imutaveis (e-mail e identidade/UNIQUE).
 */
public class EditarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;

    public EditarUsuarioUseCase(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
    }

    public Usuario executar(String usuarioId, String novoNome, String novaSenhaTexto) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new DomainException("Usuario nao encontrado."));
        if (novoNome != null && !novoNome.isBlank()) {
            usuario.alterarNome(novoNome.trim());
        }
        if (novaSenhaTexto != null && !novaSenhaTexto.isBlank()) {
            if (novaSenhaTexto.length() < 6) {
                throw new DomainException("A nova senha deve ter ao menos 6 caracteres.");
            }
            usuario.alterarSenha(new SenhaHash(passwordHasher.hash(novaSenhaTexto)));
        }
        return usuarioRepository.salvar(usuario);
    }
}
