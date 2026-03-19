package android.hardware.display;

import android.graphics.Rect;
import android.text.TextUtils;

public final class DisplayViewport {
    public int deviceHeight;
    public int deviceWidth;
    public int displayId;
    public int orientation;
    public String uniqueId;
    public boolean valid;
    public final Rect logicalFrame = new Rect();
    public final Rect physicalFrame = new Rect();

    public void copyFrom(DisplayViewport displayViewport) {
        this.valid = displayViewport.valid;
        this.displayId = displayViewport.displayId;
        this.orientation = displayViewport.orientation;
        this.logicalFrame.set(displayViewport.logicalFrame);
        this.physicalFrame.set(displayViewport.physicalFrame);
        this.deviceWidth = displayViewport.deviceWidth;
        this.deviceHeight = displayViewport.deviceHeight;
        this.uniqueId = displayViewport.uniqueId;
    }

    public DisplayViewport makeCopy() {
        DisplayViewport displayViewport = new DisplayViewport();
        displayViewport.copyFrom(this);
        return displayViewport;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DisplayViewport)) {
            return false;
        }
        DisplayViewport displayViewport = (DisplayViewport) obj;
        return this.valid == displayViewport.valid && this.displayId == displayViewport.displayId && this.orientation == displayViewport.orientation && this.logicalFrame.equals(displayViewport.logicalFrame) && this.physicalFrame.equals(displayViewport.physicalFrame) && this.deviceWidth == displayViewport.deviceWidth && this.deviceHeight == displayViewport.deviceHeight && TextUtils.equals(this.uniqueId, displayViewport.uniqueId);
    }

    public int hashCode() {
        int i = 1 + (this.valid ? 1 : 0) + 31;
        int i2 = i + (31 * i) + this.displayId;
        int i3 = i2 + (31 * i2) + this.orientation;
        int iHashCode = i3 + (31 * i3) + this.logicalFrame.hashCode();
        int iHashCode2 = iHashCode + (31 * iHashCode) + this.physicalFrame.hashCode();
        int i4 = iHashCode2 + (31 * iHashCode2) + this.deviceWidth;
        int i5 = i4 + (31 * i4) + this.deviceHeight;
        return i5 + (31 * i5) + this.uniqueId.hashCode();
    }

    public String toString() {
        return "DisplayViewport{valid=" + this.valid + ", displayId=" + this.displayId + ", uniqueId='" + this.uniqueId + "', orientation=" + this.orientation + ", logicalFrame=" + this.logicalFrame + ", physicalFrame=" + this.physicalFrame + ", deviceWidth=" + this.deviceWidth + ", deviceHeight=" + this.deviceHeight + "}";
    }
}
