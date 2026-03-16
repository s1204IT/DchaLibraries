package java.sql;

import dalvik.system.VMStack;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class DriverManager {
    private static PrintStream thePrintStream;
    private static PrintWriter thePrintWriter;
    private static int loginTimeout = 0;
    private static final List<Driver> theDrivers = new ArrayList(10);
    private static final SQLPermission logPermission = new SQLPermission("setLog");

    static {
        loadInitialDrivers();
    }

    private static void loadInitialDrivers() {
        String theDriverList = System.getProperty("jdbc.drivers", null);
        if (theDriverList != null) {
            String[] theDriverNames = theDriverList.split(":");
            for (String element : theDriverNames) {
                try {
                    Class.forName(element, true, ClassLoader.getSystemClassLoader());
                } catch (Throwable th) {
                }
            }
        }
    }

    private DriverManager() {
    }

    public static void deregisterDriver(Driver driver) throws SQLException {
        if (driver != null) {
            ClassLoader callerClassLoader = VMStack.getCallingClassLoader();
            if (!isClassFromClassLoader(driver, callerClassLoader)) {
                throw new SecurityException("calling class not authorized to deregister JDBC driver");
            }
            synchronized (theDrivers) {
                theDrivers.remove(driver);
            }
        }
    }

    public static Connection getConnection(String url) throws SQLException {
        return getConnection(url, new Properties());
    }

    public static Connection getConnection(String url, Properties info) throws SQLException {
        if (url == null) {
            throw new SQLException("The url cannot be null", "08001");
        }
        synchronized (theDrivers) {
            for (Driver theDriver : theDrivers) {
                Connection theConnection = theDriver.connect(url, info);
                if (theConnection != null) {
                    return theConnection;
                }
            }
            throw new SQLException("No suitable driver", "08001");
        }
    }

    public static Connection getConnection(String url, String user, String password) throws SQLException {
        Properties theProperties = new Properties();
        if (user != null) {
            theProperties.setProperty("user", user);
        }
        if (password != null) {
            theProperties.setProperty("password", password);
        }
        return getConnection(url, theProperties);
    }

    public static Driver getDriver(String url) throws SQLException {
        ClassLoader callerClassLoader = VMStack.getCallingClassLoader();
        synchronized (theDrivers) {
            for (Driver driver : theDrivers) {
                if (driver.acceptsURL(url) && isClassFromClassLoader(driver, callerClassLoader)) {
                    return driver;
                }
            }
            throw new SQLException("No suitable driver", "08001");
        }
    }

    public static Enumeration<Driver> getDrivers() {
        Enumeration<Driver> enumeration;
        ClassLoader callerClassLoader = VMStack.getCallingClassLoader();
        synchronized (theDrivers) {
            ArrayList<Driver> result = new ArrayList<>();
            for (Driver driver : theDrivers) {
                if (isClassFromClassLoader(driver, callerClassLoader)) {
                    result.add(driver);
                }
            }
            enumeration = Collections.enumeration(result);
        }
        return enumeration;
    }

    public static int getLoginTimeout() {
        return loginTimeout;
    }

    @Deprecated
    public static PrintStream getLogStream() {
        return thePrintStream;
    }

    public static PrintWriter getLogWriter() {
        return thePrintWriter;
    }

    public static void println(String message) {
        if (thePrintWriter != null) {
            thePrintWriter.println(message);
            thePrintWriter.flush();
        } else if (thePrintStream != null) {
            thePrintStream.println(message);
            thePrintStream.flush();
        }
    }

    public static void registerDriver(Driver driver) throws SQLException {
        if (driver == null) {
            throw new NullPointerException("driver == null");
        }
        synchronized (theDrivers) {
            theDrivers.add(driver);
        }
    }

    public static void setLoginTimeout(int seconds) {
        loginTimeout = seconds;
    }

    @Deprecated
    public static void setLogStream(PrintStream out) {
        thePrintStream = out;
    }

    public static void setLogWriter(PrintWriter out) {
        thePrintWriter = out;
    }

    private static boolean isClassFromClassLoader(Object theObject, ClassLoader theClassLoader) {
        if (theObject == null || theClassLoader == null) {
            return false;
        }
        Class<?> objectClass = theObject.getClass();
        try {
            Class<?> checkClass = Class.forName(objectClass.getName(), true, theClassLoader);
            if (checkClass == objectClass) {
                return true;
            }
        } catch (Throwable th) {
        }
        return false;
    }
}
