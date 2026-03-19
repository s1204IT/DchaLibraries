package com.mediatek.server.telecom.ext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;

public class DefaultCallMgrExt implements ICallMgrExt {
    @Override
    public int buildCallCapabilities(boolean z) {
        return 0;
    }

    @Override
    public int getCallFeatures(Object obj, int i) {
        return i;
    }

    @Override
    public boolean shouldDisconnectCallsWhenEcc() {
        return true;
    }

    @Override
    public boolean shouldPreventVideoCallIfLowBattery(Context context, Intent intent) {
        return false;
    }

    @Override
    public boolean blockOutgoingCall(Uri uri, PhoneAccountHandle phoneAccountHandle, Bundle bundle) {
        return false;
    }

    @Override
    public boolean shouldResumeHoldCall() {
        return false;
    }
}
