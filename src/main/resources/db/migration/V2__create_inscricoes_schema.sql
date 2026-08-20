-- =====================================================
-- IngressoNow — V2: Inscrições e seleção de atividades
-- =====================================================

-- Limite de vagas configurável por evento (Strategy Pattern)
ALTER TABLE eventos ADD COLUMN max_vagas INTEGER;

-- Inscrições de participantes em eventos
CREATE TABLE inscricoes (
    id                VARCHAR(36)  PRIMARY KEY,
    participante_id   VARCHAR(36)  NOT NULL,
    evento_id         VARCHAR(36)  NOT NULL,
    data_inscricao    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inscricao_participante FOREIGN KEY (participante_id)
        REFERENCES usuarios(id),
    CONSTRAINT fk_inscricao_evento FOREIGN KEY (evento_id)
        REFERENCES eventos(id),
    CONSTRAINT uk_inscricao_participante_evento UNIQUE (participante_id, evento_id)
);

-- Atividades selecionadas por inscrição (agenda do participante)
CREATE TABLE inscricao_atividades (
    inscricao_id   VARCHAR(36) NOT NULL,
    atividade_id   VARCHAR(36) NOT NULL,
    PRIMARY KEY (inscricao_id, atividade_id),
    CONSTRAINT fk_ia_inscricao FOREIGN KEY (inscricao_id)
        REFERENCES inscricoes(id) ON DELETE CASCADE,
    CONSTRAINT fk_ia_atividade FOREIGN KEY (atividade_id)
        REFERENCES atividades(id)
);

CREATE INDEX idx_inscricao_participante ON inscricoes(participante_id);
CREATE INDEX idx_inscricao_evento ON inscricoes(evento_id);
