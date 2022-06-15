package com.viktorsuetnov.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.viktorsuetnov.chat.Helper.readInt;
import static com.viktorsuetnov.chat.Helper.showMessage;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    private static Map<String, String> userMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        showMessage("Please enter server port: ");
        Integer port = readInt();
        try (ServerSocket socket = new ServerSocket(port)) {
            showMessage("server started successfully");
            while (true) {
                try (Socket client = socket.accept()) {
                    Handler handler = new Handler(client);
                    handler.start();
                } catch (Exception e) {
                    return;
                }
            }
        } catch (IOException e) {
            showMessage("Server connection error");
        }
    }

    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            SocketAddress remoteSocketAddress;
            String user = null;

            try (Connection connection = new Connection(socket)) {
                remoteSocketAddress = connection.getRemoteSocketAddress();
                showMessage("New connection from " + remoteSocketAddress);
                user = userAuth(connection);
                addUser(connection, user);
                sendMessageToAll(new Message(MessageType.USER_ADDED, user));
                serverMainLoop(connection, user);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void serverMainLoop(Connection connection, String user) throws IOException, ClassNotFoundException {
           while (true) {
               Message message = connection.readMessage();
               if (message.getMessageType() == MessageType.MESSAGE) {
                   sendMessageToAll(new Message(MessageType.MESSAGE, user + ": " + message.getData()));
               }
           }
        }

        private static void sendMessageToAll(Message message) {
            for (Map.Entry<String, Connection> connect : connectionMap.entrySet()) {
                try {
                    connect.getValue().sendMessage(message);
                } catch (IOException e) {
                    showMessage("Message not sent");
                }
            }
        }

        private void addUser(Connection connection, String user) throws IOException {
            connectionMap.put(user, connection);
            connection.sendMessage(new Message(MessageType.USER_ACCEPTED));
        }

        private String userAuth(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.sendMessage(new Message(MessageType.AUTHORIZATION));
                Message message = connection.readMessage();
                String username = getUsername(message);
                if (message.getMessageType() == MessageType.AUTHORIZATION &&
                        message.getUser() != null &&
                        !userMap.containsKey(username)) {
                    userRegistration(connection);
                    continue;
                }
                connection.sendMessage(new Message(MessageType.USER_ACCEPTED));
                return username;
            }
        }
    }

    private static void userRegistration(Connection connection) throws IOException, ClassNotFoundException {
        while (true) {
            connection.sendMessage(new Message(MessageType.REGISTRATION));
            Message message = connection.readMessage();
            String username = getUsername(message);
            String password = getPassword(message);
            if (message.getMessageType() == MessageType.REGISTRATION &&
                    username != null && !username.isEmpty() && password != null && !password.equals("")) {
                userMap.put(username, password);
                connection.sendMessage(new Message(MessageType.USER_ACCEPTED));
                break;
            }
        }
    }

    private static String getPassword(Message message) {
        return message.getUser().getPassword();
    }

    private static String getUsername(Message message) {
        return message.getUser().getUsername();

    }
}
