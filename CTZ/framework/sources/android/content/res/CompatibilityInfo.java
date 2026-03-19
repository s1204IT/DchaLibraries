package android.content.res;

import android.content.pm.ApplicationInfo;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.android.internal.logging.nano.MetricsProto;

public class CompatibilityInfo implements Parcelable {
    private static final int ALWAYS_NEEDS_COMPAT = 2;
    public static final int DEFAULT_NORMAL_SHORT_DIMENSION = 320;
    public static final float MAXIMUM_ASPECT_RATIO = 1.7791667f;
    private static final int NEEDS_COMPAT_RES = 16;
    private static final int NEEDS_SCREEN_COMPAT = 8;
    private static final int NEVER_NEEDS_COMPAT = 4;
    private static final int SCALING_REQUIRED = 1;
    public final int applicationDensity;
    public final float applicationInvertedScale;
    public final float applicationScale;
    private final int mCompatibilityFlags;
    public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {
    };
    public static final Parcelable.Creator<CompatibilityInfo> CREATOR = new Parcelable.Creator<CompatibilityInfo>() {
        @Override
        public CompatibilityInfo createFromParcel(Parcel parcel) {
            return new CompatibilityInfo(parcel);
        }

        @Override
        public CompatibilityInfo[] newArray(int i) {
            return new CompatibilityInfo[i];
        }
    };

    public CompatibilityInfo(ApplicationInfo applicationInfo, int i, int i2, boolean z) {
        int i3;
        int i4;
        int i5;
        int i6;
        boolean z2;
        int i7 = 0;
        if (applicationInfo.targetSdkVersion < 26) {
            i3 = 16;
        } else {
            i3 = 0;
        }
        if (applicationInfo.requiresSmallestWidthDp != 0 || applicationInfo.compatibleWidthLimitDp != 0 || applicationInfo.largestWidthLimitDp != 0) {
            if (applicationInfo.requiresSmallestWidthDp != 0) {
                i4 = applicationInfo.requiresSmallestWidthDp;
            } else {
                i4 = applicationInfo.compatibleWidthLimitDp;
            }
            i4 = i4 == 0 ? applicationInfo.largestWidthLimitDp : i4;
            if (applicationInfo.compatibleWidthLimitDp != 0) {
                i5 = applicationInfo.compatibleWidthLimitDp;
            } else {
                i5 = i4;
            }
            i5 = i5 < i4 ? i4 : i5;
            int i8 = applicationInfo.largestWidthLimitDp;
            if (i4 > 320) {
                i3 |= 4;
            } else if (i8 != 0 && i2 > i8) {
                i3 |= 10;
            } else if (i5 >= i2) {
                i3 |= 4;
            } else if (z) {
                i3 |= 8;
            }
            i6 = i3;
            this.applicationDensity = DisplayMetrics.DENSITY_DEVICE;
            this.applicationScale = 1.0f;
            this.applicationInvertedScale = 1.0f;
        } else {
            if ((applicationInfo.flags & 2048) == 0) {
                z2 = false;
            } else if (z) {
                z2 = true;
                i7 = 8;
            } else {
                i7 = 42;
                z2 = true;
            }
            if ((applicationInfo.flags & 524288) != 0) {
                i7 = z ? i7 : i7 | 34;
                z2 = true;
            }
            if ((applicationInfo.flags & 4096) != 0) {
                i7 |= 2;
                z2 = true;
            }
            i7 = z ? i7 & (-3) : i7;
            i6 = i3 | 8;
            switch (i & 15) {
                case 3:
                    i6 = (i7 & 8) != 0 ? i6 & (-9) : i6;
                    if ((applicationInfo.flags & 2048) != 0) {
                        i6 |= 4;
                    }
                    break;
                case 4:
                    i6 = (i7 & 32) != 0 ? i6 & (-9) : i6;
                    if ((applicationInfo.flags & 524288) != 0) {
                        i6 |= 4;
                    }
                    break;
            }
            if ((i & 268435456) != 0) {
                if ((i7 & 2) != 0) {
                    i6 &= -9;
                } else if (!z2) {
                    i6 |= 2;
                }
            } else {
                i6 = (i6 & (-9)) | 4;
            }
            if ((applicationInfo.flags & 8192) != 0) {
                this.applicationDensity = DisplayMetrics.DENSITY_DEVICE;
                this.applicationScale = 1.0f;
                this.applicationInvertedScale = 1.0f;
            } else {
                this.applicationDensity = 160;
                this.applicationScale = DisplayMetrics.DENSITY_DEVICE / 160.0f;
                this.applicationInvertedScale = 1.0f / this.applicationScale;
                i6 |= 1;
            }
        }
        this.mCompatibilityFlags = i6;
    }

