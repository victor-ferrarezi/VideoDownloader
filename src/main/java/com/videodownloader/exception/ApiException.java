package com.videodownloader.exception;

import org.springframework.http.HttpStatus;

//erro "esperado" da API: carrega a mensagem e o código HTTP.
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}