package android.hardware.hdmi;

import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.IHdmiVendorCommandListener;
import android.os.RemoteException;
import android.util.Log;

public abstract class HdmiClient {
    private static final String TAG = "HdmiClient";
    private IHdmiVendorCommandListener mIHdmiVendorCommandListener;
    final IHdmiControlService mService;

    abstract int getDeviceType();

    HdmiClient(IHdmiControlService service) {
        this.mService = service;
    }

    public HdmiDeviceInfo getActiveSource() {
        try {
            return this.mService.getActiveSource();
        } catch (RemoteException e) {
            Log.e(TAG, "getActiveSource threw exception ", e);
            return null;
        }
    }

    public void sendKeyEvent(int keyCode, boolean isPressed) {
        try {
            this.mService.sendKeyEvent(getDeviceType(), keyCode, isPressed);
        } catch (RemoteException e) {
            Log.e(TAG, "sendKeyEvent threw exception ", e);
        }
    }

    public void sendVendorCommand(int targetAddress, byte[] params, boolean hasVendorId) {
        try {
            this.mService.sendVendorCommand(getDeviceType(), targetAddress, params, hasVendorId);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to send vendor command: ", e);
        }
    }

    public void setVendorCommandListener(HdmiControlManager.VendorCommandListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (this.mIHdmiVendorCommandListener != null) {
            throw new IllegalStateException("listener was already set");
        }
        try {
            IHdmiVendorCommandListener wrappedListener = getListenerWrapper(listener);
            this.mService.addVendorCommandListener(wrappedListener, getDeviceType());
            this.mIHdmiVendorCommandListener = wrappedListener;
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set vendor command listener: ", e);
        }
    }

    private static IHdmiVendorCommandListener getListenerWrapper(final HdmiControlManager.VendorCommandListener listener) {
        return new IHdmiVendorCommandListener.Stub() {
            @Override
            public void onReceived(int srcAddress, int destAddress, byte[] params, boolean hasVendorId) {
                listener.onReceived(srcAddress, destAddress, params, hasVendorId);
            }

            @Override
            public void onControlStateChanged(boolean enabled, int reason) {
                listener.onControlStateChanged(enabled, reason);
            }
        };
    }
}
