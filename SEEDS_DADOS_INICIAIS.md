# Seeds de Dados Iniciais - Migration V3 (+ V13: terceiro perfil)

## 📅 Data: 01 de Setembro de 2026 — revisado em 06/09/2026 (base zerada V1–V8 OK)
— V13 em 15/10/2026: Maria promovida a ADMINISTRADORA + Ana organizadora seed.

> Validação 2026-09-06 (`docker-compose down -v && up --build`): V1–V8
> aplicadas em ordem, login Maria/Lucas OK, `GET /api/eventos` retorna
> 2 PUBLICADO com 5 atividades. Hash válido atual abaixo (o antigo
> `$2a$10$N9qo8...` era inválido e foi corrigido na V3).

---

## ✅ Implementação Concluída

### Arquivo Criado
**`src/main/resources/db/migration/V3__insert_initial_data.sql`**

---

## 📊 Dados Inseridos

### 1️⃣ Usuários (2 na V3 + 1 na V13)

| ID | Tipo | Nome | Email | Senha |
|---|---|---|---|---|
| `550e8400-e29b-41d4-a716-446655440001` | PARTICIPANTE | Lucas Silva | lucas.silva@email.com | senha123 |
| `550e8400-e29b-41d4-a716-446655440002` | ADMINISTRADOR (era ORGANIZADOR na V3, promovido na V13) | Maria Santos | maria.santos@email.com | senha123 |
| `550e8400-e29b-41d4-a716-446655440003` | ORGANIZADOR (seed V13) | Ana Organizadora | ana.organizadora@email.com | senha123 |

**Hash BCrypt** (`senha123`): `$2a$10$TW7F2EXj42YabuptHEuSi.hj1KR.J/TrRz0WtjO6yE0UUhlgPKUMO`

---

### 2️⃣ Eventos (2)

#### Evento 1: Conferência de Tecnologia 2026
- **ID**: `660e8400-e29b-41d4-a716-446655440001`
- **Status**: PUBLICADO
- **Organizador**: Maria Santos
- **Vagas**: 200
- **Descrição**: Evento sobre tendências em tecnologia, IA, cloud e desenvolvimento

#### Evento 2: Workshop de Java e Spring Boot
- **ID**: `660e8400-e29b-41d4-a716-446655440002`
- **Status**: PUBLICADO
- **Organizador**: Maria Santos
- **Vagas**: 50
- **Descrição**: Workshop prático sobre Java 17, Spring Boot 3 e arquitetura hexagonal

---

### 3️⃣ Atividades (5)

#### Conferência de Tecnologia (3 atividades)

| Título | Data | Horário | Local |
|--------|------|---------|-------|
| Palestra: O Futuro da IA | 15/09/2026 | 09:00 - 11:00 | Auditório Principal - Sala 101 |
| Workshop: Cloud Computing na Prática | 15/09/2026 | 14:00 - 17:00 | Lab. de Informática - Bloco B |
| Mesa Redonda: Segurança da Informação | 16/09/2026 | 10:00 - 12:00 | Sala de Conferências - 3º Andar |

#### Workshop de Java (2 atividades)

| Título | Data | Horário | Local |
|--------|------|---------|-------|
| Módulo 1: Fundamentos do Spring Boot | 20/09/2026 | 09:00 - 12:00 | Lab. de Programação - Sala 201 |
| Módulo 2: Arquitetura Hexagonal | 20/09/2026 | 14:00 - 17:00 | Lab. de Programação - Sala 201 |

---

### 4️⃣ Inscrições (2)

**Participante**: Lucas Silva

| Evento | Data da Inscrição |
|--------|-------------------|
| Conferência de Tecnologia 2026 | 01/09/2026 10:30 |
| Workshop de Java e Spring Boot | 01/09/2026 11:00 |

---

### 5️⃣ Atividades Selecionadas (3)

**Agenda de Lucas Silva**:

