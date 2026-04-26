package com.scoder.jusic.service;

/**
 * @author H
 */
public interface ConfigService {

    void setPushSwitch(String roomId, boolean pushSwitch);

    void setEnableSwitch(String roomId, boolean enableSwitch);

    void setEnableSearch(String roomId, boolean enableSearch);

    Boolean getEnableSearch(String roomId);

    Boolean getEnableSwitch(String roomId);

    Float getVoteRate(String roomId);

    Boolean getGoodModel(String roomId);

    void setGoodModel(String roomId, boolean goodModel);

    String getPlayMode(String roomId);

    void setPlayMode(String roomId, String playMode);
}
