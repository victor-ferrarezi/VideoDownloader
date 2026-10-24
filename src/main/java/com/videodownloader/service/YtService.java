package com.videodownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.videodownloader.config.YtDlpProperties;
import com.videodownloader.model.VideoFormat;
import com.videodownloader.model.VideoInfo;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consulta o yt-dlp para obter informações e formatos de um vídeo.
 */
@Service
public class YtService {

    // Qualidades oferecidas ao usuário (o "lado menor" do vídeo: 480p a 4K)
    private static final int QUALIDADE_MINIMA = 480;
    private static final int QUALIDADE_MAXIMA = 2160;

    /**
     * Um formato candidato + os dados usados para desempatar quando
     * existem vários formatos com a mesma resolução.
     */
    private record Candidato(VideoFormat formato, int prioridadeCodec, double bitrate) { }

    private final YtDlpProperties yt;
    private final ObjectMapper mapper = new ObjectMapper();

    // O Spring entrega o YtDlpProperties aqui automaticamente
    public YtService(YtDlpProperties yt) {
        this.yt = yt;
    }

    public VideoInfo buscarInformacoes(String url)
            throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder(yt.comandoCom(
                "--no-warnings",
                "--no-playlist",
                "--skip-download",
                "--print", "%(title)s",
                "--print", "%(channel)s",
                "--print", "%(duration)s",
                "--",
                url
        ));

        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder resultado = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                resultado.append(line).append("\n");
            }
        }

        int codigoSaida = process.waitFor();
        if (codigoSaida != 0) {
            throw new IOException(
                    "O yt-dlp não conseguiu consultar o vídeo:\n" + resultado);
        }

        String[] dados = resultado.toString().split("\n", -1);

        if (dados.length < 3
                || dados[0].isBlank()
                || dados[2].isBlank()
                || dados[2].equals("NA")) {
            throw new IOException(
                    "Não foi possível obter todos os dados do vídeo.");
        }

        String titulo = dados[0].strip();
        String canal = dados[1].strip();

        long duracao;
        try {
            duracao = Long.parseLong(dados[2].strip());
        } catch (NumberFormatException e) {
            throw new IOException("Duração do vídeo inválida", e);
        }

        return new VideoInfo(titulo, canal, duracao);
    }

    /**
     * Lista os formatos de vídeo entre 480p e 4K, com UMA opção por
     * resolução, da maior para a menor.
     */
    public List<VideoFormat> buscarFormatos(String url)
            throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder(yt.comandoCom(
                "--no-warnings",
                "--no-playlist",
                "--skip-download",
                "--dump-single-json",
                "--",
                url
        ));

        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();

        StringBuilder json = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        int codigoSaida = process.waitFor();
        if (codigoSaida != 0) {
            throw new IOException("Não foi possível consultar os formatos.");
        }

        JsonNode raiz = mapper.readTree(json.toString());
        JsonNode formatosJson = raiz.path("formats");

        // chave = qualidade (480, 720, 1080...), valor = melhor formato dela.
        // O TreeMap com reverseOrder já mantém tudo do maior para o menor.
        Map<Integer, Candidato> melhorPorQualidade =
                new TreeMap<>(Comparator.reverseOrder());

        for (JsonNode formato : formatosJson) {

            String id = formato.path("format_id").asText("");
            String extensao = formato.path("ext").asText("");
            String codecVideo = formato.path("vcodec").asText("none");
            String codecAudio = formato.path("acodec").asText("none");

            // Ignora formatos que não possuem vídeo (só áudio, miniaturas...)
            if (id.isBlank() || codecVideo.equals("none")) {
                continue;
            }

            int largura = formato.path("width").asInt(0);
            int altura = formato.path("height").asInt(0);
            if (largura == 0 || altura == 0) {
                continue;
            }

            // O lado menor é a "qualidade". Assim um vídeo em pé (Shorts,
            // 1080x1920) também vira 1080p.
            int qualidade = Math.min(largura, altura);
            if (qualidade < QUALIDADE_MINIMA || qualidade > QUALIDADE_MAXIMA) {
                continue;
            }

            // Prefere o códec mais compatível: avc1, depois vp9, depois o resto
            int prioridadeCodec = codecVideo.startsWith("avc1") ? 0
                    : codecVideo.startsWith("vp09") || codecVideo.startsWith("vp9") ? 1
                    : 2;

            double bitrate = formato.path("tbr").asDouble(0);
            double fps = formato.path("fps").asDouble(0);
            boolean temAudio = !codecAudio.equals("none");

            VideoFormat novo = new VideoFormat(
                    id,
                    extensao,
                    rotulo(qualidade),
                    fps > 30 ? Math.round(fps) + " fps" : "",
                    temAudio
            );

            // Se já existe um formato dessa qualidade, fica o melhor dos dois
            melhorPorQualidade.merge(qualidade,
                    new Candidato(novo, prioridadeCodec, bitrate),
                    YtService::escolherMelhor);
        }

        return melhorPorQualidade.values().stream()
                .map(Candidato::formato)
                .toList();
    }

    /** Menor prioridade de códec vence; no empate, o maior bitrate. */
    private static Candidato escolherMelhor(Candidato a, Candidato b) {
        if (a.prioridadeCodec() != b.prioridadeCodec()) {
            return a.prioridadeCodec() < b.prioridadeCodec() ? a : b;
        }
        return a.bitrate() >= b.bitrate() ? a : b;
    }

    private static String rotulo(int qualidade) {
        return qualidade >= 2160 ? qualidade + "p (4K)" : qualidade + "p";
    }
}