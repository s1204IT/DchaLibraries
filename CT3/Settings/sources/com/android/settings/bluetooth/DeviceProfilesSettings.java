package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;
import com.mediatek.bluetooth.DeviceProfilesSettingsExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settingslib.bluetooth.DunServerProfile;
import java.util.HashMap;

public final class DeviceProfilesSettings extends DialogFragment implements CachedBluetoothDevice.Callback, DialogInterface.OnClickListener, View.OnClickListener {
    private AlertDialog mAlertDialog;
    private final HashMap<LocalBluetoothProfile, CheckBoxPreference> mAutoConnectPrefs = new HashMap<>();
    private CachedBluetoothDevice mCachedDevice;
    private DeviceProfilesSettingsExt mDeviceProfilesSettingsExt;
    private AlertDialog mDisconnectDialog;
    private LocalBluetoothManager mManager;
    private ViewGroup mProfileContainer;
    private boolean mProfileGroupIsRemoved;
    private TextView mProfileLabel;
    private LocalBluetoothProfileManager mProfileManager;
    private View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mManager = Utils.getLocalBtManager(getActivity());
        CachedBluetoothDeviceManager deviceManager = this.mManager.getCachedDeviceManager();
        String address = getArguments().getString("device_address");
        BluetoothDevice remoteDevice = this.mManager.getBluetoothAdapter().getRemoteDevice(address);
        this.mCachedDevice = deviceManager.findDevice(remoteDevice);
        if (this.mCachedDevice == null) {
            this.mCachedDevice = deviceManager.addDevice(this.mManager.getBluetoothAdapter(), this.mManager.getProfileManager(), remoteDevice);
        }
        this.mProfileManager = this.mManager.getProfileManager();
        this.mDeviceProfilesSettingsExt = new DeviceProfilesSettingsExt(getActivity(), this, this.mCachedDevice);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.mRootView = LayoutInflater.from(getContext()).inflate(R.layout.device_profiles_settings, (ViewGroup) null);
        this.mProfileContainer = (ViewGroup) this.mRootView.findViewById(R.id.profiles_section);
        this.mProfileLabel = (TextView) this.mRootView.findViewById(R.id.profiles_label);
        EditText deviceName = (EditText) this.mRootView.findViewById(R.id.name);
        deviceName.setText(this.mCachedDevice.getName(), TextView.BufferType.EDITABLE);
        this.mAlertDialog = new AlertDialog.Builder(getContext()).setView(this.mRootView).setNegativeButton(R.string.forget, this).setPositiveButton(R.string.okay, this).setTitle(R.string.bluetooth_preference_paired_devices).create();
        deviceName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                boolean isEffective = s.toString().trim().length() > 0;
                DeviceProfilesSettings.this.mAlertDialog.getButton(-1).setEnabled(isEffective);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        return this.mAlertDialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                this.mCachedDevice.unpair();
                Utils.updateSearchIndex(getContext(), BluetoothSettings.class.getName(), this.mCachedDevice.getName(), getString(R.string.bluetooth_settings), R.drawable.ic_settings_bluetooth, false);
                break;
            case -1:
                EditText deviceName = (EditText) this.mRootView.findViewById(R.id.name);
                this.mCachedDevice.setName(deviceName.getText().toString());
                break;
        }
    }

    @Override
    public void onDestroy() {
        Log.d("DeviceProfilesSettings", "onDestroy");
        super.onDestroy();
        if (this.mDisconnectDialog != null) {
            this.mDisconnectDialog.dismiss();
            this.mDisconnectDialog = null;
        }
        this.mAlertDialog = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("DeviceProfilesSettings", "onResume");
        this.mManager.setForegroundActivity(getActivity());
        if (this.mCachedDevice == null) {
            return;
        }
        this.mCachedDevice.registerCallback(this);
        Log.d("DeviceProfilesSettings", "onResume, registerCallback");
        if (this.mCachedDevice.getBondState() == 10) {
            dismiss();
        } else {
            addPreferencesForProfiles();
            refresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("DeviceProfilesSettings", "onPause");
        if (this.mCachedDevice != null) {
            this.mCachedDevice.unregisterCallback(this);
            Log.d("DeviceProfilesSettings", "onPause, unregisterCallback");
        }
        this.mManager.setForegroundActivity(null);
    }

    private void addPreferencesForProfiles() {
        this.mProfileContainer.removeAllViews();
        if (FeatureOption.MTK_Bluetooth_DUN) {
            this.mDeviceProfilesSettingsExt.addPreferencesForProfiles(this.mProfileContainer, this.mCachedDevice);
        }
        for (LocalBluetoothProfile profile : this.mCachedDevice.getConnectableProfiles()) {
            if (!(profile instanceof DunServerProfile)) {
                CheckBox pref = createProfilePreference(profile);
                this.mProfileContainer.addView(pref);
            }
        }
        int pbapPermission = this.mCachedDevice.getPhonebookPermissionChoice();
        if (pbapPermission != 0) {
            PbapServerProfile psp = this.mManager.getProfileManager().getPbapProfile();
            CheckBox pbapPref = createProfilePreference(psp);
            this.mProfileContainer.addView(pbapPref);
        }
        MapProfile mapProfile = this.mManager.getProfileManager().getMapProfile();
        int mapPermission = this.mCachedDevice.getMessagePermissionChoice();
        if (mapPermission != 0) {
            CheckBox mapPreference = createProfilePreference(mapProfile);
            this.mProfileContainer.addView(mapPreference);
        }
        showOrHideProfileGroup();
    }

    private void showOrHideProfileGroup() {
        int numProfiles = this.mProfileContainer.getChildCount();
        if (!this.mProfileGroupIsRemoved && numProfiles == 0) {
            this.mProfileContainer.setVisibility(8);
            this.mProfileLabel.setVisibility(8);
            this.mProfileGroupIsRemoved = true;
        } else {
            if (!this.mProfileGroupIsRemoved || numProfiles == 0) {
                return;
            }
            this.mProfileContainer.setVisibility(0);
            this.mProfileLabel.setVisibility(0);
            this.mProfileGroupIsRemoved = false;
        }
    }

    private CheckBox createProfilePreference(LocalBluetoothProfile profile) {
        CheckBox pref = new CheckBox(getActivity());
        pref.setTag(profile.toString());
        pref.setText(profile.getNameResource(this.mCachedDevice.getDevice()));
        pref.setOnClickListener(this);
        refreshProfilePreference(pref, profile);
        return pref;
    }

    @Override
    public void onClick(View v) {
        if (!(v instanceof CheckBox)) {
            return;
        }
        LocalBluetoothProfile prof = getProfileOf(v);
        onProfileClicked(prof, (CheckBox) v);
    }

    private void onProfileClicked(LocalBluetoothProfile profile, CheckBox profilePref) {
        BluetoothDevice device = this.mCachedDevice.getDevice();
        if ("PBAP Server".equals(profilePref.getTag())) {
            int newPermission = this.mCachedDevice.getPhonebookPermissionChoice() == 1 ? 2 : 1;
            this.mCachedDevice.setPhonebookPermissionChoice(newPermission);
            profilePref.setChecked(newPermission == 1);
            return;
        }
        if (!profilePref.isChecked()) {
            profilePref.setChecked(true);
            askDisconnect(this.mManager.getForegroundActivity(), profile);
            return;
        }
        if (profile instanceof MapProfile) {
            this.mCachedDevice.setMessagePermissionChoice(1);
        }
        Log.d("DeviceProfilesSettings", this.mCachedDevice.getName() + " " + profile.toString() + " isPreferred() : " + profile.isPreferred(device));
        if (!profile.isPreferred(device)) {
            profile.setPreferred(device, true);
            Log.d("DeviceProfilesSettings", profile.toString() + " setPreferred true and connect profile");
            this.mCachedDevice.connectProfile(profile);
        } else if (profile instanceof PanProfile) {
            this.mCachedDevice.connectProfile(profile);
        } else {
            Log.d("DeviceProfilesSettings", profile.toString() + " setPreferred false");
            profile.setPreferred(device, false);
        }
        refreshProfilePreference(profilePref, profile);
    }

    private void askDisconnect(Context context, final LocalBluetoothProfile profile) {
        final CachedBluetoothDevice device = this.mCachedDevice;
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String profileName = context.getString(profile.getNameResource(device.getDevice()));
        String title = context.getString(R.string.bluetooth_disable_profile_title);
        String message = context.getString(R.string.bluetooth_disable_profile_message, profileName, name);
        DialogInterface.OnClickListener disconnectListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                device.disconnect(profile);
                Log.d("DeviceProfilesSettings", "disconnect " + profile.toString() + " , setPreferred false");
                profile.setPreferred(device.getDevice(), false);
                if (profile instanceof MapProfile) {
                    device.setMessagePermissionChoice(2);
                }
                DeviceProfilesSettings.this.refreshProfilePreference(DeviceProfilesSettings.this.findProfile(profile.toString()), profile);
            }
        };
        this.mDisconnectDialog = Utils.showDisconnectDialog(context, this.mDisconnectDialog, disconnectListener, title, Html.fromHtml(message));
    }

    @Override
    public void onDeviceAttributesChanged() {
        refresh();
    }

    private void refresh() {
        EditText deviceNameField = (EditText) this.mRootView.findViewById(R.id.name);
        if (deviceNameField != null) {
            deviceNameField.setText(this.mCachedDevice.getName());
        }
        refreshProfiles();
    }

    private void refreshProfiles() {
        for (LocalBluetoothProfile profile : this.mCachedDevice.getConnectableProfiles()) {
            CheckBox profilePref = findProfile(profile.toString());
            if (profilePref == null) {
                this.mProfileContainer.addView(createProfilePreference(profile));
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }
        for (LocalBluetoothProfile profile2 : this.mCachedDevice.getRemovedProfiles()) {
            CheckBox profilePref2 = findProfile(profile2.toString());
            if (profilePref2 != null) {
                Log.d("DeviceProfilesSettings", "Removing " + profile2.toString() + " from profile list");
                this.mProfileContainer.removeView(profilePref2);
            }
        }
        showOrHideProfileGroup();
    }

    public CheckBox findProfile(String profile) {
        return (CheckBox) this.mProfileContainer.findViewWithTag(profile);
    }

    public void refreshProfilePreference(CheckBox profilePref, LocalBluetoothProfile profile) {
        BluetoothDevice device = this.mCachedDevice.getDevice();
        Log.d("DeviceProfilesSettings", "isBusy : " + this.mCachedDevice.isBusy());
        profilePref.setEnabled(!this.mCachedDevice.isBusy());
        if (profile instanceof MapProfile) {
            profilePref.setChecked(this.mCachedDevice.getMessagePermissionChoice() == 1);
            return;
        }
        if (profile instanceof PbapServerProfile) {
            profilePref.setChecked(this.mCachedDevice.getPhonebookPermissionChoice() == 1);
            return;
        }
        if (profile instanceof PanProfile) {
            profilePref.setChecked(profile.getConnectionStatus(device) == 2);
        } else if (profile instanceof DunServerProfile) {
            Log.d("DeviceProfilesSettings", "DunProfile=" + (profile.getConnectionStatus(device) == 2));
            profilePref.setChecked(profile.getConnectionStatus(device) == 2);
        } else {
            Log.d("DeviceProfilesSettings", profile.toString() + " isPreferred : " + profile.isPreferred(device));
            profilePref.setChecked(profile.isPreferred(device));
        }
    }

    private LocalBluetoothProfile getProfileOf(View v) {
        if (!(v instanceof CheckBox)) {
            return null;
        }
        String key = (String) v.getTag();
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        try {
            return this.mProfileManager.getProfileByName(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
