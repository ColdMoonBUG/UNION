package com.scoder.jusic.service.imp;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.model.AvSignal;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.AvPlaybackStateRepository;
import com.scoder.jusic.service.AvService;
import com.scoder.jusic.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author H
 */
@Service
public class AvServiceImpl implements AvService {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private AvPlaybackStateRepository avPlaybackStateRepository;

    @Override
    public void signal(String sessionId, AvSignal signal) {
        String roomId = sessionService.getRoomId(sessionId);
        AvSignal currentSignal = signal == null ? new AvSignal() : signal;
        currentSignal.setSessionId(sessionId);
        currentSignal.setRoomId(roomId);
        currentSignal.setPushTime(System.currentTimeMillis());
        String targetSessionId = this.safeTrim(currentSignal.getTargetSessionId());
        if (!"".equals(targetSessionId)) {
            User targetUser = sessionService.getUser(targetSessionId);
            if (targetUser == null) {
                throw new IllegalArgumentException("目标用户不存在");
            }
            if (!roomId.equals(sessionService.getRoomId(targetSessionId))) {
                throw new IllegalArgumentException("目标用户不在当前房间");
            }
            sessionService.send(targetSessionId, MessageType.AV_SIGNAL, Response.success(currentSignal, "音视频信令"));
            return;
        }
        sessionService.sendRoom(roomId, MessageType.AV_SIGNAL, Response.success(currentSignal, "音视频信令"));
    }

    @Override
    public AvPlaybackState updatePlaybackState(String sessionId, AvPlaybackState state) {
        String roomId = sessionService.getRoomId(sessionId);
        AvPlaybackState currentState = state == null ? new AvPlaybackState() : state;
        currentState.setSessionId(sessionId);
        currentState.setNickName(sessionService.getNickName(sessionId));
        currentState.setRoomId(roomId);
        currentState.setPushTime(System.currentTimeMillis());
        if (currentState.getUpdatedAt() == null) {
            currentState.setUpdatedAt(System.currentTimeMillis());
        }
        if (currentState.getPlaybackRate() == null) {
            currentState.setPlaybackRate(1D);
        }
        if (currentState.getVolume() == null) {
            currentState.setVolume(1D);
        }
        AvPlaybackState storedState = avPlaybackStateRepository.get(roomId);
        if (this.isStalePlayback(currentState, storedState)) {
            return storedState;
        }
        avPlaybackStateRepository.set(roomId, currentState);
        return currentState;
    }

    @Override
    public AvPlaybackState getPlaybackState(String roomId) {
        return avPlaybackStateRepository.get(roomId);
    }

    @Override
    public void clearPlaybackState(String roomId) {
        String currentRoomId = this.safeTrim(roomId);
        if ("".equals(currentRoomId)) {
            return;
        }
        avPlaybackStateRepository.remove(currentRoomId);
    }

    private boolean isStalePlayback(AvPlaybackState currentState, AvPlaybackState storedState) {
        if (currentState == null || storedState == null) {
            return false;
        }
        Long currentUpdatedAt = currentState.getUpdatedAt();
        Long storedUpdatedAt = storedState.getUpdatedAt();
        return currentUpdatedAt != null && storedUpdatedAt != null && currentUpdatedAt < storedUpdatedAt;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
