package com.scoder.jusic.repository.memory;

import com.scoder.jusic.model.House;
import com.scoder.jusic.repository.HouseRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Memory implementation for room metadata.
 */
@Repository
@Profile("memory")
public class MemoryHouseRepository implements HouseRepository {

    private final MemoryRuntimeStore store;

    public MemoryHouseRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public void destroy() {
        List<String> removeKeys = new ArrayList<>();
        for (House house : this.store.houses.values()) {
            if (house == null || !Boolean.TRUE.equals(house.getEnableStatus())) {
                removeKeys.add(house == null ? null : house.getId());
            }
        }
        for (String key : removeKeys) {
            if (key != null) {
                this.store.houses.remove(key);
            }
        }
    }

    @Override
    public House get(String houseId) {
        return this.store.houses.get(this.normalize(houseId));
    }

    @Override
    public void set(House house) {
        if (house != null && house.getId() != null) {
            this.store.houses.put(this.normalize(house.getId()), house);
        }
    }

    @Override
    public Long remove(String houseId) {
        return this.store.houses.remove(this.normalize(houseId)) == null ? 0L : 1L;
    }

    @Override
    public List<House> list() {
        List<House> houses = new ArrayList<>(this.store.houses.values());
        houses.sort(Comparator.comparing(House::getId));
        return houses;
    }

    private String normalize(String roomId) {
        return roomId == null || roomId.trim().isEmpty() ? "default" : roomId.trim();
    }
}
