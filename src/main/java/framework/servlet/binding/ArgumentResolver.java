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

public class ArgumentResolver {

    private final ApplicationContext springContext;
    private final FileStorage fileStorage;
    private final ObjectBinder objectBinder;

    public ArgumentResolver(ApplicationContext springContext, FileStorage fileStorage, ObjectMapper objectMapper) {
        this.springContext = springContext;
        this.fileStorage = fileStorage;
        this.objectBinder = new ObjectBinder(objectMapper);
    }

    public Object[] resolve(HttpServletRequest req, HttpServletResponse resp, Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            values[i] = resolveOne(req, resp, parameters[i]);
        }
        return values;
    }

    private Object resolveOne(HttpServletRequest req, HttpServletResponse resp, Parameter parameter) {
        Class<?> type = parameter.getType();

        // 1. Framework objects injected by type
        if (ApplicationContext.class.isAssignableFrom(type)) return springContext;
        if (HttpServletRequest.class.isAssignableFrom(type)) return req;
        if (HttpServletResponse.class.isAssignableFrom(type)) return resp;
        if (FileStorage.class.isAssignableFrom(type)) return fileStorage;

        String name = parameter.isNamePresent() ? parameter.getName() : null;

        // 2. Uploaded files by field name: saveAvatar(UploadedFile photo)
        if (type == UploadedFile.class) {
            return name == null ? null : MultipartFiles.first(req, name);
        }
        if (type == UploadedFile[].class) {
            return name == null ? null : MultipartFiles.all(req, name);
        }

        // 3. Model objects: saveClient(Client client)
        if (!ValueConverter.isSimpleType(type)) {
            return objectBinder.bind(req, type);
        }

        // 4. Simple values by request parameter name: submit(String name, int age)
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
