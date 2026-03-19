package com.android.server.display;

import android.view.Display;
import android.view.DisplayCutout;
import com.android.server.vr.Vr2dDisplay;
import java.util.Arrays;
import java.util.Objects;

final class DisplayDeviceInfo {
    public static final int DIFF_COLOR_MODE = 4;
    public static final int DIFF_OTHER = 2;
    public static final int DIFF_STATE = 1;
    public static final int FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 512;
    public static final int FLAG_DEFAULT_DISPLAY = 1;
    public static final int FLAG_DESTROY_CONTENT_ON_REMOVAL = 1024;
    public static final int FLAG_NEVER_BLANK = 32;
    public static final int FLAG_OWN_CONTENT_ONLY = 128;
    public static final int FLAG_PRESENTATION = 64;
    public static final int FLAG_PRIVATE = 16;
    public static final int FLAG_ROTATES_WITH_CONTENT = 2;
    public static final int FLAG_ROUND = 256;
    public static final int FLAG_SECURE = 4;
    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 8;
    public static final int TOUCH_EXTERNAL = 2;
    public static final int TOUCH_INTERNAL = 1;
    public static final int TOUCH_NONE = 0;
    public static final int TOUCH_VIRTUAL = 3;
    public String address;
    public long appVsyncOffsetNanos;
    public int colorMode;
    public int defaultModeId;
    public int densityDpi;
    public DisplayCutout displayCutout;
    public int flags;
    public Display.HdrCapabilities hdrCapabilities;
    public int height;
    public int modeId;
    public String name;
    public String ownerPackageName;
    public int ownerUid;
    public long presentationDeadlineNanos;
    public int touch;
    public int type;
    public String uniqueId;
    public int width;
    public float xDpi;
    public float yDpi;
    public Display.Mode[] supportedModes = Display.Mode.EMPTY_ARRAY;
    public int[] supportedColorModes = {0};
    public int rotation = 0;
    public int state = 2;

    DisplayDeviceInfo() {
    }

    public void setAssumedDensityForExternalDisplay(int i, int i2) {
        this.densityDpi = (Math.min(i, i2) * Vr2dDisplay.DEFAULT_VIRTUAL_DISPLAY_DPI) / 1080;
        this.xDpi = this.densityDpi;
        this.yDpi = this.densityDpi;
    }

    public boolean equals(Object obj) {
        return (obj instanceof DisplayDeviceInfo) && equals((DisplayDeviceInfo) obj);
    }

    public boolean equals(DisplayDeviceInfo displayDeviceInfo) {
        return displayDeviceInfo != null && diff(displayDeviceInfo) == 0;
    }

    public int diff(DisplayDeviceInfo displayDeviceInfo) {
        int i;
        if (this.state != displayDeviceInfo.state) {
            i = 1;
        } else {
            i = 0;
        }
        if (this.colorMode != displayDeviceInfo.colorMode) {
            i |= 4;
        }
        if (!Objects.equals(this.name, displayDeviceInfo.name) || !Objects.equals(this.uniqueId, displayDeviceInfo.uniqueId) || this.width != displayDeviceInfo.width || this.height != displayDeviceInfo.height || this.modeId != displayDeviceInfo.modeId || this.defaultModeId != displayDeviceInfo.defaultModeId || !Arrays.equals(this.supportedModes, displayDeviceInfo.supportedModes) || !Arrays.equals(this.supportedColorModes, displayDeviceInfo.supportedColorModes) || !Objects.equals(this.hdrCapabilities, displayDeviceInfo.hdrCapabilities) || this.densityDpi != displayDeviceInfo.densityDpi || this.xDpi != displayDeviceInfo.xDpi || this.yDpi != displayDeviceInfo.yDpi || this.appVsyncOffsetNanos != displayDeviceInfo.appVsyncOffsetNanos || this.presentationDeadlineNanos != displayDeviceInfo.presentationDeadlineNanos || this.flags != displayDeviceInfo.flags || !Objects.equals(this.displayCutout, displayDeviceInfo.displayCutout) || this.touch != displayDeviceInfo.touch || this.rotation != displayDeviceInfo.rotation || this.type != displayDeviceInfo.type || !Objects.equals(this.address, displayDeviceInfo.address) || this.ownerUid != displayDeviceInfo.ownerUid || !Objects.equals(this.ownerPackageName, displayDeviceInfo.ownerPackageName)) {
            return i | 2;
        }
        return i;
    }

    public int hashCode() {
        return 0;
    }

