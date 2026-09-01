# Tarefa #2 - Sistema de Autenticação com Spring Security 6 e JWT

## ✅ Implementação Concluída

### 1. Dependências Maven Adicionadas

No `pom.xml`, foram adicionadas as seguintes dependências:

- `spring-boot-starter-security`
- `jjwt-api` (versão 0.12.6)
- `jjwt-impl` (versão 0.12.6)
- `jjwt-jackson` (versão 0.12.6)
- `spring-security-test` (scope test)

### 2. Componentes de Segurança Criados

#### 2.1. Camada de Domínio e Aplicação

**Casos de Uso:**
- `CadastrarUsuarioUseCase`: Cadastra novo usuário com senha criptografada
- `AutenticarUsuarioUseCase`: Autentica usuário validando credenciais

#### 2.2. Camada de Adaptadores (Infraestrutura)

**DTOs:**
- `LoginRequest`: Request de login (email, senha)
- `LoginResponse`: Response com token JWT e dados do usuário
- `CadastroRequest`: Request de cadastro (nome, email, senha, tipo)

**Segurança:**
- `JwtTokenProvider`: Utilitário para geração, validação e extração de claims do token JWT
- `JwtAuthenticationFilter`: Filtro que intercepta requisições e valida o token JWT
- `SecurityConfig`: Configuração central do Spring Security com:
  - Session Management: STATELESS
  - CSRF: Desabilitado
  - CORS: Configurado para frontend
  - Rotas públicas: `/api/auth/**`, arquivos estáticos, `GET /api/eventos/**`
  - Rotas protegidas: POST/PUT/DELETE de eventos, inscrições e presença

**Controller:**
- `AuthRestController`: Endpoints de autenticação
  - `POST /api/auth/login`: Autentica e retorna token JWT
  - `POST /api/auth/cadastro`: Cadastra usuário e retorna token JWT

**Exception Handler:**
- `GlobalExceptionHandler`: Tratamento global de exceções com respostas padronizadas

### 3. Configuração

No `application.properties`, foram adicionadas:

```properties
jwt.secret=my-secret-key-for-jwt-token-generation-minimum-256-bits-for-security
jwt.expiration=86400000
```

⚠️ **IMPORTANTE**: Em produção, mova o `jwt.secret` para variável de ambiente!

### 4. Frontend (JavaScript)

#### 4.1. API Client Atualizado

O arquivo `api-client.js` foi atualizado com:

**Gerenciamento de Token:**
- Objeto `Auth` com métodos para manipular token e dados do usuário no `localStorage`
- Função `getHeaders()` que injeta automaticamente o token JWT no header `Authorization: Bearer <token>`
- Logout automático em caso de resposta 401 (não autorizado)

**Nova API de Autenticação:**
- `API.Auth.login(email, senha)`: Faz login e armazena token
- `API.Auth.cadastro(nome, email, senha, tipo)`: Cadastra usuário
- `API.Auth.logout()`: Remove token e redireciona para login

**Atualização de Requisições:**
- Todas as funções `get()`, `post()`, `put()`, `del()` agora usam `getHeaders()` que adiciona o token JWT automaticamente

#### 4.2. Páginas HTML

Criadas duas novas páginas:

- `login.html`: Página de login com formulário
- `cadastro.html`: Página de cadastro com seleção de tipo de usuário (Participante/Organizador)

Ambas incluem:
- Validação de formulário
- Exibição de erros
- Redirecionamento automático se já autenticado
- Integração com a API de autenticação

## 📋 Como Testar

### 1. Compilar e Executar

```bash
mvn clean install
mvn spring-boot:run
```

### 2. Testar Endpoints via cURL

**Cadastro:**
```bash
curl -X POST http://localhost:8080/api/auth/cadastro \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "email": "joao@example.com",
    "senha": "senha123",
    "tipo": "PARTICIPANTE"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "senha": "senha123"
  }'
```

