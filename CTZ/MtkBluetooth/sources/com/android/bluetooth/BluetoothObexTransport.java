package com.android.bluetooth;

import android.bluetooth.BluetoothSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.obex.ObexTransport;

public class BluetoothObexTransport implements ObexTransport {
    private BluetoothSocket mSocket;

    public BluetoothObexTransport(BluetoothSocket bluetoothSocket) {
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
        return true;
    }

    public int getMaxTransmitPacketSize() {
        if (this.mSocket.getConnectionType() != 3) {
            return -1;
        }
        return this.mSocket.getMaxTransmitPacketSize();
    }

    public int getMaxReceivePacketSize() {
        if (this.mSocket.getConnectionType() != 3) {
            return -1;
        }
        return this.mSocket.getMaxReceivePacketSize();
    }

    public String getRemoteAddress() {
        if (this.mSocket == null) {
            return null;
        }
        return this.mSocket.getRemoteDevice().getAddress();
    }

    public boolean isSrmSupported() {
        if (this.mSocket.getConnectionType() == 3) {
            return true;
        }
        return false;
    }
}
