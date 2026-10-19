package com.videodownloader.dto;

import com.videodownloader.model.DownloadStatus;
import com.videodownloader.model.HistoricoDownload;

public record HistoricoResponse(
        Long id,
        String url,
        String formato,
        DownloadStatus status,
        String mensagem,
        String nomeArquivo,
        String iniciadoEm,
        String finalizadoEm) {

    public static HistoricoResponse from(HistoricoDownload h) {
        return new HistoricoResponse(
                h.getId(), h.getUrl(), h.getFormato(), h.getStatus(),
                h.getMensagem(), h.getNomeArquivo(),
                h.getIniciadoEm().toString(),
                h.getFinalizadoEm() != null ? h.getFinalizadoEm().toString() : null);
    }
}