package com.scoder.jusic.repository.memory;

import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPickRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;

/**
 * Memory implementation for pick lists.
 */
@Repository
@Profile("memory")
public class MemoryMusicPickRepository implements MusicPickRepository {

    private final MemoryRuntimeStore store;

    public MemoryMusicPickRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public void destroy() {
        this.store.pickLists.clear();
    }

    @Override
    public void destroy(String roomId) {
        this.store.pickLists.remove(this.normalize(roomId));
    }

    @Override
    public Long leftPush(String roomId, Music pick) {
        LinkedList<Music> list = this.store.pickList(roomId);
        synchronized (list) {
            list.addFirst(pick);
            return (long) list.size();
        }
    }

    @Override
    public Long leftPushAll(String roomId, Object... value) {
        LinkedList<Music> list = this.store.pickList(roomId);
        long count = 0L;
        synchronized (list) {
            if (value != null) {
                if (value.length == 1 && value[0] instanceof Collection) {
                    List<Music> batch = new LinkedList<>();
                    for (Object item : (Collection<?>) value[0]) {
                        if (item instanceof Music) {
                            batch.add((Music) item);
                        }
                    }
                    for (int i = batch.size() - 1; i >= 0; i--) {
                        list.addFirst(batch.get(i));
                        count++;
                    }
                } else {
                    for (Object item : value) {
                        if (item instanceof Music) {
                            list.addFirst((Music) item);
                            count++;
                        }
                    }
                }
            }
            return count;
        }
    }

    @Override
    public Long rightPushAll(String roomId, Object... value) {
        LinkedList<Music> list = this.store.pickList(roomId);
        long count = 0L;
        synchronized (list) {
            if (value != null) {
                if (value.length == 1 && value[0] instanceof Collection) {
                    for (Object item : (Collection<?>) value[0]) {
                        if (item instanceof Music) {
                            list.addLast((Music) item);
                            count++;
                        }
                    }
                } else {
                    for (Object item : value) {
                        if (item instanceof Music) {
                            list.addLast((Music) item);
                            count++;
                        }
                    }
                }
            }
            return count;
        }
    }

    @Override
    public Long size(String roomId) {
        LinkedList<Music> list = this.store.pickList(roomId);
        synchronized (list) {
            return (long) list.size();
        }
    }

    @Override
    public void reset(String roomId) {
        LinkedList<Music> list = this.store.pickList(roomId);
        synchronized (list) {
            list.clear();
        }
    }

    @Override
    public List<Music> getPickMusicList(String roomId) {
        LinkedList<Music> list = this.store.pickList(roomId);
        synchronized (list) {
            return new LinkedList<>(list);
        }
    }

    private String normalize(String roomId) {
        return roomId == null || roomId.trim().isEmpty() ? "default" : roomId.trim();
    }
}
