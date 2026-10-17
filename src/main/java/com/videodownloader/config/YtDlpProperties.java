package com.videodownloader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.ytdlp")
public record YtDlpProperties(List<String> command, String downloadsDir) {

    public List<String> comandoCom(String... argumentos) {
        List<String> comando = new ArrayList<>(command);
        comando.addAll(Arrays.asList(argumentos));
        return comando;
    }
}