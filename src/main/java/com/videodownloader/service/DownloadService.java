package com.videodownloader.service;

import com.videodownloader.config.YtDlpProperties;
import com.videodownloader.model.DownloadTask;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Executa downloads em segundo plano, guarda o andamento de cada um e registra o resultado no histórico (banco de dados).

@Service
public class DownloadService {

    private static final Logger log =
            LoggerFactory.getLogger(DownloadService.class);

    // linhas do yt-dlp como: "[download]  45.3% of 10.00MiB at ..."
    private static final Pattern PROGRESSO =
            Pattern.compile("\\[download\\]\\s+(\\d+(?:\\.\\d+)?)%");

    private final YtDlpProperties yt;
    private final HistoricoService historico;

    // mapa seguro para uso por várias threads ao mesmo tempo
    private final Map<String, DownloadTask> tarefas = new ConcurrentHashMap<>();

    // no máximo 2 downloads simultâneos; os outros esperam na fila
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public DownloadService(YtDlpProperties yt, HistoricoService historico) {
        this.yt = yt;
        this.historico = historico;
    }

    //  cria a tarefa, agenda a execução e retorna imediatamente
    public DownloadTask iniciar(String url, String formatoId, boolean temAudio) {
        String formato = escolherFormato(formatoId, temAudio);

        DownloadTask tarefa = new DownloadTask(UUID.randomUUID().toString());
        tarefas.put(tarefa.getId(), tarefa);

        executor.submit(() -> executar(tarefa, url, formato));
        return tarefa;
    }

    public Optional<DownloadTask> buscar(String id) {
        return Optional.ofNullable(tarefas.get(id));
    }

    // sem formato escolhido: melhor qualidade. formato sem áudio: junta (+)com o melhor áudio; "/best" é o plano B.

    private String escolherFormato(String formatoId, boolean temAudio) {
        if (formatoId == null || formatoId.isBlank()) {
            return "bestvideo+bestaudio/best";
        }
        return temAudio ? formatoId : formatoId + "+bestaudio/best";
    }

    private void executar(DownloadTask tarefa, String url, String formato) {
        Instant inicio = Instant.now();
        String ultimoErro = null;

        try {
            Path pasta = Path.of(yt.downloadsDir()).resolve(tarefa.getId());
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

            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);

                    Matcher m = PROGRESSO.matcher(line);
                    if (m.find()) {
                        tarefa.atualizarProgresso(Double.parseDouble(m.group(1)));
                    } else if (line.startsWith("ERROR")) {
                        ultimoErro = line;
                    }
                }
            }

            int codigoSaida = process.waitFor();
            if (codigoSaida != 0) {
                tarefa.falhar(ultimoErro != null
                        ? ultimoErro
                        : "O yt-dlp terminou com erro (código " + codigoSaida + ").");
                return;
            }

            tarefa.concluir(localizarArquivo(pasta));
            log.info("Download {} concluído", tarefa.getId());

        } catch (IOException e) {
            log.error("Falha no download {}", tarefa.getId(), e);
            tarefa.falhar("Erro ao executar o download: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tarefa.falhar("Download interrompido.");
        } catch (RuntimeException e) {
            log.error("Erro inesperado no download {}", tarefa.getId(), e);
            tarefa.falhar("Erro inesperado durante o download.");
        } finally {
            // roda SEMPRE, em qualquer desfecho, inclusive nos "return" acima
            historico.registrar(url, formato, tarefa, inicio);
        }
    }

    // acha o arquivo final na pasta da tarefa (ignora temporários)
    private Path localizarArquivo(Path pasta) throws IOException {
        try (Stream<Path> arquivos = Files.list(pasta)) {
            return arquivos
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".part"))
                    .max(Comparator.comparingLong(p -> p.toFile().length()))
                    .orElseThrow(() -> new IOException(
                            "O arquivo baixado não foi encontrado."));
        }
    }

    @PreDestroy
    public void encerrar() {
        executor.shutdownNow();
    }
}