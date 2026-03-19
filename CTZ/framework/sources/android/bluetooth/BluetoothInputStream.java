package android.bluetooth;

import java.io.IOException;
import java.io.InputStream;

final class BluetoothInputStream extends InputStream {
    private BluetoothSocket mSocket;

    BluetoothInputStream(BluetoothSocket bluetoothSocket) {
        this.mSocket = bluetoothSocket;
    }

    @Override
    public int available() throws IOException {
        return this.mSocket.available();
    }

    @Override
    public void close() throws IOException {
        this.mSocket.close();
    }

    @Override
    public int read() throws IOException {
        byte[] bArr = new byte[1];
        if (this.mSocket.read(bArr, 0, 1) == 1) {
            return bArr[0] & 255;
        }
        return -1;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null) {
            throw new NullPointerException("byte array is null");
        }
        if ((i | i2) < 0 || i2 > bArr.length - i) {
            throw new ArrayIndexOutOfBoundsException("invalid offset or length");
        }
        return this.mSocket.read(bArr, i, i2);
    }
}
