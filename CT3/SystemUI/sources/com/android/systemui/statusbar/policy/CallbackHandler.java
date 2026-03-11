package com.android.systemui.statusbar.policy;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionInfo;
import com.android.systemui.statusbar.policy.NetworkController;
import java.util.ArrayList;
import java.util.List;

public class CallbackHandler extends Handler implements NetworkController.EmergencyListener, NetworkController.SignalCallback {
    private final ArrayList<NetworkController.EmergencyListener> mEmergencyListeners;
    private final ArrayList<NetworkController.SignalCallback> mSignalCallbacks;

    public CallbackHandler() {
        this.mEmergencyListeners = new ArrayList<>();
        this.mSignalCallbacks = new ArrayList<>();
    }

    CallbackHandler(Looper looper) {
        super(looper);
        this.mEmergencyListeners = new ArrayList<>();
        this.mSignalCallbacks = new ArrayList<>();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                for (NetworkController.EmergencyListener listener : this.mEmergencyListeners) {
                    listener.setEmergencyCallsOnly(msg.arg1 != 0);
                }
                break;
            case 1:
                for (NetworkController.SignalCallback signalCluster : this.mSignalCallbacks) {
                    signalCluster.setSubs((List) msg.obj);
                }
                break;
            case 2:
                for (NetworkController.SignalCallback signalCluster2 : this.mSignalCallbacks) {
                    signalCluster2.setNoSims(msg.arg1 != 0);
                }
                break;
            case 3:
                for (NetworkController.SignalCallback signalCluster3 : this.mSignalCallbacks) {
                    signalCluster3.setEthernetIndicators((NetworkController.IconState) msg.obj);
                }
                break;
            case 4:
                for (NetworkController.SignalCallback signalCluster4 : this.mSignalCallbacks) {
                    signalCluster4.setIsAirplaneMode((NetworkController.IconState) msg.obj);
                }
                break;
            case 5:
                for (NetworkController.SignalCallback signalCluster5 : this.mSignalCallbacks) {
                    signalCluster5.setMobileDataEnabled(msg.arg1 != 0);
                }
                break;
            case 6:
                if (msg.arg1 != 0) {
                    this.mEmergencyListeners.add((NetworkController.EmergencyListener) msg.obj);
                } else {
                    this.mEmergencyListeners.remove((NetworkController.EmergencyListener) msg.obj);
                }
                break;
            case 7:
                if (msg.arg1 != 0) {
                    this.mSignalCallbacks.add((NetworkController.SignalCallback) msg.obj);
                } else {
                    this.mSignalCallbacks.remove((NetworkController.SignalCallback) msg.obj);
                }
                break;
        }
    }

    @Override
    public void setWifiIndicators(final boolean enabled, final NetworkController.IconState statusIcon, final NetworkController.IconState qsIcon, final boolean activityIn, final boolean activityOut, final String description) {
        post(new Runnable() {
            @Override
            public void run() {
                for (NetworkController.SignalCallback callback : CallbackHandler.this.mSignalCallbacks) {
                    callback.setWifiIndicators(enabled, statusIcon, qsIcon, activityIn, activityOut, description);
                }
            }
        });
    }

    @Override
    public void setMobileDataIndicators(final NetworkController.IconState statusIcon, final NetworkController.IconState qsIcon, final int statusType, final int networkIcon, final int volteType, final int qsType, final boolean activityIn, final boolean activityOut, final String typeContentDescription, final String description, final boolean isWide, final int subId) {
        post(new Runnable() {
            @Override
            public void run() {
                for (NetworkController.SignalCallback signalCluster : CallbackHandler.this.mSignalCallbacks) {
                    signalCluster.setMobileDataIndicators(statusIcon, qsIcon, statusType, networkIcon, volteType, qsType, activityIn, activityOut, typeContentDescription, description, isWide, subId);
                }
            }
        });
    }

    @Override
    public void setSubs(List<SubscriptionInfo> subs) {
        obtainMessage(1, subs).sendToTarget();
    }

    @Override
    public void setNoSims(boolean show) {
        obtainMessage(2, show ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void setMobileDataEnabled(boolean enabled) {
        obtainMessage(5, enabled ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void setEmergencyCallsOnly(boolean emergencyOnly) {
        obtainMessage(0, emergencyOnly ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void setEthernetIndicators(NetworkController.IconState icon) {
        obtainMessage(3, icon).sendToTarget();
    }

    @Override
    public void setIsAirplaneMode(NetworkController.IconState icon) {
        obtainMessage(4, icon).sendToTarget();
    }

    public void setListening(NetworkController.EmergencyListener listener, boolean listening) {
        obtainMessage(6, listening ? 1 : 0, 0, listener).sendToTarget();
    }

    public void setListening(NetworkController.SignalCallback listener, boolean listening) {
        obtainMessage(7, listening ? 1 : 0, 0, listener).sendToTarget();
    }
}
