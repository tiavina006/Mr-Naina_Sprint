package com.sprint.framework.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ScanAnnotation {
    private ScanAnnotation() {
    }

    public static List<Class<?>> findClassesWithAnnotation(Set<Class<?>> classes,
                                                            Class<? extends Annotation> annotation) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        for (Class<?> candidate : classes) {
            if (candidate.isAnnotationPresent(annotation)) {
                result.add(candidate);
            }
        }
        return result;
    }

    public static List<Method> getMethodWithAnnotation(Class<?> type,
                                                       Class<? extends Annotation> annotation) {
        List<Method> result = new ArrayList<Method>();
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                result.add(method);
            }
        }
        return result;
    }
}