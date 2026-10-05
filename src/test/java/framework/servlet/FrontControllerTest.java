package framework.servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import framework.annotation.Url;
import framework.annotation.WebAPI;
import framework.util.Mapping;
import framework.util.UrlMethod;

public class FrontControllerTest {

    public static class UserDto {
        public String name;
        public int age;

        public UserDto(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    public static class ApiController {
        @Url(value = "/api/user", method = "GET")
        @WebAPI
        public UserDto getUser() {
            return new UserDto("Alice", 42);
        }
    }

    public static class FormController {
        @Url(value = "/api/user/create", method = "POST")
        @WebAPI
        public UserDto create(String name, int age) {
            return new UserDto(name, age);
        }
    }

    @Test
    void shouldSerializeApiResponsesAsJson() throws Exception {
        FrontController controller = new FrontController();
        ServletConfig config = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);
        when(config.getServletName()).thenReturn("front-controller");
        when(context.getAttribute("urlMapping")).thenReturn(Map.of(
                new UrlMethod("/api/user", "GET"),
                new Mapping(ApiController.class, ApiController.class.getDeclaredMethod("getUser"))
        ));
        when(context.getAttribute("prefix")).thenReturn("/WEB-INF/views/");
        when(context.getAttribute("suffix")).thenReturn(".jsp");
        when(context.getAttribute("springContext")).thenReturn(null);

        controller.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body);

        when(request.getRequestURI()).thenReturn("/app/api/user");
        when(request.getContextPath()).thenReturn("/app");
        when(request.getMethod()).thenReturn("GET");
        when(response.getWriter()).thenReturn(writer);

        controller.doGet(request, response);

        writer.flush();
        String output = body.toString();

        assertTrue(output.contains("\"name\":\"Alice\""));
        assertTrue(output.contains("\"age\":42"));
        verify(response).setContentType("application/json;charset=UTF-8");
    }

    @Test
    void shouldBindRequestParametersToControllerMethodArguments() throws Exception {
        FrontController controller = new FrontController();
        ServletConfig config = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);
        when(config.getServletName()).thenReturn("front-controller");
        when(context.getAttribute("urlMapping")).thenReturn(Map.of(
                new UrlMethod("/api/user/create", "POST"),
                new Mapping(FormController.class, FormController.class.getDeclaredMethod("create", String.class, int.class))
        ));
        when(context.getAttribute("prefix")).thenReturn("/WEB-INF/views/");
        when(context.getAttribute("suffix")).thenReturn(".jsp");
        when(context.getAttribute("springContext")).thenReturn(null);

        controller.init(config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body);

        when(request.getRequestURI()).thenReturn("/app/api/user/create");
        when(request.getContextPath()).thenReturn("/app");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameterMap()).thenReturn(Map.of(
                "name", new String[] { "Bob" },
                "age", new String[] { "31" }
        ));
        when(response.getWriter()).thenReturn(writer);

        controller.doPost(request, response);

        writer.flush();
        String output = body.toString();

        assertTrue(output.contains("\"name\":\"Bob\""));
        assertTrue(output.contains("\"age\":31"));
        verify(response).setContentType("application/json;charset=UTF-8");
    }
}
