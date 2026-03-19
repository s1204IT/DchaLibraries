package android.net;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public final class IpSecAlgorithm implements Parcelable {
    public static final String AUTH_CRYPT_AES_GCM = "rfc4106(gcm(aes))";
    public static final String AUTH_HMAC_MD5 = "hmac(md5)";
    public static final String AUTH_HMAC_SHA1 = "hmac(sha1)";
    public static final String AUTH_HMAC_SHA256 = "hmac(sha256)";
    public static final String AUTH_HMAC_SHA384 = "hmac(sha384)";
    public static final String AUTH_HMAC_SHA512 = "hmac(sha512)";
    public static final Parcelable.Creator<IpSecAlgorithm> CREATOR = new Parcelable.Creator<IpSecAlgorithm>() {
        @Override
        public IpSecAlgorithm createFromParcel(Parcel parcel) {
            return new IpSecAlgorithm(parcel.readString(), parcel.createByteArray(), parcel.readInt());
        }

        @Override
        public IpSecAlgorithm[] newArray(int i) {
            return new IpSecAlgorithm[i];
        }
    };
    public static final String CRYPT_AES_CBC = "cbc(aes)";
    public static final String CRYPT_NULL = "ecb(cipher_null)";
    private static final String TAG = "IpSecAlgorithm";
    private final byte[] mKey;
    private final String mName;
    private final int mTruncLenBits;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AlgorithmName {
    }

    public IpSecAlgorithm(String str, byte[] bArr) {
        this(str, bArr, 0);
    }

    public IpSecAlgorithm(String str, byte[] bArr, int i) {
        this.mName = str;
        this.mKey = (byte[]) bArr.clone();
        this.mTruncLenBits = i;
        checkValidOrThrow(this.mName, this.mKey.length * 8, this.mTruncLenBits);
    }

    public String getName() {
        return this.mName;
    }

    public byte[] getKey() {
        return (byte[]) this.mKey.clone();
    }

    public int getTruncationLengthBits() {
        return this.mTruncLenBits;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeByteArray(this.mKey);
        parcel.writeInt(this.mTruncLenBits);
    }

    private static void checkValidOrThrow(String str, int i, int i2) {
        boolean z;
        boolean z2;
        boolean z3;
        z = true;
        switch (str) {
            case "cbc(aes)":
                z2 = i == 128 || i == 192 || i == 256;
                if (!z2) {
                    throw new IllegalArgumentException("Invalid key material keyLength: " + i);
                }
                if (!z) {
                    throw new IllegalArgumentException("Invalid truncation keyLength: " + i2);
                }
                return;
            case "hmac(md5)":
                z2 = i == 128;
                if (i2 < 96 || i2 > 128) {
                    z = false;
                }
                if (!z2) {
                }
                break;
            case "hmac(sha1)":
                z2 = i == 160;
                if (i2 < 96 || i2 > 160) {
                    z = false;
                }
                if (!z2) {
                }
                break;
            case "hmac(sha256)":
                z2 = i == 256;
                if (i2 < 96 || i2 > 256) {
                    z = false;
                }
                if (!z2) {
                }
                break;
            case "hmac(sha384)":
                z3 = i == 384;
                if (i2 < 192 || i2 > 384) {
                    z = false;
                }
                z2 = z3;
                if (!z2) {
                }
                break;
            case "hmac(sha512)":
                z3 = i == 512;
                if (i2 < 256 || i2 > 512) {
                    z = false;
                }
                z2 = z3;
                if (!z2) {
                }
                break;
            case "rfc4106(gcm(aes))":
                z2 = i == 160 || i == 224 || i == 288;
                if (i2 != 64 && i2 != 96 && i2 != 128) {
                    z = false;
                }
                if (!z2) {
                }
                break;
            default:
                throw new IllegalArgumentException("Couldn't find an algorithm: " + str);
        }
    }

    public boolean isAuthentication() {
        switch (getName()) {
            case "hmac(md5)":
            case "hmac(sha1)":
            case "hmac(sha256)":
            case "hmac(sha384)":
            case "hmac(sha512)":
                return true;
            default:
                return false;
        }
    }

    public boolean isEncryption() {
        return getName().equals(CRYPT_AES_CBC);
    }

    public boolean isAead() {
        return getName().equals(AUTH_CRYPT_AES_GCM);
    }

    private static boolean isUnsafeBuild() {
        return Build.IS_DEBUGGABLE && Build.IS_ENG;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{mName=");
        sb.append(this.mName);
        sb.append(", mKey=");
        sb.append(isUnsafeBuild() ? HexDump.toHexString(this.mKey) : "<hidden>");
        sb.append(", mTruncLenBits=");
        sb.append(this.mTruncLenBits);
        sb.append("}");
        return sb.toString();
    }

    @VisibleForTesting
    public static boolean equals(IpSecAlgorithm ipSecAlgorithm, IpSecAlgorithm ipSecAlgorithm2) {
        return (ipSecAlgorithm == null || ipSecAlgorithm2 == null) ? ipSecAlgorithm == ipSecAlgorithm2 : ipSecAlgorithm.mName.equals(ipSecAlgorithm2.mName) && Arrays.equals(ipSecAlgorithm.mKey, ipSecAlgorithm2.mKey) && ipSecAlgorithm.mTruncLenBits == ipSecAlgorithm2.mTruncLenBits;
    }
}
