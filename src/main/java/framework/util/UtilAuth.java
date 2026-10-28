package framework.util;

import java.lang.reflect.Method;
import java.util.Arrays;

import framework.annotation.Auth;

public class UtilAuth {
    private final String userKey;
    private final String roleKey;
    private final String loginUrl;

    public UtilAuth(String userKey, String roleKey, String loginUrl) {
        this.userKey = userKey;
        this.roleKey = roleKey;
        this.loginUrl = loginUrl;
    }

    public boolean isAllowed(Method method, Session session) {
        Auth auth = method.getAnnotation(Auth.class);
        if (auth == null) auth = method.getDeclaringClass().getAnnotation(Auth.class);
        if (auth == null) return true;                          // public page

        if (!session.has(userKey)) return false;                // not logged in

        if (auth.value().length == 0) return true;              // any logged-in user
        Object role = session.get(roleKey);
        return role != null && Arrays.asList(auth.value()).contains(role.toString());
    }

    public String getLoginUrl() { return loginUrl; }
}