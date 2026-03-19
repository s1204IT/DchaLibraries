package com.mediatek.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telephony.cat.Menu;

public class MtkMenu extends Menu {
    public static final Parcelable.Creator<MtkMenu> CREATOR = new Parcelable.Creator<MtkMenu>() {
        @Override
        public MtkMenu createFromParcel(Parcel parcel) {
            return new MtkMenu(parcel);
        }

        @Override
        public MtkMenu[] newArray(int i) {
            return new MtkMenu[i];
        }
    };
    public int mFromMD;
    public byte[] nextActionIndicator;

    public MtkMenu() {
        this.nextActionIndicator = null;
        this.mFromMD = 0;
    }

    private MtkMenu(Parcel parcel) {
        super(parcel);
        this.mFromMD = parcel.readInt();
        int i = parcel.readInt();
        if (i <= 0) {
            this.nextActionIndicator = null;
        } else {
            this.nextActionIndicator = new byte[i];
            parcel.readByteArray(this.nextActionIndicator);
        }
        MtkCatLog.d("[MtkMenu]", "Menu: " + this.mFromMD);
    }

    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.mFromMD);
        parcel.writeInt(this.nextActionIndicator == null ? -1 : this.nextActionIndicator.length);
        if (this.nextActionIndicator != null && this.nextActionIndicator.length > 0) {
            parcel.writeByteArray(this.nextActionIndicator);
        }
        MtkCatLog.w("[MtkMenu]", "writeToParcel: " + this.mFromMD);
    }

    public int getSetUpMenuFlag() {
        return this.mFromMD;
    }

    public void setSetUpMenuFlag(int i) {
        this.mFromMD = i;
    }
}
