package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import framework.util.AnnotationScanner;
import framework.util.Mapping;

public class FrontController extends HttpServlet {

    HashMap<String, Mapping> urlMapping;

    @Override
    public void init() throws ServletException {

        String basePackages = getInitParameter("basePackages");

        if (basePackages == null || basePackages.isBlank()) {
            throw new ServletException("Missing required init-param 'basePackages'");
        }

        try {
            urlMapping = AnnotationScanner.scanControllers(basePackages);

            for (String url : urlMapping.keySet()) {
                Mapping m = urlMapping.get(url);
                System.out.println(url + " -> " + m.getClassName() + "." + m.getMethodName());
            }

        } catch (Exception e) {
            throw new ServletException(e);
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

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) {

        String uri = req.getRequestURI();
        String context = req.getContextPath();
        String path = uri.substring(context.length());

        resp.setContentType("text/plain");

        try {
            PrintWriter out = resp.getWriter();

            out.println("URL : " + path);

            Mapping mapping = urlMapping.get(path);

            if (mapping != null) {
                out.println("Class: " + mapping.getClassName());
                out.println("Method: " + mapping.getMethodName());
            } else {
                out.println("No available mapping for this URL.");
                out.println("Available URLs:");

                for (String key : urlMapping.keySet()) {
                    out.println(" - " + key);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}