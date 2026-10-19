package framework.servlet.binding;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import framework.upload.UploadedFile;


final class ObjectBinder {

    private static final String BODY_CACHE_ATTRIBUTE = ObjectBinder.class.getName() + ".body";

    private final ObjectMapper objectMapper;

    ObjectBinder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Object bind(HttpServletRequest req, Class<?> type) {
        if (isJson(req)) {
            return bindJson(req, type);
        }
        return bindForm(req, type);
    }

    private Object bindJson(HttpServletRequest req, Class<?> type) {
        String body = readBodyOnce(req);
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON body for " + type.getSimpleName(), e);
        }
    }

    private Object bindForm(HttpServletRequest req, Class<?> type) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        boolean hasFields = parameterMap != null && !parameterMap.isEmpty();
        boolean hasFiles = !MultipartFiles.of(req).isEmpty();
        if (!hasFields && !hasFiles) {
            return null;
        }

        Map<String, Object> values = new HashMap<>();
        if (hasFields) {
            parameterMap.forEach((k, v) -> values.put(k, v.length == 1 ? v[0] : v));
        }
        Object target = objectMapper.convertValue(values, type);

        if (hasFiles) {
            bindFiles(req, target);
        }
        return target;
    }

    /** Sets Client.photo from <input type="file" name="photo">, and so on. */
    private void bindFiles(HttpServletRequest req, Object target) {
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(target.getClass());
        } catch (IntrospectionException e) {
            throw new IllegalStateException("Cannot inspect " + target.getClass().getName(), e);
        }
        for (PropertyDescriptor property : info.getPropertyDescriptors()) {
            Method setter = property.getWriteMethod();
            Class<?> propertyType = property.getPropertyType();
            if (setter == null || propertyType == null) {
                continue;
            }
            Object value = null;
            if (propertyType == UploadedFile.class) {
                value = MultipartFiles.first(req, property.getName());
            } else if (propertyType == UploadedFile[].class) {
                UploadedFile[] files = MultipartFiles.all(req, property.getName());
                value = files.length == 0 ? null : files;
            }
            if (value != null) {
                try {
                    setter.invoke(target, value);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot set " + property.getName(), e);
                }
            }
        }
    }

    private static boolean isJson(HttpServletRequest req) {
        String type = req.getContentType();
        return type != null && type.toLowerCase(Locale.ROOT).startsWith("application/json");
    }

    /** The body stream can only be read once, so keep it for other parameters. */
    private static String readBodyOnce(HttpServletRequest req) {
        Object cached = req.getAttribute(BODY_CACHE_ATTRIBUTE);
        if (cached instanceof String s) {
            return s;
        }
        StringBuilder body = new StringBuilder();
        try {
            BufferedReader reader = req.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read request body", e);
        }
        String result = body.toString();
        req.setAttribute(BODY_CACHE_ATTRIBUTE, result);
        return result;
    }
}
