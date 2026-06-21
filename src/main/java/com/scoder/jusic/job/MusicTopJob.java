package com.scoder.jusic.job;

/**
 * @author alang
 * @create 2020-01-12 14:50
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.util.FileOperater;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Slf4j
public class MusicTopJob {
    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private  ResourceLoader resourceLoader;

//    public static void main(String[] args) {
//        getData(topUrl);
//    }

    //表示每隔3小时
    @Scheduled(fixedRate = 10800000)
    public void getMusicTopJob(){
        JusicProperties.setDefaultListByJob(getData());
    }

    public ArrayList<String> getMusicTop(){
        return getData();
    }

    private class TopMusic{
        private ArrayList<String> topMusicList;
        private String topMusicStrings;

        public ArrayList<String> getTopMusicList() {
            return topMusicList;
        }

        public void setTopMusicList(ArrayList<String> topMusicList) {
            this.topMusicList = topMusicList;
        }

        public String getTopMusicStrings() {
            return topMusicStrings;
        }

        public void setTopMusicStrings(String topMusicStrings) {
            this.topMusicStrings = topMusicStrings;
        }

        public TopMusic() {
        }

        public TopMusic(ArrayList<String> topMusicList, String topMusicStrings) {
            this.topMusicList = topMusicList;
            this.topMusicStrings = topMusicStrings;
        }
    }

    private TopMusic getTopMusicWy(){
        ArrayList<String> topList = new ArrayList<>();
        String musicIds = "";
        Document doc = null;
        String cookie = jusicProperties.getNeteaseCookie();
        if (cookie == null || cookie.trim().isEmpty()) {
            cookie = "os=pc; appver=2.10.6;";
        }
        try {
            doc = Jsoup.connect(jusicProperties.getWyTopUrl()).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Cookie", cookie)
                    .header("Referer", "https://music.163.com")
                    .method(Connection.Method.GET)
                    .timeout(10000).get(); // 失败尽快返回，避免启动或定时任务长时间阻塞

            Elements names = doc.select("#song-list-pre-cache a");

            String musicId = "";
            for (Element element : names) {
                musicId = element.attr("href").
                        replace("/song?id=", "").trim();
                if (musicId != null && musicId != "") {
                    musicIds += musicId+"\n";
                    topList.add(musicId);
                }
            }
        } catch (Exception e) {
            log.error("mg音乐热门歌曲获取失败; Exception: [{}]",e.getMessage());
        }
        return new TopMusic(topList,musicIds);
    }
    private TopMusic getTopMusicQQ() {
        HttpResponse<String> response = null;
        String musicIds = "";
        ArrayList<String> topList = new ArrayList<>();
        Integer failCount = 0;

        while (failCount < jusicProperties.getRetryCount()) {
            try {
                response = Unirest.get(jusicProperties.getMusicServeDomainQq() + "/top?id=26&pageSize=300")
                        .asString();

                if (response.getStatus() != 200) {
                    failCount++;
                } else {
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (jsonObject.get("result").equals(100)) {
                        JSONArray data = jsonObject.getJSONObject("data").getJSONArray("list");
                        int size = data.size();
                        String musicId = "";
                        for(int i = 0; i < size; i++) {
                            musicId = data.getJSONObject(i).getString("mid");
                            if (musicId != null && musicId != "") {
                                musicIds += musicId+"___qq" + "\n";
                                topList.add(musicId+"___qq");
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                failCount++;
                log.error("qq音乐热门歌曲获取失败; Exception: [{}]",e.getMessage());
            }
        }

        return new TopMusic(topList,musicIds);
    }
    public ArrayList<String> getData() {
        TopMusic topMusicWy = getTopMusicWy();
        TopMusic topMusicQq = getTopMusicQQ();
        String allMusicIdsStr = "";
        ArrayList<String> allMusicIdsList = new ArrayList<>();
        if (topMusicWy.getTopMusicStrings() != "") {
            allMusicIdsStr += topMusicWy.getTopMusicStrings();
            allMusicIdsList.addAll(topMusicWy.getTopMusicList());
        }
        if(topMusicQq.getTopMusicStrings() != ""){
            allMusicIdsStr += topMusicQq.getTopMusicStrings();
            allMusicIdsList.addAll(topMusicQq.getTopMusicList());
        }
        if(allMusicIdsStr != ""){
            try {
                FileOperater.writefileinfo(allMusicIdsStr, resourceLoader.getResource(jusicProperties.getDefaultMusicFile()));
            } catch (IOException e) {
                log.error("写入热门歌曲id失败，IOException:[{}]",e.getMessage());
            }
        }
        return allMusicIdsList;
    }
}
