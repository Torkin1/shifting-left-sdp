package it.torkin.codesmells;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class CodeSmellsServer {
    public static void main(String[] args) {
        try {
            Integer port = Integer.parseInt(System.getenv("GRPC_PORT"));
            Server server = ServerBuilder.forPort(port)
                .addService(new CodeSmellsService())
                .build();
            
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.err.println("shutting down server");
                    server.shutdown().awaitTermination();
                    System.err.println("server shut down");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
            System.out.println("Server started on port " + port);
            server.awaitTermination();
        } catch (NumberFormatException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}