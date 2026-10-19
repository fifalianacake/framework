package framework.servlet.response;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import framework.annotation.WebAPI;
import framework.upload.UploadedFile;
import framework.util.ModAndView;

/** Turns whatever a controller method returned into the HTTP response. */
public class ResultHandler {

    private final ObjectMapper objectMapper;
    private final String viewPrefix;
    private final String viewSuffix;

    public ResultHandler(ObjectMapper objectMapper, String viewPrefix, String viewSuffix) {
        this.objectMapper = objectMapper;
        this.viewPrefix = viewPrefix == null ? "" : viewPrefix;
        this.viewSuffix = viewSuffix == null ? "" : viewSuffix;
    }

    public void handle(Object result, Method method, HttpServletRequest req, HttpServletResponse resp, Object routeKey)
            throws ServletException, IOException {

        if (method.isAnnotationPresent(WebAPI.class)) {
            writeJson(result, resp);
        } else if (result instanceof ModAndView mav) {
            forwardToView(mav, req, resp, routeKey);
        } else if (result instanceof UploadedFile file) {
            writeFile(file, resp);
        } else if (result instanceof String text) {
            writeText(text, resp);
        } else {
            throw new ServletException("Unsupported return type for " + routeKey + " : "
                    + (result != null ? result.getClass().getName() : "null"));
        }
    }

    private void writeJson(Object result, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print(result instanceof String text ? text : objectMapper.writeValueAsString(result));
        }
    }

    private void forwardToView(ModAndView mav, HttpServletRequest req, HttpServletResponse resp, Object routeKey)
            throws ServletException, IOException {
        for (Map.Entry<String, Object> entry : mav.getValues().entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }
        if (mav.getView() == null || mav.getView().isBlank()) {
            throw new ServletException("No view defined for " + routeKey);
        }
        req.getRequestDispatcher(viewPrefix + mav.getView() + viewSuffix).forward(req, resp);
    }

    private void writeFile(UploadedFile file, HttpServletResponse resp) throws IOException {
        resp.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        resp.setContentLength(file.getContent().length);
        try (OutputStream out = resp.getOutputStream()) {
            out.write(file.getContent());
        }
    }

    private void writeText(String text, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("Method result:");
            out.println(text);
        }
    }
}
