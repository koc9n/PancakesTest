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
 * Enhanced router for handling HTTP requests with named path parameters
 */
public class Router {
    private final List<Route> routes = new ArrayList<>();

    public void addRoute(String method, String pattern, RouteHandler handler) {
        routes.add(new Route(method, compilePattern(pattern), handler, extractParamNames(pattern)));
    }

    public boolean handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        for (Route route : routes) {
            if (route.matches(method, path)) {
                Matcher matcher = route.pattern.matcher(path);
                if (matcher.matches()) {
                    Map<String, String> pathParams = extractPathParams(matcher, route.paramNames);
                    route.handler.handle(exchange, pathParams);
                    return true;
                }
            }
        }
        return false;
    }

    private Pattern compilePattern(String pattern) {
        // Convert named parameters like {orderId} to regex groups
        String regexPattern = pattern.replaceAll("\\{([^}]+)\\}", "([^/]+)");
        return Pattern.compile(regexPattern);
    }

    private List<String> extractParamNames(String pattern) {
        List<String> paramNames = new ArrayList<>();
        Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = paramPattern.matcher(pattern);
        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }
        return paramNames;
    }

    private Map<String, String> extractPathParams(Matcher matcher, List<String> paramNames) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < paramNames.size() && i < matcher.groupCount(); i++) {
            params.put(paramNames.get(i), matcher.group(i + 1));
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
        final List<String> paramNames;

        Route(String method, Pattern pattern, RouteHandler handler, List<String> paramNames) {
            this.method = method;
            this.pattern = pattern;
            this.handler = handler;
            this.paramNames = paramNames;
        }

        boolean matches(String requestMethod, String path) {
            return method.equals(requestMethod) && pattern.matcher(path).matches();
        }
    }
}
