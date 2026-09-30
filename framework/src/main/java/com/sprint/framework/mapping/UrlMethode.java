package com.sprint.framework.mapping;

import java.util.Objects;

public final class UrlMethode {
    private final String url;
    private final HttpMethod method;

    public UrlMethode(String url, HttpMethod method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UrlMethode)) {
            return false;
        }
        UrlMethode that = (UrlMethode) other;
        return Objects.equals(url, that.url) && method == that.method;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }
}