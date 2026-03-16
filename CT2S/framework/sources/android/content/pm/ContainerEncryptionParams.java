package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Slog;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

@Deprecated
public class ContainerEncryptionParams implements Parcelable {
    public static final Parcelable.Creator<ContainerEncryptionParams> CREATOR = new Parcelable.Creator<ContainerEncryptionParams>() {
        @Override
        public ContainerEncryptionParams createFromParcel(Parcel source) {
            try {
                return new ContainerEncryptionParams(source);
            } catch (InvalidAlgorithmParameterException e) {
                Slog.e(ContainerEncryptionParams.TAG, "Invalid algorithm parameters specified", e);
                return null;
            }
        }

        @Override
        public ContainerEncryptionParams[] newArray(int size) {
            return new ContainerEncryptionParams[size];
        }
    };
    private static final int ENC_PARAMS_IV_PARAMETERS = 1;
    private static final int MAC_PARAMS_NONE = 1;
    protected static final String TAG = "ContainerEncryptionParams";
    private static final String TO_STRING_PREFIX = "ContainerEncryptionParams{";
    private final long mAuthenticatedDataStart;
    private final long mDataEnd;
    private final long mEncryptedDataStart;
    private final String mEncryptionAlgorithm;
    private final SecretKey mEncryptionKey;
    private final IvParameterSpec mEncryptionSpec;
    private final String mMacAlgorithm;
    private final SecretKey mMacKey;
    private final AlgorithmParameterSpec mMacSpec;
    private final byte[] mMacTag;

    public ContainerEncryptionParams(String encryptionAlgorithm, AlgorithmParameterSpec encryptionSpec, SecretKey encryptionKey) throws InvalidAlgorithmParameterException {
        this(encryptionAlgorithm, encryptionSpec, encryptionKey, null, null, null, null, -1L, -1L, -1L);
    }

    public ContainerEncryptionParams(String encryptionAlgorithm, AlgorithmParameterSpec encryptionSpec, SecretKey encryptionKey, String macAlgorithm, AlgorithmParameterSpec macSpec, SecretKey macKey, byte[] macTag, long authenticatedDataStart, long encryptedDataStart, long dataEnd) throws InvalidAlgorithmParameterException {
        if (TextUtils.isEmpty(encryptionAlgorithm)) {
            throw new NullPointerException("algorithm == null");
        }
        if (encryptionSpec == null) {
            throw new NullPointerException("encryptionSpec == null");
        }
        if (encryptionKey == null) {
            throw new NullPointerException("encryptionKey == null");
        }
        if (!TextUtils.isEmpty(macAlgorithm) && macKey == null) {
            throw new NullPointerException("macKey == null");
        }
        if (!(encryptionSpec instanceof IvParameterSpec)) {
            throw new InvalidAlgorithmParameterException("Unknown parameter spec class; must be IvParameters");
        }
        this.mEncryptionAlgorithm = encryptionAlgorithm;
        this.mEncryptionSpec = (IvParameterSpec) encryptionSpec;
        this.mEncryptionKey = encryptionKey;
        this.mMacAlgorithm = macAlgorithm;
        this.mMacSpec = macSpec;
        this.mMacKey = macKey;
        this.mMacTag = macTag;
        this.mAuthenticatedDataStart = authenticatedDataStart;
        this.mEncryptedDataStart = encryptedDataStart;
        this.mDataEnd = dataEnd;
    }

    public String getEncryptionAlgorithm() {
        return this.mEncryptionAlgorithm;
    }

    public AlgorithmParameterSpec getEncryptionSpec() {
        return this.mEncryptionSpec;
    }

    public SecretKey getEncryptionKey() {
        return this.mEncryptionKey;
    }

    public String getMacAlgorithm() {
        return this.mMacAlgorithm;
    }

    public AlgorithmParameterSpec getMacSpec() {
        return this.mMacSpec;
    }

    public SecretKey getMacKey() {
        return this.mMacKey;
    }

    public byte[] getMacTag() {
        return this.mMacTag;
    }

    public long getAuthenticatedDataStart() {
        return this.mAuthenticatedDataStart;
    }

    public long getEncryptedDataStart() {
        return this.mEncryptedDataStart;
    }

