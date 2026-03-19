package com.android.bluetooth.gatt;

import android.bluetooth.le.ResultStorageDescriptor;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Binder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

class ScanClient {
    private static final ScanSettings DEFAULT_SCAN_SETTINGS = new ScanSettings.Builder().setScanMode(2).build();
    public boolean appDied;
    public int appUid;
    public List<ScanFilter> filters;
    public boolean hasLocationPermission;
    public boolean hasPeersMacAddressPermission;
    public boolean legacyForegroundApp;
    public ScanSettings passiveSettings;
    public int scannerId;
    public ScanSettings settings;
    public AppScanStats stats;
    public List<List<ResultStorageDescriptor>> storages;
    public UUID[] uuids;

    ScanClient(int i) {
        this(i, new UUID[0], DEFAULT_SCAN_SETTINGS, null, null);
    }

    ScanClient(int i, UUID[] uuidArr) {
        this(i, uuidArr, DEFAULT_SCAN_SETTINGS, null, null);
    }

    ScanClient(int i, ScanSettings scanSettings, List<ScanFilter> list) {
        this(i, new UUID[0], scanSettings, list, null);
    }

    ScanClient(int i, ScanSettings scanSettings, List<ScanFilter> list, List<List<ResultStorageDescriptor>> list2) {
        this(i, new UUID[0], scanSettings, list, list2);
    }

    private ScanClient(int i, UUID[] uuidArr, ScanSettings scanSettings, List<ScanFilter> list, List<List<ResultStorageDescriptor>> list2) {
        this.stats = null;
        this.scannerId = i;
        this.uuids = uuidArr;
        this.settings = scanSettings;
        this.passiveSettings = null;
        this.filters = list;
        this.storages = list2;
        this.appUid = Binder.getCallingUid();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.scannerId == ((ScanClient) obj).scannerId) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.scannerId));
    }
}
