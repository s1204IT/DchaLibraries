package com.android.server.telecom;

import android.content.Context;
import com.android.server.telecom.TelecomSystem;

public interface HeadsetMediaButtonFactory {
    HeadsetMediaButton create(Context context, CallsManager callsManager, TelecomSystem.SyncRoot syncRoot);
}
