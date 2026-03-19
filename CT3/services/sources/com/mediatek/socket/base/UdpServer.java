package com.mediatek.socket.base;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import com.mediatek.socket.base.SocketUtils;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpServer implements SocketUtils.UdpServerInterface {
    private SocketUtils.BaseBuffer mBuff;
    private String mChannelName;
    private DataInputStream mIn;
    private boolean mIsLocalSocket = false;
    private LocalSocket mLocalSocket;
    private LocalSocketAddress.Namespace mNamespace;
    private DatagramSocket mNetSocket;
    private DatagramPacket mPacket;
    private int mPort;

    public UdpServer(int port, int recvBuffSize) {
        this.mBuff = new SocketUtils.BaseBuffer(recvBuffSize);
        this.mPort = port;
        if (bind()) {
        } else {
            throw new RuntimeException("bind() fail");
        }
    }

    public UdpServer(String channelName, LocalSocketAddress.Namespace namespace, int recvBuffSize) {
        this.mBuff = new SocketUtils.BaseBuffer(recvBuffSize);
        this.mChannelName = channelName;
        this.mNamespace = namespace;
        if (bind()) {
        } else {
            throw new RuntimeException("bind() fail");
        }
    }

    public boolean bind() {
        for (int i = 0; i < 5; i++) {
            if (this.mIsLocalSocket) {
                try {
                    this.mLocalSocket = new LocalSocket(1);
                    this.mLocalSocket.bind(new LocalSocketAddress(this.mChannelName, this.mNamespace));
                    this.mIn = new DataInputStream(this.mLocalSocket.getInputStream());
                    return true;
                } catch (IOException e) {
                    if (i == 4) {
                        throw new RuntimeException(e);
                    }
                    msleep(200L);
                }
            } else {
                try {
                    this.mNetSocket = new DatagramSocket(this.mPort);
                    this.mPacket = new DatagramPacket(this.mBuff.getBuff(), this.mBuff.getBuff().length);
                    return true;
                } catch (SocketException e2) {
                    msleep(200L);
                    if (i == 4) {
                        throw new RuntimeException(e2);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean read() {
        this.mBuff.clear();
        if (this.mIsLocalSocket) {
            try {
                return this.mIn.read(this.mBuff.getBuff()) >= 8;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                this.mNetSocket.receive(this.mPacket);
                return true;
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public SocketUtils.BaseBuffer getBuff() {
        return this.mBuff;
    }

    public void close() {
        if (this.mIsLocalSocket) {
            try {
                UdpClient client = new UdpClient(this.mChannelName, this.mNamespace, 128);
                client.connect();
                client.getBuff().putInt(-1);
                client.write();
                client.close();
                this.mLocalSocket.close();
                this.mIn.close();
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        this.mNetSocket.close();
    }

    public int available() {
        if (this.mIsLocalSocket) {
            try {
                return this.mIn.available();
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }
        try {
            throw new RuntimeException("Network Type does not support available() API");
        } catch (Exception e2) {
            e2.printStackTrace();
            return -1;
        }
    }

    @Override
    public boolean setSoTimeout(int timeout) {
        if (this.mIsLocalSocket) {
            try {
                this.mLocalSocket.setSoTimeout(timeout);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        try {
            this.mNetSocket.setSoTimeout(timeout);
            return true;
        } catch (SocketException e2) {
            e2.printStackTrace();
            return false;
        }
    }

    private void msleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
