package android.hardware.hdmi;

import android.annotation.SystemApi;
import android.hardware.hdmi.HdmiRecordListener;
import android.hardware.hdmi.HdmiRecordSources;
import android.hardware.hdmi.HdmiTimerRecordSources;
import android.hardware.hdmi.IHdmiControlCallback;
import android.hardware.hdmi.IHdmiInputChangeListener;
import android.hardware.hdmi.IHdmiMhlVendorCommandListener;
import android.hardware.hdmi.IHdmiRecordListener;
import android.os.RemoteException;
import android.util.Log;
import java.util.Collections;
import java.util.List;
import libcore.util.EmptyArray;

@SystemApi
public final class HdmiTvClient extends HdmiClient {
    private static final String TAG = "HdmiTvClient";
    public static final int VENDOR_DATA_SIZE = 16;

    public interface HdmiMhlVendorCommandListener {
        void onReceived(int i, int i2, int i3, byte[] bArr);
    }

    public interface InputChangeListener {
        void onChanged(HdmiDeviceInfo hdmiDeviceInfo);
    }

    public interface SelectCallback {
        void onComplete(int i);
    }

    HdmiTvClient(IHdmiControlService iHdmiControlService) {
        super(iHdmiControlService);
    }

    static HdmiTvClient create(IHdmiControlService iHdmiControlService) {
        return new HdmiTvClient(iHdmiControlService);
    }

    @Override
    public int getDeviceType() {
        return 0;
    }

