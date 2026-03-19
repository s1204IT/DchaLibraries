package java.util.stream;

import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.util.logging.PlatformLogger;

final class Tripwire {
    static final boolean ENABLED = ((Boolean) AccessController.doPrivileged(new PrivilegedAction() {
        @Override
        public final Object run() {
            return Boolean.valueOf(Boolean.getBoolean(Tripwire.TRIPWIRE_PROPERTY));
        }
    })).booleanValue();
    private static final String TRIPWIRE_PROPERTY = "org.openjdk.java.util.stream.tripwire";

    private Tripwire() {
    }

    static void trip(Class<?> cls, String str) {
        PlatformLogger.getLogger(cls.getName()).warning(str, cls.getName());
    }
}
