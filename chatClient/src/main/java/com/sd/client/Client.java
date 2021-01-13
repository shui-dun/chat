package com.sd.client;


import com.sd.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.writeObject(new Message(user, "server", "init"));
            Message result = (Message) in.readObject();
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
                    out.writeObject(message);
                } catch (InputMismatchException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
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
        private ObjectInputStream in;

        public Receiver(Socket socket, ObjectInputStream in) {
            this.socket = socket;
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Message message = (Message) in.readObject();
                    System.out.printf("%s << %s\n\n", message.getFrom(), message.getContent());
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}
