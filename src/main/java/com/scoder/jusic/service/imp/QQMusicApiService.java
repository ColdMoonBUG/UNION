package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Album;
import com.scoder.jusic.model.Music;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class QQMusicApiService {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    @Autowired
    private JusicProperties jusicProperties;

    public List<Music> search(String keyword, int page, int pageSize) {
        try {
            String url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=" + encode(keyword)
                    + "&p=" + page + "&n=" + pageSize + "&format=json&cr=1";
            HttpResponse<String> response = Unirest.get(url)
                    .header("User-Agent", UA)
                    .header("Referer", "https://y.qq.com")
                    .asString();
            if (response.getStatus() != 200) return Collections.emptyList();

            JSONObject body = JSON.parseObject(response.getBody());
            JSONObject data = body.getJSONObject("data");
            if (data == null) return Collections.emptyList();
            JSONObject songData = data.getJSONObject("song");
            if (songData == null) return Collections.emptyList();
            JSONArray list = songData.getJSONArray("list");
            if (list == null) return Collections.emptyList();

            List<Music> result = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                JSONObject item = list.getJSONObject(i);
                Music m = new Music();
                m.setId(item.getString("songmid"));
                m.setName(item.getString("songname"));
                m.setDuration(item.getLong("interval") * 1000);
                JSONArray singers = item.getJSONArray("singer");
                if (singers != null && !singers.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < singers.size(); j++) {
                        if (j > 0) sb.append("/");
                        sb.append(singers.getJSONObject(j).getString("name"));
                    }
                    m.setArtist(sb.toString());
                }
                String albummid = item.getString("albummid");
                Album album = new Album();
                album.setId(item.getInteger("albumid"));
                album.setName(item.getString("albumname"));
                if (albummid != null && !albummid.isEmpty()) {
                    album.setPictureUrl("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg");
                }
                m.setAlbum(album);
                m.setPictureUrl(album.getPictureUrl());
                m.setSource("qq");
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            log.error("QQ Music search error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public String getPlayUrl(String songmid) {
        try {
            String guid = String.valueOf(new Random().nextInt(900000000) + 100000000);
            String uin = extractUin();

            JSONObject reqData = new JSONObject();
            JSONObject req0 = new JSONObject();
            req0.put("module", "vkey.GetVkeyServer");
            req0.put("method", "CgiGetVkey");
            JSONObject param = new JSONObject();
            JSONArray songmidArr = new JSONArray();
            songmidArr.add(songmid);
            param.put("guid", guid);
            param.put("songmid", songmidArr);
            param.put("songtype", new JSONArray());
            param.put("uin", uin);
            param.put("loginflag", 1);
            param.put("platform", "20");
            req0.put("param", param);
            reqData.put("req_0", req0);
            reqData.put("comm", buildComm(uin));

            HttpResponse<String> response = Unirest.post("https://u.y.qq.com/cgi-bin/musicu.fcg")
                    .header("User-Agent", UA)
                    .header("Referer", "https://y.qq.com")
                    .header("Content-Type", "application/json")
                    .header("Cookie", getCookie())
                    .body(reqData.toJSONString())
                    .asString();
            if (response.getStatus() != 200) return null;

            JSONObject body = JSON.parseObject(response.getBody());
            JSONObject req0Resp = body.getJSONObject("req_0");
            if (req0Resp == null) return null;
            JSONObject data = req0Resp.getJSONObject("data");
            if (data == null) return null;

            JSONArray midurlinfo = data.getJSONArray("midurlinfo");
            String sip = "";
            JSONArray sips = data.getJSONArray("sip");
            if (sips != null && !sips.isEmpty()) {
                sip = sips.getString(0);
            }
            if (midurlinfo != null && !midurlinfo.isEmpty()) {
                String purl = midurlinfo.getJSONObject(0).getString("purl");
                if (purl != null && !purl.isEmpty()) {
                    return sip + purl;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("QQ Music getPlayUrl error: {}", e.getMessage());
            return null;
        }
    }

    public String getLyric(String songmid) {
        try {
            String url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=" + songmid + "&format=json&nobase64=1";
            HttpResponse<String> response = Unirest.get(url)
                    .header("User-Agent", UA)
                    .header("Referer", "https://y.qq.com")
                    .asString();
            if (response.getStatus() != 200) return "";
            JSONObject body = JSON.parseObject(response.getBody());
            return body.getString("lyric");
        } catch (Exception e) {
            log.error("QQ Music getLyric error: {}", e.getMessage());
            return "";
        }
    }

    public Music getMusicWithUrl(String songmid) {
        List<Music> results = search(songmid, 1, 1);
        Music m = null;
        if (!results.isEmpty() && results.get(0).getId().equals(songmid)) {
            m = results.get(0);
        }
        if (m == null) {
            m = new Music();
            m.setId(songmid);
            m.setName(songmid);
            m.setSource("qq");
        }
        String url = getPlayUrl(songmid);
        if (url != null) m.setUrl(url);
        String lyric = getLyric(songmid);
        if (lyric != null) m.setLyric(lyric);
        return m;
    }

    private JSONObject buildComm(String uin) {
        JSONObject comm = new JSONObject();
        comm.put("uin", uin);
        comm.put("format", "json");
        comm.put("ct", 24);
        comm.put("cv", 0);
        return comm;
    }

    private String extractUin() {
        String cookie = getCookie();
        if (cookie.contains("uin=")) {
            String[] parts = cookie.split("uin=");
            if (parts.length > 1) {
                String val = parts[1].split(";")[0].trim();
                return val.replaceAll("\\D", "");
            }
        }
        return "0";
    }

    private String getCookie() {
        String cookie = jusicProperties.getQqCookie();
        return cookie == null || cookie.trim().isEmpty() ? "" : cookie;
    }

    private static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
