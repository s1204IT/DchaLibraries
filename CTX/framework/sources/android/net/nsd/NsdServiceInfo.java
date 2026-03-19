package android.net.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public final class NsdServiceInfo implements Parcelable {
    public static final Parcelable.Creator<NsdServiceInfo> CREATOR = new Parcelable.Creator<NsdServiceInfo>() {
        @Override
        public NsdServiceInfo createFromParcel(Parcel parcel) {
            NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
            nsdServiceInfo.mServiceName = parcel.readString();
            nsdServiceInfo.mServiceType = parcel.readString();
            if (parcel.readInt() == 1) {
                try {
                    nsdServiceInfo.mHost = InetAddress.getByAddress(parcel.createByteArray());
                } catch (UnknownHostException e) {
                }
            }
            nsdServiceInfo.mPort = parcel.readInt();
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                byte[] bArr = null;
                if (parcel.readInt() == 1) {
                    bArr = new byte[parcel.readInt()];
                    parcel.readByteArray(bArr);
                }
                nsdServiceInfo.mTxtRecord.put(parcel.readString(), bArr);
            }
            return nsdServiceInfo;
        }

        @Override
        public NsdServiceInfo[] newArray(int i) {
            return new NsdServiceInfo[i];
        }
    };
    private static final String TAG = "NsdServiceInfo";
    private InetAddress mHost;
    private int mPort;
    private String mServiceName;
    private String mServiceType;
    private final ArrayMap<String, byte[]> mTxtRecord = new ArrayMap<>();

    public NsdServiceInfo() {
    }

    public NsdServiceInfo(String str, String str2) {
        this.mServiceName = str;
        this.mServiceType = str2;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    public void setServiceName(String str) {
        this.mServiceName = str;
    }

    public String getServiceType() {
        return this.mServiceType;
    }

    public void setServiceType(String str) {
        this.mServiceType = str;
    }

    public InetAddress getHost() {
        return this.mHost;
    }

    public void setHost(InetAddress inetAddress) {
        this.mHost = inetAddress;
    }

    public int getPort() {
        return this.mPort;
    }

    public void setPort(int i) {
        this.mPort = i;
    }

    public void setTxtRecords(String str) {
        String str2;
        byte[] bArr;
        byte[] bArrDecode = Base64.decode(str, 0);
        int i = 0;
        while (i < bArrDecode.length) {
            int length = bArrDecode[i] & 255;
            int i2 = i + 1;
            if (length == 0) {
                throw new IllegalArgumentException("Zero sized txt record");
            }
            try {
                if (i2 + length > bArrDecode.length) {
                    Log.w(TAG, "Corrupt record length (pos = " + i2 + "): " + length);
                    length = bArrDecode.length - i2;
                }
                int i3 = 0;
                str2 = null;
                bArr = null;
                for (int i4 = i2; i4 < i2 + length; i4++) {
                    if (str2 == null) {
                        if (bArrDecode[i4] == 61) {
                            str2 = new String(bArrDecode, i2, i4 - i2, StandardCharsets.US_ASCII);
                        }
                    } else {
                        if (bArr == null) {
                            bArr = new byte[(length - str2.length()) - 1];
                        }
                        bArr[i3] = bArrDecode[i4];
                        i3++;
                    }
                }
                if (str2 == null) {
                    str2 = new String(bArrDecode, i2, length, StandardCharsets.US_ASCII);
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "While parsing txt records (pos = " + i2 + "): " + e.getMessage());
            }
            if (TextUtils.isEmpty(str2)) {
                throw new IllegalArgumentException("Invalid txt record (key is empty)");
            }
            if (getAttributes().containsKey(str2)) {
                throw new IllegalArgumentException("Invalid txt record (duplicate key \"" + str2 + "\")");
            }
            setAttribute(str2, bArr);
            i = i2 + length;
        }
    }

    public void setAttribute(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < ' ' || cCharAt > '~') {
                throw new IllegalArgumentException("Key strings must be printable US-ASCII");
            }
            if (cCharAt == '=') {
                throw new IllegalArgumentException("Key strings must not include '='");
            }
        }
        if (str.length() + (bArr == null ? 0 : bArr.length) >= 255) {
            throw new IllegalArgumentException("Key length + value length must be < 255 bytes");
        }
        if (str.length() > 9) {
            Log.w(TAG, "Key lengths > 9 are discouraged: " + str);
        }
        int txtRecordSize = getTxtRecordSize() + str.length() + (bArr != null ? bArr.length : 0) + 2;
        if (txtRecordSize > 1300) {
            throw new IllegalArgumentException("Total length of attributes must be < 1300 bytes");
        }
        if (txtRecordSize > 400) {
            Log.w(TAG, "Total length of all attributes exceeds 400 bytes; truncation may occur");
        }
        this.mTxtRecord.put(str, bArr);
    }

    public void setAttribute(String str, String str2) {
        try {
            setAttribute(str, str2 == null ? (byte[]) null : str2.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Value must be UTF-8");
        }
    }

    public void removeAttribute(String str) {
        this.mTxtRecord.remove(str);
    }

    public Map<String, byte[]> getAttributes() {
        return Collections.unmodifiableMap(this.mTxtRecord);
    }

    private int getTxtRecordSize() {
        int length = 0;
        for (Map.Entry<String, byte[]> entry : this.mTxtRecord.entrySet()) {
            int length2 = length + 2 + entry.getKey().length();
            byte[] value = entry.getValue();
            length = length2 + (value == null ? 0 : value.length);
        }
        return length;
    }

    public byte[] getTxtRecord() {
        int txtRecordSize = getTxtRecordSize();
        if (txtRecordSize == 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[txtRecordSize];
        int length = 0;
        for (Map.Entry<String, byte[]> entry : this.mTxtRecord.entrySet()) {
            String key = entry.getKey();
            byte[] value = entry.getValue();
            int i = length + 1;
            bArr[length] = (byte) (key.length() + (value == null ? 0 : value.length) + 1);
            System.arraycopy(key.getBytes(StandardCharsets.US_ASCII), 0, bArr, i, key.length());
            int length2 = i + key.length();
            length = length2 + 1;
            bArr[length2] = 61;
            if (value != null) {
                System.arraycopy(value, 0, bArr, length, value.length);
                length += value.length;
            }
        }
        return bArr;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("name: ");
        stringBuffer.append(this.mServiceName);
        stringBuffer.append(", type: ");
        stringBuffer.append(this.mServiceType);
        stringBuffer.append(", host: ");
        stringBuffer.append(this.mHost);
        stringBuffer.append(", port: ");
        stringBuffer.append(this.mPort);
        byte[] txtRecord = getTxtRecord();
        if (txtRecord != null) {
            stringBuffer.append(", txtRecord: ");
            stringBuffer.append(new String(txtRecord, StandardCharsets.UTF_8));
        }
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mServiceName);
        parcel.writeString(this.mServiceType);
        if (this.mHost != null) {
            parcel.writeInt(1);
            parcel.writeByteArray(this.mHost.getAddress());
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.mPort);
        parcel.writeInt(this.mTxtRecord.size());
        for (String str : this.mTxtRecord.keySet()) {
            byte[] bArr = this.mTxtRecord.get(str);
            if (bArr != null) {
                parcel.writeInt(1);
                parcel.writeInt(bArr.length);
                parcel.writeByteArray(bArr);
            } else {
                parcel.writeInt(0);
            }
            parcel.writeString(str);
        }
    }
}
