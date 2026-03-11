package com.android.settings.bluetooth;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.widget.ImageView;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.wifi.AccessPoint;

public final class BluetoothDevicePreference extends GearPreference implements CachedBluetoothDevice.Callback {
    private static int sDimAlpha = AccessPoint.UNREACHABLE_RSSI;
    private String contentDescription;
    private final CachedBluetoothDevice mCachedDevice;
    private AlertDialog mDisconnectDialog;
    private boolean mHideSecondTarget;
    Resources mResources;
    private final boolean mShowDevicesWithoutNames;
    private final UserManager mUserManager;

    public BluetoothDevicePreference(Context context, CachedBluetoothDevice cachedBluetoothDevice, boolean z) {
        super(context, null);
        this.contentDescription = null;
        this.mHideSecondTarget = false;
        this.mResources = getContext().getResources();
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mShowDevicesWithoutNames = z;
        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.disabledAlpha, typedValue, true);
            sDimAlpha = (int) (typedValue.getFloat() * 255.0f);
        }
        this.mCachedDevice = cachedBluetoothDevice;
        this.mCachedDevice.registerCallback(this);
        onDeviceAttributesChanged();
    }

    void rebind() {
        notifyChanged();
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return this.mCachedDevice == null || this.mCachedDevice.getBondState() != 12 || this.mUserManager.hasUserRestriction("no_config_bluetooth") || this.mHideSecondTarget;
    }

    @Override
    protected int getSecondTargetResId() {
        return com.android.settings.R.layout.preference_widget_gear;
    }

    CachedBluetoothDevice getCachedDevice() {
        return this.mCachedDevice;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        Log.d("BluetoothDevicePref", "onPrepareForRemoval");
        this.mCachedDevice.unregisterCallback(this);
        if (this.mDisconnectDialog != null) {
            Log.d("BluetoothDevicePref", "dismiss dialog");
            this.mDisconnectDialog.dismiss();
            this.mDisconnectDialog = null;
        }
    }

    public CachedBluetoothDevice getBluetoothDevice() {
        return this.mCachedDevice;
    }

    public void hideSecondTarget(boolean z) {
        this.mHideSecondTarget = z;
    }

    @Override
    public void onDeviceAttributesChanged() {
        setTitle(this.mCachedDevice.getName());
        setSummary(this.mCachedDevice.getConnectionSummary());
        Pair<Drawable, String> btClassDrawableWithDescription = com.android.settingslib.bluetooth.Utils.getBtClassDrawableWithDescription(getContext(), this.mCachedDevice);
        if (btClassDrawableWithDescription.first != null) {
            setIcon((Drawable) btClassDrawableWithDescription.first);
            this.contentDescription = (String) btClassDrawableWithDescription.second;
        }
        boolean z = true;
        setEnabled(!this.mCachedDevice.isBusy());
        if (!this.mShowDevicesWithoutNames && !this.mCachedDevice.hasHumanReadableName()) {
            z = false;
        }
        setVisible(z);
        notifyHierarchyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        ImageView imageView;
        if (findPreferenceInHierarchy("bt_checkbox") != null) {
            setDependency("bt_checkbox");
        }
        if (this.mCachedDevice.getBondState() == 12 && (imageView = (ImageView) preferenceViewHolder.findViewById(com.android.settings.R.id.settings_button)) != null) {
            imageView.setOnClickListener(this);
        }
        ImageView imageView2 = (ImageView) preferenceViewHolder.findViewById(R.id.icon);
        if (imageView2 != null) {
            imageView2.setContentDescription(this.contentDescription);
        }
        super.onBindViewHolder(preferenceViewHolder);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BluetoothDevicePreference)) {
            return false;
        }
        return this.mCachedDevice.equals(((BluetoothDevicePreference) obj).mCachedDevice);
    }

    public int hashCode() {
        return this.mCachedDevice.hashCode();
    }

    @Override
    public int compareTo(Preference preference) {
        if (!(preference instanceof BluetoothDevicePreference)) {
            return super.compareTo(preference);
        }
        return this.mCachedDevice.compareTo(((BluetoothDevicePreference) preference).mCachedDevice);
    }

    void onClicked() {
        Context context = getContext();
        int bondState = this.mCachedDevice.getBondState();
        MetricsFeatureProvider metricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        if (this.mCachedDevice.isConnected()) {
            metricsFeatureProvider.action(context, 868, new Pair[0]);
            Log.d("BluetoothDevicePref", this.mCachedDevice.getName() + " askDisconnect");
            askDisconnect();
            return;
        }
        if (bondState == 12) {
            metricsFeatureProvider.action(context, 867, new Pair[0]);
            Log.d("BluetoothDevicePref", this.mCachedDevice.getName() + " connect");
            this.mCachedDevice.connect(true);
            return;
        }
        if (bondState == 10) {
            metricsFeatureProvider.action(context, 866, new Pair[0]);
            if (!this.mCachedDevice.hasHumanReadableName()) {
                metricsFeatureProvider.action(context, 1096, new Pair[0]);
            }
            Log.d("BluetoothDevicePref", this.mCachedDevice.getName() + " pair");
            pair();
        }
    }

    private void askDisconnect() {
        Context context = getContext();
        String name = this.mCachedDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(com.android.settings.R.string.bluetooth_device);
        }
        String string = context.getString(com.android.settings.R.string.bluetooth_disconnect_all_profiles, name);
        this.mDisconnectDialog = Utils.showDisconnectDialog(context, this.mDisconnectDialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BluetoothDevicePreference.this.mCachedDevice.disconnect();
            }
        }, context.getString(com.android.settings.R.string.bluetooth_disconnect_title), Html.fromHtml(string));
    }

    private void pair() {
        if (!this.mCachedDevice.startPairing()) {
            Utils.showError(getContext(), this.mCachedDevice.getName(), com.android.settings.R.string.bluetooth_pairing_error_message);
        }
    }
}
