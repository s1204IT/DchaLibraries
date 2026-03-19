package com.android.server.wm;

import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.SparseArray;
import android.view.IWallpaperVisibilityListener;

class WallpaperVisibilityListeners {
    private final SparseArray<RemoteCallbackList<IWallpaperVisibilityListener>> mDisplayListeners = new SparseArray<>();

    WallpaperVisibilityListeners() {
    }

    void registerWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) {
        RemoteCallbackList<IWallpaperVisibilityListener> remoteCallbackList = this.mDisplayListeners.get(i);
        if (remoteCallbackList == null) {
            remoteCallbackList = new RemoteCallbackList<>();
            this.mDisplayListeners.append(i, remoteCallbackList);
        }
        remoteCallbackList.register(iWallpaperVisibilityListener);
    }

    void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) {
        RemoteCallbackList<IWallpaperVisibilityListener> remoteCallbackList = this.mDisplayListeners.get(i);
        if (remoteCallbackList == null) {
            return;
        }
        remoteCallbackList.unregister(iWallpaperVisibilityListener);
    }

    void notifyWallpaperVisibilityChanged(DisplayContent displayContent) {
        int displayId = displayContent.getDisplayId();
        boolean zIsWallpaperVisible = displayContent.mWallpaperController.isWallpaperVisible();
        RemoteCallbackList<IWallpaperVisibilityListener> remoteCallbackList = this.mDisplayListeners.get(displayId);
        if (remoteCallbackList == null) {
            return;
        }
        int iBeginBroadcast = remoteCallbackList.beginBroadcast();
        while (iBeginBroadcast > 0) {
            iBeginBroadcast--;
            try {
                remoteCallbackList.getBroadcastItem(iBeginBroadcast).onWallpaperVisibilityChanged(zIsWallpaperVisible, displayId);
            } catch (RemoteException e) {
            }
        }
        remoteCallbackList.finishBroadcast();
    }
}
