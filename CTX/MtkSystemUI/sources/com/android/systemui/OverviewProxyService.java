package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.SurfaceControl;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.GraphicBufferCompat;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.CallbackController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class OverviewProxyService implements Dumpable, CallbackController<OverviewProxyListener> {
    private final Context mContext;
    private int mInteractionFlags;
    private boolean mIsEnabled;
    private IOverviewProxy mOverviewProxy;
    private final Intent mQuickStepIntent;
    private final ComponentName mRecentsComponentName;
    private final Runnable mConnectionRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.internalConnectToCurrentUser();
        }
    };
    private final DeviceProvisionedController mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
    private final List<OverviewProxyListener> mConnectionCallbacks = new ArrayList();
    private ISystemUiProxy mSysUiProxy = new AnonymousClass1();
    private final Runnable mDeferredConnectionCallback = new Runnable() {
        @Override
        public final void run() {
            OverviewProxyService.lambda$new$0(this.f$0);
        }
    };
    private final BroadcastReceiver mLauncherStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OverviewProxyService.this.updateEnabledState();
            if (!OverviewProxyService.this.isEnabled()) {
                OverviewProxyService.this.mInteractionFlags = 0;
                Prefs.remove(OverviewProxyService.this.mContext, "QuickStepInteractionFlags");
            }
            OverviewProxyService.this.startConnectionToCurrentUser();
        }
    };
    private final ServiceConnection mOverviewServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OverviewProxyService.this.mHandler.removeCallbacks(OverviewProxyService.this.mDeferredConnectionCallback);
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.mOverviewProxy = IOverviewProxy.Stub.asInterface(iBinder);
            try {
                iBinder.linkToDeath(OverviewProxyService.this.mOverviewServiceDeathRcpt, 0);
            } catch (RemoteException e) {
                Log.e("OverviewProxyService", "Lost connection to launcher service", e);
            }
            try {
                OverviewProxyService.this.mOverviewProxy.onBind(OverviewProxyService.this.mSysUiProxy);
            } catch (RemoteException e2) {
                Log.e("OverviewProxyService", "Failed to call onBind()", e2);
            }
            OverviewProxyService.this.notifyConnectionChanged();
        }

        @Override
        public void onNullBinding(ComponentName componentName) {
            Log.w("OverviewProxyService", "Null binding of '" + componentName + "', try reconnecting");
            OverviewProxyService.this.internalConnectToCurrentUser();
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            Log.w("OverviewProxyService", "Binding died of '" + componentName + "', try reconnecting");
            OverviewProxyService.this.internalConnectToCurrentUser();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
    private final DeviceProvisionedController.DeviceProvisionedListener mDeviceProvisionedCallback = new DeviceProvisionedController.DeviceProvisionedListener() {
        @Override
        public void onUserSetupChanged() {
            if (OverviewProxyService.this.mDeviceProvisionedController.isCurrentUserSetup()) {
                OverviewProxyService.this.internalConnectToCurrentUser();
            }
        }

        @Override
        public void onUserSwitched() {
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.internalConnectToCurrentUser();
        }
    };
    private final IBinder.DeathRecipient mOverviewServiceDeathRcpt = new IBinder.DeathRecipient() {
        @Override
        public final void binderDied() {
            this.f$0.startConnectionToCurrentUser();
        }
    };
    private final Handler mHandler = new Handler();
    private int mConnectionBackoffAttempts = 0;

    class AnonymousClass1 extends ISystemUiProxy.Stub {
        AnonymousClass1() {
        }

        @Override
        public GraphicBufferCompat screenshot(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return new GraphicBufferCompat(SurfaceControl.screenshotToBuffer(rect, i, i2, i3, i4, z, i5));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void startScreenPinning(final int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                OverviewProxyService.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        OverviewProxyService.AnonymousClass1.lambda$startScreenPinning$0(this.f$0, i);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public static void lambda$startScreenPinning$0(AnonymousClass1 anonymousClass1, int i) {
            StatusBar statusBar = (StatusBar) ((SystemUIApplication) OverviewProxyService.this.mContext).getComponent(StatusBar.class);
            if (statusBar != null) {
                statusBar.showScreenPinningRequest(i, false);
            }
        }

        @Override
        public void onSplitScreenInvoked() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                EventBus.getDefault().post(new DockedFirstAnimationFrameEvent());
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void onOverviewShown(final boolean z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                OverviewProxyService.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        OverviewProxyService.AnonymousClass1.lambda$onOverviewShown$1(this.f$0, z);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public static void lambda$onOverviewShown$1(AnonymousClass1 anonymousClass1, boolean z) {
            for (int size = OverviewProxyService.this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
                ((OverviewProxyListener) OverviewProxyService.this.mConnectionCallbacks.get(size)).onOverviewShown(z);
            }
        }

        @Override
        public void setInteractionState(final int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (OverviewProxyService.this.mInteractionFlags != i) {
                    OverviewProxyService.this.mInteractionFlags = i;
                    OverviewProxyService.this.mHandler.post(new Runnable() {
                        @Override
                        public final void run() {
                            OverviewProxyService.AnonymousClass1.lambda$setInteractionState$2(this.f$0, i);
                        }
                    });
                }
                Prefs.putInt(OverviewProxyService.this.mContext, "QuickStepInteractionFlags", OverviewProxyService.this.mInteractionFlags);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            } catch (Throwable th) {
                Prefs.putInt(OverviewProxyService.this.mContext, "QuickStepInteractionFlags", OverviewProxyService.this.mInteractionFlags);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }

        public static void lambda$setInteractionState$2(AnonymousClass1 anonymousClass1, int i) {
            for (int size = OverviewProxyService.this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
                ((OverviewProxyListener) OverviewProxyService.this.mConnectionCallbacks.get(size)).onInteractionFlagsChanged(i);
            }
        }

        @Override
        public Rect getNonMinimizedSplitScreenSecondaryBounds() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Divider divider = (Divider) ((SystemUIApplication) OverviewProxyService.this.mContext).getComponent(Divider.class);
                if (divider != null) {
                    return divider.getView().getNonMinimizedSplitScreenSecondaryBounds();
                }
                return null;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        @Override
        public void setBackButtonAlpha(final float f, final boolean z) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                OverviewProxyService.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        OverviewProxyService.this.notifyBackButtonAlphaChanged(f, z);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public static void lambda$new$0(OverviewProxyService overviewProxyService) {
        Log.w("OverviewProxyService", "Binder supposed established connection but actual connection to service timed out, trying again");
        overviewProxyService.internalConnectToCurrentUser();
    }

    public OverviewProxyService(Context context) {
        this.mContext = context;
        this.mRecentsComponentName = ComponentName.unflattenFromString(context.getString(android.R.string.app_blocked_message));
        this.mQuickStepIntent = new Intent("android.intent.action.QUICKSTEP_SERVICE").setPackage(this.mRecentsComponentName.getPackageName());
        this.mInteractionFlags = Prefs.getInt(this.mContext, "QuickStepInteractionFlags", 0);
        if (SystemServicesProxy.getInstance(context).isSystemUser(this.mDeviceProvisionedController.getCurrentUser())) {
            updateEnabledState();
            this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedCallback);
            IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            intentFilter.addDataScheme("package");
            intentFilter.addDataSchemeSpecificPart(this.mRecentsComponentName.getPackageName(), 0);
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            this.mContext.registerReceiver(this.mLauncherStateChangedReceiver, intentFilter);
        }
    }

    public void startConnectionToCurrentUser() {
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            this.mHandler.post(this.mConnectionRunnable);
        } else {
            internalConnectToCurrentUser();
        }
    }

    public void internalConnectToCurrentUser() {
        boolean zBindServiceAsUser;
        disconnectFromLauncherService();
        if (!this.mDeviceProvisionedController.isCurrentUserSetup() || !isEnabled()) {
            Log.v("OverviewProxyService", "Cannot attempt connection, is setup " + this.mDeviceProvisionedController.isCurrentUserSetup() + ", is enabled " + isEnabled());
            return;
        }
        this.mHandler.removeCallbacks(this.mConnectionRunnable);
        try {
            zBindServiceAsUser = this.mContext.bindServiceAsUser(new Intent("android.intent.action.QUICKSTEP_SERVICE").setPackage(this.mRecentsComponentName.getPackageName()), this.mOverviewServiceConnection, 1, UserHandle.of(this.mDeviceProvisionedController.getCurrentUser()));
        } catch (SecurityException e) {
            Log.e("OverviewProxyService", "Unable to bind because of security error", e);
            zBindServiceAsUser = false;
        }
        if (zBindServiceAsUser) {
            this.mHandler.postDelayed(this.mDeferredConnectionCallback, 5000L);
            return;
        }
        long jScalb = (long) Math.scalb(5000.0f, this.mConnectionBackoffAttempts);
        this.mHandler.postDelayed(this.mConnectionRunnable, jScalb);
        this.mConnectionBackoffAttempts++;
        Log.w("OverviewProxyService", "Failed to connect on attempt " + this.mConnectionBackoffAttempts + " will try again in " + jScalb + "ms");
    }

    @Override
    public void addCallback(OverviewProxyListener overviewProxyListener) {
        this.mConnectionCallbacks.add(overviewProxyListener);
        overviewProxyListener.onConnectionChanged(this.mOverviewProxy != null);
        overviewProxyListener.onInteractionFlagsChanged(this.mInteractionFlags);
    }

    @Override
    public void removeCallback(OverviewProxyListener overviewProxyListener) {
        this.mConnectionCallbacks.remove(overviewProxyListener);
    }

    public boolean shouldShowSwipeUpUI() {
        return isEnabled() && (this.mInteractionFlags & 1) == 0;
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public IOverviewProxy getProxy() {
        return this.mOverviewProxy;
    }

    public int getInteractionFlags() {
        return this.mInteractionFlags;
    }

    private void disconnectFromLauncherService() {
        if (this.mOverviewProxy != null) {
            this.mOverviewProxy.asBinder().unlinkToDeath(this.mOverviewServiceDeathRcpt, 0);
            this.mContext.unbindService(this.mOverviewServiceConnection);
            this.mOverviewProxy = null;
            notifyBackButtonAlphaChanged(1.0f, false);
            notifyConnectionChanged();
        }
    }

    public void notifyBackButtonAlphaChanged(float f, boolean z) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onBackButtonAlphaChanged(f, z);
        }
    }

    public void notifyConnectionChanged() {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onConnectionChanged(this.mOverviewProxy != null);
        }
    }

    public void notifyQuickStepStarted() {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onQuickStepStarted();
        }
    }

    public void notifyQuickScrubStarted() {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onQuickScrubStarted();
        }
    }

    public void updateEnabledState() {
        this.mIsEnabled = this.mContext.getPackageManager().resolveServiceAsUser(this.mQuickStepIntent, 262144, ActivityManagerWrapper.getInstance().getCurrentUserId()) != null;
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("OverviewProxyService state:");
        printWriter.print("  mConnectionBackoffAttempts=");
        printWriter.println(this.mConnectionBackoffAttempts);
        printWriter.print("  isCurrentUserSetup=");
        printWriter.println(this.mDeviceProvisionedController.isCurrentUserSetup());
        printWriter.print("  isConnected=");
        printWriter.println(this.mOverviewProxy != null);
        printWriter.print("  mRecentsComponentName=");
        printWriter.println(this.mRecentsComponentName);
        printWriter.print("  mIsEnabled=");
        printWriter.println(isEnabled());
        printWriter.print("  mInteractionFlags=");
        printWriter.println(this.mInteractionFlags);
        printWriter.print("  mQuickStepIntent=");
        printWriter.println(this.mQuickStepIntent);
    }

    public interface OverviewProxyListener {
        default void onConnectionChanged(boolean z) {
        }

        default void onQuickStepStarted() {
        }

        default void onInteractionFlagsChanged(int i) {
        }

        default void onOverviewShown(boolean z) {
        }

        default void onQuickScrubStarted() {
        }

        default void onBackButtonAlphaChanged(float f, boolean z) {
        }
    }
}
