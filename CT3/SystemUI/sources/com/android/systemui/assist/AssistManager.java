package com.android.systemui.assist;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
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
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

public class AssistManager {
    private final AssistDisclosure mAssistDisclosure;
    private final AssistUtils mAssistUtils;
    private final BaseStatusBar mBar;
    private final Context mContext;
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

    public AssistManager(BaseStatusBar bar, Context context) {
        this.mContext = context;
        this.mBar = bar;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAssistUtils = new AssistUtils(context);
        this.mAssistDisclosure = new AssistDisclosure(context, new Handler());
    }

    public void onConfigurationChanged() {
        boolean visible = false;
        if (this.mView != null) {
            visible = this.mView.isShowing();
            this.mWindowManager.removeView(this.mView);
        }
        this.mView = (AssistOrbContainer) LayoutInflater.from(this.mContext).inflate(R.layout.assist_orb, (ViewGroup) null);
        this.mView.setVisibility(8);
        this.mView.setSystemUiVisibility(1792);
        WindowManager.LayoutParams lp = getLayoutParams();
        this.mWindowManager.addView(this.mView, lp);
        if (!visible) {
            return;
        }
        this.mView.show(true, false);
    }

    public void startAssist(Bundle args) {
        long j;
        ComponentName assistComponent = getAssistInfo();
        if (assistComponent == null) {
            return;
        }
        boolean isService = assistComponent.equals(getVoiceInteractorComponentName());
        if (!isService || !isVoiceSessionRunning()) {
            showOrb(assistComponent, isService);
            AssistOrbContainer assistOrbContainer = this.mView;
            Runnable runnable = this.mHideRunnable;
            if (isService) {
                j = 2500;
            } else {
                j = 1000;
            }
            assistOrbContainer.postDelayed(runnable, j);
        }
        startAssistInternal(args, assistComponent, isService);
    }

    public void hideAssist() {
        this.mAssistUtils.hideCurrentSession();
    }

    private WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(R.dimen.assist_orb_scrim_height), 2033, 280, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.gravity = 8388691;
        lp.setTitle("AssistPreviewPanel");
        lp.softInputMode = 49;
        return lp;
    }

    private void showOrb(ComponentName assistComponent, boolean isService) {
        maybeSwapSearchIcon(assistComponent, isService);
        this.mView.show(true, true);
    }

    private void startAssistInternal(Bundle args, ComponentName assistComponent, boolean isService) {
        if (isService) {
            startVoiceInteractor(args);
        } else {
            startAssistActivity(args, assistComponent);
        }
    }

    private void startAssistActivity(Bundle args, ComponentName assistComponent) {
        if (!this.mBar.isDeviceProvisioned()) {
            return;
        }
        this.mBar.animateCollapsePanels(3);
        boolean structureEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, -2) != 0;
        final Intent intent = ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(structureEnabled);
        if (intent == null) {
            return;
        }
        intent.setComponent(assistComponent);
        intent.putExtras(args);
        if (structureEnabled) {
            showDisclosure();
        }
        try {
            final ActivityOptions opts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.search_launch_enter, R.anim.search_launch_exit);
            intent.addFlags(268435456);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    AssistManager.this.mContext.startActivityAsUser(intent, opts.toBundle(), new UserHandle(-2));
                }
            });
        } catch (ActivityNotFoundException e) {
            Log.w("AssistManager", "Activity not found for " + intent.getAction());
        }
    }

    private void startVoiceInteractor(Bundle args) {
        this.mAssistUtils.showSessionForActiveService(args, 4, this.mShowCallback, (IBinder) null);
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

    public void destroy() {
        this.mWindowManager.removeViewImmediate(this.mView);
    }

    private void maybeSwapSearchIcon(ComponentName assistComponent, boolean isService) {
        replaceDrawable(this.mView.getOrb().getLogo(), assistComponent, "com.android.systemui.action_assist_icon", isService);
    }

    public void replaceDrawable(ImageView v, ComponentName component, String name, boolean isService) {
        Bundle metaData;
        int iconResId;
        if (component != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                if (isService) {
                    metaData = packageManager.getServiceInfo(component, 128).metaData;
                } else {
                    metaData = packageManager.getActivityInfo(component, 128).metaData;
                }
                if (metaData != null && (iconResId = metaData.getInt(name)) != 0) {
                    Resources res = packageManager.getResourcesForApplication(component.getPackageName());
                    v.setImageDrawable(res.getDrawable(iconResId));
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.v("AssistManager", "Assistant component " + component.flattenToShortString() + " not found");
            } catch (Resources.NotFoundException nfe) {
                Log.w("AssistManager", "Failed to swap drawable from " + component.flattenToShortString(), nfe);
            }
        }
        v.setImageDrawable(null);
    }

    private ComponentName getAssistInfo() {
        return this.mAssistUtils.getAssistComponentForUser(-2);
    }

    public void showDisclosure() {
        this.mAssistDisclosure.postShow();
    }

    public void onLockscreenShown() {
        this.mAssistUtils.onLockscreenShown();
    }
}
