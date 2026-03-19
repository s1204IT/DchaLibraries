package android.hardware.hdmi;

import android.annotation.SystemApi;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.IHdmiVendorCommandListener;
import android.os.RemoteException;
import android.util.Log;

@SystemApi
public abstract class HdmiClient {
    private static final String TAG = "HdmiClient";
    private IHdmiVendorCommandListener mIHdmiVendorCommandListener;
    final IHdmiControlService mService;

    abstract int getDeviceType();

    HdmiClient(IHdmiControlService iHdmiControlService) {
        this.mService = iHdmiControlService;
    }

    public HdmiDeviceInfo getActiveSource() {
        try {
            return this.mService.getActiveSource();
        } catch (RemoteException e) {
            Log.e(TAG, "getActiveSource threw exception ", e);
            return null;
        }
    }

    public void sendKeyEvent(int i, boolean z) {
        try {
            this.mService.sendKeyEvent(getDeviceType(), i, z);
        } catch (RemoteException e) {
            Log.e(TAG, "sendKeyEvent threw exception ", e);
        }
    }

    public void sendVendorCommand(int i, byte[] bArr, boolean z) {
        try {
            this.mService.sendVendorCommand(getDeviceType(), i, bArr, z);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to send vendor command: ", e);
        }
    }

    public void setVendorCommandListener(HdmiControlManager.VendorCommandListener vendorCommandListener) {
        if (vendorCommandListener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        if (this.mIHdmiVendorCommandListener != null) {
            throw new IllegalStateException("listener was already set");
        }
        try {
            IHdmiVendorCommandListener listenerWrapper = getListenerWrapper(vendorCommandListener);
            this.mService.addVendorCommandListener(listenerWrapper, getDeviceType());
            this.mIHdmiVendorCommandListener = listenerWrapper;
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set vendor command listener: ", e);
        }
    }

    private static IHdmiVendorCommandListener getListenerWrapper(final HdmiControlManager.VendorCommandListener vendorCommandListener) {
        return new IHdmiVendorCommandListener.Stub() {
            @Override
            public void onReceived(int i, int i2, byte[] bArr, boolean z) {
                vendorCommandListener.onReceived(i, i2, bArr, z);
            }

            @Override
            public void onControlStateChanged(boolean z, int i) {
                vendorCommandListener.onControlStateChanged(z, i);
            }
        };
    }
}
