package com.scoder.jusic.service.imp;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.model.AvSignal;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.model.AvMediaResolveResult;
import com.scoder.jusic.repository.AvPlaybackStateRepository;
import com.scoder.jusic.service.AvMediaResolveService;
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
    @Autowired
    private AvMediaResolveService avMediaResolveService;

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
        currentState.setUpdatedBy(sessionService.getNickName(sessionId));
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
        this.normalizeSource(currentState, false);
        AvPlaybackState storedState = avPlaybackStateRepository.get(roomId);
        if (this.isStalePlayback(currentState, storedState)) {
            return storedState;
        }
        avPlaybackStateRepository.set(roomId, currentState);
        return currentState;
    }

    @Override
    public AvPlaybackState getPlaybackState(String roomId) {
        AvPlaybackState state = avPlaybackStateRepository.get(roomId);
        if (state == null) {
            return null;
        }
        this.normalizeSource(state, true);
        avPlaybackStateRepository.set(roomId, state);
        return state;
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

    private void normalizeSource(AvPlaybackState state, boolean allowRefresh) {
        if (state == null) {
            return;
        }
        String sourceType = this.safeTrim(state.getSourceType());
        String originUrl = this.safeTrim(state.getOriginUrl());
        String mediaUrl = this.safeTrim(state.getMediaUrl());
        String guessedType = this.guessSourceType(originUrl, mediaUrl);
        if ("".equals(sourceType) || ("bilibili".equals(guessedType) && !"bilibili".equalsIgnoreCase(sourceType))) {
            sourceType = guessedType;
            state.setSourceType(sourceType);
        }
        if ("".equals(originUrl)) {
            originUrl = mediaUrl;
            state.setOriginUrl(originUrl);
        }
        if (!this.shouldResolve(sourceType, allowRefresh, state.getResolvedAt(), mediaUrl, originUrl)) {
            if (state.getResolvedAt() == null) {
                state.setResolvedAt(System.currentTimeMillis());
            }
            return;
        }
        try {
            AvMediaResolveResult result = avMediaResolveService.resolve(originUrl, state.getTitle());
            if (result != null) {
                if (!"".equals(this.safeTrim(result.getMediaUrl()))) {
                    state.setMediaUrl(result.getMediaUrl());
                }
                if (!"".equals(this.safeTrim(result.getOriginUrl()))) {
                    state.setOriginUrl(result.getOriginUrl());
                }
                if (!"".equals(this.safeTrim(result.getSourceType()))) {
                    state.setSourceType(result.getSourceType());
                }
                if (!"".equals(this.safeTrim(result.getTitle()))) {
                    state.setTitle(result.getTitle());
                }
                if (!"".equals(this.safeTrim(result.getMediaId()))) {
                    state.setMediaId(result.getMediaId());
                }
                if (!"".equals(this.safeTrim(result.getPosterUrl()))) {
                    state.setPosterUrl(result.getPosterUrl());
                }
                state.setResolvedAt(result.getResolvedAt());
            }
        } catch (IllegalArgumentException e) {
            if (!allowRefresh) {
                throw e;
            }
        }
    }

    private boolean shouldResolve(String sourceType, boolean allowRefresh, Long resolvedAt, String mediaUrl, String originUrl) {
        if (!"bilibili".equalsIgnoreCase(this.safeTrim(sourceType))) {
            return false;
        }
        if (resolvedAt == null) {
            return true;
        }
        if (!allowRefresh) {
            return false;
        }
        long age = System.currentTimeMillis() - resolvedAt;
        return age > 90L * 60L * 1000L;
    }

    private String guessSourceType(String originUrl, String mediaUrl) {
        String current = (this.safeTrim(originUrl) + " " + this.safeTrim(mediaUrl)).toLowerCase();
        if (current.contains("bilibili.com") || current.contains("b23.tv") || current.contains("bili2233.cn")) {
            return "bilibili";
        }
        if (current.startsWith("/av/media/files/") || current.contains("/av/media/files/")) {
            return "upload";
        }
        if (current.endsWith(".m3u8")) {
            return "stream";
        }
        return "direct";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
