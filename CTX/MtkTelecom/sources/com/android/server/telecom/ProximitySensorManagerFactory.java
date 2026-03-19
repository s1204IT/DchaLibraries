package com.android.server.telecom;

import android.content.Context;

public interface ProximitySensorManagerFactory {
    ProximitySensorManager create(Context context, CallsManager callsManager);
}
