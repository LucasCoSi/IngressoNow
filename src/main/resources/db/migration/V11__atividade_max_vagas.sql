-- =====================================================
-- IngressoNow — V11: vagas por atividade (RF-13, RN-06)
-- null = sem limite; enforcement em SelecionarAtividadeUseCase
-- quando o evento tem vagas_por_atividade = TRUE.
-- =====================================================

ALTER TABLE atividades ADD COLUMN IF NOT EXISTS max_vagas INTEGER;
