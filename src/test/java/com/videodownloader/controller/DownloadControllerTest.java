package com.videodownloader.controller;

import com.videodownloader.controller.DownloadController;
import com.videodownloader.dto.DownloadRequest;
import com.videodownloader.dto.DownloadStatusResponse;
import com.videodownloader.exception.ApiException;
import com.videodownloader.model.DownloadStatus;
import com.videodownloader.model.DownloadTask;
import com.videodownloader.service.DownloadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadControllerTest {

    private static final String URL_VALIDA = "https://youtu.be/abc123";

    @Mock
    private DownloadService downloadService;

    @InjectMocks
    private DownloadController controller;

    @Test
    void iniciarComUrlInvalidaRetorna400() {
        ApiException erro = assertThrows(ApiException.class,
                () -> controller.iniciar(new DownloadRequest("https://google.com", "", false)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        verifyNoInteractions(downloadService);
    }

    @Test
    void iniciarComFormatoSuspeitoRetorna400() {
        ApiException erro = assertThrows(ApiException.class,
                () -> controller.iniciar(new DownloadRequest(URL_VALIDA, "137; rm -rf", false)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        verifyNoInteractions(downloadService);
    }

    @Test
    void iniciarComDadosValidosRetorna202ComOId() {
        when(downloadService.iniciar(URL_VALIDA, "", false))
                .thenReturn(new DownloadTask("id-1"));

        ResponseEntity<DownloadStatusResponse> resposta =
                controller.iniciar(new DownloadRequest(URL_VALIDA, "", false));

        assertEquals(202, resposta.getStatusCode().value());
        assertEquals("id-1", resposta.getBody().id());
        assertEquals(DownloadStatus.EM_ANDAMENTO, resposta.getBody().status());
    }

    @Test
    void statusDeIdInexistenteRetorna404() {
        when(downloadService.buscar("abc")).thenReturn(Optional.empty());

        ApiException erro = assertThrows(ApiException.class,
                () -> controller.status("abc"));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatus());
    }

    @Test
    void statusDeIdExistenteDevolveOAndamento() {
        DownloadTask tarefa = new DownloadTask("id-1");
        tarefa.atualizarProgresso(30);
        when(downloadService.buscar("id-1")).thenReturn(Optional.of(tarefa));

        DownloadStatusResponse resposta = controller.status("id-1");

        assertEquals(30, resposta.progresso());
    }

    @Test
    void arquivoAntesDeConcluirRetorna409() {
        when(downloadService.buscar("id-1"))
                .thenReturn(Optional.of(new DownloadTask("id-1")));

        ApiException erro = assertThrows(ApiException.class,
                () -> controller.arquivo("id-1"));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
    }
    @Test
    void arquivoJaRemovidoDoServidorRetorna410() {
        DownloadTask tarefa = new DownloadTask("id-1");
        tarefa.concluir(Path.of("arquivo-que-nao-existe.mp4"));
        when(downloadService.buscar("id-1")).thenReturn(Optional.of(tarefa));

        ApiException erro = assertThrows(ApiException.class,
                () -> controller.arquivo("id-1"));

        assertEquals(HttpStatus.GONE, erro.getStatus());
    }

}