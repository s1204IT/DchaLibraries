package android.hardware.hdmi;

import android.annotation.SystemApi;
import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Log;

@SystemApi
public final class HdmiPlaybackClient extends HdmiClient {
    private static final int ADDR_TV = 0;
    private static final String TAG = "HdmiPlaybackClient";

    public interface DisplayStatusCallback {
        void onComplete(int i);
    }

    public interface OneTouchPlayCallback {
        void onComplete(int i);
    }

    HdmiPlaybackClient(IHdmiControlService iHdmiControlService) {
        super(iHdmiControlService);
    }

    public void oneTouchPlay(OneTouchPlayCallback oneTouchPlayCallback) {
        try {
            this.mService.oneTouchPlay(getCallbackWrapper(oneTouchPlayCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "oneTouchPlay threw exception ", e);
        }
    }

    @Override
    public int getDeviceType() {
        return 4;
    }

    public void queryDisplayStatus(DisplayStatusCallback displayStatusCallback) {
        try {
            this.mService.queryDisplayStatus(getCallbackWrapper(displayStatusCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "queryDisplayStatus threw exception ", e);
        }
    }

    public void sendStandby() {
        try {
            this.mService.sendStandby(getDeviceType(), HdmiDeviceInfo.idForCecDevice(0));
        } catch (RemoteException e) {
            Log.e(TAG, "sendStandby threw exception ", e);
        }
    }

    private IHdmiControlCallback getCallbackWrapper(final OneTouchPlayCallback oneTouchPlayCallback) {
        return new IHdmiControlCallback.Stub() {
            @Override
            public void onComplete(int i) {
                oneTouchPlayCallback.onComplete(i);
            }
        };
    }

    private IHdmiControlCallback getCallbackWrapper(final DisplayStatusCallback displayStatusCallback) {
        return new IHdmiControlCallback.Stub() {
            @Override
            public void onComplete(int i) {
                displayStatusCallback.onComplete(i);
            }
        };
    }
}
