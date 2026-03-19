package android.app;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

public final class AuthenticationRequiredException extends SecurityException implements Parcelable {
    public static final Parcelable.Creator<AuthenticationRequiredException> CREATOR = new Parcelable.Creator<AuthenticationRequiredException>() {
        @Override
        public AuthenticationRequiredException createFromParcel(Parcel parcel) {
            return new AuthenticationRequiredException(parcel);
        }

        @Override
        public AuthenticationRequiredException[] newArray(int i) {
            return new AuthenticationRequiredException[i];
        }
    };
    private static final String TAG = "AuthenticationRequiredException";
    private final PendingIntent mUserAction;

    public AuthenticationRequiredException(Parcel parcel) {
        this(new SecurityException(parcel.readString()), PendingIntent.CREATOR.createFromParcel(parcel));
    }

    public AuthenticationRequiredException(Throwable th, PendingIntent pendingIntent) {
        super(th.getMessage());
        this.mUserAction = (PendingIntent) Preconditions.checkNotNull(pendingIntent);
    }

    public PendingIntent getUserAction() {
        return this.mUserAction;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(getMessage());
        this.mUserAction.writeToParcel(parcel, i);
    }
}
