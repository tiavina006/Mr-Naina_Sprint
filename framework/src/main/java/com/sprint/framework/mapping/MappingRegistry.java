package com.sprint.framework.mapping;

import com.sprint.framework.annotation.Controller;
import com.sprint.framework.annotation.GetMapping;
import com.sprint.framework.annotation.RequestMapping;
import com.sprint.framework.annotation.UrlMapping;
import com.sprint.framework.util.ScanAnnotation;
import com.sprint.framework.util.UtilAnalyser;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MappingRegistry {
    private final Map<UrlMethode, Mapping> mappings = new LinkedHashMap<UrlMethode, Mapping>();

    public MappingRegistry() {
    }

    public MappingRegistry(Map<UrlMethode, Mapping> mappings) {
        this.mappings.putAll(mappings);
    }

    public void scan(String packageName) {
        Set<Class<?>> classes = UtilAnalyser.findClasses(packageName);
        for (Class<?> controller : ScanAnnotation.findClassesWithAnnotation(classes, Controller.class)) {
            register(controller);
        }
    }

    private void register(Class<?> controller) {
        String classPath = pathOf(controller.getAnnotation(RequestMapping.class));
        for (Method method : ScanAnnotation.getMethodWithAnnotation(controller, GetMapping.class)) {
            GetMapping annotation = method.getAnnotation(GetMapping.class);
            add(classPath, annotation.value(), HttpMethod.GET, controller, method);
        }
        for (Method method : ScanAnnotation.getMethodWithAnnotation(controller, UrlMapping.class)) {
            UrlMapping annotation = method.getAnnotation(UrlMapping.class);
            add(classPath, annotation.value(), annotation.method(), controller, method);
        }
    }

    private void add(String parent, String child, HttpMethod httpMethod, Class<?> controller, Method method) {
        mappings.put(new UrlMethode(join(parent, child), httpMethod), new Mapping(controller, method));
    }

    private String pathOf(RequestMapping annotation) {
        return annotation == null ? "" : annotation.value();
    }

    private String join(String parent, String child) {
        return (parent + "/" + child).replaceAll("//+", "/");
    }

    public Mapping get(String path) {
        return get(path, HttpMethod.GET);
    }

    public Mapping get(String path, HttpMethod method) {
        return mappings.get(new UrlMethode(path, method));
    }

    public Map<UrlMethode, Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(Map<UrlMethode, Mapping> mappings) {
        this.mappings.clear();
        this.mappings.putAll(mappings);
    }
}