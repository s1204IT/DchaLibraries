package com.android.server.am;

import android.app.IAssistDataReceiver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.am.AssistDataRequester;

class AssistDataReceiverProxy implements AssistDataRequester.AssistDataRequesterCallbacks, IBinder.DeathRecipient {
    private static final String TAG = "ActivityManager";
    private String mCallerPackage;
    private IAssistDataReceiver mReceiver;

    public AssistDataReceiverProxy(IAssistDataReceiver iAssistDataReceiver, String str) {
        this.mReceiver = iAssistDataReceiver;
        this.mCallerPackage = str;
        linkToDeath();
    }

    @Override
    public boolean canHandleReceivedAssistDataLocked() {
        return true;
    }

    @Override
    public void onAssistDataReceivedLocked(Bundle bundle, int i, int i2) {
        if (this.mReceiver != null) {
            try {
                this.mReceiver.onHandleAssistData(bundle);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to proxy assist data to receiver in package=" + this.mCallerPackage, e);
            }
        }
    }

    @Override
    public void onAssistScreenshotReceivedLocked(Bitmap bitmap) {
        if (this.mReceiver != null) {
            try {
                this.mReceiver.onHandleAssistScreenshot(bitmap);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to proxy assist screenshot to receiver in package=" + this.mCallerPackage, e);
            }
        }
    }

    @Override
    public void onAssistRequestCompleted() {
        unlinkToDeath();
    }

    @Override
    public void binderDied() {
        unlinkToDeath();
    }

    private void linkToDeath() {
        try {
            this.mReceiver.asBinder().linkToDeath(this, 0);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not link to client death", e);
        }
    }

    private void unlinkToDeath() {
        if (this.mReceiver != null) {
            this.mReceiver.asBinder().unlinkToDeath(this, 0);
        }
        this.mReceiver = null;
    }
}
