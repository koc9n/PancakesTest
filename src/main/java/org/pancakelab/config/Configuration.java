package org.pancakelab.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration management for PancakeLab application
 */
public class Configuration {
    private static final Properties properties = new Properties();
    private static final Configuration INSTANCE = new Configuration();

    static {
        loadConfiguration();
    }

    private Configuration() {
    }

    public static Configuration getInstance() {
        return INSTANCE;
    }

    private static void loadConfiguration() {
        // Load from classpath
        try (InputStream is = Configuration.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load application.properties: " + e.getMessage());
        }

        // Override with system properties and environment variables
        properties.putAll(System.getProperties());
        System.getenv().forEach((key, value) -> {
            String propertyKey = key.toLowerCase().replace('_', '.');
            properties.setProperty(propertyKey, value);
        });
    }

    public int getServerPort() {
        return getInt("server.port", 8080);
    }

    public int getThreadPoolSize() {
        return getInt("server.thread.pool.size", 10);
    }

    public int getRequestTimeoutMs() {
        return getInt("server.request.timeout.ms", 30000);
    }

    public int getRateLimitMaxRequests() {
        return getInt("rate.limit.max.requests", 60);
    }

    public int getRateLimitWindowMs() {
        return getInt("rate.limit.window.ms", 60000);
    }

    public int getServerBacklogSize() {
        return getInt("server.backlog.size", 100);
    }

    public int getShutdownTimeoutSeconds() {
        return getInt("server.shutdown.timeout.seconds", 30);
    }

    private String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer value for " + key + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
}
