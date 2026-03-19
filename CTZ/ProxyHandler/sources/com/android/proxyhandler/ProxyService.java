package com.android.proxyhandler;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.net.IProxyCallback;
import com.android.net.IProxyPortListener;

public class ProxyService extends Service {
    private static ProxyServer server = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (server == null) {
            server = new ProxyServer();
            server.startServer();
        }
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stopServer();
            server = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IProxyCallback.Stub() {
            public void getProxyPort(IBinder iBinder) throws RemoteException {
                IProxyPortListener iProxyPortListenerAsInterface;
                if (ProxyService.server != null && (iProxyPortListenerAsInterface = IProxyPortListener.Stub.asInterface(iBinder)) != null) {
                    ProxyService.server.setCallback(iProxyPortListenerAsInterface);
                }
            }
        };
    }
}
