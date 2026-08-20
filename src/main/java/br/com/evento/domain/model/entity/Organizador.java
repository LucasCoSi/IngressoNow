package br.com.evento.domain.model.entity;

import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;

public class Organizador extends Usuario {

    public Organizador(String id, String nome, Email email, SenhaHash senha) {
        super(id, nome, email, senha);
    }

    public Organizador(String nome, Email email, SenhaHash senha) {
        super(null, nome, email, senha);
    }
}
