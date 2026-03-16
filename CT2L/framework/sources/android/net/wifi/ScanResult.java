package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class ScanResult implements Parcelable {
    public static final int AUTHENTICATION_ERROR = 128;
    public static final int AUTO_JOIN_DISABLED = 32;
    public static final int AUTO_ROAM_DISABLED = 16;
    public static final Parcelable.Creator<ScanResult> CREATOR = new Parcelable.Creator<ScanResult>() {
        @Override
        public ScanResult createFromParcel(Parcel in) {
            WifiSsid wifiSsid = null;
            if (in.readInt() == 1) {
                WifiSsid wifiSsid2 = WifiSsid.CREATOR.createFromParcel(in);
                wifiSsid = wifiSsid2;
            }
            ScanResult sr = new ScanResult(wifiSsid, in.readString(), in.readString(), in.readInt(), in.readInt(), in.readLong(), in.readInt(), in.readInt());
            sr.seen = in.readLong();
            sr.autoJoinStatus = in.readInt();
            sr.untrusted = in.readInt() != 0;
            sr.numConnection = in.readInt();
            sr.numUsage = in.readInt();
            sr.numIpConfigFailures = in.readInt();
            sr.isAutoJoinCandidate = in.readInt();
            int n = in.readInt();
            if (n != 0) {
                sr.informationElements = new InformationElement[n];
                for (int i = 0; i < n; i++) {
                    sr.informationElements[i] = new InformationElement();
                    sr.informationElements[i].id = in.readInt();
                    int len = in.readInt();
                    sr.informationElements[i].bytes = new byte[len];
                    in.readByteArray(sr.informationElements[i].bytes);
                }
            }
            return sr;
        }

        @Override
        public ScanResult[] newArray(int size) {
            return new ScanResult[size];
        }
    };
    public static final int ENABLED = 0;
    public static final int UNSPECIFIED = -1;
    public String BSSID;
    public String SSID;
    public int autoJoinStatus;
    public long blackListTimestamp;
    public String capabilities;
    public int distanceCm;
    public int distanceSdCm;
    public int frequency;
    public InformationElement[] informationElements;
    public int isAutoJoinCandidate;
    public int level;
    public int numConnection;
    public int numIpConfigFailures;
    public int numUsage;
    public long seen;
    public long timestamp;
    public boolean untrusted;
    public WifiSsid wifiSsid;

    public void averageRssi(int previousRssi, long previousSeen, int maxAge) {
        if (this.seen == 0) {
            this.seen = System.currentTimeMillis();
        }
        long age = this.seen - previousSeen;
        if (previousSeen > 0 && age > 0 && age < maxAge / 2) {
            double alpha = 0.5d - (age / ((double) maxAge));
            this.level = (int) ((((double) this.level) * (1.0d - alpha)) + (((double) previousRssi) * alpha));
        }
    }

    public void setAutoJoinStatus(int status) {
        if (status < 0) {
            status = 0;
        }
        if (status == 0) {
            this.blackListTimestamp = 0L;
        } else if (status > this.autoJoinStatus) {
            this.blackListTimestamp = System.currentTimeMillis();
        }
        this.autoJoinStatus = status;
    }

    public boolean is24GHz() {
        return is24GHz(this.frequency);
    }

    public static boolean is24GHz(int freq) {
        return freq > 2400 && freq < 2500;
    }

    public boolean is5GHz() {
        return is5GHz(this.frequency);
    }

    public static boolean is5GHz(int freq) {
        return freq > 4900 && freq < 5900;
    }

    public static class InformationElement {
        public byte[] bytes;
        public int id;

        public InformationElement() {
        }

        public InformationElement(InformationElement rhs) {
            this.id = rhs.id;
            this.bytes = (byte[]) rhs.bytes.clone();
        }
    }

    public ScanResult(WifiSsid wifiSsid, String BSSID, String caps, int level, int frequency, long tsf) {
        this.wifiSsid = wifiSsid;
        this.SSID = wifiSsid != null ? wifiSsid.toString() : WifiSsid.NONE;
        this.BSSID = BSSID;
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = -1;
        this.distanceSdCm = -1;
    }

    public ScanResult(WifiSsid wifiSsid, String BSSID, String caps, int level, int frequency, long tsf, int distCm, int distSdCm) {
        this.wifiSsid = wifiSsid;
        this.SSID = wifiSsid != null ? wifiSsid.toString() : WifiSsid.NONE;
        this.BSSID = BSSID;
        this.capabilities = caps;
        this.level = level;
        this.frequency = frequency;
        this.timestamp = tsf;
        this.distanceCm = distCm;
        this.distanceSdCm = distSdCm;
    }

    public ScanResult(ScanResult source) {
        if (source != null) {
            this.wifiSsid = source.wifiSsid;
            this.SSID = source.SSID;
            this.BSSID = source.BSSID;
            this.capabilities = source.capabilities;
            this.level = source.level;
            this.frequency = source.frequency;
            this.timestamp = source.timestamp;
            this.distanceCm = source.distanceCm;
            this.distanceSdCm = source.distanceSdCm;
            this.seen = source.seen;
            this.autoJoinStatus = source.autoJoinStatus;
            this.untrusted = source.untrusted;
            this.numConnection = source.numConnection;
            this.numUsage = source.numUsage;
            this.numIpConfigFailures = source.numIpConfigFailures;
            this.isAutoJoinCandidate = source.isAutoJoinCandidate;
        }
    }

    public ScanResult() {
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        StringBuffer stringBufferAppend = sb.append("SSID: ").append(this.wifiSsid == null ? WifiSsid.NONE : this.wifiSsid).append(", BSSID: ").append(this.BSSID == null ? "<none>" : this.BSSID).append(", capabilities: ");
        String none = this.capabilities != null ? this.capabilities : "<none>";
        stringBufferAppend.append(none).append(", level: ").append(this.level).append(", frequency: ").append(this.frequency).append(", timestamp: ").append(this.timestamp);
        sb.append(", distance: ").append(this.distanceCm != -1 ? Integer.valueOf(this.distanceCm) : "?").append("(cm)");
        sb.append(", distanceSd: ").append(this.distanceSdCm != -1 ? Integer.valueOf(this.distanceSdCm) : "?").append("(cm)");
        if (this.autoJoinStatus != 0) {
            sb.append(", status: ").append(this.autoJoinStatus);
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (this.wifiSsid != null) {
            dest.writeInt(1);
            this.wifiSsid.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.BSSID);
        dest.writeString(this.capabilities);
        dest.writeInt(this.level);
        dest.writeInt(this.frequency);
        dest.writeLong(this.timestamp);
        dest.writeInt(this.distanceCm);
        dest.writeInt(this.distanceSdCm);
        dest.writeLong(this.seen);
        dest.writeInt(this.autoJoinStatus);
        dest.writeInt(this.untrusted ? 1 : 0);
        dest.writeInt(this.numConnection);
        dest.writeInt(this.numUsage);
        dest.writeInt(this.numIpConfigFailures);
        dest.writeInt(this.isAutoJoinCandidate);
        if (this.informationElements != null) {
            dest.writeInt(this.informationElements.length);
            for (int i = 0; i < this.informationElements.length; i++) {
                dest.writeInt(this.informationElements[i].id);
                dest.writeInt(this.informationElements[i].bytes.length);
                dest.writeByteArray(this.informationElements[i].bytes);
            }
            return;
        }
        dest.writeInt(0);
    }
}
