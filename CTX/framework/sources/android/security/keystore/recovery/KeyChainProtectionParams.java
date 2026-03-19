package android.security.keystore.recovery;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

@SystemApi
public final class KeyChainProtectionParams implements Parcelable {
    public static final Parcelable.Creator<KeyChainProtectionParams> CREATOR = new Parcelable.Creator<KeyChainProtectionParams>() {
        @Override
        public KeyChainProtectionParams createFromParcel(Parcel parcel) {
            return new KeyChainProtectionParams(parcel);
        }

        @Override
        public KeyChainProtectionParams[] newArray(int i) {
            return new KeyChainProtectionParams[i];
        }
    };
    public static final int TYPE_LOCKSCREEN = 100;
    public static final int UI_FORMAT_PASSWORD = 2;
    public static final int UI_FORMAT_PATTERN = 3;
    public static final int UI_FORMAT_PIN = 1;
    private KeyDerivationParams mKeyDerivationParams;
    private Integer mLockScreenUiFormat;
    private byte[] mSecret;
    private Integer mUserSecretType;

    @Retention(RetentionPolicy.SOURCE)
    public @interface LockScreenUiFormat {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface UserSecretType {
    }

    private KeyChainProtectionParams() {
    }

    public int getUserSecretType() {
        return this.mUserSecretType.intValue();
    }

    public int getLockScreenUiFormat() {
        return this.mLockScreenUiFormat.intValue();
    }

    public KeyDerivationParams getKeyDerivationParams() {
        return this.mKeyDerivationParams;
    }

    public byte[] getSecret() {
        return this.mSecret;
    }

    public static class Builder {
        private KeyChainProtectionParams mInstance = new KeyChainProtectionParams();

        public Builder setUserSecretType(int i) {
            this.mInstance.mUserSecretType = Integer.valueOf(i);
            return this;
        }

        public Builder setLockScreenUiFormat(int i) {
            this.mInstance.mLockScreenUiFormat = Integer.valueOf(i);
            return this;
        }

        public Builder setKeyDerivationParams(KeyDerivationParams keyDerivationParams) {
            this.mInstance.mKeyDerivationParams = keyDerivationParams;
            return this;
        }

        public Builder setSecret(byte[] bArr) {
            this.mInstance.mSecret = bArr;
            return this;
        }

        public KeyChainProtectionParams build() {
            if (this.mInstance.mUserSecretType == null) {
                this.mInstance.mUserSecretType = 100;
            }
            Preconditions.checkNotNull(this.mInstance.mLockScreenUiFormat);
            Preconditions.checkNotNull(this.mInstance.mKeyDerivationParams);
            if (this.mInstance.mSecret == null) {
                this.mInstance.mSecret = new byte[0];
            }
            return this.mInstance;
        }
    }

    public void clearSecret() {
        Arrays.fill(this.mSecret, (byte) 0);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mUserSecretType.intValue());
        parcel.writeInt(this.mLockScreenUiFormat.intValue());
        parcel.writeTypedObject(this.mKeyDerivationParams, i);
        parcel.writeByteArray(this.mSecret);
    }

    protected KeyChainProtectionParams(Parcel parcel) {
        this.mUserSecretType = Integer.valueOf(parcel.readInt());
        this.mLockScreenUiFormat = Integer.valueOf(parcel.readInt());
        this.mKeyDerivationParams = (KeyDerivationParams) parcel.readTypedObject(KeyDerivationParams.CREATOR);
        this.mSecret = parcel.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
