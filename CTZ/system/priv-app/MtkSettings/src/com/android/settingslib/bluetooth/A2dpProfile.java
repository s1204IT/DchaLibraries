package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.R;
import com.android.settingslib.wrapper.BluetoothA2dpWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class A2dpProfile implements LocalBluetoothProfile {
    private Context mContext;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;
    private BluetoothA2dp mService;
    private BluetoothA2dpWrapper mServiceWrapper;
    private static boolean V = false;
    static final ParcelUuid[] SINK_UUIDS = {BluetoothUuid.AudioSink, BluetoothUuid.AdvAudioDist};

    private final class A2dpServiceListener implements BluetoothProfile.ServiceListener {
        private A2dpServiceListener() {
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (A2dpProfile.V) {
                Log.d("A2dpProfile", "Bluetooth service connected");
            }
            A2dpProfile.this.mService = (BluetoothA2dp) bluetoothProfile;
            A2dpProfile.this.mServiceWrapper = new BluetoothA2dpWrapper(A2dpProfile.this.mService);
            List<BluetoothDevice> connectedDevices = A2dpProfile.this.mService.getConnectedDevices();
            while (!connectedDevices.isEmpty()) {
                BluetoothDevice bluetoothDeviceRemove = connectedDevices.remove(0);
                CachedBluetoothDevice cachedBluetoothDeviceFindDevice = A2dpProfile.this.mDeviceManager.findDevice(bluetoothDeviceRemove);
                if (cachedBluetoothDeviceFindDevice == null) {
                    Log.w("A2dpProfile", "A2dpProfile found new device: " + bluetoothDeviceRemove);
                    cachedBluetoothDeviceFindDevice = A2dpProfile.this.mDeviceManager.addDevice(A2dpProfile.this.mLocalAdapter, A2dpProfile.this.mProfileManager, bluetoothDeviceRemove);
                }
                cachedBluetoothDeviceFindDevice.onProfileStateChanged(A2dpProfile.this, 2);
                cachedBluetoothDeviceFindDevice.refresh();
            }
            A2dpProfile.this.mIsProfileReady = true;
        }

        @Override // android.bluetooth.BluetoothProfile.ServiceListener
        public void onServiceDisconnected(int i) {
            if (A2dpProfile.V) {
                Log.d("A2dpProfile", "Bluetooth service disconnected");
            }
            A2dpProfile.this.mIsProfileReady = false;
        }
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getProfileId() {
        return 2;
    }

    A2dpProfile(Context context, LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mContext = context;
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mProfileManager = localBluetoothProfileManager;
        this.mLocalAdapter.getProfileProxy(context, new A2dpServiceListener(), 2);
    }

    @VisibleForTesting
    void setBluetoothA2dpWrapper(BluetoothA2dpWrapper bluetoothA2dpWrapper) {
        this.mServiceWrapper = bluetoothA2dpWrapper;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isConnectable() {
        return true;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isAutoConnectable() {
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        return this.mService == null ? new ArrayList(0) : this.mService.getDevicesMatchingConnectionStates(new int[]{2, 1, 3});
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean connect(BluetoothDevice bluetoothDevice) {
        List<BluetoothDevice> connectedDevices;
        if (this.mService == null) {
            return false;
        }
        if (this.mLocalAdapter.getMaxConnectedAudioDevices() == 1 && (connectedDevices = getConnectedDevices()) != null) {
            for (BluetoothDevice bluetoothDevice2 : connectedDevices) {
                if (bluetoothDevice2.equals(bluetoothDevice)) {
                    Log.w("A2dpProfile", "Connecting to device " + bluetoothDevice + " : disconnect skipped");
                } else {
                    this.mService.disconnect(bluetoothDevice2);
                }
            }
        }
        return this.mService.connect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        if (this.mService.getPriority(bluetoothDevice) > 100) {
            this.mService.setPriority(bluetoothDevice, 100);
        }
        return this.mService.disconnect(bluetoothDevice);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getConnectionStatus(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return 0;
        }
        return this.mService.getConnectionState(bluetoothDevice);
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        if (this.mService == null) {
            return false;
        }
        return this.mService.setActiveDevice(bluetoothDevice);
    }

    public BluetoothDevice getActiveDevice() {
        if (this.mService == null) {
            return null;
        }
        return this.mService.getActiveDevice();
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public boolean isPreferred(BluetoothDevice bluetoothDevice) {
        return this.mService != null && this.mService.getPriority(bluetoothDevice) > 0;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public void setPreferred(BluetoothDevice bluetoothDevice, boolean z) {
        if (this.mService == null) {
            return;
        }
        if (z) {
            if (this.mService.getPriority(bluetoothDevice) < 100) {
                this.mService.setPriority(bluetoothDevice, 100);
                return;
            }
            return;
        }
        this.mService.setPriority(bluetoothDevice, 0);
    }

    boolean isA2dpPlaying() {
        if (this.mService == null) {
            return false;
        }
        Iterator<BluetoothDevice> it = this.mService.getConnectedDevices().iterator();
        while (it.hasNext()) {
            if (this.mService.isA2dpPlaying(it.next())) {
                return true;
            }
        }
        return false;
    }

    public boolean supportsHighQualityAudio(BluetoothDevice bluetoothDevice) {
        return this.mServiceWrapper.supportsOptionalCodecs(bluetoothDevice) == 1;
    }

    public boolean isHighQualityAudioEnabled(BluetoothDevice bluetoothDevice) {
        int optionalCodecsEnabled = this.mServiceWrapper.getOptionalCodecsEnabled(bluetoothDevice);
        if (optionalCodecsEnabled != -1) {
            return optionalCodecsEnabled == 1;
        }
        if (getConnectionStatus(bluetoothDevice) != 2 && supportsHighQualityAudio(bluetoothDevice)) {
            return true;
        }
        BluetoothCodecConfig codecConfig = null;
        if (this.mServiceWrapper.getCodecStatus(bluetoothDevice) != null) {
            codecConfig = this.mServiceWrapper.getCodecStatus(bluetoothDevice).getCodecConfig();
        }
        if (codecConfig == null) {
            return false;
        }
        return !codecConfig.isMandatoryCodec();
    }

    public void setHighQualityAudioEnabled(BluetoothDevice bluetoothDevice, boolean z) {
        int i;
        if (z) {
            i = 1;
        } else {
            i = 0;
        }
        this.mServiceWrapper.setOptionalCodecsEnabled(bluetoothDevice, i);
        if (getConnectionStatus(bluetoothDevice) != 2) {
            return;
        }
        if (z) {
            this.mService.enableOptionalCodecs(bluetoothDevice);
        } else {
            this.mService.disableOptionalCodecs(bluetoothDevice);
        }
    }

    public String getHighQualityAudioOptionLabel(BluetoothDevice bluetoothDevice) {
        BluetoothCodecConfig[] codecsSelectableCapabilities;
        int i = R.string.bluetooth_profile_a2dp_high_quality_unknown_codec;
        if (supportsHighQualityAudio(bluetoothDevice)) {
            char c = 2;
            if (getConnectionStatus(bluetoothDevice) == 2) {
                BluetoothCodecConfig bluetoothCodecConfig = null;
                if (this.mServiceWrapper.getCodecStatus(bluetoothDevice) != null) {
                    codecsSelectableCapabilities = this.mServiceWrapper.getCodecStatus(bluetoothDevice).getCodecsSelectableCapabilities();
                    Arrays.sort(codecsSelectableCapabilities, new Comparator() { // from class: com.android.settingslib.bluetooth.-$$Lambda$A2dpProfile$exPXCssgW4cryyr_RqCY5K-rQFI
                        @Override // java.util.Comparator
                        public final int compare(Object obj, Object obj2) {
                            return A2dpProfile.lambda$getHighQualityAudioOptionLabel$0((BluetoothCodecConfig) obj, (BluetoothCodecConfig) obj2);
                        }
                    });
                } else {
                    codecsSelectableCapabilities = null;
                }
                if (codecsSelectableCapabilities != null && codecsSelectableCapabilities.length >= 1) {
                    bluetoothCodecConfig = codecsSelectableCapabilities[0];
                }
                switch ((bluetoothCodecConfig == null || bluetoothCodecConfig.isMandatoryCodec()) ? 1000000 : bluetoothCodecConfig.getCodecType()) {
                    case 0:
                        c = 1;
                        break;
                    case 1:
                        break;
                    case 2:
                        c = 3;
                        break;
                    case 3:
                        c = 4;
                        break;
                    case 4:
                        c = 5;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                if (c < 0) {
                    return this.mContext.getString(i);
                }
                return this.mContext.getString(R.string.bluetooth_profile_a2dp_high_quality, this.mContext.getResources().getStringArray(R.array.bluetooth_a2dp_codec_titles)[c]);
            }
        }
        return this.mContext.getString(i);
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: 0x0008: ARITH (wrap:int:0x0000: INVOKE (r1v0 android.bluetooth.BluetoothCodecConfig) VIRTUAL call: android.bluetooth.BluetoothCodecConfig.getCodecPriority():int A[MD:():int (c), WRAPPED] (LINE:266)) - (wrap:int:0x0004: INVOKE (r0v0 android.bluetooth.BluetoothCodecConfig) VIRTUAL call: android.bluetooth.BluetoothCodecConfig.getCodecPriority():int A[MD:():int (c), WRAPPED]) (LINE:266) */
    static /* synthetic */ int lambda$getHighQualityAudioOptionLabel$0(BluetoothCodecConfig bluetoothCodecConfig, BluetoothCodecConfig bluetoothCodecConfig2) {
        return bluetoothCodecConfig2.getCodecPriority() - bluetoothCodecConfig.getCodecPriority();
    }

    public String toString() {
        return "A2DP";
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getNameResource(BluetoothDevice bluetoothDevice) {
        return R.string.bluetooth_profile_a2dp;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfile
    public int getDrawableResource(BluetoothClass bluetoothClass) {
        return R.drawable.ic_bt_headphones_a2dp;
    }

    protected void finalize() {
        if (V) {
            Log.d("A2dpProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(2, this.mService);
                this.mService = null;
            } catch (Throwable th) {
                Log.w("A2dpProfile", "Error cleaning up A2DP proxy", th);
            }
        }
    }
}
