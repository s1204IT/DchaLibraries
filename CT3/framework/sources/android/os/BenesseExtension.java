package android.os;

import android.content.Context;
import android.graphics.Point;
import android.os.IBenesseExtensionService;
import android.view.IWindowManager;
import java.io.File;

public class BenesseExtension {
    static IBenesseExtensionService mBenesseExtensionService;
    static IWindowManager mWindowManager;
    public static final File IGNORE_DCHA_COMPLETED_FILE = new File("/factory/ignore_dcha_completed");
    public static final File COUNT_DCHA_COMPLETED_FILE = new File("/factory/count_dcha_completed");

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
        if (getBenesseExtensionService() == null) {
            return;
        }
        try {
            mBenesseExtensionService.setDchaState(state);
        } catch (RemoteException e) {
        }
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
        if (size != null) {
            return size.equals(width, height);
        }
        return false;
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

    public static String getString(String name) {
        if (getBenesseExtensionService() == null) {
            return null;
        }
        try {
            return mBenesseExtensionService.getString(name);
        } catch (RemoteException e) {
            return null;
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

    public static boolean checkPassword(String pwd) {
        if (getBenesseExtensionService() == null) {
            return false;
        }
        try {
            return mBenesseExtensionService.checkPassword(pwd);
        } catch (RemoteException e) {
            return false;
        }
    }
}
