package com.videodownloader.model;

public class VideoFormat {
    private final String id;
    private final String extensao;
    private final String resolucao;
    private final String descricao;
    private final boolean temAudio;

    public VideoFormat(
            String id,
            String extensao,
            String resolucao,
            String descricao,
            boolean temAudio) {

        this.id = id;
        this.extensao = extensao;
        this.resolucao = resolucao;
        this.descricao = descricao;
        this.temAudio = temAudio;
    }

    public String getId() {
        return id;
    }
    public String getExtensao() {
        return extensao;
    }
    public String getDescricao() {
        return descricao;
    }
    public String getResolucao() {
        return resolucao;
    }
    public boolean isTemAudio() {
        return temAudio;
    }

    @Override
    public String toString() {
        return id + " | " + resolucao + " | " + extensao
                + " | " + descricao + " | " + temAudio;
    }
}