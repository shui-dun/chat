package com.sd.server;

import com.sd.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Properties;

public class Server {
    private HashMap<String, ObjectOutputStream> userMap = new HashMap<>();

    private int port;

    public Server() {
        try (InputStream inputStream = Server.class.getResourceAsStream("/server.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            port = Integer.parseInt(properties.getProperty("port"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serve() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket socket = server.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String userName = "";
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
            ) {
                Message initMessage = (Message) in.readObject();
                userName = initMessage.getFrom();
                System.out.println(userName + "登录系统");
                if (userMap.get(userName) != null) {
                    out.writeObject(new Message("server", userName, "alreadyOnline"));
                    return;
                }
                out.writeObject(new Message("server", userName, "ok"));
                userMap.put(userName, out);
                while (true) {
                    Message message = (Message) in.readObject();
                    ObjectOutputStream to = userMap.get(message.getTo());
                    if (to!=null) {
                        to.writeObject(message);
                    }else {
                        userMap.get(message.getFrom()).writeObject(new Message("server", message.getFrom(), "userNotOnline"));
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(userName + ":" + e.getMessage());
                userMap.remove(userName);
            }
        }
    }
}