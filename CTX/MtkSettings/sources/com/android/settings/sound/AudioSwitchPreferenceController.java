package com.android.settings.sound;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public abstract class AudioSwitchPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener, BluetoothCallback, LifecycleObserver, OnStart, OnStop {
    private static final int INVALID_INDEX = -1;
    private static final String TAG = "AudioSwitchPreferenceController";
    protected final AudioManager mAudioManager;
    private final AudioManagerAudioDeviceCallback mAudioManagerAudioDeviceCallback;
    protected AudioSwitchCallback mAudioSwitchPreferenceCallback;
    protected final List<BluetoothDevice> mConnectedDevices;
    private final Handler mHandler;
    private LocalBluetoothManager mLocalBluetoothManager;
    protected final MediaRouter mMediaRouter;
    private final MediaRouterCallback mMediaRouterCallback;
    protected Preference mPreference;
    protected LocalBluetoothProfileManager mProfileManager;
    private final WiredHeadsetBroadcastReceiver mReceiver;
    protected int mSelectedIndex;

    public interface AudioSwitchCallback {
        void onPreferenceDataChanged(ListPreference listPreference);
    }

    public abstract void setActiveBluetoothDevice(BluetoothDevice bluetoothDevice);

    public AudioSwitchPreferenceController(Context context, String str) {
        super(context, str);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mMediaRouter = (MediaRouter) context.getSystemService("media_router");
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mAudioManagerAudioDeviceCallback = new AudioManagerAudioDeviceCallback();
        this.mReceiver = new WiredHeadsetBroadcastReceiver();
        this.mMediaRouterCallback = new MediaRouterCallback();
        this.mConnectedDevices = new ArrayList();
        FutureTask futureTask = new FutureTask(new Callable() {
            @Override
            public final Object call() {
                return Utils.getLocalBtManager(this.f$0.mContext);
            }
        });
        try {
            futureTask.run();
            this.mLocalBluetoothManager = (LocalBluetoothManager) futureTask.get();
            if (this.mLocalBluetoothManager == null) {
                Log.e(TAG, "Bluetooth is not supported on this device");
            } else {
                this.mProfileManager = this.mLocalBluetoothManager.getProfileManager();
            }
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting LocalBluetoothManager.", e);
        }
    }

    @Override
    public final int getAvailabilityStatus() {
        return (FeatureFlagUtils.isEnabled(this.mContext, "settings_audio_switcher") && this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) ? 0 : 1;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String str = (String) obj;
        if (!(preference instanceof ListPreference)) {
            return false;
        }
        ListPreference listPreference = (ListPreference) preference;
        if (TextUtils.equals(str, this.mContext.getText(R.string.media_output_default_summary))) {
            this.mSelectedIndex = getDefaultDeviceIndex();
            setActiveBluetoothDevice(null);
            listPreference.setSummary(this.mContext.getText(R.string.media_output_default_summary));
            return true;
        }
        int connectedDeviceIndex = getConnectedDeviceIndex(str);
        if (connectedDeviceIndex == INVALID_INDEX) {
            return false;
        }
        BluetoothDevice bluetoothDevice = this.mConnectedDevices.get(connectedDeviceIndex);
        this.mSelectedIndex = connectedDeviceIndex;
        setActiveBluetoothDevice(bluetoothDevice);
        listPreference.setSummary(bluetoothDevice.getAliasName());
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(this.mPreferenceKey);
        this.mPreference.setVisible(false);
    }

    @Override
    public void onStart() {
        if (this.mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
        } else {
            this.mLocalBluetoothManager.setForegroundActivity(this.mContext);
            register();
        }
    }

    @Override
    public void onStop() {
        if (this.mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
        } else {
            this.mLocalBluetoothManager.setForegroundActivity(null);
            unregister();
        }
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        updateState(this.mPreference);
    }

    @Override
    public void onAudioModeChanged() {
        updateState(this.mPreference);
    }

    @Override
    public void onProfileConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i, int i2) {
        updateState(this.mPreference);
    }

    @Override
    public void onBluetoothStateChanged(int i) {
    }

    @Override
    public void onScanningStateChanged(boolean z) {
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
        updateState(this.mPreference);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    public void setCallback(AudioSwitchCallback audioSwitchCallback) {
        this.mAudioSwitchPreferenceCallback = audioSwitchCallback;
    }

    protected boolean isStreamFromOutputDevice(int i, int i2) {
        return (this.mAudioManager.getDevicesForStream(i) & i2) != 0;
    }

    protected List<BluetoothDevice> getConnectedHfpDevices() {
        ArrayList arrayList = new ArrayList();
        HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
        if (headsetProfile == null) {
            return arrayList;
        }
        for (BluetoothDevice bluetoothDevice : headsetProfile.getConnectedDevices()) {
            if (bluetoothDevice.isConnected()) {
                arrayList.add(bluetoothDevice);
            }
        }
        return arrayList;
    }

    protected List<BluetoothDevice> getConnectedA2dpDevices() {
        ArrayList arrayList = new ArrayList();
        A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
        if (a2dpProfile == null) {
            return arrayList;
        }
        for (BluetoothDevice bluetoothDevice : a2dpProfile.getConnectedDevices()) {
            if (bluetoothDevice.isConnected()) {
                arrayList.add(bluetoothDevice);
            }
        }
        return arrayList;
    }

    protected List<BluetoothDevice> getConnectedHearingAidDevices() {
        ArrayList arrayList = new ArrayList();
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        if (hearingAidProfile == null) {
            return arrayList;
        }
        ArrayList arrayList2 = new ArrayList();
        for (BluetoothDevice bluetoothDevice : hearingAidProfile.getConnectedDevices()) {
            long hiSyncId = hearingAidProfile.getHiSyncId(bluetoothDevice);
            if (!arrayList2.contains(Long.valueOf(hiSyncId)) && bluetoothDevice.isConnected()) {
                arrayList2.add(Long.valueOf(hiSyncId));
                arrayList.add(bluetoothDevice);
            }
        }
        return arrayList;
    }

    protected BluetoothDevice findActiveDevice(int i) {
        if (i != 3 && i != 0) {
            return null;
        }
        if (isStreamFromOutputDevice(3, 896)) {
            return this.mProfileManager.getA2dpProfile().getActiveDevice();
        }
        if (isStreamFromOutputDevice(0, 112)) {
            return this.mProfileManager.getHeadsetProfile().getActiveDevice();
        }
        if (isStreamFromOutputDevice(i, 134217728)) {
            for (BluetoothDevice bluetoothDevice : this.mProfileManager.getHearingAidProfile().getActiveDevices()) {
                if (bluetoothDevice != null && this.mConnectedDevices.contains(bluetoothDevice)) {
                    return bluetoothDevice;
                }
            }
        }
        return null;
    }

    int getDefaultDeviceIndex() {
        return this.mConnectedDevices.size();
    }

    void setupPreferenceEntries(CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2, BluetoothDevice bluetoothDevice) {
        this.mSelectedIndex = getDefaultDeviceIndex();
        charSequenceArr[this.mSelectedIndex] = this.mContext.getText(R.string.media_output_default_summary);
        charSequenceArr2[this.mSelectedIndex] = this.mContext.getText(R.string.media_output_default_summary);
        int size = this.mConnectedDevices.size();
        for (int i = 0; i < size; i++) {
            BluetoothDevice bluetoothDevice2 = this.mConnectedDevices.get(i);
            charSequenceArr[i] = bluetoothDevice2.getAliasName();
            charSequenceArr2[i] = bluetoothDevice2.getAddress();
            if (bluetoothDevice2.equals(bluetoothDevice)) {
                this.mSelectedIndex = i;
            }
        }
    }

    void setPreference(CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2, Preference preference) {
        ListPreference listPreference = (ListPreference) preference;
        listPreference.setEntries(charSequenceArr);
        listPreference.setEntryValues(charSequenceArr2);
        listPreference.setValueIndex(this.mSelectedIndex);
        listPreference.setSummary(charSequenceArr[this.mSelectedIndex]);
        this.mAudioSwitchPreferenceCallback.onPreferenceDataChanged(listPreference);
    }

    private int getConnectedDeviceIndex(String str) {
        if (this.mConnectedDevices != null) {
            int size = this.mConnectedDevices.size();
            for (int i = 0; i < size; i++) {
                if (TextUtils.equals(this.mConnectedDevices.get(i).getAddress(), str)) {
                    return i;
                }
            }
            return INVALID_INDEX;
        }
        return INVALID_INDEX;
    }

    private void register() {
        this.mLocalBluetoothManager.getEventManager().registerCallback(this);
        this.mAudioManager.registerAudioDeviceCallback(this.mAudioManagerAudioDeviceCallback, this.mHandler);
        this.mMediaRouter.addCallback(4, this.mMediaRouterCallback);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    private void unregister() {
        this.mLocalBluetoothManager.getEventManager().unregisterCallback(this);
        this.mAudioManager.unregisterAudioDeviceCallback(this.mAudioManagerAudioDeviceCallback);
        this.mMediaRouter.removeCallback(this.mMediaRouterCallback);
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    private class AudioManagerAudioDeviceCallback extends AudioDeviceCallback {
        private AudioManagerAudioDeviceCallback() {
        }

        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] audioDeviceInfoArr) {
            AudioSwitchPreferenceController.this.updateState(AudioSwitchPreferenceController.this.mPreference);
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] audioDeviceInfoArr) {
            AudioSwitchPreferenceController.this.updateState(AudioSwitchPreferenceController.this.mPreference);
        }
    }

    private class WiredHeadsetBroadcastReceiver extends BroadcastReceiver {
        private WiredHeadsetBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.HEADSET_PLUG".equals(action) || "android.media.STREAM_DEVICES_CHANGED_ACTION".equals(action)) {
                AudioSwitchPreferenceController.this.updateState(AudioSwitchPreferenceController.this.mPreference);
            }
        }
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        private MediaRouterCallback() {
        }

        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
        }

        @Override
        public void onRouteUnselected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
        }

        @Override
        public void onRouteAdded(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            if (routeInfo != null && !routeInfo.isDefault()) {
                AudioSwitchPreferenceController.this.updateState(AudioSwitchPreferenceController.this.mPreference);
            }
        }

        @Override
        public void onRouteRemoved(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
        }

        @Override
        public void onRouteChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            if (routeInfo != null && !routeInfo.isDefault()) {
                AudioSwitchPreferenceController.this.updateState(AudioSwitchPreferenceController.this.mPreference);
            }
        }

        @Override
        public void onRouteGrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup, int i) {
        }

        @Override
        public void onRouteUngrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup) {
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
        }
    }
}
