package com.scoder.jusic.service;

import com.scoder.jusic.model.House;
import com.scoder.jusic.model.HouseRequest;
import com.scoder.jusic.model.Notice;
import com.scoder.jusic.model.User;

import java.util.List;

/**
 * @author H
 */
public interface HouseService {

    List<House> list();

    List<House> listWithPopulation();

    House get(String houseId);

    House getRaw(String houseId);

    String create(HouseRequest request);

    House enter(String houseId, String password);

    String bindSession(String sessionId, String houseId);

    List<User> listUsers(String houseId);

    Notice updateAnnouncement(String houseId, Notice notice);

    House updateRetainStatus(String houseId, boolean enableStatus);
}
