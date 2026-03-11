package com.android.systemui.stackdivider;

import android.content.res.Configuration;
import android.os.RemoteException;
import android.view.IDockedStackListener;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class Divider extends SystemUI {
    private DockDividerVisibilityListener mDockDividerVisibilityListener;
    private ForcedResizableInfoActivityController mForcedResizableController;
    private DividerView mView;
    private DividerWindowManager mWindowManager;
    private final DividerState mDividerState = new DividerState();
    private boolean mVisible = false;
    private boolean mMinimized = false;
    private boolean mAdjustedForIme = false;

    @Override
    public void start() {
        this.mWindowManager = new DividerWindowManager(this.mContext);
        update(this.mContext.getResources().getConfiguration());
        putComponent(Divider.class, this);
        this.mDockDividerVisibilityListener = new DockDividerVisibilityListener();
        SystemServicesProxy ssp = Recents.getSystemServices();
        ssp.registerDockedStackListener(this.mDockDividerVisibilityListener);
        this.mForcedResizableController = new ForcedResizableInfoActivityController(this.mContext);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        update(newConfig);
    }

    public DividerView getView() {
        return this.mView;
    }

    private void addDivider(Configuration configuration) {
        this.mView = (DividerView) LayoutInflater.from(this.mContext).inflate(R.layout.docked_stack_divider, (ViewGroup) null);
        this.mView.setVisibility(this.mVisible ? 0 : 4);
        int size = this.mContext.getResources().getDimensionPixelSize(android.R.dimen.action_bar_elevation_material);
        boolean landscape = configuration.orientation == 2;
        int width = landscape ? size : -1;
        int height = landscape ? -1 : size;
        this.mWindowManager.add(this.mView, width, height);
        this.mView.injectDependencies(this.mWindowManager, this.mDividerState);
    }

    private void removeDivider() {
        this.mWindowManager.remove();
    }

    private void update(Configuration configuration) {
        removeDivider();
        addDivider(configuration);
        if (!this.mMinimized) {
            return;
        }
        this.mView.setMinimizedDockStack(true);
        updateTouchable();
    }

    public void updateVisibility(final boolean visible) {
        this.mView.post(new Runnable() {
            @Override
            public void run() {
                if (Divider.this.mVisible == visible) {
                    return;
                }
                Divider.this.mVisible = visible;
                Divider.this.mView.setVisibility(visible ? 0 : 4);
                Divider.this.mView.setMinimizedDockStack(Divider.this.mMinimized);
            }
        });
    }

    public void updateMinimizedDockedStack(final boolean minimized, final long animDuration) {
        this.mView.post(new Runnable() {
            @Override
            public void run() {
                if (Divider.this.mMinimized == minimized) {
                    return;
                }
                Divider.this.mMinimized = minimized;
                Divider.this.updateTouchable();
                if (animDuration > 0) {
                    Divider.this.mView.setMinimizedDockStack(minimized, animDuration);
                } else {
                    Divider.this.mView.setMinimizedDockStack(minimized);
                }
            }
        });
    }

    public void notifyDockedStackExistsChanged(final boolean exists) {
        this.mView.post(new Runnable() {
            @Override
            public void run() {
                Divider.this.mForcedResizableController.notifyDockedStackExistsChanged(exists);
            }
        });
    }

    public void updateTouchable() {
        boolean z = false;
        DividerWindowManager dividerWindowManager = this.mWindowManager;
        if (!this.mMinimized && !this.mAdjustedForIme) {
            z = true;
        }
        dividerWindowManager.setTouchable(z);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("  mVisible=");
        pw.println(this.mVisible);
        pw.print("  mMinimized=");
        pw.println(this.mMinimized);
        pw.print("  mAdjustedForIme=");
        pw.println(this.mAdjustedForIme);
    }

    class DockDividerVisibilityListener extends IDockedStackListener.Stub {
        DockDividerVisibilityListener() {
        }

        public void onDividerVisibilityChanged(boolean visible) throws RemoteException {
            Divider.this.updateVisibility(visible);
        }

        public void onDockedStackExistsChanged(boolean exists) throws RemoteException {
            Divider.this.notifyDockedStackExistsChanged(exists);
        }

        public void onDockedStackMinimizedChanged(boolean minimized, long animDuration) throws RemoteException {
            Divider.this.updateMinimizedDockedStack(minimized, animDuration);
        }

        public void onAdjustedForImeChanged(final boolean adjustedForIme, final long animDuration) throws RemoteException {
            Divider.this.mView.post(new Runnable() {
                @Override
                public void run() {
                    DockDividerVisibilityListener.this.m1128xc2b650df(adjustedForIme, animDuration);
                }
            });
        }

        void m1128xc2b650df(boolean adjustedForIme, long animDuration) {
            if (Divider.this.mAdjustedForIme != adjustedForIme) {
                Divider.this.mAdjustedForIme = adjustedForIme;
                Divider.this.updateTouchable();
                if (Divider.this.mMinimized) {
                    return;
                }
                if (animDuration > 0) {
                    Divider.this.mView.setAdjustedForIme(adjustedForIme, animDuration);
                } else {
                    Divider.this.mView.setAdjustedForIme(adjustedForIme);
                }
            }
        }

        void m1129xc2b650e0(int newDockSide) {
            Divider.this.mView.notifyDockSideChanged(newDockSide);
        }

        public void onDockSideChanged(final int newDockSide) throws RemoteException {
            Divider.this.mView.post(new Runnable() {
                @Override
                public void run() {
                    DockDividerVisibilityListener.this.m1129xc2b650e0(newDockSide);
                }
            });
        }
    }
}
