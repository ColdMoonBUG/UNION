package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.AvPlaybackState;
import com.scoder.jusic.repository.AvPlaybackStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.Set;

/**
 * @author H
 */
@Repository
@Profile("redis")
public class AvPlaybackStateRepositoryImpl implements AvPlaybackStateRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void destroy() {
        Set keys = redisTemplate.opsForHash().keys(redisKeys.getAvPlaybackHash());
        if (keys != null && keys.size() > 0) {
            redisTemplate.opsForHash().delete(redisKeys.getAvPlaybackHash(), keys.toArray());
        }
    }

    @Override
    public AvPlaybackState get(String roomId) {
        return (AvPlaybackState) redisTemplate.opsForHash().get(redisKeys.getAvPlaybackHash(), roomId);
    }

    @Override
    public void set(String roomId, AvPlaybackState state) {
        redisTemplate.opsForHash().put(redisKeys.getAvPlaybackHash(), roomId, state);
    }

    @Override
    public Long remove(String roomId) {
        return redisTemplate.opsForHash().delete(redisKeys.getAvPlaybackHash(), roomId);
    }
}
