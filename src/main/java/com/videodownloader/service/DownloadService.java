package com.videodownloader.service;

import com.videodownloader.config.YtDlpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

//Executa downloads de vídeo usando o yt-dlp
@Service
public class DownloadService {

    private static final Logger log =
            LoggerFactory.getLogger(DownloadService.class);

    private final YtDlpProperties yt;

    public DownloadService(YtDlpProperties yt) {
        this.yt = yt;
    }

    //Baixa automaticamente a melhor qualidade disponível
    public void baixarMelhor(String url)
            throws IOException, InterruptedException {
        executar(url, "bestvideo+bestaudio/best");
    }

    public void baixarFormato(String url, String formatoId, boolean temAudio)
            throws IOException, InterruptedException {

        String formato = temAudio ? formatoId : formatoId + "+bestaudio/best";
        executar(url, formato);
    }

    private void executar(String url, String formato)
            throws IOException, InterruptedException {

        Path pasta = Path.of(yt.downloadsDir());
        Files.createDirectories(pasta);

        ProcessBuilder builder = new ProcessBuilder(yt.comandoCom(
                "--newline",
                "--no-playlist",
                "-f", formato,
                "--merge-output-format", "mp4",
                "-o", pasta.resolve("%(title)s.%(ext)s").toString(),
                "--",
                url
        ));

        // junta erros e saída normal para capturar tudo
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }

        int codigoSaida = process.waitFor();
        if (codigoSaida != 0) {
            throw new IOException(
                    "O download falhou. Código: " + codigoSaida);
        }

        log.info("Download concluído em {}", pasta.toAbsolutePath());
    }
}