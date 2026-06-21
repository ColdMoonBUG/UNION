package com.scoder.jusic.handler;

import com.scoder.jusic.common.message.Response;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedList;

/**
 * @author H
 */
@Component
@Slf4j
public class JusicWebSocketHandlerAsync {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private RoomSnapshotService roomSnapshotService;
    @Autowired
    private HouseService houseService;
    @Autowired
    private AvService avService;

    @Async
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String requestedRoomId = this.getRoomId(session);
        try {
            this.validateRoomAccess(requestedRoomId, this.getHousePassword(session));
        } catch (IllegalArgumentException e) {
            log.info("Connection rejected: {}, room: {}, reason: {}", session.getId(), requestedRoomId, e.getMessage());
            sessionService.send(session, MessageType.NOTICE, Response.failure((Object) null, e.getMessage()));
            session.close(new CloseStatus(4001, e.getMessage()));
            return;
        }

        sessionService.putSession(session);
        String currentRoomId = sessionService.getRoomId(session.getId());
        int size = sessionService.size(currentRoomId).intValue();
        log.info("Connection established: {}, room: {}, ip: {}, and now online: {}", session.getId(), currentRoomId, session.getAttributes().get("remoteAddress").toString(), size);
        Thread.sleep(500);
        currentRoomId = sessionService.getRoomId(session.getId());
        sessionService.send(session, MessageType.NOTICE, Response.success((Object) null, "连接到服务器成功！"));
        roomSnapshotService.broadcastOnline(currentRoomId);
        sessionService.sendRoom(currentRoomId, MessageType.HOUSE_USER, Response.success(houseService.listUsers(currentRoomId), "房间成员"));
        roomSnapshotService.sendRoomSnapshot(session.getId(), currentRoomId, true);
        Music playing = musicService.getPlaying(currentRoomId);
        String playingName = playing == null ? "null" : playing.getName();
        log.info("发现有客户端连接, 房间: {}, 已向该客户端: {} 发送正在播放的音乐: {}, 以及播放列表, 共 {} 首", currentRoomId, session.getId(), playingName, musicService.getPickList(currentRoomId).size());
    }

    @Async
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        boolean activeSession = sessionService.getUser(session.getId()) != null;
        String roomId = activeSession ? sessionService.getRoomId(session.getId()) : this.getRoomId(session);
        sessionService.clearSession(session);
        if (!activeSession) {
            return;
        }
        int size = sessionService.size(roomId).intValue();
        log.info("Connection closed: {}, room: {}, and now online: {}", session.getId(), roomId, size);
        if (size <= 0) {
            avService.clearPlaybackState(roomId);
        }
        roomSnapshotService.broadcastOnline(roomId);
        sessionService.sendRoom(roomId, MessageType.HOUSE_USER, Response.success(houseService.listUsers(roomId), "房间成员"));
    }

    private void validateRoomAccess(String roomId, String password) {
        if (roomId == null || "default".equals(roomId)) {
            return;
        }
        houseService.enter(roomId, password);
    }

    private String getRoomId(WebSocketSession session) {
        Object roomId = session.getAttributes().get("roomId");
        if (roomId == null || "".equals(roomId.toString().trim())) {
            return "default";
        }
        return roomId.toString().trim();
    }

    private String getHousePassword(WebSocketSession session) {
        Object housePwd = session.getAttributes().get("housePwd");
        return housePwd == null ? "" : housePwd.toString().trim();
    }

}
