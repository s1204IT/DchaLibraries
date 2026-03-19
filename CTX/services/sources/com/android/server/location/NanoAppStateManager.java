package com.android.server.location;

import android.hardware.contexthub.V1_0.HubAppInfo;
import android.hardware.location.NanoAppInstanceInfo;
import android.util.Log;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

class NanoAppStateManager {
    private static final boolean ENABLE_LOG_DEBUG = true;
    private static final String TAG = "NanoAppStateManager";
    private final HashMap<Integer, NanoAppInstanceInfo> mNanoAppHash = new HashMap<>();
    private int mNextHandle = 0;

    NanoAppStateManager() {
    }

    synchronized NanoAppInstanceInfo getNanoAppInstanceInfo(int i) {
        return this.mNanoAppHash.get(Integer.valueOf(i));
    }

    synchronized Collection<NanoAppInstanceInfo> getNanoAppInstanceInfoCollection() {
        return this.mNanoAppHash.values();
    }

    synchronized int getNanoAppHandle(int i, long j) {
        for (NanoAppInstanceInfo nanoAppInstanceInfo : this.mNanoAppHash.values()) {
            if (nanoAppInstanceInfo.getContexthubId() == i && nanoAppInstanceInfo.getAppId() == j) {
                return nanoAppInstanceInfo.getHandle();
            }
        }
        return -1;
    }

    synchronized void addNanoAppInstance(int i, long j, int i2) {
        removeNanoAppInstance(i, j);
        if (this.mNanoAppHash.size() == Integer.MAX_VALUE) {
            Log.e(TAG, "Error adding nanoapp instance: max limit exceeded");
            return;
        }
        int i3 = 0;
        int i4 = this.mNextHandle;
        int i5 = 0;
        while (true) {
            if (i5 > Integer.MAX_VALUE) {
                break;
            }
            if (!this.mNanoAppHash.containsKey(Integer.valueOf(i4))) {
                this.mNanoAppHash.put(Integer.valueOf(i4), new NanoAppInstanceInfo(i4, j, i2, i));
                if (i4 != Integer.MAX_VALUE) {
                    i3 = i4 + 1;
                }
                this.mNextHandle = i3;
            } else {
                if (i4 == Integer.MAX_VALUE) {
                    i4 = 0;
                } else {
                    i4++;
                }
                i5++;
            }
        }
        Log.v(TAG, "Added app instance with handle " + i4 + " to hub " + i + ": ID=0x" + Long.toHexString(j) + ", version=0x" + Integer.toHexString(i2));
    }

    synchronized void removeNanoAppInstance(int i, long j) {
        this.mNanoAppHash.remove(Integer.valueOf(getNanoAppHandle(i, j)));
    }

    synchronized void updateCache(int i, List<HubAppInfo> list) {
        HashSet hashSet = new HashSet();
        for (HubAppInfo hubAppInfo : list) {
            handleQueryAppEntry(i, hubAppInfo.appId, hubAppInfo.version);
            hashSet.add(Long.valueOf(hubAppInfo.appId));
        }
        Iterator<NanoAppInstanceInfo> it = this.mNanoAppHash.values().iterator();
        while (it.hasNext()) {
            NanoAppInstanceInfo next = it.next();
            if (next.getContexthubId() == i && !hashSet.contains(Long.valueOf(next.getAppId()))) {
                it.remove();
            }
        }
    }

    private void handleQueryAppEntry(int i, long j, int i2) {
        int nanoAppHandle = getNanoAppHandle(i, j);
        if (nanoAppHandle == -1) {
            addNanoAppInstance(i, j, i2);
            return;
        }
        if (this.mNanoAppHash.get(Integer.valueOf(nanoAppHandle)).getAppVersion() != i2) {
            this.mNanoAppHash.put(Integer.valueOf(nanoAppHandle), new NanoAppInstanceInfo(nanoAppHandle, j, i2, i));
            Log.v(TAG, "Updated app instance with handle " + nanoAppHandle + " at hub " + i + ": ID=0x" + Long.toHexString(j) + ", version=0x" + Integer.toHexString(i2));
        }
    }
}