    public void deviceSelect(int i, SelectCallback selectCallback) {
        if (selectCallback == null) {
            throw new IllegalArgumentException("callback must not be null.");
        }
        try {
            this.mService.deviceSelect(i, getCallbackWrapper(selectCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to select device: ", e);
        }
    }

    private static IHdmiControlCallback getCallbackWrapper(final SelectCallback selectCallback) {
        return new IHdmiControlCallback.Stub() {
            @Override
            public void onComplete(int i) {
                selectCallback.onComplete(i);
            }
        };
    }

    public void portSelect(int i, SelectCallback selectCallback) {
        if (selectCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        try {
            this.mService.portSelect(i, getCallbackWrapper(selectCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to select port: ", e);
        }
    }

    public void setInputChangeListener(InputChangeListener inputChangeListener) {
        if (inputChangeListener == null) {
            throw new IllegalArgumentException("listener must not be null.");
        }
        try {
            this.mService.setInputChangeListener(getListenerWrapper(inputChangeListener));
        } catch (RemoteException e) {
            Log.e("TAG", "Failed to set InputChangeListener:", e);
        }
    }

    private static IHdmiInputChangeListener getListenerWrapper(final InputChangeListener inputChangeListener) {
        return new IHdmiInputChangeListener.Stub() {
            @Override
            public void onChanged(HdmiDeviceInfo hdmiDeviceInfo) {
                inputChangeListener.onChanged(hdmiDeviceInfo);
            }
        };
    }

    public List<HdmiDeviceInfo> getDeviceList() {
        try {
            return this.mService.getDeviceList();
        } catch (RemoteException e) {
            Log.e("TAG", "Failed to call getDeviceList():", e);
            return Collections.emptyList();
        }
    }

    public void setSystemAudioMode(boolean z, SelectCallback selectCallback) {
        try {
            this.mService.setSystemAudioMode(z, getCallbackWrapper(selectCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set system audio mode:", e);
        }
    }

    public void setSystemAudioVolume(int i, int i2, int i3) {
        try {
            this.mService.setSystemAudioVolume(i, i2, i3);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set volume: ", e);
        }
    }

    public void setSystemAudioMute(boolean z) {
        try {
            this.mService.setSystemAudioMute(z);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set mute: ", e);
        }
    }

    public void setRecordListener(HdmiRecordListener hdmiRecordListener) {
        if (hdmiRecordListener == null) {
            throw new IllegalArgumentException("listener must not be null.");
        }
        try {
            this.mService.setHdmiRecordListener(getListenerWrapper(hdmiRecordListener));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set record listener.", e);
        }
    }

    public void sendStandby(int i) {
        try {
            this.mService.sendStandby(getDeviceType(), i);
        } catch (RemoteException e) {
            Log.e(TAG, "sendStandby threw exception ", e);
        }
    }

    private static IHdmiRecordListener getListenerWrapper(final HdmiRecordListener hdmiRecordListener) {
        return new IHdmiRecordListener.Stub() {
            @Override
            public byte[] getOneTouchRecordSource(int i) {
                HdmiRecordSources.RecordSource recordSourceOnOneTouchRecordSourceRequested = hdmiRecordListener.onOneTouchRecordSourceRequested(i);
                if (recordSourceOnOneTouchRecordSourceRequested == null) {
                    return EmptyArray.BYTE;
                }
                byte[] bArr = new byte[recordSourceOnOneTouchRecordSourceRequested.getDataSize(true)];
                recordSourceOnOneTouchRecordSourceRequested.toByteArray(true, bArr, 0);
                return bArr;
            }

            @Override
            public void onOneTouchRecordResult(int i, int i2) {
                hdmiRecordListener.onOneTouchRecordResult(i, i2);
            }

            @Override
            public void onTimerRecordingResult(int i, int i2) {
                hdmiRecordListener.onTimerRecordingResult(i, HdmiRecordListener.TimerStatusData.parseFrom(i2));
            }

            @Override
            public void onClearTimerRecordingResult(int i, int i2) {
                hdmiRecordListener.onClearTimerRecordingResult(i, i2);
            }
        };
    }

    public void startOneTouchRecord(int i, HdmiRecordSources.RecordSource recordSource) {
        if (recordSource == null) {
            throw new IllegalArgumentException("source must not be null.");
        }
        try {
            byte[] bArr = new byte[recordSource.getDataSize(true)];
            recordSource.toByteArray(true, bArr, 0);
            this.mService.startOneTouchRecord(i, bArr);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to start record: ", e);
        }
    }

    public void stopOneTouchRecord(int i) {
        try {
            this.mService.stopOneTouchRecord(i);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stop record: ", e);
        }
    }

    public void startTimerRecording(int i, int i2, HdmiTimerRecordSources.TimerRecordSource timerRecordSource) {
        if (timerRecordSource == null) {
            throw new IllegalArgumentException("source must not be null.");
        }
        checkTimerRecordingSourceType(i2);
        try {
            byte[] bArr = new byte[timerRecordSource.getDataSize()];
            timerRecordSource.toByteArray(bArr, 0);
            this.mService.startTimerRecording(i, i2, bArr);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to start record: ", e);
        }
    }

    private void checkTimerRecordingSourceType(int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
                return;
            default:
                throw new IllegalArgumentException("Invalid source type:" + i);
        }
    }

    public void clearTimerRecording(int i, int i2, HdmiTimerRecordSources.TimerRecordSource timerRecordSource) {
        if (timerRecordSource == null) {
            throw new IllegalArgumentException("source must not be null.");
        }
        checkTimerRecordingSourceType(i2);
        try {
            byte[] bArr = new byte[timerRecordSource.getDataSize()];
            timerRecordSource.toByteArray(bArr, 0);
            this.mService.clearTimerRecording(i, i2, bArr);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to start record: ", e);
        }
    }

    public void setHdmiMhlVendorCommandListener(HdmiMhlVendorCommandListener hdmiMhlVendorCommandListener) {
        if (hdmiMhlVendorCommandListener == null) {
            throw new IllegalArgumentException("listener must not be null.");
        }
        try {
            this.mService.addHdmiMhlVendorCommandListener(getListenerWrapper(hdmiMhlVendorCommandListener));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set hdmi mhl vendor command listener: ", e);
        }
    }

    private IHdmiMhlVendorCommandListener getListenerWrapper(final HdmiMhlVendorCommandListener hdmiMhlVendorCommandListener) {
        return new IHdmiMhlVendorCommandListener.Stub() {
            @Override
            public void onReceived(int i, int i2, int i3, byte[] bArr) {
                hdmiMhlVendorCommandListener.onReceived(i, i2, i3, bArr);
            }
        };
    }

    public void sendMhlVendorCommand(int i, int i2, int i3, byte[] bArr) {
        if (bArr == null || bArr.length != 16) {
            throw new IllegalArgumentException("Invalid vendor command data.");
        }
        if (i2 < 0 || i2 >= 16) {
            throw new IllegalArgumentException("Invalid offset:" + i2);
        }
        if (i3 < 0 || i2 + i3 > 16) {
            throw new IllegalArgumentException("Invalid length:" + i3);
        }
        try {
            this.mService.sendMhlVendorCommand(i, i2, i3, bArr);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to send vendor command: ", e);
        }
    }
}
