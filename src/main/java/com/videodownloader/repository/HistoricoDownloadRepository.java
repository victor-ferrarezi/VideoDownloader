package com.videodownloader.repository;

import com.videodownloader.model.HistoricoDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoDownloadRepository
        extends JpaRepository<HistoricoDownload, Long> {

    // O Spring monta o SQL só pelo nome do metodo
    // os 20 primeiros, ordenados por iniciado do mais novo ao mais antigo
    List<HistoricoDownload> findTop20ByOrderByIniciadoEmDesc();
}