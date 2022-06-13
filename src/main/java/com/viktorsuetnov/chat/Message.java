package com.viktorsuetnov.chat;

import java.io.Serializable;

public class Message implements Serializable {

    private final MessageType messageType;
    private final String data;

    public Message(MessageType messageType, String data) {
        this.messageType = messageType;
        this.data = data;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getData() {
        return data;
    }
}
