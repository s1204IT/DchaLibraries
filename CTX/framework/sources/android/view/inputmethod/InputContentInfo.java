package android.view.inputmethod;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.internal.inputmethod.IInputContentUriToken;
import java.security.InvalidParameterException;

public final class InputContentInfo implements Parcelable {
    public static final Parcelable.Creator<InputContentInfo> CREATOR = new Parcelable.Creator<InputContentInfo>() {
        @Override
        public InputContentInfo createFromParcel(Parcel parcel) {
            return new InputContentInfo(parcel);
        }

        @Override
        public InputContentInfo[] newArray(int i) {
            return new InputContentInfo[i];
        }
    };
    private final Uri mContentUri;
    private final int mContentUriOwnerUserId;
    private final ClipDescription mDescription;
    private final Uri mLinkUri;
    private IInputContentUriToken mUriToken;

    public InputContentInfo(Uri uri, ClipDescription clipDescription) {
        this(uri, clipDescription, null);
    }

    public InputContentInfo(Uri uri, ClipDescription clipDescription, Uri uri2) {
        validateInternal(uri, clipDescription, uri2, true);
        this.mContentUri = uri;
        this.mContentUriOwnerUserId = ContentProvider.getUserIdFromUri(this.mContentUri, UserHandle.myUserId());
        this.mDescription = clipDescription;
        this.mLinkUri = uri2;
    }

    public boolean validate() {
        return validateInternal(this.mContentUri, this.mDescription, this.mLinkUri, false);
    }

    private static boolean validateInternal(Uri uri, ClipDescription clipDescription, Uri uri2, boolean z) {
        if (uri == null) {
            if (!z) {
                return false;
            }
            throw new NullPointerException("contentUri");
        }
        if (clipDescription == null) {
            if (!z) {
                return false;
            }
            throw new NullPointerException("description");
        }
        if (!"content".equals(uri.getScheme())) {
            if (!z) {
                return false;
            }
            throw new InvalidParameterException("contentUri must have content scheme");
        }
        if (uri2 != null) {
            String scheme = uri2.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase(IntentFilter.SCHEME_HTTP) && !scheme.equalsIgnoreCase(IntentFilter.SCHEME_HTTPS))) {
                if (!z) {
                    return false;
                }
                throw new InvalidParameterException("linkUri must have either http or https scheme");
            }
            return true;
        }
        return true;
    }

    public Uri getContentUri() {
        if (this.mContentUriOwnerUserId != UserHandle.myUserId()) {
            return ContentProvider.maybeAddUserId(this.mContentUri, this.mContentUriOwnerUserId);
        }
        return this.mContentUri;
    }

    public ClipDescription getDescription() {
        return this.mDescription;
    }

    public Uri getLinkUri() {
        return this.mLinkUri;
    }

    void setUriToken(IInputContentUriToken iInputContentUriToken) {
        if (this.mUriToken != null) {
            throw new IllegalStateException("URI token is already set");
        }
        this.mUriToken = iInputContentUriToken;
    }

    public void requestPermission() {
        if (this.mUriToken == null) {
            return;
        }
        try {
            this.mUriToken.take();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void releasePermission() {
        if (this.mUriToken == null) {
            return;
        }
        try {
            this.mUriToken.release();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Uri.writeToParcel(parcel, this.mContentUri);
        parcel.writeInt(this.mContentUriOwnerUserId);
        this.mDescription.writeToParcel(parcel, i);
        Uri.writeToParcel(parcel, this.mLinkUri);
        if (this.mUriToken != null) {
            parcel.writeInt(1);
            parcel.writeStrongBinder(this.mUriToken.asBinder());
        } else {
            parcel.writeInt(0);
        }
    }

    private InputContentInfo(Parcel parcel) {
        this.mContentUri = Uri.CREATOR.createFromParcel(parcel);
        this.mContentUriOwnerUserId = parcel.readInt();
        this.mDescription = ClipDescription.CREATOR.createFromParcel(parcel);
        this.mLinkUri = Uri.CREATOR.createFromParcel(parcel);
        if (parcel.readInt() == 1) {
            this.mUriToken = IInputContentUriToken.Stub.asInterface(parcel.readStrongBinder());
        } else {
            this.mUriToken = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
