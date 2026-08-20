-- =====================================================
-- IngressoNow — V1: Schema inicial
-- Flyway migration
-- =====================================================

CREATE TABLE usuarios (
    id              VARCHAR(36)  PRIMARY KEY,
    tipo_usuario    VARCHAR(31)  NOT NULL,
    nome            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    senha_hash      VARCHAR(255) NOT NULL
);

CREATE TABLE eventos (
    id              VARCHAR(36)  PRIMARY KEY,
    titulo          VARCHAR(255) NOT NULL,
    descricao       TEXT,
    status          VARCHAR(30)  NOT NULL DEFAULT 'RASCUNHO',
    organizador_id  VARCHAR(36)  NOT NULL,
    CONSTRAINT fk_evento_organizador FOREIGN KEY (organizador_id)
        REFERENCES usuarios(id)
);

CREATE TABLE atividades (
    id              VARCHAR(36)  PRIMARY KEY,
    titulo          VARCHAR(255) NOT NULL,
    inicio          TIMESTAMP    NOT NULL,
    fim             TIMESTAMP    NOT NULL,
    local           VARCHAR(255) NOT NULL,
    evento_id       VARCHAR(36)  NOT NULL,
    CONSTRAINT fk_atividade_evento FOREIGN KEY (evento_id)
        REFERENCES eventos(id) ON DELETE CASCADE
);

-- Índices para consultas frequentes
CREATE INDEX idx_evento_status ON eventos(status);
CREATE INDEX idx_evento_organizador ON eventos(organizador_id);
CREATE INDEX idx_atividade_evento ON atividades(evento_id);
