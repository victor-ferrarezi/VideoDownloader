package com.videodownloader;

public class VideoInfo {

    private String titulo;
    private String canal;
    private long duracao;

    /// Getters

    public VideoInfo(String titulo, String canal, long duracao) {
        this.titulo = titulo;
        this.canal = canal;
        this.duracao = duracao;
    }
    public String getTitulo() {
        return titulo;
    }
    public String getCanal(){
        return canal;
    }
    public long getDuracao() {
        return duracao;
    }

    /// Formatar o tempo

    public String getDuracaoFormatada() {
        long horas = duracao / 3600;
        long minutos = (duracao % 3600) / 60;
        long segundos = duracao % 60;

        if (horas > 0) {
            return String.format("%02d:%02d:%02d",
                    horas, minutos, segundos);
        }
        return String.format("%02d:%02d",
                minutos, segundos);
    }
}
