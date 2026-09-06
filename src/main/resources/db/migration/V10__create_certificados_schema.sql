-- =====================================================
-- IngressoNow — V10: certificados (RF-32–35, D; RN-16)
-- Critério por evento + atividades obrigatórias + certificados
-- com código verificável. RF-34 (e-mail): sem SMTP no MVP.
-- =====================================================

CREATE TABLE IF NOT EXISTS criterios_certificado (
    id                  VARCHAR(36) PRIMARY KEY,
    evento_id           VARCHAR(36) NOT NULL UNIQUE,
    percentual_minimo   INTEGER,
    CONSTRAINT fk_criterio_evento FOREIGN KEY (evento_id)
        REFERENCES eventos(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS criterio_atividades (
    criterio_id     VARCHAR(36) NOT NULL,
    atividade_id    VARCHAR(36) NOT NULL,
    PRIMARY KEY (criterio_id, atividade_id),
    CONSTRAINT fk_ca_criterio FOREIGN KEY (criterio_id)
        REFERENCES criterios_certificado(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_atividade FOREIGN KEY (atividade_id)
        REFERENCES atividades(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS certificados (
    id                  VARCHAR(36) PRIMARY KEY,
    codigo              VARCHAR(60) NOT NULL UNIQUE,
    evento_id           VARCHAR(36) NOT NULL,
    participante_id     VARCHAR(36) NOT NULL,
    participante_nome   VARCHAR(255) NOT NULL DEFAULT '',
    papel_declarado     VARCHAR(60),
    carga_horaria       INTEGER NOT NULL DEFAULT 0,
    emitido_em          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cert_evento FOREIGN KEY (evento_id)
        REFERENCES eventos(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cert_evento ON certificados(evento_id);
CREATE INDEX IF NOT EXISTS idx_cert_participante ON certificados(participante_id);
