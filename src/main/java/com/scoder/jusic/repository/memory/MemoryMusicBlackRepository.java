package com.scoder.jusic.repository.memory;

import com.scoder.jusic.repository.MusicBlackRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Memory implementation for music blacklist.
 */
@Repository
@Profile("memory")
public class MemoryMusicBlackRepository implements MusicBlackRepository {

    private final MemoryRuntimeStore store;

    public MemoryMusicBlackRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public boolean isMember(String id) {
        return id != null && this.store.blackMusicSet().contains(id);
    }

    @Override
    public Long add(String value) {
        return value != null && this.store.blackMusicSet().add(value) ? 1L : 0L;
    }

    @Override
    public Long remove(String id) {
        return id != null && this.store.blackMusicSet().remove(id) ? 1L : 0L;
    }

    @Override
    public Set showBlackList() {
        return new LinkedHashSet<>(this.store.blackMusicSet());
    }
}
