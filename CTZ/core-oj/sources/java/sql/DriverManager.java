package java.sql;

import dalvik.system.VMStack;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import sun.reflect.CallerSensitive;

public class DriverManager {
    static final SQLPermission SET_LOG_PERMISSION;
    private static final CopyOnWriteArrayList<DriverInfo> registeredDrivers = new CopyOnWriteArrayList<>();
    private static volatile int loginTimeout = 0;
    private static volatile PrintWriter logWriter = null;
    private static volatile PrintStream logStream = null;
    private static final Object logSync = new Object();

    static {
        loadInitialDrivers();
        println("JDBC DriverManager initialized");
        SET_LOG_PERMISSION = new SQLPermission("setLog");
    }

    private DriverManager() {
    }

    public static PrintWriter getLogWriter() {
        return logWriter;
    }

    public static void setLogWriter(PrintWriter printWriter) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SET_LOG_PERMISSION);
        }
        logStream = null;
        logWriter = printWriter;
    }

    @CallerSensitive
    public static Connection getConnection(String str, Properties properties) throws SQLException {
        return getConnection(str, properties, VMStack.getCallingClassLoader());
    }

    @CallerSensitive
    public static Connection getConnection(String str, String str2, String str3) throws SQLException {
        Properties properties = new Properties();
        if (str2 != null) {
            properties.put("user", str2);
        }
        if (str3 != null) {
            properties.put("password", str3);
        }
        return getConnection(str, properties, VMStack.getCallingClassLoader());
    }

    @CallerSensitive
    public static Connection getConnection(String str) throws SQLException {
        return getConnection(str, new Properties(), VMStack.getCallingClassLoader());
    }

    @CallerSensitive
    public static Driver getDriver(String str) throws SQLException {
        println("DriverManager.getDriver(\"" + str + "\")");
        ClassLoader callingClassLoader = VMStack.getCallingClassLoader();
        for (DriverInfo driverInfo : registeredDrivers) {
            if (isDriverAllowed(driverInfo.driver, callingClassLoader)) {
                try {
                    if (driverInfo.driver.acceptsURL(str)) {
                        println("getDriver returning " + driverInfo.driver.getClass().getName());
                        return driverInfo.driver;
                    }
                    continue;
                } catch (SQLException e) {
                }
            } else {
                println("    skipping: " + driverInfo.driver.getClass().getName());
            }
        }
        println("getDriver: no suitable driver");
        throw new SQLException("No suitable driver", "08001");
    }

    public static synchronized void registerDriver(Driver driver) throws SQLException {
        if (driver != null) {
            registeredDrivers.addIfAbsent(new DriverInfo(driver));
            println("registerDriver: " + ((Object) driver));
        } else {
            throw new NullPointerException();
        }
    }

    @CallerSensitive
    public static synchronized void deregisterDriver(Driver driver) throws SQLException {
        if (driver == null) {
            return;
        }
        println("DriverManager.deregisterDriver: " + ((Object) driver));
        DriverInfo driverInfo = new DriverInfo(driver);
        if (registeredDrivers.contains(driverInfo)) {
            if (isDriverAllowed(driver, VMStack.getCallingClassLoader())) {
                registeredDrivers.remove(driverInfo);
            } else {
                throw new SecurityException();
            }
        } else {
            println("    couldn't find driver to unload");
        }
    }

    @CallerSensitive
    public static Enumeration<Driver> getDrivers() {
        Vector vector = new Vector();
        ClassLoader callingClassLoader = VMStack.getCallingClassLoader();
        for (DriverInfo driverInfo : registeredDrivers) {
            if (isDriverAllowed(driverInfo.driver, callingClassLoader)) {
                vector.addElement(driverInfo.driver);
            } else {
                println("    skipping: " + driverInfo.getClass().getName());
            }
        }
        return vector.elements();
    }

    public static void setLoginTimeout(int i) {
        loginTimeout = i;
    }

    public static int getLoginTimeout() {
        return loginTimeout;
    }

    @Deprecated
    public static void setLogStream(PrintStream printStream) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SET_LOG_PERMISSION);
        }
        logStream = printStream;
        if (printStream != null) {
            logWriter = new PrintWriter(printStream);
        } else {
            logWriter = null;
        }
    }

    @Deprecated
    public static PrintStream getLogStream() {
        return logStream;
    }

    public static void println(String str) {
        synchronized (logSync) {
            if (logWriter != null) {
                logWriter.println(str);
                logWriter.flush();
            }
        }
    }

    private static boolean isDriverAllowed(Driver driver, ClassLoader classLoader) {
        Class<?> cls;
        if (driver == null) {
            return false;
        }
        try {
            cls = Class.forName(driver.getClass().getName(), true, classLoader);
        } catch (Exception e) {
            cls = null;
        }
        if (cls != driver.getClass()) {
            return false;
        }
        return true;
    }

    private static void loadInitialDrivers() {
        String str;
        try {
            str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty("jdbc.drivers");
                }
            });
        } catch (Exception e) {
            str = null;
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Iterator it = ServiceLoader.load(Driver.class).iterator();
                while (it.hasNext()) {
                    try {
                        it.next();
                    } catch (Throwable th) {
                        return null;
                    }
                }
                return null;
            }
        });
        println("DriverManager.initialize: jdbc.drivers = " + str);
        if (str == null || str.equals("")) {
            return;
        }
        String[] strArrSplit = str.split(":");
        println("number of Drivers:" + strArrSplit.length);
        for (String str2 : strArrSplit) {
            try {
                println("DriverManager.Initialize: loading " + str2);
                Class.forName(str2, true, ClassLoader.getSystemClassLoader());
            } catch (Exception e2) {
                println("DriverManager.Initialize: load failed: " + ((Object) e2));
            }
        }
    }

    private static Connection getConnection(String str, Properties properties, ClassLoader classLoader) throws SQLException {
        synchronized (DriverManager.class) {
            if (classLoader == null) {
                try {
                    classLoader = Thread.currentThread().getContextClassLoader();
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        if (str == null) {
            throw new SQLException("The url cannot be null", "08001");
        }
        println("DriverManager.getConnection(\"" + str + "\")");
        SQLException sQLException = null;
        for (DriverInfo driverInfo : registeredDrivers) {
            if (isDriverAllowed(driverInfo.driver, classLoader)) {
                try {
                    println("    trying " + driverInfo.driver.getClass().getName());
                    Connection connectionConnect = driverInfo.driver.connect(str, properties);
                    if (connectionConnect != null) {
                        println("getConnection returning " + driverInfo.driver.getClass().getName());
                        return connectionConnect;
                    }
                    continue;
                } catch (SQLException e) {
                    if (sQLException == null) {
                        sQLException = e;
                    }
                }
            } else {
                println("    skipping: " + driverInfo.getClass().getName());
            }
        }
        if (sQLException != null) {
            println("getConnection failed: " + ((Object) sQLException));
            throw sQLException;
        }
        println("getConnection: no suitable driver found for " + str);
        throw new SQLException("No suitable driver found for " + str, "08001");
    }
}
