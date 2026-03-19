package com.android.bluetooth.opp;

import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.obex.Authenticator;
import javax.obex.ServerRequestHandler;
import javax.obex.ServerSession;

class TestTcpSessionNotifier {
    private static final String TAG = "TestTcpSessionNotifier";
    Socket mConn = null;
    ServerSocket mServer;

    TestTcpSessionNotifier(int i) throws IOException {
        this.mServer = null;
        this.mServer = new ServerSocket(i);
    }

    public ServerSession acceptAndOpen(ServerRequestHandler serverRequestHandler, Authenticator authenticator) throws IOException {
        try {
            this.mConn = this.mServer.accept();
        } catch (Exception e) {
            Log.v(TAG, "ex");
        }
        return new ServerSession(new TestTcpTransport(this.mConn), serverRequestHandler, authenticator);
    }

    public ServerSession acceptAndOpen(ServerRequestHandler serverRequestHandler) throws IOException {
        return acceptAndOpen(serverRequestHandler, null);
    }
}
