package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.ServiceBinder;
import com.android.server.telecom.TelecomSystem;
import java.util.HashMap;
import java.util.Iterator;

@VisibleForTesting
public class ConnectionServiceRepository {
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final HashMap<Pair<ComponentName, UserHandle>, ConnectionServiceWrapper> mServiceCache = new HashMap<>();
    private final ServiceBinder.Listener<ConnectionServiceWrapper> mUnbindListener = new ServiceBinder.Listener<ConnectionServiceWrapper>() {
        @Override
        public void onUnbind(ConnectionServiceWrapper connectionServiceWrapper) {
            synchronized (ConnectionServiceRepository.this.mLock) {
                ConnectionServiceRepository.this.mServiceCache.remove(Pair.create(connectionServiceWrapper.getComponentName(), connectionServiceWrapper.getUserHandle()));
            }
        }
    };

    ConnectionServiceRepository(PhoneAccountRegistrar phoneAccountRegistrar, Context context, TelecomSystem.SyncRoot syncRoot, CallsManager callsManager) {
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mContext = context;
        this.mLock = syncRoot;
        this.mCallsManager = callsManager;
    }

    @VisibleForTesting
    public ConnectionServiceWrapper getService(ComponentName componentName, UserHandle userHandle) {
        Pair<ComponentName, UserHandle> pairCreate = Pair.create(componentName, userHandle);
        ConnectionServiceWrapper connectionServiceWrapper = this.mServiceCache.get(pairCreate);
        if (connectionServiceWrapper == null) {
            ConnectionServiceWrapper connectionServiceWrapper2 = new ConnectionServiceWrapper(componentName, this, this.mPhoneAccountRegistrar, this.mCallsManager, this.mContext, this.mLock, userHandle);
            connectionServiceWrapper2.addListener(this.mUnbindListener);
            this.mServiceCache.put(pairCreate, connectionServiceWrapper2);
            return connectionServiceWrapper2;
        }
        return connectionServiceWrapper;
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
