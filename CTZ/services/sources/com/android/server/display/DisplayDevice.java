package com.android.server.display;

import android.graphics.Rect;
import android.hardware.display.DisplayViewport;
import android.os.IBinder;
import android.view.Surface;
import android.view.SurfaceControl;
import java.io.PrintWriter;

abstract class DisplayDevice {
    private Rect mCurrentDisplayRect;
    private Rect mCurrentLayerStackRect;
    private Surface mCurrentSurface;
    DisplayDeviceInfo mDebugLastLoggedDeviceInfo;
    private final DisplayAdapter mDisplayAdapter;
    private final IBinder mDisplayToken;
    private final String mUniqueId;
    private int mCurrentLayerStack = -1;
    private int mCurrentOrientation = -1;

    public abstract DisplayDeviceInfo getDisplayDeviceInfoLocked();

    public abstract boolean hasStableUniqueId();

    public DisplayDevice(DisplayAdapter displayAdapter, IBinder iBinder, String str) {
        this.mDisplayAdapter = displayAdapter;
        this.mDisplayToken = iBinder;
        this.mUniqueId = str;
    }

    public final DisplayAdapter getAdapterLocked() {
        return this.mDisplayAdapter;
    }

    public final IBinder getDisplayTokenLocked() {
        return this.mDisplayToken;
    }

    public final String getNameLocked() {
        return getDisplayDeviceInfoLocked().name;
    }

    public final String getUniqueId() {
        return this.mUniqueId;
    }

    public void applyPendingDisplayDeviceInfoChangesLocked() {
    }

    public void performTraversalLocked(SurfaceControl.Transaction transaction) {
    }

    public Runnable requestDisplayStateLocked(int i, int i2) {
        return null;
    }

    public void requestDisplayModesLocked(int i, int i2) {
    }

    public void onOverlayChangedLocked() {
    }

    public final void setLayerStackLocked(SurfaceControl.Transaction transaction, int i) {
        if (this.mCurrentLayerStack != i) {
            this.mCurrentLayerStack = i;
            transaction.setDisplayLayerStack(this.mDisplayToken, i);
        }
    }

    public final void setProjectionLocked(SurfaceControl.Transaction transaction, int i, Rect rect, Rect rect2) {
        if (this.mCurrentOrientation != i || this.mCurrentLayerStackRect == null || !this.mCurrentLayerStackRect.equals(rect) || this.mCurrentDisplayRect == null || !this.mCurrentDisplayRect.equals(rect2)) {
            this.mCurrentOrientation = i;
            if (this.mCurrentLayerStackRect == null) {
                this.mCurrentLayerStackRect = new Rect();
            }
            this.mCurrentLayerStackRect.set(rect);
            if (this.mCurrentDisplayRect == null) {
                this.mCurrentDisplayRect = new Rect();
            }
            this.mCurrentDisplayRect.set(rect2);
            transaction.setDisplayProjection(this.mDisplayToken, i, rect, rect2);
        }
    }

    public final void setSurfaceLocked(SurfaceControl.Transaction transaction, Surface surface) {
        if (this.mCurrentSurface != surface) {
            this.mCurrentSurface = surface;
            transaction.setDisplaySurface(this.mDisplayToken, surface);
        }
    }

    public final void populateViewportLocked(DisplayViewport displayViewport) {
        displayViewport.orientation = this.mCurrentOrientation;
        if (this.mCurrentLayerStackRect != null) {
            displayViewport.logicalFrame.set(this.mCurrentLayerStackRect);
        } else {
            displayViewport.logicalFrame.setEmpty();
        }
        if (this.mCurrentDisplayRect != null) {
            displayViewport.physicalFrame.set(this.mCurrentDisplayRect);
        } else {
            displayViewport.physicalFrame.setEmpty();
        }
        boolean z = true;
        if (this.mCurrentOrientation != 1 && this.mCurrentOrientation != 3) {
            z = false;
        }
        DisplayDeviceInfo displayDeviceInfoLocked = getDisplayDeviceInfoLocked();
        displayViewport.deviceWidth = z ? displayDeviceInfoLocked.height : displayDeviceInfoLocked.width;
        displayViewport.deviceHeight = z ? displayDeviceInfoLocked.width : displayDeviceInfoLocked.height;
    }

    public void dumpLocked(PrintWriter printWriter) {
        printWriter.println("mAdapter=" + this.mDisplayAdapter.getName());
        printWriter.println("mUniqueId=" + this.mUniqueId);
        printWriter.println("mDisplayToken=" + this.mDisplayToken);
        printWriter.println("mCurrentLayerStack=" + this.mCurrentLayerStack);
        printWriter.println("mCurrentOrientation=" + this.mCurrentOrientation);
        printWriter.println("mCurrentLayerStackRect=" + this.mCurrentLayerStackRect);
        printWriter.println("mCurrentDisplayRect=" + this.mCurrentDisplayRect);
        printWriter.println("mCurrentSurface=" + this.mCurrentSurface);
    }
}
