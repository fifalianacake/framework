package framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import framework.util.AnnotationScanner;

public class FrontController extends HttpServlet {

    List<String> controllerList;

    @Override
    public void init() throws ServletException {

        String basePackages = getInitParameter("basePackages");

        if (basePackages == null || basePackages.isBlank()) {
            throw new ServletException("Missing required init-param 'basePackages' in web.xml");
        }

        try {
            controllerList = AnnotationScanner.findAnnotatedClasses(
                    basePackages, "framework.annotation.Controller");

            for (String name : controllerList) {
                System.out.println("Found controller: " + name);
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }

        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) {
        String uri = req.getRequestURI();
        resp.setContentType("text/plain");

        try {
            PrintWriter out = resp.getWriter();
            out.println("URL : " + uri);

            out.println("Registered controllers:");
            for (String controllerName : controllerList) {
                out.println(" - " + controllerName);
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }
}