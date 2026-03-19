package com.android.server.wm;

import android.content.res.Configuration;
import com.android.server.wm.WindowContainer;
import com.android.server.wm.WindowContainerListener;

class WindowContainerController<E extends WindowContainer, I extends WindowContainerListener> implements ConfigurationContainerListener {
    E mContainer;
    final I mListener;
    final RootWindowContainer mRoot;
    final WindowManagerService mService;
    final WindowHashMap mWindowMap;

    WindowContainerController(I i, WindowManagerService windowManagerService) {
        this.mListener = i;
        this.mService = windowManagerService;
        this.mRoot = this.mService != null ? this.mService.mRoot : null;
        this.mWindowMap = this.mService != null ? this.mService.mWindowMap : null;
    }

    void setContainer(E e) {
        if (this.mContainer != null && e != null) {
            throw new IllegalArgumentException("Can't set container=" + e + " for controller=" + this + " Already set to=" + this.mContainer);
        }
        this.mContainer = e;
        if (this.mContainer != null && this.mListener != null) {
            this.mListener.registerConfigurationChangeListener(this);
        }
    }

    void removeContainer() {
        if (this.mContainer == null) {
            return;
        }
        this.mContainer.setController(null);
        this.mContainer = null;
        if (this.mListener != null) {
            this.mListener.unregisterConfigurationChangeListener(this);
        }
    }

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    this.mContainer.onOverrideConfigurationChanged(configuration);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }
}
