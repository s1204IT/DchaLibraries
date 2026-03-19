package android.view;

import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayCutout;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public final class DisplayInfo implements Parcelable {
    public static final Parcelable.Creator<DisplayInfo> CREATOR = new Parcelable.Creator<DisplayInfo>() {
        @Override
        public DisplayInfo createFromParcel(Parcel parcel) {
            return new DisplayInfo(parcel);
        }

        @Override
        public DisplayInfo[] newArray(int i) {
            return new DisplayInfo[i];
        }
    };
    public String address;
    public int appHeight;
    public long appVsyncOffsetNanos;
    public int appWidth;
    public int colorMode;
    public int defaultModeId;
    public DisplayCutout displayCutout;
    public int flags;
    public FullscreenCropInfo fullscreenCropInfo;
    public Display.HdrCapabilities hdrCapabilities;
    public int largestNominalAppHeight;
    public int largestNominalAppWidth;
    public int layerStack;
    public int logicalDensityDpi;
    public int logicalHeight;
    public int logicalWidth;
    public int modeId;
    public String name;
    public int overscanBottom;
    public int overscanLeft;
    public int overscanRight;
    public int overscanTop;
    public String ownerPackageName;
    public int ownerUid;
    public float physicalXDpi;
    public float physicalYDpi;
    public long presentationDeadlineNanos;
    public int removeMode;
    public int rotation;
    public int smallestNominalAppHeight;
    public int smallestNominalAppWidth;
    public int state;
    public int[] supportedColorModes;
    public Display.Mode[] supportedModes;
    public int type;
    public String uniqueId;

    public static class FullscreenCropInfo {
        public int width = 0;
        public int height = 0;
    }

    public DisplayInfo() {
        this.supportedModes = Display.Mode.EMPTY_ARRAY;
        this.supportedColorModes = new int[]{0};
        this.removeMode = 0;
        this.fullscreenCropInfo = new FullscreenCropInfo();
    }

    public DisplayInfo(DisplayInfo displayInfo) {
        this.supportedModes = Display.Mode.EMPTY_ARRAY;
        this.supportedColorModes = new int[]{0};
        this.removeMode = 0;
        this.fullscreenCropInfo = new FullscreenCropInfo();
        copyFrom(displayInfo);
    }

    private DisplayInfo(Parcel parcel) {
        this.supportedModes = Display.Mode.EMPTY_ARRAY;
        this.supportedColorModes = new int[]{0};
        this.removeMode = 0;
        this.fullscreenCropInfo = new FullscreenCropInfo();
        readFromParcel(parcel);
    }

    public boolean equals(Object obj) {
        return (obj instanceof DisplayInfo) && equals((DisplayInfo) obj);
    }

    public boolean equals(DisplayInfo displayInfo) {
        return displayInfo != null && this.layerStack == displayInfo.layerStack && this.flags == displayInfo.flags && this.type == displayInfo.type && Objects.equals(this.address, displayInfo.address) && Objects.equals(this.uniqueId, displayInfo.uniqueId) && this.appWidth == displayInfo.appWidth && this.appHeight == displayInfo.appHeight && this.smallestNominalAppWidth == displayInfo.smallestNominalAppWidth && this.smallestNominalAppHeight == displayInfo.smallestNominalAppHeight && this.largestNominalAppWidth == displayInfo.largestNominalAppWidth && this.largestNominalAppHeight == displayInfo.largestNominalAppHeight && this.logicalWidth == displayInfo.logicalWidth && this.logicalHeight == displayInfo.logicalHeight && this.overscanLeft == displayInfo.overscanLeft && this.overscanTop == displayInfo.overscanTop && this.overscanRight == displayInfo.overscanRight && this.overscanBottom == displayInfo.overscanBottom && Objects.equals(this.displayCutout, displayInfo.displayCutout) && this.rotation == displayInfo.rotation && this.modeId == displayInfo.modeId && this.defaultModeId == displayInfo.defaultModeId && this.colorMode == displayInfo.colorMode && Arrays.equals(this.supportedColorModes, displayInfo.supportedColorModes) && Objects.equals(this.hdrCapabilities, displayInfo.hdrCapabilities) && this.logicalDensityDpi == displayInfo.logicalDensityDpi && this.physicalXDpi == displayInfo.physicalXDpi && this.physicalYDpi == displayInfo.physicalYDpi && this.appVsyncOffsetNanos == displayInfo.appVsyncOffsetNanos && this.presentationDeadlineNanos == displayInfo.presentationDeadlineNanos && this.state == displayInfo.state && this.ownerUid == displayInfo.ownerUid && Objects.equals(this.ownerPackageName, displayInfo.ownerPackageName) && this.removeMode == displayInfo.removeMode;
    }

    public int hashCode() {
        return 0;
    }

    public void copyFrom(DisplayInfo displayInfo) {
        this.layerStack = displayInfo.layerStack;
        this.flags = displayInfo.flags;
        this.type = displayInfo.type;
        this.address = displayInfo.address;
        this.name = displayInfo.name;
        this.uniqueId = displayInfo.uniqueId;
        this.appWidth = displayInfo.appWidth;
        this.appHeight = displayInfo.appHeight;
        this.smallestNominalAppWidth = displayInfo.smallestNominalAppWidth;
        this.smallestNominalAppHeight = displayInfo.smallestNominalAppHeight;
        this.largestNominalAppWidth = displayInfo.largestNominalAppWidth;
        this.largestNominalAppHeight = displayInfo.largestNominalAppHeight;
        this.logicalWidth = displayInfo.logicalWidth;
        this.logicalHeight = displayInfo.logicalHeight;
        this.overscanLeft = displayInfo.overscanLeft;
        this.overscanTop = displayInfo.overscanTop;
        this.overscanRight = displayInfo.overscanRight;
        this.overscanBottom = displayInfo.overscanBottom;
        this.displayCutout = displayInfo.displayCutout;
        this.rotation = displayInfo.rotation;
        this.modeId = displayInfo.modeId;
        this.defaultModeId = displayInfo.defaultModeId;
        this.supportedModes = (Display.Mode[]) Arrays.copyOf(displayInfo.supportedModes, displayInfo.supportedModes.length);
        this.colorMode = displayInfo.colorMode;
        this.supportedColorModes = Arrays.copyOf(displayInfo.supportedColorModes, displayInfo.supportedColorModes.length);
        this.hdrCapabilities = displayInfo.hdrCapabilities;
        this.logicalDensityDpi = displayInfo.logicalDensityDpi;
        this.physicalXDpi = displayInfo.physicalXDpi;
        this.physicalYDpi = displayInfo.physicalYDpi;
        this.appVsyncOffsetNanos = displayInfo.appVsyncOffsetNanos;
        this.presentationDeadlineNanos = displayInfo.presentationDeadlineNanos;
        this.state = displayInfo.state;
        this.ownerUid = displayInfo.ownerUid;
        this.ownerPackageName = displayInfo.ownerPackageName;
        this.removeMode = displayInfo.removeMode;
    }

    public void readFromParcel(Parcel parcel) {
        this.layerStack = parcel.readInt();
        this.flags = parcel.readInt();
        this.type = parcel.readInt();
        this.address = parcel.readString();
        this.name = parcel.readString();
        this.appWidth = parcel.readInt();
        this.appHeight = parcel.readInt();
        this.smallestNominalAppWidth = parcel.readInt();
        this.smallestNominalAppHeight = parcel.readInt();
        this.largestNominalAppWidth = parcel.readInt();
        this.largestNominalAppHeight = parcel.readInt();
        this.logicalWidth = parcel.readInt();
        this.logicalHeight = parcel.readInt();
        this.overscanLeft = parcel.readInt();
        this.overscanTop = parcel.readInt();
        this.overscanRight = parcel.readInt();
        this.overscanBottom = parcel.readInt();
        this.displayCutout = DisplayCutout.ParcelableWrapper.readCutoutFromParcel(parcel);
        this.rotation = parcel.readInt();
        this.modeId = parcel.readInt();
        this.defaultModeId = parcel.readInt();
        int i = parcel.readInt();
        this.supportedModes = new Display.Mode[i];
        for (int i2 = 0; i2 < i; i2++) {
            this.supportedModes[i2] = Display.Mode.CREATOR.createFromParcel(parcel);
        }
        this.colorMode = parcel.readInt();
        int i3 = parcel.readInt();
        this.supportedColorModes = new int[i3];
        for (int i4 = 0; i4 < i3; i4++) {
            this.supportedColorModes[i4] = parcel.readInt();
        }
        this.hdrCapabilities = (Display.HdrCapabilities) parcel.readParcelable(null);
        this.logicalDensityDpi = parcel.readInt();
        this.physicalXDpi = parcel.readFloat();
        this.physicalYDpi = parcel.readFloat();
        this.appVsyncOffsetNanos = parcel.readLong();
        this.presentationDeadlineNanos = parcel.readLong();
        this.state = parcel.readInt();
        this.ownerUid = parcel.readInt();
        this.ownerPackageName = parcel.readString();
        this.uniqueId = parcel.readString();
        this.removeMode = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.layerStack);
        parcel.writeInt(this.flags);
        parcel.writeInt(this.type);
        parcel.writeString(this.address);
        parcel.writeString(this.name);
        parcel.writeInt(this.appWidth);
        parcel.writeInt(this.appHeight);
        parcel.writeInt(this.smallestNominalAppWidth);
        parcel.writeInt(this.smallestNominalAppHeight);
        parcel.writeInt(this.largestNominalAppWidth);
        parcel.writeInt(this.largestNominalAppHeight);
        parcel.writeInt(this.logicalWidth);
        parcel.writeInt(this.logicalHeight);
        parcel.writeInt(this.overscanLeft);
        parcel.writeInt(this.overscanTop);
        parcel.writeInt(this.overscanRight);
        parcel.writeInt(this.overscanBottom);
        DisplayCutout.ParcelableWrapper.writeCutoutToParcel(this.displayCutout, parcel, i);
        parcel.writeInt(this.rotation);
        parcel.writeInt(this.modeId);
        parcel.writeInt(this.defaultModeId);
        parcel.writeInt(this.supportedModes.length);
        for (int i2 = 0; i2 < this.supportedModes.length; i2++) {
            this.supportedModes[i2].writeToParcel(parcel, i);
        }
        parcel.writeInt(this.colorMode);
        parcel.writeInt(this.supportedColorModes.length);
        for (int i3 = 0; i3 < this.supportedColorModes.length; i3++) {
            parcel.writeInt(this.supportedColorModes[i3]);
        }
        parcel.writeParcelable(this.hdrCapabilities, i);
        parcel.writeInt(this.logicalDensityDpi);
        parcel.writeFloat(this.physicalXDpi);
        parcel.writeFloat(this.physicalYDpi);
        parcel.writeLong(this.appVsyncOffsetNanos);
        parcel.writeLong(this.presentationDeadlineNanos);
        parcel.writeInt(this.state);
        parcel.writeInt(this.ownerUid);
        parcel.writeString(this.ownerPackageName);
        parcel.writeString(this.uniqueId);
        parcel.writeInt(this.removeMode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Display.Mode getMode() {
        return findMode(this.modeId);
    }

    public Display.Mode getDefaultMode() {
        return findMode(this.defaultModeId);
    }

    private Display.Mode findMode(int i) {
        for (int i2 = 0; i2 < this.supportedModes.length; i2++) {
            if (this.supportedModes[i2].getModeId() == i) {
                return this.supportedModes[i2];
            }
        }
        throw new IllegalStateException("Unable to locate mode " + i);
    }

    public int findDefaultModeByRefreshRate(float f) {
        Display.Mode[] modeArr = this.supportedModes;
        Display.Mode defaultMode = getDefaultMode();
        for (int i = 0; i < modeArr.length; i++) {
            if (modeArr[i].matches(defaultMode.getPhysicalWidth(), defaultMode.getPhysicalHeight(), f)) {
                return modeArr[i].getModeId();
            }
        }
        return 0;
    }

    public float[] getDefaultRefreshRates() {
        Display.Mode[] modeArr = this.supportedModes;
        ArraySet arraySet = new ArraySet();
        Display.Mode defaultMode = getDefaultMode();
        int i = 0;
        for (Display.Mode mode : modeArr) {
            if (mode.getPhysicalWidth() == defaultMode.getPhysicalWidth() && mode.getPhysicalHeight() == defaultMode.getPhysicalHeight()) {
                arraySet.add(Float.valueOf(mode.getRefreshRate()));
            }
        }
        float[] fArr = new float[arraySet.size()];
        Iterator it = arraySet.iterator();
        while (it.hasNext()) {
            fArr[i] = ((Float) it.next()).floatValue();
            i++;
        }
        return fArr;
    }

    public void getAppMetrics(DisplayMetrics displayMetrics) {
        getAppMetrics(displayMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, null);
    }

    public void getAppMetrics(DisplayMetrics displayMetrics, DisplayAdjustments displayAdjustments) {
        getMetricsWithSize(displayMetrics, displayAdjustments.getCompatibilityInfo(), displayAdjustments.getConfiguration(), this.appWidth, this.appHeight);
    }

    public void getAppMetrics(DisplayMetrics displayMetrics, CompatibilityInfo compatibilityInfo, Configuration configuration) {
        getMetricsWithSize(displayMetrics, compatibilityInfo, configuration, this.appWidth, this.appHeight);
    }

    public void getLogicalMetrics(DisplayMetrics displayMetrics, CompatibilityInfo compatibilityInfo, Configuration configuration) {
        getMetricsWithSize(displayMetrics, compatibilityInfo, configuration, this.logicalWidth, this.logicalHeight);
    }

    public int getNaturalWidth() {
        return (this.rotation == 0 || this.rotation == 2) ? this.logicalWidth : this.logicalHeight;
    }

    public int getNaturalHeight() {
        return (this.rotation == 0 || this.rotation == 2) ? this.logicalHeight : this.logicalWidth;
    }

    public boolean isHdr() {
        int[] supportedHdrTypes = this.hdrCapabilities != null ? this.hdrCapabilities.getSupportedHdrTypes() : null;
        return supportedHdrTypes != null && supportedHdrTypes.length > 0;
    }

    public boolean isWideColorGamut() {
        for (int i : this.supportedColorModes) {
            if (i == 6 || i > 7) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAccess(int i) {
        return Display.hasAccess(i, this.flags, this.ownerUid);
    }

    private void getMetricsWithSize(DisplayMetrics displayMetrics, CompatibilityInfo compatibilityInfo, Configuration configuration, int i, int i2) {
        int i3 = this.logicalDensityDpi;
        displayMetrics.noncompatDensityDpi = i3;
        displayMetrics.densityDpi = i3;
        float f = this.logicalDensityDpi * 0.00625f;
        displayMetrics.noncompatDensity = f;
        displayMetrics.density = f;
        float f2 = displayMetrics.density;
        displayMetrics.noncompatScaledDensity = f2;
        displayMetrics.scaledDensity = f2;
        float f3 = this.physicalXDpi;
        displayMetrics.noncompatXdpi = f3;
        displayMetrics.xdpi = f3;
        float f4 = this.physicalYDpi;
        displayMetrics.noncompatYdpi = f4;
        displayMetrics.ydpi = f4;
        Rect appBounds = configuration != null ? configuration.windowConfiguration.getAppBounds() : null;
        if (appBounds != null) {
            i = appBounds.width();
        }
        if (appBounds != null) {
            i2 = appBounds.height();
        }
        displayMetrics.widthPixels = i;
        displayMetrics.noncompatWidthPixels = i;
        displayMetrics.heightPixels = i2;
        displayMetrics.noncompatHeightPixels = i2;
        if (!compatibilityInfo.equals(CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO)) {
            compatibilityInfo.applyToDisplayMetrics(displayMetrics);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DisplayInfo{\"");
        sb.append(this.name);
        sb.append("\", uniqueId \"");
        sb.append(this.uniqueId);
        sb.append("\", app ");
        sb.append(this.appWidth);
        sb.append(" x ");
        sb.append(this.appHeight);
        sb.append(", real ");
        sb.append(this.logicalWidth);
        sb.append(" x ");
        sb.append(this.logicalHeight);
        if (this.overscanLeft != 0 || this.overscanTop != 0 || this.overscanRight != 0 || this.overscanBottom != 0) {
            sb.append(", overscan (");
            sb.append(this.overscanLeft);
            sb.append(",");
            sb.append(this.overscanTop);
            sb.append(",");
            sb.append(this.overscanRight);
            sb.append(",");
            sb.append(this.overscanBottom);
            sb.append(")");
        }
        sb.append(", largest app ");
        sb.append(this.largestNominalAppWidth);
        sb.append(" x ");
        sb.append(this.largestNominalAppHeight);
        sb.append(", smallest app ");
        sb.append(this.smallestNominalAppWidth);
        sb.append(" x ");
        sb.append(this.smallestNominalAppHeight);
        sb.append(", mode ");
        sb.append(this.modeId);
        sb.append(", defaultMode ");
        sb.append(this.defaultModeId);
        sb.append(", modes ");
        sb.append(Arrays.toString(this.supportedModes));
        sb.append(", colorMode ");
        sb.append(this.colorMode);
        sb.append(", supportedColorModes ");
        sb.append(Arrays.toString(this.supportedColorModes));
        sb.append(", hdrCapabilities ");
        sb.append(this.hdrCapabilities);
        sb.append(", rotation ");
        sb.append(this.rotation);
        sb.append(", density ");
        sb.append(this.logicalDensityDpi);
        sb.append(" (");
        sb.append(this.physicalXDpi);
        sb.append(" x ");
        sb.append(this.physicalYDpi);
        sb.append(") dpi, layerStack ");
        sb.append(this.layerStack);
        sb.append(", appVsyncOff ");
        sb.append(this.appVsyncOffsetNanos);
        sb.append(", presDeadline ");
        sb.append(this.presentationDeadlineNanos);
        sb.append(", type ");
        sb.append(Display.typeToString(this.type));
        if (this.address != null) {
            sb.append(", address ");
            sb.append(this.address);
        }
        sb.append(", state ");
        sb.append(Display.stateToString(this.state));
        if (this.ownerUid != 0 || this.ownerPackageName != null) {
            sb.append(", owner ");
            sb.append(this.ownerPackageName);
            sb.append(" (uid ");
            sb.append(this.ownerUid);
            sb.append(")");
        }
        sb.append(flagsToString(this.flags));
        sb.append(", removeMode ");
        sb.append(this.removeMode);
        sb.append("}");
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.logicalWidth);
        protoOutputStream.write(1120986464258L, this.logicalHeight);
        protoOutputStream.write(1120986464259L, this.appWidth);
        protoOutputStream.write(1120986464260L, this.appHeight);
        protoOutputStream.write(1138166333445L, this.name);
        protoOutputStream.end(jStart);
    }

    private static String flagsToString(int i) {
        StringBuilder sb = new StringBuilder();
        if ((i & 2) != 0) {
            sb.append(", FLAG_SECURE");
        }
        if ((i & 1) != 0) {
            sb.append(", FLAG_SUPPORTS_PROTECTED_BUFFERS");
        }
        if ((i & 4) != 0) {
            sb.append(", FLAG_PRIVATE");
        }
        if ((i & 8) != 0) {
            sb.append(", FLAG_PRESENTATION");
        }
        if ((1073741824 & i) != 0) {
            sb.append(", FLAG_SCALING_DISABLED");
        }
        if ((i & 16) != 0) {
            sb.append(", FLAG_ROUND");
        }
        return sb.toString();
    }
}
