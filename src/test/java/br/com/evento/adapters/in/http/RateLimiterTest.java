package br.com.evento.adapters.in.http;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private final AtomicLong agora = new AtomicLong(1_000);

    private RateLimiter limiter() {
        return new RateLimiter(agora::get);
    }

    @Test
    void permiteAteOLimiteEDepoisBloqueia() {
        RateLimiter r = limiter();
        assertTrue(r.permitir("k", 2, 1_000));
        assertTrue(r.permitir("k", 2, 1_000));
        assertFalse(r.permitir("k", 2, 1_000));
    }

    @Test
    void liberaQuandoAJanelaExpira() {
        RateLimiter r = limiter();
        assertTrue(r.permitir("k", 1, 1_000));
        assertFalse(r.permitir("k", 1, 1_000));
        agora.addAndGet(1_001);
        assertTrue(r.permitir("k", 1, 1_000));
    }

    @Test
    void chavesSaoIndependentes() {
        RateLimiter r = limiter();
        assertTrue(r.permitir("login:1", 1, 1_000));
        assertFalse(r.permitir("login:1", 1, 1_000));
        assertTrue(r.permitir("login:2", 1, 1_000));
    }

    @Test
    void limparZeraAChave() {
        RateLimiter r = limiter();
        assertTrue(r.permitir("k", 1, 1_000));
        r.limpar("k");
        assertTrue(r.permitir("k", 1, 1_000));
    }

    @Test
    void esperaSegundosInformaQuandoLibera() {
        RateLimiter r = limiter();
        assertTrue(r.permitir("k", 1, 10_000));
        long espera = r.esperaSegundos("k", 1, 10_000);
        assertTrue(espera >= 1 && espera <= 10, "espera=" + espera);
    }
}
