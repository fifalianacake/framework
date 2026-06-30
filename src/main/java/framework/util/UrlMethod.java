package framework.util;

import java.util.Objects;

public class UrlMethod {

    private String url;
    private String httpMethod; 

    public UrlMethod(String url, String httpMethod) {
        this.url = url;
        this.httpMethod = httpMethod.toUpperCase();
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return httpMethod;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlMethod)) return false;

        UrlMethod that = (UrlMethod) o;

        return Objects.equals(url, that.url)
                && Objects.equals(httpMethod, that.httpMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, httpMethod);
    }

    @Override
    public String toString() {
        return httpMethod + " " + url;
    }
}