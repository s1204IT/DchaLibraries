package android.net.nsd;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class DnsSdTxtRecord implements Parcelable {
    public static final Parcelable.Creator<DnsSdTxtRecord> CREATOR = new Parcelable.Creator<DnsSdTxtRecord>() {
        @Override
        public DnsSdTxtRecord createFromParcel(Parcel parcel) {
            DnsSdTxtRecord dnsSdTxtRecord = new DnsSdTxtRecord();
            parcel.readByteArray(dnsSdTxtRecord.mData);
            return dnsSdTxtRecord;
        }

        @Override
        public DnsSdTxtRecord[] newArray(int i) {
            return new DnsSdTxtRecord[i];
        }
    };
    private static final byte mSeperator = 61;
    private byte[] mData;

    public DnsSdTxtRecord() {
        this.mData = new byte[0];
    }

    public DnsSdTxtRecord(byte[] bArr) {
        this.mData = (byte[]) bArr.clone();
    }

    public DnsSdTxtRecord(DnsSdTxtRecord dnsSdTxtRecord) {
        if (dnsSdTxtRecord != null && dnsSdTxtRecord.mData != null) {
            this.mData = (byte[]) dnsSdTxtRecord.mData.clone();
        }
    }

    public void set(String str, String str2) {
        byte[] bytes;
        int length;
        if (str2 != null) {
            bytes = str2.getBytes();
            length = bytes.length;
        } else {
            bytes = null;
            length = 0;
        }
        try {
            byte[] bytes2 = str.getBytes("US-ASCII");
            for (byte b : bytes2) {
                if (b == 61) {
                    throw new IllegalArgumentException("= is not a valid character in key");
                }
            }
            if (bytes2.length + length >= 255) {
                throw new IllegalArgumentException("Key and Value length cannot exceed 255 bytes");
            }
            int iRemove = remove(str);
            if (iRemove == -1) {
                iRemove = keyCount();
            }
            insert(bytes2, bytes, iRemove);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("key should be US-ASCII");
        }
    }

    public String get(String str) {
        byte[] value = getValue(str);
        if (value != null) {
            return new String(value);
        }
        return null;
    }

    public int remove(String str) {
        int i = 0;
        int i2 = 0;
        while (i < this.mData.length) {
            int i3 = this.mData[i];
            if (str.length() <= i3 && ((str.length() == i3 || this.mData[str.length() + i + 1] == 61) && str.compareToIgnoreCase(new String(this.mData, i + 1, str.length())) == 0)) {
                byte[] bArr = this.mData;
                this.mData = new byte[(bArr.length - i3) - 1];
                System.arraycopy(bArr, 0, this.mData, 0, i);
                System.arraycopy(bArr, i + i3 + 1, this.mData, i, ((bArr.length - i) - i3) - 1);
                return i2;
            }
            i += (i3 + 1) & 255;
            i2++;
        }
        return -1;
    }

    public int keyCount() {
        int i = 0;
        int i2 = 0;
        while (i < this.mData.length) {
            i += 255 & (this.mData[i] + 1);
            i2++;
        }
        return i2;
    }

    public boolean contains(String str) {
        int i = 0;
        while (true) {
            String key = getKey(i);
            if (key == null) {
                return false;
            }
            if (str.compareToIgnoreCase(key) == 0) {
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

    private void insert(byte[] bArr, byte[] bArr2, int i) {
        byte[] bArr3 = this.mData;
        int length = bArr2 != null ? bArr2.length : 0;
        int i2 = 0;
        for (int i3 = 0; i3 < i && i2 < this.mData.length; i3++) {
            i2 += 255 & (this.mData[i2] + 1);
        }
        int length2 = bArr.length + length + (bArr2 != null ? 1 : 0);
        int length3 = bArr3.length + length2 + 1;
        this.mData = new byte[length3];
        System.arraycopy(bArr3, 0, this.mData, 0, i2);
        int length4 = bArr3.length - i2;
        System.arraycopy(bArr3, i2, this.mData, length3 - length4, length4);
        this.mData[i2] = (byte) length2;
        int i4 = i2 + 1;
        System.arraycopy(bArr, 0, this.mData, i4, bArr.length);
        if (bArr2 != null) {
            this.mData[i4 + bArr.length] = mSeperator;
            System.arraycopy(bArr2, 0, this.mData, i2 + bArr.length + 2, length);
        }
    }

    private String getKey(int i) {
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < i && i3 < this.mData.length; i4++) {
            i3 += this.mData[i3] + 1;
        }
        if (i3 < this.mData.length) {
            byte b = this.mData[i3];
            while (i2 < b && this.mData[i3 + i2 + 1] != 61) {
                i2++;
            }
            return new String(this.mData, i3 + 1, i2);
        }
        return null;
    }

    private byte[] getValue(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < i && i2 < this.mData.length; i3++) {
            i2 += this.mData[i2] + 1;
        }
        if (i2 < this.mData.length) {
            int i4 = this.mData[i2];
            for (int i5 = 0; i5 < i4; i5++) {
                int i6 = i2 + i5;
                if (this.mData[i6 + 1] == 61) {
                    int i7 = (i4 - i5) - 1;
                    byte[] bArr = new byte[i7];
                    System.arraycopy(this.mData, i6 + 2, bArr, 0, i7);
                    return bArr;
                }
            }
        }
        return null;
    }

    private String getValueAsString(int i) {
        byte[] value = getValue(i);
        if (value != null) {
            return new String(value);
        }
        return null;
    }

    private byte[] getValue(String str) {
        int i = 0;
        while (true) {
            String key = getKey(i);
            if (key != null) {
                if (str.compareToIgnoreCase(key) != 0) {
                    i++;
                } else {
                    return getValue(i);
                }
            } else {
                return null;
            }
        }
    }

    public String toString() {
        String str;
        String str2 = null;
        int i = 0;
        while (true) {
            String key = getKey(i);
            if (key == null) {
                break;
            }
            String str3 = "{" + key;
            String valueAsString = getValueAsString(i);
            if (valueAsString != null) {
                str = str3 + "=" + valueAsString + "}";
            } else {
                str = str3 + "}";
            }
            if (str2 == null) {
                str2 = str;
            } else {
                str2 = str2 + ", " + str;
            }
            i++;
        }
        return str2 != null ? str2 : "";
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DnsSdTxtRecord)) {
            return false;
        }
        return Arrays.equals(((DnsSdTxtRecord) obj).mData, this.mData);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.mData);
    }
}
