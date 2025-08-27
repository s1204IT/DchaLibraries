package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;

/* loaded from: classes.dex */
public final class DeviceProfilesSettings extends InstrumentedDialogFragment implements DialogInterface.OnClickListener, View.OnClickListener, CachedBluetoothDevice.Callback {
    static final String HIGH_QUALITY_AUDIO_PREF_TAG = "A2dpProfileHighQualityAudio";
    private AlertDialog mAlertDialog;
    private CachedBluetoothDevice mCachedDevice;
    private AlertDialog mDisconnectDialog;
    private LocalBluetoothManager mManager;
    private ViewGroup mProfileContainer;
    private boolean mProfileGroupIsRemoved;
    private TextView mProfileLabel;
    private LocalBluetoothProfileManager mProfileManager;
    private View mRootView;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 539;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mManager = Utils.getLocalBtManager(getActivity());
        CachedBluetoothDeviceManager cachedDeviceManager = this.mManager.getCachedDeviceManager();
        BluetoothDevice remoteDevice = this.mManager.getBluetoothAdapter().getRemoteDevice(getArguments().getString("device_address"));
        this.mCachedDevice = cachedDeviceManager.findDevice(remoteDevice);
        if (this.mCachedDevice == null) {
            this.mCachedDevice = cachedDeviceManager.addDevice(this.mManager.getBluetoothAdapter(), this.mManager.getProfileManager(), remoteDevice);
        }
        this.mProfileManager = this.mManager.getProfileManager();
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        this.mRootView = LayoutInflater.from(getContext()).inflate(R.layout.device_profiles_settings, (ViewGroup) null);
        this.mProfileContainer = (ViewGroup) this.mRootView.findViewById(R.id.profiles_section);
        this.mProfileLabel = (TextView) this.mRootView.findViewById(R.id.profiles_label);
        EditText editText = (EditText) this.mRootView.findViewById(R.id.name);
        editText.setText(this.mCachedDevice.getName(), TextView.BufferType.EDITABLE);
        this.mAlertDialog = new AlertDialog.Builder(getContext()).setView(this.mRootView).setNeutralButton(R.string.forget, this).setPositiveButton(R.string.okay, this).setTitle(R.string.bluetooth_preference_paired_devices).create();
        editText.addTextChangedListener(new TextWatcher() { // from class: com.android.settings.bluetooth.DeviceProfilesSettings.1
            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable editable) {
                DeviceProfilesSettings.this.mAlertDialog.getButton(-1).setEnabled(editable.toString().trim().length() > 0);
            }

            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
        });
        return this.mAlertDialog;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -3) {
            this.mCachedDevice.unpair();
        } else if (i == -1) {
            this.mCachedDevice.setName(((EditText) this.mRootView.findViewById(R.id.name)).getText().toString());
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        Log.d("DeviceProfilesSettings", "onDestroy");
        if (this.mDisconnectDialog != null) {
            this.mDisconnectDialog.dismiss();
            this.mDisconnectDialog = null;
        }
        this.mAlertDialog = null;
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        Log.d("DeviceProfilesSettings", "onResume");
        this.mManager.setForegroundActivity(getActivity());
        if (this.mCachedDevice != null) {
            this.mCachedDevice.registerCallback(this);
            Log.d("DeviceProfilesSettings", "onResume, registerCallback");
            if (this.mCachedDevice.getBondState() == 10) {
                dismiss();
            } else {
                addPreferencesForProfiles();
                refresh();
            }
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableDialogFragment, android.app.Fragment
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
        for (LocalBluetoothProfile localBluetoothProfile : this.mCachedDevice.getConnectableProfiles()) {
            CheckBox checkBoxCreateProfilePreference = createProfilePreference(localBluetoothProfile);
            if (!(localBluetoothProfile instanceof PbapServerProfile) && !(localBluetoothProfile instanceof MapProfile)) {
                this.mProfileContainer.addView(checkBoxCreateProfilePreference);
            }
            if (localBluetoothProfile instanceof A2dpProfile) {
                final BluetoothDevice device = this.mCachedDevice.getDevice();
                final A2dpProfile a2dpProfile = (A2dpProfile) localBluetoothProfile;
                if (a2dpProfile.supportsHighQualityAudio(device)) {
                    final CheckBox checkBox = new CheckBox(getActivity());
                    checkBox.setTag(HIGH_QUALITY_AUDIO_PREF_TAG);
                    checkBox.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.bluetooth.-$$Lambda$DeviceProfilesSettings$qBNrFA8-Smm3qyHXDkezO-CS7tQ
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view) {
                            a2dpProfile.setHighQualityAudioEnabled(device, checkBox.isChecked());
                        }
                    });
                    checkBox.setVisibility(8);
                    this.mProfileContainer.addView(checkBox);
                }
                refreshProfilePreference(checkBoxCreateProfilePreference, localBluetoothProfile);
            }
        }
        int phonebookPermissionChoice = this.mCachedDevice.getPhonebookPermissionChoice();
        Log.d("DeviceProfilesSettings", "addPreferencesForProfiles: pbapPermission = " + phonebookPermissionChoice);
        if (phonebookPermissionChoice != 0) {
            this.mProfileContainer.addView(createProfilePreference(this.mManager.getProfileManager().getPbapProfile()));
        }
        MapProfile mapProfile = this.mManager.getProfileManager().getMapProfile();
        int messagePermissionChoice = this.mCachedDevice.getMessagePermissionChoice();
        Log.d("DeviceProfilesSettings", "addPreferencesForProfiles: mapPermission = " + messagePermissionChoice);
        if (messagePermissionChoice != 0) {
            this.mProfileContainer.addView(createProfilePreference(mapProfile));
        }
        showOrHideProfileGroup();
    }

    private void showOrHideProfileGroup() {
        int childCount = this.mProfileContainer.getChildCount();
        if (!this.mProfileGroupIsRemoved && childCount == 0) {
            this.mProfileContainer.setVisibility(8);
            this.mProfileLabel.setVisibility(8);
            this.mProfileGroupIsRemoved = true;
        } else if (this.mProfileGroupIsRemoved && childCount != 0) {
            this.mProfileContainer.setVisibility(0);
            this.mProfileLabel.setVisibility(0);
            this.mProfileGroupIsRemoved = false;
        }
    }

    private CheckBox createProfilePreference(LocalBluetoothProfile localBluetoothProfile) {
        CheckBox checkBox = new CheckBox(getActivity());
        checkBox.setTag(localBluetoothProfile.toString());
        checkBox.setText(localBluetoothProfile.getNameResource(this.mCachedDevice.getDevice()));
        checkBox.setOnClickListener(this);
        refreshProfilePreference(checkBox, localBluetoothProfile);
        return checkBox;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view instanceof CheckBox) {
            onProfileClicked(getProfileOf(view), (CheckBox) view);
        }
    }

    private void onProfileClicked(LocalBluetoothProfile localBluetoothProfile, CheckBox checkBox) {
        BluetoothDevice device = this.mCachedDevice.getDevice();
        if (!checkBox.isChecked()) {
            checkBox.setChecked(true);
            askDisconnect(this.mManager.getForegroundActivity(), localBluetoothProfile);
            return;
        }
        if (localBluetoothProfile instanceof MapProfile) {
            this.mCachedDevice.setMessagePermissionChoice(1);
        }
        if (localBluetoothProfile instanceof PbapServerProfile) {
            this.mCachedDevice.setPhonebookPermissionChoice(1);
            refreshProfilePreference(checkBox, localBluetoothProfile);
            return;
        }
        if (localBluetoothProfile.isPreferred(device)) {
            if (localBluetoothProfile instanceof PanProfile) {
                this.mCachedDevice.connectProfile(localBluetoothProfile);
            } else {
                localBluetoothProfile.setPreferred(device, false);
            }
        } else {
            localBluetoothProfile.setPreferred(device, true);
            this.mCachedDevice.connectProfile(localBluetoothProfile);
        }
        refreshProfilePreference(checkBox, localBluetoothProfile);
    }

    private void askDisconnect(Context context, final LocalBluetoothProfile localBluetoothProfile) {
        final CachedBluetoothDevice cachedBluetoothDevice = this.mCachedDevice;
        String name = cachedBluetoothDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String string = context.getString(localBluetoothProfile.getNameResource(cachedBluetoothDevice.getDevice()));
        this.mDisconnectDialog = Utils.showDisconnectDialog(context, this.mDisconnectDialog, new DialogInterface.OnClickListener() { // from class: com.android.settings.bluetooth.DeviceProfilesSettings.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1) {
                    cachedBluetoothDevice.disconnect(localBluetoothProfile);
                    localBluetoothProfile.setPreferred(cachedBluetoothDevice.getDevice(), false);
                    if (localBluetoothProfile instanceof MapProfile) {
                        cachedBluetoothDevice.setMessagePermissionChoice(2);
                    }
                    if (localBluetoothProfile instanceof PbapServerProfile) {
                        cachedBluetoothDevice.setPhonebookPermissionChoice(2);
                    }
                }
                DeviceProfilesSettings.this.refreshProfilePreference(DeviceProfilesSettings.this.findProfile(localBluetoothProfile.toString()), localBluetoothProfile);
            }
        }, context.getString(R.string.bluetooth_disable_profile_title), Html.fromHtml(context.getString(R.string.bluetooth_disable_profile_message, string, name)));
    }

    @Override // com.android.settingslib.bluetooth.CachedBluetoothDevice.Callback
    public void onDeviceAttributesChanged() {
        refresh();
    }

    private void refresh() {
        EditText editText = (EditText) this.mRootView.findViewById(R.id.name);
        if (editText != null) {
            editText.setText(this.mCachedDevice.getName());
            com.android.settings.Utils.setEditTextCursorPosition(editText);
        }
        refreshProfiles();
    }

    private void refreshProfiles() {
        for (LocalBluetoothProfile localBluetoothProfile : this.mCachedDevice.getConnectableProfiles()) {
            CheckBox checkBoxFindProfile = findProfile(localBluetoothProfile.toString());
            if (checkBoxFindProfile == null) {
                this.mProfileContainer.addView(createProfilePreference(localBluetoothProfile));
            } else {
                refreshProfilePreference(checkBoxFindProfile, localBluetoothProfile);
            }
        }
        for (LocalBluetoothProfile localBluetoothProfile2 : this.mCachedDevice.getRemovedProfiles()) {
            CheckBox checkBoxFindProfile2 = findProfile(localBluetoothProfile2.toString());
            if (checkBoxFindProfile2 != null) {
                if (localBluetoothProfile2 instanceof PbapServerProfile) {
                    int phonebookPermissionChoice = this.mCachedDevice.getPhonebookPermissionChoice();
                    Log.d("DeviceProfilesSettings", "refreshProfiles: pbapPermission = " + phonebookPermissionChoice);
                    if (phonebookPermissionChoice != 0) {
                    }
                }
                if (localBluetoothProfile2 instanceof MapProfile) {
                    int messagePermissionChoice = this.mCachedDevice.getMessagePermissionChoice();
                    Log.d("DeviceProfilesSettings", "refreshProfiles: mapPermission = " + messagePermissionChoice);
                    if (messagePermissionChoice != 0) {
                    }
                }
                Log.d("DeviceProfilesSettings", "Removing " + localBluetoothProfile2.toString() + " from profile list");
                this.mProfileContainer.removeView(checkBoxFindProfile2);
            }
        }
        showOrHideProfileGroup();
    }

    private CheckBox findProfile(String str) {
        return (CheckBox) this.mProfileContainer.findViewWithTag(str);
    }

    private void refreshProfilePreference(CheckBox checkBox, LocalBluetoothProfile localBluetoothProfile) {
        BluetoothDevice device = this.mCachedDevice.getDevice();
        checkBox.setEnabled(!this.mCachedDevice.isBusy());
        if (localBluetoothProfile instanceof MapProfile) {
            checkBox.setChecked(this.mCachedDevice.getMessagePermissionChoice() == 1);
        } else if (localBluetoothProfile instanceof PbapServerProfile) {
            checkBox.setChecked(this.mCachedDevice.getPhonebookPermissionChoice() == 1);
        } else if (localBluetoothProfile instanceof PanProfile) {
            checkBox.setChecked(localBluetoothProfile.getConnectionStatus(device) == 2);
        } else {
            checkBox.setChecked(localBluetoothProfile.isPreferred(device));
        }
        if (localBluetoothProfile instanceof A2dpProfile) {
            A2dpProfile a2dpProfile = (A2dpProfile) localBluetoothProfile;
            View viewFindViewWithTag = this.mProfileContainer.findViewWithTag(HIGH_QUALITY_AUDIO_PREF_TAG);
            if (viewFindViewWithTag instanceof CheckBox) {
                CheckBox checkBox2 = (CheckBox) viewFindViewWithTag;
                checkBox2.setText(a2dpProfile.getHighQualityAudioOptionLabel(device));
                checkBox2.setChecked(a2dpProfile.isHighQualityAudioEnabled(device));
                if (a2dpProfile.isPreferred(device)) {
                    viewFindViewWithTag.setVisibility(0);
                    viewFindViewWithTag.setEnabled(!this.mCachedDevice.isBusy());
                } else {
                    viewFindViewWithTag.setVisibility(8);
                }
            }
        }
    }

    private LocalBluetoothProfile getProfileOf(View view) {
        if (!(view instanceof CheckBox)) {
            return null;
        }
        String str = (String) view.getTag();
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            return this.mProfileManager.getProfileByName(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
