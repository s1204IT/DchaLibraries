package android.bluetooth;

import android.os.BatteryStats;
import java.io.IOException;
import java.io.InputStream;

final class BluetoothInputStream extends InputStream {
    private BluetoothSocket mSocket;

    BluetoothInputStream(BluetoothSocket s) {
        this.mSocket = s;
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
        byte[] b = new byte[1];
        int ret = this.mSocket.read(b, 0, 1);
        if (ret == 1) {
            return b[0] & BatteryStats.HistoryItem.CMD_NULL;
        }
        return -1;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (b == null) {
            throw new NullPointerException("byte array is null");
        }
        if ((offset | length) < 0 || length > b.length - offset) {
            throw new ArrayIndexOutOfBoundsException("invalid offset or length");
        }
        return this.mSocket.read(b, offset, length);
    }
}
