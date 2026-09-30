package com.sprint.framework.mapping;

import java.lang.reflect.Method;

public final class Mapping {
    private final Class<?> controllerClass;
    private final Method method;

    public Mapping(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }
}