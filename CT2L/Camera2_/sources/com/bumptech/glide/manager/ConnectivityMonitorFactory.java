package com.bumptech.glide.manager;

import android.content.Context;
import com.bumptech.glide.manager.ConnectivityMonitor;

public class ConnectivityMonitorFactory {
    public ConnectivityMonitor build(Context context, ConnectivityMonitor.ConnectivityListener listener) {
        int res = context.checkCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE");
        boolean hasPermission = res == 0;
        if (hasPermission) {
            return new DefaultConnectivityMonitor(context, listener);
        }
        return new NullConnectivityMonitor();
    }
}
