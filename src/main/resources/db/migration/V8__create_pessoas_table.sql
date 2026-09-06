-- =====================================================
-- IngressoNow — V8: pessoas vinculadas com papéis (RF-08)
-- Palestrante, apresentador, responsável, mediador...
-- Papel é texto livre configurável.
-- =====================================================

CREATE TABLE IF NOT EXISTS pessoas (
    id              VARCHAR(36) PRIMARY KEY,
    atividade_id    VARCHAR(36) NOT NULL,
    nome            VARCHAR(255) NOT NULL,
    papel           VARCHAR(60) NOT NULL,
    CONSTRAINT fk_pessoa_atividade FOREIGN KEY (atividade_id)
        REFERENCES atividades(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pessoa_atividade ON pessoas(atividade_id);
