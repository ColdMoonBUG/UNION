package com.scoder.jusic.repository.memory;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.ConfigRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Memory implementation for config storage.
 */
@Repository
@Profile("memory")
public class MemoryConfigRepository implements ConfigRepository {

    private final MemoryRuntimeStore store;
    private final JusicProperties jusicProperties;

    public MemoryConfigRepository(MemoryRuntimeStore store, JusicProperties jusicProperties) {
        this.store = store;
        this.jusicProperties = jusicProperties;
    }

    @Override
    public Long destroy() {
        long size = 0L;
        size += this.store.config.size();
        size += this.store.roomConfig.size();
        this.store.config.clear();
        this.store.roomConfig.clear();
        return size;
    }

    @Override
    public Long destroy(String roomId) {
        return this.store.roomConfig.remove(this.normalize(roomId)) == null ? 0L : 1L;
    }

    @Override
    public void initialize() {
        this.put(this.key("role_root_password"), this.jusicProperties.getRoleRootPassword());
        this.put(this.key("role_admin_password"), this.jusicProperties.getRoleAdminPassword());
        this.put(this.key("vote_skip_rate"), this.jusicProperties.getVoteRate());
    }

    @Override
    public Object get(Object hashKey) {
        return this.store.globalConfig().get(this.key(hashKey));
    }

    @Override
    public void put(Object hashKey, Object value) {
        this.store.globalConfig().put(this.key(hashKey), value);
    }

    @Override
    public void putAll(Map<String, Object> map) {
        if (map != null) {
            this.store.globalConfig().putAll(map);
        }
    }

    @Override
    public Object getRoom(String roomId, Object hashKey) {
        return this.store.roomConfig(this.normalize(roomId)).get(this.key(hashKey));
    }

    @Override
    public void putRoom(String roomId, Object hashKey, Object value) {
        this.store.roomConfig(this.normalize(roomId)).put(this.key(hashKey), value);
    }

    @Override
    public void putAllRoom(String roomId, Map<String, Object> map) {
        if (map != null) {
            this.store.roomConfig(this.normalize(roomId)).putAll(map);
        }
    }

    @Override
    public String getPassword(String role) {
        Object value = this.get(role);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public void setPassword(String role, String password) {
        this.put(role, password);
    }

    @Override
    public void initRootPassword() {
        this.setPassword(this.key("role_root_password"), this.jusicProperties.getRoleRootPassword());
    }

    @Override
    public void initAdminPassword() {
        this.setPassword(this.key("role_admin_password"), this.jusicProperties.getRoleAdminPassword());
    }

    @Override
    public String getRootPassword() {
        return this.getPassword(this.key("role_root_password"));
    }

    @Override
    public String getAdminPassword() {
        return this.getPassword(this.key("role_admin_password"));
    }

    @Override
    public Long getLastMusicDuration(String roomId) {
        return this.getLongRoom(roomId, this.key("last_music_duration"));
    }

    @Override
    public void setLastMusicDuration(String roomId, Long duration) {
        this.putRoom(roomId, this.key("last_music_duration"), duration);
    }

    @Override
    public Long getLastMusicPushTime(String roomId) {
        return this.getLongRoom(roomId, this.key("last_music_push_time"));
    }

    @Override
    public void setLastMusicPushTime(String roomId, Long pushTime) {
        this.putRoom(roomId, this.key("last_music_push_time"), pushTime);
    }

    @Override
    public void setLastMusicPushTimeAndDuration(String roomId, Long pushTime, Long duration) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(this.key("last_music_push_time"), pushTime);
        map.put(this.key("last_music_duration"), duration);
        this.putAllRoom(roomId, map);
    }

    @Override
    public Boolean getPushSwitch(String roomId) {
        return this.getBooleanRoom(roomId, this.key("switch_music_push"));
    }

    @Override
    public void setPushSwitch(String roomId, boolean pushSwitch) {
        this.putRoom(roomId, this.key("switch_music_push"), pushSwitch);
    }

    @Override
    public Float getVoteRate(String roomId) {
        Object value = this.getRoom(roomId, this.key("vote_skip_rate"));
        if (value == null) {
            value = this.get(this.key("vote_skip_rate"));
        }
        if (value == null) {
            return this.jusicProperties.getVoteRate();
        }
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.valueOf(value.toString());
    }

    @Override
    public Boolean getEnableSwitch(String roomId) {
        return this.getBooleanRoom(roomId, this.key("switch_music_enable"));
    }

    @Override
    public void setEnableSwitch(String roomId, boolean enableSwitch) {
        this.putRoom(roomId, this.key("switch_music_enable"), enableSwitch);
    }

    @Override
    public Boolean getEnableSearch(String roomId) {
        return this.getBooleanRoom(roomId, this.key("search_music_enable"));
    }

    @Override
    public void setEnableSearch(String roomId, boolean enableSearch) {
        this.putRoom(roomId, this.key("search_music_enable"), enableSearch);
    }

    @Override
    public Boolean getGoodModel(String roomId) {
        return this.getBooleanRoom(roomId, this.key("good_model"));
    }

    @Override
    public void setGoodModel(String roomId, boolean goodModel) {
        this.putRoom(roomId, this.key("good_model"), goodModel);
    }

    @Override
    public String getPlayMode(String roomId) {
        Object value = this.getRoom(roomId, this.key("play_mode"));
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public void setPlayMode(String roomId, String playMode) {
        this.putRoom(roomId, this.key("play_mode"), playMode);
    }

    private Long getLongRoom(String roomId, String key) {
        Object value = this.getRoom(roomId, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Boolean getBooleanRoom(String roomId, String key) {
        Object value = this.getRoom(roomId, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private String normalize(String roomId) {
        return roomId == null || roomId.trim().isEmpty() ? "default" : roomId.trim();
    }

    private String key(String suffix) {
        return suffix;
    }

    private String key(Object suffix) {
        return String.valueOf(suffix);
    }
}
