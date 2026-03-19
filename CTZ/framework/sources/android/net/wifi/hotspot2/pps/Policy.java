package android.net.wifi.hotspot2.pps;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Policy implements Parcelable {
    public static final Parcelable.Creator<Policy> CREATOR = new Parcelable.Creator<Policy>() {
        @Override
        public Policy createFromParcel(Parcel parcel) {
            Policy policy = new Policy();
            policy.setMinHomeDownlinkBandwidth(parcel.readLong());
            policy.setMinHomeUplinkBandwidth(parcel.readLong());
            policy.setMinRoamingDownlinkBandwidth(parcel.readLong());
            policy.setMinRoamingUplinkBandwidth(parcel.readLong());
            policy.setExcludedSsidList(parcel.createStringArray());
            policy.setRequiredProtoPortMap(readProtoPortMap(parcel));
            policy.setMaximumBssLoadValue(parcel.readInt());
            policy.setPreferredRoamingPartnerList(readRoamingPartnerList(parcel));
            policy.setPolicyUpdate((UpdateParameter) parcel.readParcelable(null));
            return policy;
        }

        @Override
        public Policy[] newArray(int i) {
            return new Policy[i];
        }

        private Map<Integer, String> readProtoPortMap(Parcel parcel) {
            int i = parcel.readInt();
            if (i == -1) {
                return null;
            }
            HashMap map = new HashMap(i);
            for (int i2 = 0; i2 < i; i2++) {
                int i3 = parcel.readInt();
                map.put(Integer.valueOf(i3), parcel.readString());
            }
            return map;
        }

        private List<RoamingPartner> readRoamingPartnerList(Parcel parcel) {
            int i = parcel.readInt();
            if (i == -1) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            for (int i2 = 0; i2 < i; i2++) {
                arrayList.add((RoamingPartner) parcel.readParcelable(null));
            }
            return arrayList;
        }
    };
    private static final int MAX_EXCLUSION_SSIDS = 128;
    private static final int MAX_PORT_STRING_BYTES = 64;
    private static final int MAX_SSID_BYTES = 32;
    private static final int NULL_VALUE = -1;
    private static final String TAG = "Policy";
    private String[] mExcludedSsidList;
    private int mMaximumBssLoadValue;
    private long mMinHomeDownlinkBandwidth;
    private long mMinHomeUplinkBandwidth;
    private long mMinRoamingDownlinkBandwidth;
    private long mMinRoamingUplinkBandwidth;
    private UpdateParameter mPolicyUpdate;
    private List<RoamingPartner> mPreferredRoamingPartnerList;
    private Map<Integer, String> mRequiredProtoPortMap;

    public void setMinHomeDownlinkBandwidth(long j) {
        this.mMinHomeDownlinkBandwidth = j;
    }

    public long getMinHomeDownlinkBandwidth() {
        return this.mMinHomeDownlinkBandwidth;
    }

    public void setMinHomeUplinkBandwidth(long j) {
        this.mMinHomeUplinkBandwidth = j;
    }

    public long getMinHomeUplinkBandwidth() {
        return this.mMinHomeUplinkBandwidth;
    }

    public void setMinRoamingDownlinkBandwidth(long j) {
        this.mMinRoamingDownlinkBandwidth = j;
    }

    public long getMinRoamingDownlinkBandwidth() {
        return this.mMinRoamingDownlinkBandwidth;
    }

    public void setMinRoamingUplinkBandwidth(long j) {
        this.mMinRoamingUplinkBandwidth = j;
    }

    public long getMinRoamingUplinkBandwidth() {
        return this.mMinRoamingUplinkBandwidth;
    }

    public void setExcludedSsidList(String[] strArr) {
        this.mExcludedSsidList = strArr;
    }

    public String[] getExcludedSsidList() {
        return this.mExcludedSsidList;
    }

    public void setRequiredProtoPortMap(Map<Integer, String> map) {
        this.mRequiredProtoPortMap = map;
    }

    public Map<Integer, String> getRequiredProtoPortMap() {
        return this.mRequiredProtoPortMap;
    }

    public void setMaximumBssLoadValue(int i) {
        this.mMaximumBssLoadValue = i;
    }

    public int getMaximumBssLoadValue() {
        return this.mMaximumBssLoadValue;
    }

    public static final class RoamingPartner implements Parcelable {
        public static final Parcelable.Creator<RoamingPartner> CREATOR = new Parcelable.Creator<RoamingPartner>() {
            @Override
            public RoamingPartner createFromParcel(Parcel parcel) {
                RoamingPartner roamingPartner = new RoamingPartner();
                roamingPartner.setFqdn(parcel.readString());
                roamingPartner.setFqdnExactMatch(parcel.readInt() != 0);
                roamingPartner.setPriority(parcel.readInt());
                roamingPartner.setCountries(parcel.readString());
                return roamingPartner;
            }

            @Override
            public RoamingPartner[] newArray(int i) {
                return new RoamingPartner[i];
            }
        };
        private String mCountries;
        private String mFqdn;
        private boolean mFqdnExactMatch;
        private int mPriority;

        public void setFqdn(String str) {
            this.mFqdn = str;
        }

        public String getFqdn() {
            return this.mFqdn;
        }

        public void setFqdnExactMatch(boolean z) {
            this.mFqdnExactMatch = z;
        }

        public boolean getFqdnExactMatch() {
            return this.mFqdnExactMatch;
        }

        public void setPriority(int i) {
            this.mPriority = i;
        }

        public int getPriority() {
            return this.mPriority;
        }

        public void setCountries(String str) {
            this.mCountries = str;
        }

        public String getCountries() {
            return this.mCountries;
        }

        public RoamingPartner() {
            this.mFqdn = null;
            this.mFqdnExactMatch = false;
            this.mPriority = Integer.MIN_VALUE;
            this.mCountries = null;
        }

        public RoamingPartner(RoamingPartner roamingPartner) {
            this.mFqdn = null;
            this.mFqdnExactMatch = false;
            this.mPriority = Integer.MIN_VALUE;
            this.mCountries = null;
            if (roamingPartner != null) {
                this.mFqdn = roamingPartner.mFqdn;
                this.mFqdnExactMatch = roamingPartner.mFqdnExactMatch;
                this.mPriority = roamingPartner.mPriority;
                this.mCountries = roamingPartner.mCountries;
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mFqdn);
            parcel.writeInt(this.mFqdnExactMatch ? 1 : 0);
            parcel.writeInt(this.mPriority);
            parcel.writeString(this.mCountries);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RoamingPartner)) {
                return false;
            }
            RoamingPartner roamingPartner = (RoamingPartner) obj;
            return TextUtils.equals(this.mFqdn, roamingPartner.mFqdn) && this.mFqdnExactMatch == roamingPartner.mFqdnExactMatch && this.mPriority == roamingPartner.mPriority && TextUtils.equals(this.mCountries, roamingPartner.mCountries);
        }

        public int hashCode() {
            return Objects.hash(this.mFqdn, Boolean.valueOf(this.mFqdnExactMatch), Integer.valueOf(this.mPriority), this.mCountries);
        }

        public String toString() {
            return "FQDN: " + this.mFqdn + "\nExactMatch: mFqdnExactMatch\nPriority: " + this.mPriority + "\nCountries: " + this.mCountries + "\n";
        }

        public boolean validate() {
            if (TextUtils.isEmpty(this.mFqdn)) {
                Log.d(Policy.TAG, "Missing FQDN");
                return false;
            }
            if (TextUtils.isEmpty(this.mCountries)) {
                Log.d(Policy.TAG, "Missing countries");
                return false;
            }
            return true;
        }
    }

    public void setPreferredRoamingPartnerList(List<RoamingPartner> list) {
        this.mPreferredRoamingPartnerList = list;
    }

    public List<RoamingPartner> getPreferredRoamingPartnerList() {
        return this.mPreferredRoamingPartnerList;
    }

    public void setPolicyUpdate(UpdateParameter updateParameter) {
        this.mPolicyUpdate = updateParameter;
    }

    public UpdateParameter getPolicyUpdate() {
        return this.mPolicyUpdate;
    }

    public Policy() {
        this.mMinHomeDownlinkBandwidth = Long.MIN_VALUE;
        this.mMinHomeUplinkBandwidth = Long.MIN_VALUE;
        this.mMinRoamingDownlinkBandwidth = Long.MIN_VALUE;
        this.mMinRoamingUplinkBandwidth = Long.MIN_VALUE;
        this.mExcludedSsidList = null;
        this.mRequiredProtoPortMap = null;
        this.mMaximumBssLoadValue = Integer.MIN_VALUE;
        this.mPreferredRoamingPartnerList = null;
        this.mPolicyUpdate = null;
    }

    public Policy(Policy policy) {
        this.mMinHomeDownlinkBandwidth = Long.MIN_VALUE;
        this.mMinHomeUplinkBandwidth = Long.MIN_VALUE;
        this.mMinRoamingDownlinkBandwidth = Long.MIN_VALUE;
        this.mMinRoamingUplinkBandwidth = Long.MIN_VALUE;
        this.mExcludedSsidList = null;
        this.mRequiredProtoPortMap = null;
        this.mMaximumBssLoadValue = Integer.MIN_VALUE;
        this.mPreferredRoamingPartnerList = null;
        this.mPolicyUpdate = null;
        if (policy == null) {
            return;
        }
        this.mMinHomeDownlinkBandwidth = policy.mMinHomeDownlinkBandwidth;
        this.mMinHomeUplinkBandwidth = policy.mMinHomeUplinkBandwidth;
        this.mMinRoamingDownlinkBandwidth = policy.mMinRoamingDownlinkBandwidth;
        this.mMinRoamingUplinkBandwidth = policy.mMinRoamingUplinkBandwidth;
        this.mMaximumBssLoadValue = policy.mMaximumBssLoadValue;
        if (policy.mExcludedSsidList != null) {
            this.mExcludedSsidList = (String[]) Arrays.copyOf(policy.mExcludedSsidList, policy.mExcludedSsidList.length);
        }
        if (policy.mRequiredProtoPortMap != null) {
            this.mRequiredProtoPortMap = Collections.unmodifiableMap(policy.mRequiredProtoPortMap);
        }
        if (policy.mPreferredRoamingPartnerList != null) {
            this.mPreferredRoamingPartnerList = Collections.unmodifiableList(policy.mPreferredRoamingPartnerList);
        }
        if (policy.mPolicyUpdate != null) {
            this.mPolicyUpdate = new UpdateParameter(policy.mPolicyUpdate);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mMinHomeDownlinkBandwidth);
        parcel.writeLong(this.mMinHomeUplinkBandwidth);
        parcel.writeLong(this.mMinRoamingDownlinkBandwidth);
        parcel.writeLong(this.mMinRoamingUplinkBandwidth);
        parcel.writeStringArray(this.mExcludedSsidList);
        writeProtoPortMap(parcel, this.mRequiredProtoPortMap);
        parcel.writeInt(this.mMaximumBssLoadValue);
        writeRoamingPartnerList(parcel, i, this.mPreferredRoamingPartnerList);
        parcel.writeParcelable(this.mPolicyUpdate, i);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Policy)) {
            return false;
        }
        Policy policy = (Policy) obj;
        if (this.mMinHomeDownlinkBandwidth == policy.mMinHomeDownlinkBandwidth && this.mMinHomeUplinkBandwidth == policy.mMinHomeUplinkBandwidth && this.mMinRoamingDownlinkBandwidth == policy.mMinRoamingDownlinkBandwidth && this.mMinRoamingUplinkBandwidth == policy.mMinRoamingUplinkBandwidth && Arrays.equals(this.mExcludedSsidList, policy.mExcludedSsidList) && (this.mRequiredProtoPortMap != null ? this.mRequiredProtoPortMap.equals(policy.mRequiredProtoPortMap) : policy.mRequiredProtoPortMap == null) && this.mMaximumBssLoadValue == policy.mMaximumBssLoadValue && (this.mPreferredRoamingPartnerList != null ? this.mPreferredRoamingPartnerList.equals(policy.mPreferredRoamingPartnerList) : policy.mPreferredRoamingPartnerList == null)) {
            if (this.mPolicyUpdate == null) {
                if (policy.mPolicyUpdate == null) {
                    return true;
                }
            } else if (this.mPolicyUpdate.equals(policy.mPolicyUpdate)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Long.valueOf(this.mMinHomeDownlinkBandwidth), Long.valueOf(this.mMinHomeUplinkBandwidth), Long.valueOf(this.mMinRoamingDownlinkBandwidth), Long.valueOf(this.mMinRoamingUplinkBandwidth), this.mExcludedSsidList, this.mRequiredProtoPortMap, Integer.valueOf(this.mMaximumBssLoadValue), this.mPreferredRoamingPartnerList, this.mPolicyUpdate);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MinHomeDownlinkBandwidth: ");
        sb.append(this.mMinHomeDownlinkBandwidth);
        sb.append("\n");
        sb.append("MinHomeUplinkBandwidth: ");
        sb.append(this.mMinHomeUplinkBandwidth);
        sb.append("\n");
        sb.append("MinRoamingDownlinkBandwidth: ");
        sb.append(this.mMinRoamingDownlinkBandwidth);
        sb.append("\n");
        sb.append("MinRoamingUplinkBandwidth: ");
        sb.append(this.mMinRoamingUplinkBandwidth);
        sb.append("\n");
        sb.append("ExcludedSSIDList: ");
        sb.append(this.mExcludedSsidList);
        sb.append("\n");
        sb.append("RequiredProtoPortMap: ");
        sb.append(this.mRequiredProtoPortMap);
        sb.append("\n");
        sb.append("MaximumBSSLoadValue: ");
        sb.append(this.mMaximumBssLoadValue);
        sb.append("\n");
        sb.append("PreferredRoamingPartnerList: ");
        sb.append(this.mPreferredRoamingPartnerList);
        sb.append("\n");
        if (this.mPolicyUpdate != null) {
            sb.append("PolicyUpdate Begin ---\n");
            sb.append(this.mPolicyUpdate);
            sb.append("PolicyUpdate End ---\n");
        }
        return sb.toString();
    }

    public boolean validate() {
        if (this.mPolicyUpdate == null) {
            Log.d(TAG, "PolicyUpdate not specified");
            return false;
        }
        if (!this.mPolicyUpdate.validate()) {
            return false;
        }
        if (this.mExcludedSsidList != null) {
            if (this.mExcludedSsidList.length > 128) {
                Log.d(TAG, "SSID exclusion list size exceeded the max: " + this.mExcludedSsidList.length);
                return false;
            }
            for (String str : this.mExcludedSsidList) {
                if (str.getBytes(StandardCharsets.UTF_8).length > 32) {
                    Log.d(TAG, "Invalid SSID: " + str);
                    return false;
                }
            }
        }
        if (this.mRequiredProtoPortMap != null) {
            Iterator<Map.Entry<Integer, String>> it = this.mRequiredProtoPortMap.entrySet().iterator();
            while (it.hasNext()) {
                String value = it.next().getValue();
                if (value.getBytes(StandardCharsets.UTF_8).length > 64) {
                    Log.d(TAG, "PortNumber string bytes exceeded the max: " + value);
                    return false;
                }
            }
        }
        if (this.mPreferredRoamingPartnerList != null) {
            Iterator<RoamingPartner> it2 = this.mPreferredRoamingPartnerList.iterator();
            while (it2.hasNext()) {
                if (!it2.next().validate()) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private static void writeProtoPortMap(Parcel parcel, Map<Integer, String> map) {
        if (map == null) {
            parcel.writeInt(-1);
            return;
        }
        parcel.writeInt(map.size());
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            parcel.writeInt(entry.getKey().intValue());
            parcel.writeString(entry.getValue());
        }
    }

    private static void writeRoamingPartnerList(Parcel parcel, int i, List<RoamingPartner> list) {
        if (list == null) {
            parcel.writeInt(-1);
            return;
        }
        parcel.writeInt(list.size());
        Iterator<RoamingPartner> it = list.iterator();
        while (it.hasNext()) {
            parcel.writeParcelable(it.next(), i);
        }
    }
}
