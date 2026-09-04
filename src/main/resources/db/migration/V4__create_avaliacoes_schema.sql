-- =====================================================
-- IngressoNow — V4: Questionários e avaliações (RF-24..RF-28)
-- RN-14: uma resposta por participante/questionário (unique).
-- RN-17: estrutura do questionário é congelada após respostas.
-- =====================================================

CREATE TABLE questionarios (
    id          VARCHAR(36) PRIMARY KEY,
    evento_id   VARCHAR(36) NOT NULL,
    titulo      VARCHAR(255) NOT NULL DEFAULT '',
    congelado   BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_questionario_evento FOREIGN KEY (evento_id)
        REFERENCES eventos(id) ON DELETE CASCADE
);

CREATE TABLE perguntas (
    id          VARCHAR(36) PRIMARY KEY,
    questionario_id VARCHAR(36) NOT NULL,
    enunciado   TEXT        NOT NULL,
    tipo        VARCHAR(30) NOT NULL,
    obrigatoria BOOLEAN     NOT NULL DEFAULT TRUE,
    opcoes      TEXT,
    escala_max  INTEGER,
    CONSTRAINT fk_pergunta_questionario FOREIGN KEY (questionario_id)
        REFERENCES questionarios(id) ON DELETE CASCADE
);

CREATE TABLE avaliacoes (
    id              VARCHAR(36) PRIMARY KEY,
    questionario_id VARCHAR(36) NOT NULL,
    inscricao_id    VARCHAR(36) NOT NULL,
    data_submissao  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avaliacao_questionario FOREIGN KEY (questionario_id)
        REFERENCES questionarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacao_inscricao FOREIGN KEY (inscricao_id)
        REFERENCES inscricoes(id) ON DELETE CASCADE,
    CONSTRAINT uk_avaliacao_questionario_inscricao UNIQUE (questionario_id, inscricao_id)
);

CREATE TABLE respostas (
    avaliacao_id VARCHAR(36) NOT NULL,
    pergunta_id  VARCHAR(36) NOT NULL,
    valor        TEXT,
    PRIMARY KEY (avaliacao_id, pergunta_id),
    CONSTRAINT fk_resposta_avaliacao FOREIGN KEY (avaliacao_id)
        REFERENCES avaliacoes(id) ON DELETE CASCADE,
    CONSTRAINT fk_resposta_pergunta FOREIGN KEY (pergunta_id)
        REFERENCES perguntas(id) ON DELETE CASCADE
);

CREATE INDEX idx_questionario_evento ON questionarios(evento_id);
CREATE INDEX idx_pergunta_questionario ON perguntas(questionario_id);
CREATE INDEX idx_avaliacao_questionario ON avaliacoes(questionario_id);
