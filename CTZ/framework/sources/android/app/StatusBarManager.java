package android.app;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBarService;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class StatusBarManager {
    public static final int CAMERA_LAUNCH_SOURCE_LIFT_TRIGGER = 2;
    public static final int CAMERA_LAUNCH_SOURCE_POWER_DOUBLE_TAP = 1;
    public static final int CAMERA_LAUNCH_SOURCE_WIGGLE = 0;
    public static final int DISABLE2_GLOBAL_ACTIONS = 8;
    public static final int DISABLE2_MASK = 31;
    public static final int DISABLE2_NONE = 0;
    public static final int DISABLE2_NOTIFICATION_SHADE = 4;
    public static final int DISABLE2_QUICK_SETTINGS = 1;
    public static final int DISABLE2_ROTATE_SUGGESTIONS = 16;
    public static final int DISABLE2_SYSTEM_ICONS = 2;
    public static final int DISABLE_BACK = 4194304;
    public static final int DISABLE_CLOCK = 8388608;
    public static final int DISABLE_EXPAND = 65536;
    public static final int DISABLE_HOME = 2097152;
    public static final int DISABLE_MASK = 67043328;

    @Deprecated
    public static final int DISABLE_NAVIGATION = 18874368;
    public static final int DISABLE_NONE = 0;
    public static final int DISABLE_NOTIFICATION_ALERTS = 262144;
    public static final int DISABLE_NOTIFICATION_ICONS = 131072;

    @Deprecated
    public static final int DISABLE_NOTIFICATION_TICKER = 524288;
    public static final int DISABLE_RECENT = 16777216;
    public static final int DISABLE_SEARCH = 33554432;
    public static final int DISABLE_SYSTEM_INFO = 1048576;
    public static final int NAVIGATION_HINT_BACK_ALT = 1;
    public static final int NAVIGATION_HINT_IME_SHOWN = 2;
    public static final int WINDOW_NAVIGATION_BAR = 2;
    public static final int WINDOW_STATE_HIDDEN = 2;
    public static final int WINDOW_STATE_HIDING = 1;
    public static final int WINDOW_STATE_SHOWING = 0;
    public static final int WINDOW_STATUS_BAR = 1;
    private Context mContext;
    private IStatusBarService mService;
    private IBinder mToken = new Binder();

    @Retention(RetentionPolicy.SOURCE)
    public @interface Disable2Flags {
    }

    StatusBarManager(Context context) {
        this.mContext = context;
    }

    private synchronized IStatusBarService getService() {
        if (this.mService == null) {
            this.mService = IStatusBarService.Stub.asInterface(ServiceManager.getService(Context.STATUS_BAR_SERVICE));
            if (this.mService == null) {
                Slog.w("StatusBarManager", "warning: no STATUS_BAR_SERVICE");
            }
        }
        return this.mService;
    }

    public void disable(int i) {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.disable(i, this.mToken, this.mContext.getPackageName());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disable2(int i) {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.disable2(i, this.mToken, this.mContext.getPackageName());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void expandNotificationsPanel() {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.expandNotificationsPanel();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void collapsePanels() {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.collapsePanels();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void expandSettingsPanel() {
        expandSettingsPanel(null);
    }

    public void expandSettingsPanel(String str) {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.expandSettingsPanel(str);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setIcon(String str, int i, int i2, String str2) {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.setIcon(str, this.mContext.getPackageName(), i, i2, str2);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeIcon(String str) {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.removeIcon(str);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setIconVisibility(String str, boolean z) {
        try {
            IStatusBarService service = getService();
            if (service != null) {
                service.setIconVisibility(str, z);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static String windowStateToString(int i) {
        return i == 1 ? "WINDOW_STATE_HIDING" : i == 2 ? "WINDOW_STATE_HIDDEN" : i == 0 ? "WINDOW_STATE_SHOWING" : "WINDOW_STATE_UNKNOWN";
    }
}
