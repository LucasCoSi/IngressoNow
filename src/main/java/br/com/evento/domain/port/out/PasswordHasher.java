package br.com.evento.domain.port.out;

/**
 * Porta de saída — hash e verificação de senhas (RNF-05).
 * Implementação usa jBCrypt; o domínio nunca vê a lib.
 */
public interface PasswordHasher {
    String hash(String senhaTexto);
    boolean matches(String senhaTexto, String hash);
}
