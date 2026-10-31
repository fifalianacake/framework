package framework.util;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerRegistry {

    private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();

    public ControllerRegistry(Collection<Mapping> mappings) {
        for (Mapping mapping : mappings) {
            instances.computeIfAbsent(mapping.getControllerClass(), ControllerRegistry::create);
        }
    }

    public Object get(Class<?> controllerClass) {
        return instances.computeIfAbsent(controllerClass, ControllerRegistry::create);
    }

    public int size() {
        return instances.size();
    }

    private static Object create(Class<?> controllerClass) {
        try {
            return controllerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create controller " + controllerClass.getName()
                    + ". It needs a public no-argument constructor.", e);
        }
    }
}