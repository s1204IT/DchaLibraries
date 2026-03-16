package android.os;

import android.content.Context;
import android.graphics.Point;
import android.os.IBenesseExtensionService;
import android.view.IWindowManager;

public class BenesseExtension {
    static IBenesseExtensionService mBenesseExtensionService;
    static IWindowManager mWindowManager;

    BenesseExtension() {
    }

    static IBenesseExtensionService getBenesseExtensionService() {
        if (mBenesseExtensionService == null) {
            mBenesseExtensionService = IBenesseExtensionService.Stub.asInterface(ServiceManager.getService("benesse_extension"));
        }
        return mBenesseExtensionService;
    }

    static IWindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.checkService(Context.WINDOW_SERVICE));
        }
        return mWindowManager;
    }

    public static boolean setForcedDisplaySize(int width, int height) {
        IWindowManager wm = getWindowManager();
        if (wm != null) {
            if (width < 0 || height < 0) {
                try {
                    Point initialSize = getInitialDisplaySize();
                    if (initialSize == null) {
                        return false;
                    }
                    width = initialSize.x;
                    height = initialSize.y;
                } catch (RemoteException e) {
                    return false;
                }
            }
            wm.setForcedDisplaySize(0, width, height);
        }
        Point size = getBaseDisplaySize();
        return size != null && size.equals(width, height);
    }

    public static Point getLcdSize() {
        return getInitialDisplaySize();
    }

    public static Point getInitialDisplaySize() {
        IWindowManager wm = getWindowManager();
        if (wm != null) {
            try {
                Point size = new Point();
                wm.getInitialDisplaySize(0, size);
                return size;
            } catch (RemoteException e) {
            }
        }
        return null;
    }

    public static Point getBaseDisplaySize() {
        IWindowManager wm = getWindowManager();
        if (wm != null) {
            try {
                Point size = new Point();
                wm.getBaseDisplaySize(0, size);
                return size;
            } catch (RemoteException e) {
            }
        }
        return null;
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

    public static void setDchaState(int state) {
        if (getBenesseExtensionService() != null) {
            try {
                mBenesseExtensionService.setDchaState(state);
            } catch (RemoteException e) {
            }
        }
    }
}
