package com.videodownloader.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;



public class DownloadService {
    public static void baixarVideo(String url)
            throws IOException, InterruptedException {


        //pasta para salvar
        Path Pasta = Path.of("Downloads");
        Files.createDirectories(Pasta);

        ProcessBuilder Builder = new ProcessBuilder(
                "py",
                "-m",
                "yt_dlp",
                "--newline",
                "--no-playlist",
                "-f",
                "bestvideo+bestaudio/best",
                "-o",
                Pasta.resolve("%(title)s.%(ext)s").toString(),
                "--",
                url
        );

        // captura mensagens e progresso do yt-dlp

        Builder.redirectErrorStream(true);

        Process process = Builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int codigoSaida = process.waitFor();

            if (codigoSaida != 0) {
                throw new IOException(
                        "O download do formato escolhido falhou. Código: " + codigoSaida);
            }
            System.out.println("O download concluído!");
            System.out.println("Pasta de destino"
                    + Pasta.toAbsolutePath());

        }
    }


    //esse aqui é para escolher o formato antes de baixar
    // diferente do metodo de cima que so baixa automatico
    public static void baixarFormato(String url, String formatoId, boolean temAudio) throws IOException, InterruptedException {

        Path Pasta = Path.of("Downloads");
        Files.createDirectories(Pasta);

        //caso tenha audio disponivel, combina com o melhor
        // se n tiver tbm mostra
        String formatoEscolhido = temAudio ? formatoId : formatoId + "+bestsautios/best";

        ProcessBuilder builder = new ProcessBuilder(
                "py",
                "-m",
                "yt_dlp",
                "--newline",
                "--no-playlist",
                "-f",
                formatoEscolhido,
                "--merge-output-format",
                "mp4",
                "-o",
                Pasta.resolve("%(title)s.%(ext)s").toString(),
                "--",
                url
        );
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(),
                        StandardCharsets.UTF_8))){
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        int codigoSaida = process.waitFor();
        if (codigoSaida != 0) {
            throw new IOException(
                    "O download do formato escolhido falhou. Código: " + codigoSaida);
        }
        System.out.println("O download concluído!");
        System.out.println("Pasta de destino"
                + Pasta.toAbsolutePath());

    }
}

