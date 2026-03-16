package android.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.MissingResourceException;
import java.util.Objects;

public final class StatusHints implements Parcelable {
    public static final Parcelable.Creator<StatusHints> CREATOR = new Parcelable.Creator<StatusHints>() {
        @Override
        public StatusHints createFromParcel(Parcel in) {
            return new StatusHints(in);
        }

        @Override
        public StatusHints[] newArray(int size) {
            return new StatusHints[size];
        }
    };
    private final Bundle mExtras;
    private final int mIconResId;
    private final CharSequence mLabel;
    private final ComponentName mPackageName;

    public StatusHints(ComponentName packageName, CharSequence label, int iconResId, Bundle extras) {
        this.mPackageName = packageName;
        this.mLabel = label;
        this.mIconResId = iconResId;
        this.mExtras = extras;
    }

    public ComponentName getPackageName() {
        return this.mPackageName;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public int getIconResId() {
        return this.mIconResId;
    }

    public Drawable getIcon(Context context) {
        return getIcon(context, this.mIconResId);
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.mPackageName, flags);
        out.writeCharSequence(this.mLabel);
        out.writeInt(this.mIconResId);
        out.writeParcelable(this.mExtras, 0);
    }

    private StatusHints(Parcel in) {
        this.mPackageName = (ComponentName) in.readParcelable(getClass().getClassLoader());
        this.mLabel = in.readCharSequence();
        this.mIconResId = in.readInt();
        this.mExtras = (Bundle) in.readParcelable(getClass().getClassLoader());
    }

    private Drawable getIcon(Context context, int resId) {
        try {
            Context packageContext = context.createPackageContext(this.mPackageName.getPackageName(), 0);
            try {
                return packageContext.getDrawable(resId);
            } catch (MissingResourceException e) {
                Log.e(this, e, "Cannot find icon %d in package %s", Integer.valueOf(resId), this.mPackageName.getPackageName());
                return null;
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e(this, e2, "Cannot find package %s", this.mPackageName.getPackageName());
            return null;
        }
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof StatusHints)) {
            return false;
        }
        StatusHints otherHints = (StatusHints) other;
        return Objects.equals(otherHints.getPackageName(), getPackageName()) && Objects.equals(otherHints.getLabel(), getLabel()) && otherHints.getIconResId() == getIconResId() && Objects.equals(otherHints.getExtras(), getExtras());
    }

    public int hashCode() {
        return Objects.hashCode(this.mPackageName) + Objects.hashCode(this.mLabel) + this.mIconResId + Objects.hashCode(this.mExtras);
    }
}
