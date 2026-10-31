package com.sprint.framework.security;

import com.sprint.framework.annotation.Authorized;
import com.sprint.framework.exception.AccessDeniedException;
import com.sprint.framework.session.MaSession;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Authorization {
    public static final String PRINCIPAL_ATTRIBUTE = "authorization.principal";
    public static final String ROLES_ATTRIBUTE = "authorization.roles";
    public static final String PERMISSIONS_ATTRIBUTE = "authorization.permissions";

    public static final String DEFAULT_SESSION_KEY = PRINCIPAL_ATTRIBUTE;
    public static final String SESSION_KEY_INIT_PARAM = "auth.session-key";
    public static final String SESSION_KEY_ATTRIBUTE = Authorization.class.getName() + ".sessionKey";
    public static final String AUTHENTICATED_REQUEST_ATTRIBUTE = "authenticated";
    public static final String PRINCIPAL_REQUEST_ATTRIBUTE = "principal";

    private Authorization() {
    }

    public static void login(MaSession session, String principal, Collection<String> roles,
                             Collection<String> permissions) {
        session.set(PRINCIPAL_ATTRIBUTE, principal);
        session.set(ROLES_ATTRIBUTE, copy(roles));
        session.set(PERMISSIONS_ATTRIBUTE, copy(permissions));
    }

    public static void login(MaSession session, String principal, String... roles) {
        login(session, principal, Arrays.asList(roles), Collections.<String>emptyList());
    }

    public static void logout(MaSession session) {
        session.remove(PRINCIPAL_ATTRIBUTE);
        session.remove(ROLES_ATTRIBUTE);
        session.remove(PERMISSIONS_ATTRIBUTE);
    }

    public static String getPrincipal(MaSession session) {
        if (session == null) {
            return null;
        }
        Object principal = session.get(PRINCIPAL_ATTRIBUTE);
        return principal == null ? null : String.valueOf(principal);
    }

    public static Set<String> getRoles(MaSession session) {
        return values(session, ROLES_ATTRIBUTE);
    }

    public static Set<String> getPermissions(MaSession session) {
        return values(session, PERMISSIONS_ATTRIBUTE);
    }

    public static boolean isAuthenticated(MaSession session) {
        return isAuthenticated(session, DEFAULT_SESSION_KEY);
    }

    public static boolean isAuthenticated(MaSession session, String sessionKey) {
        if (session == null) {
            return false;
        }
        if (hasValue(session.get(sessionKey))) {
            return true;
        }
        return getPrincipal(session) != null
                || !getRoles(session).isEmpty()
                || !getPermissions(session).isEmpty();
    }

    public static boolean hasRole(MaSession session, String role) {
        return role != null && getRoles(session).contains(role);
    }

    public static boolean hasPermission(MaSession session, String permission) {
        return permission != null && getPermissions(session).contains(permission);
    }

    public static boolean isAllowed(MaSession session, Authorized authorized) {
        return isAllowed(session, authorized, DEFAULT_SESSION_KEY);
    }

    public static boolean isAllowed(MaSession session, Authorized authorized, String sessionKey) {
        if (authorized == null) {
            return true;
        }
        if (!isAuthenticated(session, sessionKey)) {
            return false;
        }
        String[] requiredRoles = authorized.roles();
        if (requiredRoles.length > 0) {
            boolean matching = false;
            for (String role : requiredRoles) {
                if (hasRole(session, role)) {
                    matching = true;
                    break;
                }
            }
            if (!matching) {
                return false;
            }
        }
        for (String permission : authorized.permissions()) {
            if (!hasPermission(session, permission)) {
                return false;
            }
        }
        return true;
    }

    public static void check(MaSession session, Authorized authorized) {
        check(session, authorized, DEFAULT_SESSION_KEY);
    }

    public static void check(MaSession session, Authorized authorized, String sessionKey) {
        if (authorized == null) {
            return;
        }
        if (!isAuthenticated(session, sessionKey)) {
            throw new AccessDeniedException("Authentification requise", false);
        }
        if (!isAllowed(session, authorized, sessionKey)) {
            throw new AccessDeniedException("Droits d'acces insuffisants", true);
        }
    }

    public static void checkAuthenticated(MaSession session, String sessionKey) {
        if (!isAuthenticated(session, sessionKey)) {
            throw new AccessDeniedException("Authentification requise", false);
        }
    }

    private static boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return !String.valueOf(value).trim().isEmpty();
    }

    private static Set<String> copy(Collection<String> values) {
        Set<String> copy = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    copy.add(value.trim());
                }
            }
        }
        return copy;
    }

    private static Set<String> values(MaSession session, String key) {
        Set<String> result = new LinkedHashSet<>();
        if (session == null) {
            return result;
        }
        Object value = session.get(key);
        if (value instanceof Collection) {
            for (Object element : (Collection<?>) value) {
                add(result, element);
            }
        } else if (value instanceof Object[]) {
            for (Object element : (Object[]) value) {
                add(result, element);
            }
        } else if (value instanceof String && ((String) value).contains(",")) {
            for (String element : ((String) value).split(",")) {
                add(result, element);
            }
        } else {
            add(result, value);
        }
        return result;
    }

    private static void add(Set<String> target, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isEmpty()) {
            target.add(text);
        }
    }
}
