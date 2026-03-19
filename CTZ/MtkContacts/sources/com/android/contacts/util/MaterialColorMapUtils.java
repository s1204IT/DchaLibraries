package com.android.contacts.util;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import android.support.v4.content.ContextCompat;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;

public class MaterialColorMapUtils {
    private final TypedArray sPrimaryColors;
    private final TypedArray sSecondaryColors;

    public MaterialColorMapUtils(Resources resources) {
        this.sPrimaryColors = resources.obtainTypedArray(R.array.letter_tile_colors);
        this.sSecondaryColors = resources.obtainTypedArray(R.array.letter_tile_colors_dark);
    }

    public static class MaterialPalette implements Parcelable {
        public static final Parcelable.Creator<MaterialPalette> CREATOR = new Parcelable.Creator<MaterialPalette>() {
            @Override
            public MaterialPalette createFromParcel(Parcel parcel) {
                return new MaterialPalette(parcel);
            }

            @Override
            public MaterialPalette[] newArray(int i) {
                return new MaterialPalette[i];
            }
        };
        public final int mPrimaryColor;
        public final int mSecondaryColor;

        public MaterialPalette(int i, int i2) {
            this.mPrimaryColor = i;
            this.mSecondaryColor = i2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MaterialPalette materialPalette = (MaterialPalette) obj;
            if (this.mPrimaryColor == materialPalette.mPrimaryColor && this.mSecondaryColor == materialPalette.mSecondaryColor) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * (this.mPrimaryColor + 31)) + this.mSecondaryColor;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mPrimaryColor);
            parcel.writeInt(this.mSecondaryColor);
        }

        private MaterialPalette(Parcel parcel) {
            this.mPrimaryColor = parcel.readInt();
            this.mSecondaryColor = parcel.readInt();
        }
    }

    public MaterialPalette calculatePrimaryAndSecondaryColor(int i) {
        Trace.beginSection("calculatePrimaryAndSecondaryColor");
        float fHue = hue(i);
        int i2 = 0;
        float f = Float.MAX_VALUE;
        for (int i3 = 0; i3 < this.sPrimaryColors.length(); i3++) {
            float fAbs = Math.abs(hue(this.sPrimaryColors.getColor(i3, 0)) - fHue);
            if (fAbs < f) {
                i2 = i3;
                f = fAbs;
            }
        }
        Trace.endSection();
        return new MaterialPalette(this.sPrimaryColors.getColor(i2, 0), this.sSecondaryColors.getColor(i2, 0));
    }

    public static MaterialPalette getDefaultPrimaryAndSecondaryColors(Resources resources) {
        return new MaterialPalette(resources.getColor(R.color.quickcontact_default_photo_tint_color), resources.getColor(R.color.quickcontact_default_photo_tint_color_dark));
    }

    public static float hue(int i) {
        float f;
        int i2 = (i >> 16) & 255;
        int i3 = (i >> 8) & 255;
        int i4 = i & 255;
        int iMax = Math.max(i4, Math.max(i2, i3));
        int iMin = Math.min(i4, Math.min(i2, i3));
        if (iMax == iMin) {
            return ContactPhotoManager.OFFSET_DEFAULT;
        }
        float f2 = iMax - iMin;
        float f3 = (iMax - i2) / f2;
        float f4 = (iMax - i3) / f2;
        float f5 = (iMax - i4) / f2;
        if (i2 == iMax) {
            f = f5 - f4;
        } else if (i3 == iMax) {
            f = (2.0f + f3) - f5;
        } else {
            f = (4.0f + f4) - f3;
        }
        float f6 = f / 6.0f;
        if (f6 < ContactPhotoManager.OFFSET_DEFAULT) {
            return f6 + 1.0f;
        }
        return f6;
    }

    public static int getStatusBarColor(Activity activity) {
        if ((activity instanceof PeopleActivity) && ((PeopleActivity) activity).isGroupView()) {
            return ContextCompat.getColor(activity, R.color.group_primary_color_dark);
        }
        return ContextCompat.getColor(activity, R.color.primary_color_dark);
    }

    public static int getToolBarColor(Activity activity) {
        if ((activity instanceof PeopleActivity) && ((PeopleActivity) activity).isGroupView()) {
            return ContextCompat.getColor(activity, R.color.group_primary_color);
        }
        return ContextCompat.getColor(activity, R.color.primary_color);
    }
}
