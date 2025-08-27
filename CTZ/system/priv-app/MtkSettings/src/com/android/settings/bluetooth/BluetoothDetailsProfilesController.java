package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class BluetoothDetailsProfilesController extends BluetoothDetailsController implements Preference.OnPreferenceClickListener {
    static final String HIGH_QUALITY_AUDIO_PREF_TAG = "A2dpProfileHighQualityAudio";
    private CachedBluetoothDevice mCachedDevice;
    private LocalBluetoothManager mManager;
    private LocalBluetoothProfileManager mProfileManager;
    private PreferenceCategory mProfilesContainer;

    public BluetoothDetailsProfilesController(Context context, PreferenceFragment preferenceFragment, LocalBluetoothManager localBluetoothManager, CachedBluetoothDevice cachedBluetoothDevice, Lifecycle lifecycle) {
        super(context, preferenceFragment, cachedBluetoothDevice, lifecycle);
        this.mManager = localBluetoothManager;
        this.mProfileManager = this.mManager.getProfileManager();
        this.mCachedDevice = cachedBluetoothDevice;
        lifecycle.addObserver(this);
    }

    @Override // com.android.settings.bluetooth.BluetoothDetailsController
    protected void init(PreferenceScreen preferenceScreen) {
        this.mProfilesContainer = (PreferenceCategory) preferenceScreen.findPreference(getPreferenceKey());
        refresh();
    }

    private SwitchPreference createProfilePreference(Context context, LocalBluetoothProfile localBluetoothProfile) {
        SwitchPreference switchPreference = new SwitchPreference(context);
        switchPreference.setKey(localBluetoothProfile.toString());
        switchPreference.setTitle(localBluetoothProfile.getNameResource(this.mCachedDevice.getDevice()));
        switchPreference.setOnPreferenceClickListener(this);
        return switchPreference;
    }

    private void refreshProfilePreference(SwitchPreference switchPreference, LocalBluetoothProfile localBluetoothProfile) {
        BluetoothDevice device = this.mCachedDevice.getDevice();
        switchPreference.setEnabled(!this.mCachedDevice.isBusy());
        if (localBluetoothProfile instanceof MapProfile) {
            switchPreference.setChecked(this.mCachedDevice.getMessagePermissionChoice() == 1);
        } else if (localBluetoothProfile instanceof PbapServerProfile) {
            switchPreference.setChecked(this.mCachedDevice.getPhonebookPermissionChoice() == 1);
        } else if (localBluetoothProfile instanceof PanProfile) {
            switchPreference.setChecked(localBluetoothProfile.getConnectionStatus(device) == 2);
        } else {
            switchPreference.setChecked(localBluetoothProfile.isPreferred(device));
        }
        if (localBluetoothProfile instanceof A2dpProfile) {
            A2dpProfile a2dpProfile = (A2dpProfile) localBluetoothProfile;
            SwitchPreference switchPreference2 = (SwitchPreference) this.mProfilesContainer.findPreference(HIGH_QUALITY_AUDIO_PREF_TAG);
            if (switchPreference2 != null) {
                if (a2dpProfile.isPreferred(device) && a2dpProfile.supportsHighQualityAudio(device)) {
                    switchPreference2.setVisible(true);
                    switchPreference2.setTitle(a2dpProfile.getHighQualityAudioOptionLabel(device));
                    switchPreference2.setChecked(a2dpProfile.isHighQualityAudioEnabled(device));
                    switchPreference2.setEnabled(!this.mCachedDevice.isBusy());
                    return;
                }
                switchPreference2.setVisible(false);
            }
        }
    }

    private void enableProfile(LocalBluetoothProfile localBluetoothProfile, BluetoothDevice bluetoothDevice, SwitchPreference switchPreference) {
        if (localBluetoothProfile instanceof PbapServerProfile) {
            this.mCachedDevice.setPhonebookPermissionChoice(1);
            return;
        }
        if (localBluetoothProfile instanceof MapProfile) {
            this.mCachedDevice.setMessagePermissionChoice(1);
        }
        localBluetoothProfile.setPreferred(bluetoothDevice, true);
        this.mCachedDevice.connectProfile(localBluetoothProfile);
    }

    private void disableProfile(LocalBluetoothProfile localBluetoothProfile, BluetoothDevice bluetoothDevice, SwitchPreference switchPreference) {
        if (localBluetoothProfile instanceof PbapServerProfile) {
            this.mCachedDevice.setPhonebookPermissionChoice(2);
            return;
        }
        this.mCachedDevice.disconnect(localBluetoothProfile);
        localBluetoothProfile.setPreferred(bluetoothDevice, false);
        if (localBluetoothProfile instanceof MapProfile) {
            this.mCachedDevice.setMessagePermissionChoice(2);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        LocalBluetoothProfile profileByName = this.mProfileManager.getProfileByName(preference.getKey());
        LocalBluetoothProfile localBluetoothProfile = profileByName;
        if (profileByName == null) {
            PbapServerProfile pbapProfile = this.mManager.getProfileManager().getPbapProfile();
            boolean zEquals = TextUtils.equals(preference.getKey(), pbapProfile.toString());
            localBluetoothProfile = pbapProfile;
            if (!zEquals) {
                return false;
            }
        }
        SwitchPreference switchPreference = (SwitchPreference) preference;
        BluetoothDevice device = this.mCachedDevice.getDevice();
        if (switchPreference.isChecked()) {
            enableProfile(localBluetoothProfile, device, switchPreference);
        } else {
            disableProfile(localBluetoothProfile, device, switchPreference);
        }
        refreshProfilePreference(switchPreference, localBluetoothProfile);
        return true;
    }

    private List<LocalBluetoothProfile> getProfiles() {
        List<LocalBluetoothProfile> connectableProfiles = this.mCachedDevice.getConnectableProfiles();
        if (this.mCachedDevice.getPhonebookPermissionChoice() != 0) {
            connectableProfiles.add(this.mManager.getProfileManager().getPbapProfile());
        }
        MapProfile mapProfile = this.mManager.getProfileManager().getMapProfile();
        if (this.mCachedDevice.getMessagePermissionChoice() != 0) {
            connectableProfiles.add(mapProfile);
        }
        return connectableProfiles;
    }

    private void maybeAddHighQualityAudioPref(LocalBluetoothProfile localBluetoothProfile) {
        if (!(localBluetoothProfile instanceof A2dpProfile)) {
            return;
        }
        final A2dpProfile a2dpProfile = (A2dpProfile) localBluetoothProfile;
        if (a2dpProfile.supportsHighQualityAudio(this.mCachedDevice.getDevice())) {
            SwitchPreference switchPreference = new SwitchPreference(this.mProfilesContainer.getContext());
            switchPreference.setKey(HIGH_QUALITY_AUDIO_PREF_TAG);
            switchPreference.setVisible(false);
            switchPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.bluetooth.-$$Lambda$BluetoothDetailsProfilesController$pv2kZi3KDLDrPBqbb1ECR74MeRo
                @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
                public final boolean onPreferenceClick(Preference preference) {
                    return BluetoothDetailsProfilesController.lambda$maybeAddHighQualityAudioPref$0(this.f$0, a2dpProfile, preference);
                }
            });
            this.mProfilesContainer.addPreference(switchPreference);
        }
    }

    public static /* synthetic */ boolean lambda$maybeAddHighQualityAudioPref$0(BluetoothDetailsProfilesController bluetoothDetailsProfilesController, A2dpProfile a2dpProfile, Preference preference) {
        a2dpProfile.setHighQualityAudioEnabled(bluetoothDetailsProfilesController.mCachedDevice.getDevice(), ((SwitchPreference) preference).isChecked());
        return true;
    }

    @Override // com.android.settings.bluetooth.BluetoothDetailsController
    protected void refresh() {
        for (LocalBluetoothProfile localBluetoothProfile : getProfiles()) {
            SwitchPreference switchPreferenceCreateProfilePreference = (SwitchPreference) this.mProfilesContainer.findPreference(localBluetoothProfile.toString());
            if (switchPreferenceCreateProfilePreference == null) {
                switchPreferenceCreateProfilePreference = createProfilePreference(this.mProfilesContainer.getContext(), localBluetoothProfile);
                this.mProfilesContainer.addPreference(switchPreferenceCreateProfilePreference);
                maybeAddHighQualityAudioPref(localBluetoothProfile);
            }
            refreshProfilePreference(switchPreferenceCreateProfilePreference, localBluetoothProfile);
        }
        Iterator<LocalBluetoothProfile> it = this.mCachedDevice.getRemovedProfiles().iterator();
        while (it.hasNext()) {
            SwitchPreference switchPreference = (SwitchPreference) this.mProfilesContainer.findPreference(it.next().toString());
            if (switchPreference != null) {
                this.mProfilesContainer.removePreference(switchPreference);
            }
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bluetooth_profiles";
    }
}
