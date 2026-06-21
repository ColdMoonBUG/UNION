package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.AvMediaResolveResult;
import com.scoder.jusic.service.AvMediaResolveService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author H
 */
@Service
@Slf4j
public class AvMediaResolveServiceImpl implements AvMediaResolveService {

    @Autowired
    private JusicProperties jusicProperties;

    private static final Pattern BV_PATTERN = Pattern.compile("(BV[0-9A-Za-z]{10})");
    private static final Pattern AV_PATTERN = Pattern.compile("(?i)av(\\d+)");
    private static final Pattern PAGE_PATTERN = Pattern.compile("[?&]p=(\\d+)");
    private static final Pattern PLAYINFO_PATTERN = Pattern.compile("window\\.__playinfo__\\s*=\\s*(\\{.*?\\})\\s*;?\\s*<\\/script>", Pattern.DOTALL);

    @Override
    public AvMediaResolveResult resolve(String url, String title) {
        String currentUrl = this.safeTrim(url);
        if ("".equals(currentUrl)) {
            throw new IllegalArgumentException("请输入视频地址");
        }
        if (this.isBilibiliUrl(currentUrl)) {
            return this.resolveBilibili(currentUrl, title);
        }
        return this.resolveDirect(currentUrl, title);
    }

    private AvMediaResolveResult resolveDirect(String url, String title) {
        AvMediaResolveResult result = new AvMediaResolveResult();
        result.setSourceType(this.guessSourceType(url));
        result.setOriginUrl(url);
        result.setMediaUrl(url);
        result.setTitle(this.pickTitle(title, url));
        result.setContentType(this.guessContentType(url));
        result.setMediaId(this.pickMediaId(url));
        result.setResolvedAt(System.currentTimeMillis());
        return result;
    }

    private AvMediaResolveResult resolveBilibili(String inputUrl, String title) {
        String normalizedUrl = this.followRedirects(inputUrl);
        String bvid = this.extractBvid(normalizedUrl);
        String aid = this.extractAvid(normalizedUrl);
        int page = this.extractPage(normalizedUrl);
        JSONObject view = this.requestViewInfo(bvid, aid);
        JSONObject pageInfo = this.pickPageInfo(view, page);
        long cid = pageInfo == null ? this.safeLong(view.getString("cid")) : pageInfo.getLongValue("cid");
        String resolvedTitle = this.pickTitle(title, view.getString("title"));
        if (!StringUtils.hasText(title) && page > 1 && pageInfo != null && StringUtils.hasText(pageInfo.getString("part"))) {
            resolvedTitle = this.pickTitle(view.getString("title"), pageInfo.getString("part"));
        }

        AvMediaResolveResult result = this.resolveFromPlayInfo(normalizedUrl, bvid, aid, page, cid, resolvedTitle);
        result.setSourceType("bilibili");
        result.setOriginUrl(inputUrl);
        if (!StringUtils.hasText(result.getMediaId())) {
            result.setMediaId(StringUtils.hasText(bvid) ? bvid : aid);
        }
        if (!StringUtils.hasText(result.getTitle())) {
            result.setTitle(resolvedTitle);
        }
        result.setResolvedAt(System.currentTimeMillis());
        return result;
    }

    private AvMediaResolveResult resolveFromPlayInfo(String normalizedUrl, String bvid, String aid, int page, long cid, String title) {
        String pageHtml = this.fetchHtml(this.buildBilibiliPageUrl(normalizedUrl, bvid, aid, page, cid));
        JSONObject playInfo = this.parsePlayInfo(pageHtml);
        String mediaUrl = this.extractPlayableUrl(playInfo);
        if (!StringUtils.hasText(mediaUrl)) {
            mediaUrl = this.fetchPlayUrlByApi(bvid, aid, cid);
        }
        if (!StringUtils.hasText(mediaUrl)) {
            throw new IllegalArgumentException("无法解析哔哩哔哩视频播放地址");
        }
        AvMediaResolveResult result = new AvMediaResolveResult();
        result.setOriginUrl(normalizedUrl);
        result.setMediaUrl("/av/media/proxy?url=" + this.encodeQuery(mediaUrl));
        result.setTitle(title);
        result.setContentType("video/mp4");
        result.setMediaId(StringUtils.hasText(bvid) ? bvid : aid);
        result.setPosterUrl(this.safeTrim(this.extractPoster(playInfo)));
        return result;
    }

