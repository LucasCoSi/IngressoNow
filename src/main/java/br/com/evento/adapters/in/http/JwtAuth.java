package br.com.evento.adapters.in.http;

import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.port.out.UsuarioRepository;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Autenticação JWT sem Spring Security: emite na login e valida o Bearer
 * nas rotas protegidas (substitui SecurityConfig + filtros).
 */
public class JwtAuth {

    private final SecretKey key;
    private final long validityMs;
    private final UsuarioRepository usuarios;

    public JwtAuth(String secret, long validityMs, UsuarioRepository usuarios) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMs = validityMs;
        this.usuarios = usuarios;
    }

    public String emitir(Usuario usuario) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(usuario.getEmail().getEndereco())
                .claim("userId", usuario.getId())
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + validityMs))
                .signWith(key)
                .compact();
    }

    /** Valida o Bearer e devolve o usuário; vazio = não autenticado. */
    public Optional<Usuario> autenticar(HttpExchange ex) {
        List<String> auth = ex.getRequestHeaders().get("Authorization");
        if (auth == null || auth.isEmpty()) return Optional.empty();
        String bearer = auth.get(0);
        if (bearer == null || !bearer.startsWith("Bearer ")) return Optional.empty();
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(bearer.substring(7)).getPayload();
            if (claims.getExpiration().before(new Date())) return Optional.empty();
            String userId = claims.get("userId", String.class);
            if (userId == null) return Optional.empty();
            return usuarios.buscarPorId(userId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean exigir(HttpExchange ex) throws java.io.IOException {
        if (autenticar(ex).isPresent()) return true;
        HttpErrors.json(ex, 401, StandardError.de(401, "Unauthorized",
                "Autenticação necessária. Faça login.", ex.getRequestURI().getPath()));
        return false;
    }
}
