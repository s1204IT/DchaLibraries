package android.net.lowpan;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.HexDump;
import java.util.Arrays;
import java.util.Objects;

public class LowpanCredential implements Parcelable {
    public static final Parcelable.Creator<LowpanCredential> CREATOR = new Parcelable.Creator<LowpanCredential>() {
        @Override
        public LowpanCredential createFromParcel(Parcel parcel) {
            LowpanCredential lowpanCredential = new LowpanCredential();
            lowpanCredential.mMasterKey = parcel.createByteArray();
            lowpanCredential.mMasterKeyIndex = parcel.readInt();
            return lowpanCredential;
        }

        @Override
        public LowpanCredential[] newArray(int i) {
            return new LowpanCredential[i];
        }
    };
    public static final int UNSPECIFIED_KEY_INDEX = 0;
    private byte[] mMasterKey = null;
    private int mMasterKeyIndex = 0;

    LowpanCredential() {
    }

    private LowpanCredential(byte[] bArr, int i) {
        setMasterKey(bArr, i);
    }

    private LowpanCredential(byte[] bArr) {
        setMasterKey(bArr);
    }

    public static LowpanCredential createMasterKey(byte[] bArr) {
        return new LowpanCredential(bArr);
    }

    public static LowpanCredential createMasterKey(byte[] bArr, int i) {
        return new LowpanCredential(bArr, i);
    }

    void setMasterKey(byte[] bArr) {
        if (bArr != null) {
            bArr = (byte[]) bArr.clone();
        }
        this.mMasterKey = bArr;
    }

    void setMasterKeyIndex(int i) {
        this.mMasterKeyIndex = i;
    }

    void setMasterKey(byte[] bArr, int i) {
        setMasterKey(bArr);
        setMasterKeyIndex(i);
    }

    public byte[] getMasterKey() {
        if (this.mMasterKey != null) {
            return (byte[]) this.mMasterKey.clone();
        }
        return null;
    }

    public int getMasterKeyIndex() {
        return this.mMasterKeyIndex;
    }

    public boolean isMasterKey() {
        return this.mMasterKey != null;
    }

    public String toSensitiveString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<LowpanCredential");
        if (isMasterKey()) {
            stringBuffer.append(" MasterKey:");
            stringBuffer.append(HexDump.toHexString(this.mMasterKey));
            if (this.mMasterKeyIndex != 0) {
                stringBuffer.append(", Index:");
                stringBuffer.append(this.mMasterKeyIndex);
            }
        } else {
            stringBuffer.append(" empty");
        }
        stringBuffer.append(">");
        return stringBuffer.toString();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<LowpanCredential");
        if (isMasterKey()) {
            stringBuffer.append(" MasterKey");
            if (this.mMasterKeyIndex != 0) {
                stringBuffer.append(", Index:");
                stringBuffer.append(this.mMasterKeyIndex);
            }
        } else {
            stringBuffer.append(" empty");
        }
        stringBuffer.append(">");
        return stringBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LowpanCredential)) {
            return false;
        }
        LowpanCredential lowpanCredential = (LowpanCredential) obj;
        return Arrays.equals(this.mMasterKey, lowpanCredential.mMasterKey) && this.mMasterKeyIndex == lowpanCredential.mMasterKeyIndex;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(Arrays.hashCode(this.mMasterKey)), Integer.valueOf(this.mMasterKeyIndex));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.mMasterKey);
        parcel.writeInt(this.mMasterKeyIndex);
    }
}
