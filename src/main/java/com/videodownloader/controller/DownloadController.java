package com.videodownloader.controller;

import com.videodownloader.dto.DownloadRequest;
import com.videodownloader.dto.DownloadStatusResponse;
import com.videodownloader.exception.ApiException;
import com.videodownloader.model.DownloadStatus;
import com.videodownloader.model.DownloadTask;
import com.videodownloader.service.DownloadService;
import com.videodownloader.validator.YoutubeUrlValidator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/downloads")
public class DownloadController {

    // IDs de formato do yt-dlp são como "137", "18", "251-drc"
    private static final Pattern FORMATO_VALIDO =
            Pattern.compile("[A-Za-z0-9_.+\\-]{1,30}");

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @PostMapping
    public ResponseEntity<DownloadStatusResponse> iniciar(
            @RequestBody DownloadRequest request) {

        if (!YoutubeUrlValidator.isValid(request.url())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "URL inválida. Informe um link do YouTube "
                            + "(youtube.com ou youtu.be).");
        }

        String formatoId = request.formatoId();
        if (formatoId != null && !formatoId.isBlank()
                && !FORMATO_VALIDO.matcher(formatoId).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Formato inválido.");
        }

        DownloadTask tarefa = downloadService.iniciar(
                request.url().strip(), formatoId, request.temAudio());

        // 202 Accepted = "recebi e estou processando"
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(DownloadStatusResponse.from(tarefa));
    }

    @GetMapping("/{id}")
    public DownloadStatusResponse status(@PathVariable String id) {
        return DownloadStatusResponse.from(buscar(id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> arquivo(@PathVariable String id) {
        DownloadTask tarefa = buscar(id);

        if (tarefa.getStatus() != DownloadStatus.CONCLUIDO) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "O download ainda não foi concluído.");
        }

        Path arquivo = tarefa.getArquivo();

        // 410 Gone = "existiu, mas já foi removido"
        if (arquivo == null || !Files.exists(arquivo)) {
            throw new ApiException(HttpStatus.GONE,
                    "O arquivo não está mais disponível no servidor. "
                            + "Faça o download novamente.");
        }

        ContentDisposition disposicao = ContentDisposition.attachment()
                .filename(arquivo.getFileName().toString(), StandardCharsets.UTF_8)
                .build();

        // Quando o envio termina (o stream é fechado), a cópia do servidor é
        // apagada. O usuário fica apenas com o arquivo da pasta Downloads dele.
        Resource recurso = new FileSystemResource(arquivo) {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FilterInputStream(super.getInputStream()) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            downloadService.descartar(id);
                        }
                    }
                };
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(arquivo.toFile().length())
                .body(recurso);
    }

    private DownloadTask buscar(String id) {
        return downloadService.buscar(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND,
                        "Download não encontrado."));
    }
}