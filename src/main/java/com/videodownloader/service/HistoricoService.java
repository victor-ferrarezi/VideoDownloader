package com.videodownloader.service;

import com.videodownloader.model.DownloadTask;
import com.videodownloader.model.HistoricoDownload;
import com.videodownloader.repository.HistoricoDownloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
  regras de negócio do histórico. quem precisa gravar ou consultar
  o histórico passa por aqui, nunca direto pelo repositório.
 */
@Service
public class HistoricoService {

    private static final Logger log =
            LoggerFactory.getLogger(HistoricoService.class);

    private final HistoricoDownloadRepository repository;

    public HistoricoService(HistoricoDownloadRepository repository) {
        this.repository = repository;
    }

    /*
      grava o resultado de um download. Se o banco falhar, apenas registra
      no log: o download do usuário não deve ser prejudicado por isso.
     */
    public void registrar(String url, String formato,
                          DownloadTask tarefa, Instant inicio) {
        try {
            String nomeArquivo = tarefa.getArquivo() != null
                    ? tarefa.getArquivo().getFileName().toString()
                    : null;

            String mensagem = tarefa.getMensagem();
            if (mensagem != null && mensagem.length() > 1000) {
                mensagem = mensagem.substring(0, 1000);
            }

            repository.save(new HistoricoDownload(
                    url, formato, tarefa.getStatus(), mensagem,
                    nomeArquivo, inicio, Instant.now()));

        } catch (RuntimeException e) {
            log.warn("Não foi possível gravar o histórico", e);
        }
    }

    public List<HistoricoDownload> listarRecentes() {
        return repository.findTop20ByOrderByIniciadoEmDesc();
    }
}