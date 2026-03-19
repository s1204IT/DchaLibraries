package org.apache.commons.logging.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.apache.http.HttpHost;

@Deprecated
public class Jdk14Logger implements Log, Serializable {
    protected static final Level dummyLevel = Level.FINE;
    protected static int sLogLevel = 0;
    protected transient Logger logger;
    protected String name;

    private class HttpSimpleFormatter extends Formatter {
        HttpSimpleFormatter() {
        }

        @Override
        public String format(LogRecord r) {
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");
            Date date = new Date(r.getMillis());
            sb.append(dateFormat.format(date)).append(" ");
            sb.append(String.valueOf(r.getMillis())).append(" ");
            sb.append(HttpHost.DEFAULT_SCHEME_NAME).append(" ");
            sb.append(r.getThreadID()).append(" ");
            sb.append(formatMessage(r)).append(System.lineSeparator());
            return sb.toString();
        }
    }

    public Jdk14Logger(String name) {
        this.logger = null;
        this.name = null;
        this.name = name;
        this.logger = getLogger();
        try {
            Class<?> classType = Class.forName("android.os.SystemProperties");
            Method getIntMethod = classType.getDeclaredMethod("getInt", String.class, Integer.TYPE);
            Integer v = (Integer) getIntMethod.invoke(classType, "net.httplog.level", 0);
            sLogLevel = v.intValue();
            if (sLogLevel != 2 || name.indexOf("org.apache.http.wire") == -1) {
                return;
            }
            configLogFile();
        } catch (ClassNotFoundException c) {
            System.out.println("error:" + c.getMessage());
        } catch (IllegalAccessException iae) {
            System.out.println("error:" + iae.getMessage());
        } catch (NoSuchMethodException ie) {
            System.out.println("error:" + ie.getMessage());
        } catch (InvocationTargetException iee) {
            System.out.println("error:" + iee.getMessage());
        }
    }

    private void configLogFile() {
        try {
            new Date();
            new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            Thread.currentThread();
            File folder = new File("/data/http");
            if (!folder.isDirectory()) {
                folder.mkdir();
            }
            String tempPath = System.getProperty("java.io.tmpdir");
            System.out.println("http log file path:" + tempPath + ":%thttp.log");
            FileHandler fh = new FileHandler("%thttp.log", true);
            fh.setFormatter(new HttpSimpleFormatter());
            this.logger.addHandler(fh);
        } catch (IOException ioe) {
            System.out.println("error:" + ioe.getMessage());
        } catch (SecurityException e) {
            System.out.println("error:" + e.getMessage());
        }
    }

    private void log(Level level, String msg, Throwable ex) {
        Logger logger = getLogger();
        if (!logger.isLoggable(level)) {
            return;
        }
        Throwable dummyException = new Throwable();
        StackTraceElement[] locations = dummyException.getStackTrace();
        String cname = "unknown";
        String method = "unknown";
        if (locations != null && locations.length > 2) {
            StackTraceElement caller = locations[2];
            cname = caller.getClassName();
            method = caller.getMethodName();
        }
        if (ex == null) {
            if (sLogLevel == 1) {
                if (this.name.indexOf("org.apache.http.headers") != -1) {
                    System.out.println(this.name + ":" + method + ":" + msg);
                }
            } else if (sLogLevel == 2) {
                if (this.name.indexOf("org.apache.http.wire") != -1) {
                    System.out.println(this.name + ":" + method + ":" + msg);
                }
            } else if (sLogLevel == 3) {
                System.out.println(this.name + ":" + method + ":" + msg);
            }
            logger.logp(level, cname, method, msg);
            return;
        }
        logger.logp(level, cname, method, msg, ex);
    }

    @Override
    public void debug(Object message) {
        log(Level.FINE, String.valueOf(message), null);
    }

    @Override
    public void debug(Object message, Throwable exception) {
        log(Level.FINE, String.valueOf(message), exception);
    }

    @Override
    public void error(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public void error(Object message, Throwable exception) {
        log(Level.SEVERE, String.valueOf(message), exception);
    }

    @Override
    public void fatal(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public void fatal(Object message, Throwable exception) {
        log(Level.SEVERE, String.valueOf(message), exception);
    }

    public Logger getLogger() {
        if (this.logger == null) {
            this.logger = Logger.getLogger(this.name);
        }
        if (sLogLevel > 0) {
            this.logger.setLevel(Level.ALL);
        }
        return this.logger;
    }

    @Override
    public void info(Object message) {
        log(Level.INFO, String.valueOf(message), null);
    }

    @Override
    public void info(Object message, Throwable exception) {
        log(Level.INFO, String.valueOf(message), exception);
    }

    @Override
    public boolean isDebugEnabled() {
        return getLogger().isLoggable(Level.FINE);
    }

    @Override
    public boolean isErrorEnabled() {
        return getLogger().isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isFatalEnabled() {
        return getLogger().isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isInfoEnabled() {
        return getLogger().isLoggable(Level.INFO);
    }

    @Override
    public boolean isTraceEnabled() {
        return getLogger().isLoggable(Level.FINEST);
    }

    @Override
    public boolean isWarnEnabled() {
        return getLogger().isLoggable(Level.WARNING);
    }

    @Override
    public void trace(Object message) {
        log(Level.FINEST, String.valueOf(message), null);
    }

    @Override
    public void trace(Object message, Throwable exception) {
        log(Level.FINEST, String.valueOf(message), exception);
    }

    @Override
    public void warn(Object message) {
        log(Level.WARNING, String.valueOf(message), null);
    }

    @Override
    public void warn(Object message, Throwable exception) {
        log(Level.WARNING, String.valueOf(message), exception);
    }
}
