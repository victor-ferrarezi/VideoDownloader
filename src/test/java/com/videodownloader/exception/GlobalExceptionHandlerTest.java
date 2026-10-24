package com.videodownloader.exception;

import com.videodownloader.dto.ErrorResponse;
import com.videodownloader.exception.ApiException;
import com.videodownloader.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionVirouRespostaComStatusEMensagem() {
        ResponseEntity<ErrorResponse> r = handler.tratarApi(
                new ApiException(HttpStatus.NOT_FOUND, "Download não encontrado."));

        assertEquals(404, r.getStatusCode().value());
        assertEquals("Download não encontrado.", r.getBody().mensagem());
    }

    @Test
    void falhaDoYtDlpVira502ESemVazarDetalheTecnico() {
        ResponseEntity<ErrorResponse> r = handler.tratarIo(
                new IOException("Cannot run program \"py\": CreateProcess error=2"));

        assertEquals(502, r.getStatusCode().value());
        // o usuário não deve ver o texto técnico da exceção
        assertFalse(r.getBody().mensagem().contains("CreateProcess"));
    }
}