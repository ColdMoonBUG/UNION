package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.util.AvMediaStorage;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author H
 */
@Controller
public class AvMediaController {

    @PostMapping("/av/media/upload")
    @ResponseBody
    public Response<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Response.failure((Map<String, String>) null, "文件不能为空");
        }
        String contentType = this.safeTrim(file.getContentType());
        if (!contentType.startsWith("audio/") && !contentType.startsWith("video/")) {
            return Response.failure((Map<String, String>) null, "仅支持音频或视频文件");
        }
        AvMediaStorage.ensureDirectory();
        String fileName = this.createFileName(file.getOriginalFilename(), contentType);
        Path target = AvMediaStorage.mediaDir().resolve(fileName).normalize();
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            return Response.failure((Map<String, String>) null, "文件上传失败");
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("name", this.safeName(file.getOriginalFilename()));
        data.put("contentType", contentType);
        data.put("url", AvMediaStorage.buildAccessPath(fileName));
        return Response.success(data, "上传成功");
    }

    private String createFileName(String originalFileName, String contentType) {
        return UUID.randomUUID().toString().replace("-", "") + this.getExtension(originalFileName, contentType);
    }

    private String getExtension(String originalFileName, String contentType) {
        String fileName = this.safeName(originalFileName);
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension != null && !"".equals(extension.trim())) {
            return "." + extension.trim().toLowerCase();
        }
        if (contentType.startsWith("audio/")) {
            return ".mp3";
        }
        if (contentType.startsWith("video/")) {
            return ".mp4";
        }
        return "";
    }

    private String safeName(String originalFileName) {
        String fileName = this.safeTrim(originalFileName);
        return "".equals(fileName) ? "media" : fileName;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
