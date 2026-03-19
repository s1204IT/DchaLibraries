package android.security.keystore.recovery;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SystemApi
public final class KeyDerivationParams implements Parcelable {
    public static final int ALGORITHM_SCRYPT = 2;
    public static final int ALGORITHM_SHA256 = 1;
    public static final Parcelable.Creator<KeyDerivationParams> CREATOR = new Parcelable.Creator<KeyDerivationParams>() {
        @Override
        public KeyDerivationParams createFromParcel(Parcel parcel) {
            return new KeyDerivationParams(parcel);
        }

        @Override
        public KeyDerivationParams[] newArray(int i) {
            return new KeyDerivationParams[i];
        }
    };
    private final int mAlgorithm;
    private final int mMemoryDifficulty;
    private final byte[] mSalt;

    @Retention(RetentionPolicy.SOURCE)
    public @interface KeyDerivationAlgorithm {
    }

    public static KeyDerivationParams createSha256Params(byte[] bArr) {
        return new KeyDerivationParams(1, bArr);
    }

    public static KeyDerivationParams createScryptParams(byte[] bArr, int i) {
        return new KeyDerivationParams(2, bArr, i);
    }

    private KeyDerivationParams(int i, byte[] bArr) {
        this(i, bArr, -1);
    }

    private KeyDerivationParams(int i, byte[] bArr, int i2) {
        this.mAlgorithm = i;
        this.mSalt = (byte[]) Preconditions.checkNotNull(bArr);
        this.mMemoryDifficulty = i2;
    }

    public int getAlgorithm() {
        return this.mAlgorithm;
    }

    public byte[] getSalt() {
        return this.mSalt;
    }

    public int getMemoryDifficulty() {
        return this.mMemoryDifficulty;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAlgorithm);
        parcel.writeByteArray(this.mSalt);
        parcel.writeInt(this.mMemoryDifficulty);
    }

    protected KeyDerivationParams(Parcel parcel) {
        this.mAlgorithm = parcel.readInt();
        this.mSalt = parcel.createByteArray();
        this.mMemoryDifficulty = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
