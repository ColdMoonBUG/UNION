package com.scoder.jusic.repository;

import com.scoder.jusic.model.House;

import java.util.List;

/**
 * @author H
 */
public interface HouseRepository {

    void destroy();

    House get(String houseId);

    void set(House house);

    Long remove(String houseId);

    List<House> list();

}
