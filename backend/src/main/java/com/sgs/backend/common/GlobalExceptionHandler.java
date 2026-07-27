package com.sgs.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

// @RestControllerAdvice intercepte les exceptions levées PAR N'IMPORTE QUEL
// controller de l'application. Sans ça, une exception non gérée renvoie
// une page d'erreur Spring par défaut (ou une stacktrace) au lieu d'un JSON propre.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Ressource introuvable (ex: GET /api/categories/999 qui n'existe pas) -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Erreurs de validation (@NotNull, @NotBlank... sur un DTO annoté @Valid) -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                details,
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
