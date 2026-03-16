package com.android.server.display;

import android.view.Display;
import java.util.Arrays;
import libcore.util.EmptyArray;
import libcore.util.Objects;

final class DisplayDeviceInfo {
    public static final int FLAG_DEFAULT_DISPLAY = 1;
    public static final int FLAG_NEVER_BLANK = 32;
    public static final int FLAG_OWN_CONTENT_ONLY = 128;
    public static final int FLAG_PRESENTATION = 64;
    public static final int FLAG_PRIVATE = 16;
    public static final int FLAG_ROTATES_WITH_CONTENT = 2;
    public static final int FLAG_SECURE = 4;
    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 8;
    public static final int TOUCH_EXTERNAL = 2;
    public static final int TOUCH_INTERNAL = 1;
    public static final int TOUCH_NONE = 0;
    public String address;
    public long appVsyncOffsetNanos;
    public int densityDpi;
    public int flags;
    public int height;
    public String name;
    public String ownerPackageName;
    public int ownerUid;
    public long presentationDeadlineNanos;
    public float refreshRate;
    public int touch;
    public int type;
    public String uniqueId;
    public int width;
    public float xDpi;
    public float yDpi;
    public float[] supportedRefreshRates = EmptyArray.FLOAT;
    public int rotation = 0;
    public int state = 2;

    DisplayDeviceInfo() {
    }

    public void setAssumedDensityForExternalDisplay(int width, int height) {
        this.densityDpi = (Math.min(width, height) * 320) / 1080;
        this.xDpi = this.densityDpi;
        this.yDpi = this.densityDpi;
    }

    public boolean equals(Object o) {
        return (o instanceof DisplayDeviceInfo) && equals((DisplayDeviceInfo) o);
    }

    public boolean equals(DisplayDeviceInfo other) {
        return other != null && Objects.equal(this.name, other.name) && Objects.equal(this.uniqueId, other.uniqueId) && this.width == other.width && this.height == other.height && this.refreshRate == other.refreshRate && Arrays.equals(this.supportedRefreshRates, other.supportedRefreshRates) && this.densityDpi == other.densityDpi && this.xDpi == other.xDpi && this.yDpi == other.yDpi && this.appVsyncOffsetNanos == other.appVsyncOffsetNanos && this.presentationDeadlineNanos == other.presentationDeadlineNanos && this.flags == other.flags && this.touch == other.touch && this.rotation == other.rotation && this.type == other.type && Objects.equal(this.address, other.address) && this.state == other.state && this.ownerUid == other.ownerUid && Objects.equal(this.ownerPackageName, other.ownerPackageName);
    }

    public int hashCode() {
        return 0;
    }

    public void copyFrom(DisplayDeviceInfo other) {
        this.name = other.name;
        this.uniqueId = other.uniqueId;
        this.width = other.width;
        this.height = other.height;
        this.refreshRate = other.refreshRate;
        this.supportedRefreshRates = other.supportedRefreshRates;
        this.densityDpi = other.densityDpi;
        this.xDpi = other.xDpi;
        this.yDpi = other.yDpi;
        this.appVsyncOffsetNanos = other.appVsyncOffsetNanos;
        this.presentationDeadlineNanos = other.presentationDeadlineNanos;
        this.flags = other.flags;
        this.touch = other.touch;
        this.rotation = other.rotation;
        this.type = other.type;
        this.address = other.address;
        this.state = other.state;
        this.ownerUid = other.ownerUid;
        this.ownerPackageName = other.ownerPackageName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DisplayDeviceInfo{\"");
        sb.append(this.name).append("\": uniqueId=\"").append(this.uniqueId).append("\", ");
        sb.append(this.width).append(" x ").append(this.height);
        sb.append(", ").append(this.refreshRate).append(" fps");
        sb.append(", supportedRefreshRates ").append(Arrays.toString(this.supportedRefreshRates));
        sb.append(", density ").append(this.densityDpi);
        sb.append(", ").append(this.xDpi).append(" x ").append(this.yDpi).append(" dpi");
        sb.append(", appVsyncOff ").append(this.appVsyncOffsetNanos);
        sb.append(", presDeadline ").append(this.presentationDeadlineNanos);
        sb.append(", touch ").append(touchToString(this.touch));
        sb.append(", rotation ").append(this.rotation);
        sb.append(", type ").append(Display.typeToString(this.type));
        if (this.address != null) {
            sb.append(", address ").append(this.address);
        }
        sb.append(", state ").append(Display.stateToString(this.state));
        if (this.ownerUid != 0 || this.ownerPackageName != null) {
            sb.append(", owner ").append(this.ownerPackageName);
            sb.append(" (uid ").append(this.ownerUid).append(")");
        }
        sb.append(flagsToString(this.flags));
        sb.append("}");
        return sb.toString();
    }

    private static String touchToString(int touch) {
        switch (touch) {
            case 0:
                return "NONE";
            case 1:
                return "INTERNAL";
            case 2:
                return "EXTERNAL";
            default:
                return Integer.toString(touch);
        }
    }

    private static String flagsToString(int flags) {
        StringBuilder msg = new StringBuilder();
        if ((flags & 1) != 0) {
            msg.append(", FLAG_DEFAULT_DISPLAY");
        }
        if ((flags & 2) != 0) {
            msg.append(", FLAG_ROTATES_WITH_CONTENT");
        }
        if ((flags & 4) != 0) {
            msg.append(", FLAG_SECURE");
        }
        if ((flags & 8) != 0) {
            msg.append(", FLAG_SUPPORTS_PROTECTED_BUFFERS");
        }
        if ((flags & 16) != 0) {
            msg.append(", FLAG_PRIVATE");
        }
        if ((flags & 32) != 0) {
            msg.append(", FLAG_NEVER_BLANK");
        }
        if ((flags & 64) != 0) {
            msg.append(", FLAG_PRESENTATION");
        }
        if ((flags & 128) != 0) {
            msg.append(", FLAG_OWN_CONTENT_ONLY");
        }
        return msg.toString();
    }
}
