package android.hardware.camera2.legacy;

import android.util.AndroidException;

public class LegacyExceptionUtils {
    private static final String TAG = "LegacyExceptionUtils";

    public static class BufferQueueAbandonedException extends AndroidException {
        public BufferQueueAbandonedException() {
        }

        public BufferQueueAbandonedException(String name) {
            super(name);
        }

        public BufferQueueAbandonedException(String name, Throwable cause) {
            super(name, cause);
        }

        public BufferQueueAbandonedException(Exception cause) {
            super(cause);
        }
    }

    public static int throwOnError(int errorFlag) throws BufferQueueAbandonedException {
        switch (errorFlag) {
            case -19:
                throw new BufferQueueAbandonedException();
            case 0:
                return 0;
            default:
                if (errorFlag < 0) {
                    throw new UnsupportedOperationException("Unknown error " + errorFlag);
                }
                return errorFlag;
        }
    }

    private LegacyExceptionUtils() {
        throw new AssertionError();
    }
}
