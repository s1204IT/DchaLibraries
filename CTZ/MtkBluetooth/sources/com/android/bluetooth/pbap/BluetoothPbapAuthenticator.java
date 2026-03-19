package com.android.bluetooth.pbap;

import android.util.Log;
import javax.obex.Authenticator;
import javax.obex.PasswordAuthentication;

public class BluetoothPbapAuthenticator implements Authenticator {
    private static final String TAG = "PbapAuthenticator";
    private PbapStateMachine mPbapStateMachine;
    private boolean mChallenged = false;
    private boolean mAuthCancelled = false;
    private String mSessionKey = null;

    BluetoothPbapAuthenticator(PbapStateMachine pbapStateMachine) {
        this.mPbapStateMachine = pbapStateMachine;
    }

    final synchronized void setChallenged(boolean z) {
        this.mChallenged = z;
        notify();
    }

    final synchronized void setCancelled(boolean z) {
        this.mAuthCancelled = z;
        notify();
    }

    final synchronized void setSessionKey(String str) {
        this.mSessionKey = str;
    }

    private void waitUserConfirmation() {
        this.mPbapStateMachine.sendMessage(5);
        this.mPbapStateMachine.sendMessageDelayed(6, 30000L);
        synchronized (this) {
            while (!this.mChallenged && !this.mAuthCancelled) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting on isChallenged or AuthCancelled");
                }
            }
        }
    }

    public PasswordAuthentication onAuthenticationChallenge(String str, boolean z, boolean z2) {
        waitUserConfirmation();
        if (this.mSessionKey.trim().length() != 0) {
            return new PasswordAuthentication((byte[]) null, this.mSessionKey.getBytes());
        }
        return null;
    }

    public byte[] onAuthenticationResponse(byte[] bArr) {
        return null;
    }
}
