-- =====================================================
-- IngressoNow — V13: terceiro perfil (Administrador, §3)
-- Data de criação: 2026-10-15
-- =====================================================
-- O antigo ORGANIZADOR (poder total) passa a ADMINISTRADOR; o ORGANIZADOR
-- novo só gerencia os próprios eventos (RN-18). Migra a conta seed e cria
-- uma organizadora seed (senha "senha123", mesmo hash da V3).

UPDATE usuarios SET tipo_usuario = 'ADMINISTRADOR'
WHERE email = 'maria.santos@email.com';

INSERT INTO usuarios (id, tipo_usuario, nome, email, senha_hash) VALUES
    ('550e8400-e29b-41d4-a716-446655440003', 'ORGANIZADOR', 'Ana Organizadora', 'ana.organizadora@email.com', '$2a$10$TW7F2EXj42YabuptHEuSi.hj1KR.J/TrRz0WtjO6yE0UUhlgPKUMO');
