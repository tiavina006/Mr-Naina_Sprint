package com.sprint.framework.session;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class SessionManagerTest {

    private static HttpServletRequest requestWithSessionId(String sessionId) {
        HttpSession httpSession = (HttpSession) Proxy.newProxyInstance(
                SessionManagerTest.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> "getId".equals(method.getName()) ? sessionId : null);
        return (HttpServletRequest) Proxy.newProxyInstance(
                SessionManagerTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> "getSession".equals(method.getName()) ? httpSession : null);
    }

    @Test
    public void sameClientGetsSameSessionInstance() {
        SessionManager manager = new SessionManager(60000);

        MaSession first = manager.getOrCreate(requestWithSessionId("client-1"));
        MaSession second = manager.getOrCreate(requestWithSessionId("client-1"));

        assertSame(first, second);
        assertEquals(1, manager.size());
    }

    @Test
    public void differentClientsGetDifferentSessions() {
        SessionManager manager = new SessionManager(60000);

        MaSession first = manager.getOrCreate(requestWithSessionId("client-1"));
        MaSession second = manager.getOrCreate(requestWithSessionId("client-2"));

        assertNotSame(first, second);
        assertEquals(2, manager.size());
        assertEquals("client-1", first.getId());
        assertEquals("client-2", second.getId());
    }

    @Test
    public void expiredSessionsAreCleanedLazily() throws Exception {
        SessionManager manager = new SessionManager(10);
        manager.getOrCreate(requestWithSessionId("client-1"));
        Thread.sleep(30);

        manager.getOrCreate(requestWithSessionId("client-2"));

        assertEquals(1, manager.size());
    }

    @Test
    public void removeDeletesSession() {
        SessionManager manager = new SessionManager(60000);
        manager.getOrCreate(requestWithSessionId("client-1"));

        manager.remove("client-1");

        assertEquals(0, manager.size());
    }
}
