package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.model.AvMediaResolveResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AvMediaResolveServiceImplTest {

    private final AvMediaResolveServiceImpl service = new AvMediaResolveServiceImpl();

    @Test
    void parsesBilibiliIdentifiersAndPlayInfo() throws Exception {
        assertThat((String) invoke("extractBvid", new Class<?>[]{String.class}, "https://www.bilibili.com/video/BV1xx411c7mD?p=2")).isEqualTo("BV1xx411c7mD");
        assertThat((Integer) invoke("extractPage", new Class<?>[]{String.class}, "https://www.bilibili.com/video/BV1xx411c7mD?p=2")).isEqualTo(2);

        String html = "<html><head></head><body><script>window.__playinfo__={\"data\":{\"durl\":[{\"url\":\"https://cdn.example.com/video.mp4\"}]}};</script></body></html>";
        JSONObject playInfo = (JSONObject) invoke("parsePlayInfo", new Class<?>[]{String.class}, html);
        assertThat(playInfo).isNotNull();
        assertThat((String) invoke("extractPlayableUrl", new Class<?>[]{JSONObject.class}, playInfo)).isEqualTo("https://cdn.example.com/video.mp4");
    }

    @Test
    void resolvesDirectMediaWithoutNetwork() {
        AvMediaResolveResult result = service.resolve("https://cdn.example.com/movie.mp4", "片名");
        assertThat(result.getMediaUrl()).isEqualTo("https://cdn.example.com/movie.mp4");
        assertThat(result.getSourceType()).isEqualTo("direct");
        assertThat(result.getTitle()).isEqualTo("片名");
        assertThat(result.getContentType()).isEqualTo("video/mp4");
    }

    @Test
    void guessesCommonAudioContentTypes() throws Exception {
        assertThat((String) invoke("guessContentType", new Class<?>[]{String.class}, "https://cdn.example.com/song.mp3")).isEqualTo("audio/mpeg");
        assertThat((String) invoke("guessContentType", new Class<?>[]{String.class}, "https://cdn.example.com/voice.wav")).isEqualTo("audio/wav");
        assertThat((String) invoke("guessContentType", new Class<?>[]{String.class}, "https://cdn.example.com/movie.mp4")).isEqualTo("video/mp4");
        assertThat((String) invoke("guessSourceType", new Class<?>[]{String.class}, "https://cdn.example.com/song.mp3")).isEqualTo("direct");
    }

    private Object invoke(String method, Class<?>[] types, Object... args) throws Exception {
        Method declaredMethod = AvMediaResolveServiceImpl.class.getDeclaredMethod(method, types);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(service, args);
    }
}
