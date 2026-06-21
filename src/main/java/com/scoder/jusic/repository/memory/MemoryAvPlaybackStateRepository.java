package com.scoder.jusic.repository.memory;

import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.repository.AvPlaybackStateRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Memory implementation for AV playback state.
 */
@Repository
@Profile("memory")
public class MemoryAvPlaybackStateRepository implements AvPlaybackStateRepository {

    private final MemoryRuntimeStore store;

    public MemoryAvPlaybackStateRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public void destroy() {
        this.store.avStates.clear();
    }

    @Override
    public AvPlaybackState get(String roomId) {
        return this.store.avStates.get(this.normalize(roomId));
    }

    @Override
    public void set(String roomId, AvPlaybackState state) {
        if (state == null) {
            this.store.avStates.remove(this.normalize(roomId));
            return;
        }
        this.store.avStates.put(this.normalize(roomId), state);
    }

    @Override
    public Long remove(String roomId) {
        return this.store.avStates.remove(this.normalize(roomId)) == null ? 0L : 1L;
    }

    private String normalize(String roomId) {
        return roomId == null || roomId.trim().isEmpty() ? "default" : roomId.trim();
    }
}
