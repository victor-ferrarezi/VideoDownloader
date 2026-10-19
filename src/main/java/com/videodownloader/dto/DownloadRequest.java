package com.videodownloader.dto;

// formatoId pode ser vazio: nesse caso baixa a melhor qualidade.
public record DownloadRequest(String url, String formatoId, boolean temAudio) {}