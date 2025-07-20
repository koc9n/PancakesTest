package org.pancakelab;

import org.pancakelab.http.PancakeHttpServer;
import org.pancakelab.service.ServiceFactory;

public class Main {
    public static void main(String[] args) {
        try {
            // Create service factory for dependency injection
            ServiceFactory serviceFactory = new ServiceFactory();

            // Create server with dependency injection
            PancakeHttpServer server = new PancakeHttpServer(8080, 10, serviceFactory);
            server.start();

            // Shutdown hook to stop the server gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}