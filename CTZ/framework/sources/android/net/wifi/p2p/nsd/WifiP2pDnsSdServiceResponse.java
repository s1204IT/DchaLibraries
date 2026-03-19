package android.net.wifi.p2p.nsd;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WifiP2pDnsSdServiceResponse extends WifiP2pServiceResponse {
    private static final Map<Integer, String> sVmpack = new HashMap();
    private String mDnsQueryName;
    private int mDnsType;
    private String mInstanceName;
    private final HashMap<String, String> mTxtRecord;
    private int mVersion;

    static {
        sVmpack.put(12, "_tcp.local.");
        sVmpack.put(17, "local.");
        sVmpack.put(28, "_udp.local.");
    }

    public String getDnsQueryName() {
        return this.mDnsQueryName;
    }

    public int getDnsType() {
        return this.mDnsType;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public String getInstanceName() {
        return this.mInstanceName;
    }

    public Map<String, String> getTxtRecord() {
        return this.mTxtRecord;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("serviceType:DnsSd(");
        stringBuffer.append(this.mServiceType);
        stringBuffer.append(")");
        stringBuffer.append(" status:");
        stringBuffer.append(WifiP2pServiceResponse.Status.toString(this.mStatus));
        stringBuffer.append(" srcAddr:");
        stringBuffer.append(this.mDevice.deviceAddress);
        stringBuffer.append(" version:");
        stringBuffer.append(String.format("%02x", Integer.valueOf(this.mVersion)));
        stringBuffer.append(" dnsName:");
        stringBuffer.append(this.mDnsQueryName);
        stringBuffer.append(" TxtRecord:");
        for (String str : this.mTxtRecord.keySet()) {
            stringBuffer.append(" key:");
            stringBuffer.append(str);
            stringBuffer.append(" value:");
            stringBuffer.append(this.mTxtRecord.get(str));
        }
        if (this.mInstanceName != null) {
            stringBuffer.append(" InsName:");
            stringBuffer.append(this.mInstanceName);
        }
        return stringBuffer.toString();
    }

    protected WifiP2pDnsSdServiceResponse(int i, int i2, WifiP2pDevice wifiP2pDevice, byte[] bArr) {
        super(1, i, i2, wifiP2pDevice, bArr);
        this.mTxtRecord = new HashMap<>();
        if (!parse()) {
            throw new IllegalArgumentException("Malformed bonjour service response");
        }
    }

    private boolean parse() {
        if (this.mData == null) {
            return true;
        }
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(this.mData));
        this.mDnsQueryName = readDnsName(dataInputStream);
        if (this.mDnsQueryName == null) {
            return false;
        }
        try {
            this.mDnsType = dataInputStream.readUnsignedShort();
            this.mVersion = dataInputStream.readUnsignedByte();
            if (this.mDnsType == 12) {
                String dnsName = readDnsName(dataInputStream);
                if (dnsName == null || dnsName.length() <= this.mDnsQueryName.length()) {
                    return false;
                }
                this.mInstanceName = dnsName.substring(0, (dnsName.length() - this.mDnsQueryName.length()) - 1);
                return true;
            }
            if (this.mDnsType == 16) {
                return readTxtData(dataInputStream);
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String readDnsName(DataInputStream dataInputStream) {
        StringBuffer stringBuffer = new StringBuffer();
        HashMap map = new HashMap(sVmpack);
        if (this.mDnsQueryName != null) {
            map.put(39, this.mDnsQueryName);
        }
        while (true) {
            try {
                int unsignedByte = dataInputStream.readUnsignedByte();
                if (unsignedByte == 0) {
                    return stringBuffer.toString();
                }
                if (unsignedByte == 192) {
                    String str = (String) map.get(Integer.valueOf(dataInputStream.readUnsignedByte()));
                    if (str == null) {
                        return null;
                    }
                    stringBuffer.append(str);
                    return stringBuffer.toString();
                }
                byte[] bArr = new byte[unsignedByte];
                dataInputStream.readFully(bArr);
                stringBuffer.append(new String(bArr));
                stringBuffer.append(".");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private boolean readTxtData(DataInputStream dataInputStream) {
        int unsignedByte;
        while (dataInputStream.available() > 0 && (unsignedByte = dataInputStream.readUnsignedByte()) != 0) {
            try {
                byte[] bArr = new byte[unsignedByte];
                dataInputStream.readFully(bArr);
                String[] strArrSplit = new String(bArr).split("=");
                if (strArrSplit.length != 2) {
                    return false;
                }
                this.mTxtRecord.put(strArrSplit[0], strArrSplit[1]);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    static WifiP2pDnsSdServiceResponse newInstance(int i, int i2, WifiP2pDevice wifiP2pDevice, byte[] bArr) {
        if (i != 0) {
            return new WifiP2pDnsSdServiceResponse(i, i2, wifiP2pDevice, null);
        }
        try {
            return new WifiP2pDnsSdServiceResponse(i, i2, wifiP2pDevice, bArr);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
}
