package org.apache.commons.logging;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.NoOpLog;

@Deprecated
public class LogSource {
    protected static boolean jdk14IsAvailable;
    protected static boolean log4jIsAvailable;
    protected static Hashtable logs = new Hashtable();
    protected static Constructor logImplctor = null;

    static {
        String property;
        log4jIsAvailable = false;
        jdk14IsAvailable = false;
        try {
            if (Class.forName("org.apache.log4j.Logger") != null) {
                log4jIsAvailable = true;
            } else {
                log4jIsAvailable = false;
            }
        } catch (Throwable th) {
            log4jIsAvailable = false;
        }
        try {
            if (Class.forName("java.util.logging.Logger") != null && Class.forName("org.apache.commons.logging.impl.Jdk14Logger") != null) {
                jdk14IsAvailable = true;
            } else {
                jdk14IsAvailable = false;
            }
        } catch (Throwable th2) {
            jdk14IsAvailable = false;
        }
        try {
            property = System.getProperty("org.apache.commons.logging.log");
            if (property == null) {
                try {
                    property = System.getProperty(LogFactoryImpl.LOG_PROPERTY);
                } catch (Throwable th3) {
                }
            }
        } catch (Throwable th4) {
            property = null;
        }
        if (property != null) {
            try {
                setLogImplementation(property);
                return;
            } catch (Throwable th5) {
                try {
                    setLogImplementation("org.apache.commons.logging.impl.NoOpLog");
                    return;
                } catch (Throwable th6) {
                    return;
                }
            }
        }
        try {
            if (log4jIsAvailable) {
                setLogImplementation("org.apache.commons.logging.impl.Log4JLogger");
            } else if (jdk14IsAvailable) {
                setLogImplementation("org.apache.commons.logging.impl.Jdk14Logger");
            } else {
                setLogImplementation("org.apache.commons.logging.impl.NoOpLog");
            }
        } catch (Throwable th7) {
            try {
                setLogImplementation("org.apache.commons.logging.impl.NoOpLog");
            } catch (Throwable th8) {
            }
        }
    }

    private LogSource() {
    }

    public static void setLogImplementation(String str) throws LinkageError, NoSuchMethodException, SecurityException, ClassNotFoundException {
        try {
            logImplctor = Class.forName(str).getConstructor("".getClass());
        } catch (Throwable th) {
            logImplctor = null;
        }
    }

    public static void setLogImplementation(Class cls) throws LinkageError, NoSuchMethodException, SecurityException {
        logImplctor = cls.getConstructor("".getClass());
    }

    public static Log getInstance(String str) {
        Log log = (Log) logs.get(str);
        if (log == null) {
            Log logMakeNewLogInstance = makeNewLogInstance(str);
            logs.put(str, logMakeNewLogInstance);
            return logMakeNewLogInstance;
        }
        return log;
    }

    public static Log getInstance(Class cls) {
        return getInstance(cls.getName());
    }

    public static Log makeNewLogInstance(String str) {
        Log log;
        try {
            log = (Log) logImplctor.newInstance(str);
        } catch (Throwable th) {
            log = null;
        }
        if (log == null) {
            return new NoOpLog(str);
        }
        return log;
    }

    public static String[] getLogNames() {
        return (String[]) logs.keySet().toArray(new String[logs.size()]);
    }
}
