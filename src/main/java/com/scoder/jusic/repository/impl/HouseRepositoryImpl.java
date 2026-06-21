package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.repository.HouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author H
 */
@Repository
@Profile("redis")
public class HouseRepositoryImpl implements HouseRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void destroy() {
        Set keys = redisTemplate.opsForHash().keys(redisKeys.getHouseHash());
        if (keys != null && keys.size() > 0) {
            List<Object> removeKeys = new ArrayList<>();
            for (Object key : keys) {
                Object value = redisTemplate.opsForHash().get(redisKeys.getHouseHash(), key);
                if (!(value instanceof House) || !Boolean.TRUE.equals(((House) value).getEnableStatus())) {
                    removeKeys.add(key);
                }
            }
            if (!removeKeys.isEmpty()) {
                redisTemplate.opsForHash().delete(redisKeys.getHouseHash(), removeKeys.toArray());
            }
        }
    }

    @Override
    public House get(String houseId) {
        return (House) redisTemplate.opsForHash().get(redisKeys.getHouseHash(), houseId);
    }

    @Override
    public void set(House house) {
        redisTemplate.opsForHash().put(redisKeys.getHouseHash(), house.getId(), house);
    }

    @Override
    public Long remove(String houseId) {
        return redisTemplate.opsForHash().delete(redisKeys.getHouseHash(), houseId);
    }

    @Override
    public List<House> list() {
        List<House> houses = new ArrayList<>();
        List values = redisTemplate.opsForHash().values(redisKeys.getHouseHash());
        if (values != null) {
            for (Object value : values) {
                if (value instanceof House) {
                    houses.add((House) value);
                }
            }
        }
        houses.sort(Comparator.comparing(House::getId));
        return houses;
    }
}
