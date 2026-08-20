package br.com.evento.domain.model.usuario;

import java.util.regex.Pattern;

/**
 * Value Object (Objeto de Valor) que representa um endereço de e-mail.
 * Garante a validação de formato e imutabilidade.
 * (Cumpre o requisito ROO-03)
 */
public class Email {

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);

    private final String endereco;

    public Email(String endereco) {
        if (endereco == null || endereco.trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail não pode ser nulo ou vazio.");
        }
        if (!PATTERN.matcher(endereco).matches()) {
            throw new IllegalArgumentException("Formato de e-mail inválido.");
        }
        this.endereco = endereco;
    }

    public String getEndereco() {
        return endereco;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return endereco.equals(email.endereco);
    }

    @Override
    public int hashCode() {
        return endereco.hashCode();
    }

    @Override
    public String toString() {
        return endereco;
    }
}
