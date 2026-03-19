package android.security.keystore;

import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyGenParameterSpec;
import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

public final class ParcelableKeyGenParameterSpec implements Parcelable {
    private static final int ALGORITHM_PARAMETER_SPEC_EC = 3;
    private static final int ALGORITHM_PARAMETER_SPEC_NONE = 1;
    private static final int ALGORITHM_PARAMETER_SPEC_RSA = 2;
    public static final Parcelable.Creator<ParcelableKeyGenParameterSpec> CREATOR = new Parcelable.Creator<ParcelableKeyGenParameterSpec>() {
        @Override
        public ParcelableKeyGenParameterSpec createFromParcel(Parcel parcel) {
            return new ParcelableKeyGenParameterSpec(parcel);
        }

        @Override
        public ParcelableKeyGenParameterSpec[] newArray(int i) {
            return new ParcelableKeyGenParameterSpec[i];
        }
    };
    private final KeyGenParameterSpec mSpec;

    public ParcelableKeyGenParameterSpec(KeyGenParameterSpec keyGenParameterSpec) {
        this.mSpec = keyGenParameterSpec;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static void writeOptionalDate(Parcel parcel, Date date) {
        if (date != null) {
            parcel.writeBoolean(true);
            parcel.writeLong(date.getTime());
        } else {
            parcel.writeBoolean(false);
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mSpec.getKeystoreAlias());
        parcel.writeInt(this.mSpec.getPurposes());
        parcel.writeInt(this.mSpec.getUid());
        parcel.writeInt(this.mSpec.getKeySize());
        AlgorithmParameterSpec algorithmParameterSpec = this.mSpec.getAlgorithmParameterSpec();
        if (algorithmParameterSpec == null) {
            parcel.writeInt(1);
        } else if (algorithmParameterSpec instanceof RSAKeyGenParameterSpec) {
            RSAKeyGenParameterSpec rSAKeyGenParameterSpec = (RSAKeyGenParameterSpec) algorithmParameterSpec;
            parcel.writeInt(2);
            parcel.writeInt(rSAKeyGenParameterSpec.getKeysize());
            parcel.writeByteArray(rSAKeyGenParameterSpec.getPublicExponent().toByteArray());
        } else if (algorithmParameterSpec instanceof ECGenParameterSpec) {
            parcel.writeInt(3);
            parcel.writeString(((ECGenParameterSpec) algorithmParameterSpec).getName());
        } else {
            throw new IllegalArgumentException(String.format("Unknown algorithm parameter spec: %s", algorithmParameterSpec.getClass()));
        }
        parcel.writeByteArray(this.mSpec.getCertificateSubject().getEncoded());
        parcel.writeByteArray(this.mSpec.getCertificateSerialNumber().toByteArray());
        parcel.writeLong(this.mSpec.getCertificateNotBefore().getTime());
        parcel.writeLong(this.mSpec.getCertificateNotAfter().getTime());
        writeOptionalDate(parcel, this.mSpec.getKeyValidityStart());
        writeOptionalDate(parcel, this.mSpec.getKeyValidityForOriginationEnd());
        writeOptionalDate(parcel, this.mSpec.getKeyValidityForConsumptionEnd());
        if (this.mSpec.isDigestsSpecified()) {
            parcel.writeStringArray(this.mSpec.getDigests());
        } else {
            parcel.writeStringArray(null);
        }
        parcel.writeStringArray(this.mSpec.getEncryptionPaddings());
        parcel.writeStringArray(this.mSpec.getSignaturePaddings());
        parcel.writeStringArray(this.mSpec.getBlockModes());
        parcel.writeBoolean(this.mSpec.isRandomizedEncryptionRequired());
        parcel.writeBoolean(this.mSpec.isUserAuthenticationRequired());
        parcel.writeInt(this.mSpec.getUserAuthenticationValidityDurationSeconds());
        parcel.writeByteArray(this.mSpec.getAttestationChallenge());
        parcel.writeBoolean(this.mSpec.isUniqueIdIncluded());
        parcel.writeBoolean(this.mSpec.isUserAuthenticationValidWhileOnBody());
        parcel.writeBoolean(this.mSpec.isInvalidatedByBiometricEnrollment());
        parcel.writeBoolean(this.mSpec.isUserPresenceRequired());
    }

    private static Date readDateOrNull(Parcel parcel) {
        if (parcel.readBoolean()) {
            return new Date(parcel.readLong());
        }
        return null;
    }

    private ParcelableKeyGenParameterSpec(Parcel parcel) {
        AlgorithmParameterSpec eCGenParameterSpec;
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(parcel.readString(), parcel.readInt());
        builder.setUid(parcel.readInt());
        int i = parcel.readInt();
        if (i >= 0) {
            builder.setKeySize(i);
        }
        int i2 = parcel.readInt();
        if (i2 == 1) {
            eCGenParameterSpec = null;
        } else if (i2 == 2) {
            eCGenParameterSpec = new RSAKeyGenParameterSpec(parcel.readInt(), new BigInteger(parcel.createByteArray()));
        } else if (i2 == 3) {
            eCGenParameterSpec = new ECGenParameterSpec(parcel.readString());
        } else {
            throw new IllegalArgumentException(String.format("Unknown algorithm parameter spec: %d", Integer.valueOf(i2)));
        }
        if (eCGenParameterSpec != null) {
            builder.setAlgorithmParameterSpec(eCGenParameterSpec);
        }
        builder.setCertificateSubject(new X500Principal(parcel.createByteArray()));
        builder.setCertificateSerialNumber(new BigInteger(parcel.createByteArray()));
        builder.setCertificateNotBefore(new Date(parcel.readLong()));
        builder.setCertificateNotAfter(new Date(parcel.readLong()));
        builder.setKeyValidityStart(readDateOrNull(parcel));
        builder.setKeyValidityForOriginationEnd(readDateOrNull(parcel));
        builder.setKeyValidityForConsumptionEnd(readDateOrNull(parcel));
        String[] strArrCreateStringArray = parcel.createStringArray();
        if (strArrCreateStringArray != null) {
            builder.setDigests(strArrCreateStringArray);
        }
        builder.setEncryptionPaddings(parcel.createStringArray());
        builder.setSignaturePaddings(parcel.createStringArray());
        builder.setBlockModes(parcel.createStringArray());
        builder.setRandomizedEncryptionRequired(parcel.readBoolean());
        builder.setUserAuthenticationRequired(parcel.readBoolean());
        builder.setUserAuthenticationValidityDurationSeconds(parcel.readInt());
        builder.setAttestationChallenge(parcel.createByteArray());
        builder.setUniqueIdIncluded(parcel.readBoolean());
        builder.setUserAuthenticationValidWhileOnBody(parcel.readBoolean());
        builder.setInvalidatedByBiometricEnrollment(parcel.readBoolean());
        builder.setUserPresenceRequired(parcel.readBoolean());
        this.mSpec = builder.build();
    }

    public KeyGenParameterSpec getSpec() {
        return this.mSpec;
    }
}
