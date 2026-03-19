package com.android.server.telecom;

import android.content.Context;

public interface InCallWakeLockControllerFactory {
    InCallWakeLockController create(Context context, CallsManager callsManager);
}
