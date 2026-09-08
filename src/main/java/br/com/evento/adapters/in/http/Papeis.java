package br.com.evento.adapters.in.http;

import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Papéis de acesso (§3): separa o que é do organizador (gestão) vs
 * participante (próprios recursos). Uso: após {@code jwt.exigir(ex)} +
 * {@code jwt.autenticar(ex)}, antes da lógica. Responde 403 e retorna
 * {@code false} quando não pode seguir.
 */
public final class Papeis {

    private Papeis() {
    }

    /** Só organizador (gestão: eventos, check-in, relatórios, certificados, usuários). */
    public static boolean exigirOrganizador(Usuario logado, HttpExchange ex) throws IOException {
        if (logado instanceof Organizador) return true;
        HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                "Apenas organizadores.", ex.getRequestURI().getPath()));
        return false;
    }

    /** Dono do recurso ou organizador (perfil, inscrições, agenda, certificados próprios). */
    public static boolean exigirDonoOuOrganizador(Usuario logado, String donoId, HttpExchange ex)
            throws IOException {
        if (logado instanceof Organizador || logado.getId().equals(donoId)) return true;
        HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                "Você só pode acessar os próprios recursos.", ex.getRequestURI().getPath()));
        return false;
    }
}
