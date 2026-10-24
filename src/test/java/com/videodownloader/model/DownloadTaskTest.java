package com.videodownloader.model;

import com.videodownloader.model.DownloadStatus;
import com.videodownloader.model.DownloadTask;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadTaskTest {

    @Test
    void tarefaNovaComecaEmAndamentoComZeroPorcento() {
        DownloadTask tarefa = new DownloadTask("abc");

        assertEquals(DownloadStatus.EM_ANDAMENTO, tarefa.getStatus());
        assertEquals(0, tarefa.getProgresso());
    }

    @Test
    void atualizaProgresso() {
        DownloadTask tarefa = new DownloadTask("abc");
        tarefa.atualizarProgresso(45.5);

        assertEquals(45.5, tarefa.getProgresso());
        assertEquals(DownloadStatus.EM_ANDAMENTO, tarefa.getStatus());
    }

    @Test
    void concluirGuardaArquivoEZeraPendencias() {
        DownloadTask tarefa = new DownloadTask("abc");
        Path arquivo = Path.of("video.mp4");

        tarefa.concluir(arquivo);

        assertEquals(DownloadStatus.CONCLUIDO, tarefa.getStatus());
        assertEquals(100, tarefa.getProgresso());
        assertEquals(arquivo, tarefa.getArquivo());
    }

    @Test
    void falharGuardaAMensagemDeErro() {
        DownloadTask tarefa = new DownloadTask("abc");
        tarefa.falhar("Vídeo indisponível");

        assertEquals(DownloadStatus.ERRO, tarefa.getStatus());
        assertEquals("Vídeo indisponível", tarefa.getMensagem());
    }
}