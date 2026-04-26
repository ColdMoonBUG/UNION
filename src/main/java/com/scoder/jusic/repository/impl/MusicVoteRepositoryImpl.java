package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.MusicVoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author H
 */
@Repository
@Slf4j
public class MusicVoteRepositoryImpl implements MusicVoteRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Long destroy() {
        Long deleted = 0L;
        Set keys = redisTemplate.keys(redisKeys.getSkipSet() + "_*");
        if (keys != null) {
            for (Object key : keys) {
                Set members = redisTemplate.opsForSet().members(key);
                if (members != null && members.size() > 0) {
                    deleted += redisTemplate.opsForSet().remove(key, members.toArray());
                }
            }
        }
        return deleted;
    }

    @Override
    public Long destroy(String roomId) {
        Set members = this.members(roomId);
        if (members == null || members.isEmpty()) {
            return 0L;
        }
        return redisTemplate.opsForSet().remove(redisKeys.getSkipSet(roomId), members.toArray());
    }

    @Override
    public Long add(String roomId, Object... value) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getSkipSet(roomId), value);
    }

    @Override
    public Long size(String roomId) {
        return redisTemplate.opsForSet()
                .size(redisKeys.getSkipSet(roomId));
    }

    @Override
    public void reset(String roomId) {
        Set members = this.members(roomId);
        if (null != members && members.size() > 0) {
            redisTemplate.opsForSet()
                    .remove(redisKeys.getSkipSet(roomId), members.toArray());
        }
    }

    @Override
    public Set members(String roomId) {
        return redisTemplate.opsForSet()
                .members(redisKeys.getSkipSet(roomId));
    }

}
