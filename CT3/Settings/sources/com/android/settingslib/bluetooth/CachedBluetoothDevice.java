package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.R$string;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private BluetoothClass mBtClass;
    private boolean mConnectAfterPairing;
    private long mConnectAttempted;
    private final Context mContext;
    private final BluetoothDevice mDevice;
    private boolean mIsConnectingErrorPossible;
    private final LocalBluetoothAdapter mLocalAdapter;
    private boolean mLocalNapRoleConnected;
    private int mMessageRejectionCount;
    private String mName;
    private final LocalBluetoothProfileManager mProfileManager;
    private short mRssi;
    private boolean mVisible;
    private final List<LocalBluetoothProfile> mProfiles = new ArrayList();
    private final List<LocalBluetoothProfile> mRemovedProfiles = new ArrayList();
    private final Collection<Callback> mCallbacks = new ArrayList();
    private HashMap<LocalBluetoothProfile, Integer> mProfileConnectionState = new HashMap<>();

    public interface Callback {
        void onDeviceAttributesChanged();
    }

    private String describe(LocalBluetoothProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:").append(this.mDevice);
        if (profile != null) {
            sb.append(" Profile:").append(profile);
        }
        return sb.toString();
    }

    void onProfileStateChanged(LocalBluetoothProfile profile, int newProfileState) {
        Log.d("CachedBluetoothDevice", "onProfileStateChanged: profile " + profile + " newProfileState " + newProfileState);
        if (this.mLocalAdapter.getBluetoothState() == 13) {
            Log.d("CachedBluetoothDevice", " BT Turninig Off...Profile conn state change ignored...");
            return;
        }
        this.mProfileConnectionState.put(profile, Integer.valueOf(newProfileState));
        if (newProfileState == 2) {
            if (profile instanceof MapProfile) {
                profile.setPreferred(this.mDevice, true);
                return;
            }
            if (this.mProfiles.contains(profile)) {
                return;
            }
            this.mRemovedProfiles.remove(profile);
            this.mProfiles.add(profile);
            if (!(profile instanceof PanProfile) || !((PanProfile) profile).isLocalRoleNap(this.mDevice)) {
                return;
            }
            this.mLocalNapRoleConnected = true;
            return;
        }
        if ((profile instanceof MapProfile) && newProfileState == 0) {
            profile.setPreferred(this.mDevice, false);
            refresh();
        } else {
            if (!this.mLocalNapRoleConnected || !(profile instanceof PanProfile) || !((PanProfile) profile).isLocalRoleNap(this.mDevice) || newProfileState != 0) {
                return;
            }
            Log.d("CachedBluetoothDevice", "Removing PanProfile from device after NAP disconnect");
            this.mProfiles.remove(profile);
            this.mRemovedProfiles.add(profile);
            this.mLocalNapRoleConnected = false;
        }
    }

    CachedBluetoothDevice(Context context, LocalBluetoothAdapter adapter, LocalBluetoothProfileManager profileManager, BluetoothDevice device) {
        this.mContext = context;
        this.mLocalAdapter = adapter;
        this.mProfileManager = profileManager;
        this.mDevice = device;
        fillData();
    }

    public void disconnect() {
        for (LocalBluetoothProfile profile : this.mProfiles) {
            disconnect(profile);
        }
        PbapServerProfile PbapProfile = this.mProfileManager.getPbapProfile();
        if (PbapProfile.getConnectionStatus(this.mDevice) != 2) {
            return;
        }
        PbapProfile.disconnect(this.mDevice);
    }

    public void disconnect(LocalBluetoothProfile profile) {
        if (!profile.disconnect(this.mDevice)) {
            return;
        }
        Log.d("CachedBluetoothDevice", "Command sent successfully:DISCONNECT " + describe(profile));
    }

    public void connect(boolean connectAllProfiles) {
        if (!ensurePaired()) {
            return;
        }
        this.mConnectAttempted = SystemClock.elapsedRealtime();
        connectWithoutResettingTimer(connectAllProfiles);
    }

    void onBondingDockConnect() {
        connect(false);
    }

    private void connectWithoutResettingTimer(boolean connectAllProfiles) {
        if (this.mProfiles.isEmpty()) {
            Log.d("CachedBluetoothDevice", "No profiles. Maybe we will connect later");
            return;
        }
        this.mIsConnectingErrorPossible = true;
        int preferredProfiles = 0;
        for (LocalBluetoothProfile profile : this.mProfiles) {
            if (connectAllProfiles ? profile.isConnectable() : profile.isAutoConnectable()) {
                Log.d("CachedBluetoothDevice", describe(profile) + " isPreferred : " + profile.isPreferred(this.mDevice));
                if (profile.isPreferred(this.mDevice)) {
                    preferredProfiles++;
                    connectInt(profile);
                }
            }
        }
        if (preferredProfiles != 0) {
            return;
        }
        connectAutoConnectableProfiles();
    }

    private void connectAutoConnectableProfiles() {
        if (!ensurePaired()) {
            return;
        }
        this.mIsConnectingErrorPossible = true;
        for (LocalBluetoothProfile profile : this.mProfiles) {
            if (profile.isAutoConnectable()) {
                profile.setPreferred(this.mDevice, true);
                Log.d("CachedBluetoothDevice", describe(profile) + " setPreferred true and connect");
                connectInt(profile);
            }
        }
    }

    public void connectProfile(LocalBluetoothProfile profile) {
        this.mConnectAttempted = SystemClock.elapsedRealtime();
        this.mIsConnectingErrorPossible = true;
        connectInt(profile);
        refresh();
    }

    synchronized void connectInt(LocalBluetoothProfile profile) {
        if (!ensurePaired()) {
            return;
        }
        if (profile.connect(this.mDevice)) {
            Log.d("CachedBluetoothDevice", "Command sent successfully:CONNECT " + describe(profile));
        } else {
            Log.i("CachedBluetoothDevice", "Failed to connect " + profile.toString() + " to " + this.mName);
        }
    }

    private boolean ensurePaired() {
        if (getBondState() == 10) {
            startPairing();
            return false;
        }
        return true;
    }

    public boolean startPairing() {
        if (this.mLocalAdapter.isDiscovering()) {
            this.mLocalAdapter.cancelDiscovery();
        }
        if (!this.mDevice.createBond()) {
            return false;
        }
        this.mConnectAfterPairing = true;
        return true;
    }

    public void unpair() {
        BluetoothDevice dev;
        int state = getBondState();
        if (state == 11) {
            this.mDevice.cancelBondProcess();
        }
        if (state == 10 || (dev = this.mDevice) == null) {
            return;
        }
        boolean successful = dev.removeBond();
        if (!successful) {
            return;
        }
        Log.d("CachedBluetoothDevice", "Command sent successfully:REMOVE_BOND " + describe(null));
    }

    public int getProfileConnectionState(LocalBluetoothProfile profile) {
        if (this.mProfileConnectionState == null || this.mProfileConnectionState.get(profile) == null) {
            int state = profile.getConnectionStatus(this.mDevice);
            Log.d("CachedBluetoothDevice", describe(profile) + " state : " + state);
            this.mProfileConnectionState.put(profile, Integer.valueOf(state));
        }
        return this.mProfileConnectionState.get(profile).intValue();
    }

    public void clearProfileConnectionState() {
        Log.d("CachedBluetoothDevice", " Clearing all connection state for dev:" + this.mDevice.getName());
        for (LocalBluetoothProfile profile : getProfiles()) {
            this.mProfileConnectionState.put(profile, 0);
        }
    }

    private void fillData() {
        fetchName();
        fetchBtClass();
        updateProfiles();
        migratePhonebookPermissionChoice();
        migrateMessagePermissionChoice();
        fetchMessageRejectionCount();
        this.mVisible = false;
        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public String getName() {
        return this.mName;
    }

    void setNewName(String name) {
        if (this.mName != null) {
            return;
        }
        this.mName = name;
        if (this.mName == null || TextUtils.isEmpty(this.mName)) {
            this.mName = this.mDevice.getAddress();
        }
        dispatchAttributesChanged();
    }

    public void setName(String name) {
        if (this.mName.equals(name)) {
            return;
        }
        this.mName = name;
        this.mDevice.setAlias(name);
        dispatchAttributesChanged();
    }

    void refreshName() {
        fetchName();
        dispatchAttributesChanged();
    }

    private void fetchName() {
        this.mName = this.mDevice.getAliasName();
        Log.d("CachedBluetoothDevice", "fetchName, AlaisName is " + this.mName);
        if (TextUtils.isEmpty(this.mName)) {
            this.mName = this.mDevice.getAddress();
        }
        Log.d("CachedBluetoothDevice", "fetchName, Return Name " + this.mName);
    }

    void refresh() {
        dispatchAttributesChanged();
    }

    public void setVisible(boolean visible) {
        if (this.mVisible == visible) {
            return;
        }
        this.mVisible = visible;
        dispatchAttributesChanged();
    }

    public int getBondState() {
        return this.mDevice.getBondState();
    }

    void setRssi(short rssi) {
        if (this.mRssi == rssi) {
            return;
        }
        this.mRssi = rssi;
        dispatchAttributesChanged();
    }

    public boolean isConnected() {
        for (LocalBluetoothProfile profile : this.mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == 2) {
                return true;
            }
        }
        return false;
    }

    public boolean isBusy() {
        for (LocalBluetoothProfile profile : this.mProfiles) {
            int status = getProfileConnectionState(profile);
            if (status == 1 || status == 3) {
                return true;
            }
        }
        Log.d("CachedBluetoothDevice", this.mName + " bond state is " + getBondState());
        return getBondState() == 11;
    }

    private void fetchBtClass() {
        this.mBtClass = this.mDevice.getBluetoothClass();
        if (this.mBtClass == null) {
            Log.d("CachedBluetoothDevice", "fetchClass, mBtClass is null");
        } else {
            int Class = this.mBtClass.getMajorDeviceClass();
            Log.d("CachedBluetoothDevice", "fetchClass, mBtClass is " + Class);
        }
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids = this.mDevice.getUuids();
        if (uuids == null) {
            Log.d("CachedBluetoothDevice", "Bluetooth device get uuid is null");
            return false;
        }
        ParcelUuid[] localUuids = this.mLocalAdapter.getUuids();
        if (localUuids == null) {
            Log.d("CachedBluetoothDevice", "Bluetooth Adapter get uuid is null");
            return false;
        }
        processPhonebookAccess();
        Log.d("CachedBluetoothDevice", this.mName + " update profiles");
        this.mProfileManager.updateProfiles(uuids, localUuids, this.mProfiles, this.mRemovedProfiles, this.mLocalNapRoleConnected, this.mDevice);
        return true;
    }

    void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }

    void onUuidChanged() {
        updateProfiles();
        ParcelUuid[] uuids = this.mDevice.getUuids();
        long timeout = 5000;
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hogp)) {
            timeout = 30000;
        }
        if (!this.mProfiles.isEmpty() && this.mConnectAttempted + timeout > SystemClock.elapsedRealtime()) {
            connectWithoutResettingTimer(false);
        }
        dispatchAttributesChanged();
    }

    void onBondingStateChanged(int bondState) {
        Log.d("CachedBluetoothDevice", "onBondingStateChanged to " + bondState);
        if (bondState == 10) {
            this.mProfiles.clear();
            this.mConnectAfterPairing = false;
            setPhonebookPermissionChoice(0);
            setMessagePermissionChoice(0);
            setSimPermissionChoice(0);
            this.mMessageRejectionCount = 0;
            saveMessageRejectionCount();
        }
        refresh();
        if (bondState != 12) {
            return;
        }
        Log.d("CachedBluetoothDevice", "Bond state changed to bonded, mConnectAfterPairing is " + this.mConnectAfterPairing);
        if (this.mDevice.isBluetoothDock()) {
            onBondingDockConnect();
        } else if (this.mConnectAfterPairing) {
            connect(false);
        }
        this.mConnectAfterPairing = false;
    }

    void setBtClass(BluetoothClass btClass) {
        if (btClass == null || this.mBtClass == btClass) {
            return;
        }
        this.mBtClass = btClass;
        dispatchAttributesChanged();
    }

    public BluetoothClass getBtClass() {
        return this.mBtClass;
    }

    public List<LocalBluetoothProfile> getProfiles() {
        return Collections.unmodifiableList(this.mProfiles);
    }

    public List<LocalBluetoothProfile> getConnectableProfiles() {
        Log.d("CachedBluetoothDevice", this.mName + " mprofile size is " + this.mProfiles.size());
        List<LocalBluetoothProfile> connectableProfiles = new ArrayList<>();
        for (LocalBluetoothProfile profile : this.mProfiles) {
            if (profile.isConnectable()) {
                connectableProfiles.add(profile);
            }
        }
        Log.d("CachedBluetoothDevice", this.mName + " conectable profile size is " + connectableProfiles.size());
        return connectableProfiles;
    }

    public List<LocalBluetoothProfile> getRemovedProfiles() {
        return this.mRemovedProfiles;
    }

    public void registerCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(callback);
        }
    }

    private void dispatchAttributesChanged() {
        synchronized (this.mCallbacks) {
            for (Callback callback : this.mCallbacks) {
                callback.onDeviceAttributesChanged();
            }
        }
    }

    public String toString() {
        return this.mDevice.toString();
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CachedBluetoothDevice)) {
            return false;
        }
        return this.mDevice.equals(((CachedBluetoothDevice) o).mDevice);
    }

    public int hashCode() {
        return this.mDevice.getAddress().hashCode();
    }

    @Override
    public int compareTo(CachedBluetoothDevice another) {
        int comparison = (another.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (comparison != 0) {
            return comparison;
        }
        int comparison2 = (another.getBondState() == 12 ? 1 : 0) - (getBondState() == 12 ? 1 : 0);
        if (comparison2 != 0) {
            return comparison2;
        }
        int comparison3 = (another.mVisible ? 1 : 0) - (this.mVisible ? 1 : 0);
        if (comparison3 != 0) {
            return comparison3;
        }
        int comparison4 = another.mRssi - this.mRssi;
        return comparison4 != 0 ? comparison4 : this.mName.compareTo(another.mName);
    }

    public int getPhonebookPermissionChoice() {
        int permission = this.mDevice.getPhonebookAccessPermission();
        if (permission == 1) {
            return 1;
        }
        return permission == 2 ? 2 : 0;
    }

    public void setPhonebookPermissionChoice(int permissionChoice) {
        int permission = 0;
        if (permissionChoice == 1) {
            permission = 1;
        } else if (permissionChoice == 2) {
            permission = 2;
        }
        this.mDevice.setPhonebookAccessPermission(permission);
    }

    private void migratePhonebookPermissionChoice() {
        SharedPreferences preferences = this.mContext.getSharedPreferences("bluetooth_phonebook_permission", 0);
        if (!preferences.contains(this.mDevice.getAddress())) {
            return;
        }
        if (this.mDevice.getPhonebookAccessPermission() == 0) {
            int oldPermission = preferences.getInt(this.mDevice.getAddress(), 0);
            if (oldPermission == 1) {
                this.mDevice.setPhonebookAccessPermission(1);
            } else if (oldPermission == 2) {
                this.mDevice.setPhonebookAccessPermission(2);
            }
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(this.mDevice.getAddress());
        editor.commit();
    }

    public int getMessagePermissionChoice() {
        int permission = this.mDevice.getMessageAccessPermission();
        if (permission == 1) {
            return 1;
        }
        return permission == 2 ? 2 : 0;
    }

    public void setMessagePermissionChoice(int permissionChoice) {
        int permission = 0;
        if (permissionChoice == 1) {
            permission = 1;
        } else if (permissionChoice == 2) {
            permission = 2;
        }
        this.mDevice.setMessageAccessPermission(permission);
    }

    public int getSimPermissionChoice() {
        int permission = this.mDevice.getSimAccessPermission();
        if (permission == 1) {
            return 1;
        }
        return permission == 2 ? 2 : 0;
    }

    void setSimPermissionChoice(int permissionChoice) {
        int permission = 0;
        if (permissionChoice == 1) {
            permission = 1;
        } else if (permissionChoice == 2) {
            permission = 2;
        }
        this.mDevice.setSimAccessPermission(permission);
    }

    private void migrateMessagePermissionChoice() {
        SharedPreferences preferences = this.mContext.getSharedPreferences("bluetooth_message_permission", 0);
        if (!preferences.contains(this.mDevice.getAddress())) {
            return;
        }
        if (this.mDevice.getMessageAccessPermission() == 0) {
            int oldPermission = preferences.getInt(this.mDevice.getAddress(), 0);
            if (oldPermission == 1) {
                this.mDevice.setMessageAccessPermission(1);
            } else if (oldPermission == 2) {
                this.mDevice.setMessageAccessPermission(2);
            }
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(this.mDevice.getAddress());
        editor.commit();
    }

    public boolean checkAndIncreaseMessageRejectionCount() {
        if (this.mMessageRejectionCount < 2) {
            this.mMessageRejectionCount++;
            saveMessageRejectionCount();
        }
        return this.mMessageRejectionCount >= 2;
    }

    private void fetchMessageRejectionCount() {
        SharedPreferences preference = this.mContext.getSharedPreferences("bluetooth_message_reject", 0);
        this.mMessageRejectionCount = preference.getInt(this.mDevice.getAddress(), 0);
    }

    private void saveMessageRejectionCount() {
        SharedPreferences.Editor editor = this.mContext.getSharedPreferences("bluetooth_message_reject", 0).edit();
        if (this.mMessageRejectionCount == 0) {
            editor.remove(this.mDevice.getAddress());
        } else {
            editor.putInt(this.mDevice.getAddress(), this.mMessageRejectionCount);
        }
        editor.commit();
    }

    private void processPhonebookAccess() {
        if (this.mDevice.getBondState() != 12) {
            return;
        }
        ParcelUuid[] uuids = this.mDevice.getUuids();
        if (!BluetoothUuid.containsAnyUuid(uuids, PbapServerProfile.PBAB_CLIENT_UUIDS) || getPhonebookPermissionChoice() != 0) {
            return;
        }
        if (this.mDevice.getBluetoothClass().getDeviceClass() == 1032) {
            setPhonebookPermissionChoice(1);
        } else {
            setPhonebookPermissionChoice(2);
        }
    }

    public int getConnectionSummary() {
        Log.d("CachedBluetoothDevice", this.mName + " getConnectionSummary");
        boolean profileConnected = false;
        boolean a2dpNotConnected = false;
        boolean hfpNotConnected = false;
        for (LocalBluetoothProfile profile : getProfiles()) {
            int connectionStatus = getProfileConnectionState(profile);
            if (profile != null) {
                Log.d("CachedBluetoothDevice", "profile name is " + profile.toString() + ": " + connectionStatus);
            }
            switch (connectionStatus) {
                case DefaultWfcSettingsExt.RESUME:
                    if (profile.isProfileReady()) {
                        if ((profile instanceof A2dpProfile) || (profile instanceof A2dpSinkProfile)) {
                            a2dpNotConnected = true;
                            Log.d("CachedBluetoothDevice", "a2dpNotConnected = true");
                        } else if ((profile instanceof HeadsetProfile) || (profile instanceof HfpClientProfile)) {
                            Log.d("CachedBluetoothDevice", "hfpNotConnected = true");
                            hfpNotConnected = true;
                        }
                    }
                    break;
                case DefaultWfcSettingsExt.PAUSE:
                case DefaultWfcSettingsExt.DESTROY:
                    return Utils.getConnectionStateSummary(connectionStatus);
                case DefaultWfcSettingsExt.CREATE:
                    Log.d("CachedBluetoothDevice", "profileConnected = true");
                    profileConnected = true;
                    break;
            }
        }
        if (profileConnected) {
            if (a2dpNotConnected && hfpNotConnected) {
                return R$string.bluetooth_connected_no_headset_no_a2dp;
            }
            if (a2dpNotConnected) {
                return R$string.bluetooth_connected_no_a2dp;
            }
            if (hfpNotConnected) {
                return R$string.bluetooth_connected_no_headset;
            }
            return R$string.bluetooth_connected;
        }
        if (getBondState() == 11) {
            return R$string.bluetooth_pairing;
        }
        return 0;
    }
}