    private JSONObject requestViewInfo(String bvid, String aid) {
        StringBuilder url = new StringBuilder("https://api.bilibili.com/x/web-interface/view?");
        if (StringUtils.hasText(bvid)) {
            url.append("bvid=").append(this.encodeQuery(bvid));
        } else if (StringUtils.hasText(aid)) {
            url.append("aid=").append(this.encodeQuery(aid));
        } else {
            throw new IllegalArgumentException("无法识别 B 站视频 ID");
        }
        try {
            HttpResponse<String> response = Unirest.get(url.toString())
                    .header("User-Agent", this.defaultUa())
                    .header("Referer", "https://www.bilibili.com")
                    .header("Cookie", getBilibiliCookie())
                    .asString();
            JSONObject responseJson = JSONObject.parseObject(response.getBody());
            if (responseJson == null || !Integer.valueOf(0).equals(responseJson.getInteger("code"))) {
                throw new IllegalArgumentException("获取哔哩哔哩视频信息失败");
            }
            return responseJson.getJSONObject("data");
        } catch (Exception e) {
            throw new IllegalArgumentException("获取哔哩哔哩视频信息失败", e);
        }
    }

    private String fetchPlayUrlByApi(String bvid, String aid, long cid) {
        StringBuilder url = new StringBuilder("https://api.bilibili.com/x/player/playurl?");
        if (StringUtils.hasText(bvid)) {
            url.append("bvid=").append(this.encodeQuery(bvid));
        } else if (StringUtils.hasText(aid)) {
            url.append("aid=").append(this.encodeQuery(aid));
        }
        url.append("&cid=").append(cid)
                .append("&qn=116&fnval=0&fnver=0&fourk=1&platform=html5");
        try {
            HttpResponse<String> response = Unirest.get(url.toString())
                    .header("User-Agent", this.defaultUa())
                    .header("Referer", "https://www.bilibili.com")
                    .header("Cookie", getBilibiliCookie())
                    .asString();
            JSONObject responseJson = JSONObject.parseObject(response.getBody());
            if (responseJson == null || !Integer.valueOf(0).equals(responseJson.getInteger("code"))) {
                return null;
            }
            JSONObject data = responseJson.getJSONObject("data");
            return this.extractPlayableUrl(data);
        } catch (Exception e) {
            log.warn("哔哩哔哩播放地址解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractPlayableUrl(JSONObject playInfo) {
        if (playInfo == null) {
            return null;
        }
        JSONObject data = playInfo.getJSONObject("data");
        if (data != null) {
            playInfo = data;
        }
        JSONArray durl = playInfo.getJSONArray("durl");
        if (durl != null && !durl.isEmpty()) {
            JSONObject first = durl.getJSONObject(0);
            String url = first.getString("url");
            if (StringUtils.hasText(url)) {
                return url;
            }
            JSONArray backupUrls = first.getJSONArray("backup_url");
            if (backupUrls != null && !backupUrls.isEmpty()) {
                return backupUrls.getString(0);
            }
        }
        JSONObject dash = playInfo.getJSONObject("dash");
        if (dash != null) {
            JSONArray videos = dash.getJSONArray("video");
            if (videos != null && !videos.isEmpty()) {
                JSONObject firstVideo = videos.getJSONObject(0);
                String url = firstVideo.getString("baseUrl");
                if (!StringUtils.hasText(url)) {
                    url = firstVideo.getString("base_url");
                }
                if (StringUtils.hasText(url)) {
                    return url;
                }
            }
        }
        return null;
    }

    private String extractPoster(JSONObject playInfo) {
        if (playInfo == null) {
            return null;
        }
        JSONObject data = playInfo.getJSONObject("data");
        if (data != null) {
            playInfo = data;
        }
        JSONObject videoInfo = playInfo.getJSONObject("videoInfo");
        if (videoInfo != null && StringUtils.hasText(videoInfo.getString("cover"))) {
            return videoInfo.getString("cover");
        }
        return null;
    }

    private JSONObject parsePlayInfo(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        Matcher matcher = PLAYINFO_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        try {
            return JSONObject.parseObject(matcher.group(1));
        } catch (Exception e) {
            log.warn("解析 B 站 playinfo 失败: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject pickPageInfo(JSONObject view, int page) {
        if (view == null) {
            return null;
        }
        JSONArray pages = view.getJSONArray("pages");
        if (pages == null || pages.isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(page - 1, pages.size() - 1));
        return pages.getJSONObject(index);
    }

    private String fetchHtml(String pageUrl) {
        try {
            Document document = Jsoup.connect(pageUrl)
                    .userAgent(this.defaultUa())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Referer", "https://www.bilibili.com")
                    .header("Cookie", getBilibiliCookie())
                    .timeout(10000)
                    .get();
            return document.outerHtml();
        } catch (IOException e) {
            throw new IllegalArgumentException("获取哔哩哔哩页面失败", e);
        }
    }

    private String buildBilibiliPageUrl(String normalizedUrl, String bvid, String aid, int page, long cid) {
        if (StringUtils.hasText(bvid)) {
            StringBuilder builder = new StringBuilder("https://www.bilibili.com/video/").append(bvid);
            if (page > 1) {
                builder.append("?p=").append(page);
            }
            return builder.toString();
        }
        if (StringUtils.hasText(aid)) {
            StringBuilder builder = new StringBuilder("https://www.bilibili.com/video/av").append(aid);
            if (page > 1) {
                builder.append("?p=").append(page);
            }
            return builder.toString();
        }
        throw new IllegalArgumentException("无法识别 B 站视频 ID");
    }

    private String followRedirects(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", this.defaultUa());
            connection.setRequestProperty("Referer", "https://www.bilibili.com");
            connection.setRequestMethod("GET");
            connection.connect();
            try {
                connection.getInputStream().close();
            } catch (IOException ignored) {
            }
            return connection.getURL().toString();
        } catch (IOException e) {
            log.warn("跟随短链失败，直接使用原始地址: {}", e.getMessage());
            return url;
        }
    }

    private String extractBvid(String url) {
        String current = this.safeTrim(url);
        Matcher matcher = BV_PATTERN.matcher(current);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractAvid(String url) {
        String current = this.safeTrim(url);
        Matcher matcher = AV_PATTERN.matcher(current);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private int extractPage(String url) {
        String current = this.safeTrim(url);
        Matcher matcher = PAGE_PATTERN.matcher(current);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    private boolean isBilibiliUrl(String url) {
        String current = this.safeTrim(url).toLowerCase(Locale.ROOT);
        return current.contains("bilibili.com") || current.contains("b23.tv") || current.contains("bili2233.cn");
    }

    private String pickTitle(String title, String fallback) {
        String currentTitle = this.safeTrim(title);
        if (!"".equals(currentTitle)) {
            return currentTitle;
        }
        String currentFallback = this.safeTrim(fallback);
        if (!"".equals(currentFallback)) {
            return currentFallback;
        }
        return "在线视频";
    }

    private String guessSourceType(String url) {
        String current = this.stripQuery(this.safeTrim(url)).toLowerCase(Locale.ROOT);
        if (current.endsWith(".m3u8")) {
            return "stream";
        }
        if (this.isDirectAudioOrVideo(current)) {
            return "direct";
        }
        if (current.startsWith("/av/media/files/") || current.contains("/av/media/files/")) {
            return "upload";
        }
        return "direct";
    }

    private String guessContentType(String url) {
        String current = this.stripQuery(this.safeTrim(url)).toLowerCase(Locale.ROOT);
        if (current.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (current.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (current.endsWith(".wav")) {
            return "audio/wav";
        }
        if (current.endsWith(".m4a")) {
            return "audio/mp4";
        }
        if (current.endsWith(".aac")) {
            return "audio/aac";
        }
        if (current.endsWith(".flac")) {
            return "audio/flac";
        }
        if (current.endsWith(".ogg")) {
            return "audio/ogg";
        }
        if (current.endsWith(".webm")) {
            return "video/webm";
        }
        if (current.endsWith(".mov")) {
            return "video/quicktime";
        }
        if (current.endsWith(".mkv")) {
            return "video/x-matroska";
        }
        return "video/mp4";
    }

    private boolean isDirectAudioOrVideo(String current) {
        return current.endsWith(".mp3")
                || current.endsWith(".wav")
                || current.endsWith(".m4a")
                || current.endsWith(".aac")
                || current.endsWith(".flac")
                || current.endsWith(".ogg")
                || current.endsWith(".mp4")
                || current.endsWith(".webm")
                || current.endsWith(".mov")
                || current.endsWith(".mkv");
    }

    private String pickMediaId(String url) {
        String current = this.stripQuery(this.safeTrim(url));
        if ("".equals(current)) {
            return "";
        }
        int idx = current.lastIndexOf('/');
        if (idx >= 0 && idx < current.length() - 1) {
            return current.substring(idx + 1);
        }
        return current;
    }

    private String encodeQuery(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    private long safeLong(String value) {
        try {
            return Long.parseLong(this.safeTrim(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    private String defaultUa() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    }

    private String stripQuery(String value) {
        String current = this.safeTrim(value);
        int index = current.indexOf('?');
        return index >= 0 ? current.substring(0, index) : current;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String getBilibiliCookie() {
        String cookie = jusicProperties.getBilibiliCookie();
        return cookie == null || cookie.trim().isEmpty() ? "" : cookie;
    }
}
