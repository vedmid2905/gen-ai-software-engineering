package banking.controller;

import banking.dto.ErrorResponse;
import banking.exception.InvalidIdFormatException;
import banking.exception.NotFoundException;
import banking.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", ex.getErrors()));
    }

    @ExceptionHandler(InvalidIdFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidId(InvalidIdFormatException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage(), List.of()));
    }
}
