package com.android.bluetooth.pbapclient;

import android.os.Handler;
import android.util.Log;
import javax.obex.PasswordAuthentication;

class BluetoothPbapObexAuthenticator implements javax.obex.Authenticator {
    private static final String TAG = "BluetoothPbapObexAuthenticator";
    private final Handler mCallback;
    private String mSessionKey = "0000";

    BluetoothPbapObexAuthenticator(Handler handler) {
        this.mCallback = handler;
    }

    public PasswordAuthentication onAuthenticationChallenge(String str, boolean z, boolean z2) {
        Log.v(TAG, "onAuthenticationChallenge: starting");
        if (this.mSessionKey != null && this.mSessionKey.length() != 0) {
            Log.v(TAG, "onAuthenticationChallenge: mSessionKey=" + this.mSessionKey);
            return new PasswordAuthentication((byte[]) null, this.mSessionKey.getBytes());
        }
        Log.v(TAG, "onAuthenticationChallenge: mSessionKey is empty, timeout/cancel occured");
        return null;
    }

    public byte[] onAuthenticationResponse(byte[] bArr) {
        Log.v(TAG, "onAuthenticationResponse: " + bArr);
        return null;
    }
}
