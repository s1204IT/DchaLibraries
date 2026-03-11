package com.android.settings.dashboard;

import android.app.AutomaticZenRule;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.ims.ImsManager;
import com.android.settings.Settings;
import com.android.settingslib.drawer.Tile;
import java.util.Collection;

public class SuggestionsChecks {
    private final IWallpaperManagerCallback mCallback = new IWallpaperManagerCallback.Stub() {
        public void onWallpaperChanged() throws RemoteException {
        }
    };
    private final Context mContext;

    public SuggestionsChecks(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public boolean isSuggestionComplete(Tile suggestion) {
        String className = suggestion.intent.getComponent().getClassName();
        if (className.equals(Settings.ZenModeAutomationSuggestionActivity.class.getName())) {
            return hasEnabledZenAutoRules();
        }
        if (className.equals(Settings.WallpaperSuggestionActivity.class.getName())) {
            return hasWallpaperSet();
        }
        if (className.equals(Settings.WifiCallingSuggestionActivity.class.getName())) {
            return isWifiCallingUnavailableOrEnabled();
        }
        if (className.equals(Settings.FingerprintSuggestionActivity.class.getName())) {
            return isNotSingleFingerprintEnrolled();
        }
        if (className.equals(Settings.ScreenLockSuggestionActivity.class.getName()) || className.equals(Settings.FingerprintEnrollSuggestionActivity.class.getName())) {
            return isDeviceSecured();
        }
        return false;
    }

    private boolean isDeviceSecured() {
        KeyguardManager km = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
        return km.isKeyguardSecure();
    }

    private boolean isNotSingleFingerprintEnrolled() {
        FingerprintManager manager = (FingerprintManager) this.mContext.getSystemService(FingerprintManager.class);
        return manager == null || manager.getEnrolledFingerprints().size() != 1;
    }

    public boolean isWifiCallingUnavailableOrEnabled() {
        if (!ImsManager.isWfcEnabledByPlatform(this.mContext)) {
            return true;
        }
        if (ImsManager.isWfcEnabledByUser(this.mContext)) {
            return ImsManager.isNonTtyOrTtyOnVolteEnabled(this.mContext);
        }
        return false;
    }

    private boolean hasEnabledZenAutoRules() {
        Collection<AutomaticZenRule> zenRules = NotificationManager.from(this.mContext).getAutomaticZenRules().values();
        for (AutomaticZenRule rule : zenRules) {
            if (rule.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWallpaperSet() {
        IBinder b = ServiceManager.getService("wallpaper");
        IWallpaperManager service = IWallpaperManager.Stub.asInterface(b);
        try {
            return service.getWallpaper((IWallpaperManagerCallback) null, 1, new Bundle(), this.mContext.getUserId()) != null;
        } catch (RemoteException e) {
            return false;
        }
    }
}
