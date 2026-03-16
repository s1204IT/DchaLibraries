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
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("serviceType:DnsSd(").append(this.mServiceType).append(")");
        sbuf.append(" status:").append(WifiP2pServiceResponse.Status.toString(this.mStatus));
        sbuf.append(" srcAddr:").append(this.mDevice.deviceAddress);
        sbuf.append(" version:").append(String.format("%02x", Integer.valueOf(this.mVersion)));
        sbuf.append(" dnsName:").append(this.mDnsQueryName);
        sbuf.append(" TxtRecord:");
        for (String key : this.mTxtRecord.keySet()) {
            sbuf.append(" key:").append(key).append(" value:").append(this.mTxtRecord.get(key));
        }
        if (this.mInstanceName != null) {
            sbuf.append(" InsName:").append(this.mInstanceName);
        }
        return sbuf.toString();
    }

    protected WifiP2pDnsSdServiceResponse(int status, int tranId, WifiP2pDevice dev, byte[] data) {
        super(1, status, tranId, dev, data);
        this.mTxtRecord = new HashMap<>();
        if (!parse()) {
            throw new IllegalArgumentException("Malformed bonjour service response");
        }
    }

    private boolean parse() {
        if (this.mData == null) {
            return true;
        }
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(this.mData));
        this.mDnsQueryName = readDnsName(dis);
        if (this.mDnsQueryName == null) {
            return false;
        }
        try {
            this.mDnsType = dis.readUnsignedShort();
            this.mVersion = dis.readUnsignedByte();
            if (this.mDnsType == 12) {
                String rData = readDnsName(dis);
                if (rData != null && rData.length() > this.mDnsQueryName.length()) {
                    this.mInstanceName = rData.substring(0, (rData.length() - this.mDnsQueryName.length()) - 1);
                    return true;
                }
                return false;
            }
            if (this.mDnsType == 16) {
                return readTxtData(dis);
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String readDnsName(DataInputStream dis) {
        StringBuffer sb = new StringBuffer();
        HashMap<Integer, String> vmpack = new HashMap<>(sVmpack);
        if (this.mDnsQueryName != null) {
            vmpack.put(39, this.mDnsQueryName);
        }
        while (true) {
            try {
                int i = dis.readUnsignedByte();
                if (i == 0) {
                    return sb.toString();
                }
                if (i == 192) {
                    String ref = vmpack.get(Integer.valueOf(dis.readUnsignedByte()));
                    if (ref == null) {
                        return null;
                    }
                    sb.append(ref);
                    return sb.toString();
                }
                byte[] data = new byte[i];
                dis.readFully(data);
                sb.append(new String(data));
                sb.append(".");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private boolean readTxtData(DataInputStream dis) {
        int len;
        while (dis.available() > 0 && (len = dis.readUnsignedByte()) != 0) {
            try {
                byte[] data = new byte[len];
                dis.readFully(data);
                String[] keyVal = new String(data).split("=");
                if (keyVal.length != 2) {
                    return false;
                }
                this.mTxtRecord.put(keyVal[0], keyVal[1]);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    static WifiP2pDnsSdServiceResponse newInstance(int status, int transId, WifiP2pDevice dev, byte[] data) {
        if (status != 0) {
            return new WifiP2pDnsSdServiceResponse(status, transId, dev, null);
        }
        try {
            return new WifiP2pDnsSdServiceResponse(status, transId, dev, data);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
}
