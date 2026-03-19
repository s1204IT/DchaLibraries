package com.android.internal.app;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.app.IVoiceInteractionManagerService;

public class AssistUtils {
    private static final String TAG = "AssistUtils";
    private final Context mContext;
    private final IVoiceInteractionManagerService mVoiceInteractionManagerService = IVoiceInteractionManagerService.Stub.asInterface(ServiceManager.getService(Context.VOICE_INTERACTION_MANAGER_SERVICE));

    public AssistUtils(Context context) {
        this.mContext = context;
    }

    public boolean showSessionForActiveService(Bundle bundle, int i, IVoiceInteractionSessionShowCallback iVoiceInteractionSessionShowCallback, IBinder iBinder) {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                return this.mVoiceInteractionManagerService.showSessionForActiveService(bundle, i, iVoiceInteractionSessionShowCallback, iBinder);
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call showSessionForActiveService", e);
            return false;
        }
    }

    public void launchVoiceAssistFromKeyguard() {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                this.mVoiceInteractionManagerService.launchVoiceAssistFromKeyguard();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call launchVoiceAssistFromKeyguard", e);
        }
    }

    public boolean activeServiceSupportsAssistGesture() {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                return this.mVoiceInteractionManagerService.activeServiceSupportsAssist();
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call activeServiceSupportsAssistGesture", e);
            return false;
        }
    }

    public boolean activeServiceSupportsLaunchFromKeyguard() {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                return this.mVoiceInteractionManagerService.activeServiceSupportsLaunchFromKeyguard();
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call activeServiceSupportsLaunchFromKeyguard", e);
            return false;
        }
    }

    public ComponentName getActiveServiceComponentName() {
        try {
            if (this.mVoiceInteractionManagerService == null) {
                return null;
            }
            return this.mVoiceInteractionManagerService.getActiveServiceComponentName();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call getActiveServiceComponentName", e);
            return null;
        }
    }

    public boolean isSessionRunning() {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                return this.mVoiceInteractionManagerService.isSessionRunning();
            }
            return false;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call isSessionRunning", e);
            return false;
        }
    }

    public void hideCurrentSession() {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                this.mVoiceInteractionManagerService.hideCurrentSession();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call hideCurrentSession", e);
        }
    }

    public void onLockscreenShown() {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                this.mVoiceInteractionManagerService.onLockscreenShown();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to call onLockscreenShown", e);
        }
    }

    public void registerVoiceInteractionSessionListener(IVoiceInteractionSessionListener iVoiceInteractionSessionListener) {
        try {
            if (this.mVoiceInteractionManagerService != null) {
                this.mVoiceInteractionManagerService.registerVoiceInteractionSessionListener(iVoiceInteractionSessionListener);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to register voice interaction listener", e);
        }
    }

    public ComponentName getAssistComponentForUser(int i) {
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), Settings.Secure.ASSISTANT, i);
        if (stringForUser != null) {
            return ComponentName.unflattenFromString(stringForUser);
        }
        String string = this.mContext.getResources().getString(R.string.config_defaultAssistantComponentName);
        if (string != null) {
            return ComponentName.unflattenFromString(string);
        }
        if (activeServiceSupportsAssistGesture()) {
            return getActiveServiceComponentName();
        }
        SearchManager searchManager = (SearchManager) this.mContext.getSystemService("search");
        if (searchManager == null) {
            return null;
        }
        ResolveInfo resolveInfoResolveActivityAsUser = this.mContext.getPackageManager().resolveActivityAsUser(searchManager.getAssistIntent(false), 65536, i);
        if (resolveInfoResolveActivityAsUser == null) {
            return null;
        }
        return new ComponentName(resolveInfoResolveActivityAsUser.activityInfo.applicationInfo.packageName, resolveInfoResolveActivityAsUser.activityInfo.name);
    }

    public static boolean isPreinstalledAssistant(Context context, ComponentName componentName) {
        if (componentName == null) {
            return false;
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(componentName.getPackageName(), 0);
            return applicationInfo.isSystemApp() || applicationInfo.isUpdatedSystemApp();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean isDisclosureEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ASSIST_DISCLOSURE_ENABLED, 0) != 0;
    }

    public static boolean shouldDisclose(Context context, ComponentName componentName) {
        return (allowDisablingAssistDisclosure(context) && !isDisclosureEnabled(context) && isPreinstalledAssistant(context, componentName)) ? false : true;
    }

    public static boolean allowDisablingAssistDisclosure(Context context) {
        return context.getResources().getBoolean(R.bool.config_allowDisablingAssistDisclosure);
    }
}
