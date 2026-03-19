package com.android.se;

import android.app.Application;
import android.content.Intent;

public class SEApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(getApplicationContext(), (Class<?>) SecureElementService.class));
    }
}
