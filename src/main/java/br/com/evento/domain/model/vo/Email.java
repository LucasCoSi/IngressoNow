package br.com.evento.domain.model.vo;

import br.com.evento.domain.exception.DomainException;
import java.util.Objects;
import java.util.regex.Pattern;

public class Email {
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private final String endereco;

    public Email(String endereco) {
        if (endereco == null || endereco.trim().isEmpty()) {
            throw new DomainException("O e-mail não pode ser vazio.");
        }
        String normalizado = endereco.trim().toLowerCase();
        if (!PATTERN.matcher(normalizado).matches()) {
            throw new DomainException("Formato de e-mail inválido.");
        }
        this.endereco = normalizado;
    }

    public String getEndereco() {
        return endereco;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return Objects.equals(endereco, email.endereco);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endereco);
    }

    @Override
    public String toString() {
        return endereco;
    }
}
