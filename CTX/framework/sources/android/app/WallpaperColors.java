package android.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.graphics.palette.Palette;
import com.android.internal.graphics.palette.VariationalKMeansQuantizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class WallpaperColors implements Parcelable {
    private static final float BRIGHT_IMAGE_MEAN_LUMINANCE = 0.75f;
    public static final Parcelable.Creator<WallpaperColors> CREATOR = new Parcelable.Creator<WallpaperColors>() {
        @Override
        public WallpaperColors createFromParcel(Parcel parcel) {
            return new WallpaperColors(parcel);
        }

        @Override
        public WallpaperColors[] newArray(int i) {
            return new WallpaperColors[i];
        }
    };
    private static final float DARK_PIXEL_LUMINANCE = 0.45f;
    private static final float DARK_THEME_MEAN_LUMINANCE = 0.25f;
    public static final int HINT_FROM_BITMAP = 4;
    public static final int HINT_SUPPORTS_DARK_TEXT = 1;
    public static final int HINT_SUPPORTS_DARK_THEME = 2;
    private static final int MAX_BITMAP_SIZE = 112;
    private static final float MAX_DARK_AREA = 0.05f;
    private static final int MAX_WALLPAPER_EXTRACTION_AREA = 12544;
    private static final float MIN_COLOR_OCCURRENCE = 0.05f;
    private int mColorHints;
    private final ArrayList<Color> mMainColors;

    public WallpaperColors(Parcel parcel) {
        this.mMainColors = new ArrayList<>();
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            this.mMainColors.add(Color.valueOf(parcel.readInt()));
        }
        this.mColorHints = parcel.readInt();
    }

    public static WallpaperColors fromDrawable(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException("Drawable cannot be null");
        }
        Rect rectCopyBounds = drawable.copyBounds();
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            intrinsicWidth = 112;
            intrinsicHeight = 112;
        }
        Size sizeCalculateOptimalSize = calculateOptimalSize(intrinsicWidth, intrinsicHeight);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(sizeCalculateOptimalSize.getWidth(), sizeCalculateOptimalSize.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        drawable.setBounds(0, 0, bitmapCreateBitmap.getWidth(), bitmapCreateBitmap.getHeight());
        drawable.draw(canvas);
        WallpaperColors wallpaperColorsFromBitmap = fromBitmap(bitmapCreateBitmap);
        bitmapCreateBitmap.recycle();
        drawable.setBounds(rectCopyBounds);
        return wallpaperColorsFromBitmap;
    }

    public static WallpaperColors fromBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap can't be null");
        }
        boolean z = true;
        if (bitmap.getWidth() * bitmap.getHeight() > MAX_WALLPAPER_EXTRACTION_AREA) {
            Size sizeCalculateOptimalSize = calculateOptimalSize(bitmap.getWidth(), bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, sizeCalculateOptimalSize.getWidth(), sizeCalculateOptimalSize.getHeight(), true);
        } else {
            z = false;
        }
        ArrayList arrayList = new ArrayList(Palette.from(bitmap).setQuantizer(new VariationalKMeansQuantizer()).maximumColorCount(5).clearFilters().resizeBitmapArea(MAX_WALLPAPER_EXTRACTION_AREA).generate().getSwatches());
        final float width = bitmap.getWidth() * bitmap.getHeight() * 0.05f;
        arrayList.removeIf(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return WallpaperColors.lambda$fromBitmap$0(width, (Palette.Swatch) obj);
            }
        });
        arrayList.sort(new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return WallpaperColors.lambda$fromBitmap$1((Palette.Swatch) obj, (Palette.Swatch) obj2);
            }
        });
        int size = arrayList.size();
        Color color = null;
        Color color2 = null;
        Color color3 = null;
        for (int i = 0; i < size; i++) {
            Color colorValueOf = Color.valueOf(((Palette.Swatch) arrayList.get(i)).getRgb());
            switch (i) {
                case 0:
                    color = colorValueOf;
                    break;
                case 1:
                    color2 = colorValueOf;
                    break;
                case 2:
                    color3 = colorValueOf;
                    break;
                default:
                    int iCalculateDarkHints = calculateDarkHints(bitmap);
                    if (z) {
                        bitmap.recycle();
                    }
                    return new WallpaperColors(color, color2, color3, iCalculateDarkHints | 4);
            }
        }
        int iCalculateDarkHints2 = calculateDarkHints(bitmap);
        if (z) {
        }
        return new WallpaperColors(color, color2, color3, iCalculateDarkHints2 | 4);
    }

    static boolean lambda$fromBitmap$0(float f, Palette.Swatch swatch) {
        return ((float) swatch.getPopulation()) < f;
    }

    static int lambda$fromBitmap$1(Palette.Swatch swatch, Palette.Swatch swatch2) {
        return swatch2.getPopulation() - swatch.getPopulation();
    }

    public WallpaperColors(Color color, Color color2, Color color3) {
        this(color, color2, color3, 0);
    }

    public WallpaperColors(Color color, Color color2, Color color3, int i) {
        if (color == null) {
            throw new IllegalArgumentException("Primary color should never be null.");
        }
        this.mMainColors = new ArrayList<>(3);
        this.mMainColors.add(color);
        if (color2 != null) {
            this.mMainColors.add(color2);
        }
        if (color3 != null) {
            if (color2 == null) {
                throw new IllegalArgumentException("tertiaryColor can't be specified when secondaryColor is null");
            }
            this.mMainColors.add(color3);
        }
        this.mColorHints = i;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        List<Color> mainColors = getMainColors();
        int size = mainColors.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            parcel.writeInt(mainColors.get(i2).toArgb());
        }
        parcel.writeInt(this.mColorHints);
    }

    public Color getPrimaryColor() {
        return this.mMainColors.get(0);
    }

    public Color getSecondaryColor() {
        if (this.mMainColors.size() < 2) {
            return null;
        }
        return this.mMainColors.get(1);
    }

    public Color getTertiaryColor() {
        if (this.mMainColors.size() < 3) {
            return null;
        }
        return this.mMainColors.get(2);
    }

    public List<Color> getMainColors() {
        return Collections.unmodifiableList(this.mMainColors);
    }

    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        WallpaperColors wallpaperColors = (WallpaperColors) obj;
        return this.mMainColors.equals(wallpaperColors.mMainColors) && this.mColorHints == wallpaperColors.mColorHints;
    }

    public int hashCode() {
        return (31 * this.mMainColors.hashCode()) + this.mColorHints;
    }

    public int getColorHints() {
        return this.mColorHints;
    }

    public void setColorHints(int i) {
        this.mColorHints = i;
    }

    private static int calculateDarkHints(Bitmap bitmap) {
        int i = 0;
        if (bitmap == null) {
            return 0;
        }
        int[] iArr = new int[bitmap.getWidth() * bitmap.getHeight()];
        double d = 0.0d;
        int length = (int) (iArr.length * 0.05f);
        bitmap.getPixels(iArr, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        float[] fArr = new float[3];
        int i2 = 0;
        for (int i3 = 0; i3 < iArr.length; i3++) {
            ColorUtils.colorToHSL(iArr[i3], fArr);
            float f = fArr[2];
            int iAlpha = Color.alpha(iArr[i3]);
            if (f < DARK_PIXEL_LUMINANCE && iAlpha != 0) {
                i2++;
            }
            d += (double) f;
        }
        double length2 = d / ((double) iArr.length);
        if (length2 > 0.75d && i2 < length) {
            i = 1;
        }
        if (length2 < 0.25d) {
            return i | 2;
        }
        return i;
    }

    private static Size calculateOptimalSize(int i, int i2) {
        double dSqrt;
        int i3 = i * i2;
        if (i3 > MAX_WALLPAPER_EXTRACTION_AREA) {
            dSqrt = Math.sqrt(12544.0d / ((double) i3));
        } else {
            dSqrt = 1.0d;
        }
        int i4 = (int) (((double) i) * dSqrt);
        int i5 = (int) (((double) i2) * dSqrt);
        if (i4 == 0) {
            i4 = 1;
        }
        if (i5 == 0) {
            i5 = 1;
        }
        return new Size(i4, i5);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.mMainColors.size(); i++) {
            sb.append(Integer.toHexString(this.mMainColors.get(i).toArgb()));
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        }
        return "[WallpaperColors: " + sb.toString() + "h: " + this.mColorHints + "]";
    }
}
