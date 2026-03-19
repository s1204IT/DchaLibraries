package com.android.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import javax.obex.HeaderSet;
import javax.obex.ServerRequestHandler;

public class ObexRejectServer extends ServerRequestHandler implements Handler.Callback {
    private static final int MSG_ID_TIMEOUT = 1;
    private static final String TAG = "ObexRejectServer";
    private static final int TIMEOUT_VALUE = 5000;
    private static final boolean V = true;
    private final HandlerThread mHandlerThread = new HandlerThread("TestTimeoutHandler", 10);
    private final Handler mMessageHandler;
    private final int mResult;
    private final BluetoothSocket mSocket;

    public ObexRejectServer(int i, BluetoothSocket bluetoothSocket) {
        this.mResult = i;
        this.mSocket = bluetoothSocket;
        this.mHandlerThread.start();
        this.mMessageHandler = new Handler(this.mHandlerThread.getLooper(), this);
        this.mMessageHandler.sendEmptyMessageDelayed(1, 5000L);
    }

    public int onConnect(HeaderSet headerSet, HeaderSet headerSet2) {
        Log.i(TAG, "onConnect() returning error");
        return this.mResult;
    }

    public void shutdown() {
        this.mMessageHandler.removeCallbacksAndMessages(null);
        this.mHandlerThread.quit();
        try {
            this.mSocket.close();
        } catch (IOException e) {
            Log.w(TAG, "Unable to close socket - ignoring", e);
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.i(TAG, "Handling message ID: " + message.what);
        if (message.what == 1) {
            shutdown();
            return true;
        }
        return false;
    }
}
