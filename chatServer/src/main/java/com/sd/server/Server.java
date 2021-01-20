package com.sd.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sd.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Properties;

public class Server {
    private BiMap<String, SocketChannel> userMap = HashBiMap.create();

    private int port;

    private ByteBuffer buffer = ByteBuffer.allocate(4096);

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
        try (Selector selector = Selector.open();
             ServerSocketChannel ssChannel = ServerSocketChannel.open()) {
            ssChannel.bind(new InetSocketAddress(port));
            ssChannel.configureBlocking(false);
            ssChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (selector.select() > 0) {
                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) {
                        SocketChannel channel = ssChannel.accept();
                        channel.configureBlocking(false);
                        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                        channel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        handle(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server().serve();
    }

    private void handle(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        buffer.clear();
        try {
            channel.read(buffer);
            Message message = Message.fromBuffer(buffer);
            String userName = message.getFrom();
            buffer.clear();
            // 创建连接的请求
            if (message.getTo().equals("server") && message.getContent().equals("init")) {
                // 用户已上线，连接失败
                if (userMap.containsKey(userName)) {
                    buffer.put(new Message("server", userName, "alreadyOnline").toString().getBytes());
                } else {
                    // 连接成功
                    buffer.put(new Message("server", userName, "ok").toString().getBytes());
                    userMap.put(userName, channel);
                    System.out.println(userName + " log in");
                }
                buffer.flip();
                channel.write(buffer);
            } else {
                // 发送数据的请求
                ByteChannel toChannel = userMap.get(message.getTo());
                if (toChannel == null) {
                    // 接收的用户不在线，转发失败
                    buffer.put(new Message("server", userName, "userNotOnline").toString().getBytes());
                    buffer.flip();
                    channel.write(buffer);
                } else {
                    // 接收的用户在线，转发成功
                    buffer.put(message.toString().getBytes());
                    buffer.flip();
                    toChannel.write(buffer);
                }
            }
        } catch (IOException e) {
            // 断开连接的事件（这也是OP_READ）
            System.out.println(userMap.inverse().get(channel) + "connection break");
            try {
                channel.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            userMap.inverse().remove(channel);
        }
    }
}
