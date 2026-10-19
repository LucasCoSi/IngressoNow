package br.com.evento.domain.model.entity;

import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;

/**
 * Ator Administrador (§3): acesso total — gerencia usuários, consulta e
 * opera qualquer evento. Acumula o que o Organizador faz, sem a restrição
 * de dono (RN-18: "administrador ou organizador autorizado").
 */
public class Administrador extends Usuario {

    public Administrador(String id, String nome, Email email, SenhaHash senha) {
        super(id, nome, email, senha);
    }

    public Administrador(String nome, Email email, SenhaHash senha) {
        super(null, nome, email, senha);
    }
}
