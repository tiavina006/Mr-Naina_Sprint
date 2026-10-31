package com.sprint.framework;

import com.sprint.framework.mvc.ControllerRegistry;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ControllerSingletonTest {

    public static class PremierController {
    }

    public static class SecondController {
    }

    @Test
    public void sameControllerClassAlwaysReturnsTheSameInstance() throws Exception {
        ControllerRegistry registry = new ControllerRegistry();

        Object first = registry.getOrCreate(PremierController.class);
        Object second = registry.getOrCreate(PremierController.class);

        assertSame(first, second);
        assertTrue(registry.contains(PremierController.class));
        assertSame(first, registry.get(PremierController.class));
        assertTrue(registry.size() == 1);
    }

    @Test
    public void eachControllerClassHasItsOwnInstance() throws Exception {
        ControllerRegistry registry = new ControllerRegistry();

        Object premier = registry.getOrCreate(PremierController.class);
        Object second = registry.getOrCreate(SecondController.class);

        assertNotNull(premier);
        assertNotNull(second);
        assertFalse(premier == second);
        assertTrue(registry.size() == 2);
    }

    @Test
    public void frontControllerUsesTheSharedControllerRegistry() throws Exception {
        FrontController frontController = new FrontController();

        ControllerRegistry registry = frontController.controllerRegistry();
        Object controller = registry.getOrCreate(PremierController.class);

        assertSame(registry, frontController.controllerRegistry());
        assertSame(controller, frontController.controllerRegistry().getOrCreate(PremierController.class));
    }
}
