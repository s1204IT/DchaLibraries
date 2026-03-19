package android.app;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.PrintWriter;

public final class Vr2dDisplayProperties implements Parcelable {
    public static final Parcelable.Creator<Vr2dDisplayProperties> CREATOR = new Parcelable.Creator<Vr2dDisplayProperties>() {
        @Override
        public Vr2dDisplayProperties createFromParcel(Parcel parcel) {
            return new Vr2dDisplayProperties(parcel);
        }

        @Override
        public Vr2dDisplayProperties[] newArray(int i) {
            return new Vr2dDisplayProperties[i];
        }
    };
    public static final int FLAG_VIRTUAL_DISPLAY_ENABLED = 1;
    private final int mAddedFlags;
    private final int mDpi;
    private final int mHeight;
    private final int mRemovedFlags;
    private final int mWidth;

    public Vr2dDisplayProperties(int i, int i2, int i3) {
        this(i, i2, i3, 0, 0);
    }

    private Vr2dDisplayProperties(int i, int i2, int i3, int i4, int i5) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mDpi = i3;
        this.mAddedFlags = i4;
        this.mRemovedFlags = i5;
    }

    public int hashCode() {
        return (31 * ((getWidth() * 31) + getHeight())) + getDpi();
    }

    public String toString() {
        return "Vr2dDisplayProperties{mWidth=" + this.mWidth + ", mHeight=" + this.mHeight + ", mDpi=" + this.mDpi + ", flags=" + toReadableFlags(this.mAddedFlags) + ", removed_flags=" + toReadableFlags(this.mRemovedFlags) + "}";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Vr2dDisplayProperties vr2dDisplayProperties = (Vr2dDisplayProperties) obj;
        if (getFlags() == vr2dDisplayProperties.getFlags() && getRemovedFlags() == vr2dDisplayProperties.getRemovedFlags() && getWidth() == vr2dDisplayProperties.getWidth() && getHeight() == vr2dDisplayProperties.getHeight() && getDpi() == vr2dDisplayProperties.getDpi()) {
            return true;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mWidth);
        parcel.writeInt(this.mHeight);
        parcel.writeInt(this.mDpi);
        parcel.writeInt(this.mAddedFlags);
        parcel.writeInt(this.mRemovedFlags);
    }

    private Vr2dDisplayProperties(Parcel parcel) {
        this.mWidth = parcel.readInt();
        this.mHeight = parcel.readInt();
        this.mDpi = parcel.readInt();
        this.mAddedFlags = parcel.readInt();
        this.mRemovedFlags = parcel.readInt();
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + toString());
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getDpi() {
        return this.mDpi;
    }

    public int getFlags() {
        return this.mAddedFlags;
    }

    public int getRemovedFlags() {
        return this.mRemovedFlags;
    }

    private static String toReadableFlags(int i) {
        String str = "{";
        if ((i & 1) == 1) {
            str = "{enabled";
        }
        return str + "}";
    }

    public static class Builder {
        private int mAddedFlags = 0;
        private int mRemovedFlags = 0;
        private int mWidth = -1;
        private int mHeight = -1;
        private int mDpi = -1;

        public Builder setDimensions(int i, int i2, int i3) {
            this.mWidth = i;
            this.mHeight = i2;
            this.mDpi = i3;
            return this;
        }

        public Builder setEnabled(boolean z) {
            if (z) {
                addFlags(1);
            } else {
                removeFlags(1);
            }
            return this;
        }

        public Builder addFlags(int i) {
            this.mAddedFlags |= i;
            this.mRemovedFlags = (~i) & this.mRemovedFlags;
            return this;
        }

        public Builder removeFlags(int i) {
            this.mRemovedFlags |= i;
            this.mAddedFlags = (~i) & this.mAddedFlags;
            return this;
        }

        public Vr2dDisplayProperties build() {
            return new Vr2dDisplayProperties(this.mWidth, this.mHeight, this.mDpi, this.mAddedFlags, this.mRemovedFlags);
        }
    }
}
