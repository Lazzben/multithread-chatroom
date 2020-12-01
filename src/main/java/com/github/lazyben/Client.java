package com.github.lazyben;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println("输入你的昵称");
        Scanner userInput = new Scanner(System.in);
        String name = userInput.nextLine();

        Socket socket = new Socket("127.0.0.1", 8080);

        Util.writeMessage(socket, name);

        System.out.println("连接成功！");

        System.out.println("---------------------------------------------");
        System.out.println("使用贴士：");
        System.out.println("id:message,例如1:hello代表你要想id为1的用户发送消息");
        System.out.println("id=0代表向所有用户发送消息");
        System.out.println("---------------------------------------------");

        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        while (true) {
            System.out.println("输入你要发送的聊天信息");
            String line = userInput.nextLine();

            if (!line.contains(":")) {
                System.err.println("输入的格式不对");
            } else {
                int colonIndex = line.indexOf(':');
                int id = Integer.parseInt(line.substring(0, colonIndex));
                String message = line.substring(colonIndex + 1);

                String json = JSON.toJSONString(new Message(id, message));
                Util.writeMessage(socket, json);
            }
        }
    }
}
