package android.view;

import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.IccCardConstants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public final class Display {
    private static final int CACHED_APP_SIZE_DURATION_MILLIS = 20;
    public static final int COLOR_MODE_ADOBE_RGB = 8;
    public static final int COLOR_MODE_BT601_525 = 3;
    public static final int COLOR_MODE_BT601_525_UNADJUSTED = 4;
    public static final int COLOR_MODE_BT601_625 = 1;
    public static final int COLOR_MODE_BT601_625_UNADJUSTED = 2;
    public static final int COLOR_MODE_BT709 = 5;
    public static final int COLOR_MODE_DCI_P3 = 6;
    public static final int COLOR_MODE_DEFAULT = 0;
    public static final int COLOR_MODE_DISPLAY_P3 = 9;
    public static final int COLOR_MODE_INVALID = -1;
    public static final int COLOR_MODE_SRGB = 7;
    private static final boolean DEBUG = false;
    public static final int DEFAULT_DISPLAY = 0;
    public static final int FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 32;
    public static final int FLAG_PRESENTATION = 8;
    public static final int FLAG_PRIVATE = 4;
    public static final int FLAG_ROUND = 16;
    public static final int FLAG_SCALING_DISABLED = 1073741824;
    public static final int FLAG_SECURE = 2;
    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 1;
    public static final int INVALID_DISPLAY = -1;
    public static final int REMOVE_MODE_DESTROY_CONTENT = 1;
    public static final int REMOVE_MODE_MOVE_CONTENT_TO_PRIMARY = 0;
    public static final int STATE_DOZE = 3;
    public static final int STATE_DOZE_SUSPEND = 4;
    public static final int STATE_OFF = 1;
    public static final int STATE_ON = 2;
    public static final int STATE_ON_SUSPEND = 6;
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_VR = 5;
    private static final String TAG = "Display";
    public static final int TYPE_BUILT_IN = 1;
    public static final int TYPE_HDMI = 2;
    public static final int TYPE_OVERLAY = 4;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_VIRTUAL = 5;
    public static final int TYPE_WIFI = 3;
    private final String mAddress;
    private int mCachedAppHeightCompat;
    private int mCachedAppWidthCompat;
    private DisplayAdjustments mDisplayAdjustments;
    private final int mDisplayId;
    private DisplayInfo mDisplayInfo;
    private final int mFlags;
    private final DisplayManagerGlobal mGlobal;
    private boolean mIsValid;
    private long mLastCachedAppSizeUpdate;
    private final int mLayerStack;
    private final String mOwnerPackageName;
    private final int mOwnerUid;
    private final Resources mResources;
    private final DisplayMetrics mTempMetrics;
    private final int mType;

    public Display(DisplayManagerGlobal displayManagerGlobal, int i, DisplayInfo displayInfo, DisplayAdjustments displayAdjustments) {
        this(displayManagerGlobal, i, displayInfo, displayAdjustments, null);
    }

    public Display(DisplayManagerGlobal displayManagerGlobal, int i, DisplayInfo displayInfo, Resources resources) {
        this(displayManagerGlobal, i, displayInfo, null, resources);
    }

    private Display(DisplayManagerGlobal displayManagerGlobal, int i, DisplayInfo displayInfo, DisplayAdjustments displayAdjustments, Resources resources) {
        DisplayAdjustments displayAdjustments2;
        this.mTempMetrics = new DisplayMetrics();
        this.mGlobal = displayManagerGlobal;
        this.mDisplayId = i;
        this.mDisplayInfo = displayInfo;
        this.mResources = resources;
        if (this.mResources != null) {
            displayAdjustments2 = new DisplayAdjustments(this.mResources.getConfiguration());
        } else {
            displayAdjustments2 = displayAdjustments != null ? new DisplayAdjustments(displayAdjustments) : null;
        }
        this.mDisplayAdjustments = displayAdjustments2;
        this.mIsValid = true;
        this.mLayerStack = displayInfo.layerStack;
        this.mFlags = displayInfo.flags;
        this.mType = displayInfo.type;
        this.mAddress = displayInfo.address;
        this.mOwnerUid = displayInfo.ownerUid;
        this.mOwnerPackageName = displayInfo.ownerPackageName;
    }

    public int getDisplayId() {
        return this.mDisplayId;
    }

    public boolean isValid() {
        boolean z;
        synchronized (this) {
            updateDisplayInfoLocked();
            z = this.mIsValid;
        }
        return z;
    }

    public boolean getDisplayInfo(DisplayInfo displayInfo) {
        boolean z;
        synchronized (this) {
            updateDisplayInfoLocked();
            displayInfo.copyFrom(this.mDisplayInfo);
            z = this.mIsValid;
        }
        return z;
    }

    public int getLayerStack() {
        return this.mLayerStack;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public int getType() {
        return this.mType;
    }

    public String getAddress() {
        return this.mAddress;
    }

    public int getOwnerUid() {
        return this.mOwnerUid;
    }

    public String getOwnerPackageName() {
        return this.mOwnerPackageName;
    }

    public DisplayAdjustments getDisplayAdjustments() {
        if (this.mResources != null) {
            DisplayAdjustments displayAdjustments = this.mResources.getDisplayAdjustments();
            if (!this.mDisplayAdjustments.equals(displayAdjustments)) {
                this.mDisplayAdjustments = new DisplayAdjustments(displayAdjustments);
            }
        }
        return this.mDisplayAdjustments;
    }

    public String getName() {
        String str;
        synchronized (this) {
            updateDisplayInfoLocked();
            str = this.mDisplayInfo.name;
        }
        return str;
    }

    public void getSize(Point point) {
        synchronized (this) {
            updateDisplayInfoLocked();
            this.mDisplayInfo.getAppMetrics(this.mTempMetrics, getDisplayAdjustments());
            point.x = this.mTempMetrics.widthPixels;
            point.y = this.mTempMetrics.heightPixels;
        }
    }

    public void getRectSize(Rect rect) {
        synchronized (this) {
            updateDisplayInfoLocked();
            this.mDisplayInfo.getAppMetrics(this.mTempMetrics, getDisplayAdjustments());
            rect.set(0, 0, this.mTempMetrics.widthPixels, this.mTempMetrics.heightPixels);
        }
    }

    public void getCurrentSizeRange(Point point, Point point2) {
        synchronized (this) {
            updateDisplayInfoLocked();
            point.x = this.mDisplayInfo.smallestNominalAppWidth;
            point.y = this.mDisplayInfo.smallestNominalAppHeight;
            point2.x = this.mDisplayInfo.largestNominalAppWidth;
            point2.y = this.mDisplayInfo.largestNominalAppHeight;
        }
    }

    public int getMaximumSizeDimension() {
        int iMax;
        synchronized (this) {
            updateDisplayInfoLocked();
            iMax = Math.max(this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
        }
        return iMax;
    }

    @Deprecated
    public int getWidth() {
        int i;
        synchronized (this) {
            updateCachedAppSizeIfNeededLocked();
            i = this.mCachedAppWidthCompat;
        }
        return i;
    }

    @Deprecated
    public int getHeight() {
        int i;
        synchronized (this) {
            updateCachedAppSizeIfNeededLocked();
            i = this.mCachedAppHeightCompat;
        }
        return i;
    }

    public void getOverscanInsets(Rect rect) {
        synchronized (this) {
            updateDisplayInfoLocked();
            rect.set(this.mDisplayInfo.overscanLeft, this.mDisplayInfo.overscanTop, this.mDisplayInfo.overscanRight, this.mDisplayInfo.overscanBottom);
        }
    }

    public int getRotation() {
        int i;
        synchronized (this) {
            updateDisplayInfoLocked();
            i = this.mDisplayInfo.rotation;
        }
        return i;
    }

    @Deprecated
    public int getOrientation() {
        return getRotation();
    }

    @Deprecated
    public int getPixelFormat() {
        return 1;
    }

    public float getRefreshRate() {
        float refreshRate;
        synchronized (this) {
            updateDisplayInfoLocked();
            refreshRate = this.mDisplayInfo.getMode().getRefreshRate();
        }
        return refreshRate;
    }

    @Deprecated
    public float[] getSupportedRefreshRates() {
        float[] defaultRefreshRates;
        synchronized (this) {
            updateDisplayInfoLocked();
            defaultRefreshRates = this.mDisplayInfo.getDefaultRefreshRates();
        }
        return defaultRefreshRates;
    }

    public Mode getMode() {
        Mode mode;
        synchronized (this) {
            updateDisplayInfoLocked();
            mode = this.mDisplayInfo.getMode();
        }
        return mode;
    }

    public Mode[] getSupportedModes() {
        Mode[] modeArr;
        synchronized (this) {
            updateDisplayInfoLocked();
            Mode[] modeArr2 = this.mDisplayInfo.supportedModes;
            modeArr = (Mode[]) Arrays.copyOf(modeArr2, modeArr2.length);
        }
        return modeArr;
    }

    public void requestColorMode(int i) {
        this.mGlobal.requestColorMode(this.mDisplayId, i);
    }

    public int getColorMode() {
        int i;
        synchronized (this) {
            updateDisplayInfoLocked();
            i = this.mDisplayInfo.colorMode;
        }
        return i;
    }

    public int getRemoveMode() {
        return this.mDisplayInfo.removeMode;
    }

    public HdrCapabilities getHdrCapabilities() {
        HdrCapabilities hdrCapabilities;
        synchronized (this) {
            updateDisplayInfoLocked();
            hdrCapabilities = this.mDisplayInfo.hdrCapabilities;
        }
        return hdrCapabilities;
    }

    public boolean isHdr() {
        boolean zIsHdr;
        synchronized (this) {
            updateDisplayInfoLocked();
            zIsHdr = this.mDisplayInfo.isHdr();
        }
        return zIsHdr;
    }

    public boolean isWideColorGamut() {
        boolean zIsWideColorGamut;
        synchronized (this) {
            updateDisplayInfoLocked();
            zIsWideColorGamut = this.mDisplayInfo.isWideColorGamut();
        }
        return zIsWideColorGamut;
    }

    public int[] getSupportedColorModes() {
        int[] iArrCopyOf;
        synchronized (this) {
            updateDisplayInfoLocked();
            int[] iArr = this.mDisplayInfo.supportedColorModes;
            iArrCopyOf = Arrays.copyOf(iArr, iArr.length);
        }
        return iArrCopyOf;
    }

    public long getAppVsyncOffsetNanos() {
        long j;
        synchronized (this) {
            updateDisplayInfoLocked();
            j = this.mDisplayInfo.appVsyncOffsetNanos;
        }
        return j;
    }

    public long getPresentationDeadlineNanos() {
        long j;
        synchronized (this) {
            updateDisplayInfoLocked();
            j = this.mDisplayInfo.presentationDeadlineNanos;
        }
        return j;
    }

    public void getMetrics(DisplayMetrics displayMetrics) {
        synchronized (this) {
            updateDisplayInfoLocked();
            this.mDisplayInfo.getAppMetrics(displayMetrics, getDisplayAdjustments());
        }
    }

    public void getRealSize(Point point) {
        synchronized (this) {
            updateDisplayInfoLocked();
            point.x = this.mDisplayInfo.logicalWidth;
            point.y = this.mDisplayInfo.logicalHeight;
        }
    }

    public void getRealMetrics(DisplayMetrics displayMetrics) {
        synchronized (this) {
            updateDisplayInfoLocked();
            this.mDisplayInfo.getLogicalMetrics(displayMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, null);
        }
    }

    public int getState() {
        int i;
        synchronized (this) {
            updateDisplayInfoLocked();
            i = this.mIsValid ? this.mDisplayInfo.state : 0;
        }
        return i;
    }

    public boolean hasAccess(int i) {
        return hasAccess(i, this.mFlags, this.mOwnerUid);
    }

    public static boolean hasAccess(int i, int i2, int i3) {
        return (i2 & 4) == 0 || i == i3 || i == 1000 || i == 0;
    }

    public boolean isPublicPresentation() {
        return (this.mFlags & 12) == 8;
    }

    private void updateDisplayInfoLocked() {
        DisplayInfo displayInfo = this.mGlobal.getDisplayInfo(this.mDisplayId);
        if (displayInfo == null) {
            if (this.mIsValid) {
                this.mIsValid = false;
            }
        } else {
            this.mDisplayInfo = displayInfo;
            if (!this.mIsValid) {
                this.mIsValid = true;
            }
        }
    }

    private void updateCachedAppSizeIfNeededLocked() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (jUptimeMillis > this.mLastCachedAppSizeUpdate + 20) {
            updateDisplayInfoLocked();
            this.mDisplayInfo.getAppMetrics(this.mTempMetrics, getDisplayAdjustments());
            this.mCachedAppWidthCompat = this.mTempMetrics.widthPixels;
            this.mCachedAppHeightCompat = this.mTempMetrics.heightPixels;
            this.mLastCachedAppSizeUpdate = jUptimeMillis;
        }
    }

    public String toString() {
        String str;
        synchronized (this) {
            updateDisplayInfoLocked();
            this.mDisplayInfo.getAppMetrics(this.mTempMetrics, getDisplayAdjustments());
            str = "Display id " + this.mDisplayId + ": " + this.mDisplayInfo + ", " + this.mTempMetrics + ", isValid=" + this.mIsValid;
        }
        return str;
    }

    public static String typeToString(int i) {
        switch (i) {
            case 0:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            case 1:
                return "BUILT_IN";
            case 2:
                return "HDMI";
            case 3:
                return "WIFI";
            case 4:
                return "OVERLAY";
            case 5:
                return "VIRTUAL";
            default:
                return Integer.toString(i);
        }
    }

    public static String stateToString(int i) {
        switch (i) {
            case 0:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            case 1:
                return "OFF";
            case 2:
                return "ON";
            case 3:
                return "DOZE";
            case 4:
                return "DOZE_SUSPEND";
            case 5:
                return "VR";
            case 6:
                return "ON_SUSPEND";
            default:
                return Integer.toString(i);
        }
    }

    public static boolean isSuspendedState(int i) {
        return i == 1 || i == 4 || i == 6;
    }

    public static boolean isDozeState(int i) {
        return i == 3 || i == 4;
    }

    public static final class Mode implements Parcelable {
        private final int mHeight;
        private final int mModeId;
        private final float mRefreshRate;
        private final int mWidth;
        public static final Mode[] EMPTY_ARRAY = new Mode[0];
        public static final Parcelable.Creator<Mode> CREATOR = new Parcelable.Creator<Mode>() {
            @Override
            public Mode createFromParcel(Parcel parcel) {
                return new Mode(parcel);
            }

            @Override
            public Mode[] newArray(int i) {
                return new Mode[i];
            }
        };

        public Mode(int i, int i2, int i3, float f) {
            this.mModeId = i;
            this.mWidth = i2;
            this.mHeight = i3;
            this.mRefreshRate = f;
        }

        public int getModeId() {
            return this.mModeId;
        }

        public int getPhysicalWidth() {
            return this.mWidth;
        }

        public int getPhysicalHeight() {
            return this.mHeight;
        }

        public float getRefreshRate() {
            return this.mRefreshRate;
        }

        public boolean matches(int i, int i2, float f) {
            return this.mWidth == i && this.mHeight == i2 && Float.floatToIntBits(this.mRefreshRate) == Float.floatToIntBits(f);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Mode)) {
                return false;
            }
            Mode mode = (Mode) obj;
            return this.mModeId == mode.mModeId && matches(mode.mWidth, mode.mHeight, mode.mRefreshRate);
        }

        public int hashCode() {
            return ((((((this.mModeId + 17) * 17) + this.mWidth) * 17) + this.mHeight) * 17) + Float.floatToIntBits(this.mRefreshRate);
        }

        public String toString() {
            return "{id=" + this.mModeId + ", width=" + this.mWidth + ", height=" + this.mHeight + ", fps=" + this.mRefreshRate + "}";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        private Mode(Parcel parcel) {
            this(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readFloat());
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mModeId);
            parcel.writeInt(this.mWidth);
            parcel.writeInt(this.mHeight);
            parcel.writeFloat(this.mRefreshRate);
        }
    }

    public static final class HdrCapabilities implements Parcelable {
        public static final Parcelable.Creator<HdrCapabilities> CREATOR = new Parcelable.Creator<HdrCapabilities>() {
            @Override
            public HdrCapabilities createFromParcel(Parcel parcel) {
                return new HdrCapabilities(parcel);
            }

            @Override
            public HdrCapabilities[] newArray(int i) {
                return new HdrCapabilities[i];
            }
        };
        public static final int HDR_TYPE_DOLBY_VISION = 1;
        public static final int HDR_TYPE_HDR10 = 2;
        public static final int HDR_TYPE_HLG = 3;
        public static final float INVALID_LUMINANCE = -1.0f;
        private float mMaxAverageLuminance;
        private float mMaxLuminance;
        private float mMinLuminance;
        private int[] mSupportedHdrTypes;

        @Retention(RetentionPolicy.SOURCE)
        public @interface HdrType {
        }

        public HdrCapabilities() {
            this.mSupportedHdrTypes = new int[0];
            this.mMaxLuminance = -1.0f;
            this.mMaxAverageLuminance = -1.0f;
            this.mMinLuminance = -1.0f;
        }

        public HdrCapabilities(int[] iArr, float f, float f2, float f3) {
            this.mSupportedHdrTypes = new int[0];
            this.mMaxLuminance = -1.0f;
            this.mMaxAverageLuminance = -1.0f;
            this.mMinLuminance = -1.0f;
            this.mSupportedHdrTypes = iArr;
            this.mMaxLuminance = f;
            this.mMaxAverageLuminance = f2;
            this.mMinLuminance = f3;
        }

        public int[] getSupportedHdrTypes() {
            return this.mSupportedHdrTypes;
        }

        public float getDesiredMaxLuminance() {
            return this.mMaxLuminance;
        }

        public float getDesiredMaxAverageLuminance() {
            return this.mMaxAverageLuminance;
        }

        public float getDesiredMinLuminance() {
            return this.mMinLuminance;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof HdrCapabilities)) {
                return false;
            }
            HdrCapabilities hdrCapabilities = (HdrCapabilities) obj;
            return Arrays.equals(this.mSupportedHdrTypes, hdrCapabilities.mSupportedHdrTypes) && this.mMaxLuminance == hdrCapabilities.mMaxLuminance && this.mMaxAverageLuminance == hdrCapabilities.mMaxAverageLuminance && this.mMinLuminance == hdrCapabilities.mMinLuminance;
        }

        public int hashCode() {
            return ((((((MetricsProto.MetricsEvent.ACTION_WINDOW_DOCK_UNRESIZABLE + Arrays.hashCode(this.mSupportedHdrTypes)) * 17) + Float.floatToIntBits(this.mMaxLuminance)) * 17) + Float.floatToIntBits(this.mMaxAverageLuminance)) * 17) + Float.floatToIntBits(this.mMinLuminance);
        }

        private HdrCapabilities(Parcel parcel) {
            this.mSupportedHdrTypes = new int[0];
            this.mMaxLuminance = -1.0f;
            this.mMaxAverageLuminance = -1.0f;
            this.mMinLuminance = -1.0f;
            readFromParcel(parcel);
        }

        public void readFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            this.mSupportedHdrTypes = new int[i];
            for (int i2 = 0; i2 < i; i2++) {
                this.mSupportedHdrTypes[i2] = parcel.readInt();
            }
            this.mMaxLuminance = parcel.readFloat();
            this.mMaxAverageLuminance = parcel.readFloat();
            this.mMinLuminance = parcel.readFloat();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mSupportedHdrTypes.length);
            for (int i2 = 0; i2 < this.mSupportedHdrTypes.length; i2++) {
                parcel.writeInt(this.mSupportedHdrTypes[i2]);
            }
            parcel.writeFloat(this.mMaxLuminance);
            parcel.writeFloat(this.mMaxAverageLuminance);
            parcel.writeFloat(this.mMinLuminance);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
