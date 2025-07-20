package org.pancakelab.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonUtil {
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
        RecordComponent[] components = clazz.getRecordComponents();
        Object[] args = new Object[components.length];
        Class<?>[] argTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            argTypes[i] = component.getType();
            Object value = jsonMap.get(component.getName());
            if (value instanceof List && !component.getType().equals(List.class)) {
                // If we got a List but don't expect one, take the first element
                value = ((List<?>) value).get(0);
            }
            args[i] = convertValue(value, component.getType());
        }

        Constructor<T> constructor = clazz.getDeclaredConstructor(argTypes);
        return constructor.newInstance(args);
    }

    private static <T> T createClassInstance(Map<String, Object> jsonMap, Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        for (var field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (jsonMap.containsKey(field.getName())) {
                Object value = jsonMap.get(field.getName());
                field.set(instance, convertValue(value, field.getType()));
            }
            field.setAccessible(false);
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;

        // Handle primitive types
        if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            return ((Number) value).intValue();
        }
        if (targetType.equals(long.class) || targetType.equals(Long.class)) {
            if (value instanceof String) {
                return Long.parseLong((String) value);
            }
            return ((Number) value).longValue();
        }
        if (targetType.equals(double.class) || targetType.equals(Double.class)) {
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            return ((Number) value).doubleValue();
        }
        if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
            return value;
        }

        // Handle Lists
        if (targetType.equals(List.class)) {
            if (value instanceof List) {
                return value;
            }
            if (value instanceof String) {
                return Collections.singletonList(value);
            }
            return Collections.emptyList();
        }

        return value;
    }

    public static <T> T fromJson(InputStream is, Class<T> clazz) {
        String jsonString = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining());
        return fromJson(jsonString, clazz);
    }

    public static String toJson(Object obj) {
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }

        Map<String, Object> map = new HashMap<>();
        try {
            for (var field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    map.put(field.getName(), value);
                }
                field.setAccessible(false);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
        return mapToJson(map);
    }

    public static <T> T parseResponse(HttpResponse<String> response, Class<T> clazz) {
        return fromJson(response.body(), clazz);
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder("{");
        json.append(map.entrySet().stream()
                .map(e -> String.format("\"%s\":%s",
                        e.getKey(),
                        formatJsonValue(e.getValue())))
                .collect(Collectors.joining(",")));
        json.append("}");
        return json.toString();
    }

    private static String formatJsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + value + "\"";
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return "[" + list.stream()
                    .map(JsonUtil::formatJsonValue)
                    .collect(Collectors.joining(",")) + "]";
        }
        return value.toString();
    }

    private static Map<String, Object> parseJson(String jsonString) {
        Map<String, Object> result = new HashMap<>();
        jsonString = jsonString.trim();
        if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format");
        }

        String content = jsonString.substring(1, jsonString.length() - 1);
        String[] pairs = content.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length != 2) continue;

            String key = keyValue[0].trim().replaceAll("\"", "");
            String value = keyValue[1].trim();

            if (value.startsWith("[") && value.endsWith("]")) {
                // Handle arrays/lists
                String arrayContent = value.substring(1, value.length() - 1);
                List<String> items = List.of(arrayContent.split(","));
                result.put(key, items.stream()
                        .map(item -> item.trim().replaceAll("\"", ""))
                        .collect(Collectors.toList()));
            } else {
                result.put(key, value.replaceAll("\"", ""));
            }
        }
        return result;
    }
}
