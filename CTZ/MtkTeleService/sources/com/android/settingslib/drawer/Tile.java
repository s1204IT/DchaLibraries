package com.android.settingslib.drawer;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.widget.RemoteViews;
import java.util.ArrayList;

public class Tile implements Parcelable {
    public static final Parcelable.Creator<Tile> CREATOR = new Parcelable.Creator<Tile>() {
        @Override
        public Tile createFromParcel(Parcel parcel) {
            return new Tile(parcel);
        }

        @Override
        public Tile[] newArray(int i) {
            return new Tile[i];
        }
    };
    public String category;
    public Bundle extras;
    public Icon icon;
    public Intent intent;
    public boolean isIconTintable;
    public String key;
    public Bundle metaData;
    public int priority;
    public RemoteViews remoteViews;
    public CharSequence summary;
    public CharSequence title;
    public ArrayList<UserHandle> userHandle = new ArrayList<>();

    public Tile() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        TextUtils.writeToParcel(this.title, parcel, i);
        TextUtils.writeToParcel(this.summary, parcel, i);
        if (this.icon != null) {
            parcel.writeByte((byte) 1);
            this.icon.writeToParcel(parcel, i);
        } else {
            parcel.writeByte((byte) 0);
        }
        if (this.intent != null) {
            parcel.writeByte((byte) 1);
            this.intent.writeToParcel(parcel, i);
        } else {
            parcel.writeByte((byte) 0);
        }
        int size = this.userHandle.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            this.userHandle.get(i2).writeToParcel(parcel, i);
        }
        parcel.writeBundle(this.extras);
        parcel.writeString(this.category);
        parcel.writeInt(this.priority);
        parcel.writeBundle(this.metaData);
        parcel.writeString(this.key);
        parcel.writeParcelable(this.remoteViews, i);
        parcel.writeBoolean(this.isIconTintable);
    }

    public void readFromParcel(Parcel parcel) {
        this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.summary = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        if (parcel.readByte() != 0) {
            this.icon = (Icon) Icon.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readByte() != 0) {
            this.intent = (Intent) Intent.CREATOR.createFromParcel(parcel);
        }
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            this.userHandle.add((UserHandle) UserHandle.CREATOR.createFromParcel(parcel));
        }
        this.extras = parcel.readBundle();
        this.category = parcel.readString();
        this.priority = parcel.readInt();
        this.metaData = parcel.readBundle();
        this.key = parcel.readString();
        this.remoteViews = (RemoteViews) parcel.readParcelable(RemoteViews.class.getClassLoader());
        this.isIconTintable = parcel.readBoolean();
    }

    Tile(Parcel parcel) {
        readFromParcel(parcel);
    }
}
