package com.android.commands.uiautomator;

import android.os.Bundle;
import android.util.Log;
import com.android.commands.uiautomator.Launcher;
import com.android.uiautomator.testrunner.UiAutomatorTestRunner;
import dalvik.system.DexFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class RunTestCommand extends Launcher.Command {
    private static final int ARG_FAIL_INCOMPLETE_C = -2;
    private static final int ARG_FAIL_INCOMPLETE_E = -1;
    private static final int ARG_FAIL_NO_CLASS = -3;
    private static final int ARG_FAIL_RUNNER = -4;
    private static final int ARG_FAIL_UNSUPPORTED = -99;
    private static final int ARG_OK = 0;
    private static final String CLASS_PARAM = "class";
    private static final String CLASS_SEPARATOR = ",";
    private static final String DEBUG_PARAM = "debug";
    private static final String JARS_PARAM = "jars";
    private static final String JARS_SEPARATOR = ":";
    private static final String LOGTAG = RunTestCommand.class.getSimpleName();
    private static final String OUTPUT_FORMAT_KEY = "outputFormat";
    private static final String OUTPUT_SIMPLE = "simple";
    private static final String RUNNER_PARAM = "runner";
    private boolean mDebug;
    private boolean mMonkey;
    private final Bundle mParams;
    private UiAutomatorTestRunner mRunner;
    private String mRunnerClassName;
    private final List<String> mTestClasses;

    public RunTestCommand() {
        super("runtest");
        this.mParams = new Bundle();
        this.mTestClasses = new ArrayList();
        this.mMonkey = false;
    }

    @Override
    public void run(String[] strArr) {
        int args = parseArgs(strArr);
        if (args != ARG_FAIL_UNSUPPORTED) {
            switch (args) {
                case ARG_FAIL_INCOMPLETE_C:
                    System.err.println("Incomplete '-c' parameter.");
                    System.exit(ARG_FAIL_INCOMPLETE_C);
                    break;
                case ARG_FAIL_INCOMPLETE_E:
                    System.err.println("Incomplete '-e' parameter.");
                    System.exit(ARG_FAIL_INCOMPLETE_E);
                    break;
            }
        } else {
            System.err.println("Unsupported standalone parameter.");
            System.exit(ARG_FAIL_UNSUPPORTED);
        }
        if (this.mTestClasses.isEmpty()) {
            addTestClassesFromJars();
            if (this.mTestClasses.isEmpty()) {
                System.err.println("No test classes found.");
                System.exit(ARG_FAIL_NO_CLASS);
            }
        }
        getRunner().run(this.mTestClasses, this.mParams, this.mDebug, this.mMonkey);
    }

    private int parseArgs(String[] strArr) {
        int i = ARG_OK;
        while (i < strArr.length) {
            if (strArr[i].equals("-e")) {
                if (i + 2 < strArr.length) {
                    int i2 = i + 1;
                    String str = strArr[i2];
                    i = i2 + 1;
                    String str2 = strArr[i];
                    if (CLASS_PARAM.equals(str)) {
                        addTestClasses(str2);
                    } else if (DEBUG_PARAM.equals(str)) {
                        this.mDebug = ("true".equals(str2) || "1".equals(str2)) ? true : ARG_OK;
                    } else if (RUNNER_PARAM.equals(str)) {
                        this.mRunnerClassName = str2;
                    } else {
                        this.mParams.putString(str, str2);
                    }
                } else {
                    return ARG_FAIL_INCOMPLETE_E;
                }
            } else if (strArr[i].equals("-c")) {
                i++;
                if (i < strArr.length) {
                    addTestClasses(strArr[i]);
                } else {
                    return ARG_FAIL_INCOMPLETE_C;
                }
            } else if (strArr[i].equals("--monkey")) {
                this.mMonkey = true;
            } else if (strArr[i].equals("-s")) {
                this.mParams.putString(OUTPUT_FORMAT_KEY, OUTPUT_SIMPLE);
            } else {
                return ARG_FAIL_UNSUPPORTED;
            }
            i++;
        }
        return ARG_OK;
    }

    protected UiAutomatorTestRunner getRunner() {
        Object objNewInstance;
        if (this.mRunner != null) {
            return this.mRunner;
        }
        if (this.mRunnerClassName == null) {
            this.mRunner = new UiAutomatorTestRunner();
            return this.mRunner;
        }
        try {
            objNewInstance = Class.forName(this.mRunnerClassName).newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot find runner: " + this.mRunnerClassName);
            System.exit(ARG_FAIL_RUNNER);
            objNewInstance = null;
        } catch (IllegalAccessException e2) {
            System.err.println("Constructor of runner " + this.mRunnerClassName + " is not accessibile");
            System.exit(ARG_FAIL_RUNNER);
            objNewInstance = null;
        } catch (InstantiationException e3) {
            System.err.println("Cannot instantiate runner: " + this.mRunnerClassName);
            System.exit(ARG_FAIL_RUNNER);
            objNewInstance = null;
        }
        try {
            UiAutomatorTestRunner uiAutomatorTestRunner = (UiAutomatorTestRunner) objNewInstance;
            this.mRunner = uiAutomatorTestRunner;
            return uiAutomatorTestRunner;
        } catch (ClassCastException e4) {
            System.err.println("Specified runner is not subclass of " + UiAutomatorTestRunner.class.getSimpleName());
            System.exit(ARG_FAIL_RUNNER);
            return null;
        }
    }

    private void addTestClasses(String str) {
        String[] strArrSplit = str.split(CLASS_SEPARATOR);
        int length = strArrSplit.length;
        for (int i = ARG_OK; i < length; i++) {
            this.mTestClasses.add(strArrSplit[i]);
        }
    }

    private void addTestClassesFromJars() {
        String string = this.mParams.getString(JARS_PARAM);
        if (string == null) {
            return;
        }
        String[] strArrSplit = string.split(JARS_SEPARATOR);
        int length = strArrSplit.length;
        for (int i = ARG_OK; i < length; i++) {
            String strTrim = strArrSplit[i].trim();
            if (!strTrim.isEmpty()) {
                try {
                    DexFile dexFile = new DexFile(strTrim);
                    Enumeration<String> enumerationEntries = dexFile.entries();
                    while (enumerationEntries.hasMoreElements()) {
                        String strNextElement = enumerationEntries.nextElement();
                        if (isTestClass(strNextElement)) {
                            this.mTestClasses.add(strNextElement);
                        }
                    }
                    dexFile.close();
                } catch (IOException e) {
                    Log.w(LOGTAG, String.format("Could not read %s: %s", strTrim, e.getMessage()));
                }
            }
        }
    }

    private boolean isTestClass(String str) {
        try {
            Class<?> clsLoadClass = getClass().getClassLoader().loadClass(str);
            if (clsLoadClass.getEnclosingClass() != null) {
                return false;
            }
            return getRunner().getTestCaseFilter().accept(clsLoadClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String detailedOptions() {
        return "    runtest <class spec> [options]\n    <class spec>: <JARS> < -c <CLASSES> | -e class <CLASSES> >\n      <JARS>: a list of jar files containing test classes and dependencies. If\n        the path is relative, it's assumed to be under /data/local/tmp. Use\n        absolute path if the file is elsewhere. Multiple files can be\n        specified, separated by space.\n      <CLASSES>: a list of test class names to run, separated by comma. To\n        a single method, use TestClass#testMethod format. The -e or -c option\n        may be repeated. This option is not required and if not provided then\n        all the tests in provided jars will be run automatically.\n    options:\n      --nohup: trap SIG_HUP, so test won't terminate even if parent process\n               is terminated, e.g. USB is disconnected.\n      -e debug [true|false]: wait for debugger to connect before starting.\n      -e runner [CLASS]: use specified test runner class instead. If\n        unspecified, framework default runner will be used.\n      -e <NAME> <VALUE>: other name-value pairs to be passed to test classes.\n        May be repeated.\n      -e outputFormat simple | -s: enabled less verbose JUnit style output.\n";
    }

    @Override
    public String shortHelp() {
        return "executes UI automation tests";
    }
}
