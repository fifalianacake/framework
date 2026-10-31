package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import framework.annotation.WebAPI;
import framework.config.FrameworkConfig;
import framework.servlet.binding.ArgumentResolver;
import framework.servlet.response.ResultHandler;
import framework.upload.FileStorage;
import framework.util.Mapping;
import framework.util.Session;
import framework.util.UrlMethod;
import framework.util.UtilAuth;
import framework.util.UtilSession;
import framework.util.ControllerRegistry;

@MultipartConfig
public class FrontController extends HttpServlet {

    Map<UrlMethod, Mapping> urlMapping;
    String viewPrefix;
    String viewSuffix;
    ApplicationContext springContext;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ArgumentResolver argumentResolver;
    private ResultHandler resultHandler;
    private UtilAuth utilAuth;
    private ControllerRegistry controllers;

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        urlMapping = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("urlMapping");
        viewPrefix = (String) getServletContext().getAttribute("prefix");
        viewSuffix = (String) getServletContext().getAttribute("suffix");
        springContext = (ApplicationContext) getServletContext().getAttribute("springContext");
        FrameworkConfig config = FrameworkConfig.load();
        String userKey = setting(config, "auth.session-user");
        String roleKey = setting(config, "auth.session-role");
        String loginUrl = config.get("auth.login-url", "/login");
        utilAuth = new UtilAuth(userKey, roleKey, loginUrl);

        if (urlMapping == null) {
            throw new ServletException("urlMapping not initialized.");
        }
        try {
            controllers = new ControllerRegistry(urlMapping.values()); 
        } catch (IllegalStateException e) {
            throw new ServletException(e.getMessage(), e);
        }
        log(controllers.size() + " controller(s) created");

        FileStorage fileStorage = FileStorage.from(FrameworkConfig.load());
        getServletContext().setAttribute("fileStorage", fileStorage);
        log("Uploads are saved to " + fileStorage.getRoot());

        argumentResolver = new ArgumentResolver(springContext, fileStorage, objectMapper);
        resultHandler = new ResultHandler(objectMapper, viewPrefix, viewSuffix);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        UrlMethod key = new UrlMethod(path, req.getMethod());
        Mapping mapping = urlMapping.get(key);

        if (mapping == null) {
            writeNotFound(path, resp);
            return;
        }

        try {
            Object controller = controllers.get(mapping.getControllerClass());
            Method method = mapping.getMethod();
            Session session = UtilSession.chargerSession(req);
            if (!utilAuth.isAllowed(method, session)) {
                if (method.isAnnotationPresent(WebAPI.class)) {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    resp.sendRedirect(req.getContextPath() + utilAuth.getLoginUrl());
                }
                return;
            }
            Object[] arguments = argumentResolver.resolve(req, resp, method, session);
            Object result = method.invoke(controller, arguments);
            UtilSession.ecraseSession(req, session);
            req.setAttribute("mySession", session);
            resultHandler.handle(result, method, req, resp, key);

        } catch (InvocationTargetException e) {
            handleControllerFailure(e.getCause(), key, resp);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Unable to execute method for " + key, e);
        }
    }

    private void handleControllerFailure(Throwable cause, UrlMethod key, HttpServletResponse resp)
            throws ServletException, IOException {
        if (cause instanceof IllegalArgumentException) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, cause.getMessage());
            return;
        }
        throw new ServletException("Controller method failed for " + key, cause);
    }

    private void writeNotFound(String path, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("No mapping found for URL: " + path);
            out.println("Available URLs:");
            for (UrlMethod k : urlMapping.keySet()) {
                out.println(" - " + k);
            }
        }
    }

    // application.properties first, then web.xml <context-param>, otherwise stop
    private String setting(FrameworkConfig config, String name) throws ServletException {
        String value = config.get(name, getServletContext().getInitParameter(name));
        if (value == null || value.isBlank()) {
            throw new ServletException(name + " is not defined in application.properties or web.xml");
        }
        return value;
    }
}
