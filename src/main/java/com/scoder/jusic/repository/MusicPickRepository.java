package com.scoder.jusic.repository;

import com.scoder.jusic.model.Music;

import java.util.List;

/**
 * @author H
 */
public interface MusicPickRepository {

    void destroy();

    void destroy(String roomId);

    Long leftPush(String roomId, Music pick);

    Long leftPushAll(String roomId, Object... value);

    Long rightPushAll(String roomId, Object... value);

    Long size(String roomId);

    void reset(String roomId);

    List<Music> getPickMusicList(String roomId);
}
