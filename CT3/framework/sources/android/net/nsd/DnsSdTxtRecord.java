package android.net.nsd;

import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class DnsSdTxtRecord implements Parcelable {
    public static final Parcelable.Creator<DnsSdTxtRecord> CREATOR = new Parcelable.Creator<DnsSdTxtRecord>() {
        @Override
        public DnsSdTxtRecord createFromParcel(Parcel in) {
            DnsSdTxtRecord info = new DnsSdTxtRecord();
            in.readByteArray(info.mData);
            return info;
        }

        @Override
        public DnsSdTxtRecord[] newArray(int size) {
            return new DnsSdTxtRecord[size];
        }
    };
    private static final byte mSeperator = 61;
    private byte[] mData;

    public DnsSdTxtRecord() {
        this.mData = new byte[0];
    }

    public DnsSdTxtRecord(byte[] data) {
        this.mData = (byte[]) data.clone();
    }

    public DnsSdTxtRecord(DnsSdTxtRecord src) {
        if (src == null || src.mData == null) {
            return;
        }
        this.mData = (byte[]) src.mData.clone();
    }

    public void set(String key, String value) {
        byte[] valBytes;
        int valLen;
        if (value != null) {
            valBytes = value.getBytes();
            valLen = valBytes.length;
        } else {
            valBytes = null;
            valLen = 0;
        }
        try {
            byte[] keyBytes = key.getBytes("US-ASCII");
            for (byte b : keyBytes) {
                if (b == 61) {
                    throw new IllegalArgumentException("= is not a valid character in key");
                }
            }
            if (keyBytes.length + valLen >= 255) {
                throw new IllegalArgumentException("Key and Value length cannot exceed 255 bytes");
            }
            int currentLoc = remove(key);
            if (currentLoc == -1) {
                currentLoc = keyCount();
            }
            insert(keyBytes, valBytes, currentLoc);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("key should be US-ASCII");
        }
    }

    public String get(String key) {
        byte[] val = getValue(key);
        if (val != null) {
            return new String(val);
        }
        return null;
    }

    public int remove(String key) {
        int avStart = 0;
        int i = 0;
        while (avStart < this.mData.length) {
            int avLen = this.mData[avStart];
            if (key.length() <= avLen && (key.length() == avLen || this.mData[key.length() + avStart + 1] == 61)) {
                String s = new String(this.mData, avStart + 1, key.length());
                if (key.compareToIgnoreCase(s) == 0) {
                    byte[] oldBytes = this.mData;
                    this.mData = new byte[(oldBytes.length - avLen) - 1];
                    System.arraycopy(oldBytes, 0, this.mData, 0, avStart);
                    System.arraycopy(oldBytes, avStart + avLen + 1, this.mData, avStart, ((oldBytes.length - avStart) - avLen) - 1);
                    return i;
                }
            }
            avStart += (avLen + 1) & 255;
            i++;
        }
        return -1;
    }

    public int keyCount() {
        int count = 0;
        int nextKey = 0;
        while (nextKey < this.mData.length) {
            nextKey += (this.mData[nextKey] + 1) & 255;
            count++;
        }
        return count;
    }

    public boolean contains(String key) {
        int i = 0;
        while (true) {
            String s = getKey(i);
            if (s == null) {
                return false;
            }
            if (key.compareToIgnoreCase(s) == 0) {
                return true;
            }
            i++;
        }
    }

    public int size() {
        return this.mData.length;
    }

    public byte[] getRawData() {
        return (byte[]) this.mData.clone();
    }

    private void insert(byte[] keyBytes, byte[] value, int index) {
        byte[] oldBytes = this.mData;
        int valLen = value != null ? value.length : 0;
        int insertion = 0;
        for (int i = 0; i < index && insertion < this.mData.length; i++) {
            insertion += (this.mData[insertion] + 1) & 255;
        }
        int avLen = keyBytes.length + valLen + (value != null ? 1 : 0);
        int newLen = oldBytes.length + avLen + 1;
        this.mData = new byte[newLen];
        System.arraycopy(oldBytes, 0, this.mData, 0, insertion);
        int secondHalfLen = oldBytes.length - insertion;
        System.arraycopy(oldBytes, insertion, this.mData, newLen - secondHalfLen, secondHalfLen);
        this.mData[insertion] = (byte) avLen;
        System.arraycopy(keyBytes, 0, this.mData, insertion + 1, keyBytes.length);
        if (value == null) {
            return;
        }
        this.mData[insertion + 1 + keyBytes.length] = mSeperator;
        System.arraycopy(value, 0, this.mData, keyBytes.length + insertion + 2, valLen);
    }

    private String getKey(int index) {
        int avStart = 0;
        for (int i = 0; i < index && avStart < this.mData.length; i++) {
            avStart += this.mData[avStart] + 1;
        }
        if (avStart < this.mData.length) {
            int avLen = this.mData[avStart];
            int aLen = 0;
            while (aLen < avLen && this.mData[avStart + aLen + 1] != 61) {
                aLen++;
            }
            return new String(this.mData, avStart + 1, aLen);
        }
        return null;
    }

    private byte[] getValue(int index) {
        int avStart = 0;
        for (int i = 0; i < index && avStart < this.mData.length; i++) {
            avStart += this.mData[avStart] + 1;
        }
        if (avStart >= this.mData.length) {
            return null;
        }
        int avLen = this.mData[avStart];
        for (int aLen = 0; aLen < avLen; aLen++) {
            if (this.mData[avStart + aLen + 1] == 61) {
                byte[] value = new byte[(avLen - aLen) - 1];
                System.arraycopy(this.mData, avStart + aLen + 2, value, 0, (avLen - aLen) - 1);
                return value;
            }
        }
        return null;
    }

    private String getValueAsString(int index) {
        byte[] value = getValue(index);
        if (value != null) {
            return new String(value);
        }
        return null;
    }

    private byte[] getValue(String forKey) {
        int i = 0;
        while (true) {
            String s = getKey(i);
            if (s == null) {
                return null;
            }
            if (forKey.compareToIgnoreCase(s) != 0) {
                i++;
            } else {
                return getValue(i);
            }
        }
    }

    public String toString() {
        String av;
        String result = null;
        int i = 0;
        while (true) {
            String a = getKey(i);
            if (a == null) {
                break;
            }
            String av2 = "{" + a;
            String val = getValueAsString(i);
            if (val != null) {
                av = av2 + "=" + val + "}";
            } else {
                av = av2 + "}";
            }
            if (result == null) {
                result = av;
            } else {
                result = result + ", " + av;
            }
            i++;
        }
        return result != null ? result : ProxyInfo.LOCAL_EXCL_LIST;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DnsSdTxtRecord)) {
            return false;
        }
        return Arrays.equals(obj.mData, this.mData);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.mData);
    }
}
