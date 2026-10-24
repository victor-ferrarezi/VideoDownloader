package com.videodownloader.model;

import com.videodownloader.model.VideoInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VideoInfoTest {

    @ParameterizedTest
    @CsvSource({
            "0,00:00",
            "59,00:59",
            "171,02:51",
            "3599,59:59",
            "3600,01:00:00",
            "3661,01:01:01"
    })
    void formataDuracao(long segundos, String esperado) {
        VideoInfo info = new VideoInfo("titulo", "canal", segundos);
        assertEquals(esperado, info.getDuracaoFormatada());
    }
}