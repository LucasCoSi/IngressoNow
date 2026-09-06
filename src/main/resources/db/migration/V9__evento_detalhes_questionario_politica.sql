-- =====================================================
-- IngressoNow — V9: detalhes do evento + política de identificação (RF-04, RF-13, RN-15, RN-20)
-- =====================================================

ALTER TABLE eventos ADD COLUMN IF NOT EXISTS periodo_inicio TIMESTAMP;
ALTER TABLE eventos ADD COLUMN IF NOT EXISTS periodo_fim TIMESTAMP;
ALTER TABLE eventos ADD COLUMN IF NOT EXISTS local VARCHAR(255);
ALTER TABLE eventos ADD COLUMN IF NOT EXISTS fuso VARCHAR(60) NOT NULL DEFAULT 'UTC';
ALTER TABLE eventos ADD COLUMN IF NOT EXISTS exige_escolha_atividades BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE eventos ADD COLUMN IF NOT EXISTS vagas_por_atividade BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE atividades ADD COLUMN IF NOT EXISTS politica_frequencia VARCHAR(20) NOT NULL DEFAULT 'UNICA';

ALTER TABLE questionarios ADD COLUMN IF NOT EXISTS politica_identificacao VARCHAR(15) NOT NULL DEFAULT 'IDENTIFICADO';
