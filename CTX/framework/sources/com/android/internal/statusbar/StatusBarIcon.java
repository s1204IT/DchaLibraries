package com.android.internal.statusbar;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.TextUtils;

public class StatusBarIcon implements Parcelable {
    public static final Parcelable.Creator<StatusBarIcon> CREATOR = new Parcelable.Creator<StatusBarIcon>() {
        @Override
        public StatusBarIcon createFromParcel(Parcel parcel) {
            return new StatusBarIcon(parcel);
        }

        @Override
        public StatusBarIcon[] newArray(int i) {
            return new StatusBarIcon[i];
        }
    };
    public CharSequence contentDescription;
    public Icon icon;
    public int iconLevel;
    public int number;
    public String pkg;
    public UserHandle user;
    public boolean visible;

    public StatusBarIcon(UserHandle userHandle, String str, Icon icon, int i, int i2, CharSequence charSequence) {
        this.visible = true;
        if (icon.getType() == 2 && TextUtils.isEmpty(icon.getResPackage())) {
            icon = Icon.createWithResource(str, icon.getResId());
        }
        this.pkg = str;
        this.user = userHandle;
        this.icon = icon;
        this.iconLevel = i;
        this.number = i2;
        this.contentDescription = charSequence;
    }

    public StatusBarIcon(String str, UserHandle userHandle, int i, int i2, int i3, CharSequence charSequence) {
        this(userHandle, str, Icon.createWithResource(str, i), i2, i3, charSequence);
    }

    public String toString() {
        String str;
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append("StatusBarIcon(icon=");
        sb.append(this.icon);
        if (this.iconLevel != 0) {
            str = " level=" + this.iconLevel;
        } else {
            str = "";
        }
        sb.append(str);
        sb.append(this.visible ? " visible" : "");
        sb.append(" user=");
        sb.append(this.user.getIdentifier());
        if (this.number != 0) {
            str2 = " num=" + this.number;
        } else {
            str2 = "";
        }
        sb.append(str2);
        sb.append(" )");
        return sb.toString();
    }

    public StatusBarIcon m47clone() {
        StatusBarIcon statusBarIcon = new StatusBarIcon(this.user, this.pkg, this.icon, this.iconLevel, this.number, this.contentDescription);
        statusBarIcon.visible = this.visible;
        return statusBarIcon;
    }

    public StatusBarIcon(Parcel parcel) {
        this.visible = true;
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.icon = (Icon) parcel.readParcelable(null);
        this.pkg = parcel.readString();
        this.user = (UserHandle) parcel.readParcelable(null);
        this.iconLevel = parcel.readInt();
        this.visible = parcel.readInt() != 0;
        this.number = parcel.readInt();
        this.contentDescription = parcel.readCharSequence();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.icon, 0);
        parcel.writeString(this.pkg);
        parcel.writeParcelable(this.user, 0);
        parcel.writeInt(this.iconLevel);
        parcel.writeInt(this.visible ? 1 : 0);
        parcel.writeInt(this.number);
        parcel.writeCharSequence(this.contentDescription);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
