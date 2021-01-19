package com.sd.client;


import com.sd.Message;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.Scanner;

public class Client {

    private String server;

    private int serverPort;

    private String user;

    public Client() {
        try (InputStream inputStream = Client.class.getResourceAsStream("/client.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            server = properties.getProperty("server");
            serverPort = Integer.parseInt(properties.getProperty("serverPort"));
            user = properties.getProperty("user");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        try (Socket socket = new Socket(server, serverPort);
             OutputStream out = socket.getOutputStream();
             BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
            out.write(new Message(user, "server", "init").toString().getBytes());
            Message result = Message.fromStream(in);
            if (result.getFrom().equals("server") && result.getContent().equals("alreadyOnline")) {
                System.out.println(user + " is already online, connection failed");
                return;
            }
            System.out.println("welcome, " + user);
            Receiver receiver = new Receiver(socket, in);
            receiver.start();
            Scanner scanner = new Scanner(System.in);
            while (true) {
                try {
                    String input = scanner.nextLine();
                    System.out.println();
                    if (input.equals("bye")) {
                        System.exit(0);
                    }
                    Message message = parseInput(input);
                    out.write(message.toString().getBytes());
                } catch (InputMismatchException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }

    private Message parseInput(String s) throws InputMismatchException {
        Message message = new Message();
        message.setFrom(user);
        String[] strings = s.split(" ", 2);
        if (strings.length != 2) {
            throw new InputMismatchException("format error e.g 'jack hello'");
        }
        message.setTo(strings[0]);
        message.setContent(strings[1]);
        return message;
    }

    private class Receiver extends Thread {
        private Socket socket;
        private InputStream in;

        public Receiver(Socket socket, InputStream in) {
            this.socket = socket;
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Message message = Message.fromStream(in);
                    System.out.printf("%s << %s\n\n", message.getFrom(), message.getContent());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
