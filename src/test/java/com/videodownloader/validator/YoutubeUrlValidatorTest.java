package com.videodownloader.validator;

import com.videodownloader.validator.YoutubeUrlValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeUrlValidatorTest {

    // @ParameterizedTest roda o mesmo teste uma vez para cada valor da lista
    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=jNQXAC9IJRE",
            "https://youtube.com/watch?v=jNQXAC9IJRE",
            "https://m.youtube.com/watch?v=jNQXAC9IJRE",
            "https://youtu.be/Mmw-PpWhKFg?list=RDMmw-PpWhKFg",
            "http://www.youtube.com/watch?v=abc",
            "  https://youtu.be/abc  "
    })
    void aceitaUrlsValidas(String url) {
        assertTrue(YoutubeUrlValidator.isValid(url), "Deveria aceitar: " + url);
    }

    @ParameterizedTest
    @NullAndEmptySource   // testa null e ""
    @ValueSource(strings = {
            "   ",
            "abc",
            "youtube.com/watch?v=abc",              // sem protocolo
            "ftp://youtube.com/video",              // protocolo não permitido
            "https://google.com",                   // outro site
            "https://evilyoutube.com/watch?v=abc",  // domínio parecido
            "https://evilyoutu.be/abc",             // domínio parecido
            "https://www.youtube.com.evil.com/x",   // youtube só no começo
            "https://youtu.be/abc def"              // espaço no meio
    })
    void rejeitaUrlsInvalidas(String url) {
        assertFalse(YoutubeUrlValidator.isValid(url), "Deveria rejeitar: " + url);
    }
}