| Evento | Atividade Selecionada |
|--------|-----------------------|
| Conferência | Palestra: O Futuro da IA |
| Conferência | Workshop: Cloud Computing na Prática |
| Workshop Java | Módulo 1: Fundamentos do Spring Boot |

---

## 🎯 Características do Script

### ✅ Boas Práticas Aplicadas

1. **IDs UUIDs Explícitos**: Não conflita com `BIGSERIAL`, usa UUIDs válidos
2. **Datas Válidas**: Eventos e atividades em setembro de 2026 (futuro)
3. **Integridade Referencial**: Todas as FKs apontam para registros existentes
4. **Dados Realistas**: Nomes, emails e descrições coerentes
5. **Comentários Detalhados**: Cada seção documentada
6. **Senhas Seguras**: Hash BCrypt (mesmo para testes)
7. **Status Correto**: Eventos com status PUBLICADO
8. **Constraints Respeitadas**: UNIQUE, NOT NULL, FK

---

## 🔄 Como Aplicar a Migration

### Opção 1: Docker (Recomendado)
```bash
# Recriar containers com banco limpo
docker-compose down -v
docker-compose up -d

# Aguardar inicialização
docker logs poo2-app -f
```

### Opção 2: JAR local (modo headless)
```bash
# Requer Postgres local (db.url em config.properties ou DB_URL)
java -jar target/plataforma-eventos-*.jar --headless
```

O `MigradorSql` aplica V1–V10 em ordem (manifesto `migrations.txt`).

---

## 📝 Validação dos Dados

### Consultas SQL para Verificar

```sql
-- Ver todos os usuários
SELECT id, tipo_usuario, nome, email FROM usuarios;

-- Ver todos os eventos
SELECT id, titulo, status, organizador_id, max_vagas FROM eventos;

-- Ver todas as atividades
SELECT id, titulo, inicio, fim, local FROM atividades;

-- Ver inscrições com dados do participante
SELECT i.id, u.nome as participante, e.titulo as evento, i.data_inscricao
FROM inscricoes i
JOIN usuarios u ON i.participante_id = u.id
JOIN eventos e ON i.evento_id = e.id;

-- Ver agenda do participante
SELECT u.nome, e.titulo as evento, a.titulo as atividade, a.inicio, a.fim
FROM inscricao_atividades ia
JOIN inscricoes i ON ia.inscricao_id = i.id
JOIN usuarios u ON i.participante_id = u.id
JOIN eventos e ON i.evento_id = e.id
JOIN atividades a ON ia.atividade_id = a.id
ORDER BY a.inicio;
```

---

## 🧪 Testes Possíveis

### APIs que Podem Usar os Seeds

1. **GET /api/eventos**
   - Deve retornar 2 eventos publicados

2. **GET /api/inscricoes/{inscricaoId}/agenda**
   - ID: `880e8400-e29b-41d4-a716-446655440001`
   - Deve retornar 2 atividades da conferência

3. **POST /api/inscricoes**
   - Criar nova inscrição para o participante Lucas

4. **GET /api/eventos/{eventoId}**
   - Buscar detalhes dos eventos criados

---

## 🔐 Credenciais para Testes

**Participante**:
- Email: `lucas.silva@email.com`
- Senha: `senha123`

**Organizador**:
- Email: `maria.santos@email.com`
- Senha: `senha123`

---

## 📌 Notas Importantes

- **UUIDs**: Todos os IDs seguem o padrão UUID v4
- **Datas**: Eventos em 15, 16 e 20 de setembro de 2026
- **Timezone**: `fuso` default `UTC` (coluna V9, RN-20); horários em `LocalDateTime` sem conversão de zona (simplificação declarada)
- **MigradorSql**: sem Flyway na era Java SE; baseline `flyway_schema_history` só em bancos da era Spring
- **Rollback**: sem rollback automático, faça backup do banco antes

---

**Tarefa #3 Concluída! ✨**

O banco de dados agora possui massa de testes realista para desenvolvimento.
