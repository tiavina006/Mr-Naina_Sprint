package com.sprint.framework;

import com.sprint.framework.annotation.Authenticated;
import com.sprint.framework.annotation.Authorized;
import com.sprint.framework.exception.AccessDeniedException;
import com.sprint.framework.security.Authorization;
import com.sprint.framework.session.MaSession;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AuthorizationTest {

    public static class OpenController {
        public void open() {
        }
    }

    @Authorized(roles = "admin")
    public static class ClassAdminController {
        public void action() {
        }
    }

    @Authorized(roles = "user")
    public static class MixedController {
        @Authorized(roles = "admin")
        public void admin() {
        }

        public void user() {
        }
    }

    public static class AuthenticatedController {
        @Authenticated
        public void profile() {
        }
    }

    public static class ProfAdminController {
        @Authorized(roles = {"PROF", "ADMIN"})
        public void enseigner() {
        }
    }

    public static class MethodController {
        @Authorized(roles = {"admin", "manager"})
        public void manage() {
        }

        @Authorized(permissions = {"employe:read", "employe:write"})
        public void write() {
        }

        @Authorized
        public void authenticated() {
        }

        public void open() {
        }
    }

    private static HttpServletRequest request(String sessionId) {
        return request(sessionId, new HashMap<String, Object>());
    }

    private static HttpServletRequest request(String sessionId, final Map<String, Object> attributes) {
        HttpSession httpSession = (HttpSession) Proxy.newProxyInstance(
                AuthorizationTest.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> "getId".equals(method.getName()) ? sessionId : null);
        return (HttpServletRequest) Proxy.newProxyInstance(
                AuthorizationTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getSession".equals(method.getName())) {
                        return httpSession;
                    }
                    if ("setAttribute".equals(method.getName())) {
                        attributes.put((String) args[0], args[1]);
                        return null;
                    }
                    if ("getAttribute".equals(method.getName())) {
                        return attributes.get(args[0]);
                    }
                    return null;
                });
    }

    private static Method method(Class<?> type, String name) throws NoSuchMethodException {
        return type.getMethod(name);
    }

    private static MaSession login(FrontController frontController, HttpServletRequest request,
                                   String principal, String... roles) {
        MaSession session = frontController.sessionManager().getOrCreate(request);
        Authorization.login(session, principal, roles);
        return session;
    }

    private static void assertDenied(boolean authenticated, FrontController frontController,
                                     Method action, Class<?> controllerClass, HttpServletRequest request) {
        try {
            frontController.authorize(action, controllerClass, request);
            fail("AccessDeniedException attendue");
        } catch (AccessDeniedException exception) {
            assertTrue(exception.isAuthenticated() == authenticated);
        }
    }

    @Test
    public void methodWithoutAnnotationIsAllowed() throws Exception {
        FrontController frontController = new FrontController();

        frontController.authorize(method(OpenController.class, "open"),
                OpenController.class, request("client-open"));
    }

    @Test
    public void unauthenticatedClientIsRejected() throws Exception {
        FrontController frontController = new FrontController();

        assertDenied(false, frontController,
                method(ClassAdminController.class, "action"),
                ClassAdminController.class, request("client-anonymous"));
    }

    @Test
    public void classLevelAuthorizationIsApplied() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-admin");
        login(frontController, request, "karoty", "admin");

        frontController.authorize(method(ClassAdminController.class, "action"),
                ClassAdminController.class, request);
    }

    @Test
    public void authenticatedClientWithoutRequiredRoleIsRejected() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-user");
        login(frontController, request, "votaby", "user");

        assertDenied(true, frontController,
                method(ClassAdminController.class, "action"),
                ClassAdminController.class, request);
    }

    @Test
    public void methodAnnotationOverridesClassAnnotation() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-mixed");
        login(frontController, request, "karoty", "admin");

        frontController.authorize(method(MixedController.class, "admin"),
                MixedController.class, request);
    }

    @Test
    public void classAnnotationAppliesWhenMethodHasNone() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-class-user");
        login(frontController, request, "votaby", "user");

        frontController.authorize(method(MixedController.class, "user"),
                MixedController.class, request);
    }

    @Test
    public void anyOfTheRequiredRolesGrantsAccess() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-manager");
        login(frontController, request, "manager", "manager");

        frontController.authorize(method(MethodController.class, "manage"),
                MethodController.class, request);
    }

    @Test
    public void professorRoleIsAcceptedForAProfOrAdminAction() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-prof");
        login(frontController, request, "soa", "PROF");

        frontController.authorize(method(ProfAdminController.class, "enseigner"),
                ProfAdminController.class, request);
    }

    @Test
    public void adminRoleIsAlsoAcceptedForAProfOrAdminAction() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-admin-prof");
        login(frontController, request, "karoty", "ADMIN");

        frontController.authorize(method(ProfAdminController.class, "enseigner"),
                ProfAdminController.class, request);
    }

    @Test
    public void unknownRoleIsRejectedForAProfOrAdminAction() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-etudiant");
        login(frontController, request, "votaby", "ETUDIANT");

        assertDenied(true, frontController,
                method(ProfAdminController.class, "enseigner"),
                ProfAdminController.class, request);
    }

    @Test
    public void missingRoleRejectsAccessWhenSeveralAreRequired() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-user-2");
        login(frontController, request, "votaby", "user");

        assertDenied(true, frontController,
                method(MethodController.class, "manage"),
                MethodController.class, request);
    }

    @Test
    public void allRequiredPermissionsAreNeeded() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-permissions");
        MaSession session = login(frontController, request, "karoty", "user");
        session.set(Authorization.PERMISSIONS_ATTRIBUTE, Arrays.asList("employe:read"));

        assertDenied(true, frontController,
                method(MethodController.class, "write"),
                MethodController.class, request);

        session.set(Authorization.PERMISSIONS_ATTRIBUTE,
                Arrays.asList("employe:read", "employe:write"));
        frontController.authorize(method(MethodController.class, "write"),
                MethodController.class, request);
    }

    @Test
    public void emptyAuthorizedRequiresAuthenticationOnly() throws Exception {
        FrontController frontController = new FrontController();
        HttpServletRequest request = request("client-auth");

        assertDenied(false, frontController,
                method(MethodController.class, "authenticated"),
                MethodController.class, request);

        frontController.sessionManager().getOrCreate(request).set(Authorization.PRINCIPAL_ATTRIBUTE, "karoty");
        frontController.authorize(method(MethodController.class, "authenticated"),
                MethodController.class, request);
    }

    @Test
    public void authenticatedAnnotationRequiresTheConfiguredSessionKey() throws Exception {
        FrontController frontController = new FrontController();
        frontController.setAuthSessionKey("utilisateur");
        Method profile = method(AuthenticatedController.class, "profile");

        assertDenied(false, frontController, profile,
                AuthenticatedController.class, request("client-auth-key"));

        frontController.sessionManager().getOrCreate(request("client-auth-key"))
                .set("utilisateur", "karoty");
        Map<String, Object> attributes = new HashMap<>();
        frontController.authorize(profile, AuthenticatedController.class,
                request("client-auth-key", attributes));

        assertEquals(Boolean.TRUE, attributes.get(Authorization.AUTHENTICATED_REQUEST_ATTRIBUTE));
        assertEquals("karoty", attributes.get(Authorization.PRINCIPAL_REQUEST_ATTRIBUTE));
    }

    @Test
    public void configuredSessionKeyCanProveAuthentication() {
        MaSession session = new MaSession("s1");
        session.set("utilisateur", "karoty");

        assertTrue(Authorization.isAuthenticated(session, "utilisateur"));
        assertFalse(Authorization.isAuthenticated(session, "autre-cle"));
    }

    @Test
    public void logoutClearsAuthorizationData() {
        MaSession session = new MaSession("s1");
        Authorization.login(session, "karoty", Arrays.asList("admin"),
                Arrays.asList("employe:write"));
        assertTrue(Authorization.isAuthenticated(session));
        assertTrue(Authorization.hasRole(session, "admin"));

        Authorization.logout(session);

        assertFalse(Authorization.isAuthenticated(session));
        assertFalse(Authorization.hasRole(session, "admin"));
        assertFalse(Authorization.hasPermission(session, "employe:write"));
    }

    @Test
    public void commaSeparatedRolesAreSupported() {
        MaSession session = new MaSession("s1");
        session.set(Authorization.ROLES_ATTRIBUTE, "admin, manager");

        assertTrue(Authorization.hasRole(session, "admin"));
        assertTrue(Authorization.hasRole(session, "manager"));
    }

    @Test
    public void missingAnnotationAllowsEverything() {
        assertTrue(Authorization.isAllowed(null, null));
    }
}
