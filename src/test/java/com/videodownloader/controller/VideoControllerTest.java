package controller;

import com.videodownloader.controller.VideoController;
import com.videodownloader.dto.UrlRequest;
import com.videodownloader.exception.ApiException;
import com.videodownloader.model.VideoFormat;
import com.videodownloader.model.VideoInfo;
import com.videodownloader.service.YtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

    private static final String URL_VALIDA = "https://youtu.be/abc123";

    @Mock
    private YtService ytService;

    @InjectMocks
    private VideoController controller;

    @Test
    void infoComUrlInvalidaRetorna400ENaoChamaOService() {
        ApiException erro = assertThrows(ApiException.class,
                () -> controller.informacoes(new UrlRequest("https://google.com")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        verifyNoInteractions(ytService);
    }

    @Test
    void infoComUrlVaziaRetorna400() {
        ApiException erro = assertThrows(ApiException.class,
                () -> controller.informacoes(new UrlRequest("")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
    }

    @Test
    void infoComUrlValidaDevolveOsDadosDoVideo() throws Exception {
        VideoInfo esperado = new VideoInfo("Titulo", "Canal", 171);
        when(ytService.buscarInformacoes(URL_VALIDA)).thenReturn(esperado);

        VideoInfo resultado = controller.informacoes(new UrlRequest(URL_VALIDA));

        assertEquals("Titulo", resultado.getTitulo());
        assertEquals("02:51", resultado.getDuracaoFormatada());
    }

    @Test
    void infoRemoveEspacosAntesDeConsultar() throws Exception {
        when(ytService.buscarInformacoes(URL_VALIDA))
                .thenReturn(new VideoInfo("T", "C", 10));

        controller.informacoes(new UrlRequest("  " + URL_VALIDA + "  "));

        verify(ytService).buscarInformacoes(URL_VALIDA);
    }

    @Test
    void formatosComUrlInvalidaRetorna400() {
        ApiException erro = assertThrows(ApiException.class,
                () -> controller.formatos(new UrlRequest("abc")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        verifyNoInteractions(ytService);
    }

    @Test
    void formatosComUrlValidaDevolveALista() throws Exception {
        VideoFormat formato = new VideoFormat("137", "mp4", "1920x1080", "1080p", false);
        when(ytService.buscarFormatos(URL_VALIDA)).thenReturn(List.of(formato));

        List<VideoFormat> resultado = controller.formatos(new UrlRequest(URL_VALIDA));

        assertEquals(1, resultado.size());
        assertEquals("137", resultado.get(0).getId());
    }
}
