-- =====================================================
-- IngressoNow — V14: parâmetros gerais da organização (§3, admin)
-- O administrador gerencia os parâmetros gerais (identidade da instituição
-- e percentual padrão de certificado). Linha única (id = 'global').
-- =====================================================

CREATE TABLE IF NOT EXISTS configuracoes_organizacao (
    id                            VARCHAR(36) PRIMARY KEY,
    nome_instituicao              VARCHAR(255) NOT NULL,
    sigla                         VARCHAR(40),
    email_contato                 VARCHAR(255),
    site                          VARCHAR(255),
    percentual_certificado_padrao INTEGER,
    atualizado_em                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_por                VARCHAR(36)
);

INSERT INTO configuracoes_organizacao
    (id, nome_instituicao, sigla, email_contato, site, percentual_certificado_padrao)
VALUES
    ('global', 'Centro de Eventos Sul', 'CES', 'contato@centroeventos.com.br', NULL, NULL)
ON CONFLICT (id) DO NOTHING;
