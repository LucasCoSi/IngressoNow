-- =====================================================
-- IngressoNow — V7: origem + responsável em check-ins (RF-22, RN-11/RN-12)
-- Rastreabilidade da frequência: QR_CODE (automático) ou
-- MANUAL (lançado por um responsável/organizador).
-- =====================================================

ALTER TABLE checkins ADD COLUMN IF NOT EXISTS origem VARCHAR(10) NOT NULL DEFAULT 'QR_CODE';
ALTER TABLE checkins ADD COLUMN IF NOT EXISTS responsavel_id VARCHAR(36);
