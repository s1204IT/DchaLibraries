package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.bluetooth.CachedBluetoothDevice;
import java.util.HashMap;

public final class DeviceProfilesSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, CachedBluetoothDevice.Callback {
    private final HashMap<LocalBluetoothProfile, CheckBoxPreference> mAutoConnectPrefs = new HashMap<>();
    private CachedBluetoothDevice mCachedDevice;
    private EditTextPreference mDeviceNamePref;
    private AlertDialog mDisconnectDialog;
    private LocalBluetoothManager mManager;
    private PreferenceGroup mProfileContainer;
    private boolean mProfileGroupIsRemoved;
    private LocalBluetoothProfileManager mProfileManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bluetooth_device_advanced);
        getPreferenceScreen().setOrderingAsAdded(false);
        this.mProfileContainer = (PreferenceGroup) findPreference("profile_container");
        this.mProfileContainer.setLayoutResource(R.layout.bluetooth_preference_category);
        this.mManager = LocalBluetoothManager.getInstance(getActivity());
        this.mManager.getCachedDeviceManager();
        this.mProfileManager = this.mManager.getProfileManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mDisconnectDialog != null) {
            this.mDisconnectDialog.dismiss();
            this.mDisconnectDialog = null;
        }
        if (this.mCachedDevice != null) {
            this.mCachedDevice.unregisterCallback(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mManager.setForegroundActivity(getActivity());
        if (this.mCachedDevice != null) {
            this.mCachedDevice.registerCallback(this);
            if (this.mCachedDevice.getBondState() == 10) {
                finish();
            } else {
                refresh();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mCachedDevice != null) {
            this.mCachedDevice.unregisterCallback(this);
        }
        this.mManager.setForegroundActivity(null);
    }

    public void setDevice(CachedBluetoothDevice cachedDevice) {
        this.mCachedDevice = cachedDevice;
        if (isResumed()) {
            this.mCachedDevice.registerCallback(this);
            addPreferencesForProfiles();
            refresh();
        }
    }

    private void addPreferencesForProfiles() {
        this.mProfileContainer.removeAll();
        for (LocalBluetoothProfile profile : this.mCachedDevice.getConnectableProfiles()) {
            Preference pref = createProfilePreference(profile);
            this.mProfileContainer.addPreference(pref);
        }
        int pbapPermission = this.mCachedDevice.getPhonebookPermissionChoice();
        if (pbapPermission != 0) {
            PbapServerProfile psp = this.mManager.getProfileManager().getPbapProfile();
            CheckBoxPreference pbapPref = createProfilePreference(psp);
            this.mProfileContainer.addPreference(pbapPref);
        }
        MapProfile mapProfile = this.mManager.getProfileManager().getMapProfile();
        int mapPermission = this.mCachedDevice.getMessagePermissionChoice();
        if (mapPermission != 0) {
            CheckBoxPreference mapPreference = createProfilePreference(mapProfile);
            this.mProfileContainer.addPreference(mapPreference);
        }
        showOrHideProfileGroup();
    }

    private void showOrHideProfileGroup() {
        int numProfiles = this.mProfileContainer.getPreferenceCount();
        if (!this.mProfileGroupIsRemoved && numProfiles == 0) {
            getPreferenceScreen().removePreference(this.mProfileContainer);
            this.mProfileGroupIsRemoved = true;
        } else if (this.mProfileGroupIsRemoved && numProfiles != 0) {
            getPreferenceScreen().addPreference(this.mProfileContainer);
            this.mProfileGroupIsRemoved = false;
        }
    }

    private CheckBoxPreference createProfilePreference(LocalBluetoothProfile profile) {
        CheckBoxPreference pref = new CheckBoxPreference(getActivity());
        pref.setLayoutResource(R.layout.preference_start_widget);
        pref.setKey(profile.toString());
        pref.setTitle(profile.getNameResource(this.mCachedDevice.getDevice()));
        pref.setPersistent(false);
        pref.setOrder(getProfilePreferenceIndex(profile.getOrdinal()));
        pref.setOnPreferenceChangeListener(this);
        int iconResource = profile.getDrawableResource(this.mCachedDevice.getBtClass());
        if (iconResource != 0) {
            pref.setIcon(getActivity().getDrawable(iconResource));
        }
        refreshProfilePreference(pref, profile);
        return pref;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == this.mDeviceNamePref) {
            this.mCachedDevice.setName((String) newValue);
            return true;
        }
        if (!(preference instanceof CheckBoxPreference)) {
            return false;
        }
        LocalBluetoothProfile prof = getProfileOf(preference);
        onProfileClicked(prof, (CheckBoxPreference) preference);
        return false;
    }

    private void onProfileClicked(LocalBluetoothProfile profile, CheckBoxPreference profilePref) {
        BluetoothDevice device = this.mCachedDevice.getDevice();
        if (profilePref.getKey().equals("PBAP Server")) {
            int newPermission = this.mCachedDevice.getPhonebookPermissionChoice() != 1 ? 1 : 2;
            this.mCachedDevice.setPhonebookPermissionChoice(newPermission);
            profilePref.setChecked(newPermission == 1);
            return;
        }
        int status = profile.getConnectionStatus(device);
        if (status == 2) {
        }
        if (profilePref.isChecked()) {
            askDisconnect(this.mManager.getForegroundActivity(), profile);
            return;
        }
        if (profile instanceof MapProfile) {
            this.mCachedDevice.setMessagePermissionChoice(1);
            refreshProfilePreference(profilePref, profile);
        }
        if (profile.isPreferred(device)) {
            profile.setPreferred(device, false);
            refreshProfilePreference(profilePref, profile);
        } else {
            profile.setPreferred(device, true);
            this.mCachedDevice.connectProfile(profile);
        }
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
                profile.setPreferred(device.getDevice(), false);
                if (profile instanceof MapProfile) {
                    device.setMessagePermissionChoice(2);
                    DeviceProfilesSettings.this.refreshProfilePreference((CheckBoxPreference) DeviceProfilesSettings.this.findPreference(profile.toString()), profile);
                }
            }
        };
        this.mDisconnectDialog = Utils.showDisconnectDialog(context, this.mDisconnectDialog, disconnectListener, title, Html.fromHtml(message));
    }

    @Override
    public void onDeviceAttributesChanged() {
        refresh();
    }

    private void refresh() {
        EditText deviceNameField = (EditText) getView().findViewById(R.id.name);
        if (deviceNameField != null) {
            deviceNameField.setText(this.mCachedDevice.getName());
        }
        refreshProfiles();
    }

    private void refreshProfiles() {
        for (LocalBluetoothProfile profile : this.mCachedDevice.getConnectableProfiles()) {
            CheckBoxPreference profilePref = (CheckBoxPreference) findPreference(profile.toString());
            if (profilePref == null) {
                this.mProfileContainer.addPreference(createProfilePreference(profile));
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }
        for (LocalBluetoothProfile profile2 : this.mCachedDevice.getRemovedProfiles()) {
            Preference profilePref2 = findPreference(profile2.toString());
            if (profilePref2 != null) {
                Log.d("DeviceProfilesSettings", "Removing " + profile2.toString() + " from profile list");
                this.mProfileContainer.removePreference(profilePref2);
            }
        }
        showOrHideProfileGroup();
    }

    private void refreshProfilePreference(CheckBoxPreference profilePref, LocalBluetoothProfile profile) {
        BluetoothDevice device = this.mCachedDevice.getDevice();
        profilePref.setEnabled(!this.mCachedDevice.isBusy());
        if (profile instanceof MapProfile) {
            profilePref.setChecked(this.mCachedDevice.getMessagePermissionChoice() == 1);
        } else if (profile instanceof PbapServerProfile) {
            profilePref.setChecked(this.mCachedDevice.getPhonebookPermissionChoice() == 1);
        } else {
            profilePref.setChecked(profile.isPreferred(device));
        }
    }

    private LocalBluetoothProfile getProfileOf(Preference pref) {
        if (!(pref instanceof CheckBoxPreference)) {
            return null;
        }
        String key = pref.getKey();
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        try {
            return this.mProfileManager.getProfileByName(pref.getKey());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int getProfilePreferenceIndex(int profIndex) {
        return this.mProfileContainer.getOrder() + (profIndex * 10);
    }
}
