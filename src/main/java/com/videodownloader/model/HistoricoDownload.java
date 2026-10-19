package com.videodownloader.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// registro do histórico cada download aqui vai virar uma linha
@Entity
@Table(name = "historico_download")
public class HistoricoDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String url;

    private String formato;

    @Enumerated(EnumType.STRING)   // grava "CONCLUIDO" em vez de um número
    @Column(nullable = false)
    private DownloadStatus status;

    @Column(length = 1000)
    private String mensagem;

    private String nomeArquivo;

    @Column(nullable = false)
    private Instant iniciadoEm;

    private Instant finalizadoEm;

    // JPA precisa um construtor sem argumentos
    protected HistoricoDownload() {
    }

    public HistoricoDownload(String url, String formato, DownloadStatus status,
                             String mensagem, String nomeArquivo,
                             Instant iniciadoEm, Instant finalizadoEm) {
        this.url = url;
        this.formato = formato;
        this.status = status;
        this.mensagem = mensagem;
        this.nomeArquivo = nomeArquivo;
        this.iniciadoEm = iniciadoEm;
        this.finalizadoEm = finalizadoEm;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public String getFormato() { return formato; }
    public DownloadStatus getStatus() { return status; }
    public String getMensagem() { return mensagem; }
    public String getNomeArquivo() { return nomeArquivo; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public Instant getFinalizadoEm() { return finalizadoEm; }
}