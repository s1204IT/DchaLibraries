package com.android.contacts.common.util;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Trace;
import com.android.contacts.R;

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
            public MaterialPalette createFromParcel(Parcel in) {
                return new MaterialPalette(in);
            }

            @Override
            public MaterialPalette[] newArray(int size) {
                return new MaterialPalette[size];
            }
        };
        public final int mPrimaryColor;
        public final int mSecondaryColor;

        public MaterialPalette(int primaryColor, int secondaryColor) {
            this.mPrimaryColor = primaryColor;
            this.mSecondaryColor = secondaryColor;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                MaterialPalette other = (MaterialPalette) obj;
                return this.mPrimaryColor == other.mPrimaryColor && this.mSecondaryColor == other.mSecondaryColor;
            }
            return false;
        }

        public int hashCode() {
            int result = this.mPrimaryColor + 31;
            return (result * 31) + this.mSecondaryColor;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mPrimaryColor);
            dest.writeInt(this.mSecondaryColor);
        }

        private MaterialPalette(Parcel in) {
            this.mPrimaryColor = in.readInt();
            this.mSecondaryColor = in.readInt();
        }
    }

    public MaterialPalette calculatePrimaryAndSecondaryColor(int color) {
        Trace.beginSection("calculatePrimaryAndSecondaryColor");
        float colorHue = hue(color);
        float minimumDistance = Float.MAX_VALUE;
        int indexBestMatch = 0;
        for (int i = 0; i < this.sPrimaryColors.length(); i++) {
            int primaryColor = this.sPrimaryColors.getColor(i, 0);
            float comparedHue = hue(primaryColor);
            float distance = Math.abs(comparedHue - colorHue);
            if (distance < minimumDistance) {
                minimumDistance = distance;
                indexBestMatch = i;
            }
        }
        Trace.endSection();
        return new MaterialPalette(this.sPrimaryColors.getColor(indexBestMatch, 0), this.sSecondaryColors.getColor(indexBestMatch, 0));
    }

    public static MaterialPalette getDefaultPrimaryAndSecondaryColors(Resources resources) {
        int primaryColor = resources.getColor(R.color.quickcontact_default_photo_tint_color);
        int secondaryColor = resources.getColor(R.color.quickcontact_default_photo_tint_color_dark);
        return new MaterialPalette(primaryColor, secondaryColor);
    }

    public static float hue(int color) {
        float H;
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        int V = Math.max(b, Math.max(r, g));
        int temp = Math.min(b, Math.min(r, g));
        if (V == temp) {
            return 0.0f;
        }
        float vtemp = V - temp;
        float cr = (V - r) / vtemp;
        float cg = (V - g) / vtemp;
        float cb = (V - b) / vtemp;
        if (r == V) {
            H = cb - cg;
        } else if (g == V) {
            H = (2.0f + cr) - cb;
        } else {
            H = (4.0f + cg) - cr;
        }
        float H2 = H / 6.0f;
        if (H2 < 0.0f) {
            return H2 + 1.0f;
        }
        return H2;
    }
}
