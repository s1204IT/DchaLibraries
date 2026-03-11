package com.android.settingslib.bluetooth;

import android.R;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LocalBluetoothProfileManager {
    private A2dpProfile mA2dpProfile;
    private A2dpSinkProfile mA2dpSinkProfile;
    private final Context mContext;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final BluetoothEventManager mEventManager;
    private HeadsetProfile mHeadsetProfile;
    private HearingAidProfile mHearingAidProfile;
    private HfpClientProfile mHfpClientProfile;
    private HidDeviceProfile mHidDeviceProfile;
    private HidProfile mHidProfile;
    private final LocalBluetoothAdapter mLocalAdapter;
    private MapClientProfile mMapClientProfile;
    private MapProfile mMapProfile;
    private OppProfile mOppProfile;
    private PanProfile mPanProfile;
    private PbapClientProfile mPbapClientProfile;
    private PbapServerProfile mPbapProfile;
    private final Map<String, LocalBluetoothProfile> mProfileNameMap = new HashMap();
    private final Collection<ServiceListener> mServiceListeners = new ArrayList();
    private final boolean mUseMapClient;
    private final boolean mUsePbapPce;

    public interface ServiceListener {
        void onServiceConnected();

        void onServiceDisconnected();
    }

    LocalBluetoothProfileManager(Context context, LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, BluetoothEventManager bluetoothEventManager) {
        this.mContext = context;
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mEventManager = bluetoothEventManager;
        this.mUsePbapPce = this.mContext.getResources().getBoolean(R.^attr-private.searchDialogTheme);
        this.mUseMapClient = this.mContext.getResources().getBoolean(R.^attr-private.searchDialogTheme);
        this.mLocalAdapter.setProfileManager(this);
        this.mEventManager.setProfileManager(this);
        ParcelUuid[] uuids = localBluetoothAdapter.getUuids();
        if (uuids != null) {
            Log.d("LocalBluetoothProfileManager", "bluetooth adapter uuid: ");
            for (ParcelUuid parcelUuid : uuids) {
                Log.d("LocalBluetoothProfileManager", "  " + parcelUuid);
            }
            updateLocalProfiles(uuids);
        }
        if (this.mHidProfile == null) {
            this.mHidProfile = new HidProfile(context, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mHidProfile, "HID", "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mPanProfile == null) {
            this.mPanProfile = new PanProfile(context, this.mLocalAdapter);
            addPanProfile(this.mPanProfile, "PAN", "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mHidDeviceProfile == null) {
            this.mHidDeviceProfile = new HidDeviceProfile(context, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mHidDeviceProfile, "HID DEVICE", "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED");
        }
        Log.d("LocalBluetoothProfileManager", "Adding local MAP profile");
        if (this.mUseMapClient) {
            if (this.mMapClientProfile == null) {
                this.mMapClientProfile = new MapClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mMapClientProfile, "MAP Client", "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mMapClientProfile == null) {
            this.mMapProfile = new MapProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mMapProfile, "MAP", "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
        }
        Log.d("LocalBluetoothProfileManager", "Adding local PBAP profile");
        if (this.mPbapProfile == null) {
            this.mPbapProfile = new PbapServerProfile(context);
            addProfile(this.mPbapProfile, PbapServerProfile.NAME, "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mLocalAdapter.getSupportedProfiles().contains(21) && this.mHearingAidProfile == null) {
            this.mHearingAidProfile = new HearingAidProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mHearingAidProfile, "HearingAid", "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
        }
        Log.d("LocalBluetoothProfileManager", "LocalBluetoothProfileManager construction complete");
    }

    void updateLocalProfiles(ParcelUuid[] parcelUuidArr) {
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.AudioSource)) {
            if (this.mA2dpProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local A2DP SRC profile");
                this.mA2dpProfile = new A2dpProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mA2dpProfile, "A2DP", "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mA2dpProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: A2DP profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.AudioSink)) {
            if (this.mA2dpSinkProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local A2DP Sink profile");
                this.mA2dpSinkProfile = new A2dpSinkProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mA2dpSinkProfile, "A2DPSink", "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mA2dpSinkProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: A2DP Sink profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Handsfree_AG) || BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HSP_AG)) {
            if (this.mHeadsetProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local HEADSET profile");
                this.mHeadsetProfile = new HeadsetProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addHeadsetProfile(this.mHeadsetProfile, "HEADSET", "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED", "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED", 10);
            }
        } else if (this.mHeadsetProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: HEADSET profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Handsfree)) {
            if (this.mHfpClientProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local HfpClient profile");
                this.mHfpClientProfile = new HfpClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addHeadsetProfile(this.mHfpClientProfile, "HEADSET_CLIENT", "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED", "android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED", 0);
            }
        } else if (this.mHfpClientProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: Hfp Client profile was previously added but the UUID is now missing.");
        } else {
            Log.d("LocalBluetoothProfileManager", "Handsfree Uuid not found.");
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.MNS)) {
            if (this.mMapClientProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local Map Client profile");
                this.mMapClientProfile = new MapClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mMapClientProfile, "MAP Client", "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mMapClientProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: MAP Client profile was previously added but the UUID is now missing.");
        } else {
            Log.d("LocalBluetoothProfileManager", "MAP Client Uuid not found.");
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.ObexObjectPush)) {
            if (this.mOppProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local OPP profile");
                this.mOppProfile = new OppProfile();
                this.mProfileNameMap.put("OPP", this.mOppProfile);
            }
        } else if (this.mOppProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: OPP profile was previously added but the UUID is now missing.");
        }
        if (this.mUsePbapPce) {
            if (this.mPbapClientProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local PBAP Client profile");
                this.mPbapClientProfile = new PbapClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mPbapClientProfile, "PbapClient", "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mPbapClientProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: PBAP Client profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HearingAid)) {
            if (this.mHearingAidProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local Hearing Aid profile");
                this.mHearingAidProfile = new HearingAidProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mHearingAidProfile, "HearingAid", "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mHearingAidProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: Hearing Aid profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hid) || BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hogp)) {
            if (this.mHidProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local Hid profile");
                this.mHidProfile = new HidProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mHidProfile, "HID", "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mHidProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: Hid profile was previously added but the UUID is now missing.");
        }
        this.mEventManager.registerProfileIntentReceiver();
    }

    private void addHeadsetProfile(LocalBluetoothProfile localBluetoothProfile, String str, String str2, String str3, int i) {
        HeadsetStateChangeHandler headsetStateChangeHandler = new HeadsetStateChangeHandler(localBluetoothProfile, str3, i);
        this.mEventManager.addProfileHandler(str2, headsetStateChangeHandler);
        this.mEventManager.addProfileHandler(str3, headsetStateChangeHandler);
        this.mProfileNameMap.put(str, localBluetoothProfile);
    }

    private void addProfile(LocalBluetoothProfile localBluetoothProfile, String str, String str2) {
        this.mEventManager.addProfileHandler(str2, new StateChangedHandler(localBluetoothProfile));
        this.mProfileNameMap.put(str, localBluetoothProfile);
    }

    private void addPanProfile(LocalBluetoothProfile localBluetoothProfile, String str, String str2) {
        this.mEventManager.addProfileHandler(str2, new PanStateChangedHandler(localBluetoothProfile));
        this.mProfileNameMap.put(str, localBluetoothProfile);
    }

    void setBluetoothStateOn() {
        if (this.mHidProfile == null) {
            this.mHidProfile = new HidProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mHidProfile, "HID", "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mPanProfile == null) {
            this.mPanProfile = new PanProfile(this.mContext, this.mLocalAdapter);
            addPanProfile(this.mPanProfile, "PAN", "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mHidDeviceProfile == null) {
            this.mHidDeviceProfile = new HidDeviceProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mHidDeviceProfile, "HID DEVICE", "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mMapProfile == null) {
            Log.d("LocalBluetoothProfileManager", "Adding local MAP profile");
            if (this.mUseMapClient) {
                this.mMapClientProfile = new MapClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mMapClientProfile, "MAP Client", "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
            } else {
                this.mMapProfile = new MapProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mMapProfile, "MAP", "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
            }
        }
        if (this.mPbapProfile == null) {
            Log.d("LocalBluetoothProfileManager", "Adding local PBAP profile");
            this.mPbapProfile = new PbapServerProfile(this.mContext);
            addProfile(this.mPbapProfile, PbapServerProfile.NAME, "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mHearingAidProfile == null && this.mLocalAdapter.getSupportedProfiles().contains(21)) {
            this.mHearingAidProfile = new HearingAidProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mHearingAidProfile, "HearingAid", "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
        }
        ParcelUuid[] uuids = this.mLocalAdapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }
        this.mEventManager.readPairedDevices();
    }

    private class StateChangedHandler implements BluetoothEventManager.Handler {
        final LocalBluetoothProfile mProfile;

        StateChangedHandler(LocalBluetoothProfile localBluetoothProfile) {
            this.mProfile = localBluetoothProfile;
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            CachedBluetoothDevice cachedBluetoothDeviceFindDevice = LocalBluetoothProfileManager.this.mDeviceManager.findDevice(bluetoothDevice);
            if (cachedBluetoothDeviceFindDevice == null) {
                Log.w("LocalBluetoothProfileManager", "StateChangedHandler found new device: " + bluetoothDevice);
                cachedBluetoothDeviceFindDevice = LocalBluetoothProfileManager.this.mDeviceManager.addDevice(LocalBluetoothProfileManager.this.mLocalAdapter, LocalBluetoothProfileManager.this, bluetoothDevice);
            }
            onReceiveInternal(intent, cachedBluetoothDeviceFindDevice);
        }

        protected void onReceiveInternal(Intent intent, CachedBluetoothDevice cachedBluetoothDevice) {
            int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
            int intExtra2 = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0);
            if (intExtra == 0 && intExtra2 == 1) {
                Log.i("LocalBluetoothProfileManager", "Failed to connect " + this.mProfile + " device");
            }
            if (LocalBluetoothProfileManager.this.getHearingAidProfile() != null && (this.mProfile instanceof HearingAidProfile) && intExtra == 2 && cachedBluetoothDevice.getHiSyncId() == 0) {
                long hiSyncId = LocalBluetoothProfileManager.this.getHearingAidProfile().getHiSyncId(cachedBluetoothDevice.getDevice());
                if (hiSyncId != 0) {
                    cachedBluetoothDevice.setHiSyncId(hiSyncId);
                    LocalBluetoothProfileManager.this.mDeviceManager.onHiSyncIdChanged(hiSyncId);
                }
            }
            LocalBluetoothProfileManager.this.mEventManager.dispatchProfileConnectionStateChanged(cachedBluetoothDevice, intExtra, this.mProfile.getProfileId());
            cachedBluetoothDevice.onProfileStateChanged(this.mProfile, intExtra);
            cachedBluetoothDevice.refresh();
        }
    }

    private class HeadsetStateChangeHandler extends StateChangedHandler {
        private final String mAudioChangeAction;
        private final int mAudioDisconnectedState;

        HeadsetStateChangeHandler(LocalBluetoothProfile localBluetoothProfile, String str, int i) {
            super(localBluetoothProfile);
            this.mAudioChangeAction = str;
            this.mAudioDisconnectedState = i;
        }

        @Override
        public void onReceiveInternal(Intent intent, CachedBluetoothDevice cachedBluetoothDevice) {
            if (this.mAudioChangeAction.equals(intent.getAction())) {
                if (intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0) != this.mAudioDisconnectedState) {
                    cachedBluetoothDevice.onProfileStateChanged(this.mProfile, 2);
                }
                cachedBluetoothDevice.refresh();
                return;
            }
            super.onReceiveInternal(intent, cachedBluetoothDevice);
        }
    }

    private class PanStateChangedHandler extends StateChangedHandler {
        PanStateChangedHandler(LocalBluetoothProfile localBluetoothProfile) {
            super(localBluetoothProfile);
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            PanProfile panProfile = (PanProfile) this.mProfile;
            int intExtra = intent.getIntExtra("android.bluetooth.pan.extra.LOCAL_ROLE", 0);
            panProfile.setLocalRole(bluetoothDevice, intExtra);
            Log.d("LocalBluetoothProfileManager", "pan profile state change, role is " + intExtra);
            super.onReceive(context, intent, bluetoothDevice);
        }
    }

    public void addServiceListener(ServiceListener serviceListener) {
        this.mServiceListeners.add(serviceListener);
    }

    void callServiceConnectedListeners() {
        Iterator<ServiceListener> it = this.mServiceListeners.iterator();
        while (it.hasNext()) {
            it.next().onServiceConnected();
        }
    }

    void callServiceDisconnectedListeners() {
        Iterator<ServiceListener> it = this.mServiceListeners.iterator();
        while (it.hasNext()) {
            it.next().onServiceDisconnected();
        }
    }

    public A2dpProfile getA2dpProfile() {
        return this.mA2dpProfile;
    }

    public A2dpSinkProfile getA2dpSinkProfile() {
        if (this.mA2dpSinkProfile != null && this.mA2dpSinkProfile.isProfileReady()) {
            return this.mA2dpSinkProfile;
        }
        return null;
    }

    public HeadsetProfile getHeadsetProfile() {
        return this.mHeadsetProfile;
    }

    public PbapServerProfile getPbapProfile() {
        return this.mPbapProfile;
    }

    public HearingAidProfile getHearingAidProfile() {
        return this.mHearingAidProfile;
    }

    HidProfile getHidProfile() {
        return this.mHidProfile;
    }

    HidDeviceProfile getHidDeviceProfile() {
        return this.mHidDeviceProfile;
    }

    synchronized void updateProfiles(ParcelUuid[] parcelUuidArr, ParcelUuid[] parcelUuidArr2, Collection<LocalBluetoothProfile> collection, Collection<LocalBluetoothProfile> collection2, boolean z, BluetoothDevice bluetoothDevice) {
        collection2.clear();
        collection2.addAll(collection);
        Log.d("LocalBluetoothProfileManager", "Current Profiles" + collection.toString());
        collection.clear();
        Log.d("LocalBluetoothProfileManager", "update profiles");
        if (parcelUuidArr == null) {
            Log.d("LocalBluetoothProfileManager", "remote device uuid is null");
            return;
        }
        if (this.mHeadsetProfile != null && ((BluetoothUuid.isUuidPresent(parcelUuidArr2, BluetoothUuid.HSP_AG) && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HSP)) || (BluetoothUuid.isUuidPresent(parcelUuidArr2, BluetoothUuid.Handsfree_AG) && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Handsfree)))) {
            Log.d("LocalBluetoothProfileManager", "Add HeadsetProfile to connectable profile list");
            collection.add(this.mHeadsetProfile);
            collection2.remove(this.mHeadsetProfile);
        }
        if (this.mHfpClientProfile != null && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Handsfree_AG) && BluetoothUuid.isUuidPresent(parcelUuidArr2, BluetoothUuid.Handsfree)) {
            collection.add(this.mHfpClientProfile);
            collection2.remove(this.mHfpClientProfile);
        }
        if (BluetoothUuid.containsAnyUuid(parcelUuidArr, A2dpProfile.SINK_UUIDS) && this.mA2dpProfile != null) {
            Log.d("LocalBluetoothProfileManager", "Add A2dpProfile to connectable profile list");
            collection.add(this.mA2dpProfile);
            collection2.remove(this.mA2dpProfile);
        }
        if (BluetoothUuid.containsAnyUuid(parcelUuidArr, A2dpSinkProfile.SRC_UUIDS) && this.mA2dpSinkProfile != null) {
            collection.add(this.mA2dpSinkProfile);
            collection2.remove(this.mA2dpSinkProfile);
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.ObexObjectPush) && this.mOppProfile != null) {
            Log.d("LocalBluetoothProfileManager", "Add OppProfile to connectable profile list");
            collection.add(this.mOppProfile);
            collection2.remove(this.mOppProfile);
        }
        if ((BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hid) || BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hogp)) && this.mHidProfile != null) {
            Log.d("LocalBluetoothProfileManager", "Add HidProfile to connectable profile list");
            collection.add(this.mHidProfile);
            collection2.remove(this.mHidProfile);
        }
        if (this.mHidDeviceProfile != null && this.mHidDeviceProfile.getConnectionStatus(bluetoothDevice) != 0) {
            collection.add(this.mHidDeviceProfile);
            collection2.remove(this.mHidDeviceProfile);
        }
        if (z) {
            Log.d("LocalBluetoothProfileManager", "Valid PAN-NAP connection exists.");
        }
        if ((BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.NAP) && this.mPanProfile != null) || z) {
            Log.d("LocalBluetoothProfileManager", "Add PanProfile to connectable profile list");
            collection.add(this.mPanProfile);
            collection2.remove(this.mPanProfile);
        }
        if (this.mMapProfile != null && this.mMapProfile.getConnectionStatus(bluetoothDevice) == 2) {
            Log.d("LocalBluetoothProfileManager", "Add MapProfile to connectable profile list");
            collection.add(this.mMapProfile);
            collection2.remove(this.mMapProfile);
            this.mMapProfile.setPreferred(bluetoothDevice, true);
        }
        if (this.mPbapProfile != null && this.mPbapProfile.getConnectionStatus(bluetoothDevice) == 2) {
            collection.add(this.mPbapProfile);
            collection2.remove(this.mPbapProfile);
            this.mPbapProfile.setPreferred(bluetoothDevice, true);
        }
        if (this.mMapClientProfile != null) {
            collection.add(this.mMapClientProfile);
            collection2.remove(this.mMapClientProfile);
        }
        if (this.mUsePbapPce) {
            collection.add(this.mPbapClientProfile);
            collection2.remove(this.mPbapClientProfile);
        }
        if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HearingAid) && this.mHearingAidProfile != null) {
            collection.add(this.mHearingAidProfile);
            collection2.remove(this.mHearingAidProfile);
        }
        Log.d("LocalBluetoothProfileManager", "New Profiles" + collection.toString());
    }
}
