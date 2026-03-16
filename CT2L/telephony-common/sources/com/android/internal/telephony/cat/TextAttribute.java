package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class TextAttribute implements Parcelable {
    public static final Parcelable.Creator<TextAttribute> CREATOR = new Parcelable.Creator<TextAttribute>() {
        @Override
        public TextAttribute createFromParcel(Parcel in) {
            return new TextAttribute(in);
        }

        @Override
        public TextAttribute[] newArray(int size) {
            return new TextAttribute[size];
        }
    };
    public TextAlignment align;
    public boolean bold;
    public TextColor color;
    public TextColor colorBG;
    public boolean italic;
    public int length;
    public FontSize size;
    public int start;
    public boolean strikeThrough;
    public boolean underlined;

    public TextAttribute(int start, int length, TextAlignment align, FontSize size, boolean bold, boolean italic, boolean underlined, boolean strikeThrough, TextColor color, TextColor colorBG) {
        this.start = start;
        this.length = length;
        this.align = align;
        this.size = size;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikeThrough = strikeThrough;
        this.color = color;
        this.colorBG = colorBG;
    }

    private TextAttribute(Parcel in) {
        this.start = in.readInt();
        this.length = in.readInt();
        this.align = TextAlignment.values()[in.readInt()];
        this.size = FontSize.values()[in.readInt()];
        int format = in.readInt();
        this.bold = (format & 16) != 0;
        this.italic = (format & 32) != 0;
        this.underlined = (format & 64) != 0;
        this.strikeThrough = (format & 128) != 0;
        this.color = TextColor.values()[in.readInt()];
        this.colorBG = TextColor.values()[in.readInt()];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.start);
        dest.writeInt(this.length);
        dest.writeInt(this.align.ordinal());
        dest.writeInt(this.size.ordinal());
        int format = (this.underlined ? 64 : 0) | (this.italic ? 32 : 0) | (this.bold ? 16 : 0) | (this.strikeThrough ? 128 : 0);
        dest.writeInt(format);
        dest.writeInt(this.color.ordinal());
        dest.writeInt(this.colorBG.ordinal());
    }
}
