package com.android.settings.dashboard;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class DashboardTile implements Parcelable {
    public static final Parcelable.Creator<DashboardTile> CREATOR = new Parcelable.Creator<DashboardTile>() {
        @Override
        public DashboardTile createFromParcel(Parcel source) {
            return new DashboardTile(source);
        }

        @Override
        public DashboardTile[] newArray(int size) {
            return new DashboardTile[size];
        }
    };
    public Bundle extras;
    public String fragment;
    public Bundle fragmentArguments;
    public int iconRes;
    public long id = -1;
    public Intent intent;
    public CharSequence summary;
    public int summaryRes;
    public CharSequence title;
    public int titleRes;

    public DashboardTile() {
    }

    public CharSequence getTitle(Resources res) {
        return this.titleRes != 0 ? res.getText(this.titleRes) : this.title;
    }

    public CharSequence getSummary(Resources res) {
        return this.summaryRes != 0 ? res.getText(this.summaryRes) : this.summary;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeInt(this.titleRes);
        TextUtils.writeToParcel(this.title, dest, flags);
        dest.writeInt(this.summaryRes);
        TextUtils.writeToParcel(this.summary, dest, flags);
        dest.writeInt(this.iconRes);
        dest.writeString(this.fragment);
        dest.writeBundle(this.fragmentArguments);
        if (this.intent != null) {
            dest.writeInt(1);
            this.intent.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeBundle(this.extras);
    }

    public void readFromParcel(Parcel in) {
        this.id = in.readLong();
        this.titleRes = in.readInt();
        this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.summaryRes = in.readInt();
        this.summary = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.iconRes = in.readInt();
        this.fragment = in.readString();
        this.fragmentArguments = in.readBundle();
        if (in.readInt() != 0) {
            this.intent = (Intent) Intent.CREATOR.createFromParcel(in);
        }
        this.extras = in.readBundle();
    }

    DashboardTile(Parcel in) {
        readFromParcel(in);
    }
}
