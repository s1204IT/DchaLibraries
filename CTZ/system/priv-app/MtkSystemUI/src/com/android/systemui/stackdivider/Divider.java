package com.android.systemui.stackdivider;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.RemoteException;
import android.view.IDockedStackListener;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.stackdivider.Divider;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class Divider extends SystemUI {
    private DockDividerVisibilityListener mDockDividerVisibilityListener;
    private ForcedResizableInfoActivityController mForcedResizableController;
    private DividerView mView;
    private DividerWindowManager mWindowManager;
    private final DividerState mDividerState = new DividerState();
    private boolean mVisible = false;
    private boolean mMinimized = false;
    private boolean mAdjustedForIme = false;
    private boolean mHomeStackResizable = false;

    @Override // com.android.systemui.SystemUI
    public void start() throws Resources.NotFoundException, NoSuchMethodException, SecurityException {
        this.mWindowManager = new DividerWindowManager(this.mContext);
        update(this.mContext.getResources().getConfiguration());
        putComponent(Divider.class, this);
        this.mDockDividerVisibilityListener = new DockDividerVisibilityListener();
        Recents.getSystemServices().registerDockedStackListener(this.mDockDividerVisibilityListener);
        this.mForcedResizableController = new ForcedResizableInfoActivityController(this.mContext);
        EventBus.getDefault().register(this);
    }

    @Override // com.android.systemui.SystemUI
    protected void onConfigurationChanged(Configuration configuration) throws Resources.NotFoundException {
        super.onConfigurationChanged(configuration);
        update(configuration);
    }

    public DividerView getView() {
        return this.mView;
    }

    public boolean isMinimized() {
        return this.mMinimized;
    }

    public boolean isHomeStackResizable() {
        return this.mHomeStackResizable;
    }

    private void addDivider(Configuration configuration) throws Resources.NotFoundException {
        this.mView = (DividerView) LayoutInflater.from(this.mContext).inflate(R.layout.docked_stack_divider, (ViewGroup) null);
        this.mView.injectDependencies(this.mWindowManager, this.mDividerState);
        this.mView.setVisibility(this.mVisible ? 0 : 4);
        this.mView.setMinimizedDockStack(this.mMinimized, this.mHomeStackResizable);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(android.R.dimen.car_padding_2);
        boolean z = configuration.orientation == 2;
        int i = -1;
        int i2 = z ? dimensionPixelSize : -1;
        if (!z) {
            i = dimensionPixelSize;
        }
        this.mWindowManager.add(this.mView, i2, i);
    }

    private void removeDivider() {
        if (this.mView != null) {
            this.mView.onDividerRemoved();
        }
        this.mWindowManager.remove();
    }

    private void update(Configuration configuration) throws Resources.NotFoundException {
        removeDivider();
        addDivider(configuration);
        if (this.mMinimized) {
            this.mView.setMinimizedDockStack(true, this.mHomeStackResizable);
            updateTouchable();
        }
    }

    /* renamed from: com.android.systemui.stackdivider.Divider$1 */
    class AnonymousClass1 implements Runnable {
        final /* synthetic */ boolean val$visible;

        AnonymousClass1(boolean z) {
            z = z;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (Divider.this.mVisible != z) {
                Divider.this.mVisible = z;
                Divider.this.mView.setVisibility(z ? 0 : 4);
                Divider.this.mView.setMinimizedDockStack(Divider.this.mMinimized, Divider.this.mHomeStackResizable);
            }
        }
    }

    private void updateVisibility(boolean z) {
        this.mView.post(new Runnable() { // from class: com.android.systemui.stackdivider.Divider.1
            final /* synthetic */ boolean val$visible;

            AnonymousClass1(boolean z2) {
                z = z2;
            }

            @Override // java.lang.Runnable
            public void run() {
                if (Divider.this.mVisible != z) {
                    Divider.this.mVisible = z;
                    Divider.this.mView.setVisibility(z ? 0 : 4);
                    Divider.this.mView.setMinimizedDockStack(Divider.this.mMinimized, Divider.this.mHomeStackResizable);
                }
            }
        });
    }

    /* renamed from: com.android.systemui.stackdivider.Divider$2 */
    class AnonymousClass2 implements Runnable {
        final /* synthetic */ long val$animDuration;
        final /* synthetic */ boolean val$isHomeStackResizable;
        final /* synthetic */ boolean val$minimized;

        AnonymousClass2(boolean z, boolean z2, long j) {
            z = z;
            z = z2;
            j = j;
        }

        @Override // java.lang.Runnable
        public void run() {
            Divider.this.mHomeStackResizable = z;
            if (Divider.this.mMinimized != z) {
                Divider.this.mMinimized = z;
                Divider.this.updateTouchable();
                if (j > 0) {
                    Divider.this.mView.setMinimizedDockStack(z, j, z);
                } else {
                    Divider.this.mView.setMinimizedDockStack(z, z);
                }
            }
        }
    }

    private void updateMinimizedDockedStack(boolean z, long j, boolean z2) {
        this.mView.post(new Runnable() { // from class: com.android.systemui.stackdivider.Divider.2
            final /* synthetic */ long val$animDuration;
            final /* synthetic */ boolean val$isHomeStackResizable;
            final /* synthetic */ boolean val$minimized;

            AnonymousClass2(boolean z22, boolean z3, long j2) {
                z = z22;
                z = z3;
                j = j2;
            }

            @Override // java.lang.Runnable
            public void run() {
                Divider.this.mHomeStackResizable = z;
                if (Divider.this.mMinimized != z) {
                    Divider.this.mMinimized = z;
                    Divider.this.updateTouchable();
                    if (j > 0) {
                        Divider.this.mView.setMinimizedDockStack(z, j, z);
                    } else {
                        Divider.this.mView.setMinimizedDockStack(z, z);
                    }
                }
            }
        });
    }

    /* renamed from: com.android.systemui.stackdivider.Divider$3 */
    class AnonymousClass3 implements Runnable {
        final /* synthetic */ boolean val$exists;

        AnonymousClass3(boolean z) {
            z = z;
        }

        @Override // java.lang.Runnable
        public void run() {
            Divider.this.mForcedResizableController.notifyDockedStackExistsChanged(z);
        }
    }

    private void notifyDockedStackExistsChanged(boolean z) {
        this.mView.post(new Runnable() { // from class: com.android.systemui.stackdivider.Divider.3
            final /* synthetic */ boolean val$exists;

            AnonymousClass3(boolean z2) {
                z = z2;
            }

            @Override // java.lang.Runnable
            public void run() {
                Divider.this.mForcedResizableController.notifyDockedStackExistsChanged(z);
            }
        });
    }

    private void updateTouchable() {
        this.mWindowManager.setTouchable((this.mHomeStackResizable || !this.mMinimized) && !this.mAdjustedForIme);
    }

    public final void onBusEvent(RecentsDrawnEvent recentsDrawnEvent) {
        if (this.mView != null) {
            this.mView.onRecentsDrawn();
        }
    }

    @Override // com.android.systemui.SystemUI
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mVisible=");
        printWriter.println(this.mVisible);
        printWriter.print("  mMinimized=");
        printWriter.println(this.mMinimized);
        printWriter.print("  mAdjustedForIme=");
        printWriter.println(this.mAdjustedForIme);
    }

    class DockDividerVisibilityListener extends IDockedStackListener.Stub {
        DockDividerVisibilityListener() {
        }

        public void onDividerVisibilityChanged(boolean z) throws RemoteException {
            Divider.this.updateVisibility(z);
        }

        public void onDockedStackExistsChanged(boolean z) throws RemoteException {
            Divider.this.notifyDockedStackExistsChanged(z);
        }

        public void onDockedStackMinimizedChanged(boolean z, long j, boolean z2) throws RemoteException {
            Divider.this.mHomeStackResizable = z2;
            Divider.this.updateMinimizedDockedStack(z, j, z2);
        }

        public void onAdjustedForImeChanged(final boolean z, final long j) throws RemoteException {
            Divider.this.mView.post(new Runnable() { // from class: com.android.systemui.stackdivider.-$$Lambda$Divider$DockDividerVisibilityListener$fZDE4rhC5s3QEgR-7YXeKi_feiY
                @Override // java.lang.Runnable
                public final void run() {
                    Divider.DockDividerVisibilityListener.lambda$onAdjustedForImeChanged$0(this.f$0, z, j);
                }
            });
        }

        public static /* synthetic */ void lambda$onAdjustedForImeChanged$0(DockDividerVisibilityListener dockDividerVisibilityListener, boolean z, long j) {
            if (Divider.this.mAdjustedForIme != z) {
                Divider.this.mAdjustedForIme = z;
                Divider.this.updateTouchable();
                if (Divider.this.mMinimized) {
                    return;
                }
                if (j > 0) {
                    Divider.this.mView.setAdjustedForIme(z, j);
                } else {
                    Divider.this.mView.setAdjustedForIme(z);
                }
            }
        }

        public void onDockSideChanged(final int i) throws RemoteException {
            Divider.this.mView.post(new Runnable() { // from class: com.android.systemui.stackdivider.-$$Lambda$Divider$DockDividerVisibilityListener$cPiHgQdgCDQeKAQTEdGGnGaaM_c
                @Override // java.lang.Runnable
                public final void run() {
                    Divider.this.mView.notifyDockSideChanged(i);
                }
            });
        }
    }
}
