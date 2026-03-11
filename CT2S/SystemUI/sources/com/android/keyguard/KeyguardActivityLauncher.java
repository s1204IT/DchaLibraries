package com.android.keyguard;

import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import java.util.List;

public abstract class KeyguardActivityLauncher {
    private static final String TAG = KeyguardActivityLauncher.class.getSimpleName();
    private static final Intent SECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
    private static final Intent INSECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA");

    public static class CameraWidgetInfo {
        public String contextPackage;
        public int layoutId;
    }

    abstract Context getContext();

    abstract LockPatternUtils getLockPatternUtils();

    abstract void requestDismissKeyguard();

    abstract void setOnDismissAction(KeyguardHostView.OnDismissAction onDismissAction);

    public CameraWidgetInfo getCameraWidgetInfo() {
        int layoutId;
        CameraWidgetInfo info = new CameraWidgetInfo();
        Intent intent = getCameraIntent();
        PackageManager packageManager = getContext().getPackageManager();
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, getLockPatternUtils().getCurrentUser());
        if (appList.size() == 0) {
            return null;
        }
        ResolveInfo resolved = packageManager.resolveActivityAsUser(intent, 65664, getLockPatternUtils().getCurrentUser());
        if (!wouldLaunchResolverActivity(resolved, appList)) {
            if (resolved == null || resolved.activityInfo == null) {
                return null;
            }
            if (resolved.activityInfo.metaData != null && !resolved.activityInfo.metaData.isEmpty() && (layoutId = resolved.activityInfo.metaData.getInt("com.android.keyguard.layout")) != 0) {
                info.contextPackage = resolved.activityInfo.packageName;
                info.layoutId = layoutId;
                return info;
            }
            return info;
        }
        return info;
    }

    public void launchCamera(Handler worker, Runnable onSecureCameraStarted) {
        getLockPatternUtils();
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        updateMonitor.setAlternateUnlockEnabled(false);
        if (mustLaunchSecurely()) {
            if (wouldLaunchResolverActivity(SECURE_CAMERA_INTENT)) {
                launchActivity(SECURE_CAMERA_INTENT, false, false, null, null);
                return;
            } else {
                launchActivity(SECURE_CAMERA_INTENT, true, false, worker, onSecureCameraStarted);
                return;
            }
        }
        launchActivity(INSECURE_CAMERA_INTENT, false, false, null, null);
    }

    private boolean mustLaunchSecurely() {
        LockPatternUtils lockPatternUtils = getLockPatternUtils();
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        int currentUser = lockPatternUtils.getCurrentUser();
        return lockPatternUtils.isSecure() && !updateMonitor.getUserHasTrust(currentUser);
    }

    public void launchWidgetPicker(int appWidgetId) {
        Intent pickIntent = new Intent("android.appwidget.action.KEYGUARD_APPWIDGET_PICK");
        pickIntent.putExtra("appWidgetId", appWidgetId);
        pickIntent.putExtra("customSort", false);
        pickIntent.putExtra("categoryFilter", 2);
        Bundle options = new Bundle();
        options.putInt("appWidgetCategory", 2);
        pickIntent.putExtra("appWidgetOptions", options);
        pickIntent.addFlags(880803840);
        launchActivity(pickIntent, false, false, null, null);
    }

    public void launchActivity(Intent intent, boolean showsWhileLocked, boolean useDefaultAnimations, Handler worker, Runnable onStarted) {
        Context context = getContext();
        Bundle animation = useDefaultAnimations ? null : ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle();
        launchActivityWithAnimation(intent, showsWhileLocked, animation, worker, onStarted);
    }

    public void launchActivityWithAnimation(final Intent intent, boolean showsWhileLocked, final Bundle animation, final Handler worker, final Runnable onStarted) {
        getLockPatternUtils();
        intent.addFlags(872415232);
        boolean mustLaunchSecurely = mustLaunchSecurely();
        if (!mustLaunchSecurely || showsWhileLocked) {
            if (!mustLaunchSecurely) {
                dismissKeyguardOnNextActivity();
            }
            try {
                startActivityForCurrentUser(intent, animation, worker, onStarted);
                return;
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity not found for intent + " + intent.getAction());
                return;
            }
        }
        setOnDismissAction(new KeyguardHostView.OnDismissAction() {
            @Override
            public boolean onDismiss() {
                KeyguardActivityLauncher.this.dismissKeyguardOnNextActivity();
                KeyguardActivityLauncher.this.startActivityForCurrentUser(intent, animation, worker, onStarted);
                return true;
            }
        });
        requestDismissKeyguard();
    }

    public void dismissKeyguardOnNextActivity() {
        try {
            WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
        } catch (RemoteException e) {
            Log.w(TAG, "Error dismissing keyguard", e);
        }
    }

    public void startActivityForCurrentUser(final Intent intent, final Bundle options, Handler worker, final Runnable onStarted) {
        final UserHandle user = new UserHandle(-2);
        if (worker == null || onStarted == null) {
            getContext().startActivityAsUser(intent, options, user);
        } else {
            worker.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        ActivityManagerNative.getDefault().startActivityAndWait((IApplicationThread) null, (String) null, intent, intent.resolveTypeIfNeeded(KeyguardActivityLauncher.this.getContext().getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, options, user.getIdentifier());
                        try {
                            onStarted.run();
                        } catch (Throwable t) {
                            Log.w(KeyguardActivityLauncher.TAG, "Error running onStarted callback", t);
                        }
                    } catch (RemoteException e) {
                        Log.w(KeyguardActivityLauncher.TAG, "Error starting activity", e);
                    }
                }
            });
        }
    }

    private Intent getCameraIntent() {
        return mustLaunchSecurely() ? SECURE_CAMERA_INTENT : INSECURE_CAMERA_INTENT;
    }

    private boolean wouldLaunchResolverActivity(Intent intent) {
        PackageManager packageManager = getContext().getPackageManager();
        ResolveInfo resolved = packageManager.resolveActivityAsUser(intent, 65536, getLockPatternUtils().getCurrentUser());
        List<ResolveInfo> appList = packageManager.queryIntentActivitiesAsUser(intent, 65536, getLockPatternUtils().getCurrentUser());
        return wouldLaunchResolverActivity(resolved, appList);
    }

    private boolean wouldLaunchResolverActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
        for (int i = 0; i < appList.size(); i++) {
            ResolveInfo tmp = appList.get(i);
            if (tmp.activityInfo.name.equals(resolved.activityInfo.name) && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                return false;
            }
        }
        return true;
    }
}
