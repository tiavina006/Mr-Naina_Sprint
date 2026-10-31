package com.sprint.framework.mvc;

import java.util.HashMap;
import java.util.Map;

public final class ControllerRegistry {
    public static final String ATTRIBUTE = ControllerRegistry.class.getName();

    private final Map<Class<?>, Object> controllers = new HashMap<Class<?>, Object>();

    public synchronized Object getOrCreate(Class<?> controllerClass)
            throws InstantiationException, IllegalAccessException {
        Object controller = controllers.get(controllerClass);
        if (controller == null) {
            controller = controllerClass.newInstance();
            controllers.put(controllerClass, controller);
        }
        return controller;
    }

    public synchronized boolean contains(Class<?> controllerClass) {
        return controllers.containsKey(controllerClass);
    }

    public synchronized Object get(Class<?> controllerClass) {
        return controllers.get(controllerClass);
    }

    public synchronized int size() {
        return controllers.size();
    }
}
