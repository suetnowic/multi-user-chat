package com.viktorsuetnov.chat;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;

public class Connection implements Closeable {

    private final Socket socket;
    private final ObjectInputStream reader;
    private final ObjectOutputStream writer;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new ObjectInputStream(socket.getInputStream());
        this.writer = new ObjectOutputStream(socket.getOutputStream());
    }

    public void sendMessage(Message message) throws IOException {
        synchronized (writer) {
            writer.writeObject(message);
        }
    }

    public Message readMessage() throws IOException, ClassNotFoundException {
        synchronized (reader) {
            return (Message) reader.readObject();
        }
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    @Override
    public void close() throws IOException {
        socket.close();
        writer.close();
        reader.close();
    }
}
