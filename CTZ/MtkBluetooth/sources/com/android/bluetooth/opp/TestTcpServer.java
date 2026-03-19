package com.android.bluetooth.opp;

import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ServerRequestHandler;

class TestTcpServer extends ServerRequestHandler implements Runnable {
    static final int PORT = 6500;
    private static final String TAG = "ServerRequestHandler";
    private static final boolean V = Constants.VERBOSE;
    public boolean a = false;

    @Override
    public void run() {
        try {
            updateStatus("[server:] listen on port 6500");
            TestTcpSessionNotifier testTcpSessionNotifier = new TestTcpSessionNotifier(PORT);
            updateStatus("[server:] Now waiting for a client to connect");
            testTcpSessionNotifier.acceptAndOpen(this);
            updateStatus("[server:] A client is now connected");
        } catch (Exception e) {
            updateStatus("[server:] Caught the error: " + e);
        }
    }

    TestTcpServer() {
        updateStatus("enter construtor of TcpServer");
    }

    public int onConnect(HeaderSet headerSet, HeaderSet headerSet2) {
        updateStatus("[server:] The client has created an OBEX session");
        synchronized (this) {
            while (!this.a) {
                try {
                    wait(500L);
                } catch (InterruptedException e) {
                    if (V) {
                        Log.v(TAG, "Interrupted waiting for markBatchFailed");
                    }
                }
            }
        }
        updateStatus("[server:] we accpet the seesion");
        return 160;
    }

    public int onPut(Operation operation) {
        InputStream inputStreamOpenInputStream;
        File file;
        FileOutputStream fileOutputStream;
        int i;
        FileOutputStream fileOutputStream2 = null;
        try {
            inputStreamOpenInputStream = operation.openInputStream();
            updateStatus("Got data bytes " + inputStreamOpenInputStream.available() + " name " + operation.getReceivedHeader().getHeader(1) + " type " + operation.getType());
            file = new File((String) operation.getReceivedHeader().getHeader(1));
            fileOutputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e = e;
        }
        try {
            byte[] bArr = new byte[1000];
            while (inputStreamOpenInputStream.available() > 0 && (i = inputStreamOpenInputStream.read(bArr)) > 0) {
                fileOutputStream.write(bArr, 0, i);
            }
            fileOutputStream.close();
            inputStreamOpenInputStream.close();
            updateStatus("[server:] Wrote data to " + file.getAbsolutePath());
            return 160;
        } catch (Exception e2) {
            fileOutputStream2 = fileOutputStream;
            e = e2;
            if (fileOutputStream2 != null) {
                try {
                    fileOutputStream2.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            e.printStackTrace();
            return 160;
        }
    }

    public void onDisconnect(HeaderSet headerSet, HeaderSet headerSet2) {
        updateStatus("[server:] The client has disconnected the OBEX session");
    }

    public void updateStatus(String str) {
        Log.v(TAG, "\n" + str);
    }

    public void onAuthenticationFailure(byte[] bArr) {
    }

    public int onSetPath(HeaderSet headerSet, HeaderSet headerSet2, boolean z, boolean z2) {
        return 209;
    }

    public int onDelete(HeaderSet headerSet, HeaderSet headerSet2) {
        return 209;
    }

    public int onGet(Operation operation) {
        return 209;
    }
}
