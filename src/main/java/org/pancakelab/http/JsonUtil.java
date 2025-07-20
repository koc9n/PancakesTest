package org.pancakelab.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class JsonUtil {
    public static <T> T deserialize(InputStream json, Class<T> clazz) {
        String jsonString = new BufferedReader(new InputStreamReader(json))
                .lines().collect(Collectors.joining());
        return fromJson(jsonString, clazz);
    }

    public static <T> T fromJson(HttpExchange exchange, Class<T> clazz) {
        try {
            String jsonString = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());
            return fromJson(jsonString, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON from request: " + e.getMessage(), e);
        }
    }

    public static byte[] serialize(Object obj) {
        return toJson(obj).getBytes();
    }

    public static <T> T parseResponse(HttpResponse<String> response, Class<T> clazz) {
        return fromJson(response.body(), clazz);
    }

    public static <T> T fromJson(String jsonString, Class<T> clazz) {
        try {
            Map<String, Object> jsonMap = parseJson(jsonString);

            if (clazz.isRecord()) {
                return createRecordInstance(jsonMap, clazz);
            } else {
                return createClassInstance(jsonMap, clazz);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    private static <T> T createRecordInstance(Map<String, Object> jsonMap, Class<T> clazz) throws Exception {
        var components = clazz.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] argTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            argTypes[i] = component.getType();
            Object value = jsonMap.get(component.getName());
            args[i] = convertValue(value, component.getType());
        }

        return clazz.getDeclaredConstructor(argTypes).newInstance(args);
    }

    private static <T> T createClassInstance(Map<String, Object> jsonMap, Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (var field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (jsonMap.containsKey(field.getName())) {
                field.set(instance, convertValue(jsonMap.get(field.getName()), field.getType()));
            }
            field.setAccessible(false);
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType == UUID.class && value instanceof String) {
            return UUID.fromString((String) value);
        }

        if (targetType.isEnum() && value instanceof String) {
            return Enum.valueOf((Class<? extends Enum>) targetType, (String) value);
        }

        if ((targetType == int.class || targetType == Integer.class) && value instanceof String) {
            return Integer.parseInt((String) value);
        }

        if ((targetType == long.class || targetType == Long.class) && value instanceof String) {
            return Long.parseLong((String) value);
        }

        if ((targetType == boolean.class || targetType == Boolean.class) && value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }

        if (targetType == List.class) {
            if (value instanceof List) return value;
            if (value instanceof String) return Collections.singletonList(value);
            return Collections.emptyList();
        }

        return value;
    }

    private static Map<String, Object> parseJson(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format");
        }

        Map<String, Object> result = new HashMap<>();
        json = json.substring(1, json.length() - 1).trim();

        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inString = false;
        boolean inKey = true;
        int depth = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;

                if (depth == 0) {
                    if (c == ':') {
                        inKey = false;
                        continue;
                    }
                    if (c == ',' && !inKey) {
                        addKeyValue(result, key.toString().trim(), value.toString().trim());
                        key = new StringBuilder();
                        value = new StringBuilder();
                        inKey = true;
                        continue;
                    }
                }
            }

            if (inKey) {
                key.append(c);
            } else {
                value.append(c);
            }
        }

        if (!key.isEmpty() || !value.isEmpty()) {
            addKeyValue(result, key.toString().trim(), value.toString().trim());
        }

        return result;
    }

    private static void addKeyValue(Map<String, Object> map, String key, String value) {
        key = key.replaceAll("\"", "").trim();
        value = value.trim();

        if (value.startsWith("[") && value.endsWith("]")) {
            List<String> items = parseJsonArray(value);
            map.put(key, items);
        } else if (value.startsWith("{") && value.endsWith("}")) {
            map.put(key, parseJson(value));
        } else {
            map.put(key, value.replaceAll("\"", ""));
        }
    }

    private static List<String> parseJsonArray(String arrayJson) {
        arrayJson = arrayJson.substring(1, arrayJson.length() - 1).trim();
        if (arrayJson.isEmpty()) return Collections.emptyList();

        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        int depth = 0;

        for (int i = 0; i < arrayJson.length(); i++) {
            char c = arrayJson.charAt(i);

            if (c == '"' && (i == 0 || arrayJson.charAt(i - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;

                if (depth == 0 && c == ',') {
                    items.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            items.add(current.toString().trim());
        }

        return items.stream()
                .map(s -> s.replaceAll("^\"|\"$", ""))
                .collect(Collectors.toList());
    }

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        if (obj instanceof List) return listToJson((List<?>) obj);
        if (obj instanceof String) return "\"" + obj + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof UUID) return "\"" + obj + "\"";
        if (obj instanceof Enum<?>) return "\"" + obj + "\"";
        return objectToJson(obj);
    }

    private static String mapToJson(Map<?, ?> map) {
        return "{" + map.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + toJson(e.getValue()))
                .collect(Collectors.joining(",")) + "}";
    }

    private static String listToJson(List<?> list) {
        return "[" + list.stream()
                .map(JsonUtil::toJson)
                .collect(Collectors.joining(",")) + "]";
    }

    private static String objectToJson(Object obj) {
        try {
            Map<String, Object> map = new HashMap<>();
            for (var field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    map.put(field.getName(), value);
                }
                field.setAccessible(false);
            }
            return mapToJson(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
