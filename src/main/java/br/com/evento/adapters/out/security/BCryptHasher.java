package br.com.evento.adapters.out.security;

import br.com.evento.domain.port.out.PasswordHasher;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Adapter de saída — BCrypt via jBCrypt (sem Spring).
 * Hashes compatíveis com os seeds V3 ($2a$...).
 */
public class BCryptHasher implements PasswordHasher {

    @Override
    public String hash(String senhaTexto) {
        return BCrypt.hashpw(senhaTexto, BCrypt.gensalt());
    }

    @Override
    public boolean matches(String senhaTexto, String hash) {
        try {
            return BCrypt.checkpw(senhaTexto, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
