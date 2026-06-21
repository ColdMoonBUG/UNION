package com.scoder.jusic.repository.memory;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.MusicDefaultRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Memory implementation for default music ids.
 */
@Repository
@Profile("memory")
public class MemoryMusicDefaultRepository implements MusicDefaultRepository {

    private final MemoryRuntimeStore store;

    public MemoryMusicDefaultRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public Long destroy() {
        long size = this.store.defaultMusicIds.size();
        this.store.defaultMusicIds.clear();
        return size;
    }

    @Override
    public Long initialize() {
        this.store.defaultMusicIds.clear();
        List<String> defaults = JusicProperties.getDefaultListForRepository();
        if (defaults != null) {
            this.store.defaultMusicIds.addAll(defaults);
        }
        return (long) this.store.defaultMusicIds.size();
    }

    @Override
    public Long size() {
        return (long) this.store.defaultMusicIds.size();
    }

    @Override
    public String randomMember() {
        if (this.store.defaultMusicIds.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(this.store.defaultMusicIds.size());
        return this.store.defaultMusicIds.get(index);
    }

    @Override
    public Long add(String[] value) {
        if (value == null || value.length == 0) {
            return 0L;
        }
        long added = 0L;
        for (String v : value) {
            if (v != null && !v.trim().isEmpty()) {
                this.store.defaultMusicIds.add(v.trim());
                added++;
            }
        }
        return added;
    }
}
