package com.android.systemui.statusbar.policy;

import android.content.Context;
import com.android.internal.view.RotationPolicy;
import com.android.systemui.statusbar.policy.RotationLockController;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RotationLockControllerImpl implements RotationLockController {
    private final Context mContext;
    private final CopyOnWriteArrayList<RotationLockController.RotationLockControllerCallback> mCallbacks = new CopyOnWriteArrayList<>();
    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
        public void onChange() {
            RotationLockControllerImpl.this.notifyChanged();
        }
    };

    public RotationLockControllerImpl(Context context) {
        this.mContext = context;
        setListening(true);
    }

    @Override
    public void addRotationLockControllerCallback(RotationLockController.RotationLockControllerCallback callback) {
        this.mCallbacks.add(callback);
        notifyChanged(callback);
    }

    @Override
    public void removeRotationLockControllerCallback(RotationLockController.RotationLockControllerCallback callback) {
        this.mCallbacks.remove(callback);
    }

    @Override
    public int getRotationLockOrientation() {
        return RotationPolicy.getRotationLockOrientation(this.mContext);
    }

    @Override
    public boolean isRotationLocked() {
        return RotationPolicy.isRotationLocked(this.mContext);
    }

    @Override
    public void setRotationLocked(boolean locked) {
        RotationPolicy.setRotationLock(this.mContext, locked);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            RotationPolicy.registerRotationPolicyListener(this.mContext, this.mRotationPolicyListener, -1);
        } else {
            RotationPolicy.unregisterRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
        }
    }

    public void notifyChanged() {
        for (RotationLockController.RotationLockControllerCallback callback : this.mCallbacks) {
            notifyChanged(callback);
        }
    }

    private void notifyChanged(RotationLockController.RotationLockControllerCallback callback) {
        callback.onRotationLockStateChanged(RotationPolicy.isRotationLocked(this.mContext), RotationPolicy.isRotationLockToggleVisible(this.mContext));
    }
}
