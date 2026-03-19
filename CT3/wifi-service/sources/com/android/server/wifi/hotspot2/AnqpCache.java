package com.android.server.wifi.hotspot2;

import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.anqp.ANQPElement;
import com.android.server.wifi.anqp.Constants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnqpCache {
    private static final long CACHE_RECHECK = 60000;
    private static final boolean DBG = false;
    private static final boolean STANDARD_ESS = true;
    private final HashMap<CacheKey, ANQPData> mANQPCache = new HashMap<>();
    private Clock mClock;
    private long mLastSweep;

    public AnqpCache(Clock clock) {
        this.mClock = clock;
        this.mLastSweep = this.mClock.currentTimeMillis();
    }

    private static class CacheKey {
        private final long mBSSID;
        private final long mHESSID;
        private final String mSSID;

        private CacheKey(String ssid, long bssid, long hessid) {
            this.mSSID = ssid;
            this.mBSSID = bssid;
            this.mHESSID = hessid;
        }

        private static CacheKey buildKey(NetworkDetail network, boolean standardESS) {
            String ssid;
            long bssid;
            long hessid;
            if (network.getAnqpDomainID() == 0 || (network.getHESSID() == 0 && !standardESS)) {
                ssid = network.getSSID();
                bssid = network.getBSSID();
                hessid = 0;
            } else if (network.getHESSID() != 0 && network.getAnqpDomainID() > 0) {
                ssid = null;
                bssid = 0;
                hessid = network.getHESSID();
            } else {
                ssid = network.getSSID();
                bssid = 0;
                hessid = 0;
            }
            return new CacheKey(ssid, bssid, hessid);
        }

        public int hashCode() {
            if (this.mHESSID != 0) {
                return (int) (((this.mHESSID >>> 32) * 31) + this.mHESSID);
            }
            if (this.mBSSID != 0) {
                return (int) (((((long) (this.mSSID.hashCode() * 31)) + (this.mBSSID >>> 32)) * 31) + this.mBSSID);
            }
            return this.mSSID.hashCode();
        }

        public boolean equals(Object thatObject) {
            if (thatObject == this) {
                return true;
            }
            if (thatObject == null || thatObject.getClass() != CacheKey.class) {
                return false;
            }
            CacheKey that = (CacheKey) thatObject;
            if (Utils.compare(that.mSSID, this.mSSID) == 0 && that.mBSSID == this.mBSSID) {
                return that.mHESSID == this.mHESSID;
            }
            return false;
        }

        public String toString() {
            return this.mHESSID != 0 ? "HESSID:" + NetworkDetail.toMACString(this.mHESSID) : this.mBSSID != 0 ? NetworkDetail.toMACString(this.mBSSID) + ":<" + Utils.toUnicodeEscapedString(this.mSSID) + ">" : '<' + Utils.toUnicodeEscapedString(this.mSSID) + '>';
        }
    }

    public List<Constants.ANQPElementType> initiate(NetworkDetail network, List<Constants.ANQPElementType> querySet) {
        CacheKey key = CacheKey.buildKey(network, true);
        synchronized (this.mANQPCache) {
            ANQPData data = this.mANQPCache.get(key);
            if (data == null || data.expired()) {
                this.mANQPCache.put(key, new ANQPData(this.mClock, network, data));
                return querySet;
            }
            List<Constants.ANQPElementType> newList = data.disjoint(querySet);
            Log.d(Utils.hs2LogTag(getClass()), String.format("New ANQP elements for BSSID %012x: %s", Long.valueOf(network.getBSSID()), newList));
            return newList;
        }
    }

    public void update(NetworkDetail network, Map<Constants.ANQPElementType, ANQPElement> anqpElements) {
        CacheKey key = CacheKey.buildKey(network, true);
        synchronized (this.mANQPCache) {
            ANQPData data = this.mANQPCache.get(key);
            if (data != null && data.hasData()) {
                data.merge(anqpElements);
            } else {
                this.mANQPCache.put(key, new ANQPData(this.mClock, network, anqpElements));
            }
        }
    }

    public ANQPData getEntry(NetworkDetail network) {
        ANQPData data;
        CacheKey key = CacheKey.buildKey(network, true);
        synchronized (this.mANQPCache) {
            data = this.mANQPCache.get(key);
        }
        if (data == null || !data.isValid(network)) {
            return null;
        }
        return data;
    }

    public void clear(boolean all, boolean debug) {
        long now = this.mClock.currentTimeMillis();
        synchronized (this.mANQPCache) {
            if (all) {
                this.mANQPCache.clear();
                this.mLastSweep = now;
            } else if (now > this.mLastSweep + 60000) {
                List<CacheKey> retirees = new ArrayList<>();
                for (Map.Entry<CacheKey, ANQPData> entry : this.mANQPCache.entrySet()) {
                    if (entry.getValue().expired(now)) {
                        retirees.add(entry.getKey());
                    }
                }
                for (CacheKey key : retirees) {
                    this.mANQPCache.remove(key);
                    if (debug) {
                        Log.d(Utils.hs2LogTag(getClass()), "Retired " + key);
                    }
                }
                this.mLastSweep = now;
            }
        }
    }

    public void dump(PrintWriter out) {
        out.println("Last sweep " + Utils.toHMS(this.mClock.currentTimeMillis() - this.mLastSweep) + " ago.");
        for (ANQPData anqpData : this.mANQPCache.values()) {
            out.println(anqpData.toString(false));
        }
    }
}
