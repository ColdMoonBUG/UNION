package com.scoder.jusic.repository.memory;

import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPlayingRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;

/**
 * Memory implementation for currently playing music.
 */
@Repository
@Profile("memory")
public class MemoryMusicPlayingRepository implements MusicPlayingRepository {

    private final MemoryRuntimeStore store;

    public MemoryMusicPlayingRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public void destroy() {
        this.store.playing.clear();
    }

    @Override
    public void destroy(String roomId) {
        this.store.setPlaying(roomId, null);
    }

    @Override
    public Long leftPush(String roomId, Music pick) {
        this.store.setPlaying(roomId, pick);
        return pick == null ? 0L : 1L;
    }

    @Override
    public Music pickToPlaying(String roomId) {
        LinkedList<Music> pickList = this.store.pickList(roomId);
        synchronized (pickList) {
            if (pickList.isEmpty()) {
                this.store.setPlaying(roomId, null);
                return null;
            }
            Music music = pickList.removeLast();
            this.store.setPlaying(roomId, music);
            return music;
        }
    }

    @Override
    public void keepTheOne(String roomId) {
        Music music = this.store.playing(roomId);
        if (music != null) {
            this.store.setPlaying(roomId, music);
        }
    }

    @Override
    public Music getPlaying(String roomId) {
        return this.store.playing(roomId);
    }
}
