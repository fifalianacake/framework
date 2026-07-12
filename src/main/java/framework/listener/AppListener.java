package framework.listener;

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import framework.util.AnnotationScanner;
import framework.util.Mapping;
import framework.util.UrlMethod;

public class AppListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();

        String basePackages = context.getInitParameter("basePackages");
        String viewPrefix = context.getInitParameter("view.prefix");
        String viewSuffix = context.getInitParameter("view.suffix");

        if (basePackages == null || basePackages.isBlank()) {
            throw new RuntimeException("Missing context-param 'basePackages'");
        }

        try {
            HashMap<UrlMethod, Mapping> urlMapping = AnnotationScanner.scanControllers(basePackages);

            context.setAttribute("urlMapping", urlMapping);
            context.setAttribute("prefix", viewPrefix);
            context.setAttribute("suffix", viewSuffix);

            for (UrlMethod key : urlMapping.keySet()) {
                Mapping m = urlMapping.get(key);

                context.log(
                        key.getMethod() + " " + key.getUrl()
                                + " -> " + m.getClassName() + "." + m.getMethodName());
            }

        } catch (Exception e) {
            context.log("Error during framework initialization", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}