package com.videodownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videodownloader.VideoFormat;
import com.videodownloader.VideoInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ytService {
    public static VideoInfo buscarInformacoes(String url)
                throws IOException, InterruptedException{

            ProcessBuilder builder = new ProcessBuilder(
            "py",
                    "-m",
                    "yt_dlp",
                    "--no-warnings",
                    "--no-playlist",
                    "--skip-download",
                    "--print", "%(title)s",
                    "--print", "%(channel)s",
                    "--print", "%(duration)s",
                    "--",
                    url
            );

            builder.redirectErrorStream(true);

            Process process = builder.start();

            StringBuilder resultado = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream(),
                            StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine())!= null) {
                    resultado.append(line).append("\n");
                }

            }

            int codigoSaida = process.waitFor();
            if(codigoSaida != 0){
                throw new IOException(
                        "o yt-dlp não conseguiu consultar o vídeo:\n"
                        +resultado
                );
            }

            String[] dados = resultado.toString().split("\n", -1);

            if (dados.length < 3
                || dados[0].isBlank()
                || dados[2].isBlank()
                || dados[2].equals("NA")){

                throw new IOException(
                        "Não foi possível obter todos os dados do vídeo."
                );
            }

            String titulo = dados[0].strip();
            String canal = dados[1].strip();

            long duracao;

            try{
                duracao = Long.parseLong(dados[2].strip());
            } catch(NumberFormatException e) {
                throw new IOException("Duração do vídeo invalida", e);
            }
            return new VideoInfo(titulo, canal, duracao);
            }


    public static List<VideoFormat> buscarFormatos(String url)
            throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder(
                "py",
                "-m",
                "yt_dlp",
                "--no-warnings",
                "--no-playlist",
                "--skip-download",
                "--dump-single-json",
                "--",
                url
        );

        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = builder.start();

        StringBuilder json = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        int codigoSaida = process.waitFor();

        if (codigoSaida != 0) {
            throw new IOException(
                    "Não foi possível consultar os formatos."
            );
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode raiz = mapper.readTree(json.toString());
        JsonNode formatosJson = raiz.path("formats");

        List<VideoFormat> formatos = new ArrayList<>();

        for (JsonNode formato : formatosJson) {

            String id = formato.path("format_id").asText("");
            String extensao = formato.path("ext").asText("");
            String resolucao = formato.path("resolution")
                    .asText("Desconhecida");
            String descricao = formato.path("format_note").asText("");

            String codecVideo = formato.path("vcodec").asText("none");
            String codecAudio = formato.path("acodec").asText("none");

            // Ignora formatos que não possuem vídeo
            if (id.isBlank() || codecVideo.equals("none")) {
                continue;
            }

            // Verifica se o formato já possui áudio
            boolean temAudio = !codecAudio.equals("none");

            VideoFormat videoFormat = new VideoFormat(
                    id,
                    extensao,
                    resolucao,
                    descricao,
                    temAudio
            );

            formatos.add(videoFormat);
        }

        System.out.println("5. Formatos encontrados: " + formatos.size());
        return formatos;
    }
}
