package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.AvMediaResolveRequest;
import com.scoder.jusic.model.AvMediaResolveResult;
import com.scoder.jusic.util.AvMediaStorage;
import com.scoder.jusic.service.AvMediaResolveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author H
 */
@Controller
public class AvMediaController {

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "wav", "flac", "aac", "m4a", "ogg",
            "mp4", "webm", "mov", "mkv"
    ));

    @Autowired
    private AvMediaResolveService avMediaResolveService;

    @PostMapping("/av/media/upload")
    @ResponseBody
    public Response<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Response.failure((Map<String, String>) null, "文件不能为空");
        }
        String contentType = this.safeTrim(file.getContentType());
        String originalFileName = this.safeName(file.getOriginalFilename());
        if (!this.isSupportedMedia(contentType, originalFileName)) {
            return Response.failure((Map<String, String>) null, "仅支持音频或视频文件");
        }
        AvMediaStorage.ensureDirectory();
        String fileName = this.createFileName(originalFileName, contentType);
        Path target = AvMediaStorage.mediaDir().resolve(fileName).normalize();
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            return Response.failure((Map<String, String>) null, "文件上传失败");
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("name", originalFileName);
        data.put("contentType", this.normalizeContentType(contentType, fileName));
        data.put("url", AvMediaStorage.buildAccessPath(fileName));
        return Response.success(data, "上传成功");
    }

    @PostMapping("/av/media/resolve")
    @ResponseBody
    public Response<AvMediaResolveResult> resolve(@RequestBody AvMediaResolveRequest request) {
        try {
            AvMediaResolveResult result = avMediaResolveService.resolve(
                    request == null ? null : request.getUrl(),
                    request == null ? null : request.getTitle()
            );
            return Response.success(result, "解析成功");
        } catch (IllegalArgumentException e) {
            return Response.failure((AvMediaResolveResult) null, e.getMessage());
        }
    }

    @GetMapping("/av/media/proxy")
    public void proxy(@RequestParam("url") String targetUrl, HttpServletRequest req, HttpServletResponse resp) {
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            resp.setStatus(400);
            return;
        }
        try {
            URL url = new URL(targetUrl);
            String host = url.getHost();
            if (!host.contains("bilivideo") && !host.contains("bilibili") && !host.contains("akamaized")) {
                resp.setStatus(403);
                return;
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.bilibili.com");
            conn.setRequestProperty("Origin", "https://www.bilibili.com");

            String rangeHeader = req.getHeader("Range");
            if (rangeHeader != null) {
                conn.setRequestProperty("Range", rangeHeader);
            }

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.connect();

            int status = conn.getResponseCode();
            resp.setStatus(status);
            String ct = conn.getContentType();
            if (ct != null) resp.setContentType(ct);
            long cl = conn.getContentLengthLong();
            if (cl >= 0) resp.setContentLengthLong(cl);
            String cr = conn.getHeaderField("Content-Range");
            if (cr != null) resp.setHeader("Content-Range", cr);
            resp.setHeader("Accept-Ranges", "bytes");

            try (InputStream in = conn.getInputStream(); OutputStream out = resp.getOutputStream()) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
        } catch (Exception e) {
            resp.setStatus(502);
        }
    }

    private String createFileName(String originalFileName, String contentType) {
        return UUID.randomUUID().toString().replace("-", "") + this.getExtension(originalFileName, contentType);
    }

    private String getExtension(String originalFileName, String contentType) {
        String fileName = this.safeName(originalFileName);
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension != null && !"".equals(extension.trim())) {
            String safeExt = extension.trim().toLowerCase();
            if (this.isSafeExtension(safeExt)) {
                return "." + safeExt;
            }
        }
        if (this.isAudioContentType(contentType)) {
            return this.guessAudioExtension(contentType);
        }
        if (this.isVideoContentType(contentType)) {
            return this.guessVideoExtension(contentType);
        }
        return "";
    }

    private boolean isSafeExtension(String extension) {
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    private String guessAudioExtension(String contentType) {
        if (contentType.contains("wav")) {
            return ".wav";
        }
        if (contentType.contains("mpeg")) {
            return ".mp3";
        }
        if (contentType.contains("flac")) {
            return ".flac";
        }
        if (contentType.contains("aac")) {
            return ".aac";
        }
        if (contentType.contains("m4a")) {
            return ".m4a";
        }
        if (contentType.contains("mp4")) {
            return ".m4a";
        }
        return ".mp3";
    }

    private String guessVideoExtension(String contentType) {
        if (contentType.contains("webm")) {
            return ".webm";
        }
        if (contentType.contains("quicktime") || contentType.contains("mov")) {
            return ".mov";
        }
        if (contentType.contains("ogg")) {
            return ".ogg";
        }
        if (contentType.contains("x-matroska") || contentType.contains("mkv")) {
            return ".mkv";
        }
        return ".mp4";
    }

    private boolean isSupportedMedia(String contentType, String originalFileName) {
        String extension = StringUtils.getFilenameExtension(this.safeName(originalFileName));
        if (extension != null && this.isSafeExtension(extension.trim().toLowerCase())) {
            return true;
        }
        return this.isAudioContentType(contentType) || this.isVideoContentType(contentType);
    }

    private boolean isAudioContentType(String contentType) {
        String current = this.safeTrim(contentType).toLowerCase();
        return current.startsWith("audio/");
    }

    private boolean isVideoContentType(String contentType) {
        String current = this.safeTrim(contentType).toLowerCase();
        return current.startsWith("video/");
    }

    private String normalizeContentType(String contentType, String fileName) {
        String current = this.safeTrim(contentType);
        if (!"".equals(current)) {
            return current;
        }
        String extension = StringUtils.getFilenameExtension(this.safeName(fileName));
        if (extension == null) {
            return "";
        }
        String safeExt = extension.trim().toLowerCase();
        if ("mp3".equals(safeExt)) {
            return "audio/mpeg";
        }
        if ("wav".equals(safeExt)) {
            return "audio/wav";
        }
        if ("m4a".equals(safeExt)) {
            return "audio/mp4";
        }
        if ("aac".equals(safeExt)) {
            return "audio/aac";
        }
        if ("flac".equals(safeExt)) {
            return "audio/flac";
        }
        if ("ogg".equals(safeExt)) {
            return "audio/ogg";
        }
        if ("webm".equals(safeExt)) {
            return "video/webm";
        }
        if ("mov".equals(safeExt)) {
            return "video/quicktime";
        }
        if ("mkv".equals(safeExt)) {
            return "video/x-matroska";
        }
        return "video/mp4";
    }

    private String safeName(String originalFileName) {
        String fileName = this.safeTrim(originalFileName);
        return "".equals(fileName) ? "media" : fileName;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
