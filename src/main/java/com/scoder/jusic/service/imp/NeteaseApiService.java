package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Album;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.util.WeapiCrypto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class NeteaseApiService {

    private static final String BASE = "https://music.163.com";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    @Autowired
    private JusicProperties jusicProperties;

    public List<Music> search(String keyword, int page, int pageSize) {
        JSONObject payload = new JSONObject();
        payload.put("s", keyword);
        payload.put("type", 1);
        payload.put("offset", (page - 1) * pageSize);
        payload.put("limit", pageSize);
        payload.put("total", true);
        payload.put("csrf_token", "");

        JSONObject resp = doWeapiPost("/weapi/cloudsearch/get/web", payload);
        if (resp == null) return Collections.emptyList();

        JSONObject result = resp.getJSONObject("result");
        if (result == null) return Collections.emptyList();
        JSONArray songs = result.getJSONArray("songs");
        if (songs == null) return Collections.emptyList();

        List<Music> list = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            JSONObject song = songs.getJSONObject(i);
            Music m = new Music();
            m.setId(song.getString("id"));
            m.setName(song.getString("name"));
            m.setDuration(song.getLong("dt"));
            JSONArray ar = song.getJSONArray("ar");
            if (ar != null && !ar.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < ar.size(); j++) {
                    if (j > 0) sb.append("/");
                    sb.append(ar.getJSONObject(j).getString("name"));
                }
                m.setArtist(sb.toString());
            }
            JSONObject al = song.getJSONObject("al");
            if (al != null) {
                Album album = new Album();
                album.setId(al.getInteger("id"));
                album.setName(al.getString("name"));
                album.setPictureUrl(al.getString("picUrl"));
                m.setAlbum(album);
                m.setPictureUrl(al.getString("picUrl"));
            }
            m.setSource("wy");
            list.add(m);
        }
        return list;
    }

    public Music getDetail(String songId) {
        JSONObject payload = new JSONObject();
        JSONArray c = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("id", songId);
        c.add(item);
        payload.put("c", c.toJSONString());
        payload.put("csrf_token", "");

        JSONObject resp = doWeapiPost("/weapi/v3/song/detail", payload);
        if (resp == null) return null;

        JSONArray songs = resp.getJSONArray("songs");
        if (songs == null || songs.isEmpty()) return null;
        JSONObject song = songs.getJSONObject(0);

        Music m = new Music();
        m.setId(song.getString("id"));
        m.setName(song.getString("name"));
        m.setDuration(song.getLong("dt"));
        JSONArray ar = song.getJSONArray("ar");
        if (ar != null && !ar.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < ar.size(); j++) {
                if (j > 0) sb.append("/");
                sb.append(ar.getJSONObject(j).getString("name"));
            }
            m.setArtist(sb.toString());
        }
        JSONObject al = song.getJSONObject("al");
        if (al != null) {
            Album album = new Album();
            album.setId(al.getInteger("id"));
            album.setName(al.getString("name"));
            album.setPictureUrl(al.getString("picUrl"));
            m.setAlbum(album);
            m.setPictureUrl(al.getString("picUrl"));
        }
        m.setSource("wy");
        return m;
    }

    public String getPlayUrl(String songId) {
        JSONObject payload = new JSONObject();
        JSONArray ids = new JSONArray();
        ids.add(Long.parseLong(songId));
        payload.put("ids", ids.toJSONString());
        payload.put("br", 320000);
        payload.put("csrf_token", "");

        JSONObject resp = doWeapiPost("/weapi/song/enhance/player/url/v1", payload);
        if (resp == null) return null;

        JSONArray data = resp.getJSONArray("data");
        if (data == null || data.isEmpty()) return null;
        JSONObject first = data.getJSONObject(0);
        String url = first.getString("url");
        if (url == null || url.isEmpty()) {
            url = "https://music.163.com/song/media/outer/url?id=" + songId + ".mp3";
        }
        return url;
    }

    public String getLyric(String songId) {
        JSONObject payload = new JSONObject();
        payload.put("id", songId);
        payload.put("lv", -1);
        payload.put("tv", -1);
        payload.put("csrf_token", "");

        JSONObject resp = doWeapiPost("/weapi/song/lyric", payload);
        if (resp == null) return "";

        JSONObject lrc = resp.getJSONObject("lrc");
        if (lrc == null) return "";
        return lrc.getString("lyric");
    }

    public Music getMusicWithUrl(String songId) {
        Music m = getDetail(songId);
        if (m == null) return null;
        String url = getPlayUrl(songId);
        if (url != null) m.setUrl(url);
        String lyric = getLyric(songId);
        if (lyric != null) m.setLyric(lyric);
        return m;
    }

    private JSONObject doWeapiPost(String path, JSONObject payload) {
        try {
            Map<String, String> encrypted = WeapiCrypto.encrypt(payload.toJSONString());
            HttpResponse<String> response = Unirest.post(BASE + path)
                    .header("User-Agent", UA)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", "https://music.163.com")
                    .header("Cookie", getCookie())
                    .field("params", encrypted.get("params"))
                    .field("encSecKey", encrypted.get("encSecKey"))
                    .asString();
            if (response.getStatus() != 200) {
                log.warn("Netease API {} returned {}", path, response.getStatus());
                return null;
            }
            return JSON.parseObject(response.getBody());
        } catch (Exception e) {
            log.error("Netease API {} error: {}", path, e.getMessage());
            return null;
        }
    }

    private String getCookie() {
        String cookie = jusicProperties.getNeteaseCookie();
        if (cookie == null || cookie.trim().isEmpty()) {
            return "os=pc; appver=2.10.6;";
        }
        return cookie;
    }
}
