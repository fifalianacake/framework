package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;

import framework.util.Mapping;
import framework.util.ModAndView;
import framework.util.UrlMethod;

public class FrontController extends HttpServlet {

    Map<UrlMethod, Mapping> urlMapping;
    String viewPrefix;
    String viewSuffix;
    ApplicationContext springContext;

    @Override
    public void init() throws ServletException {
        urlMapping = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("urlMapping");
        viewPrefix = (String) getServletContext().getAttribute("prefix");
        viewSuffix = (String) getServletContext().getAttribute("suffix");
        springContext = (ApplicationContext) getServletContext().getAttribute("springContext");

        if (urlMapping == null) {
            throw new ServletException("urlMapping not initialized.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String context = req.getContextPath();
        String path = uri.substring(context.length());
        String httpMethod = req.getMethod();

        UrlMethod key = new UrlMethod(path, httpMethod);
        Mapping mapping = urlMapping.get(key);

        if (mapping != null) {
            try {
                Object controller = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                Method controllerMethod = mapping.getMethod();
                Class<?>[] parameterTypes = controllerMethod.getParameterTypes();
                Object[] parameters = new Object[parameterTypes.length];

                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> paramType = parameterTypes[i];
                    if (ApplicationContext.class.isAssignableFrom(paramType)) {
                        parameters[i] = springContext;
                    } else {
                        parameters[i] = null;
                    }
                }

                Object result = controllerMethod.invoke(controller, parameters);

                if (result instanceof ModAndView mav) {
                    for (Map.Entry<String, Object> en : mav.getValues().entrySet()) {
                        req.setAttribute(en.getKey(), en.getValue());
                    }

                    if (mav.getView() != null && !mav.getView().isBlank()) {
                        String viewPath = viewPrefix + mav.getView() + viewSuffix;
                        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
                        dispatcher.forward(req, resp);
                        return;
                    }

                    throw new ServletException("No view defined for " + key);
                }

                if (result instanceof String text) {
                    resp.setContentType("text/plain;charset=UTF-8");
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Method result:");
                        out.println(text);
                    }
                    return;
                }

                throw new ServletException("Unsupported return type for " + key + " : "
                        + (result != null ? result.getClass().getName() : "null"));

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new RuntimeException("Unable to execute method for " + key, e);
            }
        } else {
            resp.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("No mapping found for URL: " + path);
                out.println("Available URLs:");

                for (UrlMethod k : urlMapping.keySet()) {
                    out.println(" - " + k);
                }
            }
        }
    }
}