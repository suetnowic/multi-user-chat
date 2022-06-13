package com.viktorsuetnov.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        System.out.println("Please enter server port");
        BufferedReader readerServerPort = new BufferedReader(new InputStreamReader(System.in));
        Integer port = null;
        try {
            port = Integer.parseInt(readerServerPort.readLine());
        } catch (IOException e) {
            System.out.println("Oops, an error occurred, please try again.");
        }
        try (ServerSocket socket = new ServerSocket(port)){
            System.out.println("server started successfully");
            while (true) {
                try (Socket client = socket.accept()) {
                    Handler handler = new Handler(client);
                    handler.start();
                } catch (Exception e) {
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("Server connection error");
        }
    }

    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }
    }
}
