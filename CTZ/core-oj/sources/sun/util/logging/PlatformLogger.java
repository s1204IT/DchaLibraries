package sun.util.logging;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PlatformLogger {
    private static final int ALL = Integer.MIN_VALUE;
    private static final int CONFIG = 700;
    private static final int FINE = 500;
    private static final int FINER = 400;
    private static final int FINEST = 300;
    private static final int INFO = 800;
    private static final int OFF = Integer.MAX_VALUE;
    private static final int SEVERE = 1000;
    private static final int WARNING = 900;
    private volatile JavaLoggerProxy javaLoggerProxy;
    private volatile LoggerProxy loggerProxy;
    private static final Level DEFAULT_LEVEL = Level.INFO;
    private static boolean loggingEnabled = ((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return Boolean.valueOf((System.getProperty("java.util.logging.config.class") == null && System.getProperty("java.util.logging.config.file") == null) ? false : true);
        }
    })).booleanValue();
    private static Map<String, WeakReference<PlatformLogger>> loggers = new HashMap();

    public enum Level {
        ALL,
        FINEST,
        FINER,
        FINE,
        CONFIG,
        INFO,
        WARNING,
        SEVERE,
        OFF;

        private static final int[] LEVEL_VALUES = {Integer.MIN_VALUE, 300, 400, 500, PlatformLogger.CONFIG, PlatformLogger.INFO, PlatformLogger.WARNING, 1000, Integer.MAX_VALUE};
        Object javaLevel;

        public int intValue() {
            return LEVEL_VALUES[ordinal()];
        }

        static Level valueOf(int i) {
            if (i == Integer.MIN_VALUE) {
                return ALL;
            }
            if (i == 300) {
                return FINEST;
            }
            if (i == 400) {
                return FINER;
            }
            if (i == 500) {
                return FINE;
            }
            if (i == PlatformLogger.CONFIG) {
                return CONFIG;
            }
            if (i == PlatformLogger.INFO) {
                return INFO;
            }
            if (i == PlatformLogger.WARNING) {
                return WARNING;
            }
            if (i == 1000) {
                return SEVERE;
            }
            if (i == Integer.MAX_VALUE) {
                return OFF;
            }
            int iBinarySearch = Arrays.binarySearch(LEVEL_VALUES, 0, LEVEL_VALUES.length - 2, i);
            Level[] levelArrValues = values();
            if (iBinarySearch < 0) {
                iBinarySearch = (-iBinarySearch) - 1;
            }
            return levelArrValues[iBinarySearch];
        }
    }

    public static synchronized PlatformLogger getLogger(String str) {
        PlatformLogger platformLogger;
        platformLogger = null;
        WeakReference<PlatformLogger> weakReference = loggers.get(str);
        if (weakReference != null) {
            platformLogger = weakReference.get();
        }
        if (platformLogger == null) {
            platformLogger = new PlatformLogger(str);
            loggers.put(str, new WeakReference<>(platformLogger));
        }
        return platformLogger;
    }

    public static synchronized void redirectPlatformLoggers() {
        if (!loggingEnabled && LoggingSupport.isAvailable()) {
            loggingEnabled = true;
            Iterator<Map.Entry<String, WeakReference<PlatformLogger>>> it = loggers.entrySet().iterator();
            while (it.hasNext()) {
                PlatformLogger platformLogger = it.next().getValue().get();
                if (platformLogger != null) {
                    platformLogger.redirectToJavaLoggerProxy();
                }
            }
        }
    }

    private void redirectToJavaLoggerProxy() {
        DefaultLoggerProxy defaultLoggerProxy = (DefaultLoggerProxy) DefaultLoggerProxy.class.cast(this.loggerProxy);
        JavaLoggerProxy javaLoggerProxy = new JavaLoggerProxy(defaultLoggerProxy.name, defaultLoggerProxy.level);
        this.javaLoggerProxy = javaLoggerProxy;
        this.loggerProxy = javaLoggerProxy;
    }

    private PlatformLogger(String str) {
        if (loggingEnabled) {
            JavaLoggerProxy javaLoggerProxy = new JavaLoggerProxy(str);
            this.javaLoggerProxy = javaLoggerProxy;
            this.loggerProxy = javaLoggerProxy;
            return;
        }
        this.loggerProxy = new DefaultLoggerProxy(str);
    }

    public boolean isEnabled() {
        return this.loggerProxy.isEnabled();
    }

    public String getName() {
        return this.loggerProxy.name;
    }

    public boolean isLoggable(Level level) {
        if (level == null) {
            throw new NullPointerException();
        }
        JavaLoggerProxy javaLoggerProxy = this.javaLoggerProxy;
        return javaLoggerProxy != null ? javaLoggerProxy.isLoggable(level) : this.loggerProxy.isLoggable(level);
    }

    public Level level() {
        return this.loggerProxy.getLevel();
    }

    public void setLevel(Level level) {
        this.loggerProxy.setLevel(level);
    }

    public void severe(String str) {
        this.loggerProxy.doLog(Level.SEVERE, str);
    }

    public void severe(String str, Throwable th) {
        this.loggerProxy.doLog(Level.SEVERE, str, th);
    }

    public void severe(String str, Object... objArr) {
        this.loggerProxy.doLog(Level.SEVERE, str, objArr);
    }

    public void warning(String str) {
        this.loggerProxy.doLog(Level.WARNING, str);
    }

    public void warning(String str, Throwable th) {
        this.loggerProxy.doLog(Level.WARNING, str, th);
    }

    public void warning(String str, Object... objArr) {
        this.loggerProxy.doLog(Level.WARNING, str, objArr);
    }

    public void info(String str) {
        this.loggerProxy.doLog(Level.INFO, str);
    }

    public void info(String str, Throwable th) {
        this.loggerProxy.doLog(Level.INFO, str, th);
    }

    public void info(String str, Object... objArr) {
        this.loggerProxy.doLog(Level.INFO, str, objArr);
    }

    public void config(String str) {
        this.loggerProxy.doLog(Level.CONFIG, str);
    }

    public void config(String str, Throwable th) {
        this.loggerProxy.doLog(Level.CONFIG, str, th);
    }

    public void config(String str, Object... objArr) {
        this.loggerProxy.doLog(Level.CONFIG, str, objArr);
    }

    public void fine(String str) {
        this.loggerProxy.doLog(Level.FINE, str);
    }

    public void fine(String str, Throwable th) {
        this.loggerProxy.doLog(Level.FINE, str, th);
    }

    public void fine(String str, Object... objArr) {
        this.loggerProxy.doLog(Level.FINE, str, objArr);
    }

    public void finer(String str) {
        this.loggerProxy.doLog(Level.FINER, str);
    }

    public void finer(String str, Throwable th) {
        this.loggerProxy.doLog(Level.FINER, str, th);
    }

    public void finer(String str, Object... objArr) {
        this.loggerProxy.doLog(Level.FINER, str, objArr);
    }

    public void finest(String str) {
        this.loggerProxy.doLog(Level.FINEST, str);
    }

    public void finest(String str, Throwable th) {
        this.loggerProxy.doLog(Level.FINEST, str, th);
    }

    public void finest(String str, Object... objArr) {
        this.loggerProxy.doLog(Level.FINEST, str, objArr);
    }

    private static abstract class LoggerProxy {
        final String name;

        abstract void doLog(Level level, String str);

        abstract void doLog(Level level, String str, Throwable th);

        abstract void doLog(Level level, String str, Object... objArr);

        abstract Level getLevel();

        abstract boolean isEnabled();

        abstract boolean isLoggable(Level level);

        abstract void setLevel(Level level);

        protected LoggerProxy(String str) {
            this.name = str;
        }
    }

    private static final class DefaultLoggerProxy extends LoggerProxy {
        private static final String formatString = LoggingSupport.getSimpleFormat(false);
        private Date date;
        volatile Level effectiveLevel;
        volatile Level level;

        private static PrintStream outputStream() {
            return System.err;
        }

        DefaultLoggerProxy(String str) {
            super(str);
            this.date = new Date();
            this.effectiveLevel = deriveEffectiveLevel(null);
            this.level = null;
        }

        @Override
        boolean isEnabled() {
            return this.effectiveLevel != Level.OFF;
        }

        @Override
        Level getLevel() {
            return this.level;
        }

        @Override
        void setLevel(Level level) {
            if (this.level != level) {
                this.level = level;
                this.effectiveLevel = deriveEffectiveLevel(level);
            }
        }

        @Override
        void doLog(Level level, String str) {
            if (isLoggable(level)) {
                outputStream().print(format(level, str, null));
            }
        }

        @Override
        void doLog(Level level, String str, Throwable th) {
            if (isLoggable(level)) {
                outputStream().print(format(level, str, th));
            }
        }

        @Override
        void doLog(Level level, String str, Object... objArr) {
            if (isLoggable(level)) {
                outputStream().print(format(level, formatMessage(str, objArr), null));
            }
        }

        @Override
        boolean isLoggable(Level level) {
            Level level2 = this.effectiveLevel;
            return level.intValue() >= level2.intValue() && level2 != Level.OFF;
        }

        private Level deriveEffectiveLevel(Level level) {
            return level == null ? PlatformLogger.DEFAULT_LEVEL : level;
        }

        private String formatMessage(String str, Object... objArr) {
            if (objArr != null) {
                try {
                    if (objArr.length != 0) {
                        if (str.indexOf("{0") < 0 && str.indexOf("{1") < 0 && str.indexOf("{2") < 0 && str.indexOf("{3") < 0) {
                            return str;
                        }
                        return MessageFormat.format(str, objArr);
                    }
                } catch (Exception e) {
                    return str;
                }
            }
            return str;
        }

        private synchronized String format(Level level, String str, Throwable th) {
            String string;
            this.date.setTime(System.currentTimeMillis());
            string = "";
            if (th != null) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                printWriter.println();
                th.printStackTrace(printWriter);
                printWriter.close();
                string = stringWriter.toString();
            }
            return String.format(formatString, this.date, getCallerInfo(), this.name, level.name(), str, string);
        }

        private String getCallerInfo() {
            String str;
            String methodName;
            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            int length = stackTrace.length;
            boolean z = true;
            int i = 0;
            while (true) {
                str = null;
                if (i < length) {
                    StackTraceElement stackTraceElement = stackTrace[i];
                    String className = stackTraceElement.getClassName();
                    if (z) {
                        if (className.equals("sun.util.logging.PlatformLogger")) {
                            z = false;
                        }
                    } else if (!className.equals("sun.util.logging.PlatformLogger")) {
                        methodName = stackTraceElement.getMethodName();
                        str = className;
                        break;
                    }
                    i++;
                } else {
                    methodName = null;
                    break;
                }
            }
            if (str != null) {
                return str + " " + methodName;
            }
            return this.name;
        }
    }

    private static final class JavaLoggerProxy extends LoggerProxy {
        private final Object javaLogger;

        static {
            for (Level level : Level.values()) {
                level.javaLevel = LoggingSupport.parseLevel(level.name());
            }
        }

        JavaLoggerProxy(String str) {
            this(str, null);
        }

        JavaLoggerProxy(String str, Level level) {
            super(str);
            this.javaLogger = LoggingSupport.getLogger(str);
            if (level != null) {
                LoggingSupport.setLevel(this.javaLogger, level.javaLevel);
            }
        }

        @Override
        void doLog(Level level, String str) {
            LoggingSupport.log(this.javaLogger, level.javaLevel, str);
        }

        @Override
        void doLog(Level level, String str, Throwable th) {
            LoggingSupport.log(this.javaLogger, level.javaLevel, str, th);
        }

        @Override
        void doLog(Level level, String str, Object... objArr) {
            if (!isLoggable(level)) {
                return;
            }
            int length = objArr != null ? objArr.length : 0;
            String[] strArr = new String[length];
            for (int i = 0; i < length; i++) {
                strArr[i] = String.valueOf(objArr[i]);
            }
            LoggingSupport.log(this.javaLogger, level.javaLevel, str, strArr);
        }

        @Override
        boolean isEnabled() {
            return LoggingSupport.isLoggable(this.javaLogger, Level.OFF.javaLevel);
        }

        @Override
        Level getLevel() {
            Object level = LoggingSupport.getLevel(this.javaLogger);
            if (level == null) {
                return null;
            }
            try {
                return Level.valueOf(LoggingSupport.getLevelName(level));
            } catch (IllegalArgumentException e) {
                return Level.valueOf(LoggingSupport.getLevelValue(level));
            }
        }

        @Override
        void setLevel(Level level) {
            LoggingSupport.setLevel(this.javaLogger, level == null ? null : level.javaLevel);
        }

        @Override
        boolean isLoggable(Level level) {
            return LoggingSupport.isLoggable(this.javaLogger, level.javaLevel);
        }
    }
}
