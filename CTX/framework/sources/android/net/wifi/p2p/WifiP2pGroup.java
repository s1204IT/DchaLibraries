package android.net.wifi.p2p;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiP2pGroup implements Parcelable {
    public static final int PERSISTENT_NET_ID = -2;
    private static final String TAG = "WifiP2pGroup";
    public static final int TEMPORARY_NET_ID = -1;
    private List<WifiP2pDevice> mClients = new ArrayList();
    private int mFreq;
    private String mInterface;
    private boolean mIsGroupOwner;
    private int mNetId;
    private String mNetworkName;
    private WifiP2pDevice mOwner;
    private String mPassphrase;
    private static final Pattern groupStartedPattern = Pattern.compile("ssid=\"(.+)\" freq=(\\d+) (?:psk=)?([0-9a-fA-F]{64})?(?:passphrase=)?(?:\"(.{0,63})\")? go_dev_addr=((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) ?(\\[PERSISTENT\\])?");
    private static final Pattern groupStartedPattern2 = Pattern.compile("go_dev_addr=((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) ?(\\[PERSISTENT\\])?");
    public static final Parcelable.Creator<WifiP2pGroup> CREATOR = new Parcelable.Creator<WifiP2pGroup>() {
        @Override
        public WifiP2pGroup createFromParcel(Parcel parcel) {
            WifiP2pGroup wifiP2pGroup = new WifiP2pGroup();
            wifiP2pGroup.setNetworkName(parcel.readString());
            wifiP2pGroup.setOwner((WifiP2pDevice) parcel.readParcelable(null));
            wifiP2pGroup.setIsGroupOwner(parcel.readByte() == 1);
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                wifiP2pGroup.addClient((WifiP2pDevice) parcel.readParcelable(null));
            }
            wifiP2pGroup.setPassphrase(parcel.readString());
            wifiP2pGroup.setInterface(parcel.readString());
            wifiP2pGroup.setNetworkId(parcel.readInt());
            wifiP2pGroup.setFrequency(parcel.readInt());
            return wifiP2pGroup;
        }

        @Override
        public WifiP2pGroup[] newArray(int i) {
            return new WifiP2pGroup[i];
        }
    };

    public WifiP2pGroup() {
    }

    public WifiP2pGroup(String str) throws IllegalArgumentException {
        String[] strArrSplit = str.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (strArrSplit.length < 3) {
            throw new IllegalArgumentException("Malformed supplicant event");
        }
        if (!strArrSplit[0].startsWith("P2P-GROUP")) {
            if (strArrSplit[0].equals("P2P-INVITATION-RECEIVED")) {
                this.mNetId = -2;
                String str2 = null;
                for (String str3 : strArrSplit) {
                    String[] strArrSplit2 = str3.split("=");
                    if (strArrSplit2.length == 2) {
                        if (strArrSplit2[0].equals("sa")) {
                            str2 = strArrSplit2[1];
                            WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
                            wifiP2pDevice.deviceAddress = strArrSplit2[1];
                            this.mClients.add(wifiP2pDevice);
                        } else if (strArrSplit2[0].equals("go_dev_addr")) {
                            this.mOwner = new WifiP2pDevice(strArrSplit2[1]);
                        } else if (strArrSplit2[0].equals("persistent")) {
                            this.mOwner = new WifiP2pDevice(str2);
                            this.mNetId = Integer.parseInt(strArrSplit2[1]);
                        }
                    }
                }
                return;
            }
            throw new IllegalArgumentException("Malformed supplicant event");
        }
        this.mInterface = strArrSplit[1];
        this.mIsGroupOwner = strArrSplit[2].equals("GO");
        Matcher matcher = groupStartedPattern.matcher(str);
        Matcher matcher2 = groupStartedPattern2.matcher(str);
        if (matcher != null && matcher.find()) {
            this.mNetworkName = matcher.group(1);
            this.mFreq = Integer.parseInt(matcher.group(2));
            this.mPassphrase = matcher.group(4);
            this.mOwner = new WifiP2pDevice(matcher.group(5));
            if (matcher.group(6) != null) {
                this.mNetId = -2;
            } else {
                this.mNetId = -1;
            }
            Log.d(TAG, "Normal match! frequency = " + this.mFreq);
            return;
        }
        if (matcher2 != null && matcher2.find()) {
            this.mNetworkName = "";
            this.mPassphrase = "";
            this.mOwner = new WifiP2pDevice(matcher2.group(1));
            if (matcher2.group(2) != null) {
                this.mNetId = -2;
            } else {
                this.mNetId = -1;
            }
            Log.d(TAG, "Not matched SSID, but matched device address!");
            return;
        }
        this.mNetworkName = "";
        this.mPassphrase = "";
        this.mOwner = new WifiP2pDevice();
        this.mNetId = -1;
        Log.d(TAG, "Match nothing!");
    }

    public void setNetworkName(String str) {
        this.mNetworkName = str;
    }

    public String getNetworkName() {
        return this.mNetworkName;
    }

    public void setIsGroupOwner(boolean z) {
        this.mIsGroupOwner = z;
    }

    public boolean isGroupOwner() {
        return this.mIsGroupOwner;
    }

    public void setOwner(WifiP2pDevice wifiP2pDevice) {
        this.mOwner = wifiP2pDevice;
    }

    public WifiP2pDevice getOwner() {
        return this.mOwner;
    }

    public void addClient(String str) {
        addClient(new WifiP2pDevice(str));
    }

    public void addClient(WifiP2pDevice wifiP2pDevice) {
        Iterator<WifiP2pDevice> it = this.mClients.iterator();
        while (it.hasNext()) {
            if (it.next().equals(wifiP2pDevice)) {
                return;
            }
        }
        this.mClients.add(wifiP2pDevice);
    }

    public boolean removeClient(String str) {
        return this.mClients.remove(new WifiP2pDevice(str));
    }

    public boolean removeClient(WifiP2pDevice wifiP2pDevice) {
        return this.mClients.remove(wifiP2pDevice);
    }

    public boolean isClientListEmpty() {
        return this.mClients.size() == 0;
    }

    public int getClientAmount() {
        return this.mClients.size();
    }

    public boolean contains(WifiP2pDevice wifiP2pDevice) {
        if (this.mOwner.equals(wifiP2pDevice) || this.mClients.contains(wifiP2pDevice)) {
            return true;
        }
        return false;
    }

    public Collection<WifiP2pDevice> getClientList() {
        return Collections.unmodifiableCollection(this.mClients);
    }

    public void setPassphrase(String str) {
        this.mPassphrase = str;
    }

    public String getPassphrase() {
        return this.mPassphrase;
    }

    public void setInterface(String str) {
        this.mInterface = str;
    }

    public String getInterface() {
        return this.mInterface;
    }

    public int getNetworkId() {
        return this.mNetId;
    }

    public void setNetworkId(int i) {
        this.mNetId = i;
    }

    public int getFrequency() {
        return this.mFreq;
    }

    public void setFrequency(int i) {
        this.mFreq = i;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("network: ");
        stringBuffer.append(this.mNetworkName);
        stringBuffer.append("\n isGO: ");
        stringBuffer.append(this.mIsGroupOwner);
        stringBuffer.append("\n GO: ");
        stringBuffer.append(this.mOwner);
        for (WifiP2pDevice wifiP2pDevice : this.mClients) {
            stringBuffer.append("\n Client: ");
            stringBuffer.append(wifiP2pDevice);
        }
        stringBuffer.append("\n interface: ");
        stringBuffer.append(this.mInterface);
        stringBuffer.append("\n networkId: ");
        stringBuffer.append(this.mNetId);
        stringBuffer.append("\n frequency: ");
        stringBuffer.append(this.mFreq);
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pGroup(WifiP2pGroup wifiP2pGroup) {
        if (wifiP2pGroup != null) {
            this.mNetworkName = wifiP2pGroup.getNetworkName();
            this.mOwner = new WifiP2pDevice(wifiP2pGroup.getOwner());
            this.mIsGroupOwner = wifiP2pGroup.mIsGroupOwner;
            Iterator<WifiP2pDevice> it = wifiP2pGroup.getClientList().iterator();
            while (it.hasNext()) {
                this.mClients.add(it.next());
            }
            this.mPassphrase = wifiP2pGroup.getPassphrase();
            this.mInterface = wifiP2pGroup.getInterface();
            this.mNetId = wifiP2pGroup.getNetworkId();
            this.mFreq = wifiP2pGroup.getFrequency();
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mNetworkName);
        parcel.writeParcelable(this.mOwner, i);
        parcel.writeByte(this.mIsGroupOwner ? (byte) 1 : (byte) 0);
        parcel.writeInt(this.mClients.size());
        Iterator<WifiP2pDevice> it = this.mClients.iterator();
        while (it.hasNext()) {
            parcel.writeParcelable(it.next(), i);
        }
        parcel.writeString(this.mPassphrase);
        parcel.writeString(this.mInterface);
        parcel.writeInt(this.mNetId);
        parcel.writeInt(this.mFreq);
    }
}
