package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.notification.ZenModeConfig;
import android.util.Slog;
import android.view.View;
import android.widget.Toast;
import com.android.internal.R;
import java.util.List;

public class IntentForwarderActivity extends Activity {
    public static String TAG = "IntentForwarderActivity";
    public static String FORWARD_INTENT_TO_USER_OWNER = "com.android.internal.app.ForwardIntentToUserOwner";
    public static String FORWARD_INTENT_TO_MANAGED_PROFILE = "com.android.internal.app.ForwardIntentToManagedProfile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int userMessageId;
        int targetUserId;
        super.onCreate(savedInstanceState);
        Intent intentReceived = getIntent();
        String className = intentReceived.getComponent().getClassName();
        if (className.equals(FORWARD_INTENT_TO_USER_OWNER)) {
            userMessageId = R.string.forward_intent_to_owner;
            targetUserId = 0;
        } else if (className.equals(FORWARD_INTENT_TO_MANAGED_PROFILE)) {
            userMessageId = R.string.forward_intent_to_work;
            targetUserId = getManagedProfile();
        } else {
            Slog.wtf(TAG, IntentForwarderActivity.class.getName() + " cannot be called directly");
            userMessageId = -1;
            targetUserId = -10000;
        }
        if (targetUserId == -10000) {
            finish();
            return;
        }
        Intent newIntent = new Intent(intentReceived);
        newIntent.setComponent(null);
        newIntent.setPackage(null);
        newIntent.addFlags(View.SCROLLBARS_OUTSIDE_INSET);
        int callingUserId = getUserId();
        if (canForward(newIntent, targetUserId)) {
            if (newIntent.getAction().equals(Intent.ACTION_CHOOSER)) {
                Intent innerIntent = (Intent) newIntent.getParcelableExtra(Intent.EXTRA_INTENT);
                innerIntent.setContentUserHint(callingUserId);
            } else {
                newIntent.setContentUserHint(callingUserId);
            }
            ResolveInfo ri = getPackageManager().resolveActivityAsUser(newIntent, 65536, targetUserId);
            boolean shouldShowDisclosure = ri == null || ri.activityInfo == null || !ZenModeConfig.SYSTEM_AUTHORITY.equals(ri.activityInfo.packageName) || !(ResolverActivity.class.getName().equals(ri.activityInfo.name) || ChooserActivity.class.getName().equals(ri.activityInfo.name));
            try {
                startActivityAsCaller(newIntent, null, targetUserId);
            } catch (RuntimeException e) {
                int launchedFromUid = -1;
                String launchedFromPackage = "?";
                try {
                    launchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
                    launchedFromPackage = ActivityManagerNative.getDefault().getLaunchedFromPackage(getActivityToken());
                } catch (RemoteException e2) {
                }
                Slog.wtf(TAG, "Unable to launch as UID " + launchedFromUid + " package " + launchedFromPackage + ", while running in " + ActivityThread.currentProcessName(), e);
            }
            if (shouldShowDisclosure) {
                Toast.makeText(this, getString(userMessageId), 1).show();
            }
        } else {
            Slog.wtf(TAG, "the intent: " + newIntent + "cannot be forwarded from user " + callingUserId + " to user " + targetUserId);
        }
        finish();
    }

    boolean canForward(Intent intent, int targetUserId) {
        IPackageManager ipm = AppGlobals.getPackageManager();
        if (intent.getAction().equals(Intent.ACTION_CHOOSER)) {
            if (intent.hasExtra(Intent.EXTRA_INITIAL_INTENTS)) {
                Slog.wtf(TAG, "An chooser intent with extra initial intents cannot be forwarded to a different user");
                return false;
            }
            if (intent.hasExtra(Intent.EXTRA_REPLACEMENT_EXTRAS)) {
                Slog.wtf(TAG, "A chooser intent with replacement extras cannot be forwarded to a different user");
                return false;
            }
            intent = (Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT);
        }
        String resolvedType = intent.resolveTypeIfNeeded(getContentResolver());
        if (intent.getSelector() != null) {
            intent = intent.getSelector();
        }
        try {
            return ipm.canForwardTo(intent, resolvedType, getUserId(), targetUserId);
        } catch (RemoteException e) {
            Slog.e(TAG, "PackageManagerService is dead?");
            return false;
        }
    }

    private int getManagedProfile() {
        UserManager userManager = (UserManager) getSystemService("user");
        List<UserInfo> relatedUsers = userManager.getProfiles(0);
        for (UserInfo userInfo : relatedUsers) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        Slog.wtf(TAG, FORWARD_INTENT_TO_MANAGED_PROFILE + " has been called, but there is no managed profile");
        return -10000;
    }
}
