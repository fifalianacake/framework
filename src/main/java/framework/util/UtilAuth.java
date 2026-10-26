package framework.util;


import java.lang.reflect.Method;

import framework.annotation.Auth;

public class UtilAuth {
    public static boolean isAllowed(Method method, Session session) {
        Auth auth = method.getAnnotation(Auth.class);
        if (auth == null)
            auth = method.getDeclaringClass().getAnnotation(Auth.class);
        if (auth == null)
            return true; // public page
        if (session.get("user") == null)
            return false; // not logged in
        return auth.value().isEmpty() || auth.value().equals(session.get("role"));
    }
}
