package sun.util.logging;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.List;

public class LoggingSupport {
    private static final String DEFAULT_FORMAT = "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%6$s%n";
    private static final String FORMAT_PROP_KEY = "java.util.logging.SimpleFormatter.format";
    private static final LoggingProxy proxy = (LoggingProxy) AccessController.doPrivileged(new PrivilegedAction<LoggingProxy>() {
        @Override
        public LoggingProxy run() {
            try {
                Field declaredField = Class.forName("java.util.logging.LoggingProxyImpl", true, null).getDeclaredField("INSTANCE");
                declaredField.setAccessible(true);
                return (LoggingProxy) declaredField.get(null);
            } catch (ClassNotFoundException e) {
                return null;
            } catch (IllegalAccessException e2) {
                throw new AssertionError(e2);
            } catch (NoSuchFieldException e3) {
                throw new AssertionError(e3);
            }
        }
    });

    private LoggingSupport() {
    }

    public static boolean isAvailable() {
        return proxy != null;
    }

    private static void ensureAvailable() {
        if (proxy == null) {
            throw new AssertionError((Object) "Should not here");
        }
    }

    public static List<String> getLoggerNames() {
        ensureAvailable();
        return proxy.getLoggerNames();
    }

    public static String getLoggerLevel(String str) {
        ensureAvailable();
        return proxy.getLoggerLevel(str);
    }

    public static void setLoggerLevel(String str, String str2) {
        ensureAvailable();
        proxy.setLoggerLevel(str, str2);
    }

    public static String getParentLoggerName(String str) {
        ensureAvailable();
        return proxy.getParentLoggerName(str);
    }

    public static Object getLogger(String str) {
        ensureAvailable();
        return proxy.getLogger(str);
    }

    public static Object getLevel(Object obj) {
        ensureAvailable();
        return proxy.getLevel(obj);
    }

    public static void setLevel(Object obj, Object obj2) {
        ensureAvailable();
        proxy.setLevel(obj, obj2);
    }

    public static boolean isLoggable(Object obj, Object obj2) {
        ensureAvailable();
        return proxy.isLoggable(obj, obj2);
    }

    public static void log(Object obj, Object obj2, String str) {
        ensureAvailable();
        proxy.log(obj, obj2, str);
    }

    public static void log(Object obj, Object obj2, String str, Throwable th) {
        ensureAvailable();
        proxy.log(obj, obj2, str, th);
    }

    public static void log(Object obj, Object obj2, String str, Object... objArr) {
        ensureAvailable();
        proxy.log(obj, obj2, str, objArr);
    }

    public static Object parseLevel(String str) {
        ensureAvailable();
        return proxy.parseLevel(str);
    }

    public static String getLevelName(Object obj) {
        ensureAvailable();
        return proxy.getLevelName(obj);
    }

    public static int getLevelValue(Object obj) {
        ensureAvailable();
        return proxy.getLevelValue(obj);
    }

    public static String getSimpleFormat() {
        return getSimpleFormat(true);
    }

    static String getSimpleFormat(boolean z) {
        String property = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(LoggingSupport.FORMAT_PROP_KEY);
            }
        });
        if (z && proxy != null && property == null) {
            property = proxy.getProperty(FORMAT_PROP_KEY);
        }
        if (property != null) {
            try {
                String.format(property, new Date(), "", "", "", "", "");
                return property;
            } catch (IllegalArgumentException e) {
                return DEFAULT_FORMAT;
            }
        }
        return DEFAULT_FORMAT;
    }
}