    public void copyFrom(DisplayDeviceInfo displayDeviceInfo) {
        this.name = displayDeviceInfo.name;
        this.uniqueId = displayDeviceInfo.uniqueId;
        this.width = displayDeviceInfo.width;
        this.height = displayDeviceInfo.height;
        this.modeId = displayDeviceInfo.modeId;
        this.defaultModeId = displayDeviceInfo.defaultModeId;
        this.supportedModes = displayDeviceInfo.supportedModes;
        this.colorMode = displayDeviceInfo.colorMode;
        this.supportedColorModes = displayDeviceInfo.supportedColorModes;
        this.hdrCapabilities = displayDeviceInfo.hdrCapabilities;
        this.densityDpi = displayDeviceInfo.densityDpi;
        this.xDpi = displayDeviceInfo.xDpi;
        this.yDpi = displayDeviceInfo.yDpi;
        this.appVsyncOffsetNanos = displayDeviceInfo.appVsyncOffsetNanos;
        this.presentationDeadlineNanos = displayDeviceInfo.presentationDeadlineNanos;
        this.flags = displayDeviceInfo.flags;
        this.displayCutout = displayDeviceInfo.displayCutout;
        this.touch = displayDeviceInfo.touch;
        this.rotation = displayDeviceInfo.rotation;
        this.type = displayDeviceInfo.type;
        this.address = displayDeviceInfo.address;
        this.state = displayDeviceInfo.state;
        this.ownerUid = displayDeviceInfo.ownerUid;
        this.ownerPackageName = displayDeviceInfo.ownerPackageName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DisplayDeviceInfo{\"");
        sb.append(this.name);
        sb.append("\": uniqueId=\"");
        sb.append(this.uniqueId);
        sb.append("\", ");
        sb.append(this.width);
        sb.append(" x ");
        sb.append(this.height);
        sb.append(", modeId ");
        sb.append(this.modeId);
        sb.append(", defaultModeId ");
        sb.append(this.defaultModeId);
        sb.append(", supportedModes ");
        sb.append(Arrays.toString(this.supportedModes));
        sb.append(", colorMode ");
        sb.append(this.colorMode);
        sb.append(", supportedColorModes ");
        sb.append(Arrays.toString(this.supportedColorModes));
        sb.append(", HdrCapabilities ");
        sb.append(this.hdrCapabilities);
        sb.append(", density ");
        sb.append(this.densityDpi);
        sb.append(", ");
        sb.append(this.xDpi);
        sb.append(" x ");
        sb.append(this.yDpi);
        sb.append(" dpi");
        sb.append(", appVsyncOff ");
        sb.append(this.appVsyncOffsetNanos);
        sb.append(", presDeadline ");
        sb.append(this.presentationDeadlineNanos);
        if (this.displayCutout != null) {
            sb.append(", cutout ");
            sb.append(this.displayCutout);
        }
        sb.append(", touch ");
        sb.append(touchToString(this.touch));
        sb.append(", rotation ");
        sb.append(this.rotation);
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
        sb.append("}");
        return sb.toString();
    }

    private static String touchToString(int i) {
        switch (i) {
            case 0:
                return "NONE";
            case 1:
                return "INTERNAL";
            case 2:
                return "EXTERNAL";
            case 3:
                return "VIRTUAL";
            default:
                return Integer.toString(i);
        }
    }

    private static String flagsToString(int i) {
        StringBuilder sb = new StringBuilder();
        if ((i & 1) != 0) {
            sb.append(", FLAG_DEFAULT_DISPLAY");
        }
        if ((i & 2) != 0) {
            sb.append(", FLAG_ROTATES_WITH_CONTENT");
        }
        if ((i & 4) != 0) {
            sb.append(", FLAG_SECURE");
        }
        if ((i & 8) != 0) {
            sb.append(", FLAG_SUPPORTS_PROTECTED_BUFFERS");
        }
        if ((i & 16) != 0) {
            sb.append(", FLAG_PRIVATE");
        }
        if ((i & 32) != 0) {
            sb.append(", FLAG_NEVER_BLANK");
        }
        if ((i & 64) != 0) {
            sb.append(", FLAG_PRESENTATION");
        }
        if ((i & 128) != 0) {
            sb.append(", FLAG_OWN_CONTENT_ONLY");
        }
        if ((i & 256) != 0) {
            sb.append(", FLAG_ROUND");
        }
        if ((i & 512) != 0) {
            sb.append(", FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD");
        }
        return sb.toString();
    }
}
