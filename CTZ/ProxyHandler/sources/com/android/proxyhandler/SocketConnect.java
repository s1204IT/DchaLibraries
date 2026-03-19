package com.android.proxyhandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketConnect extends Thread {
    private InputStream from;
    private OutputStream to;

    public SocketConnect(Socket socket, Socket socket2) throws IOException {
        this.from = socket.getInputStream();
        this.to = socket2.getOutputStream();
        start();
    }

    @Override
    public void run() {
        byte[] bArr = new byte[512];
        while (true) {
            try {
                int i = this.from.read(bArr);
                if (i >= 0) {
                    this.to.write(bArr, 0, i);
                } else {
                    this.from.close();
                    this.to.close();
                    return;
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    public static void connect(Socket socket, Socket socket2) {
        try {
            SocketConnect socketConnect = new SocketConnect(socket, socket2);
            SocketConnect socketConnect2 = new SocketConnect(socket2, socket);
            try {
                socketConnect.join();
            } catch (InterruptedException e) {
            }
            try {
                socketConnect2.join();
            } catch (InterruptedException e2) {
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
    }
}
