# Implementação de Bean Validation e Exception Handler Global

## 📅 Data: 01 de Setembro de 2026

---

## ✅ Implementações Realizadas

### 1️⃣ Dependência Maven Adicionada

**Arquivo**: `pom.xml`

Adicionada a dependência `spring-boot-starter-validation`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

### 2️⃣ DTOs de Request Criados com Validações

Criados 5 DTOs validados no pacote `br.com.evento.adapters.in.web.dto`:

#### ✅ `CheckInRequest.java`
- `@NotBlank` para `qrCodeToken`

#### ✅ `InscreverRequest.java`
- `@NotBlank` e `@Size` (36 caracteres UUID) para `participanteId` e `eventoId`

#### ✅ `CriarEventoRequest.java`
- `@NotBlank` e `@Size` para `titulo` (3-200 caracteres)
- `@NotBlank` e `@Size` para `descricao` (10-2000 caracteres)
- `@NotBlank` para `organizadorId`
- `@Positive` para `maxVagas`

#### ✅ `CriarAtividadeRequest.java`
- `@NotBlank` e `@Size` para `titulo` (3-200 caracteres)
- `@NotBlank` e `@Size` para `descricao` (10-1000 caracteres)
- `@NotNull` e `@FutureOrPresent` para `inicio`
- `@NotNull` e `@Future` para `fim`
- `@NotBlank` e `@Size` para `local` (3-200 caracteres)
- `@NotBlank` para `eventoId`
- `@Positive` para `maxVagas`

#### ✅ `CriarUsuarioRequest.java`
- `@NotBlank` e `@Size` para `nome` (3-100 caracteres)
- `@NotBlank`, `@Email` e `@Size` para `email` (máx 150 caracteres)
- `@NotBlank` e `@Pattern` para `cpf` (11 dígitos numéricos)
- `@NotBlank` e `@Pattern` para `tipo` (PARTICIPANTE ou ORGANIZADOR)

---

### 3️⃣ DTO de Resposta de Erro Padronizada

**Arquivo**: `StandardError.java`

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardError(
    Instant timestamp,
    Integer status,
    String error,
    String message,
    String path,
    List<FieldError> fieldErrors
)
```

**Campos**:
- `timestamp`: Momento do erro
- `status`: Código HTTP
- `error`: Tipo de erro
- `message`: Mensagem descritiva
- `path`: Endpoint que gerou o erro
- `fieldErrors`: Lista de campos inválidos (opcional)

**Record interno**:
```java
public record FieldError(String field, String message) {}
```

---

### 4️⃣ Handler Global de Exceções

**Arquivo**: `GlobalExceptionHandler.java`

Anotado com `@RestControllerAdvice`, tratando:

#### ✅ `MethodArgumentNotValidException` → HTTP 400 (Bad Request)
- Retorna lista detalhada de campos inválidos
- Mensagem: "Um ou mais campos estão inválidos"
- Inclui `fieldErrors` com campo e mensagem de cada erro

#### ✅ `ConstraintViolationException` → HTTP 400 (Bad Request)
- Violações de constraints do Bean Validation
- Retorna lista de violações com campo e mensagem

#### ✅ `DomainException` → HTTP 422 (Unprocessable Entity)
- Exceções de regras de negócio do domínio
- Mensagem customizada da exceção

#### ✅ `IllegalArgumentException` → HTTP 400 (Bad Request)
- Argumentos inválidos
- Mensagem da exceção

#### ✅ `RuntimeException` → HTTP 404 (Not Found) ou HTTP 500
- Detecta "nao encontrad" na mensagem → HTTP 404
- Outros casos → HTTP 500

#### ✅ `Exception` (Genérica) → HTTP 500 (Internal Server Error)
- Mensagem segura: "Ocorreu um erro interno no servidor. Tente novamente mais tarde."
- Não expõe detalhes internos

---

### 5️⃣ Controllers REST Atualizados

#### ✅ `InscricaoRestController.java`
- Adicionado `@Valid` no método `inscrever()`
- Importado DTO `InscreverRequest` do pacote `dto`
- Removido record local

#### ✅ `ControlePresencaRestController.java`
- Adicionado `@Valid` no método `realizarCheckIn()`
- Importado DTO `CheckInRequest` do pacote `dto`
- Removido record local

---

## 📂 Estrutura de Arquivos Criados

```
src/main/java/br/com/evento/
├── adapters/in/web/
│   ├── dto/
│   │   ├── CheckInRequest.java           ✅ NOVO
│   │   ├── InscreverRequest.java         ✅ NOVO
│   │   ├── CriarEventoRequest.java       ✅ NOVO
│   │   ├── CriarAtividadeRequest.java    ✅ NOVO
│   │   └── CriarUsuarioRequest.java      ✅ NOVO
│   ├── exception/
│   │   ├── StandardError.java            ✅ NOVO
│   │   └── GlobalExceptionHandler.java   ✅ NOVO
│   ├── InscricaoRestController.java      ✅ ATUALIZADO
│   └── ControlePresencaRestController.java ✅ ATUALIZADO
```

---

## 🔧 Próximos Passos

1. **Compilar o Projeto**:
   ```bash
   mvn clean compile -DskipTests
   ```

2. **Criar Controllers Faltantes**:
   - `EventoController` com endpoints POST/PUT usando `CriarEventoRequest`
   - `AtividadeController` usando `CriarAtividadeRequest`
   - `UsuarioController` usando `CriarUsuarioRequest`

3. **Testar Validações**:
   - Enviar requisições com campos inválidos
   - Verificar respostas HTTP 400 com `fieldErrors`
   - Testar exceções de domínio (HTTP 422)

---

## 📋 Exemplo de Resposta de Erro

### Requisição Inválida:
```json
POST /api/inscricoes
{
  "participanteId": "",
  "eventoId": "123"
}
```

### Resposta HTTP 400:
```json
{
  "timestamp": "2026-09-01T21:30:00.000Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Um ou mais campos estão inválidos",
  "path": "/api/inscricoes",
  "fieldErrors": [
    {
      "field": "participanteId",
      "message": "O ID do participante é obrigatório"
    },
    {
      "field": "eventoId",
      "message": "O ID do evento deve ser um UUID válido"
    }
  ]
}
```

---

## 🎯 Benefícios Implementados

✅ **Validação Automática**: Dados validados antes de chegarem aos Use Cases  
✅ **Mensagens Claras**: Respostas padronizadas e descritivas  
✅ **Segurança**: Exceções internas não expõem detalhes sensíveis  
✅ **Rastreabilidade**: Timestamp e path em todos os erros  
✅ **Manutenibilidade**: Tratamento centralizado em um único lugar  
✅ **Arquitetura Limpa**: Validações na camada de adapters (entrada)  

---

## 📌 Notas Importantes

- **Jakarta Validation**: Spring Boot 3.2.3 usa `jakarta.validation.*`, não `javax.validation.*`
- **@Valid vs @Validated**: Usamos `@Valid` para validação em métodos de controllers
- **Campos Opcionais**: `fieldErrors` só aparece em erros de validação de campos
- **Segurança**: Mensagens genéricas para erros internos (HTTP 500)
- **Padrão REST**: Códigos HTTP semânticos (400, 404, 422, 500)

---

**Tarefa #4 Concluída! ✨**

Aguardando compilação do projeto com Maven.
