package com.android.server.backup.transport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.android.server.backup.TransportManager;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class TransportClientManager {
    private static final String TAG = "TransportClientManager";
    private final Context mContext;
    private final TransportStats mTransportStats;
    private final Object mTransportClientsLock = new Object();
    private int mTransportClientsCreated = 0;
    private Map<TransportClient, String> mTransportClientsCallerMap = new WeakHashMap();

    public TransportClientManager(Context context, TransportStats transportStats) {
        this.mContext = context;
        this.mTransportStats = transportStats;
    }

    public TransportClient getTransportClient(ComponentName componentName, String str) {
        return getTransportClient(componentName, str, new Intent(TransportManager.SERVICE_ACTION_TRANSPORT_HOST).setComponent(componentName));
    }

    public TransportClient getTransportClient(ComponentName componentName, Bundle bundle, String str) {
        Intent component = new Intent(TransportManager.SERVICE_ACTION_TRANSPORT_HOST).setComponent(componentName);
        component.putExtras(bundle);
        return getTransportClient(componentName, str, component);
    }

    private TransportClient getTransportClient(ComponentName componentName, String str, Intent intent) {
        TransportClient transportClient;
        synchronized (this.mTransportClientsLock) {
            transportClient = new TransportClient(this.mContext, this.mTransportStats, intent, componentName, Integer.toString(this.mTransportClientsCreated), str);
            this.mTransportClientsCallerMap.put(transportClient, str);
            this.mTransportClientsCreated++;
            TransportUtils.log(3, TAG, TransportUtils.formatMessage(null, str, "Retrieving " + transportClient));
        }
        return transportClient;
    }

    public void disposeOfTransportClient(TransportClient transportClient, String str) {
        transportClient.unbind(str);
        transportClient.markAsDisposed();
        synchronized (this.mTransportClientsLock) {
            TransportUtils.log(3, TAG, TransportUtils.formatMessage(null, str, "Disposing of " + transportClient));
            this.mTransportClientsCallerMap.remove(transportClient);
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Transport clients created: " + this.mTransportClientsCreated);
        synchronized (this.mTransportClientsLock) {
            printWriter.println("Current transport clients: " + this.mTransportClientsCallerMap.size());
            for (TransportClient transportClient : this.mTransportClientsCallerMap.keySet()) {
                printWriter.println("    " + transportClient + " [" + this.mTransportClientsCallerMap.get(transportClient) + "]");
                Iterator<String> it = transportClient.getLogBuffer().iterator();
                while (it.hasNext()) {
                    printWriter.println("        " + it.next());
                }
            }
        }
    }
}
