package com.android.phone;

import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.CallManager;
import java.util.ArrayList;

class CallStateMonitor extends Handler {
    private static final boolean DBG;
    private static final String LOG_TAG = CallStateMonitor.class.getSimpleName();
    private CallManager callManager;
    private ArrayList<Handler> registeredHandlers = new ArrayList<>();

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
    }

    public CallStateMonitor(CallManager callManager) {
        this.callManager = callManager;
        registerForNotifications();
    }

    private void registerForNotifications() {
        this.callManager.registerForCdmaOtaStatusChange(this, 20, (Object) null);
        this.callManager.registerForDisplayInfo(this, 6, (Object) null);
        this.callManager.registerForSignalInfo(this, 7, (Object) null);
        this.callManager.registerForInCallVoicePrivacyOn(this, 9, (Object) null);
        this.callManager.registerForInCallVoicePrivacyOff(this, 10, (Object) null);
        this.callManager.registerForSuppServiceFailed(this, 14, (Object) null);
        this.callManager.registerForTtyModeReceived(this, 15, (Object) null);
    }

    public void addListener(Handler handler) {
        if (handler != null && !this.registeredHandlers.contains(handler)) {
            if (DBG) {
                Log.d(LOG_TAG, "Adding Handler: " + handler);
            }
            this.registeredHandlers.add(handler);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (DBG) {
            Log.d(LOG_TAG, "handleMessage(" + msg.what + ")");
        }
        for (Handler handler : this.registeredHandlers) {
            handler.handleMessage(msg);
        }
    }

    public void updateAfterRadioTechnologyChange() {
        if (DBG) {
            Log.d(LOG_TAG, "updateCallNotifierRegistrationsAfterRadioTechnologyChange...");
        }
        this.callManager.unregisterForDisplayInfo(this);
        this.callManager.unregisterForSignalInfo(this);
        this.callManager.unregisterForCdmaOtaStatusChange(this);
        this.callManager.unregisterForInCallVoicePrivacyOn(this);
        this.callManager.unregisterForInCallVoicePrivacyOff(this);
        this.callManager.unregisterForSuppServiceFailed(this);
        this.callManager.unregisterForTtyModeReceived(this);
        registerForNotifications();
    }
}
