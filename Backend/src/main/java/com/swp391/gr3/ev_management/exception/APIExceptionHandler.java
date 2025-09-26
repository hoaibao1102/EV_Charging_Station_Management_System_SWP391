package com.swp391.gr3.ev_management.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class APIExceptionHandler {

    // chạy mỗi khi mà dính lỗi
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus()
    public ResponseEntity handleBadRequest(MethodArgumentNotValidException exception) {
        String msg = "";
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            msg += error.getDefaultMessage() + "\n";
        }
        return ResponseEntity.badRequest().body(msg);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus()
    public ResponseEntity handleBadCredentialsException(BadCredentialsException exception) {
        return ResponseEntity.status(401).body("Invalid username or password");
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity handleInternalAuthenticationServiceException(InternalAuthenticationServiceException exception) {
        return ResponseEntity.status(401).body("Invalid username or password");
    }

}

