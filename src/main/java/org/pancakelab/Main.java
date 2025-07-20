package org.pancakelab;

import org.pancakelab.config.Configuration;
import org.pancakelab.http.PancakeHttpServer;
import org.pancakelab.service.ServiceFactory;

public class Main {
    public static void main(String[] args) {
        try {
            // Load configuration
            Configuration config = Configuration.getInstance();

            // Create service factory for dependency injection
            ServiceFactory serviceFactory = new ServiceFactory();

            // Create server with configuration values
            PancakeHttpServer server = new PancakeHttpServer(
                    config.getServerPort(),
                    config.getThreadPoolSize(),
                    serviceFactory
            );
            server.start();

            // Shutdown hook to stop the server gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}