package android.telecom;

import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class StatusHints implements Parcelable {
    public static final Parcelable.Creator<StatusHints> CREATOR = new Parcelable.Creator<StatusHints>() {
        @Override
        public StatusHints createFromParcel(Parcel parcel) {
            return new StatusHints(parcel);
        }

        @Override
        public StatusHints[] newArray(int i) {
            return new StatusHints[i];
        }
    };
    private final Bundle mExtras;
    private final Icon mIcon;
    private final CharSequence mLabel;

    @SystemApi
    @Deprecated
    public StatusHints(ComponentName componentName, CharSequence charSequence, int i, Bundle bundle) {
        this(charSequence, i == 0 ? null : Icon.createWithResource(componentName.getPackageName(), i), bundle);
    }

    public StatusHints(CharSequence charSequence, Icon icon, Bundle bundle) {
        this.mLabel = charSequence;
        this.mIcon = icon;
        this.mExtras = bundle;
    }

    @SystemApi
    @Deprecated
    public ComponentName getPackageName() {
        return new ComponentName("", "");
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    @SystemApi
    @Deprecated
    public int getIconResId() {
        return 0;
    }

    @SystemApi
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeCharSequence(this.mLabel);
        parcel.writeParcelable(this.mIcon, 0);
        parcel.writeParcelable(this.mExtras, 0);
    }

    private StatusHints(Parcel parcel) {
        this.mLabel = parcel.readCharSequence();
        this.mIcon = (Icon) parcel.readParcelable(getClass().getClassLoader());
        this.mExtras = (Bundle) parcel.readParcelable(getClass().getClassLoader());
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof StatusHints)) {
            return false;
        }
        StatusHints statusHints = (StatusHints) obj;
        return Objects.equals(statusHints.getLabel(), getLabel()) && Objects.equals(statusHints.getIcon(), getIcon()) && Objects.equals(statusHints.getExtras(), getExtras());
    }

    public int hashCode() {
        return Objects.hashCode(this.mLabel) + Objects.hashCode(this.mIcon) + Objects.hashCode(this.mExtras);
    }
}
