package br.com.evento.adapters.in.web.exception;

import br.com.evento.domain.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<StandardError> handleDomainException(DomainException ex, HttpServletRequest request) {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<StandardError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new StandardError.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Um ou mais campos estão inválidos",
                request.getRequestURI(),
                fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<StandardError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
            .map(v -> new StandardError.FieldError(v.getPropertyPath().toString(), v.getMessage()))
            .toList();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Um ou mais campos estão inválidos",
                request.getRequestURI(),
                fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<StandardError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Credenciais inválidas",
                request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Acesso negado",
                request.getRequestURI()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardError> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("nao encontrad")) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new StandardError(
                    Instant.now(),
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    ex.getMessage(),
                    request.getRequestURI()));
        }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocorreu um erro interno no servidor. Tente novamente mais tarde.",
                request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGenericException(Exception ex, HttpServletRequest request) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new StandardError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocorreu um erro interno no servidor. Tente novamente mais tarde.",
                request.getRequestURI()));
    }
}
