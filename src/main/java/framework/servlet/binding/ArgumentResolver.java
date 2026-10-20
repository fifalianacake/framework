package framework.servlet.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import framework.upload.FileStorage;
import framework.upload.UploadedFile;
import framework.util.Session;


public class ArgumentResolver {

    private final ApplicationContext springContext;
    private final FileStorage fileStorage;
    private final ObjectBinder objectBinder;

    public ArgumentResolver(ApplicationContext springContext, FileStorage fileStorage, ObjectMapper objectMapper) {
        this.springContext = springContext;
        this.fileStorage = fileStorage;
        this.objectBinder = new ObjectBinder(objectMapper);
    }

    public Object[] resolve(HttpServletRequest req, HttpServletResponse resp, Method method, Session session) {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            values[i] = resolveOne(req, resp, parameters[i], session);
        }
        return values;
    }

    private Object resolveOne(HttpServletRequest req, HttpServletResponse resp, Parameter parameter, Session session) {
        Class<?> type = parameter.getType();

        // 1. Framework objects injected by type
        if (ApplicationContext.class.isAssignableFrom(type)) return springContext;
        if (HttpServletRequest.class.isAssignableFrom(type)) return req;
        if (HttpServletResponse.class.isAssignableFrom(type)) return resp;
        if (FileStorage.class.isAssignableFrom(type)) return fileStorage;
        if (Session.class.isAssignableFrom(type)) return session;

        String name = parameter.isNamePresent() ? parameter.getName() : null;

        if (type == UploadedFile.class) {
            return name == null ? null : MultipartFiles.first(req, name);
        }
        if (type == UploadedFile[].class) {
            return name == null ? null : MultipartFiles.all(req, name);
        }

        if (!ValueConverter.isSimpleType(type)) {
            return objectBinder.bind(req, type);
        }

        return resolveSimple(req, name, type);
    }

    private static Object resolveSimple(HttpServletRequest req, String name, Class<?> type) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        if (name == null || parameterMap == null || parameterMap.isEmpty()) {
            return ValueConverter.defaultValueFor(type);
        }
        String[] raw = parameterMap.get(name);
        if (raw == null || raw.length == 0) {
            return ValueConverter.defaultValueFor(type);
        }
        if (type.isArray()) {
            return ValueConverter.convertArray(raw, type.getComponentType());
        }
        return ValueConverter.convertScalar(raw[0], type);
    }
}
