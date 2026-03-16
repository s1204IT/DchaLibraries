package com.android.internal.statusbar;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

public class StatusBarIcon implements Parcelable {
    public static final Parcelable.Creator<StatusBarIcon> CREATOR = new Parcelable.Creator<StatusBarIcon>() {
        @Override
        public StatusBarIcon createFromParcel(Parcel parcel) {
            return new StatusBarIcon(parcel);
        }

        @Override
        public StatusBarIcon[] newArray(int size) {
            return new StatusBarIcon[size];
        }
    };
    public CharSequence contentDescription;
    public int iconId;
    public int iconLevel;
    public String iconPackage;
    public int number;
    public UserHandle user;
    public boolean visible = true;

    public StatusBarIcon(String iconPackage, UserHandle user, int iconId, int iconLevel, int number, CharSequence contentDescription) {
        this.iconPackage = iconPackage;
        this.user = user;
        this.iconId = iconId;
        this.iconLevel = iconLevel;
        this.number = number;
        this.contentDescription = contentDescription;
    }

    public String toString() {
        return "StatusBarIcon(pkg=" + this.iconPackage + "user=" + this.user.getIdentifier() + " id=0x" + Integer.toHexString(this.iconId) + " level=" + this.iconLevel + " visible=" + this.visible + " num=" + this.number + " )";
    }

    public StatusBarIcon m30clone() {
        StatusBarIcon that = new StatusBarIcon(this.iconPackage, this.user, this.iconId, this.iconLevel, this.number, this.contentDescription);
        that.visible = this.visible;
        return that;
    }

    public StatusBarIcon(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        this.iconPackage = in.readString();
        this.user = (UserHandle) in.readParcelable(null);
        this.iconId = in.readInt();
        this.iconLevel = in.readInt();
        this.visible = in.readInt() != 0;
        this.number = in.readInt();
        this.contentDescription = in.readCharSequence();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.iconPackage);
        out.writeParcelable(this.user, 0);
        out.writeInt(this.iconId);
        out.writeInt(this.iconLevel);
        out.writeInt(this.visible ? 1 : 0);
        out.writeInt(this.number);
        out.writeCharSequence(this.contentDescription);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
