package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author H
 */
@Repository
public class ConfigRepositoryImpl implements ConfigRepository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Long destroy() {
        Long deleted = this.destroyHash(redisKeys.getConfigHash()) + this.destroyHash(redisKeys.getRoomConfigHash());
        Set roomKeys = redisTemplate.keys(redisKeys.getRoomConfigHash() + "_*");
        if (roomKeys != null) {
            for (Object roomKey : roomKeys) {
                deleted += this.destroyHash(roomKey);
            }
        }
        return deleted;
    }

    @Override
    public Long destroy(String roomId) {
        return this.destroyHash(redisKeys.getRoomConfigHash(roomId));
    }

    @Override
    public void initialize() {
        this.put(redisKeys.getRedisRoleRoot(), jusicProperties.getRoleRootPassword());
        this.put(redisKeys.getRedisRoleAdmin(), jusicProperties.getRoleAdminPassword());
        this.put(redisKeys.getVoteSkipRate(), jusicProperties.getVoteRate());
    }

    @Override
    public Object get(Object hashKey) {
        return redisTemplate.opsForHash()
                .get(redisKeys.getConfigHash(), hashKey);
    }

    @Override
    public void put(Object hashKey, Object value) {
        redisTemplate.opsForHash()
                .put(redisKeys.getConfigHash(), hashKey, value);
    }

    @Override
    public void putAll(Map<String, Object> map) {
        redisTemplate.opsForHash()
                .putAll(redisKeys.getConfigHash(), map);
    }

    @Override
    public Object getRoom(String roomId, Object hashKey) {
        return redisTemplate.opsForHash()
                .get(redisKeys.getRoomConfigHash(roomId), hashKey);
    }

    @Override
    public void putRoom(String roomId, Object hashKey, Object value) {
        redisTemplate.opsForHash()
                .put(redisKeys.getRoomConfigHash(roomId), hashKey, value);
    }

    @Override
    public void putAllRoom(String roomId, Map<String, Object> map) {
        redisTemplate.opsForHash()
                .putAll(redisKeys.getRoomConfigHash(roomId), map);
    }

    @Override
    public String getPassword(String role) {
        return (String) this.get(role);
    }

    @Override
    public void setPassword(String role, String password) {
        this.put(role, password);
    }

    @Override
    public void initRootPassword() {
        this.setPassword(redisKeys.getRedisRoleRoot(), jusicProperties.getRoleRootPassword());
    }

    @Override
    public void initAdminPassword() {
        this.setPassword(redisKeys.getRedisRoleAdmin(), jusicProperties.getRoleAdminPassword());
    }

    @Override
    public String getRootPassword() {
        return this.getPassword(redisKeys.getRedisRoleRoot());
    }

    @Override
    public String getAdminPassword() {
        return this.getPassword(redisKeys.getRedisRoleAdmin());
    }

    @Override
    public Long getLastMusicDuration(String roomId) {
        return this.getLongRoom(roomId, redisKeys.getLastMusicDuration());
    }

    @Override
    public void setLastMusicDuration(String roomId, Long duration) {
        this.putRoom(roomId, redisKeys.getLastMusicDuration(), duration);
    }

    @Override
    public Long getLastMusicPushTime(String roomId) {
        return this.getLongRoom(roomId, redisKeys.getLastMusicPushTime());
    }

    @Override
    public void setLastMusicPushTime(String roomId, Long pushTime) {
        this.putRoom(roomId, redisKeys.getLastMusicPushTime(), pushTime);
    }

    @Override
    public void setLastMusicPushTimeAndDuration(String roomId, Long pushTime, Long duration) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(redisKeys.getLastMusicPushTime(), pushTime);
        map.put(redisKeys.getLastMusicDuration(), duration);
        this.putAllRoom(roomId, map);
    }

    @Override
    public Boolean getPushSwitch(String roomId) {
        return this.getBooleanRoom(roomId, redisKeys.getSwitchMusicPush());
    }

    @Override
    public void setPushSwitch(String roomId, boolean pushSwitch) {
        this.putRoom(roomId, redisKeys.getSwitchMusicPush(), pushSwitch);
    }

    @Override
    public Float getVoteRate(String roomId) {
        Object value = this.getRoom(roomId, redisKeys.getVoteSkipRate());
        if (value == null) {
            value = this.get(redisKeys.getVoteSkipRate());
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Double) {
            return ((Double) value).floatValue();
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.valueOf(value.toString());
    }

    @Override
    public Boolean getEnableSwitch(String roomId) {
        return this.getBooleanRoom(roomId, redisKeys.getSwitchMusicEnable());
    }

    @Override
    public void setEnableSwitch(String roomId, boolean enableSwitch) {
        this.putRoom(roomId, redisKeys.getSwitchMusicEnable(), enableSwitch);
    }

    @Override
    public Boolean getEnableSearch(String roomId) {
        return this.getBooleanRoom(roomId, redisKeys.getSearchMusicEnable());
    }

    @Override
    public void setEnableSearch(String roomId, boolean enableSearch) {
        this.putRoom(roomId, redisKeys.getSearchMusicEnable(), enableSearch);
    }

    @Override
    public Boolean getGoodModel(String roomId) {
        return this.getBooleanRoom(roomId, redisKeys.getGoodModel());
    }

    @Override
    public void setGoodModel(String roomId, boolean goodModel) {
        this.putRoom(roomId, redisKeys.getGoodModel(), goodModel);
    }

    @Override
    public String getPlayMode(String roomId) {
        Object value = this.getRoom(roomId, redisKeys.getPlayMode());
        return value == null ? null : value.toString();
    }

    @Override
    public void setPlayMode(String roomId, String playMode) {
        this.putRoom(roomId, redisKeys.getPlayMode(), playMode);
    }

    private Long destroyHash(Object key) {
        Set keys = redisTemplate.opsForHash().keys(key);
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return redisTemplate.opsForHash().delete(key, keys.toArray());
    }

    private Long getLongRoom(String roomId, String key) {
        Object value = this.getRoom(roomId, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Boolean getBooleanRoom(String roomId, String key) {
        Object value = this.getRoom(roomId, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(value.toString());
    }
}
