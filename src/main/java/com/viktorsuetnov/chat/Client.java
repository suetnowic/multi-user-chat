package com.viktorsuetnov.chat;

import java.io.IOException;
import java.net.Socket;

import static com.viktorsuetnov.chat.Helper.*;

public class Client {

    private Connection connection;
    private volatile boolean clientConnected = false;

    private String getServerAddress() {
        showMessage("Enter server address");
        return readString();
    }

    private Integer getServerPort() {
        showMessage("Enter port");
        return readInt();
    }

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
            try {
                String message = readString();
                if (!message.equalsIgnoreCase("exit") && clientConnected) {
                    connection.sendMessage(new Message(MessageType.MESSAGE, message));
                } else
                    break;
            } catch (IOException e) {
                showMessage("Oops, an error occurred, please try again.");
            }
        }
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
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                connectionStatusChanged(false);
            }
        }

        private void authorization() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.readMessage();
                if (message.getMessageType() == MessageType.AUTHORIZATION) {
                    showMessage("Authorization form");
                    String username = getUsername();
                    String password = getPassword();
                    connection.sendMessage(new Message(MessageType.AUTHORIZATION, new User(username, password)));
                } else if (message.getMessageType() == MessageType.REGISTRATION) {
                    showMessage("User with this username is missing");
                    registration();
                } else if (message.getMessageType() == MessageType.USER_ACCEPTED) {
                    showMessage("Authorization passed");
                    break;
                } else throw new IOException("Unknown message type");
            }
        }

        private String getUsername() {
            showMessage("Enter your username");
            return readString();
        }

        private String getPassword() {
            showMessage("Enter your password");
            return readString();
        }

        private void registration() throws IOException {
            showMessage("Registration Form");
            while (true) {
                showMessage("Username must contains [a-zA-Z_0-9]");
                String username = getUsername();
                String password = getPassword();
                connection.sendMessage(new Message(MessageType.REGISTRATION, new User(username, password)));
                Message msg = connection.readMessage();
                if (msg.getMessageType() == MessageType.USER_ACCEPTED) {
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

        private void clientMainLoop() throws IOException {
            while (true) {
                Message message = connection.readMessage();
                switch (message.getMessageType()) {
                    case MESSAGE:
                        System.out.println(message.getData());
                        break;
                    case USER_ADDED:
                        System.out.println(String.format("%s join to chat " + message.getData()));
                        break;
                    case USER_REMOVED:
                        System.out.println(String.format("%s left chat " + message.getData()));
                        break;
                    default:
                        throw new IOException("Unknown message type");
                }
            }
        }
    }
}
