package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import android.util.Log;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

public final class ImsiEncryptionInfo implements Parcelable {
    public static final Parcelable.Creator<ImsiEncryptionInfo> CREATOR = new Parcelable.Creator<ImsiEncryptionInfo>() {
        @Override
        public ImsiEncryptionInfo createFromParcel(Parcel parcel) {
            return new ImsiEncryptionInfo(parcel);
        }

        @Override
        public ImsiEncryptionInfo[] newArray(int i) {
            return new ImsiEncryptionInfo[i];
        }
    };
    private static final String LOG_TAG = "ImsiEncryptionInfo";
    private final Date expirationTime;
    private final String keyIdentifier;
    private final int keyType;
    private final String mcc;
    private final String mnc;
    private final PublicKey publicKey;

    public ImsiEncryptionInfo(String str, String str2, int i, String str3, byte[] bArr, Date date) {
        this(str, str2, i, str3, makeKeyObject(bArr), date);
    }

    public ImsiEncryptionInfo(String str, String str2, int i, String str3, PublicKey publicKey, Date date) {
        this.mcc = str;
        this.mnc = str2;
        this.keyType = i;
        this.publicKey = publicKey;
        this.keyIdentifier = str3;
        this.expirationTime = date;
    }

    public ImsiEncryptionInfo(Parcel parcel) {
        byte[] bArr = new byte[parcel.readInt()];
        parcel.readByteArray(bArr);
        this.publicKey = makeKeyObject(bArr);
        this.mcc = parcel.readString();
        this.mnc = parcel.readString();
        this.keyIdentifier = parcel.readString();
        this.keyType = parcel.readInt();
        this.expirationTime = new Date(parcel.readLong());
    }

    public String getMnc() {
        return this.mnc;
    }

    public String getMcc() {
        return this.mcc;
    }

    public String getKeyIdentifier() {
        return this.keyIdentifier;
    }

    public int getKeyType() {
        return this.keyType;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public Date getExpirationTime() {
        return this.expirationTime;
    }

    private static PublicKey makeKeyObject(byte[] bArr) {
        try {
            return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA).generatePublic(new X509EncodedKeySpec(bArr));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(LOG_TAG, "Error makeKeyObject: unable to convert into PublicKey", e);
            throw new IllegalArgumentException();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        byte[] encoded = this.publicKey.getEncoded();
        parcel.writeInt(encoded.length);
        parcel.writeByteArray(encoded);
        parcel.writeString(this.mcc);
        parcel.writeString(this.mnc);
        parcel.writeString(this.keyIdentifier);
        parcel.writeInt(this.keyType);
        parcel.writeLong(this.expirationTime.getTime());
    }

    public String toString() {
        return "[ImsiEncryptionInfo mcc=" + this.mcc + "mnc=" + this.mnc + "publicKey=" + this.publicKey + ", keyIdentifier=" + this.keyIdentifier + ", keyType=" + this.keyType + ", expirationTime=" + this.expirationTime + "]";
    }
}
