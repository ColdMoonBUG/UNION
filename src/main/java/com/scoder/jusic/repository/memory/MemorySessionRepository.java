package com.scoder.jusic.repository.memory;

import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.SessionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Memory implementation for sessions.
 */
@Repository
@Profile("memory")
public class MemorySessionRepository implements SessionRepository {

    private final MemoryRuntimeStore store;

    public MemorySessionRepository(MemoryRuntimeStore store) {
        this.store = store;
    }

    @Override
    public Long destroy() {
        long size = this.store.sessions.size();
        this.store.sessions.clear();
        return size;
    }

    @Override
    public User getSession(String sessionId) {
        return this.store.sessions.get(sessionId);
    }

    @Override
    public void setSession(User user) {
        if (user != null && user.getSessionId() != null) {
            this.store.sessions.put(user.getSessionId(), user);
        }
    }

    @Override
    public Long size() {
        return (long) this.store.sessions.size();
    }

    @Override
    public Long removeSession(String sessionId) {
        return this.store.sessions.remove(sessionId) == null ? 0L : 1L;
    }
}
