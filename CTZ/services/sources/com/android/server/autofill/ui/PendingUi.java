package com.android.server.autofill.ui;

import android.os.IBinder;
import android.util.DebugUtils;
import android.view.autofill.IAutoFillManagerClient;

public final class PendingUi {
    public static final int STATE_CREATED = 1;
    public static final int STATE_FINISHED = 4;
    public static final int STATE_PENDING = 2;
    public final IAutoFillManagerClient client;
    private int mState = 1;
    private final IBinder mToken;
    public final int sessionId;

    public PendingUi(IBinder iBinder, int i, IAutoFillManagerClient iAutoFillManagerClient) {
        this.mToken = iBinder;
        this.sessionId = i;
        this.client = iAutoFillManagerClient;
    }

    public IBinder getToken() {
        return this.mToken;
    }

    public void setState(int i) {
        this.mState = i;
    }

    public int getState() {
        return this.mState;
    }

    public boolean matches(IBinder iBinder) {
        return this.mToken.equals(iBinder);
    }

    public String toString() {
        return "PendingUi: [token=" + this.mToken + ", sessionId=" + this.sessionId + ", state=" + DebugUtils.flagsToString(PendingUi.class, "STATE_", this.mState) + "]";
    }
}
