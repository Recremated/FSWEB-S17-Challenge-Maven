package com.workintech.spring17challenge.exceptions;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private HttpStatus httpStatus;

    // 2-parametreli constructor
    public ApiException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    // 1-parametreli constructor (opsiyonel, default olarak BAD_REQUEST dönebilir)
    public ApiException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
