package com.scoder.jusic.service.imp;

import com.scoder.jusic.model.House;
import com.scoder.jusic.model.HouseRequest;
import com.scoder.jusic.model.Notice;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.HouseRepository;
import com.scoder.jusic.service.HouseService;
import com.scoder.jusic.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author H
 */
@Service
public class HouseServiceImpl implements HouseService {

    private static final String DEFAULT_HOUSE_ID = "default";

    @Autowired
    private HouseRepository houseRepository;
    @Autowired
    private SessionService sessionService;

    @Override
    public List<House> list() {
        return houseRepository.list();
    }

    @Override
    public List<House> listWithPopulation() {
        List<House> houses = new ArrayList<>();
        houses.add(this.publicView(this.getDefaultHouse()));
        for (House house : houseRepository.list()) {
            if (!DEFAULT_HOUSE_ID.equals(house.getId())) {
                houses.add(this.publicView(this.fillPopulation(house)));
            }
        }
        return houses;
    }

    @Override
    public House get(String houseId) {
        return this.publicView(this.getRaw(houseId));
    }

    @Override
    public House getRaw(String houseId) {
        String currentHouseId = this.normalizeHouseId(houseId);
        if (DEFAULT_HOUSE_ID.equals(currentHouseId)) {
            return this.getDefaultHouse();
        }
        House house = houseRepository.get(currentHouseId);
        if (house == null) {
            return null;
        }
        return this.fillPopulation(house);
    }

    @Override
    public String create(HouseRequest request) {
        this.validateCreateRequest(request);
        String houseId = this.createHouseId();
        House house = House.builder()
                .id(houseId)
                .name(this.safeTrim(request.getName()))
                .desc(this.safeTrim(request.getDesc()))
                .needPwd(Boolean.TRUE.equals(request.getNeedPwd()))
                .password(Boolean.TRUE.equals(request.getNeedPwd()) ? this.safeTrim(request.getPassword()) : "")
                .enableStatus(Boolean.TRUE.equals(request.getEnableStatus()))
                .retainKey(this.safeTrim(request.getRetainKey()))
                .population(0)
                .announce(new Notice())
                .build();
        houseRepository.set(house);
        return houseId;
    }

    @Override
    public House enter(String houseId, String password) {
        House house = this.getRaw(houseId);
        if (house == null) {
            throw new IllegalArgumentException("房间不存在");
        }
        if (Boolean.TRUE.equals(house.getNeedPwd()) && !this.safeTrim(house.getPassword()).equals(this.safeTrim(password))) {
            throw new IllegalArgumentException("房间密码错误");
        }
        return this.publicView(house);
    }

    @Override
    public String bindSession(String sessionId, String houseId) {
        if (this.getRaw(houseId) == null) {
            throw new IllegalArgumentException("房间不存在");
        }
        return sessionService.switchRoom(sessionId, this.normalizeHouseId(houseId));
    }

    @Override
    public List<User> listUsers(String houseId) {
        return sessionService.listUsers(this.normalizeHouseId(houseId));
    }

    @Override
    public Notice updateAnnouncement(String houseId, Notice notice) {
        String currentHouseId = this.normalizeHouseId(houseId);
        Notice currentNotice = notice == null ? new Notice() : notice;
        currentNotice.setContent(this.safeTrim(currentNotice.getContent()));
        House house = this.getOrCreateDefault(currentHouseId);
        house.setAnnounce(currentNotice);
        houseRepository.set(house);
        return currentNotice;
    }

    @Override
    public House updateRetainStatus(String houseId, boolean enableStatus) {
        String currentHouseId = this.normalizeHouseId(houseId);
        House house = this.getOrCreateDefault(currentHouseId);
        if (enableStatus && "".equals(this.safeTrim(house.getRetainKey()))) {
            throw new IllegalArgumentException("房间永存订单号不能为空");
        }
        house.setEnableStatus(enableStatus);
        if (!DEFAULT_HOUSE_ID.equals(currentHouseId)) {
            houseRepository.set(house);
        }
        return this.publicView(this.fillPopulation(house));
    }

    private House fillPopulation(House source) {
        return source.toBuilder()
                .population(sessionService.size(source.getId()).intValue())
                .build();
    }

    private House publicView(House source) {
        if (source == null) {
            return null;
        }
        return source.toBuilder()
                .password("")
                .retainKey("")
                .build();
    }

    private House buildDefaultHouse() {
        return House.builder()
                .id(DEFAULT_HOUSE_ID)
                .name("公共房间")
                .desc("默认公共房间")
                .needPwd(false)
                .password("")
                .enableStatus(false)
                .retainKey("")
                .population(sessionService.size(DEFAULT_HOUSE_ID).intValue())
                .announce(null)
                .build();
    }

    private House getDefaultHouse() {
        House house = houseRepository.get(DEFAULT_HOUSE_ID);
        if (house == null) {
            return this.buildDefaultHouse();
        }
        return this.fillPopulation(house);
    }

    private House getOrCreateDefault(String houseId) {
        if (DEFAULT_HOUSE_ID.equals(houseId)) {
            House house = houseRepository.get(DEFAULT_HOUSE_ID);
            return house == null ? this.buildDefaultHouse() : house;
        }
        House house = houseRepository.get(houseId);
        if (house == null) {
            throw new IllegalArgumentException("房间不存在");
        }
        return house;
    }

    private void validateCreateRequest(HouseRequest request) {
        if (request == null || "".equals(this.safeTrim(request.getName()))) {
            throw new IllegalArgumentException("房间名不能为空");
        }
        if (Boolean.TRUE.equals(request.getEnableStatus()) && "".equals(this.safeTrim(request.getRetainKey()))) {
            throw new IllegalArgumentException("房间永存订单号不能为空");
        }
        if (Boolean.TRUE.equals(request.getNeedPwd()) && "".equals(this.safeTrim(request.getPassword()))) {
            throw new IllegalArgumentException("房间密码不能为空");
        }
    }

    private String createHouseId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String normalizeHouseId(String houseId) {
        String currentHouseId = this.safeTrim(houseId);
        return "".equals(currentHouseId) ? DEFAULT_HOUSE_ID : currentHouseId;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
