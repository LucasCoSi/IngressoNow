-- =====================================================
-- IngressoNow — V5: Persistência de check-ins (RN-11)
-- Check-ins eram calculados só em memória e se perdiam
-- entre requisições; agora são tabela própria.
-- =====================================================

CREATE TABLE checkins (
    id              VARCHAR(36) PRIMARY KEY,
    inscricao_id    VARCHAR(36) NOT NULL,
    atividade_id    VARCHAR(36) NOT NULL,
    data_hora       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_checkin_inscricao FOREIGN KEY (inscricao_id)
        REFERENCES inscricoes(id) ON DELETE CASCADE,
    CONSTRAINT fk_checkin_atividade FOREIGN KEY (atividade_id)
        REFERENCES atividades(id) ON DELETE CASCADE
);

CREATE INDEX idx_checkin_inscricao ON checkins(inscricao_id);
CREATE INDEX idx_checkin_atividade ON checkins(atividade_id);
