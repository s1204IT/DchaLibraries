package com.android.server.wm;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.animation.Animation;

class WallpaperWindowToken extends WindowToken {
    private static final String TAG = "WindowManager";

    WallpaperWindowToken(WindowManagerService windowManagerService, IBinder iBinder, boolean z, DisplayContent displayContent, boolean z2) {
        super(windowManagerService, iBinder, 2013, z, displayContent, z2);
        displayContent.mWallpaperController.addWallpaperToken(this);
    }

    @Override
    void setExiting() {
        super.setExiting();
        this.mDisplayContent.mWallpaperController.removeWallpaperToken(this);
    }

    void hideWallpaperToken(boolean z, String str) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).hideWallpaperWindow(z, str);
        }
        setHidden(true);
    }

    void sendWindowWallpaperCommand(String str, int i, int i2, int i3, Bundle bundle, boolean z) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            try {
                ((WindowState) this.mChildren.get(size)).mClient.dispatchWallpaperCommand(str, i, i2, i3, bundle, z);
                z = false;
            } catch (RemoteException e) {
            }
        }
    }

    void updateWallpaperOffset(int i, int i2, boolean z) {
        WallpaperController wallpaperController = this.mDisplayContent.mWallpaperController;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (wallpaperController.updateWallpaperOffset((WindowState) this.mChildren.get(size), i, i2, z)) {
                z = false;
            }
        }
    }

    void updateWallpaperVisibility(boolean z) {
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        int i = displayInfo.logicalWidth;
        int i2 = displayInfo.logicalHeight;
        if (isHidden() == z) {
            setHidden(!z);
            this.mDisplayContent.setLayoutNeeded();
        }
        WallpaperController wallpaperController = this.mDisplayContent.mWallpaperController;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState windowState = (WindowState) this.mChildren.get(size);
            if (z) {
                wallpaperController.updateWallpaperOffset(windowState, i, i2, false);
            }
            windowState.dispatchWallpaperVisibility(z);
        }
    }

    void startAnimation(Animation animation) {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            ((WindowState) this.mChildren.get(size)).startAnimation(animation);
        }
    }

    void updateWallpaperWindows(boolean z) {
        if (isHidden() == z) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                StringBuilder sb = new StringBuilder();
                sb.append("Wallpaper token ");
                sb.append(this.token);
                sb.append(" hidden=");
                sb.append(!z);
                Slog.d("WindowManager", sb.toString());
            }
            setHidden(!z);
            this.mDisplayContent.setLayoutNeeded();
        }
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        int i = displayInfo.logicalWidth;
        int i2 = displayInfo.logicalHeight;
        WallpaperController wallpaperController = this.mDisplayContent.mWallpaperController;
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            WindowState windowState = (WindowState) this.mChildren.get(size);
            if (z) {
                wallpaperController.updateWallpaperOffset(windowState, i, i2, false);
            }
            windowState.dispatchWallpaperVisibility(z);
            if (WindowManagerDebugConfig.DEBUG_LAYERS || WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT) {
                Slog.v("WindowManager", "adjustWallpaper win " + windowState + " anim layer: " + windowState.mWinAnimator.mAnimLayer);
            }
        }
    }

    boolean hasVisibleNotDrawnWallpaper() {
        for (int size = this.mChildren.size() - 1; size >= 0; size--) {
            if (((WindowState) this.mChildren.get(size)).hasVisibleNotDrawnWallpaper()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (this.stringName == null) {
            this.stringName = "WallpaperWindowToken{" + Integer.toHexString(System.identityHashCode(this)) + " token=" + this.token + '}';
        }
        return this.stringName;
    }
}
