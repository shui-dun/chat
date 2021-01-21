package com.sd.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sd.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Properties;

public class Server {
    private AsynchronousServerSocketChannel ssChannel;

    private BiMap<String, AsynchronousSocketChannel> userMap = HashBiMap.create();

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
        try {
            ssChannel = AsynchronousServerSocketChannel.open();
            ssChannel.bind(new InetSocketAddress(port));
            ssChannel.accept(null, new AcceptHandler());
            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                ssChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 同意创建连接
     */
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            ssChannel.accept(null, this);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            result.read(buffer, buffer, new InitHandler(result));
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            exc.printStackTrace();
        }
    }

    /**
     * 客户端请求加入聊天室
     */
    private class InitHandler implements CompletionHandler<Integer, ByteBuffer> {
        private AsynchronousSocketChannel channel;

        public InitHandler(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            Message message = Message.fromBuffer(attachment);
            String userName = message.getFrom();
            attachment.clear();
            if (userMap.containsKey(userName)) {
                attachment.put(new Message("server", userName, "alreadyOnline").toString().getBytes());
                attachment.flip();
                channel.write(attachment);
            } else {
                attachment.put(new Message("server", userName, "ok").toString().getBytes());
                attachment.flip();
                channel.write(attachment);
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                channel.read(readBuffer, readBuffer, new ReadHandler(channel));
                userMap.put(userName, channel);
                System.out.println(userName + " log in");
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            exc.printStackTrace();
        }
    }

    /**
     * 转发聊天信息
     */
    private class ReadHandler implements CompletionHandler<Integer, ByteBuffer> {
        private AsynchronousSocketChannel channel;

        public ReadHandler(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            Message message = Message.fromBuffer(attachment);
            if (message == null) {
                exit(channel);
                return;
            }
            String userName = message.getFrom();
            attachment.clear();
            AsynchronousSocketChannel toChannel = userMap.get(message.getTo());
            if (toChannel == null) {
                attachment.put(new Message("server", userName, "userNotOnline").toString().getBytes());
                attachment.flip();
                channel.write(attachment);
            } else {
                attachment.put(message.toString().getBytes());
                attachment.flip();
                toChannel.write(attachment);
            }
            ByteBuffer readBuffer = ByteBuffer.allocate(1024);
            channel.read(readBuffer, readBuffer, this);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            exit(channel);
        }
    }

    private void exit(AsynchronousSocketChannel channel) {
        String name = userMap.inverse().get(channel);
        System.out.println(name + " leave");
        userMap.inverse().remove(channel);
    }

    public static void main(String[] args) {
        new Server().serve();
    }
}
