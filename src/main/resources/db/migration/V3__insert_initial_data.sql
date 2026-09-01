-- =====================================================
-- IngressoNow — V3: Dados iniciais para testes
-- Data de criação: 2026-09-01
-- =====================================================

-- =====================================================
-- USUÁRIOS (Participante e Organizador)
-- =====================================================
-- Senha para testes: "senha123" (em produção usar BCrypt)
-- Hash BCrypt de "senha123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

INSERT INTO usuarios (id, tipo_usuario, nome, email, senha_hash) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'PARTICIPANTE', 'Lucas Silva', 'lucas.silva@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
    ('550e8400-e29b-41d4-a716-446655440002', 'ORGANIZADOR', 'Maria Santos', 'maria.santos@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- =====================================================
-- EVENTOS
-- =====================================================
-- Eventos futuros com datas válidas (setembro de 2026)

INSERT INTO eventos (id, titulo, descricao, status, organizador_id, max_vagas) VALUES
    (
        '660e8400-e29b-41d4-a716-446655440001',
        'Conferência de Tecnologia 2026',
        'Evento anual sobre as últimas tendências em tecnologia, inteligência artificial, cloud computing e desenvolvimento de software. Participe de palestras, workshops e networking com profissionais da área.',
        'PUBLICADO',
        '550e8400-e29b-41d4-a716-446655440002',
        200
    ),
    (
        '660e8400-e29b-41d4-a716-446655440002',
        'Workshop de Java e Spring Boot',
        'Workshop prático sobre desenvolvimento de aplicações modernas com Java 17, Spring Boot 3, arquitetura hexagonal e boas práticas de programação orientada a objetos.',
        'PUBLICADO',
        '550e8400-e29b-41d4-a716-446655440002',
        50
    );

-- =====================================================
-- ATIVIDADES
-- =====================================================
-- Atividades vinculadas aos eventos com horários válidos
-- Conferência de Tecnologia: dias 15 e 16 de setembro de 2026
-- Workshop de Java: dia 20 de setembro de 2026

INSERT INTO atividades (id, titulo, inicio, fim, local, evento_id) VALUES
    -- Atividades da Conferência de Tecnologia
    (
        '770e8400-e29b-41d4-a716-446655440001',
        'Palestra: O Futuro da Inteligência Artificial',
        '2026-09-15 09:00:00',
        '2026-09-15 11:00:00',
        'Auditório Principal - Sala 101',
        '660e8400-e29b-41d4-a716-446655440001'
    ),
    (
        '770e8400-e29b-41d4-a716-446655440002',
        'Workshop: Cloud Computing na Prática',
        '2026-09-15 14:00:00',
        '2026-09-15 17:00:00',
        'Laboratório de Informática - Bloco B',
        '660e8400-e29b-41d4-a716-446655440001'
    ),
    (
        '770e8400-e29b-41d4-a716-446655440003',
        'Mesa Redonda: Segurança da Informação',
        '2026-09-16 10:00:00',
        '2026-09-16 12:00:00',
        'Sala de Conferências - 3º Andar',
        '660e8400-e29b-41d4-a716-446655440001'
    ),
    -- Atividades do Workshop de Java
    (
        '770e8400-e29b-41d4-a716-446655440004',
        'Módulo 1: Fundamentos do Spring Boot',
        '2026-09-20 09:00:00',
        '2026-09-20 12:00:00',
        'Laboratório de Programação - Sala 201',
        '660e8400-e29b-41d4-a716-446655440002'
    ),
    (
        '770e8400-e29b-41d4-a716-446655440005',
        'Módulo 2: Arquitetura Hexagonal',
        '2026-09-20 14:00:00',
        '2026-09-20 17:00:00',
        'Laboratório de Programação - Sala 201',
        '660e8400-e29b-41d4-a716-446655440002'
    );

-- =====================================================
-- INSCRIÇÕES
-- =====================================================
-- Participante Lucas Silva inscrito nos 2 eventos

INSERT INTO inscricoes (id, participante_id, evento_id, data_inscricao) VALUES
    (
        '880e8400-e29b-41d4-a716-446655440001',
        '550e8400-e29b-41d4-a716-446655440001',
        '660e8400-e29b-41d4-a716-446655440001',
        '2026-09-01 10:30:00'
    ),
    (
        '880e8400-e29b-41d4-a716-446655440002',
        '550e8400-e29b-41d4-a716-446655440001',
        '660e8400-e29b-41d4-a716-446655440002',
        '2026-09-01 11:00:00'
    );

-- =====================================================
-- ATIVIDADES SELECIONADAS (Agenda do Participante)
-- =====================================================
-- Lucas selecionou 3 atividades: 2 da conferência e 1 do workshop

INSERT INTO inscricao_atividades (inscricao_id, atividade_id) VALUES
    -- Atividades da Conferência de Tecnologia
    (
        '880e8400-e29b-41d4-a716-446655440001',
        '770e8400-e29b-41d4-a716-446655440001'  -- Palestra de IA
    ),
    (
        '880e8400-e29b-41d4-a716-446655440001',
        '770e8400-e29b-41d4-a716-446655440002'  -- Workshop de Cloud
    ),
    -- Atividade do Workshop de Java
    (
        '880e8400-e29b-41d4-a716-446655440002',
        '770e8400-e29b-41d4-a716-446655440004'  -- Módulo 1 Spring Boot
    );

-- =====================================================
-- FIM DA MIGRATION V3
-- =====================================================
-- Dados inseridos:
-- - 2 Usuários (1 Participante + 1 Organizador)
-- - 2 Eventos (ambos publicados)
-- - 5 Atividades (3 na conferência + 2 no workshop)
-- - 2 Inscrições (participante nos 2 eventos)
-- - 3 Seleções de atividades (agenda do participante)
-- =====================================================
