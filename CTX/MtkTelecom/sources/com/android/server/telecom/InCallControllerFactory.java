package com.android.server.telecom;

import android.content.Context;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.Timeouts;

public interface InCallControllerFactory {
    InCallController create(Context context, TelecomSystem.SyncRoot syncRoot, CallsManager callsManager, SystemStateProvider systemStateProvider, DefaultDialerCache defaultDialerCache, Timeouts.Adapter adapter, EmergencyCallHelper emergencyCallHelper);
}
