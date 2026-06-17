package framework.util;

import java.util.ArrayList;
import java.util.List;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class AnnotationScanner {

    public static List<String> findAnnotatedClasses(String basePackages, String annotationClassName) {
        List<String> result = new ArrayList<>();

        String[] packages = basePackages.split(";");

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(packages)
                .scan()) {

            for (ClassInfo clazz : scanResult.getAllClasses()) {
                if (clazz.hasAnnotation(annotationClassName)) {
                    result.add(clazz.getName());
                }
            }
        }

        return result;
    }
}