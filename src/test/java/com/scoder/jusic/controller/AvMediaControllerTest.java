package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.AvMediaResolveResult;
import com.scoder.jusic.service.AvMediaResolveService;
import com.scoder.jusic.util.AvMediaStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AvMediaControllerTest {

    private Path createdFile;

    @AfterEach
    void tearDown() throws Exception {
        if (createdFile != null) {
            Files.deleteIfExists(createdFile);
            createdFile = null;
        }
    }

    @Test
    void uploadsLargeVideoFilesToMediaDirectory() {
        AvMediaController controller = new AvMediaController();
        ReflectionTestUtils.setField(controller, "avMediaResolveService", new NoopResolver());

        byte[] payload = new byte[5 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big-video.mp4", "video/mp4", payload);

        Response<Map<String, String>> response = controller.upload(file);
        assertThat(response.getCode()).isEqualTo("20000");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().get("url")).startsWith("/av/media/files/");

        createdFile = AvMediaStorage.mediaDir().resolve(response.getData().get("url").substring("/av/media/files/".length())).normalize();
        assertThat(Files.exists(createdFile)).isTrue();
        assertThat(createdFile.toFile().length()).isEqualTo(payload.length);
    }

    @Test
    void acceptsCommonAudioAndVideoFormats() throws Exception {
        AvMediaController controller = new AvMediaController();
        ReflectionTestUtils.setField(controller, "avMediaResolveService", new NoopResolver());

        byte[] payload = new byte[]{0x00, 0x01, 0x02, 0x03};
        for (Object[] sample : Arrays.asList(
                new Object[]{"song.mp3", "audio/mpeg"},
                new Object[]{"voice.wav", "audio/wav"},
                new Object[]{"movie.mp4", "video/mp4"}
        )) {
            String originalName = (String) sample[0];
            String contentType = (String) sample[1];
            MockMultipartFile file = new MockMultipartFile("file", originalName, contentType, payload);

            Response<Map<String, String>> response = controller.upload(file);
            assertThat(response.getCode()).isEqualTo("20000");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().get("contentType")).isEqualTo(contentType);
            assertThat(response.getData().get("url")).endsWith(originalName.substring(originalName.lastIndexOf('.')));

            Path created = AvMediaStorage.mediaDir().resolve(response.getData().get("url").substring("/av/media/files/".length())).normalize();
            assertThat(Files.exists(created)).isTrue();
            assertThat(created.toFile().length()).isEqualTo(payload.length);
            Files.deleteIfExists(created);
        }
    }

    private static class NoopResolver implements AvMediaResolveService {
        @Override
        public AvMediaResolveResult resolve(String url, String title) {
            return null;
        }
    }
}
