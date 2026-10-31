package com.sprint.testapp.controller;

import com.sprint.framework.annotation.Authenticated;
import com.sprint.framework.annotation.Authorized;
import com.sprint.framework.annotation.Controller;
import com.sprint.framework.annotation.GetMapping;
import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mapping.MappingRegistry;
import com.sprint.framework.mvc.ModelAndView;
import com.sprint.framework.json.JsonSerializer;
import com.sprint.framework.security.Authorization;
import com.sprint.framework.session.MaSession;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        assertEquals("[\"karoty\",\"Amina\"]",
                JsonSerializer.toJson(new String[]{"karoty", "Amina"}));
    }

    @Test
    public void adminActionRequiresAdminRole() throws NoSuchMethodException {
        Method method = EmployeController.class.getMethod("admin", MaSession.class);
        Authorized authorized = method.getAnnotation(Authorized.class);

        assertTrue(authorized != null);
        assertEquals(1, authorized.roles().length);
        assertEquals("admin", authorized.roles()[0]);
    }

    @Test
    public void profilActionRequiresAuthentication() throws NoSuchMethodException {
        assertTrue(EmployeController.class.getMethod("profil", MaSession.class)
                .isAnnotationPresent(Authenticated.class));
    }

    @Test
    public void loginStoresAdminRoleForAdminUser() {
        MaSession session = new MaSession("s1");
        new EmployeController().login("admin", session);

        assertTrue(Authorization.hasRole(session, "admin"));
        assertTrue(Authorization.isAuthenticated(session));
    }

    @Test
    public void loginStoresUserRoleForRegularUser() {
        MaSession session = new MaSession("s1");
        new EmployeController().login("karoty", session);

        assertTrue(Authorization.hasRole(session, "user"));
        assertFalse(Authorization.hasRole(session, "admin"));
    }

    @Test
    public void logoutClearsSessionAndAuthorization() {
        MaSession session = new MaSession("s1");
        new EmployeController().login("admin", session);

        new EmployeController().logout(session);

        assertFalse(Authorization.isAuthenticated(session));
        assertEquals(null, session.get("utilisateur"));
    }
}