package com.azienda.documentmanager.controllers;

import com.azienda.documentmanager.dtos.ErrorDetails;
import com.azienda.documentmanager.exceptions.ResourceNotFoundException;
import com.azienda.documentmanager.exceptions.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorDetails> buildResponse(HttpStatus status, String errorType, String message) {
        ErrorDetails error = new ErrorDetails(LocalDateTime.now(), status.value(), errorType, message);
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Risorsa non trovata", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Accesso negato", "Non hai i permessi per eseguire questa operazione.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDetails> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File troppo grande", "La dimensione del file supera il limite massimo.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Dati non validi", ex.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDetails> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Parametro non valido",
                "Il parametro '" + ex.getName() + "' ha un formato non valido.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDetails> handleNoResource(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Endpoint inesistente", "La risorsa richiesta non esiste.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDetails> handleConflict(OptimisticLockingFailureException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflitto di modifica",
                "Il documento è stato modificato da un altro utente. Ricarica e riprova.");
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorDetails> handleStorageError(StorageException ex) {
        log.error("Errore storage Supabase", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Storage",
                "Errore nella gestione del file. Riprova più tardi.");
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleAuthError(AuthenticationCredentialsNotFoundException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Non Autenticato", ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDetails> handleMissingParameters(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Missing Required Parameters", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex) {
        log.error("Errore inatteso", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Interno",
                "Si è verificato un errore inaspettato. Riprova più tardi.");
    }
}
