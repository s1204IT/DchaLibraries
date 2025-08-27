package android.os;

import android.content.Context;
import android.graphics.Point;
import android.os.IBenesseExtensionService;
import android.view.IWindowManager;
import java.io.File;

/* loaded from: a04br3-02.05.000-framework.jar:android/os/BenesseExtension.class */
public class BenesseExtension {
    static IBenesseExtensionService mBenesseExtensionService;
    static IWindowManager mWindowManager;
    public static final File IGNORE_DCHA_COMPLETED_FILE = new File("/factory/ignore_dcha_completed");
    public static final File COUNT_DCHA_COMPLETED_FILE = new File("/factory/count_dcha_completed");

    BenesseExtension() {
    }

    public static boolean checkPassword(String str) {
        if (getBenesseExtensionService() == null) {
            return false;
        }
        try {
            return mBenesseExtensionService.checkPassword(str);
        } catch (RemoteException e) {
            return false;
        }
    }

    public static boolean checkUsbCam() {
        if (getBenesseExtensionService() == null) {
            return false;
        }
        try {
            return mBenesseExtensionService.checkUsbCam();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static Point getBaseDisplaySize() {
        IWindowManager windowManager = getWindowManager();
        if (windowManager == null) {
            return null;
        }
        try {
            Point point = new Point();
            windowManager.getBaseDisplaySize(0, point);
            return point;
        } catch (RemoteException e) {
            return null;
        }
    }

    static IBenesseExtensionService getBenesseExtensionService() {
        if (mBenesseExtensionService == null) {
            mBenesseExtensionService = IBenesseExtensionService.Stub.asInterface(ServiceManager.getService("benesse_extension"));
        }
        return mBenesseExtensionService;
    }

    public static int getDchaState() {
        if (getBenesseExtensionService() == null) {
            return 0;
        }
        try {
            return mBenesseExtensionService.getDchaState();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public static Point getInitialDisplaySize() {
        IWindowManager windowManager = getWindowManager();
        if (windowManager == null) {
            return null;
        }
        try {
            Point point = new Point();
            windowManager.getInitialDisplaySize(0, point);
            return point;
        } catch (RemoteException e) {
            return null;
        }
    }

    public static Point getLcdSize() {
        return getInitialDisplaySize();
    }

    public static String getString(String str) {
        if (getBenesseExtensionService() == null) {
            return null;
        }
        try {
            return mBenesseExtensionService.getString(str);
        } catch (RemoteException e) {
            return null;
        }
    }

    static IWindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.checkService(Context.WINDOW_SERVICE));
        }
        return mWindowManager;
    }

    public static void setDchaState(int i) {
        if (getBenesseExtensionService() == null) {
            return;
        }
        try {
            mBenesseExtensionService.setDchaState(i);
        } catch (RemoteException e) {
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:50:0x001d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static boolean setForcedDisplaySize(int i, int i2) {
        boolean zEquals = false;
        IWindowManager windowManager = getWindowManager();
        int i3 = i;
        int i4 = i2;
        if (windowManager != null) {
            if (i >= 0) {
                i3 = i;
                int i5 = i2;
                if (i2 < 0) {
                    try {
                        Point initialDisplaySize = getInitialDisplaySize();
                        if (initialDisplaySize == null) {
                            return false;
                        }
                        i3 = initialDisplaySize.x;
                        i5 = initialDisplaySize.y;
                    } catch (RemoteException e) {
                        return false;
                    }
                }
                windowManager.setForcedDisplaySize(0, i3, i5);
                i4 = i5;
            }
        }
        Point baseDisplaySize = getBaseDisplaySize();
        if (baseDisplaySize != null) {
            zEquals = baseDisplaySize.equals(i3, i4);
        }
        return zEquals;
    }
}