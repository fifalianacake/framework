package framework.listener;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import framework.util.AnnotationScanner;
import framework.util.Mapping;
import framework.util.UrlMethod;

public class AppListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();
        Properties properties = new Properties();

        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            context.log("Unable to read config.properties", e);
        }

        String basePackages = context.getInitParameter("basePackages");
        String viewPrefix = context.getInitParameter("view.prefix");
        String viewSuffix = context.getInitParameter("view.suffix");

        if (basePackages == null || basePackages.isBlank()) {
            basePackages = properties.getProperty("basePackages");
        }
        if (viewPrefix == null || viewPrefix.isBlank()) {
            viewPrefix = properties.getProperty("view.prefix");
        }
        if (viewSuffix == null || viewSuffix.isBlank()) {
            viewSuffix = properties.getProperty("view.suffix");
        }

        if (basePackages == null || basePackages.isBlank()) {
            throw new RuntimeException("Missing context-param 'basePackages'");
        }

        try {
            HashMap<UrlMethod, Mapping> urlMapping = AnnotationScanner.scanControllers(basePackages);
            ApplicationContext springContext = null;

            try {
                springContext = WebApplicationContextUtils.getWebApplicationContext(context);
            } catch (Exception ignored) {
                context.log("No Spring web application context available; controller injection will be skipped");
            }

            context.setAttribute("urlMapping", urlMapping);
            context.setAttribute("prefix", viewPrefix);
            context.setAttribute("suffix", viewSuffix);
            context.setAttribute("springContext", springContext);

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