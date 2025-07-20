package org.pancakelab.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonUtil {

    public static <T> T fromJson(InputStream is, Class<T> clazz) {
        String jsonString = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining());
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            Map<String, Object> jsonMap = parseJson(jsonString);

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (jsonMap.containsKey(field.getName())) {
                    Object value = jsonMap.get(field.getName());
                    if (field.getType().equals(int.class)) {
                        field.set(instance, Integer.parseInt(value.toString()));
                    } else {
                        field.set(instance, value);
                    }
                }
                field.setAccessible(false);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public static String toJson(Object obj) {
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }

        Map<String, Object> map = new HashMap<>();
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
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
