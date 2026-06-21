package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.model.AvSignal;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.service.AvService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * @author H
 */
@Controller
@Slf4j
public class AvController {

    @Autowired
    private AvService avService;
    @Autowired
    private SessionService sessionService;

    @MessageMapping("/av/signal")
    public void signal(AvSignal signal, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        try {
            avService.signal(sessionId, signal);
        } catch (IllegalArgumentException e) {
            sessionService.send(sessionId, MessageType.AV_SIGNAL, Response.failure((Object) null, e.getMessage()));
        }
    }

    @MessageMapping("/av/playback/state")
    public void playbackState(AvPlaybackState state, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        try {
            AvPlaybackState currentState = avService.updatePlaybackState(sessionId, state);
            sessionService.sendRoom(currentState.getRoomId(), MessageType.AV_PLAYBACK_STATE, Response.success(currentState, "播放状态"));
        } catch (IllegalArgumentException e) {
            sessionService.send(sessionId, MessageType.AV_PLAYBACK_STATE, Response.failure((AvPlaybackState) null, e.getMessage()));
        }
    }

    @MessageMapping("/av/playback/get")
    public void getPlaybackState(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String roomId = sessionService.getRoomId(sessionId);
        AvPlaybackState state = avService.getPlaybackState(roomId);
        sessionService.send(sessionId, MessageType.AV_PLAYBACK_STATE, Response.success(state, "播放状态"));
    }
}
