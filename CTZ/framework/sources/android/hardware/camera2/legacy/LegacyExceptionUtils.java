package android.hardware.camera2.legacy;

import android.os.ServiceSpecificException;
import android.system.OsConstants;
import android.util.AndroidException;

public class LegacyExceptionUtils {
    public static final int NO_ERROR = 0;
    private static final String TAG = "LegacyExceptionUtils";
    public static final int PERMISSION_DENIED = -OsConstants.EPERM;
    public static final int ALREADY_EXISTS = -OsConstants.EEXIST;
    public static final int BAD_VALUE = -OsConstants.EINVAL;
    public static final int DEAD_OBJECT = -OsConstants.ENOSYS;
    public static final int INVALID_OPERATION = -OsConstants.EPIPE;
    public static final int TIMED_OUT = -OsConstants.ETIMEDOUT;

    public static class BufferQueueAbandonedException extends AndroidException {
        public BufferQueueAbandonedException() {
        }

        public BufferQueueAbandonedException(String str) {
            super(str);
        }

        public BufferQueueAbandonedException(String str, Throwable th) {
            super(str, th);
        }

        public BufferQueueAbandonedException(Exception exc) {
            super(exc);
        }
    }

    public static int throwOnError(int i) throws BufferQueueAbandonedException {
        if (i == 0) {
            return 0;
        }
        if (i == BAD_VALUE) {
            throw new BufferQueueAbandonedException();
        }
        if (i < 0) {
            throw new UnsupportedOperationException("Unknown error " + i);
        }
        return i;
    }

    public static void throwOnServiceError(int i) {
        String str;
        if (i >= 0) {
            return;
        }
        int i2 = 4;
        if (i == PERMISSION_DENIED) {
            i2 = 1;
            str = "Lacking privileges to access camera service";
        } else {
            if (i == ALREADY_EXISTS) {
                return;
            }
            if (i == BAD_VALUE) {
                i2 = 3;
                str = "Bad argument passed to camera service";
            } else if (i != DEAD_OBJECT) {
                if (i == TIMED_OUT) {
                    str = "Operation timed out in camera service";
                } else if (i == (-OsConstants.EACCES)) {
                    i2 = 6;
                    str = "Camera disabled by policy";
                } else if (i == (-OsConstants.EBUSY)) {
                    i2 = 7;
                    str = "Camera already in use";
                } else if (i == (-OsConstants.EUSERS)) {
                    i2 = 8;
                    str = "Maximum number of cameras in use";
                } else if (i == (-OsConstants.ENODEV)) {
                    str = "Camera device not available";
                } else if (i == (-OsConstants.EOPNOTSUPP)) {
                    i2 = 9;
                    str = "Deprecated camera HAL does not support this";
                } else if (i == INVALID_OPERATION) {
                    str = "Illegal state encountered in camera service.";
                } else {
                    str = "Unknown camera device error " + i;
                }
                i2 = 10;
            } else {
                str = "Camera service not available";
            }
        }
        throw new ServiceSpecificException(i2, str);
    }

    private LegacyExceptionUtils() {
        throw new AssertionError();
    }
}
