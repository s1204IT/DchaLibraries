package com.android.bluetooth.pbapclient;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticationService extends Service {
    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        this.mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mAuthenticator.getIBinder();
    }
}
