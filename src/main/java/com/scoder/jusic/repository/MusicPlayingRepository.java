package com.scoder.jusic.repository;

import com.scoder.jusic.model.Music;

/**
 * @author H
 */
public interface MusicPlayingRepository {

    void destroy();

    void destroy(String roomId);

    Long leftPush(String roomId, Music pick);

    Music pickToPlaying(String roomId);

    void keepTheOne(String roomId);

    Music getPlaying(String roomId);
}
