package com.scoder.jusic.service;

/**
 * @author H
 */
public interface RoomSnapshotService {

    void sendRoomSnapshot(String sessionId);

    void sendRoomSnapshot(String sessionId, String roomId);

    void sendRoomSnapshot(String sessionId, String roomId, boolean includeHouseInfo);

    void broadcastOnline(String roomId);
}
