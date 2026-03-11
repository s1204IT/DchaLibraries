package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settings.search.Index;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import java.util.List;

public final class BluetoothDevicePreference extends Preference implements CachedBluetoothDevice.Callback, View.OnClickListener {
    private static int sDimAlpha = Integer.MIN_VALUE;
    public final String BLUETOOTH;
    public final String COMPUTER;
    public final String HEADPHONE;
    public final String HEADSET;
    public final String IMAGING;
    public final String INPUT_PERIPHERAL;
    public final String PHONE;
    private String contentDescription;
    private final CachedBluetoothDevice mCachedDevice;
    private AlertDialog mDisconnectDialog;
    private View.OnClickListener mOnSettingsClickListener;
    Resources r;

    public BluetoothDevicePreference(Context context, CachedBluetoothDevice cachedDevice) {
        super(context);
        this.contentDescription = null;
        this.r = getContext().getResources();
        this.COMPUTER = this.r.getString(R.string.bluetooth_talkback_computer);
        this.INPUT_PERIPHERAL = this.r.getString(R.string.bluetooth_talkback_input_peripheral);
        this.HEADSET = this.r.getString(R.string.bluetooth_talkback_headset);
        this.PHONE = this.r.getString(R.string.bluetooth_talkback_phone);
        this.IMAGING = this.r.getString(R.string.bluetooth_talkback_imaging);
        this.HEADPHONE = this.r.getString(R.string.bluetooth_talkback_headphone);
        this.BLUETOOTH = this.r.getString(R.string.bluetooth_talkback_bluetooth);
        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255.0f);
        }
        this.mCachedDevice = cachedDevice;
        setLayoutResource(R.layout.preference_bt_icon);
        if (cachedDevice.getBondState() == 12) {
            UserManager um = (UserManager) context.getSystemService("user");
            if (!um.hasUserRestriction("no_config_bluetooth")) {
                setWidgetLayoutResource(R.layout.preference_bluetooth);
            }
        }
        this.mCachedDevice.registerCallback(this);
        onDeviceAttributesChanged();
    }

    void rebind() {
        notifyChanged();
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
        Log.d("BluetoothDevicePreference", "onPrepareForRemoval");
        this.mCachedDevice.unregisterCallback(this);
        if (this.mDisconnectDialog == null) {
            return;
        }
        Log.d("BluetoothDevicePreference", "dismiss dialog");
        this.mDisconnectDialog.dismiss();
        this.mDisconnectDialog = null;
    }

    @Override
    public void onDeviceAttributesChanged() {
        setTitle(this.mCachedDevice.getName());
        int summaryResId = this.mCachedDevice.getConnectionSummary();
        if (summaryResId != 0) {
            setSummary(summaryResId);
        } else {
            setSummary((CharSequence) null);
        }
        Pair<Integer, String> pair = getBtClassDrawableWithDescription();
        if (((Integer) pair.first).intValue() != 0) {
            setIcon(((Integer) pair.first).intValue());
            this.contentDescription = (String) pair.second;
        }
        setEnabled(!this.mCachedDevice.isBusy());
        notifyHierarchyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        ImageView deviceDetails;
        if (findPreferenceInHierarchy("bt_checkbox") != null) {
            setDependency("bt_checkbox");
        }
        if (this.mCachedDevice.getBondState() == 12 && (deviceDetails = (ImageView) view.findViewById(R.id.deviceDetails)) != null) {
            deviceDetails.setOnClickListener(this);
            deviceDetails.setTag(this.mCachedDevice);
        }
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        if (imageView != null) {
            imageView.setContentDescription(this.contentDescription);
        }
        super.onBindViewHolder(view);
    }

    @Override
    public void onClick(View v) {
        if (this.mOnSettingsClickListener == null) {
            return;
        }
        this.mOnSettingsClickListener.onClick(v);
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
        if (!(another instanceof BluetoothDevicePreference)) {
            return super.compareTo(another);
        }
        return this.mCachedDevice.compareTo(((BluetoothDevicePreference) another).mCachedDevice);
    }

    void onClicked() {
        int bondState = this.mCachedDevice.getBondState();
        if (this.mCachedDevice.isConnected()) {
            askDisconnect();
            return;
        }
        if (bondState == 12) {
            Log.d("BluetoothDevicePreference", this.mCachedDevice.getName() + " connect");
            this.mCachedDevice.connect(true);
        } else {
            if (bondState != 10) {
                return;
            }
            pair();
        }
    }

    private void askDisconnect() {
        Context context = getContext();
        String name = this.mCachedDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String message = context.getString(R.string.bluetooth_disconnect_all_profiles, name);
        String title = context.getString(R.string.bluetooth_disconnect_title);
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
            Utils.showError(getContext(), this.mCachedDevice.getName(), R.string.bluetooth_pairing_error_message);
            return;
        }
        Context context = getContext();
        SearchIndexableRaw data = new SearchIndexableRaw(context);
        data.className = BluetoothSettings.class.getName();
        data.title = this.mCachedDevice.getName();
        data.screenTitle = context.getResources().getString(R.string.bluetooth_settings);
        data.iconResId = R.drawable.ic_settings_bluetooth;
        data.enabled = true;
        Index.getInstance(context).updateFromSearchIndexableData(data);
    }

    private Pair<Integer, String> getBtClassDrawableWithDescription() {
        BluetoothClass btClass = this.mCachedDevice.getBtClass();
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case 256:
                    return new Pair<>(Integer.valueOf(R.drawable.ic_bt_laptop), this.COMPUTER);
                case 512:
                    return new Pair<>(Integer.valueOf(R.drawable.ic_bt_cellphone), this.PHONE);
                case 1280:
                    return new Pair<>(Integer.valueOf(HidProfile.getHidClassDrawable(btClass)), this.INPUT_PERIPHERAL);
                case 1536:
                    return new Pair<>(Integer.valueOf(R.drawable.ic_bt_imaging), this.IMAGING);
                default:
                    Log.d("BluetoothDevicePreference", "unrecognized device class " + btClass);
                    break;
            }
        } else {
            Log.w("BluetoothDevicePreference", "mBtClass is null");
        }
        List<LocalBluetoothProfile> profiles = this.mCachedDevice.getProfiles();
        for (LocalBluetoothProfile profile : profiles) {
            int resId = profile.getDrawableResource(btClass);
            if (resId != 0) {
                return new Pair<>(Integer.valueOf(resId), null);
            }
        }
        if (btClass != null) {
            if (btClass.doesClassMatch(1)) {
                return new Pair<>(Integer.valueOf(R.drawable.ic_bt_headphones_a2dp), this.HEADPHONE);
            }
            if (btClass.doesClassMatch(0)) {
                return new Pair<>(Integer.valueOf(R.drawable.ic_bt_headset_hfp), this.HEADSET);
            }
        }
        return new Pair<>(Integer.valueOf(R.drawable.ic_settings_bluetooth), this.BLUETOOTH);
    }
}
