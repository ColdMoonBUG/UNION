package com.scoder.jusic.service;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.common.page.Page;
import com.scoder.jusic.model.Music;

import java.util.LinkedList;
import java.util.List;

/**
 * @author H
 */
public interface MusicService {

    Music toPick(String sessionId, Music request);

    Music toPick(String roomId, String sessionId, Music request);

    Music musicSwitch(String roomId);

    LinkedList<Music> getPickList(String roomId);

    List<Music> getPickListNoPlaying(String roomId);

    LinkedList<Music> getSortedPickList(String roomId, List<Music> musicList);

    Music getPlaying(String roomId);

    Long modifyPickOrder(String roomId, LinkedList<Music> musicList);

    Long vote(String sessionId);

    Long vote(String roomId, String sessionId);

    Long getVoteCount(String roomId);

    Music getMusic(String keyword);

    Music getQQMusic(String keyword);

    Music getLZMusic(Integer index);

    Music getQQMusicById(String id);

    Music getMGMusic(String keyword);

    Music getMGMusicById(String id);

    String getMusicUrl(String musicId);

    String getQQMusicUrl(String musicId);

    String getMGMusicUrl(String musicId, String musicName);

    boolean deletePickMusic(String roomId, Music music);

    void topPickMusic(String roomId, Music music);

    Long black(String id);

    Long unblack(String id);

    boolean isBlack(String id);

    boolean isPicked(String roomId, String id);

    Object[] getMusicById(String roomId, String id);

    Page<List<Music>> search(Music music, HulkPage hulkPage);

    boolean clearPlayList(String roomId);

    String showBlackMusic();
}