    private CompatibilityInfo(int i, int i2, float f, float f2) {
        this.mCompatibilityFlags = i;
        this.applicationDensity = i2;
        this.applicationScale = f;
        this.applicationInvertedScale = f2;
    }

    private CompatibilityInfo() {
        this(4, DisplayMetrics.DENSITY_DEVICE, 1.0f, 1.0f);
    }

    public boolean isScalingRequired() {
        return (this.mCompatibilityFlags & 1) != 0;
    }

    public boolean supportsScreen() {
        return (this.mCompatibilityFlags & 8) == 0;
    }

    public boolean neverSupportsScreen() {
        return (this.mCompatibilityFlags & 2) != 0;
    }

    public boolean alwaysSupportsScreen() {
        return (this.mCompatibilityFlags & 4) != 0;
    }

    public boolean needsCompatResources() {
        return (this.mCompatibilityFlags & 16) != 0;
    }

    public Translator getTranslator() {
        if (isScalingRequired()) {
            return new Translator(this);
        }
        return null;
    }

    public class Translator {
        public final float applicationInvertedScale;
        public final float applicationScale;
        private Rect mContentInsetsBuffer;
        private Region mTouchableAreaBuffer;
        private Rect mVisibleInsetsBuffer;

        Translator(float f, float f2) {
            this.mContentInsetsBuffer = null;
            this.mVisibleInsetsBuffer = null;
            this.mTouchableAreaBuffer = null;
            this.applicationScale = f;
            this.applicationInvertedScale = f2;
        }

        Translator(CompatibilityInfo compatibilityInfo) {
            this(compatibilityInfo.applicationScale, compatibilityInfo.applicationInvertedScale);
        }

        public void translateRectInScreenToAppWinFrame(Rect rect) {
            rect.scale(this.applicationInvertedScale);
        }

        public void translateRegionInWindowToScreen(Region region) {
            region.scale(this.applicationScale);
        }

        public void translateCanvas(Canvas canvas) {
            if (this.applicationScale == 1.5f) {
                canvas.translate(0.0026143792f, 0.0026143792f);
            }
            canvas.scale(this.applicationScale, this.applicationScale);
        }

        public void translateEventInScreenToAppWindow(MotionEvent motionEvent) {
            motionEvent.scale(this.applicationInvertedScale);
        }

        public void translateWindowLayout(WindowManager.LayoutParams layoutParams) {
            layoutParams.scale(this.applicationScale);
        }

        public void translateRectInAppWindowToScreen(Rect rect) {
            rect.scale(this.applicationScale);
        }

        public void translateRectInScreenToAppWindow(Rect rect) {
            rect.scale(this.applicationInvertedScale);
        }

        public void translatePointInScreenToAppWindow(PointF pointF) {
            float f = this.applicationInvertedScale;
            if (f != 1.0f) {
                pointF.x *= f;
                pointF.y *= f;
            }
        }

        public void translateLayoutParamsInAppWindowToScreen(WindowManager.LayoutParams layoutParams) {
            layoutParams.scale(this.applicationScale);
        }

        public Rect getTranslatedContentInsets(Rect rect) {
            if (this.mContentInsetsBuffer == null) {
                this.mContentInsetsBuffer = new Rect();
            }
            this.mContentInsetsBuffer.set(rect);
            translateRectInAppWindowToScreen(this.mContentInsetsBuffer);
            return this.mContentInsetsBuffer;
        }

        public Rect getTranslatedVisibleInsets(Rect rect) {
            if (this.mVisibleInsetsBuffer == null) {
                this.mVisibleInsetsBuffer = new Rect();
            }
            this.mVisibleInsetsBuffer.set(rect);
            translateRectInAppWindowToScreen(this.mVisibleInsetsBuffer);
            return this.mVisibleInsetsBuffer;
        }

        public Region getTranslatedTouchableArea(Region region) {
            if (this.mTouchableAreaBuffer == null) {
                this.mTouchableAreaBuffer = new Region();
            }
            this.mTouchableAreaBuffer.set(region);
            this.mTouchableAreaBuffer.scale(this.applicationScale);
            return this.mTouchableAreaBuffer;
        }
    }

