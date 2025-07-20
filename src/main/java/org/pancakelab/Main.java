package org.pancakelab;

import org.pancakelab.http.PancakeHttpServer;

public class Main {
    public static void main(String[] args) {
        try {
            PancakeHttpServer server = new PancakeHttpServer(8080, 10);
            server.start();

            // Shutdown hook to stop the server gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}