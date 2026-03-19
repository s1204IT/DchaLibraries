package android.content.pm;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

@Deprecated
public class VerificationParams implements Parcelable {
    public static final Parcelable.Creator<VerificationParams> CREATOR = new Parcelable.Creator<VerificationParams>() {
        @Override
        public VerificationParams createFromParcel(Parcel source) {
            return new VerificationParams(source, null);
        }

        @Override
        public VerificationParams[] newArray(int size) {
            return new VerificationParams[size];
        }
    };
    public static final int NO_UID = -1;
    private static final String TO_STRING_PREFIX = "VerificationParams{";
    private int mInstallerUid;
    private final Uri mOriginatingURI;
    private final int mOriginatingUid;
    private final Uri mReferrer;
    private final Uri mVerificationURI;

    VerificationParams(Parcel source, VerificationParams verificationParams) {
        this(source);
    }

    public VerificationParams(Uri verificationURI, Uri originatingURI, Uri referrer, int originatingUid) {
        this.mVerificationURI = verificationURI;
        this.mOriginatingURI = originatingURI;
        this.mReferrer = referrer;
        this.mOriginatingUid = originatingUid;
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

    public void setInstallerUid(int uid) {
        this.mInstallerUid = uid;
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
        if (this.mVerificationURI == null) {
            if (obj.mVerificationURI != null) {
                return false;
            }
        } else if (!this.mVerificationURI.equals(obj.mVerificationURI)) {
            return false;
        }
        if (this.mOriginatingURI == null) {
            if (obj.mOriginatingURI != null) {
                return false;
            }
        } else if (!this.mOriginatingURI.equals(obj.mOriginatingURI)) {
            return false;
        }
        if (this.mReferrer == null) {
            if (obj.mReferrer != null) {
                return false;
            }
        } else if (!this.mReferrer.equals(obj.mReferrer)) {
            return false;
        }
        return this.mOriginatingUid == obj.mOriginatingUid && this.mInstallerUid == obj.mInstallerUid;
    }

    public int hashCode() {
        int hash = ((this.mVerificationURI == null ? 1 : this.mVerificationURI.hashCode()) * 5) + 3;
        return hash + ((this.mOriginatingURI == null ? 1 : this.mOriginatingURI.hashCode()) * 7) + ((this.mReferrer != null ? this.mReferrer.hashCode() : 1) * 11) + (this.mOriginatingUid * 13) + (this.mInstallerUid * 17);
    }

    public String toString() {
        return TO_STRING_PREFIX + "mVerificationURI=" + this.mVerificationURI.toString() + ",mOriginatingURI=" + this.mOriginatingURI.toString() + ",mReferrer=" + this.mReferrer.toString() + ",mOriginatingUid=" + this.mOriginatingUid + ",mInstallerUid=" + this.mInstallerUid + '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mVerificationURI, 0);
        dest.writeParcelable(this.mOriginatingURI, 0);
        dest.writeParcelable(this.mReferrer, 0);
        dest.writeInt(this.mOriginatingUid);
        dest.writeInt(this.mInstallerUid);
    }

    private VerificationParams(Parcel source) {
        this.mVerificationURI = (Uri) source.readParcelable(Uri.class.getClassLoader());
        this.mOriginatingURI = (Uri) source.readParcelable(Uri.class.getClassLoader());
        this.mReferrer = (Uri) source.readParcelable(Uri.class.getClassLoader());
        this.mOriginatingUid = source.readInt();
        this.mInstallerUid = source.readInt();
    }
}
