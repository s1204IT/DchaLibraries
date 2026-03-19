package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.ZenModeConfig;
import android.util.Slog;
import android.view.View;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;

public class IntentForwarderActivity extends Activity {
    private Injector mInjector;
    public static String TAG = "IntentForwarderActivity";
    public static String FORWARD_INTENT_TO_PARENT = "com.android.internal.app.ForwardIntentToParent";
    public static String FORWARD_INTENT_TO_MANAGED_PROFILE = "com.android.internal.app.ForwardIntentToManagedProfile";

    public interface Injector {
        IPackageManager getIPackageManager();

        PackageManager getPackageManager();

        UserManager getUserManager();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        int managedProfile;
        int i;
        int launchedFromUid;
        String launchedFromPackage;
        super.onCreate(bundle);
        this.mInjector = createInjector();
        Intent intent = getIntent();
        String className = intent.getComponent().getClassName();
        if (className.equals(FORWARD_INTENT_TO_PARENT)) {
            i = R.string.forward_intent_to_owner;
            managedProfile = getProfileParent();
        } else if (className.equals(FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            i = R.string.forward_intent_to_work;
            managedProfile = getManagedProfile();
        } else {
            Slog.wtf(TAG, IntentForwarderActivity.class.getName() + " cannot be called directly");
            managedProfile = -10000;
            i = -1;
        }
        if (managedProfile == -10000) {
            finish();
            return;
        }
        int userId = getUserId();
        Intent intentCanForward = canForward(intent, managedProfile);
        if (intentCanForward != null) {
            if (Intent.ACTION_CHOOSER.equals(intentCanForward.getAction())) {
                ((Intent) intentCanForward.getParcelableExtra(Intent.EXTRA_INTENT)).prepareToLeaveUser(userId);
            } else {
                intentCanForward.prepareToLeaveUser(userId);
            }
            ResolveInfo resolveInfoResolveActivityAsUser = this.mInjector.getPackageManager().resolveActivityAsUser(intentCanForward, 65536, managedProfile);
            boolean z = resolveInfoResolveActivityAsUser == null || resolveInfoResolveActivityAsUser.activityInfo == null || !ZenModeConfig.SYSTEM_AUTHORITY.equals(resolveInfoResolveActivityAsUser.activityInfo.packageName) || !(ResolverActivity.class.getName().equals(resolveInfoResolveActivityAsUser.activityInfo.name) || ChooserActivity.class.getName().equals(resolveInfoResolveActivityAsUser.activityInfo.name));
            try {
                startActivityAsCaller(intentCanForward, null, false, managedProfile);
            } catch (RuntimeException e) {
                try {
                    launchedFromUid = ActivityManager.getService().getLaunchedFromUid(getActivityToken());
                } catch (RemoteException e2) {
                    launchedFromUid = -1;
                }
                try {
                    launchedFromPackage = ActivityManager.getService().getLaunchedFromPackage(getActivityToken());
                } catch (RemoteException e3) {
                    launchedFromPackage = "?";
                    Slog.wtf(TAG, "Unable to launch as UID " + launchedFromUid + " package " + launchedFromPackage + ", while running in " + ActivityThread.currentProcessName(), e);
                    if (z) {
                    }
                    finish();
                }
                Slog.wtf(TAG, "Unable to launch as UID " + launchedFromUid + " package " + launchedFromPackage + ", while running in " + ActivityThread.currentProcessName(), e);
            }
            if (z) {
                Toast.makeText(this, getString(i), 1).show();
            }
        } else {
            Slog.wtf(TAG, "the intent: " + intent + " cannot be forwarded from user " + userId + " to user " + managedProfile);
        }
        finish();
    }

    Intent canForward(Intent intent, int i) {
        Intent selector;
        Intent intent2 = new Intent(intent);
        intent2.addFlags(View.SCROLLBARS_OUTSIDE_INSET);
        sanitizeIntent(intent2);
        if (Intent.ACTION_CHOOSER.equals(intent2.getAction())) {
            if (intent2.hasExtra(Intent.EXTRA_INITIAL_INTENTS)) {
                Slog.wtf(TAG, "An chooser intent with extra initial intents cannot be forwarded to a different user");
                return null;
            }
            if (intent2.hasExtra(Intent.EXTRA_REPLACEMENT_EXTRAS)) {
                Slog.wtf(TAG, "A chooser intent with replacement extras cannot be forwarded to a different user");
                return null;
            }
            selector = (Intent) intent2.getParcelableExtra(Intent.EXTRA_INTENT);
            if (selector == null) {
                Slog.wtf(TAG, "Cannot forward a chooser intent with no extra android.intent.extra.INTENT");
                return null;
            }
        } else {
            selector = intent2;
        }
        if (intent2.getSelector() != null) {
            selector = intent2.getSelector();
        }
        String strResolveTypeIfNeeded = selector.resolveTypeIfNeeded(getContentResolver());
        sanitizeIntent(selector);
        try {
        } catch (RemoteException e) {
            Slog.e(TAG, "PackageManagerService is dead?");
        }
        if (this.mInjector.getIPackageManager().canForwardTo(selector, strResolveTypeIfNeeded, getUserId(), i)) {
            return intent2;
        }
        return null;
    }

    private int getManagedProfile() {
        for (UserInfo userInfo : this.mInjector.getUserManager().getProfiles(UserHandle.myUserId())) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        Slog.wtf(TAG, FORWARD_INTENT_TO_MANAGED_PROFILE + " has been called, but there is no managed profile");
        return -10000;
    }

    private int getProfileParent() {
        UserInfo profileParent = this.mInjector.getUserManager().getProfileParent(UserHandle.myUserId());
        if (profileParent == null) {
            Slog.wtf(TAG, FORWARD_INTENT_TO_PARENT + " has been called, but there is no parent");
            return -10000;
        }
        return profileParent.id;
    }

    private void sanitizeIntent(Intent intent) {
        intent.setPackage(null);
        intent.setComponent(null);
    }

    @VisibleForTesting
    protected Injector createInjector() {
        return new InjectorImpl();
    }

    private class InjectorImpl implements Injector {
        private InjectorImpl() {
        }

        @Override
        public IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        @Override
        public UserManager getUserManager() {
            return (UserManager) IntentForwarderActivity.this.getSystemService(UserManager.class);
        }

        @Override
        public PackageManager getPackageManager() {
            return IntentForwarderActivity.this.getPackageManager();
        }
    }
}
