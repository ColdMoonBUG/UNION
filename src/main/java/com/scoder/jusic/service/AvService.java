package com.scoder.jusic.service;

import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.model.AvSignal;

/**
 * @author H
 */
public interface AvService {

    void signal(String sessionId, AvSignal signal);

    AvPlaybackState updatePlaybackState(String sessionId, AvPlaybackState state);

    AvPlaybackState getPlaybackState(String roomId);

    void clearPlaybackState(String roomId);
}
