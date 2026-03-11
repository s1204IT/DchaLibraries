package android.os;

import android.graphics.Point;
import android.os.IBenesseExtensionService;
import android.view.IWindowManager;

public class BenesseExtension {
    static IBenesseExtensionService mBenesseExtensionService;
    static IWindowManager mWindowManager;

    BenesseExtension() {
    }

    public static Point getBaseDisplaySize() {
        IWindowManager windowManager = getWindowManager();
        if (windowManager != null) {
            try {
                Point point = new Point();
                windowManager.getBaseDisplaySize(0, point);
                return point;
            } catch (RemoteException e) {
            }
        }
        return null;
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
        if (windowManager != null) {
            try {
                Point point = new Point();
                windowManager.getInitialDisplaySize(0, point);
                return point;
            } catch (RemoteException e) {
            }
        }
        return null;
    }

    public static Point getLcdSize() {
        return getInitialDisplaySize();
    }

    static IWindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.checkService("window"));
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

    public static boolean setForcedDisplaySize(int i, int i2) {
        IWindowManager windowManager = getWindowManager();
        if (windowManager != null) {
            if (i < 0 || i2 < 0) {
                try {
                    Point initialDisplaySize = getInitialDisplaySize();
                    if (initialDisplaySize == null) {
                        return false;
                    }
                    i = initialDisplaySize.x;
                    i2 = initialDisplaySize.y;
                } catch (RemoteException e) {
                    return false;
                }
            }
            windowManager.setForcedDisplaySize(0, i, i2);
        }
        Point baseDisplaySize = getBaseDisplaySize();
        return baseDisplaySize != null && baseDisplaySize.equals(i, i2);
    }
}
