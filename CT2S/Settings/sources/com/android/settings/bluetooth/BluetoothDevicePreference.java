package com.android.settings.bluetooth;

import android.R;
import android.app.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.DialogInterface;
import android.os.UserManager;
import android.preference.Preference;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.bluetooth.CachedBluetoothDevice;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;
import java.util.List;

public final class BluetoothDevicePreference extends Preference implements View.OnClickListener, CachedBluetoothDevice.Callback {
    private static int sDimAlpha = Integer.MIN_VALUE;
    private final CachedBluetoothDevice mCachedDevice;
    private AlertDialog mDisconnectDialog;
    private View.OnClickListener mOnSettingsClickListener;

    public BluetoothDevicePreference(Context context, CachedBluetoothDevice cachedDevice) {
        super(context);
        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255.0f);
        }
        this.mCachedDevice = cachedDevice;
        setLayoutResource(com.android.settings.R.layout.preference_bt_icon);
        if (cachedDevice.getBondState() == 12) {
            UserManager um = (UserManager) context.getSystemService("user");
            if (!um.hasUserRestriction("no_config_bluetooth")) {
                setWidgetLayoutResource(com.android.settings.R.layout.preference_bluetooth);
            }
        }
        this.mCachedDevice.registerCallback(this);
        onDeviceAttributesChanged();
    }

    CachedBluetoothDevice getCachedDevice() {
        return this.mCachedDevice;
    }

    public void setOnSettingsClickListener(View.OnClickListener listener) {
        this.mOnSettingsClickListener = listener;
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        this.mCachedDevice.unregisterCallback(this);
        if (this.mDisconnectDialog != null) {
            this.mDisconnectDialog.dismiss();
            this.mDisconnectDialog = null;
        }
    }

    @Override
    public void onDeviceAttributesChanged() {
        setTitle(this.mCachedDevice.getName());
        int summaryResId = getConnectionSummary();
        if (summaryResId != 0) {
            setSummary(summaryResId);
        } else {
            setSummary((CharSequence) null);
        }
        int iconResId = getBtClassDrawable();
        if (iconResId != 0) {
            setIcon(iconResId);
        }
        setEnabled(!this.mCachedDevice.isBusy());
        notifyHierarchyChanged();
    }

    @Override
    protected void onBindView(View view) {
        ImageView deviceDetails;
        if (findPreferenceInHierarchy("bt_checkbox") != null) {
            setDependency("bt_checkbox");
        }
        if (this.mCachedDevice.getBondState() == 12 && (deviceDetails = (ImageView) view.findViewById(com.android.settings.R.id.deviceDetails)) != null) {
            deviceDetails.setOnClickListener(this);
            deviceDetails.setTag(this.mCachedDevice);
        }
        super.onBindView(view);
    }

    @Override
    public void onClick(View v) {
        if (this.mOnSettingsClickListener != null) {
            this.mOnSettingsClickListener.onClick(v);
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof BluetoothDevicePreference)) {
            return false;
        }
        return this.mCachedDevice.equals(((BluetoothDevicePreference) o).mCachedDevice);
    }

    public int hashCode() {
        return this.mCachedDevice.hashCode();
    }

    @Override
    public int compareTo(Preference another) {
        return !(another instanceof BluetoothDevicePreference) ? super.compareTo(another) : this.mCachedDevice.compareTo(((BluetoothDevicePreference) another).mCachedDevice);
    }

    void onClicked() {
        int bondState = this.mCachedDevice.getBondState();
        if (this.mCachedDevice.isConnected()) {
            askDisconnect();
        } else if (bondState == 12) {
            this.mCachedDevice.connect(true);
        } else if (bondState == 10) {
            pair();
        }
    }

    private void askDisconnect() {
        Context context = getContext();
        String name = this.mCachedDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(com.android.settings.R.string.bluetooth_device);
        }
        String message = context.getString(com.android.settings.R.string.bluetooth_disconnect_all_profiles, name);
        String title = context.getString(com.android.settings.R.string.bluetooth_disconnect_title);
        DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothDevicePreference.this.mCachedDevice.disconnect();
            }
        };
        this.mDisconnectDialog = Utils.showDisconnectDialog(context, this.mDisconnectDialog, disconnectListener, title, Html.fromHtml(message));
    }

    private void pair() {
        if (!this.mCachedDevice.startPairing()) {
            Utils.showError(getContext(), this.mCachedDevice.getName(), com.android.settings.R.string.bluetooth_pairing_error_message);
            return;
        }
        Context context = getContext();
        SearchIndexableRaw data = new SearchIndexableRaw(context);
        data.className = BluetoothSettings.class.getName();
        data.title = this.mCachedDevice.getName();
        data.screenTitle = context.getResources().getString(com.android.settings.R.string.bluetooth_settings);
        data.iconResId = com.android.settings.R.drawable.ic_settings_bluetooth2;
        data.enabled = true;
        Index.getInstance(context).updateFromSearchIndexableData(data);
    }

    private int getConnectionSummary() {
        CachedBluetoothDevice cachedDevice = this.mCachedDevice;
        boolean profileConnected = false;
        boolean a2dpNotConnected = false;
        boolean headsetNotConnected = false;
        for (LocalBluetoothProfile profile : cachedDevice.getProfiles()) {
            int connectionStatus = cachedDevice.getProfileConnectionState(profile);
            switch (connectionStatus) {
                case 0:
                    if (profile.isProfileReady()) {
                        if (profile instanceof A2dpProfile) {
                            a2dpNotConnected = true;
                        } else if (profile instanceof HeadsetProfile) {
                            headsetNotConnected = true;
                        }
                    }
                    break;
                case 1:
                case 3:
                    return Utils.getConnectionStateSummary(connectionStatus);
                case 2:
                    profileConnected = true;
                    break;
            }
        }
        if (profileConnected) {
            if (a2dpNotConnected && headsetNotConnected) {
                return com.android.settings.R.string.bluetooth_connected_no_headset_no_a2dp;
            }
            if (a2dpNotConnected) {
                return com.android.settings.R.string.bluetooth_connected_no_a2dp;
            }
            if (headsetNotConnected) {
                return com.android.settings.R.string.bluetooth_connected_no_headset;
            }
            return com.android.settings.R.string.bluetooth_connected;
        }
        switch (cachedDevice.getBondState()) {
            case 11:
                return com.android.settings.R.string.bluetooth_pairing;
            default:
                return 0;
        }
    }

    private int getBtClassDrawable() {
        BluetoothClass btClass = this.mCachedDevice.getBtClass();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case 256:
                    return com.android.settings.R.drawable.ic_bt_laptop;
                case 512:
                    return com.android.settings.R.drawable.ic_bt_cellphone;
                case 1280:
                    return HidProfile.getHidClassDrawable(btClass);
                case 1536:
                    return com.android.settings.R.drawable.ic_bt_imaging;
            }
        }
        Log.w("BluetoothDevicePreference", "mBtClass is null");
        List<LocalBluetoothProfile> profiles = this.mCachedDevice.getProfiles();
        for (LocalBluetoothProfile profile : profiles) {
            int resId = profile.getDrawableResource(btClass);
            if (resId != 0) {
                return resId;
            }
        }
        if (btClass != null) {
            if (btClass.doesClassMatch(1)) {
                return com.android.settings.R.drawable.ic_bt_headphones_a2dp;
            }
            if (btClass.doesClassMatch(0)) {
                return com.android.settings.R.drawable.ic_bt_headset_hfp;
            }
        }
        return com.android.settings.R.drawable.ic_settings_bluetooth2;
    }
}
