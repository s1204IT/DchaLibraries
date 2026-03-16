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
    Socket conn = null;
    ServerSocket server;

    public TestTcpSessionNotifier(int port) throws IOException {
        this.server = null;
        this.server = new ServerSocket(port);
    }

    public ServerSession acceptAndOpen(ServerRequestHandler handler, Authenticator auth) throws IOException {
        try {
            this.conn = this.server.accept();
        } catch (Exception e) {
            Log.v(TAG, "ex");
        }
        TestTcpTransport tt = new TestTcpTransport(this.conn);
        return new ServerSession(tt, handler, auth);
    }

    public ServerSession acceptAndOpen(ServerRequestHandler handler) throws IOException {
        return acceptAndOpen(handler, null);
    }
}
