package org.apache.commons.logging.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;

@Deprecated
public class SimpleLog implements Log, Serializable {
    protected static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS zzz";
    public static final int LOG_LEVEL_ALL = 0;
    public static final int LOG_LEVEL_DEBUG = 2;
    public static final int LOG_LEVEL_ERROR = 5;
    public static final int LOG_LEVEL_FATAL = 6;
    public static final int LOG_LEVEL_INFO = 3;
    public static final int LOG_LEVEL_OFF = 7;
    public static final int LOG_LEVEL_TRACE = 1;
    public static final int LOG_LEVEL_WARN = 4;
    protected static DateFormat dateFormatter = null;
    protected static String dateTimeFormat = null;
    protected static boolean showDateTime = false;
    protected static boolean showLogName = false;
    protected static boolean showShortName = false;
    protected static final Properties simpleLogProps = new Properties();
    protected static final String systemPrefix = "org.apache.commons.logging.simplelog.";
    protected int currentLogLevel;
    protected String logName;
    private String shortLogName = null;

    static {
        showLogName = false;
        showShortName = true;
        showDateTime = false;
        dateTimeFormat = DEFAULT_DATE_TIME_FORMAT;
        dateFormatter = null;
        InputStream in = getResourceAsStream("simplelog.properties");
        if (in != null) {
            try {
                simpleLogProps.load(in);
                in.close();
            } catch (IOException e) {
            }
        }
        showLogName = getBooleanProperty("org.apache.commons.logging.simplelog.showlogname", showLogName);
        showShortName = getBooleanProperty("org.apache.commons.logging.simplelog.showShortLogname", showShortName);
        showDateTime = getBooleanProperty("org.apache.commons.logging.simplelog.showdatetime", showDateTime);
        if (!showDateTime) {
            return;
        }
        dateTimeFormat = getStringProperty("org.apache.commons.logging.simplelog.dateTimeFormat", dateTimeFormat);
        try {
            dateFormatter = new SimpleDateFormat(dateTimeFormat);
        } catch (IllegalArgumentException e2) {
            dateTimeFormat = DEFAULT_DATE_TIME_FORMAT;
            dateFormatter = new SimpleDateFormat(dateTimeFormat);
        }
    }

