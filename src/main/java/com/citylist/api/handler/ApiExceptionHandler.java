package com.citylist.api.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = Objects.requireNonNullElse(ex.getMessage(), "Invalid request parameter");
        log.warn("Bad request: {}", message);
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
