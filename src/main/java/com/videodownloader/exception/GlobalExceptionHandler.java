package com.videodownloader.exception;

import com.videodownloader.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

/** Converte exceções em respostas JSON com mensagem clara. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> tratarApi(ApiException e) {
        return resposta(e.getStatus(), e.getMessage());
    }

    // yt-dlp falhou (vídeo indisponível, sem internet, yt-dlp não instalado...)
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> tratarIo(IOException e) {
        log.error("Falha ao executar o yt-dlp", e);
        return resposta(HttpStatus.BAD_GATEWAY,
                "Não foi possível consultar o vídeo. Verifique se o link "
                        + "está correto, se o vídeo está disponível e se o yt-dlp "
                        + "está instalado.");
    }

    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<ErrorResponse> tratarInterrupcao(InterruptedException e) {
        Thread.currentThread().interrupt();
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "A operação foi interrompida.");
    }

    // JSON malformado ou corpo ausente
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> tratarCorpoInvalido(
            HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Requisição inválida: envie um JSON no formato esperado.");
    }

    private ResponseEntity<ErrorResponse> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), mensagem));
    }
}