package com.scoder.jusic.repository;

import java.util.Set;

/**
 * @author H
 */
public interface MusicVoteRepository {

    Long destroy();

    Long destroy(String roomId);

    Long add(String roomId, Object... value);

    Long size(String roomId);

    void reset(String roomId);

    Set members(String roomId);
}
