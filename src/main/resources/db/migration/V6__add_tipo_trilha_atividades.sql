-- =====================================================
-- IngressoNow — V6: tipo + trilha em atividades (RF-05/RF-06)
-- Tipo configurável (palestra, workshop...) sem categoria fixa;
-- trilha para filtrar/agrupar a programação.
-- =====================================================

ALTER TABLE atividades ADD COLUMN IF NOT EXISTS tipo VARCHAR(60);
ALTER TABLE atividades ADD COLUMN IF NOT EXISTS trilha VARCHAR(60);

CREATE INDEX IF NOT EXISTS idx_atividade_tipo ON atividades(tipo);
CREATE INDEX IF NOT EXISTS idx_atividade_trilha ON atividades(trilha);
