package com.mediatek.server.telecom.ext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;

public interface ICallMgrExt {
    boolean blockOutgoingCall(Uri uri, PhoneAccountHandle phoneAccountHandle, Bundle bundle);

    int buildCallCapabilities(boolean z);

    int getCallFeatures(Object obj, int i);

    boolean shouldDisconnectCallsWhenEcc();

    boolean shouldPreventVideoCallIfLowBattery(Context context, Intent intent);

    boolean shouldResumeHoldCall();
}
