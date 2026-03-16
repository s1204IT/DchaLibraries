package android.hardware.camera2.utils;

import android.hardware.camera2.utils.Decorator;
import android.os.DeadObjectException;
import android.os.RemoteException;
import java.lang.reflect.Method;

public class CameraBinderDecorator {
    public static final int ALREADY_EXISTS = -17;
    public static final int BAD_VALUE = -22;
    public static final int DEAD_OBJECT = -32;
    public static final int EACCES = -13;
    public static final int EBUSY = -16;
    public static final int ENODEV = -19;
    public static final int EOPNOTSUPP = -95;
    public static final int EUSERS = -87;
    public static final int INVALID_OPERATION = -38;
    public static final int NO_ERROR = 0;
    public static final int PERMISSION_DENIED = -1;
    public static final int TIMED_OUT = -110;

    static class CameraBinderDecoratorListener implements Decorator.DecoratorListener {
        CameraBinderDecoratorListener() {
        }

        @Override
        public void onBeforeInvocation(Method m, Object[] args) {
        }

        @Override
        public void onAfterInvocation(Method m, Object[] args, Object result) {
            if (m.getReturnType() == Integer.TYPE) {
                int returnValue = ((Integer) result).intValue();
                CameraBinderDecorator.throwOnError(returnValue);
            }
        }

        @Override
        public boolean onCatchException(Method m, Object[] args, Throwable t) {
            if (t instanceof DeadObjectException) {
                throw new CameraRuntimeException(2, "Process hosting the camera service has died unexpectedly", t);
            }
            if (t instanceof RemoteException) {
                throw new UnsupportedOperationException("An unknown RemoteException was thrown which should never happen.", t);
            }
            return false;
        }

        @Override
        public void onFinally(Method m, Object[] args) {
        }
    }

    public static void throwOnError(int errorFlag) {
        switch (errorFlag) {
            case -110:
                throw new CameraRuntimeException(3, "Operation timed out in camera service");
            case EOPNOTSUPP:
                throw new CameraRuntimeException(1000);
            case EUSERS:
                throw new CameraRuntimeException(5);
            case -38:
                throw new CameraRuntimeException(3, "Illegal state encountered in camera service.");
            case -32:
                throw new CameraRuntimeException(2);
            case -22:
                throw new IllegalArgumentException("Bad argument passed to camera service");
            case -19:
                throw new CameraRuntimeException(2);
            case -17:
            case 0:
                return;
            case -16:
                throw new CameraRuntimeException(4);
            case -13:
                throw new CameraRuntimeException(1);
            case -1:
                throw new SecurityException("Lacking privileges to access camera service");
            default:
                if (errorFlag < 0) {
                    throw new UnsupportedOperationException(String.format("Unknown error %d", Integer.valueOf(errorFlag)));
                }
                return;
        }
    }

    public static <T> T newInstance(T t) {
        return (T) Decorator.newInstance(t, new CameraBinderDecoratorListener());
    }
}
