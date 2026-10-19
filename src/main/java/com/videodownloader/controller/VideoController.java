package com.videodownloader.controller;

import com.videodownloader.dto.UrlRequest;
import com.videodownloader.exception.ApiException;
import com.videodownloader.model.VideoFormat;
import com.videodownloader.model.VideoInfo;
import com.videodownloader.service.YtService;
import com.videodownloader.validator.YoutubeUrlValidator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final YtService ytService;

    public VideoController(YtService ytService) {
        this.ytService = ytService;
    }

    @PostMapping("/info")
    public VideoInfo informacoes(@RequestBody UrlRequest request)
            throws IOException, InterruptedException {
        return ytService.buscarInformacoes(validar(request.url()));
    }

    @PostMapping("/formats")
    public List<VideoFormat> formatos(@RequestBody UrlRequest request)
            throws IOException, InterruptedException {
        return ytService.buscarFormatos(validar(request.url()));
    }

    private String validar(String url) {
        if (!YoutubeUrlValidator.isValid(url)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "URL inválida. Informe um link do YouTube "
                            + "(youtube.com ou youtu.be).");
        }
        return url.strip();
    }
}