package com.github.lazyben;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Server {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final ServerSocket server;
    private final Map<Integer, ClientConnection> clients = new ConcurrentHashMap<>();

    public Server(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    public static void main(String[] args) throws IOException {
        new Server(8080).start();
    }

    public void start() throws IOException {
        while (true) {
            // 没有客户端插销，就一直卡在这里。这个方法返回一个对象Socket。
            // 使用socket.getInputStream().read()去读数据是一件很没有效率的事情。在读数据时，其他客户端就连接不上了。
            // 所以需要将其放到新的线程中去
            final Socket socket = server.accept();
            new ClientConnection(COUNTER.incrementAndGet(), this, socket).start();
        }
    }

    public String getAllUserInfo() {
        return clients.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().getClientName()).collect(Collectors.joining(","));
    }

    public void registerClient(ClientConnection clientConnection) {
        clients.put(clientConnection.getClientId(), clientConnection);
        this.clientOnline(clientConnection);
    }

    private void dispatchMessage(ClientConnection client, String src, String target, String message) {
        try {
            client.sendMessage(src + "对" + target + "说：" + message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(ClientConnection src, Message message) {
        if (message.getId() == 0) {
            clients.values().forEach(client -> dispatchMessage(client, src.getClientName(), "所有人", message.getMessage()));
        } else {
            int targetUser = message.getId();
            final ClientConnection target = clients.get(targetUser);
            if (target == null) {
                System.err.println("用户" + targetUser + "不存在");
            } else {
                dispatchMessage(target, src.getClientName(), "你", message.getMessage());
            }
        }
    }

    public void clientOffline(ClientConnection clientConnection) {
        clients.remove(clientConnection.getClientId());
        clients.values().forEach(client -> dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "下线了。当前用户：" + getAllUserInfo()));
    }

    private void clientOnline(ClientConnection clientWhoJustLoggedIn) {
        clients.values().forEach(client -> dispatchMessage(client, "系统", "所有人", clientWhoJustLoggedIn.getClientName() + "上线了。当前用户：" + getAllUserInfo()));
    }
}

class ClientConnection extends Thread {
    private final Socket socket;
    private final int clientId;
    private String clientName;
    private final Server server;

    public int getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public ClientConnection(int clientId, Server server, Socket socket) {
        this.clientId = clientId;
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (isNotLoginYet()) {
                    clientName = line;
                    server.registerClient(this);
                } else {
                    Message message = JSON.parseObject(line, Message.class);
                    server.sendMessage(this, message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 处理下线逻辑。
            server.clientOffline(this);
        }
    }

    private boolean isNotLoginYet() {
        return clientName == null;
    }

    public void sendMessage(String message) throws IOException {
        Util.writeMessage(socket, message);
    }
}

class Message {
    private Integer id;
    private String message;

    public Message() {
    }

    public Message(Integer id, String message) {
        this.id = id;
        this.message = message;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
