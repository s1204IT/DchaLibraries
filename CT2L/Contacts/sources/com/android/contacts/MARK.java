package com.android.contacts;

import android.os.Parcel;
import android.os.Parcelable;

public class MARK implements Parcelable {
    public static final Parcelable.Creator<MARK> CREATOR = new Parcelable.Creator<MARK>() {
        @Override
        public MARK createFromParcel(Parcel source) {
            long raw_id = source.readLong();
            long contact_id = source.readLong();
            boolean checked = source.readInt() != 0;
            return new MARK(raw_id, contact_id, checked);
        }

        @Override
        public MARK[] newArray(int size) {
            return new MARK[size];
        }
    };
    public boolean checked;
    public long contact_id;
    public long raw_id;

    public MARK(long raw_id, boolean checked) {
        this.raw_id = raw_id;
        this.checked = checked;
    }

    public MARK(long raw_id, long contact_id, boolean checked) {
        this.raw_id = raw_id;
        this.contact_id = contact_id;
        this.checked = checked;
    }

    public MARK() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.raw_id);
        dest.writeLong(this.contact_id);
        dest.writeInt(this.checked ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
