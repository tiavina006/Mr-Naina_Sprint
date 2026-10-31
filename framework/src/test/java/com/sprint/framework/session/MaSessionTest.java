package com.sprint.framework.session;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MaSessionTest {

    @Test
    public void storesUpdatesAndRemovesAttributes() {
        MaSession session = new MaSession("s1");

        session.set("utilisateur", "karoty");
        assertEquals("karoty", session.get("utilisateur"));

        session.set("utilisateur", "votaby");
        assertEquals("votaby", session.get("utilisateur"));

        session.remove("utilisateur");
        assertNull(session.get("utilisateur"));
    }

    @Test
    public void invalidateClearsAllAttributes() {
        MaSession session = new MaSession("s1");
        session.set("a", 1);
        session.set("b", 2);

        session.invalidate();

        assertEquals(0, session.getAttributes().size());
    }

    @Test
    public void sessionExpiresAfterTimeout() throws Exception {
        MaSession session = new MaSession("s1");
        Thread.sleep(30);

        assertTrue(session.isExpired(10));
        assertFalse(session.isExpired(60000));
    }

    @Test
    public void touchRefreshesLastAccessTime() throws Exception {
        MaSession session = new MaSession("s1");
        Thread.sleep(30);
        session.touch();

        assertFalse(session.isExpired(10000));
    }
}
