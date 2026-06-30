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
import framework.util.UrlMethod;
import framework.util.UrlMethod;

public class FrontController extends HttpServlet {

    HashMap<UrlMethod, Mapping> urlMapping;

    @Override
    public void init() throws ServletException {
        urlMapping = (HashMap<UrlMethod, Mapping>) getServletContext().getAttribute("urlMapping");

        if (urlMapping == null) {
            throw new ServletException("urlMapping not initialized.");
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
        String httpMethod = req.getMethod();

        resp.setContentType("text/plain");

        try {
            PrintWriter out = resp.getWriter();

            out.println("URL : " + path);

            UrlMethod key = new UrlMethod(path, httpMethod);
            Mapping mapping = urlMapping.get(key);

            if (mapping != null) {
                out.println("Class: " + mapping.getClassName());
                out.println("Method: " + mapping.getMethodName());
            } else {
                out.println("No available mapping for this URL.");
                out.println("Available URLs:");

                for (UrlMethod k : urlMapping.keySet()) {
                    out.println(" - " + k);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}