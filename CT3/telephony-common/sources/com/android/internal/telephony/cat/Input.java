package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;

public class Input implements Parcelable {
    public static final Parcelable.Creator<Input> CREATOR = new Parcelable.Creator<Input>() {
        @Override
        public Input createFromParcel(Parcel in) {
            return new Input(in, null);
        }

        @Override
        public Input[] newArray(int size) {
            return new Input[size];
        }
    };
    public String defaultText;
    public boolean digitOnly;
    public Duration duration;
    public boolean echo;
    public boolean helpAvailable;
    public Bitmap icon;
    public boolean iconSelfExplanatory;
    public int maxLen;
    public int minLen;
    public boolean packed;
    public String text;
    public boolean ucs2;
    public boolean yesNo;

    Input(Parcel in, Input input) {
        this(in);
    }

    Input() {
        this.text = UsimPBMemInfo.STRING_NOT_SET;
        this.defaultText = null;
        this.icon = null;
        this.minLen = 0;
        this.maxLen = 1;
        this.ucs2 = false;
        this.packed = false;
        this.digitOnly = false;
        this.echo = false;
        this.yesNo = false;
        this.helpAvailable = false;
        this.duration = null;
        this.iconSelfExplanatory = false;
    }

    private Input(Parcel in) {
        this.text = in.readString();
        this.defaultText = in.readString();
        this.icon = (Bitmap) in.readParcelable(null);
        this.minLen = in.readInt();
        this.maxLen = in.readInt();
        this.ucs2 = in.readInt() == 1;
        this.packed = in.readInt() == 1;
        this.digitOnly = in.readInt() == 1;
        this.echo = in.readInt() == 1;
        this.yesNo = in.readInt() == 1;
        this.helpAvailable = in.readInt() == 1;
        this.duration = (Duration) in.readParcelable(null);
        this.iconSelfExplanatory = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.text);
        dest.writeString(this.defaultText);
        dest.writeParcelable(this.icon, 0);
        dest.writeInt(this.minLen);
        dest.writeInt(this.maxLen);
        dest.writeInt(this.ucs2 ? 1 : 0);
        dest.writeInt(this.packed ? 1 : 0);
        dest.writeInt(this.digitOnly ? 1 : 0);
        dest.writeInt(this.echo ? 1 : 0);
        dest.writeInt(this.yesNo ? 1 : 0);
        dest.writeInt(this.helpAvailable ? 1 : 0);
        dest.writeParcelable(this.duration, 0);
        dest.writeInt(this.iconSelfExplanatory ? 1 : 0);
    }

    boolean setIcon(Bitmap Icon) {
        return true;
    }

    public static Input getInstance() {
        Input self = new Input();
        return self;
    }
}
