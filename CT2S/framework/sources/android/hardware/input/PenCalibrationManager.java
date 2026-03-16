package android.hardware.input;

import android.content.Context;
import android.hardware.input.IPenCalibrationService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public final class PenCalibrationManager {
    static final String TAG = "PenCalibrationManager";
    private final IPenCalibrationService mService = IPenCalibrationService.Stub.asInterface(ServiceManager.getService(Context.PEN_CALIBRATION_SERVICE));

    public void writeBytes(byte[] data) {
        if (this.mService == null) {
            Log.w(TAG, "failed to writeBytes; no pen calibration service.");
            return;
        }
        try {
            this.mService.writeBytes(data);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to writeBytes.", e);
        }
    }

    public void writeChar(char command) {
        if (this.mService == null) {
            Log.w(TAG, "failed to writeChar; no pen calibration service.");
            return;
        }
        try {
            this.mService.writeChar(command);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to writeChar.", e);
        }
    }

    public void read(byte[] buffer) {
        if (this.mService == null) {
            Log.w(TAG, "failed to read; no pen calibration service.");
            return;
        }
        try {
            this.mService.read(buffer);
        } catch (RemoteException e) {
            Log.w(TAG, "failed to read.", e);
        }
    }
}
