package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * @author H
 */
@Repository
public class MusicPickRepositoryImpl implements MusicPickRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void destroy() {
        Set keys = redisTemplate.keys(redisKeys.getPickList() + "_*");
        if (keys != null) {
            for (Object key : keys) {
                redisTemplate.opsForList().trim(key, 1, 0);
            }
        }
    }

    @Override
    public void destroy(String roomId) {
        redisTemplate.opsForList().trim(redisKeys.getPickList(roomId), 1, 0);
    }

    @Override
    public Long leftPush(String roomId, Music pick) {
        return redisTemplate.opsForList()
                .leftPush(redisKeys.getPickList(roomId), pick);
    }

    @Override
    public Long leftPushAll(String roomId, Object... value) {
        return redisTemplate.opsForList()
                .leftPushAll(redisKeys.getPickList(roomId), value);
    }

    @Override
    public Long rightPushAll(String roomId, Object... value) {
        return redisTemplate.opsForList()
                .rightPushAll(redisKeys.getPickList(roomId), value);
    }

    @Override
    public Long size(String roomId) {
        return redisTemplate.opsForList()
                .size(redisKeys.getPickList(roomId));
    }

    /**
     * clear the pick list.
     */
    @Override
    public void reset(String roomId) {
        redisTemplate.opsForList()
                .trim(redisKeys.getPickList(roomId), 1, 0);
    }

    /**
     * get all pick music.
     *
     * @return LinkedList
     */
    @Override
    public List<Music> getPickMusicList(String roomId) {
        Long size = this.size(roomId);
        size = size == null ? 0 : size;
        return (List<Music>) redisTemplate.opsForList()
                .range(redisKeys.getPickList(roomId), 0, size);
    }

}
