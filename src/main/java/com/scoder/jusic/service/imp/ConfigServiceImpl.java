package com.scoder.jusic.service.imp;

import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author H
 */
@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void setPushSwitch(String roomId, boolean pushSwitch) {
        configRepository.setPushSwitch(roomId, pushSwitch);
    }

    @Override
    public void setEnableSwitch(String roomId, boolean enableSwitch) {
        configRepository.setEnableSwitch(roomId, enableSwitch);
    }

    @Override
    public void setEnableSearch(String roomId, boolean enableSearch) {
        configRepository.setEnableSearch(roomId, enableSearch);
    }

    @Override
    public Boolean getEnableSearch(String roomId) {
        return configRepository.getEnableSearch(roomId);
    }

    @Override
    public Float getVoteRate(String roomId) {
        return configRepository.getVoteRate(roomId);
    }

    @Override
    public Boolean getEnableSwitch(String roomId) {
        return configRepository.getEnableSwitch(roomId);
    }

    @Override
    public Boolean getGoodModel(String roomId) {
        return configRepository.getGoodModel(roomId);
    }

    @Override
    public void setGoodModel(String roomId, boolean goodModel) {
        configRepository.setGoodModel(roomId, goodModel);
    }

    @Override
    public String getPlayMode(String roomId) {
        return configRepository.getPlayMode(roomId);
    }

    @Override
    public void setPlayMode(String roomId, String playMode) {
        configRepository.setPlayMode(roomId, playMode);
    }
}
