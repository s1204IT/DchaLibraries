package com.android.internal.telephony.imsphone;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImsExternalConnection extends Connection {
    private ImsExternalCall mCall;
    private int mCallId;
    private boolean mIsPullable;
    private final Set<Listener> mListeners;

    public interface Listener {
        void onPullExternalCall(ImsExternalConnection imsExternalConnection);
    }

    protected ImsExternalConnection(Phone phone, int callId, String address, boolean isPullable) {
        super(phone.getPhoneType());
        this.mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mCall = new ImsExternalCall(phone, this);
        this.mCallId = callId;
        this.mAddress = address;
        this.mNumberPresentation = 1;
        this.mIsPullable = isPullable;
        rebuildCapabilities();
        setActive();
    }

    public int getCallId() {
        return this.mCallId;
    }

    @Override
    public Call getCall() {
        return this.mCall;
    }

    @Override
    public long getDisconnectTime() {
        return 0L;
    }

    @Override
    public long getHoldDurationMillis() {
        return 0L;
    }

    @Override
    public String getVendorDisconnectCause() {
        return null;
    }

    @Override
    public void hangup() throws CallStateException {
    }

    @Override
    public void separate() throws CallStateException {
    }

    @Override
    public void proceedAfterWaitChar() {
    }

    @Override
    public void proceedAfterWildChar(String str) {
    }

    @Override
    public void cancelPostDial() {
    }

    @Override
    public int getNumberPresentation() {
        return this.mNumberPresentation;
    }

    @Override
    public UUSInfo getUUSInfo() {
        return null;
    }

    @Override
    public int getPreciseDisconnectCause() {
        return 0;
    }

    @Override
    public boolean isMultiparty() {
        return false;
    }

    @Override
    public void pullExternalCall() {
        for (Listener listener : this.mListeners) {
            listener.onPullExternalCall(this);
        }
    }

    public void setActive() {
        if (this.mCall == null) {
            return;
        }
        this.mCall.setActive();
    }

    public void setTerminated() {
        if (this.mCall == null) {
            return;
        }
        this.mCall.setTerminated();
    }

    public void setIsPullable(boolean isPullable) {
        this.mIsPullable = isPullable;
        rebuildCapabilities();
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(128);
        str.append("[ImsExternalConnection dialogCallId:");
        str.append(this.mCallId);
        str.append(" state:");
        if (this.mCall.getState() == Call.State.ACTIVE) {
            str.append("Active");
        } else if (this.mCall.getState() == Call.State.DISCONNECTED) {
            str.append("Disconnected");
        }
        str.append("]");
        return str.toString();
    }

    private void rebuildCapabilities() {
        int capabilities = 16;
        if (this.mIsPullable) {
            capabilities = 48;
        }
        setConnectionCapabilities(capabilities);
    }
}
