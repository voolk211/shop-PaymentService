package com.shop.paymentservice.controller;

import com.shop.paymentservice.exception.ExceptionBody;
import com.shop.paymentservice.exception.ExternalServiceException;
import com.shop.paymentservice.exception.ExternalServiceUnavailableException;
import com.shop.paymentservice.exception.ResourceNotFoundException;
import com.shop.paymentservice.exception.KafkaDeliveryException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(KafkaDeliveryException.class)
    public ResponseEntity<ExceptionBody> handleKafkaDelivery(KafkaDeliveryException e){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ExceptionBody(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionBody> handleResourceNotFound(ResourceNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ExceptionBody(e.getMessage()));
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<ExceptionBody> handleExternalServiceUnavailable(ExternalServiceUnavailableException e){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ExceptionBody(e.getMessage()));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ExceptionBody> handleExternalService(ExternalServiceException e){
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ExceptionBody(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionBody> handleAccessDenied(AccessDeniedException e){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ExceptionBody(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ExceptionBody> handleIllegalState(IllegalStateException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionBody(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionBody> handleIllegalArgument(IllegalArgumentException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionBody(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionBody> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        ExceptionBody exceptionBody = new ExceptionBody("Validation failed");
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        exceptionBody.setErrors(errors.stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fieldError ->
                                Optional.ofNullable(fieldError.getDefaultMessage())
                                        .orElse("Empty message")
                )));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionBody);

    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionBody> handleConstraintViolationException(ConstraintViolationException e) {
        ExceptionBody exceptionBody = new ExceptionBody("Validation failed");
        exceptionBody.setErrors(e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> Optional.ofNullable(violation.getMessage()).orElse("Empty message")
                )));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionBody);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionBody> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ExceptionBody(e.getMessage()));
    }

}
