package android.security.keystore.recovery;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

@SystemApi
public final class WrappedApplicationKey implements Parcelable {
    public static final Parcelable.Creator<WrappedApplicationKey> CREATOR = new Parcelable.Creator<WrappedApplicationKey>() {
        @Override
        public WrappedApplicationKey createFromParcel(Parcel parcel) {
            return new WrappedApplicationKey(parcel);
        }

        @Override
        public WrappedApplicationKey[] newArray(int i) {
            return new WrappedApplicationKey[i];
        }
    };
    private String mAlias;
    private byte[] mEncryptedKeyMaterial;

    public static class Builder {
        private WrappedApplicationKey mInstance = new WrappedApplicationKey();

        public Builder setAlias(String str) {
            this.mInstance.mAlias = str;
            return this;
        }

        @Deprecated
        public Builder setAccount(byte[] bArr) {
            throw new UnsupportedOperationException();
        }

        public Builder setEncryptedKeyMaterial(byte[] bArr) {
            this.mInstance.mEncryptedKeyMaterial = bArr;
            return this;
        }

        public WrappedApplicationKey build() {
            Preconditions.checkNotNull(this.mInstance.mAlias);
            Preconditions.checkNotNull(this.mInstance.mEncryptedKeyMaterial);
            return this.mInstance;
        }
    }

    private WrappedApplicationKey() {
    }

    public WrappedApplicationKey(String str, byte[] bArr) {
        this.mAlias = (String) Preconditions.checkNotNull(str);
        this.mEncryptedKeyMaterial = (byte[]) Preconditions.checkNotNull(bArr);
    }

    public String getAlias() {
        return this.mAlias;
    }

    public byte[] getEncryptedKeyMaterial() {
        return this.mEncryptedKeyMaterial;
    }

    @Deprecated
    public byte[] getAccount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mAlias);
        parcel.writeByteArray(this.mEncryptedKeyMaterial);
    }

    protected WrappedApplicationKey(Parcel parcel) {
        this.mAlias = parcel.readString();
        this.mEncryptedKeyMaterial = parcel.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
