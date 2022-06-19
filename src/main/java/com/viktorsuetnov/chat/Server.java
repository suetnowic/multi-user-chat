package com.viktorsuetnov.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.viktorsuetnov.chat.Helper.readInt;
import static com.viktorsuetnov.chat.Helper.showMessage;
import static com.viktorsuetnov.chat.MessageType.*;

public class Server {

    private static Map<Integer, Map<String, Connection>> connectionMap = new ConcurrentHashMap<>();
    private static Map<String, String> userMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        showMessage("Please enter server port: ");
        try {
            ServerSocket socket = new ServerSocket(readInt());
            showMessage("server started successfully");
            while (true) {
                new Handler(socket.accept()).start();
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
            String user;
            Integer roomNumber;
            try (Connection connection = new Connection(socket)) {
                remoteSocketAddress = connection.getRemoteSocketAddress();
                showMessage("New connection from " + remoteSocketAddress);
                user = userAuth(connection);
                roomNumber = chooseRoom(connection);
                addUserToRoom(connection, user, roomNumber);
                sendMessageToAllInRoom(new Message(USER_ADDED, user, roomNumber));
                serverMainLoop(connection, user, roomNumber);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void serverMainLoop(Connection connection, String user, Integer roomNumber) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receiveMessage();
                if (message.getMessageType() == MESSAGE) {
                    String msg = String.format("%s : %s", user, message.getData());
                    sendMessageToAllInRoom(new Message(MESSAGE, msg, roomNumber));
                }
            }
        }

        private void sendMessageToAllInRoom(Message message) {
            for (Connection connect : connectionMap.get(message.getRoomNumber()).values()) {
                connect.sendMessage(message);
            }
        }

        private void addUserToRoom(Connection connection, String user, Integer roomNumber) {
            if (!connectionMap.containsKey(roomNumber)) {
                connectionMap.put(roomNumber, new ConcurrentHashMap<>());
            }
            Map<String, Connection> map = connectionMap.get(roomNumber);
            map.put(user, connection);
            connectionMap.put(roomNumber, map);
            connection.sendMessage(new Message(USER_ACCEPTED));
        }

        private String userAuth(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.sendMessage(new Message(AUTHORIZATION));
                Message message = connection.receiveMessage();
                String username = getUsername(message);
                if (message.getMessageType() == AUTHORIZATION &&
                        message.getUser() != null &&
                        !userMap.containsKey(username)) {
                    userRegistration(connection);
                    continue;
                }
                connection.sendMessage(new Message(USER_ACCEPTED));
                return username;
            }
        }

        private int chooseRoom(Connection connection) throws IOException {
            connection.sendMessage(new Message(ROOM_CHOICE));
            Message message = connection.receiveMessage();
            return message.getRoomNumber();
        }
    }

    private static void userRegistration(Connection connection) throws IOException, ClassNotFoundException {
        while (true) {
            connection.sendMessage(new Message(REGISTRATION));
            Message message = connection.receiveMessage();
            String username = getUsername(message);
            String password = getPassword(message);
            if (message.getMessageType() == REGISTRATION &&
                    username != null && !username.isEmpty() && password != null && !password.equals("")) {
                userMap.put(username, password);
                connection.sendMessage(new Message(USER_ACCEPTED));
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
