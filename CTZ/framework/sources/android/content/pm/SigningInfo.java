package android.content.pm;

import android.content.pm.PackageParser;
import android.os.Parcel;
import android.os.Parcelable;

public final class SigningInfo implements Parcelable {
    public static final Parcelable.Creator<SigningInfo> CREATOR = new Parcelable.Creator<SigningInfo>() {
        @Override
        public SigningInfo createFromParcel(Parcel parcel) {
            return new SigningInfo(parcel);
        }

        @Override
        public SigningInfo[] newArray(int i) {
            return new SigningInfo[i];
        }
    };
    private final PackageParser.SigningDetails mSigningDetails;

    public SigningInfo() {
        this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
    }

    public SigningInfo(PackageParser.SigningDetails signingDetails) {
        this.mSigningDetails = new PackageParser.SigningDetails(signingDetails);
    }

    public SigningInfo(SigningInfo signingInfo) {
        this.mSigningDetails = new PackageParser.SigningDetails(signingInfo.mSigningDetails);
    }

    private SigningInfo(Parcel parcel) {
        this.mSigningDetails = PackageParser.SigningDetails.CREATOR.createFromParcel(parcel);
    }

    public boolean hasMultipleSigners() {
        return this.mSigningDetails.signatures != null && this.mSigningDetails.signatures.length > 1;
    }

    public boolean hasPastSigningCertificates() {
        return (this.mSigningDetails.signatures == null || this.mSigningDetails.pastSigningCertificates == null) ? false : true;
    }

    public Signature[] getSigningCertificateHistory() {
        if (hasMultipleSigners()) {
            return null;
        }
        if (!hasPastSigningCertificates()) {
            return this.mSigningDetails.signatures;
        }
        return this.mSigningDetails.pastSigningCertificates;
    }

    public Signature[] getApkContentsSigners() {
        return this.mSigningDetails.signatures;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mSigningDetails.writeToParcel(parcel, i);
    }
}
