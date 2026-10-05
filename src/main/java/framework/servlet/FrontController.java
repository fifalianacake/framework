package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import framework.annotation.WebAPI;
import framework.util.Mapping;
import framework.util.ModAndView;
import framework.util.UrlMethod;

public class FrontController extends HttpServlet {

    Map<UrlMethod, Mapping> urlMapping;
    String viewPrefix;
    String viewSuffix;
    ApplicationContext springContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                Object[] parameters = buildParameters(req, resp, controllerMethod);

                Object result = controllerMethod.invoke(controller, parameters);

                if (controllerMethod.isAnnotationPresent(WebAPI.class)) { 
                    resp.setContentType("application/json;charset=UTF-8");
                    try (PrintWriter out = resp.getWriter()) {
                        if (result instanceof String text) {
                            out.print(text);
                        } else {
                            out.print(objectMapper.writeValueAsString(result));
                        }
                    }
                    return;
                }

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

    private Object[] buildParameters(HttpServletRequest req, HttpServletResponse resp, Method controllerMethod) {
        Class<?>[] parameterTypes = controllerMethod.getParameterTypes();
        Parameter[] parameters = controllerMethod.getParameters();
        Object[] values = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramType = parameterTypes[i];

            if (ApplicationContext.class.isAssignableFrom(paramType)) {
                values[i] = springContext;
                continue;
            }

            if (HttpServletRequest.class.isAssignableFrom(paramType)) {
                values[i] = req;
                continue;
            }

            if (HttpServletResponse.class.isAssignableFrom(paramType)) {
                values[i] = resp;
                continue;
            }

            String paramName = null;
            if (i < parameters.length && parameters[i].isNamePresent()) {
                paramName = parameters[i].getName();
            }

            if (paramName != null && !paramName.isBlank()) {
                values[i] = resolveValue(req, paramName, paramType);
            } else {
                values[i] = defaultValueFor(paramType);
            }
        }

        return values;
    }

    private Object resolveValue(HttpServletRequest req, String paramName, Class<?> paramType) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return defaultValueFor(paramType);
        }

        String[] rawValues = parameterMap.get(paramName);
        if (rawValues == null || rawValues.length == 0) {
            return defaultValueFor(paramType);
        }

        if (paramType.isArray()) {
            return convertArray(rawValues, paramType.getComponentType());
        }

        return convertScalar(rawValues[0], paramType);
    }

    private Object convertArray(String[] rawValues, Class<?> componentType) {
        Object array = java.lang.reflect.Array.newInstance(componentType, rawValues.length);
        for (int i = 0; i < rawValues.length; i++) {
            java.lang.reflect.Array.set(array, i, convertScalar(rawValues[i], componentType));
        }
        return array;
    }

    private Object convertScalar(String rawValue, Class<?> targetType) {
        if (targetType == String.class) {
            return rawValue;
        }
        if (targetType == String[].class) {
            return new String[] { rawValue };
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(rawValue);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(rawValue);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(rawValue);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(rawValue);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(rawValue);
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.parseShort(rawValue);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.parseByte(rawValue);
        }
        if (targetType == Character.class || targetType == char.class) {
            return rawValue.isEmpty() ? '\0' : rawValue.charAt(0);
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType, rawValue);
        }
        return rawValue;
    }

    private Object defaultValueFor(Class<?> paramType) {
        if (paramType == boolean.class) return false;
        if (paramType == byte.class) return (byte) 0;
        if (paramType == short.class) return (short) 0;
        if (paramType == int.class) return 0;
        if (paramType == long.class) return 0L;
        if (paramType == float.class) return 0F;
        if (paramType == double.class) return 0D;
        if (paramType == char.class) return '\0';
        return null;
    }
}