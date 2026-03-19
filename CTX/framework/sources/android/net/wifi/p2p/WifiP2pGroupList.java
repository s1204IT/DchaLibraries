package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.LruCache;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class WifiP2pGroupList implements Parcelable {
    public static final Parcelable.Creator<WifiP2pGroupList> CREATOR = new Parcelable.Creator<WifiP2pGroupList>() {
        @Override
        public WifiP2pGroupList createFromParcel(Parcel parcel) {
            WifiP2pGroupList wifiP2pGroupList = new WifiP2pGroupList();
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                wifiP2pGroupList.add((WifiP2pGroup) parcel.readParcelable(null));
            }
            return wifiP2pGroupList;
        }

        @Override
        public WifiP2pGroupList[] newArray(int i) {
            return new WifiP2pGroupList[i];
        }
    };
    private static final int CREDENTIAL_MAX_NUM = 32;
    private boolean isClearCalled;
    private final LruCache<Integer, WifiP2pGroup> mGroups;
    private final GroupDeleteListener mListener;

    public interface GroupDeleteListener {
        void onDeleteGroup(int i);
    }

    public WifiP2pGroupList() {
        this(null, null);
    }

    public WifiP2pGroupList(WifiP2pGroupList wifiP2pGroupList, GroupDeleteListener groupDeleteListener) {
        this.isClearCalled = false;
        this.mListener = groupDeleteListener;
        this.mGroups = new LruCache<Integer, WifiP2pGroup>(32) {
            @Override
            protected void entryRemoved(boolean z, Integer num, WifiP2pGroup wifiP2pGroup, WifiP2pGroup wifiP2pGroup2) {
                if (WifiP2pGroupList.this.mListener != null && !WifiP2pGroupList.this.isClearCalled) {
                    WifiP2pGroupList.this.mListener.onDeleteGroup(wifiP2pGroup.getNetworkId());
                }
            }
        };
        if (wifiP2pGroupList != null) {
            for (Map.Entry<Integer, WifiP2pGroup> entry : wifiP2pGroupList.mGroups.snapshot().entrySet()) {
                this.mGroups.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Collection<WifiP2pGroup> getGroupList() {
        return this.mGroups.snapshot().values();
    }

    public void add(WifiP2pGroup wifiP2pGroup) {
        this.mGroups.put(Integer.valueOf(wifiP2pGroup.getNetworkId()), wifiP2pGroup);
    }

    public void remove(int i) {
        this.mGroups.remove(Integer.valueOf(i));
    }

    void remove(String str) {
        remove(getNetworkId(str));
    }

    public boolean clear() {
        if (this.mGroups.size() == 0) {
            return false;
        }
        this.isClearCalled = true;
        this.mGroups.evictAll();
        this.isClearCalled = false;
        return true;
    }

    public int getNetworkId(String str) {
        if (str == null) {
            return -1;
        }
        for (WifiP2pGroup wifiP2pGroup : this.mGroups.snapshot().values()) {
            if (str.equalsIgnoreCase(wifiP2pGroup.getOwner().deviceAddress)) {
                this.mGroups.get(Integer.valueOf(wifiP2pGroup.getNetworkId()));
                return wifiP2pGroup.getNetworkId();
            }
        }
        return -1;
    }

    public int getNetworkId(String str, String str2) {
        if (str == null || str2 == null) {
            return -1;
        }
        for (WifiP2pGroup wifiP2pGroup : this.mGroups.snapshot().values()) {
            if (str.equalsIgnoreCase(wifiP2pGroup.getOwner().deviceAddress) && str2.equals(wifiP2pGroup.getNetworkName())) {
                this.mGroups.get(Integer.valueOf(wifiP2pGroup.getNetworkId()));
                return wifiP2pGroup.getNetworkId();
            }
        }
        return -1;
    }

    public String getOwnerAddr(int i) {
        WifiP2pGroup wifiP2pGroup = this.mGroups.get(Integer.valueOf(i));
        if (wifiP2pGroup != null) {
            return wifiP2pGroup.getOwner().deviceAddress;
        }
        return null;
    }

    public boolean contains(int i) {
        Iterator<WifiP2pGroup> it = this.mGroups.snapshot().values().iterator();
        while (it.hasNext()) {
            if (i == it.next().getNetworkId()) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<WifiP2pGroup> it = this.mGroups.snapshot().values().iterator();
        while (it.hasNext()) {
            stringBuffer.append(it.next());
            stringBuffer.append("\n");
        }
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Collection<WifiP2pGroup> collectionValues = this.mGroups.snapshot().values();
        parcel.writeInt(collectionValues.size());
        Iterator<WifiP2pGroup> it = collectionValues.iterator();
        while (it.hasNext()) {
            parcel.writeParcelable(it.next(), i);
        }
    }
}
