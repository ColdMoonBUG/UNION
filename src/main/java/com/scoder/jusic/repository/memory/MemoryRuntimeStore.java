package com.scoder.jusic.repository.memory;

import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory fallback store used when Redis is unavailable.
 */
@Component
public class MemoryRuntimeStore {

    public final Map<String, User> sessions = new ConcurrentHashMap<>();
    public final Map<String, User> blackSessions = new ConcurrentHashMap<>();
    public final Map<String, House> houses = new ConcurrentHashMap<>();
    public final Map<String, LinkedList<Music>> pickLists = new ConcurrentHashMap<>();
    public final Map<String, Music> playing = new ConcurrentHashMap<>();
    public final Map<String, Set<String>> voteSets = new ConcurrentHashMap<>();
    public final Map<String, Set<String>> blackMusics = new ConcurrentHashMap<>();
    public final Map<String, Map<String, Object>> config = new ConcurrentHashMap<>();
    public final Map<String, Map<String, Object>> roomConfig = new ConcurrentHashMap<>();
    public final Map<String, AvPlaybackState> avStates = new ConcurrentHashMap<>();
    public final List<String> defaultMusicIds = new CopyOnWriteArrayList<>();

    public LinkedList<Music> pickList(String roomId) {
        return pickLists.computeIfAbsent(normalize(roomId), key -> new LinkedList<>());
    }

    public Music playing(String roomId) {
        return playing.get(normalize(roomId));
    }

    public void setPlaying(String roomId, Music music) {
        String key = normalize(roomId);
        if (music == null) {
            playing.remove(key);
        } else {
            playing.put(key, music);
        }
    }

    public Set<String> votes(String roomId) {
        return voteSets.computeIfAbsent(normalize(roomId), key -> new CopyOnWriteArraySet<>());
    }

    public Set<String> blackMusicSet() {
        return blackMusics.computeIfAbsent("default", key -> new CopyOnWriteArraySet<>());
    }

    public Map<String, Object> config(String roomId) {
        return roomConfig.computeIfAbsent(normalize(roomId), key -> new ConcurrentHashMap<>());
    }

    public Map<String, Object> globalConfig() {
        return config.computeIfAbsent("global", key -> new ConcurrentHashMap<>());
    }

    public Map<String, Object> roomConfig(String roomId) {
        return roomConfig.computeIfAbsent(normalize(roomId), key -> new ConcurrentHashMap<>());
    }

    public void clearRoom(String roomId) {
        String key = normalize(roomId);
        pickLists.remove(key);
        playing.remove(key);
        voteSets.remove(key);
        roomConfig.remove(key);
        avStates.remove(key);
    }

    public List<House> listHouses() {
        return new ArrayList<>(houses.values());
    }

    public void clearAll() {
        sessions.clear();
        blackSessions.clear();
        houses.clear();
        pickLists.clear();
        playing.clear();
        voteSets.clear();
        blackMusics.clear();
        config.clear();
        roomConfig.clear();
        avStates.clear();
    }

    private String normalize(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return "default";
        }
        return roomId.trim();
    }
}
