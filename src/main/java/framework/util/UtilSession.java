package framework.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class UtilSession {

    private UtilSession() {
    }

    public static Session chargerSession(HttpServletRequest req) {
        Map<String, Object> values = new HashMap<>();
        HttpSession httpSession = req.getSession(false); 
        if (httpSession != null) {
            for (String name : Collections.list(httpSession.getAttributeNames())) {
                values.put(name, httpSession.getAttribute(name));
            }
        }
        return new Session(values);
    }

  
    public static void ecraseSession(HttpServletRequest req, Session session) {
        HttpSession httpSession = req.getSession(false);

        if (session.isInvalidated()) {
            if (httpSession != null) {
                httpSession.invalidate();
            }
            if (session.getValues().isEmpty()) {
                return;
            }
            httpSession = null;
        }

        if (httpSession == null) {
            if (session.getValues().isEmpty()) {
                return; 
            }
            httpSession = req.getSession(true);
        }

        for (String name : Collections.list(httpSession.getAttributeNames())) {
            if (!session.has(name)) {
                httpSession.removeAttribute(name);
            }
        }
        for (Map.Entry<String, Object> entry : session.getValues().entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}
