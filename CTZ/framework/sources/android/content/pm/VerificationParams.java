package android.content.pm;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

@Deprecated
public class VerificationParams implements Parcelable {
    public static final Parcelable.Creator<VerificationParams> CREATOR = new Parcelable.Creator<VerificationParams>() {
        @Override
        public VerificationParams createFromParcel(Parcel parcel) {
            return new VerificationParams(parcel);
        }

        @Override
        public VerificationParams[] newArray(int i) {
            return new VerificationParams[i];
        }
    };
    public static final int NO_UID = -1;
    private static final String TO_STRING_PREFIX = "VerificationParams{";
    private int mInstallerUid;
    private final Uri mOriginatingURI;
    private final int mOriginatingUid;
    private final Uri mReferrer;
    private final Uri mVerificationURI;

    public VerificationParams(Uri uri, Uri uri2, Uri uri3, int i) {
        this.mVerificationURI = uri;
        this.mOriginatingURI = uri2;
        this.mReferrer = uri3;
        this.mOriginatingUid = i;
        this.mInstallerUid = -1;
    }

    public Uri getVerificationURI() {
        return this.mVerificationURI;
    }

    public Uri getOriginatingURI() {
        return this.mOriginatingURI;
    }

    public Uri getReferrer() {
        return this.mReferrer;
    }

    public int getOriginatingUid() {
        return this.mOriginatingUid;
    }

    public int getInstallerUid() {
        return this.mInstallerUid;
    }

    public void setInstallerUid(int i) {
        this.mInstallerUid = i;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VerificationParams)) {
            return false;
        }
        VerificationParams verificationParams = (VerificationParams) obj;
        if (this.mVerificationURI == null) {
            if (verificationParams.mVerificationURI != null) {
                return false;
            }
        } else if (!this.mVerificationURI.equals(verificationParams.mVerificationURI)) {
            return false;
        }
        if (this.mOriginatingURI == null) {
            if (verificationParams.mOriginatingURI != null) {
                return false;
            }
        } else if (!this.mOriginatingURI.equals(verificationParams.mOriginatingURI)) {
            return false;
        }
        if (this.mReferrer == null) {
            if (verificationParams.mReferrer != null) {
                return false;
            }
        } else if (!this.mReferrer.equals(verificationParams.mReferrer)) {
            return false;
        }
        return this.mOriginatingUid == verificationParams.mOriginatingUid && this.mInstallerUid == verificationParams.mInstallerUid;
    }

    public int hashCode() {
        return 3 + (5 * (this.mVerificationURI == null ? 1 : this.mVerificationURI.hashCode())) + (7 * (this.mOriginatingURI == null ? 1 : this.mOriginatingURI.hashCode())) + (11 * (this.mReferrer != null ? this.mReferrer.hashCode() : 1)) + (13 * this.mOriginatingUid) + (17 * this.mInstallerUid);
    }

    public String toString() {
        return TO_STRING_PREFIX + "mVerificationURI=" + this.mVerificationURI.toString() + ",mOriginatingURI=" + this.mOriginatingURI.toString() + ",mReferrer=" + this.mReferrer.toString() + ",mOriginatingUid=" + this.mOriginatingUid + ",mInstallerUid=" + this.mInstallerUid + '}';
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mVerificationURI, 0);
        parcel.writeParcelable(this.mOriginatingURI, 0);
        parcel.writeParcelable(this.mReferrer, 0);
        parcel.writeInt(this.mOriginatingUid);
        parcel.writeInt(this.mInstallerUid);
    }

    private VerificationParams(Parcel parcel) {
        this.mVerificationURI = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.mOriginatingURI = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.mReferrer = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.mOriginatingUid = parcel.readInt();
        this.mInstallerUid = parcel.readInt();
    }
}
