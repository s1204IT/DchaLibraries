package com.android.server.display;

import android.graphics.Rect;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class LogicalDisplay {
    private static final int BLANK_LAYER_STACK = -1;
    private final int mDisplayId;
    private int mDisplayOffsetX;
    private int mDisplayOffsetY;
    private boolean mHasContent;
    private DisplayInfo mInfo;
    private final int mLayerStack;
    private DisplayInfo mOverrideDisplayInfo;
    private DisplayDevice mPrimaryDisplayDevice;
    private DisplayDeviceInfo mPrimaryDisplayDeviceInfo;
    private int mRequestedColorMode;
    private int mRequestedModeId;
    private final DisplayInfo mBaseDisplayInfo = new DisplayInfo();
    private final Rect mTempLayerStackRect = new Rect();
    private final Rect mTempDisplayRect = new Rect();

    public LogicalDisplay(int i, int i2, DisplayDevice displayDevice) {
        this.mDisplayId = i;
        this.mLayerStack = i2;
        this.mPrimaryDisplayDevice = displayDevice;
    }

    public int getDisplayIdLocked() {
        return this.mDisplayId;
    }

    public DisplayDevice getPrimaryDisplayDeviceLocked() {
        return this.mPrimaryDisplayDevice;
    }

    public DisplayInfo getDisplayInfoLocked() {
        if (this.mInfo == null) {
            this.mInfo = new DisplayInfo();
            this.mInfo.copyFrom(this.mBaseDisplayInfo);
            if (this.mOverrideDisplayInfo != null) {
                this.mInfo.appWidth = this.mOverrideDisplayInfo.appWidth;
                this.mInfo.appHeight = this.mOverrideDisplayInfo.appHeight;
                this.mInfo.smallestNominalAppWidth = this.mOverrideDisplayInfo.smallestNominalAppWidth;
                this.mInfo.smallestNominalAppHeight = this.mOverrideDisplayInfo.smallestNominalAppHeight;
                this.mInfo.largestNominalAppWidth = this.mOverrideDisplayInfo.largestNominalAppWidth;
                this.mInfo.largestNominalAppHeight = this.mOverrideDisplayInfo.largestNominalAppHeight;
                this.mInfo.logicalWidth = this.mOverrideDisplayInfo.logicalWidth;
                this.mInfo.logicalHeight = this.mOverrideDisplayInfo.logicalHeight;
                this.mInfo.overscanLeft = this.mOverrideDisplayInfo.overscanLeft;
                this.mInfo.overscanTop = this.mOverrideDisplayInfo.overscanTop;
                this.mInfo.overscanRight = this.mOverrideDisplayInfo.overscanRight;
                this.mInfo.overscanBottom = this.mOverrideDisplayInfo.overscanBottom;
                this.mInfo.rotation = this.mOverrideDisplayInfo.rotation;
                this.mInfo.displayCutout = this.mOverrideDisplayInfo.displayCutout;
                this.mInfo.logicalDensityDpi = this.mOverrideDisplayInfo.logicalDensityDpi;
                this.mInfo.physicalXDpi = this.mOverrideDisplayInfo.physicalXDpi;
                this.mInfo.physicalYDpi = this.mOverrideDisplayInfo.physicalYDpi;
            }
        }
        return this.mInfo;
    }

    void getNonOverrideDisplayInfoLocked(DisplayInfo displayInfo) {
        displayInfo.copyFrom(this.mBaseDisplayInfo);
    }

    public boolean setDisplayInfoOverrideFromWindowManagerLocked(DisplayInfo displayInfo) {
        if (displayInfo != null) {
            if (this.mOverrideDisplayInfo == null) {
                this.mOverrideDisplayInfo = new DisplayInfo(displayInfo);
                this.mInfo = null;
                return true;
            }
            if (!this.mOverrideDisplayInfo.equals(displayInfo)) {
                this.mOverrideDisplayInfo.copyFrom(displayInfo);
                this.mInfo = null;
                return true;
            }
            return false;
        }
        if (this.mOverrideDisplayInfo != null) {
            this.mOverrideDisplayInfo = null;
            this.mInfo = null;
            return true;
        }
        return false;
    }

    public boolean isValidLocked() {
        return this.mPrimaryDisplayDevice != null;
    }

    public void updateLocked(List<DisplayDevice> list) {
        if (this.mPrimaryDisplayDevice == null) {
            return;
        }
        if (!list.contains(this.mPrimaryDisplayDevice)) {
            this.mPrimaryDisplayDevice = null;
            return;
        }
        DisplayDeviceInfo displayDeviceInfoLocked = this.mPrimaryDisplayDevice.getDisplayDeviceInfoLocked();
        if (!Objects.equals(this.mPrimaryDisplayDeviceInfo, displayDeviceInfoLocked)) {
            this.mBaseDisplayInfo.layerStack = this.mLayerStack;
            this.mBaseDisplayInfo.flags = 0;
            if ((displayDeviceInfoLocked.flags & 8) != 0) {
                this.mBaseDisplayInfo.flags |= 1;
            }
            if ((displayDeviceInfoLocked.flags & 4) != 0) {
                this.mBaseDisplayInfo.flags |= 2;
            }
            if ((displayDeviceInfoLocked.flags & 16) != 0) {
                this.mBaseDisplayInfo.flags |= 4;
                this.mBaseDisplayInfo.removeMode = 1;
            }
            if ((displayDeviceInfoLocked.flags & 1024) != 0) {
                this.mBaseDisplayInfo.removeMode = 1;
            }
            if ((displayDeviceInfoLocked.flags & 64) != 0) {
                this.mBaseDisplayInfo.flags |= 8;
            }
            if ((displayDeviceInfoLocked.flags & 256) != 0) {
                this.mBaseDisplayInfo.flags |= 16;
            }
            if ((displayDeviceInfoLocked.flags & 512) != 0) {
                this.mBaseDisplayInfo.flags |= 32;
            }
            this.mBaseDisplayInfo.type = displayDeviceInfoLocked.type;
            this.mBaseDisplayInfo.address = displayDeviceInfoLocked.address;
            this.mBaseDisplayInfo.name = displayDeviceInfoLocked.name;
            this.mBaseDisplayInfo.uniqueId = displayDeviceInfoLocked.uniqueId;
            this.mBaseDisplayInfo.appWidth = displayDeviceInfoLocked.width;
            this.mBaseDisplayInfo.appHeight = displayDeviceInfoLocked.height;
            this.mBaseDisplayInfo.logicalWidth = displayDeviceInfoLocked.width;
            this.mBaseDisplayInfo.logicalHeight = displayDeviceInfoLocked.height;
            this.mBaseDisplayInfo.rotation = 0;
            this.mBaseDisplayInfo.modeId = displayDeviceInfoLocked.modeId;
            this.mBaseDisplayInfo.defaultModeId = displayDeviceInfoLocked.defaultModeId;
            this.mBaseDisplayInfo.supportedModes = (Display.Mode[]) Arrays.copyOf(displayDeviceInfoLocked.supportedModes, displayDeviceInfoLocked.supportedModes.length);
            this.mBaseDisplayInfo.colorMode = displayDeviceInfoLocked.colorMode;
            this.mBaseDisplayInfo.supportedColorModes = Arrays.copyOf(displayDeviceInfoLocked.supportedColorModes, displayDeviceInfoLocked.supportedColorModes.length);
            this.mBaseDisplayInfo.hdrCapabilities = displayDeviceInfoLocked.hdrCapabilities;
            this.mBaseDisplayInfo.logicalDensityDpi = displayDeviceInfoLocked.densityDpi;
            this.mBaseDisplayInfo.physicalXDpi = displayDeviceInfoLocked.xDpi;
            this.mBaseDisplayInfo.physicalYDpi = displayDeviceInfoLocked.yDpi;
            this.mBaseDisplayInfo.appVsyncOffsetNanos = displayDeviceInfoLocked.appVsyncOffsetNanos;
            this.mBaseDisplayInfo.presentationDeadlineNanos = displayDeviceInfoLocked.presentationDeadlineNanos;
            this.mBaseDisplayInfo.state = displayDeviceInfoLocked.state;
            this.mBaseDisplayInfo.smallestNominalAppWidth = displayDeviceInfoLocked.width;
            this.mBaseDisplayInfo.smallestNominalAppHeight = displayDeviceInfoLocked.height;
            this.mBaseDisplayInfo.largestNominalAppWidth = displayDeviceInfoLocked.width;
            this.mBaseDisplayInfo.largestNominalAppHeight = displayDeviceInfoLocked.height;
            this.mBaseDisplayInfo.ownerUid = displayDeviceInfoLocked.ownerUid;
            this.mBaseDisplayInfo.ownerPackageName = displayDeviceInfoLocked.ownerPackageName;
            this.mBaseDisplayInfo.displayCutout = displayDeviceInfoLocked.displayCutout;
            this.mPrimaryDisplayDeviceInfo = displayDeviceInfoLocked;
            this.mInfo = null;
        }
    }

    public void configureDisplayLocked(SurfaceControl.Transaction transaction, DisplayDevice displayDevice, boolean z) {
        int i;
        int i2;
        int i3;
        displayDevice.setLayerStackLocked(transaction, z ? -1 : this.mLayerStack);
        if (displayDevice == this.mPrimaryDisplayDevice) {
            displayDevice.requestDisplayModesLocked(this.mRequestedColorMode, this.mRequestedModeId);
        } else {
            displayDevice.requestDisplayModesLocked(0, 0);
        }
        DisplayInfo displayInfoLocked = getDisplayInfoLocked();
        DisplayDeviceInfo displayDeviceInfoLocked = displayDevice.getDisplayDeviceInfoLocked();
        this.mTempLayerStackRect.set(0, 0, displayInfoLocked.logicalWidth, displayInfoLocked.logicalHeight);
        if ((displayDeviceInfoLocked.flags & 2) != 0) {
            i = displayInfoLocked.rotation;
        } else {
            i = 0;
        }
        int i4 = (i + displayDeviceInfoLocked.rotation) % 4;
        boolean z2 = i4 == 1 || i4 == 3;
        int i5 = z2 ? displayDeviceInfoLocked.height : displayDeviceInfoLocked.width;
        int i6 = z2 ? displayDeviceInfoLocked.width : displayDeviceInfoLocked.height;
        if ((displayInfoLocked.flags & 1073741824) != 0) {
            i2 = displayInfoLocked.logicalWidth;
            i3 = displayInfoLocked.logicalHeight;
        } else if (displayInfoLocked.logicalHeight * i5 < displayInfoLocked.logicalWidth * i6) {
            i3 = (displayInfoLocked.logicalHeight * i5) / displayInfoLocked.logicalWidth;
            i2 = i5;
        } else {
            i2 = (displayInfoLocked.logicalWidth * i6) / displayInfoLocked.logicalHeight;
            i3 = i6;
        }
        int i7 = (i6 - i3) / 2;
        int i8 = (i5 - i2) / 2;
        this.mTempDisplayRect.set(i8, i7, i2 + i8, i3 + i7);
        this.mTempDisplayRect.left += this.mDisplayOffsetX;
        this.mTempDisplayRect.right += this.mDisplayOffsetX;
        this.mTempDisplayRect.top += this.mDisplayOffsetY;
        this.mTempDisplayRect.bottom += this.mDisplayOffsetY;
        displayDevice.setProjectionLocked(transaction, i4, this.mTempLayerStackRect, this.mTempDisplayRect);
    }

    public boolean hasContentLocked() {
        return this.mHasContent;
    }

    public void setHasContentLocked(boolean z) {
        this.mHasContent = z;
    }

    public void setRequestedModeIdLocked(int i) {
        this.mRequestedModeId = i;
    }

    public int getRequestedModeIdLocked() {
        return this.mRequestedModeId;
    }

    public void setRequestedColorModeLocked(int i) {
        this.mRequestedColorMode = i;
    }

    public int getRequestedColorModeLocked() {
        return this.mRequestedColorMode;
    }

    public int getDisplayOffsetXLocked() {
        return this.mDisplayOffsetX;
    }

    public int getDisplayOffsetYLocked() {
        return this.mDisplayOffsetY;
    }

    public void setDisplayOffsetsLocked(int i, int i2) {
        this.mDisplayOffsetX = i;
        this.mDisplayOffsetY = i2;
    }

    public void dumpLocked(PrintWriter printWriter) {
        printWriter.println("mDisplayId=" + this.mDisplayId);
        printWriter.println("mLayerStack=" + this.mLayerStack);
        printWriter.println("mHasContent=" + this.mHasContent);
        printWriter.println("mRequestedMode=" + this.mRequestedModeId);
        printWriter.println("mRequestedColorMode=" + this.mRequestedColorMode);
        printWriter.println("mDisplayOffset=(" + this.mDisplayOffsetX + ", " + this.mDisplayOffsetY + ")");
        StringBuilder sb = new StringBuilder();
        sb.append("mPrimaryDisplayDevice=");
        sb.append(this.mPrimaryDisplayDevice != null ? this.mPrimaryDisplayDevice.getNameLocked() : "null");
        printWriter.println(sb.toString());
        printWriter.println("mBaseDisplayInfo=" + this.mBaseDisplayInfo);
        printWriter.println("mOverrideDisplayInfo=" + this.mOverrideDisplayInfo);
    }
}
