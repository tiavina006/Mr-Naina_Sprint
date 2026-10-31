package com.sprint.framework;

import com.sprint.framework.mapping.Mapping;
import com.sprint.framework.mapping.MappingRegistry;
import com.sprint.framework.mvc.ControllerRegistry;
import com.sprint.framework.security.Authorization;
import com.sprint.framework.session.SessionManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Map;

public class FrontControllerListener implements ServletContextListener {
    public static final String MAPPINGS_ATTRIBUTE = FrontControllerListener.class.getName() + ".mappings";
    public static final String CONTROLLER_PACKAGE_ATTRIBUTE = FrontControllerListener.class.getName() + ".package";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String packageName = context.getInitParameter("controller-package");
        MappingRegistry registry = new MappingRegistry();
        registry.scan(packageName);
        Map<?, Mapping> mappings = registry.getMappings();
        context.setAttribute(MAPPINGS_ATTRIBUTE, mappings);
        context.setAttribute(CONTROLLER_PACKAGE_ATTRIBUTE, packageName);
        context.setAttribute(ControllerRegistry.ATTRIBUTE, new ControllerRegistry());
        long timeoutMs = parseTimeoutSeconds(context.getInitParameter("session.timeout"));
        context.setAttribute(SessionManager.ATTRIBUTE, new SessionManager(timeoutMs));
        String authSessionKey = context.getInitParameter(Authorization.SESSION_KEY_INIT_PARAM);
        context.setAttribute(Authorization.SESSION_KEY_ATTRIBUTE,
                authSessionKey == null || authSessionKey.trim().isEmpty()
                        ? Authorization.DEFAULT_SESSION_KEY : authSessionKey.trim());
    }

    private long parseTimeoutSeconds(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SessionManager.DEFAULT_TIMEOUT_MS;
        }
        try {
            return Long.parseLong(value.trim()) * 1000L;
        } catch (NumberFormatException exception) {
            return SessionManager.DEFAULT_TIMEOUT_MS;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}