package android.content.pm;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IShortcutService;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.annotations.VisibleForTesting;
import java.util.List;

public class ShortcutManager {
    private static final String TAG = "ShortcutManager";
    private final Context mContext;
    private final IShortcutService mService;

    public ShortcutManager(Context context, IShortcutService iShortcutService) {
        this.mContext = context;
        this.mService = iShortcutService;
    }

    public ShortcutManager(Context context) {
        this(context, IShortcutService.Stub.asInterface(ServiceManager.getService("shortcut")));
    }

    public boolean setDynamicShortcuts(List<ShortcutInfo> list) {
        try {
            return this.mService.setDynamicShortcuts(this.mContext.getPackageName(), new ParceledListSlice(list), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ShortcutInfo> getDynamicShortcuts() {
        try {
            return this.mService.getDynamicShortcuts(this.mContext.getPackageName(), injectMyUserId()).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ShortcutInfo> getManifestShortcuts() {
        try {
            return this.mService.getManifestShortcuts(this.mContext.getPackageName(), injectMyUserId()).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean addDynamicShortcuts(List<ShortcutInfo> list) {
        try {
            return this.mService.addDynamicShortcuts(this.mContext.getPackageName(), new ParceledListSlice(list), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeDynamicShortcuts(List<String> list) {
        try {
            this.mService.removeDynamicShortcuts(this.mContext.getPackageName(), list, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeAllDynamicShortcuts() {
        try {
            this.mService.removeAllDynamicShortcuts(this.mContext.getPackageName(), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ShortcutInfo> getPinnedShortcuts() {
        try {
            return this.mService.getPinnedShortcuts(this.mContext.getPackageName(), injectMyUserId()).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean updateShortcuts(List<ShortcutInfo> list) {
        try {
            return this.mService.updateShortcuts(this.mContext.getPackageName(), new ParceledListSlice(list), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableShortcuts(List<String> list) {
        try {
            this.mService.disableShortcuts(this.mContext.getPackageName(), list, null, 0, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableShortcuts(List<String> list, int i) {
        try {
            this.mService.disableShortcuts(this.mContext.getPackageName(), list, null, i, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableShortcuts(List<String> list, String str) {
        disableShortcuts(list, (CharSequence) str);
    }

    public void disableShortcuts(List<String> list, CharSequence charSequence) {
        try {
            this.mService.disableShortcuts(this.mContext.getPackageName(), list, charSequence, 0, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void enableShortcuts(List<String> list) {
        try {
            this.mService.enableShortcuts(this.mContext.getPackageName(), list, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getMaxShortcutCountForActivity() {
        return getMaxShortcutCountPerActivity();
    }

    public int getMaxShortcutCountPerActivity() {
        try {
            return this.mService.getMaxShortcutCountPerActivity(this.mContext.getPackageName(), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getRemainingCallCount() {
        try {
            return this.mService.getRemainingCallCount(this.mContext.getPackageName(), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getRateLimitResetTime() {
        try {
            return this.mService.getRateLimitResetTime(this.mContext.getPackageName(), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isRateLimitingActive() {
        try {
            return this.mService.getRemainingCallCount(this.mContext.getPackageName(), injectMyUserId()) == 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getIconMaxWidth() {
        try {
            return this.mService.getIconMaxDimensions(this.mContext.getPackageName(), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getIconMaxHeight() {
        try {
            return this.mService.getIconMaxDimensions(this.mContext.getPackageName(), injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reportShortcutUsed(String str) {
        try {
            this.mService.reportShortcutUsed(this.mContext.getPackageName(), str, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isRequestPinShortcutSupported() {
        try {
            return this.mService.isRequestPinItemSupported(injectMyUserId(), 1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean requestPinShortcut(ShortcutInfo shortcutInfo, IntentSender intentSender) {
        try {
            return this.mService.requestPinShortcut(this.mContext.getPackageName(), shortcutInfo, intentSender, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Intent createShortcutResultIntent(ShortcutInfo shortcutInfo) {
        try {
            return this.mService.createShortcutResultIntent(this.mContext.getPackageName(), shortcutInfo, injectMyUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onApplicationActive(String str, int i) {
        try {
            this.mService.onApplicationActive(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @VisibleForTesting
    protected int injectMyUserId() {
        return this.mContext.getUserId();
    }
}
