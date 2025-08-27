package com.android.systemui.shared.system;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.view.IRotationWatcher;
import android.view.WindowManagerGlobal;

/* loaded from: classes.dex */
public abstract class RotationWatcher {
    private static final String TAG = "RotationWatcher";
    private final Context mContext;
    private final IRotationWatcher mWatcher = new IRotationWatcher.Stub() { // from class: com.android.systemui.shared.system.RotationWatcher.1
        AnonymousClass1() {
        }

        public void onRotationChanged(int rotation) {
            RotationWatcher.this.onRotationChanged(rotation);
        }
    };
    private boolean mIsWatching = false;

    protected abstract void onRotationChanged(int i);

    /* renamed from: com.android.systemui.shared.system.RotationWatcher$1 */
    class AnonymousClass1 extends IRotationWatcher.Stub {
        AnonymousClass1() {
        }

        public void onRotationChanged(int rotation) {
            RotationWatcher.this.onRotationChanged(rotation);
        }
    }

    public RotationWatcher(Context context) {
        this.mContext = context;
    }

    public void enable() {
        if (!this.mIsWatching) {
            try {
                WindowManagerGlobal.getWindowManagerService().watchRotation(this.mWatcher, this.mContext.getDisplay().getDisplayId());
                this.mIsWatching = true;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to set rotation watcher", e);
            }
        }
    }

    public void disable() {
        if (this.mIsWatching) {
            try {
                WindowManagerGlobal.getWindowManagerService().removeRotationWatcher(this.mWatcher);
                this.mIsWatching = false;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to remove rotation watcher", e);
            }
        }
    }
}
