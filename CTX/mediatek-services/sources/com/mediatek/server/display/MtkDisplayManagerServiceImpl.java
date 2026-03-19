package com.mediatek.server.display;

import android.os.SystemProperties;
import android.util.Slog;
import android.view.DisplayInfo;

public class MtkDisplayManagerServiceImpl extends MtkDisplayManagerService {
    private static final boolean DEBUG = false;
    private static final String TAG = "MtkDisplayManagerServiceImpl";
    private boolean mSupportFullscreenSwitch = "1".equals(SystemProperties.get("ro.vendor.fullscreen_switch"));
    private DisplayInfo mDisplayInfo = new DisplayInfo();

    public void setDisplayInfoForFullscreenSwitch(DisplayInfo displayInfo) {
        if (this.mSupportFullscreenSwitch) {
            if (displayInfo != null && displayInfo.fullscreenCropInfo.width != 0 && displayInfo.fullscreenCropInfo.height != 0) {
                if (this.mDisplayInfo == null) {
                    this.mDisplayInfo = new DisplayInfo();
                }
                this.mDisplayInfo.fullscreenCropInfo.width = displayInfo.fullscreenCropInfo.width;
                this.mDisplayInfo.fullscreenCropInfo.height = displayInfo.fullscreenCropInfo.height;
                Slog.d(TAG, "fullscreen switch, crop display:" + this.mDisplayInfo.fullscreenCropInfo.width + ", " + this.mDisplayInfo.fullscreenCropInfo.height + " mDisplayInfo " + this.mDisplayInfo);
                return;
            }
            this.mDisplayInfo = null;
        }
    }

    public DisplayInfo getDisplayInfoForFullscreenSwitch(DisplayInfo displayInfo, int i) {
        int iMin;
        int iMax;
        if (this.mSupportFullscreenSwitch && i != 1000 && this.mDisplayInfo != null && this.mDisplayInfo.fullscreenCropInfo.width != 0 && this.mDisplayInfo.fullscreenCropInfo.height != 0) {
            DisplayInfo displayInfo2 = new DisplayInfo();
            displayInfo2.copyFrom(displayInfo);
            boolean z = displayInfo.logicalWidth > displayInfo.logicalHeight ? true : DEBUG;
            if (z) {
                iMin = Math.max(this.mDisplayInfo.fullscreenCropInfo.width, this.mDisplayInfo.fullscreenCropInfo.height);
            } else {
                iMin = Math.min(this.mDisplayInfo.fullscreenCropInfo.width, this.mDisplayInfo.fullscreenCropInfo.height);
            }
            displayInfo2.logicalWidth = iMin;
            if (z) {
                iMax = Math.min(this.mDisplayInfo.fullscreenCropInfo.width, this.mDisplayInfo.fullscreenCropInfo.height);
            } else {
                iMax = Math.max(this.mDisplayInfo.fullscreenCropInfo.width, this.mDisplayInfo.fullscreenCropInfo.height);
            }
            displayInfo2.logicalHeight = iMax;
            return displayInfo2;
        }
        return displayInfo;
    }
}
