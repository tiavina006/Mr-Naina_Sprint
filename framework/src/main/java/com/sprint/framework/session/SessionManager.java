package com.sprint.framework.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    public static final String ATTRIBUTE = SessionManager.class.getName();
    public static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, MaSession> sessions = new ConcurrentHashMap<>();
    private final long timeoutMs;

    public SessionManager(long timeoutMs) {
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    public MaSession getOrCreate(HttpServletRequest request) {
        cleanExpired();
        HttpSession httpSession = request.getSession(true);
        String id = httpSession.getId();
        MaSession session = sessions.get(id);
        if (session == null) {
            session = new MaSession(id);
            sessions.put(id, session);
        }
        session.touch();
        return session;
    }

    public MaSession get(String id) {
        return sessions.get(id);
    }

    public void remove(String id) {
        sessions.remove(id);
    }

    public void cleanExpired() {
        for (Map.Entry<String, MaSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired(timeoutMs)) {
                sessions.remove(entry.getKey());
            }
        }
    }

    public int size() {
        return sessions.size();
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}
