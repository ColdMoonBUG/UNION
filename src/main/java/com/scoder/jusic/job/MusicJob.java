package com.scoder.jusic.job;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * @author H
 */
@Component
@Slf4j
public class MusicJob {

    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private MusicService musicService;

    /**
     * 广播条件：第一次启动时 playing 为空、音乐播放完毕、投票切歌
     */
    @Scheduled(fixedRate = 1000)
    private void sendIfSufficient() {
        List<String> roomIds = sessionService.listRoomIds();
        for (String roomId : roomIds) {
            this.sendIfSufficient(roomId);
        }
    }

    private void sendIfSufficient(String roomId) {
        if (this.isPlayingNull(roomId)) {
            configRepository.setPushSwitch(roomId, true);
            log.info("推送开关开启, 房间: {}, 原因: 首次启动", roomId);
        } else if (this.isPlayingOver(roomId)) {
            configRepository.setPushSwitch(roomId, true);
            log.info("推送开关开启, 房间: {}, 原因: 上一首播放完毕", roomId);
        } else if (this.isPlayingSkip(roomId)) {
            configRepository.setPushSwitch(roomId, true);
            log.info("推送开关开启, 房间: {}, 原因: 投票通过", roomId);
        }

        if (this.isPushSwitchOpen(roomId)) {
            log.info("检测到推送开关已开启, 房间: {}", roomId);
            Music music = musicService.musicSwitch(roomId);
            if (music == null) {
                configRepository.setPushSwitch(roomId, false);
                return;
            }
            long pushTime = System.currentTimeMillis();
            Long duration = music.getDuration() == null || music.getDuration() <= 0 ? 0L : music.getDuration();

            configRepository.setLastMusicPushTimeAndDuration(roomId, pushTime, duration);
            music.setPushTime(pushTime);
            musicPlayingRepository.leftPush(roomId, music);
            musicPlayingRepository.keepTheOne(roomId);
            log.info("已保存推送时间和音乐时长, 房间: {}", roomId);
            configRepository.setPushSwitch(roomId, false);
            log.info("已关闭音乐推送开关, 房间: {}", roomId);
            musicVoteRepository.reset(roomId);
            log.info("已重置投票, 房间: {}", roomId);
            sessionService.sendRoom(roomId, MessageType.MUSIC, Response.success(music, "正在播放"));
            log.info("已向房间客户端推送音乐, 房间: {}, 音乐: {}, 时长: {}, 推送时间: {}", roomId, music.getName(), duration, pushTime);
            LinkedList<Music> result = musicService.getPickList(roomId);
            sessionService.sendRoom(roomId, MessageType.PICK, Response.success(result, "播放列表"));
            log.info("已向房间客户端推送播放列表, 房间: {}, 共 {} 首", roomId, result.size());
        }
    }

    private boolean isPushSwitchOpen(String roomId) {
        Boolean pushSwitch = configRepository.getPushSwitch(roomId);
        return pushSwitch != null && pushSwitch;
    }

    private boolean isPlayingSkip(String roomId) {
        Long voteSize = musicVoteRepository.size(roomId);
        Long sessionSize = sessionService.size(roomId);
        Float voteRate = configRepository.getVoteRate(roomId);
        if (voteSize != null && voteSize != 0 && sessionSize != null && voteRate != null) {
            return voteSize >= sessionSize * voteRate;
        }
        return false;
    }

    private boolean isPlayingOver(String roomId) {
        Long lastMusicDuration = configRepository.getLastMusicDuration(roomId);
        Long lastMusicPushTime = configRepository.getLastMusicPushTime(roomId);
        if (lastMusicDuration != null && lastMusicDuration > 0 && lastMusicPushTime != null) {
            return (lastMusicPushTime + lastMusicDuration) - System.currentTimeMillis() <= 0;
        }
        return false;
    }

    private boolean isPlayingNull(String roomId) {
        return musicPlayingRepository.getPlaying(roomId) == null;
    }

}
