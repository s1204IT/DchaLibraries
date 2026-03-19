package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import com.android.server.wifi.hotspot2.AnqpCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class ScanDetailCache {
    private static final boolean DBG = false;
    private static final String TAG = "ScanDetailCache";
    private final WifiConfiguration mConfig;
    private final HashMap<String, ScanDetail> mMap = new HashMap<>(16, 0.75f);
    private final int mMaxSize;
    private final int mTrimSize;

    ScanDetailCache(WifiConfiguration wifiConfiguration, int i, int i2) {
        this.mConfig = wifiConfiguration;
        this.mMaxSize = i;
        this.mTrimSize = i2;
    }

    void put(ScanDetail scanDetail) {
        if (this.mMap.size() >= this.mMaxSize) {
            trim();
        }
        this.mMap.put(scanDetail.getBSSIDString(), scanDetail);
    }

    public ScanResult getScanResult(String str) {
        ScanDetail scanDetail = getScanDetail(str);
        if (scanDetail == null) {
            return null;
        }
        return scanDetail.getScanResult();
    }

    public ScanDetail getScanDetail(String str) {
        return this.mMap.get(str);
    }

    void remove(String str) {
        this.mMap.remove(str);
    }

    int size() {
        return this.mMap.size();
    }

    boolean isEmpty() {
        if (size() == 0) {
            return true;
        }
        return DBG;
    }

    Collection<String> keySet() {
        return this.mMap.keySet();
    }

    Collection<ScanDetail> values() {
        return this.mMap.values();
    }

    private void trim() {
        int size = this.mMap.size();
        if (size < this.mTrimSize) {
            return;
        }
        ArrayList arrayList = new ArrayList(this.mMap.values());
        if (arrayList.size() != 0) {
            Collections.sort(arrayList, new Comparator() {
                @Override
                public int compare(Object obj, Object obj2) {
                    ScanDetail scanDetail = (ScanDetail) obj;
                    ScanDetail scanDetail2 = (ScanDetail) obj2;
                    if (scanDetail.getSeen() > scanDetail2.getSeen()) {
                        return 1;
                    }
                    if (scanDetail.getSeen() < scanDetail2.getSeen()) {
                        return -1;
                    }
                    return scanDetail.getBSSIDString().compareTo(scanDetail2.getBSSIDString());
                }
            });
        }
        for (int i = 0; i < size - this.mTrimSize; i++) {
            this.mMap.remove(((ScanDetail) arrayList.get(i)).getBSSIDString());
        }
    }

    private ArrayList<ScanDetail> sort() {
        ArrayList<ScanDetail> arrayList = new ArrayList<>(this.mMap.values());
        if (arrayList.size() != 0) {
            Collections.sort(arrayList, new Comparator() {
                @Override
                public int compare(Object obj, Object obj2) {
                    ScanResult scanResult = ((ScanDetail) obj).getScanResult();
                    ScanResult scanResult2 = ((ScanDetail) obj2).getScanResult();
                    if (scanResult.seen > scanResult2.seen) {
                        return -1;
                    }
                    if (scanResult.seen < scanResult2.seen) {
                        return 1;
                    }
                    if (scanResult.level > scanResult2.level) {
                        return -1;
                    }
                    if (scanResult.level < scanResult2.level) {
                        return 1;
                    }
                    return scanResult.BSSID.compareTo(scanResult2.BSSID);
                }
            });
        }
        return arrayList;
    }

    public String toString() {
        Iterator<ScanDetail> it;
        long j;
        long j2;
        long j3;
        long j4;
        long j5;
        StringBuilder sb = new StringBuilder();
        sb.append("Scan Cache:  ");
        sb.append('\n');
        ArrayList<ScanDetail> arrayListSort = sort();
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (arrayListSort.size() > 0) {
            Iterator<ScanDetail> it2 = arrayListSort.iterator();
            while (it2.hasNext()) {
                ScanDetail next = it2.next();
                ScanResult scanResult = next.getScanResult();
                long seen = jCurrentTimeMillis - next.getSeen();
                if (jCurrentTimeMillis <= next.getSeen() || next.getSeen() <= 0) {
                    it = it2;
                    j = 0;
                    j2 = 0;
                    j3 = 0;
                    j4 = 0;
                    j5 = 0;
                } else {
                    j4 = seen % 1000;
                    j3 = (seen / 1000) % 60;
                    long j6 = (seen / AnqpCache.CACHE_SWEEP_INTERVAL_MILLISECONDS) % 60;
                    long j7 = (seen / 3600000) % 24;
                    j2 = seen / 86400000;
                    it = it2;
                    j5 = j6;
                    j = j7;
                }
                sb.append("{");
                sb.append(scanResult.BSSID);
                sb.append(",");
                sb.append(scanResult.frequency);
                sb.append(",");
                sb.append(String.format("%3d", Integer.valueOf(scanResult.level)));
                if (j3 > 0 || j4 > 0) {
                    sb.append(String.format(",%4d.%02d.%02d.%02d.%03dms", Long.valueOf(j2), Long.valueOf(j), Long.valueOf(j5), Long.valueOf(j3), Long.valueOf(j4)));
                }
                sb.append("} ");
                it2 = it;
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
