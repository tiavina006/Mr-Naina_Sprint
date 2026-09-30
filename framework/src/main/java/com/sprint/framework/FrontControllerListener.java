package com.sprint.framework;

import com.sprint.framework.mapping.Mapping;
import com.sprint.framework.mapping.MappingRegistry;

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
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}