    public void applyToDisplayMetrics(DisplayMetrics displayMetrics) {
        if (!supportsScreen()) {
            computeCompatibleScaling(displayMetrics, displayMetrics);
        } else {
            displayMetrics.widthPixels = displayMetrics.noncompatWidthPixels;
            displayMetrics.heightPixels = displayMetrics.noncompatHeightPixels;
        }
        if (isScalingRequired()) {
            float f = this.applicationInvertedScale;
            displayMetrics.density = displayMetrics.noncompatDensity * f;
            displayMetrics.densityDpi = (int) ((displayMetrics.noncompatDensityDpi * f) + 0.5f);
            displayMetrics.scaledDensity = displayMetrics.noncompatScaledDensity * f;
            displayMetrics.xdpi = displayMetrics.noncompatXdpi * f;
            displayMetrics.ydpi = displayMetrics.noncompatYdpi * f;
            displayMetrics.widthPixels = (int) ((displayMetrics.widthPixels * f) + 0.5f);
            displayMetrics.heightPixels = (int) ((displayMetrics.heightPixels * f) + 0.5f);
        }
    }

    public void applyToConfiguration(int i, Configuration configuration) {
        if (!supportsScreen()) {
            configuration.screenLayout = (configuration.screenLayout & (-16)) | 2;
            configuration.screenWidthDp = configuration.compatScreenWidthDp;
            configuration.screenHeightDp = configuration.compatScreenHeightDp;
            configuration.smallestScreenWidthDp = configuration.compatSmallestScreenWidthDp;
        }
        configuration.densityDpi = i;
        if (isScalingRequired()) {
            configuration.densityDpi = (int) ((configuration.densityDpi * this.applicationInvertedScale) + 0.5f);
        }
    }

    public static float computeCompatibleScaling(DisplayMetrics displayMetrics, DisplayMetrics displayMetrics2) {
        int i;
        int i2;
        int i3 = displayMetrics.noncompatWidthPixels;
        int i4 = displayMetrics.noncompatHeightPixels;
        if (i3 < i4) {
            i2 = i3;
            i = i4;
        } else {
            i = i3;
            i2 = i4;
        }
        int i5 = (int) ((320.0f * displayMetrics.density) + 0.5f);
        float f = i / i2;
        if (f > 1.7791667f) {
            f = 1.7791667f;
        }
        int i6 = (int) ((i5 * f) + 0.5f);
        if (i3 < i4) {
            i5 = i6;
            i6 = i5;
        }
        float f2 = i3 / i6;
        float f3 = i4 / i5;
        if (f2 >= f3) {
            f2 = f3;
        }
        if (f2 < 1.0f) {
            f2 = 1.0f;
        }
        if (displayMetrics2 != null) {
            displayMetrics2.widthPixels = i6;
            displayMetrics2.heightPixels = i5;
        }
        return f2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        try {
            CompatibilityInfo compatibilityInfo = (CompatibilityInfo) obj;
            if (this.mCompatibilityFlags != compatibilityInfo.mCompatibilityFlags || this.applicationDensity != compatibilityInfo.applicationDensity || this.applicationScale != compatibilityInfo.applicationScale) {
                return false;
            }
            if (this.applicationInvertedScale == compatibilityInfo.applicationInvertedScale) {
                return true;
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{");
        sb.append(this.applicationDensity);
        sb.append("dpi");
        if (isScalingRequired()) {
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.applicationScale);
            sb.append("x");
        }
        if (!supportsScreen()) {
            sb.append(" resizing");
        }
        if (neverSupportsScreen()) {
            sb.append(" never-compat");
        }
        if (alwaysSupportsScreen()) {
            sb.append(" always-compat");
        }
        sb.append("}");
        return sb.toString();
    }

    public int hashCode() {
        return (31 * (((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.mCompatibilityFlags) * 31) + this.applicationDensity) * 31) + Float.floatToIntBits(this.applicationScale))) + Float.floatToIntBits(this.applicationInvertedScale);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCompatibilityFlags);
        parcel.writeInt(this.applicationDensity);
        parcel.writeFloat(this.applicationScale);
        parcel.writeFloat(this.applicationInvertedScale);
    }

    private CompatibilityInfo(Parcel parcel) {
        this.mCompatibilityFlags = parcel.readInt();
        this.applicationDensity = parcel.readInt();
        this.applicationScale = parcel.readFloat();
        this.applicationInvertedScale = parcel.readFloat();
    }
}
