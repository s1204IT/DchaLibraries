package android.net.lowpan;

import android.icu.text.StringPrep;
import android.icu.text.StringPrepParseException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.android.internal.util.HexDump;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class LowpanIdentity implements Parcelable {
    public static final int UNSPECIFIED_CHANNEL = -1;
    public static final int UNSPECIFIED_PANID = -1;
    private static final String TAG = LowpanIdentity.class.getSimpleName();
    public static final Parcelable.Creator<LowpanIdentity> CREATOR = new Parcelable.Creator<LowpanIdentity>() {
        @Override
        public LowpanIdentity createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            builder.setRawName(parcel.createByteArray());
            builder.setType(parcel.readString());
            builder.setXpanid(parcel.createByteArray());
            builder.setPanid(parcel.readInt());
            builder.setChannel(parcel.readInt());
            return builder.build();
        }

        @Override
        public LowpanIdentity[] newArray(int i) {
            return new LowpanIdentity[i];
        }
    };
    private String mName = "";
    private boolean mIsNameValid = true;
    private byte[] mRawName = new byte[0];
    private String mType = "";
    private byte[] mXpanid = new byte[0];
    private int mPanid = -1;
    private int mChannel = -1;

    public static class Builder {
        private static final StringPrep stringPrep = StringPrep.getInstance(8);
        final LowpanIdentity mIdentity = new LowpanIdentity();

        private static String escape(byte[] bArr) {
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bArr) {
                if (b >= 32 && b <= 126) {
                    stringBuffer.append((char) b);
                } else {
                    stringBuffer.append(String.format("\\0x%02x", Integer.valueOf(b & 255)));
                }
            }
            return stringBuffer.toString();
        }

        public Builder setLowpanIdentity(LowpanIdentity lowpanIdentity) {
            Objects.requireNonNull(lowpanIdentity);
            setRawName(lowpanIdentity.getRawName());
            setXpanid(lowpanIdentity.getXpanid());
            setPanid(lowpanIdentity.getPanid());
            setChannel(lowpanIdentity.getChannel());
            setType(lowpanIdentity.getType());
            return this;
        }

        public Builder setName(String str) {
            Objects.requireNonNull(str);
            try {
                this.mIdentity.mName = stringPrep.prepare(str, 0);
                this.mIdentity.mRawName = this.mIdentity.mName.getBytes(StandardCharsets.UTF_8);
                this.mIdentity.mIsNameValid = true;
            } catch (StringPrepParseException e) {
                Log.w(LowpanIdentity.TAG, e.toString());
                setRawName(str.getBytes(StandardCharsets.UTF_8));
            }
            return this;
        }

        public Builder setRawName(byte[] bArr) {
            Objects.requireNonNull(bArr);
            this.mIdentity.mRawName = (byte[]) bArr.clone();
            this.mIdentity.mName = new String(bArr, StandardCharsets.UTF_8);
            try {
                this.mIdentity.mIsNameValid = Arrays.equals(stringPrep.prepare(this.mIdentity.mName, 0).getBytes(StandardCharsets.UTF_8), bArr);
            } catch (StringPrepParseException e) {
                Log.w(LowpanIdentity.TAG, e.toString());
                this.mIdentity.mIsNameValid = false;
            }
            if (!this.mIdentity.mIsNameValid) {
                this.mIdentity.mName = "«" + escape(bArr) + "»";
            }
            return this;
        }

        public Builder setXpanid(byte[] bArr) {
            this.mIdentity.mXpanid = bArr != null ? (byte[]) bArr.clone() : null;
            return this;
        }

        public Builder setPanid(int i) {
            this.mIdentity.mPanid = i;
            return this;
        }

        public Builder setType(String str) {
            this.mIdentity.mType = str;
            return this;
        }

        public Builder setChannel(int i) {
            this.mIdentity.mChannel = i;
            return this;
        }

        public LowpanIdentity build() {
            return this.mIdentity;
        }
    }

    LowpanIdentity() {
    }

    public String getName() {
        return this.mName;
    }

    public boolean isNameValid() {
        return this.mIsNameValid;
    }

    public byte[] getRawName() {
        return (byte[]) this.mRawName.clone();
    }

    public byte[] getXpanid() {
        return (byte[]) this.mXpanid.clone();
    }

    public int getPanid() {
        return this.mPanid;
    }

    public String getType() {
        return this.mType;
    }

    public int getChannel() {
        return this.mChannel;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Name:");
        stringBuffer.append(getName());
        if (this.mType.length() > 0) {
            stringBuffer.append(", Type:");
            stringBuffer.append(this.mType);
        }
        if (this.mXpanid.length > 0) {
            stringBuffer.append(", XPANID:");
            stringBuffer.append(HexDump.toHexString(this.mXpanid));
        }
        if (this.mPanid != -1) {
            stringBuffer.append(", PANID:");
            stringBuffer.append(String.format("0x%04X", Integer.valueOf(this.mPanid)));
        }
        if (this.mChannel != -1) {
            stringBuffer.append(", Channel:");
            stringBuffer.append(this.mChannel);
        }
        return stringBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LowpanIdentity)) {
            return false;
        }
        LowpanIdentity lowpanIdentity = (LowpanIdentity) obj;
        return Arrays.equals(this.mRawName, lowpanIdentity.mRawName) && Arrays.equals(this.mXpanid, lowpanIdentity.mXpanid) && this.mType.equals(lowpanIdentity.mType) && this.mPanid == lowpanIdentity.mPanid && this.mChannel == lowpanIdentity.mChannel;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(Arrays.hashCode(this.mRawName)), this.mType, Integer.valueOf(Arrays.hashCode(this.mXpanid)), Integer.valueOf(this.mPanid), Integer.valueOf(this.mChannel));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.mRawName);
        parcel.writeString(this.mType);
        parcel.writeByteArray(this.mXpanid);
        parcel.writeInt(this.mPanid);
        parcel.writeInt(this.mChannel);
    }
}
