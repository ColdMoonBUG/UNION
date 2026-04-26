package com.scoder.jusic.service.imp;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.Notice;
import com.scoder.jusic.model.Online;
import com.scoder.jusic.service.AvService;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.HouseService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.RoomSnapshotService;
import com.scoder.jusic.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

/**
 * @author H
 */
@Service
public class RoomSnapshotServiceImpl implements RoomSnapshotService {

    private static final String PLAY_MODE_SINGLE = "single";
    private static final String PLAY_MODE_RANDOM = "random";
    private static final String PLAY_MODE_LIST = "list";

    @Autowired
    private SessionService sessionService;
    @Autowired
    private HouseService houseService;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private AvService avService;

    @Override
    public void sendRoomSnapshot(String sessionId) {
        this.sendRoomSnapshot(sessionId, sessionService.getRoomId(sessionId), true);
    }

    @Override
    public void sendRoomSnapshot(String sessionId, String roomId) {
        this.sendRoomSnapshot(sessionId, roomId, true);
    }

    @Override
    public void sendRoomSnapshot(String sessionId, String roomId, boolean includeHouseInfo) {
        String currentRoomId = this.safeRoomId(sessionId, roomId);
        sessionService.send(sessionId, MessageType.SESSION_INFO, Response.success(sessionService.getUser(sessionId), "当前会话"));
        House house = houseService.get(currentRoomId);
        if (includeHouseInfo && house != null) {
            sessionService.send(sessionId, MessageType.ENTER_HOUSE, Response.success(house, "房间信息"));
        }

        sessionService.send(sessionId, MessageType.HOUSE_USER,
                Response.success(houseService.listUsers(currentRoomId), "房间成员"));

        Notice notice = house != null && house.getAnnounce() != null ? house.getAnnounce() : new Notice();
        sessionService.send(sessionId, MessageType.ANNOUNCEMENT, Response.success(notice, "房间公告"));

        Music playing = musicService.getPlaying(currentRoomId);
        sessionService.send(sessionId, MessageType.MUSIC, Response.success(playing, "正在播放"));

        Boolean goodModel = configService.getGoodModel(currentRoomId);
        LinkedList<Music> pickList = musicService.getPickList(currentRoomId);
        if (Boolean.TRUE.equals(goodModel)) {
            sessionService.send(sessionId, MessageType.PICK, Response.success(pickList, "goodlist"));
            sessionService.send(sessionId, MessageType.GOODMODEL, Response.success("GOOD", "goodlist"));
        } else {
            sessionService.send(sessionId, MessageType.PICK, Response.success(pickList, "播放列表"));
            sessionService.send(sessionId, MessageType.GOODMODEL, Response.success("EXITGOOD", "goodlist"));
        }

        this.sendPlayMode(sessionId, currentRoomId);

        AvPlaybackState state = avService.getPlaybackState(currentRoomId);
        sessionService.send(sessionId, MessageType.AV_PLAYBACK_STATE, Response.success(state, "播放状态"));
    }

    @Override
    public void broadcastOnline(String roomId) {
        Online online = new Online();
        online.setCount(sessionService.size(roomId).intValue());
        sessionService.sendRoom(roomId, MessageType.ONLINE, Response.success(online));
    }

    private void sendPlayMode(String sessionId, String roomId) {
        String playMode = configService.getPlayMode(roomId);
        if (PLAY_MODE_SINGLE.equals(playMode)) {
            sessionService.send(sessionId, MessageType.CIRCLEMODEL, Response.success(playMode, "已启用单曲循环"));
            return;
        }
        if (PLAY_MODE_RANDOM.equals(playMode)) {
            sessionService.send(sessionId, MessageType.RANDOMMODEL, Response.success(playMode, "已启用随机模式"));
            return;
        }
        sessionService.send(sessionId, MessageType.LISTMODEL, Response.success(PLAY_MODE_LIST, "已启用列表循环"));
    }

    private String safeRoomId(String sessionId, String roomId) {
        if (roomId == null || "".equals(roomId.trim())) {
            return sessionService.getRoomId(sessionId);
        }
        return roomId.trim();
    }
}
