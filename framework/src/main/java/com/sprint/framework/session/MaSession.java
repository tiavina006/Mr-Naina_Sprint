package com.sprint.framework.session;

import java.util.LinkedHashMap;
import java.util.Map;

public class MaSession {
    private final String id;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private long lastAccessTime;

    public MaSession(String id) {
        this.id = id;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    public void remove(String key) {
        attributes.remove(key);
    }

    public void invalidate() {
        attributes.clear();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void touch() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - lastAccessTime > timeoutMs;
    }
}
