package com.android.bluetooth.opp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.obex.ObexTransport;

class TestTcpTransport implements ObexTransport {
    Socket s;

    public TestTcpTransport(Socket s) {
        this.s = null;
        this.s = s;
    }

    public void close() throws IOException {
        this.s.close();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public InputStream openInputStream() throws IOException {
        return this.s.getInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return this.s.getOutputStream();
    }

    public void connect() throws IOException {
    }

    public void create() throws IOException {
    }

    public void disconnect() throws IOException {
    }

    public void listen() throws IOException {
    }

    public boolean isConnected() throws IOException {
        return this.s.isConnected();
    }
}
