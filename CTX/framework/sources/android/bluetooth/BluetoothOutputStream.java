package android.bluetooth;

import java.io.IOException;
import java.io.OutputStream;

final class BluetoothOutputStream extends OutputStream {
    private BluetoothSocket mSocket;

    BluetoothOutputStream(BluetoothSocket bluetoothSocket) {
        this.mSocket = bluetoothSocket;
    }

    @Override
    public void close() throws IOException {
        this.mSocket.close();
    }

    @Override
    public void write(int i) throws IOException {
        this.mSocket.write(new byte[]{(byte) i}, 0, 1);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null) {
            throw new NullPointerException("buffer is null");
        }
        if ((i | i2) < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException("invalid offset or length");
        }
        this.mSocket.write(bArr, i, i2);
    }

    @Override
    public void flush() throws IOException {
        this.mSocket.flush();
    }
}
