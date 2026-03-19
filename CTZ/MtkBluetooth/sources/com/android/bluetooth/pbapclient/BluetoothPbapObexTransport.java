package com.android.bluetooth.pbapclient;

import android.bluetooth.BluetoothSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.obex.ObexTransport;

class BluetoothPbapObexTransport implements ObexTransport {
    private BluetoothSocket mSocket;

    BluetoothPbapObexTransport(BluetoothSocket bluetoothSocket) {
        this.mSocket = null;
        this.mSocket = bluetoothSocket;
    }

    public void close() throws IOException {
        this.mSocket.close();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public InputStream openInputStream() throws IOException {
        return this.mSocket.getInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return this.mSocket.getOutputStream();
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
        return this.mSocket.isConnected();
    }

    public int getMaxTransmitPacketSize() {
        return -1;
    }

    public int getMaxReceivePacketSize() {
        return -1;
    }

    public boolean isSrmSupported() {
        return false;
    }
}