    private static String getStringProperty(String name) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (SecurityException e) {
        }
        return prop == null ? simpleLogProps.getProperty(name) : prop;
    }

    private static String getStringProperty(String name, String dephault) {
        String prop = getStringProperty(name);
        return prop == null ? dephault : prop;
    }

    private static boolean getBooleanProperty(String name, boolean dephault) {
        String prop = getStringProperty(name);
        if (prop == null) {
            return dephault;
        }
        boolean dephault2 = "true".equalsIgnoreCase(prop);
        return dephault2;
    }

    public SimpleLog(String name) {
        this.logName = null;
        this.logName = name;
        setLevel(3);
        String lvl = getStringProperty("org.apache.commons.logging.simplelog.log." + this.logName);
        int i = String.valueOf(name).lastIndexOf(".");
        while (lvl == null && i > -1) {
            name = name.substring(0, i);
            lvl = getStringProperty("org.apache.commons.logging.simplelog.log." + name);
            i = String.valueOf(name).lastIndexOf(".");
        }
        lvl = lvl == null ? getStringProperty("org.apache.commons.logging.simplelog.defaultlog") : lvl;
        if ("all".equalsIgnoreCase(lvl)) {
            setLevel(0);
            return;
        }
        if ("trace".equalsIgnoreCase(lvl)) {
            setLevel(1);
            return;
        }
        if ("debug".equalsIgnoreCase(lvl)) {
            setLevel(2);
            return;
        }
        if ("info".equalsIgnoreCase(lvl)) {
            setLevel(3);
            return;
        }
        if ("warn".equalsIgnoreCase(lvl)) {
            setLevel(4);
            return;
        }
        if ("error".equalsIgnoreCase(lvl)) {
            setLevel(5);
        } else if ("fatal".equalsIgnoreCase(lvl)) {
            setLevel(6);
        } else {
            if (!"off".equalsIgnoreCase(lvl)) {
                return;
            }
            setLevel(7);
        }
    }

    public void setLevel(int currentLogLevel) {
        this.currentLogLevel = currentLogLevel;
    }

    public int getLevel() {
        return this.currentLogLevel;
    }

    protected void log(int type, Object message, Throwable t) {
        StringBuffer buf = new StringBuffer();
        if (showDateTime) {
            buf.append(dateFormatter.format(new Date()));
            buf.append(" ");
        }
        switch (type) {
            case 1:
                buf.append("[TRACE] ");
                break;
            case 2:
                buf.append("[DEBUG] ");
                break;
            case 3:
                buf.append("[INFO] ");
                break;
            case 4:
                buf.append("[WARN] ");
                break;
            case 5:
                buf.append("[ERROR] ");
                break;
            case LOG_LEVEL_FATAL:
                buf.append("[FATAL] ");
                break;
        }
        if (showShortName) {
            if (this.shortLogName == null) {
                this.shortLogName = this.logName.substring(this.logName.lastIndexOf(".") + 1);
                this.shortLogName = this.shortLogName.substring(this.shortLogName.lastIndexOf("/") + 1);
            }
            buf.append(String.valueOf(this.shortLogName)).append(" - ");
        } else if (showLogName) {
            buf.append(String.valueOf(this.logName)).append(" - ");
        }
        buf.append(String.valueOf(message));
        if (t != null) {
            buf.append(" <");
            buf.append(t.toString());
            buf.append(">");
            StringWriter sw = new StringWriter(1024);
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            buf.append(sw.toString());
        }
        write(buf);
    }

    protected void write(StringBuffer buffer) {
        System.err.println(buffer.toString());
    }

    protected boolean isLevelEnabled(int logLevel) {
        return logLevel >= this.currentLogLevel;
    }

    @Override
    public final void debug(Object message) {
        if (!isLevelEnabled(2)) {
            return;
        }
        log(2, message, null);
    }

    @Override
    public final void debug(Object message, Throwable t) {
        if (!isLevelEnabled(2)) {
            return;
        }
        log(2, message, t);
    }

    @Override
    public final void trace(Object message) {
        if (!isLevelEnabled(1)) {
            return;
        }
        log(1, message, null);
    }

    @Override
    public final void trace(Object message, Throwable t) {
        if (!isLevelEnabled(1)) {
            return;
        }
        log(1, message, t);
    }

    @Override
    public final void info(Object message) {
        if (!isLevelEnabled(3)) {
            return;
        }
        log(3, message, null);
    }

    @Override
    public final void info(Object message, Throwable t) {
        if (!isLevelEnabled(3)) {
            return;
        }
        log(3, message, t);
    }

    @Override
    public final void warn(Object message) {
        if (!isLevelEnabled(4)) {
            return;
        }
        log(4, message, null);
    }

    @Override
    public final void warn(Object message, Throwable t) {
        if (!isLevelEnabled(4)) {
            return;
        }
        log(4, message, t);
    }

    @Override
    public final void error(Object message) {
        if (!isLevelEnabled(5)) {
            return;
        }
        log(5, message, null);
    }

    @Override
    public final void error(Object message, Throwable t) {
        if (!isLevelEnabled(5)) {
            return;
        }
        log(5, message, t);
    }

    @Override
    public final void fatal(Object message) {
        if (!isLevelEnabled(6)) {
            return;
        }
        log(6, message, null);
    }

    @Override
    public final void fatal(Object message, Throwable t) {
        if (!isLevelEnabled(6)) {
            return;
        }
        log(6, message, t);
    }

    @Override
    public final boolean isDebugEnabled() {
        return isLevelEnabled(2);
    }

    @Override
    public final boolean isErrorEnabled() {
        return isLevelEnabled(5);
    }

    @Override
    public final boolean isFatalEnabled() {
        return isLevelEnabled(6);
    }

    @Override
    public final boolean isInfoEnabled() {
        return isLevelEnabled(3);
    }

    @Override
    public final boolean isTraceEnabled() {
        return isLevelEnabled(1);
    }

    @Override
    public final boolean isWarnEnabled() {
        return isLevelEnabled(4);
    }

    private static ClassLoader getContextClassLoader() {
        ClassLoader classLoader = null;
        try {
            Method method = Thread.class.getMethod("getContextClassLoader", null);
            try {
                classLoader = (ClassLoader) method.invoke(Thread.currentThread(), null);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e2) {
                if (!(e2.getTargetException() instanceof SecurityException)) {
                    throw new LogConfigurationException("Unexpected InvocationTargetException", e2.getTargetException());
                }
            }
        } catch (NoSuchMethodException e3) {
        }
        if (classLoader == null) {
            return SimpleLog.class.getClassLoader();
        }
        return classLoader;
    }

    private static InputStream getResourceAsStream(final String name) {
        return (InputStream) AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                ClassLoader threadCL = SimpleLog.getContextClassLoader();
                if (threadCL != null) {
                    return threadCL.getResourceAsStream(name);
                }
                return ClassLoader.getSystemResourceAsStream(name);
            }
        });
    }
}
