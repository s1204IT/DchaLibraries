package android.bluetooth;

import java.io.IOException;
import java.io.OutputStream;

final class BluetoothOutputStream extends OutputStream {
    private BluetoothSocket mSocket;

    BluetoothOutputStream(BluetoothSocket s) {
        this.mSocket = s;
    }

    @Override
    public void close() throws IOException {
        this.mSocket.close();
    }

    @Override
    public void write(int oneByte) throws IOException {
        byte[] b = {(byte) oneByte};
        this.mSocket.write(b, 0, 1);
    }

    @Override
    public void write(byte[] b, int offset, int count) throws IOException {
        if (b == null) {
            throw new NullPointerException("buffer is null");
        }
        if ((offset | count) < 0 || count > b.length - offset) {
            throw new IndexOutOfBoundsException("invalid offset or length");
        }
        this.mSocket.write(b, offset, count);
    }

    @Override
    public void flush() throws IOException {
        this.mSocket.flush();
    }
}
