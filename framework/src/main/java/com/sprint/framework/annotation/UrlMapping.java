package com.sprint.framework.annotation;

import com.sprint.framework.mapping.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlMapping {
    String value();
    HttpMethod method() default HttpMethod.GET;
}