package com.azienda.documentmanager.controller;

import com.azienda.documentmanager.exception.ResourceNotFoundException;
import com.azienda.documentmanager.exception.StorageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex) {
        // Logs the error on console for debugging (Let's hope I'll never need this :) )
        ex.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Interno", "Si è verificato un errore inaspettato. Riprova più tardi.");
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorDetails> handleStorageError(StorageException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Errore Storage", ex.getMessage());
    }

    private ResponseEntity<ErrorDetails> buildResponse(HttpStatus status, String errorType, String message) {
        ErrorDetails error = new ErrorDetails(LocalDateTime.now(), status.value(), errorType, message);
        return new ResponseEntity<>(error, status);
    }
}
