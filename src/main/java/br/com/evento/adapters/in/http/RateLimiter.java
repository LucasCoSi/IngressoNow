package br.com.evento.adapters.in.http;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limitador de tentativas em memória (janela deslizante).
 *
 * <p>Protege login e cadastro contra força bruta/abuso simples, devolvendo 429
 * com {@code Retry-After}. Não substitui um rate limit distribuído: reinicia
 * junto com o processo e é local a uma instância — suficiente para o MVP.</p>
 */
public final class RateLimiter {

    private final Map<String, Deque<Long>> tentativas = new ConcurrentHashMap<>();
    private final LongSupplier relogio;

    public RateLimiter() {
        this(System::currentTimeMillis);
    }

    /** Construtor com relógio injetável (testes). */
    RateLimiter(LongSupplier relogio) {
        this.relogio = relogio;
    }

    /**
     * Registra uma tentativa para a chave e informa se ela é permitida.
     * Conta tentativas (inclusive bem-sucedidas) dentro da janela.
     */
    public boolean permitir(String chave, int maximo, long janelaMs) {
        long agora = relogio.getAsLong();
        Deque<Long> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            descartarAntigas(fila, agora, janelaMs);
            if (fila.size() >= maximo) {
                return false;
            }
            fila.addLast(agora);
            return true;
        }
    }

    /** Segundos até a próxima tentativa caber na janela (mínimo 1). */
    public long esperaSegundos(String chave, int maximo, long janelaMs) {
        long agora = relogio.getAsLong();
        Deque<Long> fila = tentativas.get(chave);
        if (fila == null) return 0;
        synchronized (fila) {
            descartarAntigas(fila, agora, janelaMs);
            if (fila.size() < maximo || fila.isEmpty()) return 0;
            long liberaEm = fila.peekFirst() + janelaMs;
            return Math.max(1, (liberaEm - agora + 999) / 1000);
        }
    }

    /** Zera a chave (ex.: após login bem-sucedido). */
    public void limpar(String chave) {
        tentativas.remove(chave);
    }

    private static void descartarAntigas(Deque<Long> fila, long agora, long janelaMs) {
        while (!fila.isEmpty() && agora - fila.peekFirst() >= janelaMs) {
            fila.pollFirst();
        }
    }
}
