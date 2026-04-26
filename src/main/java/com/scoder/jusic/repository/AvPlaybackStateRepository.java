package com.scoder.jusic.repository;

import com.scoder.jusic.model.AvPlaybackState;

/**
 * @author H
 */
public interface AvPlaybackStateRepository {

    void destroy();

    AvPlaybackState get(String roomId);

    void set(String roomId, AvPlaybackState state);

    Long remove(String roomId);

}
