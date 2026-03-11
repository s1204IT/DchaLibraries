package com.android.settings.development;

import android.content.Context;
import android.content.pm.IShortcutService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ShortcutManagerThrottlingPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin {
    private final IShortcutService mShortcutService;

    public ShortcutManagerThrottlingPreferenceController(Context context) {
        super(context);
        this.mShortcutService = getShortCutService();
    }

    @Override
    public String getPreferenceKey() {
        return "reset_shortcut_manager_throttling";
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals("reset_shortcut_manager_throttling", preference.getKey())) {
            return false;
        }
        resetShortcutManagerThrottling();
        return true;
    }

    private void resetShortcutManagerThrottling() {
        if (this.mShortcutService == null) {
            return;
        }
        try {
            this.mShortcutService.resetThrottling();
            Toast.makeText(this.mContext, R.string.reset_shortcut_manager_throttling_complete, 0).show();
        } catch (RemoteException e) {
            Log.e("ShortcutMgrPrefCtrl", "Failed to reset rate limiting", e);
        }
    }

    private IShortcutService getShortCutService() {
        try {
            return IShortcutService.Stub.asInterface(ServiceManager.getService("shortcut"));
        } catch (VerifyError e) {
            return null;
        }
    }
}
