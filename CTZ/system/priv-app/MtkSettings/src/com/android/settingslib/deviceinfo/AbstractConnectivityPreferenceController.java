package com.android.settingslib.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import java.lang.ref.WeakReference;

/* loaded from: classes.dex */
public abstract class AbstractConnectivityPreferenceController extends AbstractPreferenceController implements LifecycleObserver, OnStart, OnStop {
    private final BroadcastReceiver mConnectivityReceiver;
    private Handler mHandler;

    protected abstract String[] getConnectivityIntents();

    protected abstract void updateConnectivity();

    public AbstractConnectivityPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mConnectivityReceiver = new BroadcastReceiver() { // from class: com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (ArrayUtils.contains(AbstractConnectivityPreferenceController.this.getConnectivityIntents(), intent.getAction())) {
                    AbstractConnectivityPreferenceController.this.getHandler().sendEmptyMessage(600);
                }
            }
        };
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        this.mContext.unregisterReceiver(this.mConnectivityReceiver);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        for (String str : getConnectivityIntents()) {
            intentFilter.addAction(str);
        }
        this.mContext.registerReceiver(this.mConnectivityReceiver, intentFilter, "android.permission.CHANGE_NETWORK_STATE", null);
    }

    private Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new ConnectivityEventHandler(this);
        }
        return this.mHandler;
    }

    private static class ConnectivityEventHandler extends Handler {
        private WeakReference<AbstractConnectivityPreferenceController> mPreferenceController;

        public ConnectivityEventHandler(AbstractConnectivityPreferenceController abstractConnectivityPreferenceController) {
            this.mPreferenceController = new WeakReference<>(abstractConnectivityPreferenceController);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            AbstractConnectivityPreferenceController abstractConnectivityPreferenceController = this.mPreferenceController.get();
            if (abstractConnectivityPreferenceController == null) {
                return;
            }
            if (message.what == 600) {
                abstractConnectivityPreferenceController.updateConnectivity();
                return;
            }
            throw new IllegalStateException("Unknown message " + message.what);
        }
    }
}
