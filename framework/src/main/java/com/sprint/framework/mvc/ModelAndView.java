package com.sprint.framework.mvc;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModelAndView {
    private String view;
    private final Map<String, Object> data = new LinkedHashMap<String, Object>();

    public ModelAndView(String view) {
        this.view = view;
    }

    public ModelAndView addAttribut(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public void setview(String view) {
        setView(view);
    }

    public Map<String, Object> getData() {
        return data;
    }
}