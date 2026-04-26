package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPlayingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author H
 */
@Repository
public class MusicPlayingRepositoryImpl implements MusicPlayingRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void destroy() {
        Set keys = redisTemplate.keys(redisKeys.getPlayingList() + "_*");
        if (keys != null) {
            for (Object key : keys) {
                redisTemplate.opsForList().trim(key, 1, 0);
            }
        }
    }

    @Override
    public void destroy(String roomId) {
        redisTemplate.opsForList().trim(redisKeys.getPlayingList(roomId), 1, 0);
    }

    @Override
    public Long leftPush(String roomId, Music pick) {
        return redisTemplate.opsForList()
                .leftPush(redisKeys.getPlayingList(roomId), pick);
    }

    @Override
    public Music pickToPlaying(String roomId) {
        return (Music) redisTemplate.opsForList()
                .rightPopAndLeftPush(redisKeys.getPickList(roomId), redisKeys.getPlayingList(roomId));
    }

    @Override
    public void keepTheOne(String roomId) {
        redisTemplate.opsForList()
                .trim(redisKeys.getPlayingList(roomId), 0, 0);
    }

    @Override
    public Music getPlaying(String roomId) {
        return (Music) redisTemplate.opsForList()
                .index(redisKeys.getPlayingList(roomId), 0);
    }

}
