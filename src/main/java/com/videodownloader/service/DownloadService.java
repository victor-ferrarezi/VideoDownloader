package com.videodownloader.service;

import com.videodownloader.config.YtDlpProperties;
import com.videodownloader.model.DownloadTask;
import jakarta.annotation.PostConstruct;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Executa downloads em segundo plano, guarda o andamento de cada um e registra o resultado no histórico (banco de dados).
//
// O arquivo baixado fica numa pasta TEMPORÁRIA do servidor (Downloads/<id>/) só
// até ser entregue ao navegador. Depois de entregue, a cópia do servidor é
// apagada (veja descartar). Se ninguém buscar o arquivo, ele expira sozinho.

@Service
public class DownloadService {

    private static final Logger log =
            LoggerFactory.getLogger(DownloadService.class);

    // linhas do yt-dlp como: "[download]  45.3% of 10.00MiB at ..."
    private static final Pattern PROGRESSO =
            Pattern.compile("\\[download\\]\\s+(\\d+(?:\\.\\d+)?)%");

    // as pastas de tarefa têm o nome de um UUID; só estas são apagadas na limpeza
    private static final Pattern PASTA_DE_TAREFA = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    // depois deste tempo, o arquivo que ninguém buscou é apagado do servidor
    private static final long MINUTOS_PARA_EXPIRAR = 30;

    private final YtDlpProperties yt;
    private final HistoricoService historico;

    // mapa seguro para uso por várias threads ao mesmo tempo
    private final Map<String, DownloadTask> tarefas = new ConcurrentHashMap<>();

    // no máximo 2 downloads simultâneos; os outros esperam na fila
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    // agenda a limpeza dos arquivos que não foram buscados (thread "daemon":
    // não impede o programa de encerrar)
    private final ScheduledExecutorService limpeza =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "limpeza-downloads");
                t.setDaemon(true);
                return t;
            });

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
            return "bestvideo[height<=2160]+bestaudio/best[height<=2160]";
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
            agendarExpiracao(tarefa.getId());
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


    /* ---------- limpeza dos arquivos temporários do servidor ---------- */

    /**
     * Apaga a cópia do servidor. É chamado quando o arquivo já foi entregue
     * ao navegador (o usuário fica só com a cópia da pasta Downloads dele).
     */
    public void descartar(String id) {
        // só mexe em tarefas que o próprio sistema criou
        if (tarefas.containsKey(id)) {
            apagarPasta(id);
        }
    }

    // se ninguém buscar o arquivo, ele é apagado depois de um tempo
    private void agendarExpiracao(String id) {
        try {
            limpeza.schedule(() -> {
                apagarPasta(id);
                tarefas.remove(id);
            }, MINUTOS_PARA_EXPIRAR, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            log.warn("Não foi possível agendar a limpeza do download {}", id, e);
        }
    }

    private void apagarPasta(String id) {
        Path pasta = Path.of(yt.downloadsDir()).resolve(id);
        if (!Files.exists(pasta)) {
            return;
        }

        // apaga primeiro os arquivos, depois a pasta (ordem reversa)
        try (Stream<Path> arvore = Files.walk(pasta)) {
            arvore.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("Não foi possível apagar {}", p, e);
                }
            });
        } catch (IOException e) {
            log.warn("Não foi possível limpar a pasta {}", pasta, e);
        }
    }

    // ao iniciar, apaga sobras de execuções anteriores. Só mexe em pastas
    // com nome de UUID, então nunca apaga outros arquivos da pasta configurada.
    @PostConstruct
    void limparSobrasAntigas() {
        Path raiz = Path.of(yt.downloadsDir());
        if (!Files.isDirectory(raiz)) {
            return;
        }

        try (Stream<Path> itens = Files.list(raiz)) {
            itens.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(nome -> PASTA_DE_TAREFA.matcher(nome).matches())
                    .forEach(this::apagarPasta);
        } catch (IOException e) {
            log.warn("Não foi possível limpar downloads antigos", e);
        }
    }

    @PreDestroy
    public void encerrar() {
        executor.shutdownNow();
        limpeza.shutdownNow();
    }
}
