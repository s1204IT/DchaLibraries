package com.android.systemui.assist;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionListener;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.applications.InterestingConfigChanges;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

public class AssistManager implements ConfigurationChangedReceiver {
    private final AssistDisclosure mAssistDisclosure;
    protected final AssistUtils mAssistUtils;
    protected final Context mContext;
    private final DeviceProvisionedController mDeviceProvisionedController;
    private final InterestingConfigChanges mInterestingConfigChanges;
    private final boolean mShouldEnableOrb;
    private AssistOrbContainer mView;
    private final WindowManager mWindowManager;
    private IVoiceInteractionSessionShowCallback mShowCallback = new IVoiceInteractionSessionShowCallback.Stub() {
        public void onFailed() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }

        public void onShown() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }
    };
    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            AssistManager.this.mView.removeCallbacks(this);
            AssistManager.this.mView.show(false, true);
        }
    };

    public AssistManager(DeviceProvisionedController deviceProvisionedController, Context context) {
        this.mContext = context;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAssistUtils = new AssistUtils(context);
        this.mAssistDisclosure = new AssistDisclosure(context, new Handler());
        registerVoiceInteractionSessionListener();
        this.mInterestingConfigChanges = new InterestingConfigChanges(-2147482748);
        onConfigurationChanged(context.getResources().getConfiguration());
        this.mShouldEnableOrb = !ActivityManager.isLowRamDeviceStatic();
    }

    protected void registerVoiceInteractionSessionListener() {
        this.mAssistUtils.registerVoiceInteractionSessionListener(new IVoiceInteractionSessionListener.Stub() {
            public void onVoiceSessionShown() throws RemoteException {
                Log.v("AssistManager", "Voice open");
            }

            public void onVoiceSessionHidden() throws RemoteException {
                Log.v("AssistManager", "Voice closed");
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        boolean zIsShowing;
        if (!this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
            return;
        }
        if (this.mView != null) {
            zIsShowing = this.mView.isShowing();
            this.mWindowManager.removeView(this.mView);
        } else {
            zIsShowing = false;
        }
        this.mView = (AssistOrbContainer) LayoutInflater.from(this.mContext).inflate(R.layout.assist_orb, (ViewGroup) null);
        this.mView.setVisibility(8);
        this.mView.setSystemUiVisibility(1792);
        this.mWindowManager.addView(this.mView, getLayoutParams());
        if (zIsShowing) {
            this.mView.show(true, false);
        }
    }

    protected boolean shouldShowOrb() {
        return false;
    }

    public void startAssist(Bundle bundle) {
        long j;
        ComponentName assistInfo = getAssistInfo();
        if (assistInfo == null) {
            return;
        }
        boolean zEquals = assistInfo.equals(getVoiceInteractorComponentName());
        if (!zEquals || (!isVoiceSessionRunning() && shouldShowOrb())) {
            showOrb(assistInfo, zEquals);
            AssistOrbContainer assistOrbContainer = this.mView;
            Runnable runnable = this.mHideRunnable;
            if (zEquals) {
                j = 2500;
            } else {
                j = 1000;
            }
            assistOrbContainer.postDelayed(runnable, j);
        }
        startAssistInternal(bundle, assistInfo, zEquals);
    }

    public void hideAssist() {
        this.mAssistUtils.hideCurrentSession();
    }

    private WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(R.dimen.assist_orb_scrim_height), 2033, 280, -3);
        layoutParams.token = new Binder();
        layoutParams.gravity = 8388691;
        layoutParams.setTitle("AssistPreviewPanel");
        layoutParams.softInputMode = 49;
        return layoutParams;
    }

    private void showOrb(ComponentName componentName, boolean z) {
        maybeSwapSearchIcon(componentName, z);
        if (this.mShouldEnableOrb) {
            this.mView.show(true, true);
        }
    }

    private void startAssistInternal(Bundle bundle, ComponentName componentName, boolean z) {
        if (z) {
            startVoiceInteractor(bundle);
        } else {
            startAssistActivity(bundle, componentName);
        }
    }

    private void startAssistActivity(Bundle bundle, ComponentName componentName) {
        final Intent assistIntent;
        if (!this.mDeviceProvisionedController.isDeviceProvisioned()) {
            return;
        }
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).animateCollapsePanels(3);
        boolean z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, -2) != 0;
        SearchManager searchManager = (SearchManager) this.mContext.getSystemService("search");
        if (searchManager == null || (assistIntent = searchManager.getAssistIntent(z)) == null) {
            return;
        }
        assistIntent.setComponent(componentName);
        assistIntent.putExtras(bundle);
        if (z) {
            showDisclosure();
        }
        try {
            final ActivityOptions activityOptionsMakeCustomAnimation = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.search_launch_enter, R.anim.search_launch_exit);
            assistIntent.addFlags(268435456);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    AssistManager.this.mContext.startActivityAsUser(assistIntent, activityOptionsMakeCustomAnimation.toBundle(), new UserHandle(-2));
                }
            });
        } catch (ActivityNotFoundException e) {
            Log.w("AssistManager", "Activity not found for " + assistIntent.getAction());
        }
    }

    private void startVoiceInteractor(Bundle bundle) {
        this.mAssistUtils.showSessionForActiveService(bundle, 4, this.mShowCallback, (IBinder) null);
    }

    public void launchVoiceAssistFromKeyguard() {
        this.mAssistUtils.launchVoiceAssistFromKeyguard();
    }

    public boolean canVoiceAssistBeLaunchedFromKeyguard() {
        return this.mAssistUtils.activeServiceSupportsLaunchFromKeyguard();
    }

    public ComponentName getVoiceInteractorComponentName() {
        return this.mAssistUtils.getActiveServiceComponentName();
    }

    private boolean isVoiceSessionRunning() {
        return this.mAssistUtils.isSessionRunning();
    }

    private void maybeSwapSearchIcon(ComponentName componentName, boolean z) {
        replaceDrawable(this.mView.getOrb().getLogo(), componentName, "com.android.systemui.action_assist_icon", z);
    }

    public void replaceDrawable(ImageView imageView, ComponentName componentName, String str, boolean z) {
        Bundle bundle;
        int i;
        if (componentName != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                if (z) {
                    bundle = packageManager.getServiceInfo(componentName, 128).metaData;
                } else {
                    bundle = packageManager.getActivityInfo(componentName, 128).metaData;
                }
                if (bundle != null && (i = bundle.getInt(str)) != 0) {
                    imageView.setImageDrawable(packageManager.getResourcesForApplication(componentName.getPackageName()).getDrawable(i));
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.v("AssistManager", "Assistant component " + componentName.flattenToShortString() + " not found");
            } catch (Resources.NotFoundException e2) {
                Log.w("AssistManager", "Failed to swap drawable from " + componentName.flattenToShortString(), e2);
            }
        }
        imageView.setImageDrawable(null);
    }

    private ComponentName getAssistInfo() {
        return this.mAssistUtils.getAssistComponentForUser(KeyguardUpdateMonitor.getCurrentUser());
    }

    public void showDisclosure() {
        this.mAssistDisclosure.postShow();
    }

    public void onLockscreenShown() {
        this.mAssistUtils.onLockscreenShown();
    }
}
