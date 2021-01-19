package com.sd;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class Message implements Serializable {
    private String from;
    private String to;
    private String content;

    public Message() {
    }

    public Message(String from, String to, String content) {
        this.from = from;
        this.to = to;
        this.content = content;
    }

    @Override
    public String toString() {
        return from + "\r\n" + to + "\r\n" + content + "\r\n";
    }

    public static Message fromString(String s) {
        Message message = new Message();
        String[] array = s.split("\r\n");
        if (array.length < 3) {
            return null;
        }
        message.setFrom(array[0]);
        message.setTo(array[1]);
        message.setContent(array[2]);
        return message;
    }

    public static Message fromStream(InputStream in) throws IOException {
        StringBuilder[] sb = new StringBuilder[3];
        for (int i = 0; i < 3; i++) {
            sb[i] = new StringBuilder();
            int ch;
            while ((ch = in.read()) != '\r') {
                sb[i].append((char) ch);
            }
            in.read();
        }
        return new Message(sb[0].toString(), sb[1].toString(), sb[2].toString());
    }

    public static Message fromBuffer(ByteBuffer buffer) {
        buffer.flip();
        String s = new String(buffer.array(), 0, buffer.remaining());
        return fromString(s);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}