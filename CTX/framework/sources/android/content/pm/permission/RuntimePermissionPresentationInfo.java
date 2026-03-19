package android.content.pm.permission;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class RuntimePermissionPresentationInfo implements Parcelable {
    public static final Parcelable.Creator<RuntimePermissionPresentationInfo> CREATOR = new Parcelable.Creator<RuntimePermissionPresentationInfo>() {
        @Override
        public RuntimePermissionPresentationInfo createFromParcel(Parcel parcel) {
            return new RuntimePermissionPresentationInfo(parcel);
        }

        @Override
        public RuntimePermissionPresentationInfo[] newArray(int i) {
            return new RuntimePermissionPresentationInfo[i];
        }
    };
    private static final int FLAG_GRANTED = 1;
    private static final int FLAG_STANDARD = 2;
    private final int mFlags;
    private final CharSequence mLabel;

    public RuntimePermissionPresentationInfo(CharSequence charSequence, boolean z, boolean z2) {
        int i;
        this.mLabel = charSequence;
        if (z) {
            i = 1;
        } else {
            i = 0;
        }
        this.mFlags = z2 ? i | 2 : i;
    }

    private RuntimePermissionPresentationInfo(Parcel parcel) {
        this.mLabel = parcel.readCharSequence();
        this.mFlags = parcel.readInt();
    }

    public boolean isGranted() {
        return (this.mFlags & 1) != 0;
    }

    public boolean isStandard() {
        return (this.mFlags & 2) != 0;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeCharSequence(this.mLabel);
        parcel.writeInt(this.mFlags);
    }
}
