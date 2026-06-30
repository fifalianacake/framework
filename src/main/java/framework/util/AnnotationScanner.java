package framework.util;

import java.util.HashMap;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class AnnotationScanner {

    public static HashMap<UrlMethod, Mapping> scanControllers(String basePackages) throws Exception {

        HashMap<UrlMethod, Mapping> map = new HashMap<>();
        String[] packages = basePackages.split(";");

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(packages)
                .scan()) {

            for (ClassInfo clazzInfo : scanResult.getAllClasses()) {

                if (clazzInfo.hasAnnotation("framework.annotation.Controller")) {

                    Class<?> clazz = Class.forName(clazzInfo.getName());

                    for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {

                        if (method.isAnnotationPresent(framework.annotation.Url.class)) {

                            framework.annotation.Url url =
                                    method.getAnnotation(framework.annotation.Url.class);

                            String path = url.value();
                            String httpMethod = url.method();

                            UrlMethod key = new UrlMethod(path, httpMethod);

                            if (map.containsKey(key)) {
                                throw new Exception("Duplicate route: "
                                        + key.getMethod() + " " + key.getUrl());
                            }

                            map.put(key, new Mapping(clazz.getName(), method.getName()));
                        }
                    }
                }
            }
        }

        return map;
    }
}