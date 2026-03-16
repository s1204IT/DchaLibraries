package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.util.Pair;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.ServiceBinder;
import java.util.HashMap;
import java.util.Iterator;

final class ConnectionServiceRepository implements ServiceBinder.Listener<ConnectionServiceWrapper> {
    private final Context mContext;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final HashMap<Pair<ComponentName, UserHandle>, ConnectionServiceWrapper> mServiceCache = new HashMap<>();

    ConnectionServiceRepository(PhoneAccountRegistrar phoneAccountRegistrar, Context context) {
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mContext = context;
    }

    ConnectionServiceWrapper getService(ComponentName componentName, UserHandle userHandle) {
        Pair<ComponentName, UserHandle> pairCreate = Pair.create(componentName, userHandle);
        ConnectionServiceWrapper connectionServiceWrapper = this.mServiceCache.get(pairCreate);
        if (connectionServiceWrapper == null) {
            ConnectionServiceWrapper connectionServiceWrapper2 = new ConnectionServiceWrapper(componentName, this, this.mPhoneAccountRegistrar, this.mContext, userHandle);
            connectionServiceWrapper2.addListener(this);
            this.mServiceCache.put(pairCreate, connectionServiceWrapper2);
            return connectionServiceWrapper2;
        }
        return connectionServiceWrapper;
    }

    @Override
    public void onUnbind(ConnectionServiceWrapper connectionServiceWrapper) {
        this.mServiceCache.remove(connectionServiceWrapper.getComponentName());
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("mServiceCache:");
        indentingPrintWriter.increaseIndent();
        Iterator<Pair<ComponentName, UserHandle>> it = this.mServiceCache.keySet().iterator();
        while (it.hasNext()) {
            indentingPrintWriter.println((ComponentName) it.next().first);
        }
        indentingPrintWriter.decreaseIndent();
    }
}
