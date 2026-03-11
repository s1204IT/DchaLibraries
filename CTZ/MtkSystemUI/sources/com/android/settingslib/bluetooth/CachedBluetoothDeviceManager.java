package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class CachedBluetoothDeviceManager {
    private final LocalBluetoothManager mBtManager;
    private Context mContext;

    @VisibleForTesting
    final List<CachedBluetoothDevice> mCachedDevices = new ArrayList();

    @VisibleForTesting
    final List<CachedBluetoothDevice> mHearingAidDevicesNotAddedInCache = new ArrayList();

    @VisibleForTesting
    final Map<Long, CachedBluetoothDevice> mCachedDevicesMapForHearingAids = new HashMap();

    CachedBluetoothDeviceManager(Context context, LocalBluetoothManager localBluetoothManager) {
        this.mContext = context;
        this.mBtManager = localBluetoothManager;
    }

    public synchronized Collection<CachedBluetoothDevice> getCachedDevicesCopy() {
        return new ArrayList(this.mCachedDevices);
    }

    public static boolean onDeviceDisappeared(CachedBluetoothDevice cachedBluetoothDevice) {
        cachedBluetoothDevice.setJustDiscovered(false);
        return cachedBluetoothDevice.getBondState() == 10;
    }

    public void onDeviceNameUpdated(BluetoothDevice bluetoothDevice) {
        CachedBluetoothDevice cachedBluetoothDeviceFindDevice = findDevice(bluetoothDevice);
        if (cachedBluetoothDeviceFindDevice != null) {
            cachedBluetoothDeviceFindDevice.refreshName();
        }
    }

    public synchronized CachedBluetoothDevice findDevice(BluetoothDevice bluetoothDevice) {
        for (CachedBluetoothDevice cachedBluetoothDevice : this.mCachedDevices) {
            if (cachedBluetoothDevice.getDevice().equals(bluetoothDevice)) {
                return cachedBluetoothDevice;
            }
        }
        for (CachedBluetoothDevice cachedBluetoothDevice2 : this.mHearingAidDevicesNotAddedInCache) {
            if (cachedBluetoothDevice2.getDevice().equals(bluetoothDevice)) {
                return cachedBluetoothDevice2;
            }
        }
        return null;
    }

    public CachedBluetoothDevice addDevice(LocalBluetoothAdapter localBluetoothAdapter, LocalBluetoothProfileManager localBluetoothProfileManager, BluetoothDevice bluetoothDevice) {
        CachedBluetoothDevice cachedBluetoothDevice = new CachedBluetoothDevice(this.mContext, localBluetoothAdapter, localBluetoothProfileManager, bluetoothDevice);
        if (localBluetoothProfileManager.getHearingAidProfile() != null && localBluetoothProfileManager.getHearingAidProfile().getHiSyncId(cachedBluetoothDevice.getDevice()) != 0) {
            cachedBluetoothDevice.setHiSyncId(localBluetoothProfileManager.getHearingAidProfile().getHiSyncId(cachedBluetoothDevice.getDevice()));
        }
        if (isPairAddedInCache(cachedBluetoothDevice.getHiSyncId())) {
            synchronized (this) {
                this.mHearingAidDevicesNotAddedInCache.add(cachedBluetoothDevice);
            }
        } else {
            synchronized (this) {
                this.mCachedDevices.add(cachedBluetoothDevice);
                if (cachedBluetoothDevice.getHiSyncId() != 0 && !this.mCachedDevicesMapForHearingAids.containsKey(Long.valueOf(cachedBluetoothDevice.getHiSyncId()))) {
                    this.mCachedDevicesMapForHearingAids.put(Long.valueOf(cachedBluetoothDevice.getHiSyncId()), cachedBluetoothDevice);
                }
                this.mBtManager.getEventManager().dispatchDeviceAdded(cachedBluetoothDevice);
            }
        }
        return cachedBluetoothDevice;
    }

    private synchronized boolean isPairAddedInCache(long j) {
        if (j == 0) {
            return false;
        }
        return this.mCachedDevicesMapForHearingAids.containsKey(Long.valueOf(j));
    }

    public synchronized void updateHearingAidsDevices(LocalBluetoothProfileManager localBluetoothProfileManager) {
        HearingAidProfile hearingAidProfile = localBluetoothProfileManager.getHearingAidProfile();
        if (hearingAidProfile == null) {
            log("updateHearingAidsDevices: getHearingAidProfile() is null");
            return;
        }
        HashSet hashSet = new HashSet();
        for (CachedBluetoothDevice cachedBluetoothDevice : this.mCachedDevices) {
            if (cachedBluetoothDevice.getHiSyncId() == 0) {
                long hiSyncId = hearingAidProfile.getHiSyncId(cachedBluetoothDevice.getDevice());
                if (hiSyncId != 0) {
                    cachedBluetoothDevice.setHiSyncId(hiSyncId);
                    hashSet.add(Long.valueOf(hiSyncId));
                }
            }
        }
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            onHiSyncIdChanged(((Long) it.next()).longValue());
        }
    }

    public synchronized void clearNonBondedDevices() {
        this.mCachedDevicesMapForHearingAids.entrySet().removeIf(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CachedBluetoothDeviceManager.lambda$clearNonBondedDevices$0((Map.Entry) obj);
            }
        });
        this.mCachedDevices.removeIf(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CachedBluetoothDeviceManager.lambda$clearNonBondedDevices$1((CachedBluetoothDevice) obj);
            }
        });
        this.mHearingAidDevicesNotAddedInCache.removeIf(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CachedBluetoothDeviceManager.lambda$clearNonBondedDevices$2((CachedBluetoothDevice) obj);
            }
        });
    }

    static boolean lambda$clearNonBondedDevices$0(Map.Entry entry) {
        return ((CachedBluetoothDevice) entry.getValue()).getBondState() == 10;
    }

    static boolean lambda$clearNonBondedDevices$1(CachedBluetoothDevice cachedBluetoothDevice) {
        return cachedBluetoothDevice.getBondState() == 10;
    }

    static boolean lambda$clearNonBondedDevices$2(CachedBluetoothDevice cachedBluetoothDevice) {
        return cachedBluetoothDevice.getBondState() == 10;
    }

    public synchronized void onScanningStateChanged(boolean z) {
        if (z) {
            for (int size = this.mCachedDevices.size() - 1; size >= 0; size--) {
                this.mCachedDevices.get(size).setJustDiscovered(false);
            }
            for (int size2 = this.mHearingAidDevicesNotAddedInCache.size() - 1; size2 >= 0; size2--) {
                this.mHearingAidDevicesNotAddedInCache.get(size2).setJustDiscovered(false);
            }
        }
    }

    public synchronized void onBtClassChanged(BluetoothDevice bluetoothDevice) {
        CachedBluetoothDevice cachedBluetoothDeviceFindDevice = findDevice(bluetoothDevice);
        if (cachedBluetoothDeviceFindDevice != null) {
            cachedBluetoothDeviceFindDevice.refreshBtClass();
        }
    }

    public synchronized void onUuidChanged(BluetoothDevice bluetoothDevice) {
        CachedBluetoothDevice cachedBluetoothDeviceFindDevice = findDevice(bluetoothDevice);
        if (cachedBluetoothDeviceFindDevice != null) {
            cachedBluetoothDeviceFindDevice.onUuidChanged();
        }
    }

    public synchronized void onBluetoothStateChanged(int i) {
        if (i == 13) {
            for (int size = this.mCachedDevices.size() - 1; size >= 0; size--) {
                CachedBluetoothDevice cachedBluetoothDevice = this.mCachedDevices.get(size);
                if (cachedBluetoothDevice.getBondState() != 12) {
                    Log.d("CachedBluetoothDeviceManager", "Remove device for bond state : " + cachedBluetoothDevice.getBondState() + "     and device name is : " + cachedBluetoothDevice.getName());
                    cachedBluetoothDevice.setJustDiscovered(false);
                    this.mCachedDevices.remove(size);
                    if (cachedBluetoothDevice.getHiSyncId() != 0 && this.mCachedDevicesMapForHearingAids.containsKey(Long.valueOf(cachedBluetoothDevice.getHiSyncId()))) {
                        this.mCachedDevicesMapForHearingAids.remove(Long.valueOf(cachedBluetoothDevice.getHiSyncId()));
                    }
                } else {
                    cachedBluetoothDevice.clearProfileConnectionState();
                }
            }
            for (int size2 = this.mHearingAidDevicesNotAddedInCache.size() - 1; size2 >= 0; size2--) {
                CachedBluetoothDevice cachedBluetoothDevice2 = this.mHearingAidDevicesNotAddedInCache.get(size2);
                if (cachedBluetoothDevice2.getBondState() != 12) {
                    cachedBluetoothDevice2.setJustDiscovered(false);
                    this.mHearingAidDevicesNotAddedInCache.remove(size2);
                } else {
                    cachedBluetoothDevice2.clearProfileConnectionState();
                }
            }
        }
    }

    public synchronized void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        for (CachedBluetoothDevice cachedBluetoothDevice2 : this.mCachedDevices) {
            cachedBluetoothDevice2.onActiveDeviceChanged(Objects.equals(cachedBluetoothDevice2, cachedBluetoothDevice), i);
        }
    }

    public synchronized void onHiSyncIdChanged(long j) {
        int size = this.mCachedDevices.size() - 1;
        int i = -1;
        while (true) {
            if (size < 0) {
                break;
            }
            CachedBluetoothDevice cachedBluetoothDevice = this.mCachedDevices.get(size);
            if (cachedBluetoothDevice.getHiSyncId() == j) {
                if (i != -1) {
                    break;
                }
                this.mCachedDevicesMapForHearingAids.put(Long.valueOf(j), cachedBluetoothDevice);
                i = size;
            }
            size--;
        }
    }

    private CachedBluetoothDevice getHearingAidOtherDevice(CachedBluetoothDevice cachedBluetoothDevice, long j) {
        if (j == 0) {
            return null;
        }
        for (CachedBluetoothDevice cachedBluetoothDevice2 : this.mHearingAidDevicesNotAddedInCache) {
            if (j == cachedBluetoothDevice2.getHiSyncId() && !Objects.equals(cachedBluetoothDevice2, cachedBluetoothDevice)) {
                return cachedBluetoothDevice2;
            }
        }
        CachedBluetoothDevice cachedBluetoothDevice3 = this.mCachedDevicesMapForHearingAids.get(Long.valueOf(j));
        if (Objects.equals(cachedBluetoothDevice3, cachedBluetoothDevice)) {
            return null;
        }
        return cachedBluetoothDevice3;
    }

    private void hearingAidSwitchDisplayDevice(CachedBluetoothDevice cachedBluetoothDevice, CachedBluetoothDevice cachedBluetoothDevice2, long j) {
        log("hearingAidSwitchDisplayDevice: toDisplayDevice=" + cachedBluetoothDevice + ", toHideDevice=" + cachedBluetoothDevice2);
        this.mHearingAidDevicesNotAddedInCache.add(cachedBluetoothDevice2);
        this.mCachedDevices.remove(cachedBluetoothDevice2);
        this.mBtManager.getEventManager().dispatchDeviceRemoved(cachedBluetoothDevice2);
        this.mHearingAidDevicesNotAddedInCache.remove(cachedBluetoothDevice);
        this.mCachedDevices.add(cachedBluetoothDevice);
        this.mCachedDevicesMapForHearingAids.put(Long.valueOf(j), cachedBluetoothDevice);
        this.mBtManager.getEventManager().dispatchDeviceAdded(cachedBluetoothDevice);
    }

    public synchronized void onProfileConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i, int i2) {
        CachedBluetoothDevice cachedBluetoothDevice2;
        if (i2 == 21) {
            if (cachedBluetoothDevice.getHiSyncId() != 0 && cachedBluetoothDevice.getBondState() == 12) {
                long hiSyncId = cachedBluetoothDevice.getHiSyncId();
                CachedBluetoothDevice hearingAidOtherDevice = getHearingAidOtherDevice(cachedBluetoothDevice, hiSyncId);
                if (hearingAidOtherDevice == null) {
                    return;
                }
                if (i == 2 && this.mHearingAidDevicesNotAddedInCache.contains(cachedBluetoothDevice)) {
                    hearingAidSwitchDisplayDevice(cachedBluetoothDevice, hearingAidOtherDevice, hiSyncId);
                } else if (i == 0 && hearingAidOtherDevice.isConnected() && (cachedBluetoothDevice2 = this.mCachedDevicesMapForHearingAids.get(Long.valueOf(hiSyncId))) != null && Objects.equals(cachedBluetoothDevice, cachedBluetoothDevice2)) {
                    hearingAidSwitchDisplayDevice(hearingAidOtherDevice, cachedBluetoothDevice, hiSyncId);
                }
            }
        }
    }

    public synchronized void onDeviceUnpaired(CachedBluetoothDevice cachedBluetoothDevice) {
        long hiSyncId = cachedBluetoothDevice.getHiSyncId();
        if (hiSyncId == 0) {
            return;
        }
        for (int size = this.mHearingAidDevicesNotAddedInCache.size() - 1; size >= 0; size--) {
            CachedBluetoothDevice cachedBluetoothDevice2 = this.mHearingAidDevicesNotAddedInCache.get(size);
            if (cachedBluetoothDevice2.getHiSyncId() == hiSyncId) {
                this.mHearingAidDevicesNotAddedInCache.remove(size);
                if (cachedBluetoothDevice != cachedBluetoothDevice2) {
                    log("onDeviceUnpaired: Unpair device=" + cachedBluetoothDevice2);
                    cachedBluetoothDevice2.unpair();
                }
            }
        }
        CachedBluetoothDevice cachedBluetoothDevice3 = this.mCachedDevicesMapForHearingAids.get(Long.valueOf(hiSyncId));
        if (cachedBluetoothDevice3 != null && !Objects.equals(cachedBluetoothDevice, cachedBluetoothDevice3)) {
            log("onDeviceUnpaired: Unpair mapped device=" + cachedBluetoothDevice3);
            cachedBluetoothDevice3.unpair();
        }
    }

    public synchronized void dispatchAudioModeChanged() {
        Iterator<CachedBluetoothDevice> it = this.mCachedDevices.iterator();
        while (it.hasNext()) {
            it.next().onAudioModeChanged();
        }
    }

    private void log(String str) {
        Log.d("CachedBluetoothDeviceManager", str);
    }
}
