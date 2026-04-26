package com.scoder.jusic.repository;

import java.util.Map;

/**
 * @author H
 */
public interface ConfigRepository {

    Long destroy();

    Long destroy(String roomId);

    void initialize();

    Object get(Object hashKey);

    void put(Object hashKey, Object value);

    void putAll(Map<String, Object> map);

    Object getRoom(String roomId, Object hashKey);

    void putRoom(String roomId, Object hashKey, Object value);

    void putAllRoom(String roomId, Map<String, Object> map);

    String getPassword(String role);

    void setPassword(String role, String password);

    void initRootPassword();

    void initAdminPassword();

    String getRootPassword();

    String getAdminPassword();

    Long getLastMusicDuration(String roomId);

    void setLastMusicDuration(String roomId, Long duration);

    Long getLastMusicPushTime(String roomId);

    void setLastMusicPushTime(String roomId, Long pushTime);

    void setLastMusicPushTimeAndDuration(String roomId, Long pushTime, Long duration);

    Boolean getPushSwitch(String roomId);

    void setPushSwitch(String roomId, boolean pushSwitch);

    Float getVoteRate(String roomId);

    Boolean getEnableSwitch(String roomId);

    void setEnableSwitch(String roomId, boolean enableSwitch);

    Boolean getEnableSearch(String roomId);

    void setEnableSearch(String roomId, boolean enableSearch);

    Boolean getGoodModel(String roomId);

    void setGoodModel(String roomId, boolean goodModel);

    String getPlayMode(String roomId);

    void setPlayMode(String roomId, String playMode);
}
