package com.videodownloader.model;

import java.nio.file.Path;

/**
 * Representa um download em andamento (ou finalizado).
 * Os campos são "volatile" porque duas threads mexem neles:
 * a que executa o yt-dlp (escreve) e a que atende o navegador (lê).
 */
public class DownloadTask {

    private final String id;
    private volatile DownloadStatus status = DownloadStatus.EM_ANDAMENTO;
    private volatile double progresso = 0;
    private volatile String mensagem = "Download iniciado...";
    private volatile Path arquivo;

    public DownloadTask(String id) {
        this.id = id;
    }

    public void atualizarProgresso(double progresso) {
        this.progresso = progresso;
    }

    public void concluir(Path arquivo) {
        this.arquivo = arquivo;          // grava o arquivo ANTES do status
        this.progresso = 100;
        this.mensagem = "Download concluído!";
        this.status = DownloadStatus.CONCLUIDO;
    }

    public void falhar(String mensagem) {
        this.mensagem = mensagem;
        this.status = DownloadStatus.ERRO;
    }

    public String getId() { return id; }
    public DownloadStatus getStatus() { return status; }
    public double getProgresso() { return progresso; }
    public String getMensagem() { return mensagem; }
    public Path getArquivo() { return arquivo; }
}