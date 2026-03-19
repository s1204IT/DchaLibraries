package junit.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.Properties;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestSuite;

public abstract class BaseTestRunner implements TestListener {
    public static final String SUITE_METHODNAME = "suite";
    private static Properties fPreferences;
    static boolean fgFilterStack = true;
    static int fgMaxMessageLength;
    boolean fLoading = true;

    protected abstract void runFailed(String str);

    public abstract void testEnded(String str);

    public abstract void testFailed(int i, Test test, Throwable th);

    public abstract void testStarted(String str);

    static {
        fgMaxMessageLength = 500;
        fgMaxMessageLength = getPreference("maxmessage", fgMaxMessageLength);
    }

    @Override
    public synchronized void startTest(Test test) {
        testStarted(test.toString());
    }

    protected static void setPreferences(Properties properties) {
        fPreferences = properties;
    }

    protected static Properties getPreferences() throws Throwable {
        if (fPreferences == null) {
            fPreferences = new Properties();
            fPreferences.put("loading", "true");
            fPreferences.put("filterstack", "true");
            readPreferences();
        }
        return fPreferences;
    }

    public static void savePreferences() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(getPreferencesFile());
        try {
            getPreferences().store(fileOutputStream, "");
        } finally {
            fileOutputStream.close();
        }
    }

    public static void setPreference(String str, String str2) {
        getPreferences().put(str, str2);
    }

    @Override
    public synchronized void endTest(Test test) {
        testEnded(test.toString());
    }

    @Override
    public synchronized void addError(Test test, Throwable th) {
        testFailed(1, test, th);
    }

    @Override
    public synchronized void addFailure(Test test, AssertionFailedError assertionFailedError) {
        testFailed(2, test, assertionFailedError);
    }

    public Test getTest(String str) {
        if (str.length() <= 0) {
            clearStatus();
            return null;
        }
        try {
            Class<?> clsLoadSuiteClass = loadSuiteClass(str);
            try {
                Method method = clsLoadSuiteClass.getMethod(SUITE_METHODNAME, new Class[0]);
                if (!Modifier.isStatic(method.getModifiers())) {
                    runFailed("Suite() method must be static");
                    return null;
                }
                try {
                    Test test = (Test) method.invoke(null, new Object[0]);
                    if (test == null) {
                        return test;
                    }
                    clearStatus();
                    return test;
                } catch (IllegalAccessException e) {
                    runFailed("Failed to invoke suite():" + e.toString());
                    return null;
                } catch (InvocationTargetException e2) {
                    runFailed("Failed to invoke suite():" + e2.getTargetException().toString());
                    return null;
                }
            } catch (Exception e3) {
                clearStatus();
                return new TestSuite(clsLoadSuiteClass);
            }
        } catch (ClassNotFoundException e4) {
            String message = e4.getMessage();
            if (message != null) {
                str = message;
            }
            runFailed("Class not found \"" + str + "\"");
            return null;
        } catch (Exception e5) {
            runFailed("Error: " + e5.toString());
            return null;
        }
    }

    public String elapsedTimeAsString(long j) {
        return NumberFormat.getInstance().format(j / 1000.0d);
    }

    protected String processArguments(String[] strArr) {
        String strExtractClassName = null;
        int i = 0;
        while (i < strArr.length) {
            if (strArr[i].equals("-noloading")) {
                setLoading(false);
            } else if (strArr[i].equals("-nofilterstack")) {
                fgFilterStack = false;
            } else if (strArr[i].equals("-c")) {
                i++;
                if (strArr.length > i) {
                    strExtractClassName = extractClassName(strArr[i]);
                } else {
                    System.out.println("Missing Test class name");
                }
            } else {
                strExtractClassName = strArr[i];
            }
            i++;
        }
        return strExtractClassName;
    }

    public void setLoading(boolean z) {
        this.fLoading = z;
    }

    public String extractClassName(String str) {
        if (str.startsWith("Default package for")) {
            return str.substring(str.lastIndexOf(".") + 1);
        }
        return str;
    }

    public static String truncate(String str) {
        if (fgMaxMessageLength != -1 && str.length() > fgMaxMessageLength) {
            return str.substring(0, fgMaxMessageLength) + "...";
        }
        return str;
    }

    protected Class<?> loadSuiteClass(String str) throws ClassNotFoundException {
        return Class.forName(str);
    }

    protected void clearStatus() {
    }

    protected boolean useReloadingTestSuiteLoader() {
        return getPreference("loading").equals("true") && this.fLoading;
    }

    private static File getPreferencesFile() {
        return new File(System.getProperty("user.home"), "junit.properties");
    }

    private static void readPreferences() throws Throwable {
        FileInputStream fileInputStream;
        Throwable th;
        try {
            try {
                fileInputStream = new FileInputStream(getPreferencesFile());
                try {
                    setPreferences(new Properties(getPreferences()));
                    getPreferences().load(fileInputStream);
                    fileInputStream.close();
                } catch (IOException e) {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e2) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e3) {
            }
        } catch (IOException e4) {
            fileInputStream = null;
        } catch (Throwable th3) {
            fileInputStream = null;
            th = th3;
        }
    }

    public static String getPreference(String str) {
        return getPreferences().getProperty(str);
    }

    public static int getPreference(String str, int i) {
        String preference = getPreference(str);
        if (preference == null) {
            return i;
        }
        try {
            return Integer.parseInt(preference);
        } catch (NumberFormatException e) {
            return i;
        }
    }

    public static String getFilteredTrace(Throwable th) {
        StringWriter stringWriter = new StringWriter();
        th.printStackTrace(new PrintWriter(stringWriter));
        return getFilteredTrace(stringWriter.toString());
    }

    public static String getFilteredTrace(String str) {
        if (showStackRaw()) {
            return str;
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line != null) {
                    if (!filterLine(line)) {
                        printWriter.println(line);
                    }
                } else {
                    return stringWriter.toString();
                }
            } catch (Exception e) {
                return str;
            }
        }
    }

    protected static boolean showStackRaw() {
        return (getPreference("filterstack").equals("true") && fgFilterStack) ? false : true;
    }

    static boolean filterLine(String str) {
        for (String str2 : new String[]{"junit.framework.TestCase", "junit.framework.TestResult", "junit.framework.TestSuite", "junit.framework.Assert.", "junit.swingui.TestRunner", "junit.awtui.TestRunner", "junit.textui.TestRunner", "java.lang.reflect.Method.invoke("}) {
            if (str.indexOf(str2) > 0) {
                return true;
            }
        }
        return false;
    }
}
