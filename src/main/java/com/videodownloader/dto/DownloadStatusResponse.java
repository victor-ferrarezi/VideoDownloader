package com.videodownloader.dto;

import com.videodownloader.model.DownloadStatus;
import com.videodownloader.model.DownloadTask;

public record DownloadStatusResponse(
        String id,
        DownloadStatus status,
        double progresso,
        String mensagem) {

    public static DownloadStatusResponse from(DownloadTask tarefa) {
        return new DownloadStatusResponse(
                tarefa.getId(),
                tarefa.getStatus(),
                tarefa.getProgresso(),
                tarefa.getMensagem());
    }
}