package com.android.settings.development;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public abstract class AbstractBluetoothA2dpPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, BluetoothServiceConnectionListener, LifecycleObserver, OnDestroy {
    static final int STREAMING_LABEL_ID = 2131886889;
    protected BluetoothA2dp mBluetoothA2dp;
    protected final BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private final String[] mListSummaries;
    private final String[] mListValues;
    protected ListPreference mPreference;

    protected abstract int getCurrentA2dpSettingIndex(BluetoothCodecConfig bluetoothCodecConfig);

    protected abstract int getDefaultIndex();

    protected abstract String[] getListSummaries();

    protected abstract String[] getListValues();

    protected abstract void writeConfigurationValues(Object obj);

    public AbstractBluetoothA2dpPreferenceController(Context context, Lifecycle lifecycle, BluetoothA2dpConfigStore bluetoothA2dpConfigStore) {
        super(context);
        this.mBluetoothA2dpConfigStore = bluetoothA2dpConfigStore;
        this.mListValues = getListValues();
        this.mListSummaries = getListSummaries();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (ListPreference) preferenceScreen.findPreference(getPreferenceKey());
        this.mPreference.setValue(this.mListValues[getDefaultIndex()]);
        this.mPreference.setSummary(this.mListSummaries[getDefaultIndex()]);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mBluetoothA2dp == null) {
            return false;
        }
        writeConfigurationValues(obj);
        BluetoothCodecConfig bluetoothCodecConfigCreateCodecConfig = this.mBluetoothA2dpConfigStore.createCodecConfig();
        synchronized (this.mBluetoothA2dpConfigStore) {
            if (this.mBluetoothA2dp != null) {
                setCodecConfigPreference(null, bluetoothCodecConfigCreateCodecConfig);
            }
        }
        int iFindIndexOfValue = this.mPreference.findIndexOfValue(obj.toString());
        if (iFindIndexOfValue == getDefaultIndex()) {
            this.mPreference.setSummary(this.mListSummaries[iFindIndexOfValue]);
        } else {
            this.mPreference.setSummary(this.mContext.getResources().getString(R.string.bluetooth_select_a2dp_codec_streaming_label, this.mListSummaries[iFindIndexOfValue]));
        }
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        BluetoothCodecConfig codecConfig;
        if (getCodecConfig(null) == null || this.mPreference == null) {
            return;
        }
        synchronized (this.mBluetoothA2dpConfigStore) {
            codecConfig = getCodecConfig(null);
        }
        int currentA2dpSettingIndex = getCurrentA2dpSettingIndex(codecConfig);
        this.mPreference.setValue(this.mListValues[currentA2dpSettingIndex]);
        if (currentA2dpSettingIndex == getDefaultIndex()) {
            this.mPreference.setSummary(this.mListSummaries[currentA2dpSettingIndex]);
        } else {
            this.mPreference.setSummary(this.mContext.getResources().getString(R.string.bluetooth_select_a2dp_codec_streaming_label, this.mListSummaries[currentA2dpSettingIndex]));
        }
        writeConfigurationValues(this.mListValues[currentA2dpSettingIndex]);
    }

    @Override // com.android.settings.development.BluetoothServiceConnectionListener
    public void onBluetoothServiceConnected(BluetoothA2dp bluetoothA2dp) {
        this.mBluetoothA2dp = bluetoothA2dp;
        updateState(this.mPreference);
    }

    @Override // com.android.settings.development.BluetoothServiceConnectionListener
    public void onBluetoothCodecUpdated() {
    }

    @Override // com.android.settings.development.BluetoothServiceConnectionListener
    public void onBluetoothServiceDisconnected() {
        this.mBluetoothA2dp = null;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnDestroy
    public void onDestroy() {
        this.mBluetoothA2dp = null;
    }

    void setCodecConfigPreference(BluetoothDevice bluetoothDevice, BluetoothCodecConfig bluetoothCodecConfig) {
        this.mBluetoothA2dp.setCodecConfigPreference(bluetoothDevice, bluetoothCodecConfig);
    }

    BluetoothCodecConfig getCodecConfig(BluetoothDevice bluetoothDevice) {
        BluetoothCodecStatus codecStatus;
        if (this.mBluetoothA2dp != null && (codecStatus = this.mBluetoothA2dp.getCodecStatus(bluetoothDevice)) != null) {
            return codecStatus.getCodecConfig();
        }
        return null;
    }
}
