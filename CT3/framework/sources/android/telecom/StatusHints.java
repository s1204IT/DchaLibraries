package android.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class StatusHints implements Parcelable {
    public static final Parcelable.Creator<StatusHints> CREATOR = new Parcelable.Creator<StatusHints>() {
        @Override
        public StatusHints createFromParcel(Parcel in) {
            return new StatusHints(in, null);
        }

        @Override
        public StatusHints[] newArray(int size) {
            return new StatusHints[size];
        }
    };
    private final Bundle mExtras;
    private final Icon mIcon;
    private final CharSequence mLabel;

    StatusHints(Parcel in, StatusHints statusHints) {
        this(in);
    }

    @Deprecated
    public StatusHints(ComponentName packageName, CharSequence label, int iconResId, Bundle extras) {
        this(label, iconResId == 0 ? null : Icon.createWithResource(packageName.getPackageName(), iconResId), extras);
    }

    public StatusHints(CharSequence label, Icon icon, Bundle extras) {
        this.mLabel = label;
        this.mIcon = icon;
        this.mExtras = extras;
    }

    @Deprecated
    public ComponentName getPackageName() {
        return new ComponentName(ProxyInfo.LOCAL_EXCL_LIST, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    @Deprecated
    public int getIconResId() {
        return 0;
    }

    @Deprecated
    public Drawable getIcon(Context context) {
        return this.mIcon.loadDrawable(context);
    }

    public Icon getIcon() {
        return this.mIcon;
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
        out.writeCharSequence(this.mLabel);
        out.writeParcelable(this.mIcon, 0);
        out.writeParcelable(this.mExtras, 0);
    }

    private StatusHints(Parcel in) {
        this.mLabel = in.readCharSequence();
        this.mIcon = (Icon) in.readParcelable(getClass().getClassLoader());
        this.mExtras = (Bundle) in.readParcelable(getClass().getClassLoader());
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof StatusHints)) {
            return false;
        }
        StatusHints otherHints = (StatusHints) other;
        if (Objects.equals(otherHints.getLabel(), getLabel()) && Objects.equals(otherHints.getIcon(), getIcon())) {
            return Objects.equals(otherHints.getExtras(), getExtras());
        }
        return false;
    }

    public int hashCode() {
        return Objects.hashCode(this.mLabel) + Objects.hashCode(this.mIcon) + Objects.hashCode(this.mExtras);
    }
}