    public long getDataEnd() {
        return this.mDataEnd;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContainerEncryptionParams)) {
            return false;
        }
        ContainerEncryptionParams other = (ContainerEncryptionParams) o;
        if (this.mAuthenticatedDataStart != other.mAuthenticatedDataStart || this.mEncryptedDataStart != other.mEncryptedDataStart || this.mDataEnd != other.mDataEnd) {
            return false;
        }
        if (!this.mEncryptionAlgorithm.equals(other.mEncryptionAlgorithm) || !this.mMacAlgorithm.equals(other.mMacAlgorithm)) {
            return false;
        }
        if (isSecretKeyEqual(this.mEncryptionKey, other.mEncryptionKey) && isSecretKeyEqual(this.mMacKey, other.mMacKey)) {
            return Arrays.equals(this.mEncryptionSpec.getIV(), other.mEncryptionSpec.getIV()) && Arrays.equals(this.mMacTag, other.mMacTag) && this.mMacSpec == other.mMacSpec;
        }
        return false;
    }

    private static final boolean isSecretKeyEqual(SecretKey key1, SecretKey key2) {
        String keyFormat = key1.getFormat();
        String otherKeyFormat = key2.getFormat();
        if (keyFormat == null) {
            if (keyFormat != otherKeyFormat || key1.getEncoded() != key2.getEncoded()) {
                return false;
            }
        } else if (!keyFormat.equals(key2.getFormat()) || !Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hash = 3 + (this.mEncryptionAlgorithm.hashCode() * 5);
        return (int) (((long) ((int) (((long) ((int) (((long) (hash + (Arrays.hashCode(this.mEncryptionSpec.getIV()) * 7) + (this.mEncryptionKey.hashCode() * 11) + (this.mMacAlgorithm.hashCode() * 13) + (this.mMacKey.hashCode() * 17) + (Arrays.hashCode(this.mMacTag) * 19))) + (23 * this.mAuthenticatedDataStart)))) + (29 * this.mEncryptedDataStart)))) + (31 * this.mDataEnd));
    }

    public String toString() {
        return TO_STRING_PREFIX + "mEncryptionAlgorithm=\"" + this.mEncryptionAlgorithm + "\",mEncryptionSpec=" + this.mEncryptionSpec.toString() + "mEncryptionKey=" + this.mEncryptionKey.toString() + "mMacAlgorithm=\"" + this.mMacAlgorithm + "\",mMacSpec=" + this.mMacSpec.toString() + "mMacKey=" + this.mMacKey.toString() + ",mAuthenticatedDataStart=" + this.mAuthenticatedDataStart + ",mEncryptedDataStart=" + this.mEncryptedDataStart + ",mDataEnd=" + this.mDataEnd + '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mEncryptionAlgorithm);
        dest.writeInt(1);
        dest.writeByteArray(this.mEncryptionSpec.getIV());
        dest.writeSerializable(this.mEncryptionKey);
        dest.writeString(this.mMacAlgorithm);
        dest.writeInt(1);
        dest.writeByteArray(new byte[0]);
        dest.writeSerializable(this.mMacKey);
        dest.writeByteArray(this.mMacTag);
        dest.writeLong(this.mAuthenticatedDataStart);
        dest.writeLong(this.mEncryptedDataStart);
        dest.writeLong(this.mDataEnd);
    }

    private ContainerEncryptionParams(Parcel source) throws InvalidAlgorithmParameterException {
        this.mEncryptionAlgorithm = source.readString();
        int encParamType = source.readInt();
        byte[] encParamsEncoded = source.createByteArray();
        this.mEncryptionKey = (SecretKey) source.readSerializable();
        this.mMacAlgorithm = source.readString();
        int macParamType = source.readInt();
        source.createByteArray();
        this.mMacKey = (SecretKey) source.readSerializable();
        this.mMacTag = source.createByteArray();
        this.mAuthenticatedDataStart = source.readLong();
        this.mEncryptedDataStart = source.readLong();
        this.mDataEnd = source.readLong();
        switch (encParamType) {
            case 1:
                this.mEncryptionSpec = new IvParameterSpec(encParamsEncoded);
                switch (macParamType) {
                    case 1:
                        this.mMacSpec = null;
                        if (this.mEncryptionKey == null) {
                            throw new NullPointerException("encryptionKey == null");
                        }
                        return;
                    default:
                        throw new InvalidAlgorithmParameterException("Unknown parameter type " + macParamType);
                }
            default:
                throw new InvalidAlgorithmParameterException("Unknown parameter type " + encParamType);
        }
    }
}
