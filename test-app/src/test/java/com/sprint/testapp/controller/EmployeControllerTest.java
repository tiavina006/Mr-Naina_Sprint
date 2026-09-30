package com.sprint.testapp.controller;

import com.sprint.framework.annotation.Controller;
import com.sprint.framework.annotation.GetMapping;
import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mapping.MappingRegistry;
import com.sprint.framework.mvc.ModelAndView;
import com.sprint.framework.json.JsonSerializer;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EmployeControllerTest {
    @Test
    public void controllerAndMappingAreDiscoverable() throws NoSuchMethodException {
        assertTrue(EmployeController.class.isAnnotationPresent(Controller.class));
        Method method = EmployeController.class.getMethod("liste");
        assertEquals("/employe/liste", method.getAnnotation(GetMapping.class).value());
        ModelAndView modelAndView = new EmployeController().liste();
        assertEquals("/WEB-INF/views/employes.jsp", modelAndView.getView());
        assertEquals("Liste des employes", modelAndView.getData().get("titre"));
    }

    @Test
    public void registryFindsGetUrlByHttpMethod() {
        MappingRegistry registry = new MappingRegistry();
        registry.scan("com.sprint.testapp.controller");

        assertEquals(EmployeController.class,
                registry.get("/employe/liste", HttpMethod.GET).getControllerClass());
    }

    @Test
    public void restActionsExposeTheTwoJsonModes() throws NoSuchMethodException {
        assertTrue(EmployeController.class.getMethod("apiListe")
                .isAnnotationPresent(com.sprint.framework.annotation.APIREST.class));
        assertTrue(EmployeController.class.getMethod("apiJson")
                .getAnnotation(com.sprint.framework.annotation.APIREST.class).alreadyJson());
    }

    @Test
    public void jsonSerializerProducesJsonArray() {
        assertEquals("[\"Naina\",\"Amina\"]",
                JsonSerializer.toJson(new String[]{"Naina", "Amina"}));
    }
}