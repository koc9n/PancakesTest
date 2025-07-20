package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple router for handling HTTP requests with path parameters
 */
public class Router {
    private final List<Route> routes = new ArrayList<>();

    public void addRoute(String method, String pattern, RouteHandler handler) {
        routes.add(new Route(method, Pattern.compile(pattern), handler));
    }

    public boolean handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        for (Route route : routes) {
            if (route.matches(method, path)) {
                Matcher matcher = route.pattern.matcher(path);
                if (matcher.matches()) {
                    Map<String, String> pathParams = extractPathParams(matcher);
                    route.handler.handle(exchange, pathParams);
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, String> extractPathParams(Matcher matcher) {
        Map<String, String> params = new HashMap<>();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            params.put("param" + i, matcher.group(i));
        }
        return params;
    }

    @FunctionalInterface
    public interface RouteHandler {
        void handle(HttpExchange exchange, Map<String, String> pathParams) throws IOException;
    }

    private static class Route {
        final String method;
        final Pattern pattern;
        final RouteHandler handler;

        Route(String method, Pattern pattern, RouteHandler handler) {
            this.method = method;
            this.pattern = pattern;
            this.handler = handler;
        }

        boolean matches(String requestMethod, String path) {
            return method.equals(requestMethod) && pattern.matcher(path).matches();
        }
    }
}
