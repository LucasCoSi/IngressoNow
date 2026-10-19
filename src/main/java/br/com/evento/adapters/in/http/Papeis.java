package br.com.evento.adapters.in.http;

import br.com.evento.domain.model.entity.Administrador;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Papéis de acesso (§3/RN-18): Participante (próprios recursos),
 * Organizador (só os próprios eventos e seus inscritos),
 * Administrador (tudo). Uso: após {@code jwt.exigir(ex)} +
 * {@code jwt.autenticar(ex)}, antes da lógica. Responde 403 e retorna
 * {@code false} quando não pode seguir.
 */
public final class Papeis {

    private Papeis() {
    }

    /** Só administrador (gestão global: usuários, parâmetros, qualquer evento). */
    public static boolean exigirAdministrador(Usuario logado, HttpExchange ex) throws IOException {
        if (logado instanceof Administrador) return true;
        HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                "Apenas administradores.", ex.getRequestURI().getPath()));
        return false;
    }

    /** Organizador ou administrador (criar evento, telas de gestão). */
    public static boolean exigirGestor(Usuario logado, HttpExchange ex) throws IOException {
        if (logado instanceof Organizador || logado instanceof Administrador) return true;
        HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                "Apenas organizadores.", ex.getRequestURI().getPath()));
        return false;
    }

    /** Dono do recurso ou administrador (perfil, inscrições, agenda, certificados próprios). */
    public static boolean exigirDonoOuAdmin(Usuario logado, String donoId, HttpExchange ex)
            throws IOException {
        if (logado instanceof Administrador || logado.getId().equals(donoId)) return true;
        HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                "Você só pode acessar os próprios recursos.", ex.getRequestURI().getPath()));
        return false;
    }

    /** Dono do evento (organizador) ou administrador (RN-18). */
    public static boolean exigirDonoEventoOuAdmin(Usuario logado, Evento evento, HttpExchange ex)
            throws IOException {
        if (logado instanceof Administrador) return true;
        if (logado instanceof Organizador && evento.getOrganizador() != null
                && logado.getId().equals(evento.getOrganizador().getId())) return true;
        HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                "Você só pode gerenciar os próprios eventos.", ex.getRequestURI().getPath()));
        return false;
    }
}
