package br.com.evento.domain.model.entity;

import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;

/**
 * Participante — usuário que se inscreve em eventos e seleciona atividades.
 */
public class Participante extends Usuario {

    public Participante(String id, String nome, Email email, SenhaHash senha) {
        super(id, nome, email, senha);
    }

    public Participante(String nome, Email email, SenhaHash senha) {
        super(null, nome, email, senha);
    }
}
