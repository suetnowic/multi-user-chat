package com.viktorsuetnov.chat;

import java.io.IOException;
import java.net.Socket;

import static com.viktorsuetnov.chat.Helper.*;
import static com.viktorsuetnov.chat.MessageType.*;

public class Client {

    private Connection connection;
    private volatile boolean clientConnected = false;

    public void run() {
        SocketThread socketThread = new SocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            showMessage("Connection failed");
        }
        if (clientConnected) {
            showMessage("Connection established. Print exit to quit");
        } else {
            showMessage("Connection failed");
        }
        while (clientConnected) {
            String message = readString();
            if (!message.equalsIgnoreCase("exit") && clientConnected) {
                connection.sendMessage(new Message(MESSAGE, message));
            } else
                break;
        }
    }

    private String getServerAddress() {
        showMessage("Enter server address");
        return readString();
    }

    private Integer getServerPort() {
        showMessage("Enter port");
        return readInt();
    }

    private String getUsername() {
        showMessage("Enter your username");
        return readString();
    }

    private String getPassword() {
        showMessage("Enter your password");
        return readString();
    }

    private int getRoomNumber() {
        showMessage("Choose a room");
        return readInt();
    }

    public static void main(String[] args) {
        new Client().run();
    }

    private class SocketThread extends Thread {

        @Override
        public void run() {
            String host = getServerAddress();
            int port = getServerPort();
            try {
                Socket socket = new Socket(host, port);
                connection = new Connection(socket);
                authorization();
                chooseRoom();
                clientMainLoop();
            } catch (IOException e) {
                connectionStatusChanged(false);
            }
        }

        private void authorization() throws IOException {
            while (true) {
                Message message = connection.receiveMessage();
                if (message.getMessageType() == AUTHORIZATION) {
                    showMessage("Authorization form");
                    String username = getUsername();
                    String password = getPassword();
                    connection.sendMessage(new Message(AUTHORIZATION, new User(username, password)));
                } else if (message.getMessageType() == REGISTRATION) {
                    showMessage("User with this username is missing");
                    registration();
                } else if (message.getMessageType() == USER_ACCEPTED) {
                    showMessage("Authorization passed");
                    break;
                } else throw new IOException("Unknown message type");
            }
        }

        private void registration() throws IOException {
            showMessage("Registration Form");
            while (true) {
                showMessage("Username must contains [a-zA-Z_0-9]");
                String username = getUsername();
                String password = getPassword();
                connection.sendMessage(new Message(REGISTRATION, new User(username, password)));
                Message msg = connection.receiveMessage();
                if (msg.getMessageType() == USER_ACCEPTED) {
                    showMessage("Registration successfully!");
                    break;
                }
            }
        }

        private void connectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        private void chooseRoom() throws IOException {
            while (true) {
                Message message = connection.receiveMessage();
                if (message.getMessageType() == ROOM_CHOICE) {
                    int roomNumber = getRoomNumber();
                    connection.sendMessage(new Message(ROOM_CHOICE, roomNumber));
                } else if (message.getMessageType() == USER_ACCEPTED) {
                    connectionStatusChanged(true);
                    break;
                }
            }
        }

        private void clientMainLoop() throws IOException {
            while (true) {
                Message message = connection.receiveMessage();
                switch (message.getMessageType()) {
                    case MESSAGE:
                        showMessage(message.getData());
                        break;
                    case USER_ADDED:
                        showMessage(String.format("%s join to chat ", message.getData()));
                        break;
                    case USER_REMOVED:
                        showMessage(String.format("%s left chat ", message.getUser()));
                        break;
                    default:
                        throw new IOException("Unknown message type");
                }
            }
        }
    }


}
