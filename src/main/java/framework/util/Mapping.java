package framework.util;

import java.lang.reflect.Method;

public class Mapping {
    private String className;
    private String methodName;
    private Class<?> controllerClass;
    private Method method;

    public Mapping(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public Mapping(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.className = controllerClass.getName();
        this.methodName = method.getName();
    }

    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public Class<?> getControllerClass() { return controllerClass; }
    public Method getMethod() { return method; }
}