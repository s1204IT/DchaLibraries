package android.bluetooth;

import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;

public final class BluetoothServerSocket implements Closeable {
    private static final boolean DBG = false;
    private static final String TAG = "BluetoothServerSocket";
    private int mChannel;
    private Handler mHandler;
    private int mMessage;
    final BluetoothSocket mSocket;

    BluetoothServerSocket(int i, boolean z, boolean z2, int i2) throws IOException {
        this.mChannel = i2;
        this.mSocket = new BluetoothSocket(i, -1, z, z2, null, i2, null);
        if (i2 == -2) {
            this.mSocket.setExcludeSdp(true);
        }
    }

    BluetoothServerSocket(int i, boolean z, boolean z2, int i2, boolean z3, boolean z4) throws IOException {
        this.mChannel = i2;
        this.mSocket = new BluetoothSocket(i, -1, z, z2, null, i2, null, z3, z4);
        if (i2 == -2) {
            this.mSocket.setExcludeSdp(true);
        }
    }

    BluetoothServerSocket(int i, boolean z, boolean z2, ParcelUuid parcelUuid) throws IOException {
        this.mSocket = new BluetoothSocket(i, -1, z, z2, null, -1, parcelUuid);
        this.mChannel = this.mSocket.getPort();
    }

    public BluetoothSocket accept() throws IOException {
        return accept(-1);
    }

    public BluetoothSocket accept(int i) throws IOException {
        return this.mSocket.accept(i);
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (this.mHandler != null) {
                this.mHandler.obtainMessage(this.mMessage).sendToTarget();
            }
        }
        this.mSocket.close();
    }

    synchronized void setCloseHandler(Handler handler, int i) {
        this.mHandler = handler;
        this.mMessage = i;
    }

    void setServiceName(String str) {
        this.mSocket.setServiceName(str);
    }

    public int getChannel() {
        return this.mChannel;
    }

    public int getPsm() {
        return this.mChannel;
    }

    void setChannel(int i) {
        if (this.mSocket != null && this.mSocket.getPort() != i) {
            Log.w(TAG, "The port set is different that the underlying port. mSocket.getPort(): " + this.mSocket.getPort() + " requested newChannel: " + i);
        }
        this.mChannel = i;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServerSocket: Type: ");
        switch (this.mSocket.getConnectionType()) {
            case 1:
                sb.append("TYPE_RFCOMM");
                break;
            case 2:
                sb.append("TYPE_SCO");
                break;
            case 3:
                sb.append("TYPE_L2CAP");
                break;
            case 4:
                sb.append("TYPE_L2CAP_LE");
                break;
        }
        sb.append(" Channel: ");
        sb.append(this.mChannel);
        return sb.toString();
    }
}
