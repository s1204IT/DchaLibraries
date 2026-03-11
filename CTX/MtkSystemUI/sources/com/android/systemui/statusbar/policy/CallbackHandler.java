package com.android.systemui.statusbar.policy;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.policy.NetworkController;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CallbackHandler extends Handler implements NetworkController.EmergencyListener, NetworkController.SignalCallback {
    static final boolean CHATTY;
    static final boolean DEBUG;
    private final ArrayList<NetworkController.EmergencyListener> mEmergencyListeners;
    private final ArrayList<NetworkController.SignalCallback> mSignalCallbacks;

    static {
        boolean z = true;
        DEBUG = Log.isLoggable("CallbackHandler", 3) || FeatureOptions.LOG_ENABLE;
        if (!Log.isLoggable("CallbackHandlerChat", 3) && !FeatureOptions.LOG_ENABLE) {
            z = false;
        }
        CHATTY = z;
    }

    public CallbackHandler() {
        super(Looper.getMainLooper());
        this.mEmergencyListeners = new ArrayList<>();
        this.mSignalCallbacks = new ArrayList<>();
    }

    @VisibleForTesting
    CallbackHandler(Looper looper) {
        super(looper);
        this.mEmergencyListeners = new ArrayList<>();
        this.mSignalCallbacks = new ArrayList<>();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 0:
                Iterator<NetworkController.EmergencyListener> it = this.mEmergencyListeners.iterator();
                while (it.hasNext()) {
                    it.next().setEmergencyCallsOnly(message.arg1 != 0);
                }
                break;
            case 1:
                if (CHATTY) {
                    Log.d("CallbackHandler", "handleMessage(MSG_SUBS_CHANGED), mSignalCallbacks = " + this.mSignalCallbacks);
                }
                Iterator<NetworkController.SignalCallback> it2 = this.mSignalCallbacks.iterator();
                while (it2.hasNext()) {
                    it2.next().setSubs((List) message.obj);
                }
                break;
            case 2:
                Iterator<NetworkController.SignalCallback> it3 = this.mSignalCallbacks.iterator();
                while (it3.hasNext()) {
                    it3.next().setNoSims(message.arg1 != 0, message.arg2 != 0);
                }
                break;
            case 3:
                Iterator<NetworkController.SignalCallback> it4 = this.mSignalCallbacks.iterator();
                while (it4.hasNext()) {
                    it4.next().setEthernetIndicators((NetworkController.IconState) message.obj);
                }
                break;
            case 4:
                Iterator<NetworkController.SignalCallback> it5 = this.mSignalCallbacks.iterator();
                while (it5.hasNext()) {
                    it5.next().setIsAirplaneMode((NetworkController.IconState) message.obj);
                }
                break;
            case 5:
                Iterator<NetworkController.SignalCallback> it6 = this.mSignalCallbacks.iterator();
                while (it6.hasNext()) {
                    it6.next().setMobileDataEnabled(message.arg1 != 0);
                }
                break;
            case 6:
                if (message.arg1 != 0) {
                    this.mEmergencyListeners.add((NetworkController.EmergencyListener) message.obj);
                } else {
                    this.mEmergencyListeners.remove((NetworkController.EmergencyListener) message.obj);
                }
                break;
            case 7:
                if (CHATTY) {
                    Log.d("CallbackHandler", "handleMessage(MSG_ADD_REMOVE_SIGNAL), arg1 = " + message.arg1 + ", obj = " + message.obj);
                }
                if (message.arg1 != 0) {
                    this.mSignalCallbacks.add((NetworkController.SignalCallback) message.obj);
                } else {
                    this.mSignalCallbacks.remove((NetworkController.SignalCallback) message.obj);
                }
                break;
        }
    }

    @Override
    public void setWifiIndicators(final boolean z, final NetworkController.IconState iconState, final NetworkController.IconState iconState2, final boolean z2, final boolean z3, final String str, final boolean z4, final String str2) {
        post(new Runnable() {
            @Override
            public void run() {
                Iterator it = CallbackHandler.this.mSignalCallbacks.iterator();
                while (it.hasNext()) {
                    ((NetworkController.SignalCallback) it.next()).setWifiIndicators(z, iconState, iconState2, z2, z3, str, z4, str2);
                }
            }
        });
    }

    @Override
    public void setMobileDataIndicators(final NetworkController.IconState iconState, final NetworkController.IconState iconState2, final int i, final int i2, final int i3, final int i4, final boolean z, final boolean z2, final String str, final String str2, final boolean z3, final int i5, final boolean z4, final boolean z5) {
        post(new Runnable() {
            @Override
            public void run() {
                for (Iterator it = CallbackHandler.this.mSignalCallbacks.iterator(); it.hasNext(); it = it) {
                    ((NetworkController.SignalCallback) it.next()).setMobileDataIndicators(iconState, iconState2, i, i2, i3, i4, z, z2, str, str2, z3, i5, z4, z5);
                }
            }
        });
    }

    @Override
    public void setSubs(List<SubscriptionInfo> list) {
        if (DEBUG) {
            Log.d("CallbackHandler", "setSubs, subs = " + list);
        }
        obtainMessage(1, list).sendToTarget();
    }

    @Override
    public void setNoSims(boolean z, boolean z2) {
        obtainMessage(2, z ? 1 : 0, z2 ? 1 : 0).sendToTarget();
    }

    @Override
    public void setMobileDataEnabled(boolean z) {
        obtainMessage(5, z ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void setEmergencyCallsOnly(boolean z) {
        obtainMessage(0, z ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void setEthernetIndicators(NetworkController.IconState iconState) {
        obtainMessage(3, iconState).sendToTarget();
    }

    @Override
    public void setIsAirplaneMode(NetworkController.IconState iconState) {
        obtainMessage(4, iconState).sendToTarget();
    }

    public void setListening(NetworkController.EmergencyListener emergencyListener, boolean z) {
        obtainMessage(6, z ? 1 : 0, 0, emergencyListener).sendToTarget();
    }

    public void setListening(NetworkController.SignalCallback signalCallback, boolean z) {
        if (DEBUG) {
            Log.d("CallbackHandler", "setListening, listener = " + signalCallback + ", listening = " + z);
        }
        obtainMessage(7, z ? 1 : 0, 0, signalCallback).sendToTarget();
    }

    public int getSignalCallbackCount() {
        return this.mSignalCallbacks.size();
    }
}
