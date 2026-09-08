-- =====================================================
-- IngressoNow — V12: índice p/ vagas por atividade (RF-13, RNF-10)
-- contarPorAtividade filtra por atividade_id; a PK (inscricao_id,
-- atividade_id) não cobre essa direção — sem o índice, seq scan
-- crescente com a base (RNF-10: 500+ inscritos).
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_ia_atividade ON inscricao_atividades(atividade_id);
