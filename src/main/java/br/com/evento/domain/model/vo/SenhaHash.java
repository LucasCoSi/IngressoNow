package br.com.evento.domain.model.vo;

import br.com.evento.domain.exception.DomainException;
import java.util.Objects;

public class SenhaHash {
    private final String hash;

    public SenhaHash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            throw new DomainException("O hash da senha não pode ser vazio.");
        }
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SenhaHash senhaHash = (SenhaHash) o;
        return Objects.equals(hash, senhaHash.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }
}
