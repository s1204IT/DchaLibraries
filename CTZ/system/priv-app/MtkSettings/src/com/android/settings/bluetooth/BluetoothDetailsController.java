package com.android.settings.bluetooth;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public abstract class BluetoothDetailsController extends AbstractPreferenceController implements PreferenceControllerMixin, CachedBluetoothDevice.Callback, LifecycleObserver, OnPause, OnResume {
    protected final CachedBluetoothDevice mCachedDevice;
    protected final Context mContext;
    protected final PreferenceFragment mFragment;

    protected abstract void init(PreferenceScreen preferenceScreen);

    protected abstract void refresh();

    public BluetoothDetailsController(Context context, PreferenceFragment preferenceFragment, CachedBluetoothDevice cachedBluetoothDevice, Lifecycle lifecycle) {
        super(context);
        this.mContext = context;
        this.mFragment = preferenceFragment;
        this.mCachedDevice = cachedBluetoothDevice;
        lifecycle.addObserver(this);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        this.mCachedDevice.unregisterCallback(this);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        this.mCachedDevice.registerCallback(this);
        refresh();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.bluetooth.CachedBluetoothDevice.Callback
    public void onDeviceAttributesChanged() {
        refresh();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public final void displayPreference(PreferenceScreen preferenceScreen) {
        init(preferenceScreen);
        super.displayPreference(preferenceScreen);
    }
}
