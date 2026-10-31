package com.sprint.framework.exception;

public class AccessDeniedException extends RuntimeException {
    private final boolean authenticated;

    public AccessDeniedException(String message, boolean authenticated) {
        super(message);
        this.authenticated = authenticated;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
