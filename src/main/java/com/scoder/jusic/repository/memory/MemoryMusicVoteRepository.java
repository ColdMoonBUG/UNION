package com.scoder.jusic.repository.memory;

import com.scoder.jusic.repository.MusicVoteRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Memory implementation for skip votes.
 */
@Repository
@Profile("memory")
public class MemoryMusicVoteRepository implements MusicVoteRepository {

    private final MemoryRuntimeStore store;

    public MemoryMusicVoteRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public Long destroy() {
        long size = 0L;
        for (Set<String> votes : this.store.voteSets.values()) {
            size += votes.size();
        }
        this.store.voteSets.clear();
        return size;
    }

    @Override
    public Long destroy(String roomId) {
        Set<String> votes = this.store.voteSets.remove(roomId == null ? "default" : roomId.trim());
        return votes == null ? 0L : (long) votes.size();
    }

    @Override
    public Long add(String roomId, Object... value) {
        Set<String> votes = this.store.votes(roomId);
        long added = 0L;
        if (value != null) {
            for (Object item : value) {
                if (item != null && votes.add(String.valueOf(item))) {
                    added++;
                }
            }
        }
        return added;
    }

    @Override
    public Long size(String roomId) {
        return (long) this.store.votes(roomId).size();
    }

    @Override
    public void reset(String roomId) {
        this.store.votes(roomId).clear();
    }

    @Override
    public Set members(String roomId) {
        return new LinkedHashSet<>(this.store.votes(roomId));
    }
}
