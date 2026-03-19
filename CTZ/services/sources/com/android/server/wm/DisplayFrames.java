package com.android.server.wm;

import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import com.android.server.wm.utils.WmDisplayCutout;
import java.io.PrintWriter;

public class DisplayFrames {
    public int mDisplayHeight;
    public final int mDisplayId;
    public int mDisplayWidth;
    public int mRotation;
    public final Rect mOverscan = new Rect();
    public final Rect mUnrestricted = new Rect();
    public final Rect mRestrictedOverscan = new Rect();
    public final Rect mRestricted = new Rect();
    public final Rect mSystem = new Rect();
    public final Rect mStable = new Rect();
    public final Rect mStableFullscreen = new Rect();
    public final Rect mCurrent = new Rect();
    public final Rect mContent = new Rect();
    public final Rect mVoiceContent = new Rect();
    public final Rect mDock = new Rect();
    public WmDisplayCutout mDisplayCutout = WmDisplayCutout.NO_CUTOUT;
    public WmDisplayCutout mDisplayInfoCutout = WmDisplayCutout.NO_CUTOUT;
    public final Rect mDisplayCutoutSafe = new Rect();
    private final Rect mDisplayInfoOverscan = new Rect();
    private final Rect mRotatedDisplayInfoOverscan = new Rect();

    public DisplayFrames(int i, DisplayInfo displayInfo, WmDisplayCutout wmDisplayCutout) {
        this.mDisplayId = i;
        onDisplayInfoUpdated(displayInfo, wmDisplayCutout);
    }

    public void onDisplayInfoUpdated(DisplayInfo displayInfo, WmDisplayCutout wmDisplayCutout) {
        this.mDisplayWidth = displayInfo.logicalWidth;
        this.mDisplayHeight = displayInfo.logicalHeight;
        this.mRotation = displayInfo.rotation;
        this.mDisplayInfoOverscan.set(displayInfo.overscanLeft, displayInfo.overscanTop, displayInfo.overscanRight, displayInfo.overscanBottom);
        if (wmDisplayCutout == null) {
            wmDisplayCutout = WmDisplayCutout.NO_CUTOUT;
        }
        this.mDisplayInfoCutout = wmDisplayCutout;
    }

    public void onBeginLayout() {
        switch (this.mRotation) {
            case 1:
                this.mRotatedDisplayInfoOverscan.left = this.mDisplayInfoOverscan.top;
                this.mRotatedDisplayInfoOverscan.top = this.mDisplayInfoOverscan.right;
                this.mRotatedDisplayInfoOverscan.right = this.mDisplayInfoOverscan.bottom;
                this.mRotatedDisplayInfoOverscan.bottom = this.mDisplayInfoOverscan.left;
                break;
            case 2:
                this.mRotatedDisplayInfoOverscan.left = this.mDisplayInfoOverscan.right;
                this.mRotatedDisplayInfoOverscan.top = this.mDisplayInfoOverscan.bottom;
                this.mRotatedDisplayInfoOverscan.right = this.mDisplayInfoOverscan.left;
                this.mRotatedDisplayInfoOverscan.bottom = this.mDisplayInfoOverscan.top;
                break;
            case 3:
                this.mRotatedDisplayInfoOverscan.left = this.mDisplayInfoOverscan.bottom;
                this.mRotatedDisplayInfoOverscan.top = this.mDisplayInfoOverscan.left;
                this.mRotatedDisplayInfoOverscan.right = this.mDisplayInfoOverscan.top;
                this.mRotatedDisplayInfoOverscan.bottom = this.mDisplayInfoOverscan.right;
                break;
            default:
                this.mRotatedDisplayInfoOverscan.set(this.mDisplayInfoOverscan);
                break;
        }
        this.mRestrictedOverscan.set(0, 0, this.mDisplayWidth, this.mDisplayHeight);
        this.mOverscan.set(this.mRestrictedOverscan);
        this.mSystem.set(this.mRestrictedOverscan);
        this.mUnrestricted.set(this.mRotatedDisplayInfoOverscan);
        this.mUnrestricted.right = this.mDisplayWidth - this.mUnrestricted.right;
        this.mUnrestricted.bottom = this.mDisplayHeight - this.mUnrestricted.bottom;
        this.mRestricted.set(this.mUnrestricted);
        this.mDock.set(this.mUnrestricted);
        this.mContent.set(this.mUnrestricted);
        this.mVoiceContent.set(this.mUnrestricted);
        this.mStable.set(this.mUnrestricted);
        this.mStableFullscreen.set(this.mUnrestricted);
        this.mCurrent.set(this.mUnrestricted);
        this.mDisplayCutout = this.mDisplayInfoCutout;
        this.mDisplayCutoutSafe.set(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        if (!this.mDisplayCutout.getDisplayCutout().isEmpty()) {
            DisplayCutout displayCutout = this.mDisplayCutout.getDisplayCutout();
            if (displayCutout.getSafeInsetLeft() > 0) {
                this.mDisplayCutoutSafe.left = this.mRestrictedOverscan.left + displayCutout.getSafeInsetLeft();
            }
            if (displayCutout.getSafeInsetTop() > 0) {
                this.mDisplayCutoutSafe.top = this.mRestrictedOverscan.top + displayCutout.getSafeInsetTop();
            }
            if (displayCutout.getSafeInsetRight() > 0) {
                this.mDisplayCutoutSafe.right = this.mRestrictedOverscan.right - displayCutout.getSafeInsetRight();
            }
            if (displayCutout.getSafeInsetBottom() > 0) {
                this.mDisplayCutoutSafe.bottom = this.mRestrictedOverscan.bottom - displayCutout.getSafeInsetBottom();
            }
        }
    }

    public int getInputMethodWindowVisibleHeight() {
        return this.mDock.bottom - this.mCurrent.bottom;
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        this.mStable.writeToProto(protoOutputStream, 1146756268033L);
        protoOutputStream.end(jStart);
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.println(str + "DisplayFrames w=" + this.mDisplayWidth + " h=" + this.mDisplayHeight + " r=" + this.mRotation);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("  ");
        String string = sb.toString();
        dumpFrame(this.mStable, "mStable", string, printWriter);
        dumpFrame(this.mStableFullscreen, "mStableFullscreen", string, printWriter);
        dumpFrame(this.mDock, "mDock", string, printWriter);
        dumpFrame(this.mCurrent, "mCurrent", string, printWriter);
        dumpFrame(this.mSystem, "mSystem", string, printWriter);
        dumpFrame(this.mContent, "mContent", string, printWriter);
        dumpFrame(this.mVoiceContent, "mVoiceContent", string, printWriter);
        dumpFrame(this.mOverscan, "mOverscan", string, printWriter);
        dumpFrame(this.mRestrictedOverscan, "mRestrictedOverscan", string, printWriter);
        dumpFrame(this.mRestricted, "mRestricted", string, printWriter);
        dumpFrame(this.mUnrestricted, "mUnrestricted", string, printWriter);
        dumpFrame(this.mDisplayInfoOverscan, "mDisplayInfoOverscan", string, printWriter);
        dumpFrame(this.mRotatedDisplayInfoOverscan, "mRotatedDisplayInfoOverscan", string, printWriter);
        printWriter.println(string + "mDisplayCutout=" + this.mDisplayCutout);
    }

    private void dumpFrame(Rect rect, String str, String str2, PrintWriter printWriter) {
        printWriter.print(str2 + str + "=");
        rect.printShortString(printWriter);
        printWriter.println();
    }
}
