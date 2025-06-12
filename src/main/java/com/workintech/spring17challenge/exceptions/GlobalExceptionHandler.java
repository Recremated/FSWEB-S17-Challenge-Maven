package com.workintech.spring17challenge.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponseError> handleApiException(ApiException ex) {
        log.error("ApiException: ", ex);

        // Use the HttpStatus from the exception instead of hardcoding BAD_REQUEST
        HttpStatus status = ex.getHttpStatus();

        ApiResponseError error = new ApiResponseError(
                ex.getMessage(),
                status.value(),
                System.currentTimeMillis());

        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseError> handleException(Exception ex) {
        log.error("Unhandled Exception: ", ex);
        ApiResponseError error = new ApiResponseError(
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}