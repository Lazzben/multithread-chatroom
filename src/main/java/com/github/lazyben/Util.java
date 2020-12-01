package com.github.lazyben;

import java.io.IOException;
import java.net.Socket;

public class Util {
    public static void writeMessage(Socket socket, String message) throws IOException {
        socket.getOutputStream().write(message.getBytes());
        socket.getOutputStream().write('\n');
        // socket 这里的io是有缓冲的，可能没有立刻发出去，需要手动"冲下去"。
        socket.getOutputStream().flush();
    }
}
