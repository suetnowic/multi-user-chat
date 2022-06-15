package com.viktorsuetnov.chat;

import java.io.IOException;
import java.net.Socket;

import static com.viktorsuetnov.chat.Helper.readInt;
import static com.viktorsuetnov.chat.Helper.readString;

public class Client {

    private Connection connection;
    private volatile boolean clientConnected = false;

    private String getServerAddress() {
        System.out.println("Enter server address");
        return readString();
    }

    private Integer getServerPort() {
        System.out.println("Enter port");
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
            System.out.println("Connection failed");
        }
        if (clientConnected) {
            System.out.println("Connection established. Print exit to quit");
        } else {
            System.out.println("Connection failed");
        }
        while (clientConnected) {
            try {
                String message = readString();
                if (!message.equalsIgnoreCase("exit") && clientConnected) {
                    connection.sendMessage(new Message(MessageType.MESSAGE, message));
                } else
                    break;
            } catch (IOException e) {
                System.out.println("Oops, an error occurred, please try again.");
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
                    System.out.println("Authorization form");
                    System.out.println("Enter your username");
                    String username = readString();
                    System.out.println("Enter your password");
                    String password = readString();
                    connection.sendMessage(new Message(MessageType.AUTHORIZATION, new User(username, password)));
                } else if (message.getMessageType() == MessageType.REGISTRATION) {
                    System.out.println("User with this username is missing");
                    registration();
                } else if (message.getMessageType() == MessageType.USER_ACCEPTED) {
                    System.out.println("Authorization passed");
                    break;
                } else throw new IOException("Unknown message type");
            }
        }

        private void registration() throws IOException, ClassNotFoundException {
            while (true) {
                System.out.println("Registration Form");
                System.out.println("Username must contains [a-zA-Z_0-9]");
                System.out.println("Enter your username");
                String username = readString();
                System.out.println("Enter your password");
                String password = readString();
                connection.sendMessage(new Message(MessageType.REGISTRATION, new User(username, password)));
                Message msg = connection.readMessage();
                if (msg.getMessageType() == MessageType.USER_ACCEPTED) {
                    System.out.println("Registration successfully!");
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

        private void clientMainLoop() throws IOException, ClassNotFoundException {
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
