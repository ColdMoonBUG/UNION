package com.scoder.jusic.repository.memory;

import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.SessionBlackRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Memory implementation for session blacklist.
 */
@Repository
@Profile("memory")
public class MemorySessionBlackRepository implements SessionBlackRepository {

    private final MemoryRuntimeStore store;

    public MemorySessionBlackRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public Long destroy() {
        long size = this.store.blackSessions.size();
        this.store.blackSessions.clear();
        return size;
    }

    @Override
    public User getSession(String sessionId) {
        return this.store.blackSessions.get(sessionId);
    }

    @Override
    public void setSession(User user) {
        if (user != null && user.getSessionId() != null) {
            this.store.blackSessions.put(user.getSessionId(), user);
        }
    }

    @Override
    public Long removeSession(String sessionId) {
        return this.store.blackSessions.remove(sessionId) == null ? 0L : 1L;
    }

    @Override
    public Set showBlackList() {
        return new LinkedHashSet<>(this.store.blackSessions.keySet());
    }
}
