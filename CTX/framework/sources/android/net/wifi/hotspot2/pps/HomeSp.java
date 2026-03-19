package android.net.wifi.hotspot2.pps;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class HomeSp implements Parcelable {
    public static final Parcelable.Creator<HomeSp> CREATOR = new Parcelable.Creator<HomeSp>() {
        @Override
        public HomeSp createFromParcel(Parcel parcel) {
            HomeSp homeSp = new HomeSp();
            homeSp.setFqdn(parcel.readString());
            homeSp.setFriendlyName(parcel.readString());
            homeSp.setIconUrl(parcel.readString());
            homeSp.setHomeNetworkIds(readHomeNetworkIds(parcel));
            homeSp.setMatchAllOis(parcel.createLongArray());
            homeSp.setMatchAnyOis(parcel.createLongArray());
            homeSp.setOtherHomePartners(parcel.createStringArray());
            homeSp.setRoamingConsortiumOis(parcel.createLongArray());
            return homeSp;
        }

        @Override
        public HomeSp[] newArray(int i) {
            return new HomeSp[i];
        }

        private Map<String, Long> readHomeNetworkIds(Parcel parcel) {
            Long lValueOf;
            int i = parcel.readInt();
            if (i == -1) {
                return null;
            }
            HashMap map = new HashMap(i);
            for (int i2 = 0; i2 < i; i2++) {
                String string = parcel.readString();
                long j = parcel.readLong();
                if (j != -1) {
                    lValueOf = Long.valueOf(j);
                } else {
                    lValueOf = null;
                }
                map.put(string, lValueOf);
            }
            return map;
        }
    };
    private static final int MAX_SSID_BYTES = 32;
    private static final int NULL_VALUE = -1;
    private static final String TAG = "HomeSp";
    private String mFqdn;
    private String mFriendlyName;
    private Map<String, Long> mHomeNetworkIds;
    private String mIconUrl;
    private long[] mMatchAllOis;
    private long[] mMatchAnyOis;
    private String[] mOtherHomePartners;
    private long[] mRoamingConsortiumOis;

    public void setFqdn(String str) {
        this.mFqdn = str;
    }

    public String getFqdn() {
        return this.mFqdn;
    }

    public void setFriendlyName(String str) {
        this.mFriendlyName = str;
    }

    public String getFriendlyName() {
        return this.mFriendlyName;
    }

    public void setIconUrl(String str) {
        this.mIconUrl = str;
    }

    public String getIconUrl() {
        return this.mIconUrl;
    }

    public void setHomeNetworkIds(Map<String, Long> map) {
        this.mHomeNetworkIds = map;
    }

    public Map<String, Long> getHomeNetworkIds() {
        return this.mHomeNetworkIds;
    }

    public void setMatchAllOis(long[] jArr) {
        this.mMatchAllOis = jArr;
    }

    public long[] getMatchAllOis() {
        return this.mMatchAllOis;
    }

    public void setMatchAnyOis(long[] jArr) {
        this.mMatchAnyOis = jArr;
    }

    public long[] getMatchAnyOis() {
        return this.mMatchAnyOis;
    }

    public void setOtherHomePartners(String[] strArr) {
        this.mOtherHomePartners = strArr;
    }

    public String[] getOtherHomePartners() {
        return this.mOtherHomePartners;
    }

    public void setRoamingConsortiumOis(long[] jArr) {
        this.mRoamingConsortiumOis = jArr;
    }

    public long[] getRoamingConsortiumOis() {
        return this.mRoamingConsortiumOis;
    }

    public HomeSp() {
        this.mFqdn = null;
        this.mFriendlyName = null;
        this.mIconUrl = null;
        this.mHomeNetworkIds = null;
        this.mMatchAllOis = null;
        this.mMatchAnyOis = null;
        this.mOtherHomePartners = null;
        this.mRoamingConsortiumOis = null;
    }

    public HomeSp(HomeSp homeSp) {
        this.mFqdn = null;
        this.mFriendlyName = null;
        this.mIconUrl = null;
        this.mHomeNetworkIds = null;
        this.mMatchAllOis = null;
        this.mMatchAnyOis = null;
        this.mOtherHomePartners = null;
        this.mRoamingConsortiumOis = null;
        if (homeSp == null) {
            return;
        }
        this.mFqdn = homeSp.mFqdn;
        this.mFriendlyName = homeSp.mFriendlyName;
        this.mIconUrl = homeSp.mIconUrl;
        if (homeSp.mHomeNetworkIds != null) {
            this.mHomeNetworkIds = Collections.unmodifiableMap(homeSp.mHomeNetworkIds);
        }
        if (homeSp.mMatchAllOis != null) {
            this.mMatchAllOis = Arrays.copyOf(homeSp.mMatchAllOis, homeSp.mMatchAllOis.length);
        }
        if (homeSp.mMatchAnyOis != null) {
            this.mMatchAnyOis = Arrays.copyOf(homeSp.mMatchAnyOis, homeSp.mMatchAnyOis.length);
        }
        if (homeSp.mOtherHomePartners != null) {
            this.mOtherHomePartners = (String[]) Arrays.copyOf(homeSp.mOtherHomePartners, homeSp.mOtherHomePartners.length);
        }
        if (homeSp.mRoamingConsortiumOis != null) {
            this.mRoamingConsortiumOis = Arrays.copyOf(homeSp.mRoamingConsortiumOis, homeSp.mRoamingConsortiumOis.length);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mFqdn);
        parcel.writeString(this.mFriendlyName);
        parcel.writeString(this.mIconUrl);
        writeHomeNetworkIds(parcel, this.mHomeNetworkIds);
        parcel.writeLongArray(this.mMatchAllOis);
        parcel.writeLongArray(this.mMatchAnyOis);
        parcel.writeStringArray(this.mOtherHomePartners);
        parcel.writeLongArray(this.mRoamingConsortiumOis);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HomeSp)) {
            return false;
        }
        HomeSp homeSp = (HomeSp) obj;
        return TextUtils.equals(this.mFqdn, homeSp.mFqdn) && TextUtils.equals(this.mFriendlyName, homeSp.mFriendlyName) && TextUtils.equals(this.mIconUrl, homeSp.mIconUrl) && (this.mHomeNetworkIds != null ? this.mHomeNetworkIds.equals(homeSp.mHomeNetworkIds) : homeSp.mHomeNetworkIds == null) && Arrays.equals(this.mMatchAllOis, homeSp.mMatchAllOis) && Arrays.equals(this.mMatchAnyOis, homeSp.mMatchAnyOis) && Arrays.equals(this.mOtherHomePartners, homeSp.mOtherHomePartners) && Arrays.equals(this.mRoamingConsortiumOis, homeSp.mRoamingConsortiumOis);
    }

    public int hashCode() {
        return Objects.hash(this.mFqdn, this.mFriendlyName, this.mIconUrl, this.mHomeNetworkIds, this.mMatchAllOis, this.mMatchAnyOis, this.mOtherHomePartners, this.mRoamingConsortiumOis);
    }

    public String toString() {
        return "FQDN: " + this.mFqdn + "\nFriendlyName: " + this.mFriendlyName + "\nIconURL: " + this.mIconUrl + "\nHomeNetworkIDs: " + this.mHomeNetworkIds + "\nMatchAllOIs: " + this.mMatchAllOis + "\nMatchAnyOIs: " + this.mMatchAnyOis + "\nOtherHomePartners: " + this.mOtherHomePartners + "\nRoamingConsortiumOIs: " + this.mRoamingConsortiumOis + "\n";
    }

    public boolean validate() {
        if (TextUtils.isEmpty(this.mFqdn)) {
            Log.d(TAG, "Missing FQDN");
            return false;
        }
        if (TextUtils.isEmpty(this.mFriendlyName)) {
            Log.d(TAG, "Missing friendly name");
            return false;
        }
        if (this.mHomeNetworkIds != null) {
            for (Map.Entry<String, Long> entry : this.mHomeNetworkIds.entrySet()) {
                if (entry.getKey() == null || entry.getKey().getBytes(StandardCharsets.UTF_8).length > 32) {
                    Log.d(TAG, "Invalid SSID in HomeNetworkIDs");
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private static void writeHomeNetworkIds(Parcel parcel, Map<String, Long> map) {
        if (map == null) {
            parcel.writeInt(-1);
            return;
        }
        parcel.writeInt(map.size());
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            parcel.writeString(entry.getKey());
            if (entry.getValue() == null) {
                parcel.writeLong(-1L);
            } else {
                parcel.writeLong(entry.getValue().longValue());
            }
        }
    }
}