**Resposta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "Bearer",
  "usuarioId": "uuid-do-usuario",
  "nome": "João Silva",
  "email": "joao@example.com"
}
```

**Acessar recurso protegido:**
```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{ "nome": "Workshop Java", ... }'
```

### 3. Testar via Frontend

1. Acesse: `http://localhost:8080/cadastro.html`
2. Preencha o formulário de cadastro
3. Após cadastro, será redirecionado para a home
4. O token será armazenado no `localStorage`
5. Todas as requisições autenticadas incluirão o token automaticamente

## 🔒 Fluxo de Autenticação

1. **Cadastro/Login**: Usuário envia credenciais → Backend valida → Retorna token JWT
2. **Armazenamento**: Frontend salva token no `localStorage`
3. **Requisições**: Frontend adiciona `Authorization: Bearer <token>` em todas as requisições
4. **Validação**: `JwtAuthenticationFilter` intercepta, valida o token e popula o `SecurityContext`
5. **Autorização**: Spring Security valida permissões baseado nas rotas configuradas
6. **Expiração**: Token expira após 24h (configurável via `jwt.expiration`)

## 🛡️ Segurança

- **BCrypt**: Senhas são criptografadas com BCrypt (força de trabalho padrão)
- **JWT**: Tokens assinados com HMAC-SHA256
- **Stateless**: Sem sessão HTTP, totalmente stateless
- **CORS**: Configurado para origens específicas
- **Validação**: Bean Validation nos DTOs

## 📁 Arquivos Criados/Modificados

### Criados:
- `src/main/java/br/com/evento/adapters/in/web/dto/LoginRequest.java`
- `src/main/java/br/com/evento/adapters/in/web/dto/LoginResponse.java`
- `src/main/java/br/com/evento/adapters/in/web/dto/CadastroRequest.java`
- `src/main/java/br/com/evento/adapters/in/web/security/JwtTokenProvider.java`
- `src/main/java/br/com/evento/adapters/in/web/security/JwtAuthenticationFilter.java`
- `src/main/java/br/com/evento/adapters/in/web/security/SecurityConfig.java`
- `src/main/java/br/com/evento/adapters/in/web/AuthRestController.java`
- `src/main/java/br/com/evento/adapters/in/web/exception/GlobalExceptionHandler.java`
- `src/main/java/br/com/evento/application/usecase/CadastrarUsuarioUseCase.java`
- `src/main/java/br/com/evento/application/usecase/AutenticarUsuarioUseCase.java`
- `src/main/resources/static/login.html`
- `src/main/resources/static/cadastro.html`

### Modificados:
- `pom.xml` (dependências de segurança e JWT)
- `src/main/resources/application.properties` (configurações JWT)
- `src/main/resources/static/js/api-client.js` (gerenciamento de token)

## ✅ Checklist de Validação

- [x] Dependências Maven adicionadas
- [x] Bean `BCryptPasswordEncoder` configurado
- [x] Casos de uso de cadastro e autenticação criados
- [x] `JwtTokenProvider` implementado (geração, validação, extração)
- [x] `JwtAuthenticationFilter` implementado
- [x] `SecurityConfig` configurado (STATELESS, CORS, rotas públicas/protegidas)
- [x] Endpoints `/api/auth/login` e `/api/auth/cadastro` implementados
- [x] Frontend atualizado (gerenciamento de token no `localStorage`)
- [x] Páginas de login e cadastro criadas
- [x] Handler global de exceções implementado

## 🎯 Próximos Passos Sugeridos

1. **Testar a compilação**: Execute `mvn clean install`
2. **Iniciar o banco de dados**: Certifique-se de que o PostgreSQL está rodando
3. **Executar a aplicação**: `mvn spring-boot:run`
4. **Testar endpoints**: Use o cURL ou Postman para testar cadastro e login
5. **Testar frontend**: Acesse as páginas HTML no navegador
6. **Criar testes unitários**: Testar os casos de uso e o `JwtTokenProvider`
7. **Criar testes de integração**: Testar os endpoints com Spring Security Test

## 📝 Notas Importantes

- O sistema segue a **Arquitetura Hexagonal** do projeto
- A senha é criptografada no **Use Case**, não no controller
- O token JWT contém `email` (subject) e `userId` (claim customizada)
- Rotas públicas permitem acesso sem autenticação
- Erro 401 no frontend resulta em logout automático e redirecionamento
