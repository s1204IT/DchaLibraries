package com.android.uiautomator.core;

import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Tracer {
    private static final int CALLER_LOCATION = 6;
    private static final int METHOD_TO_TRACE_LOCATION = 5;
    private static final int MIN_STACK_TRACE_LENGTH = 7;
    private static final String UIAUTOMATOR_PACKAGE = "com.android.uiautomator.core";
    private static final String UNKNOWN_METHOD_STRING = "(unknown method)";
    private static Tracer mInstance = null;
    private File mOutputFile;
    private Mode mCurrentMode = Mode.NONE;
    private List<TracerSink> mSinks = new ArrayList();

    public enum Mode {
        NONE,
        FILE,
        LOGCAT,
        ALL
    }

    private interface TracerSink {
        void close();

        void log(String str);
    }

    private class FileSink implements TracerSink {
        private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        private PrintWriter mOut;

        public FileSink(File file) throws FileNotFoundException {
            this.mOut = new PrintWriter(file);
        }

        @Override
        public void log(String str) {
            this.mOut.printf("%s %s\n", this.mDateFormat.format(new Date()), str);
        }

        @Override
        public void close() {
            this.mOut.close();
        }
    }

    private class LogcatSink implements TracerSink {
        private static final String LOGCAT_TAG = "UiAutomatorTrace";

        private LogcatSink() {
        }

        LogcatSink(Tracer tracer, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void log(String str) {
            Log.i(LOGCAT_TAG, str);
        }

        @Override
        public void close() {
        }
    }

    public static Tracer getInstance() {
        if (mInstance == null) {
            mInstance = new Tracer();
        }
        return mInstance;
    }

    public void setOutputMode(Mode mode) {
        closeSinks();
        this.mCurrentMode = mode;
        try {
            AnonymousClass1 anonymousClass1 = null;
            switch (AnonymousClass1.$SwitchMap$com$android$uiautomator$core$Tracer$Mode[mode.ordinal()]) {
                case 1:
                    if (this.mOutputFile == null) {
                        throw new IllegalArgumentException("Please provide a filename before attempting write trace to a file");
                    }
                    this.mSinks.add(new FileSink(this.mOutputFile));
                    return;
                case 2:
                    this.mSinks.add(new LogcatSink(this, anonymousClass1));
                    return;
                case 3:
                    this.mSinks.add(new LogcatSink(this, anonymousClass1));
                    if (this.mOutputFile == null) {
                        throw new IllegalArgumentException("Please provide a filename before attempting write trace to a file");
                    }
                    this.mSinks.add(new FileSink(this.mOutputFile));
                    return;
                default:
                    return;
            }
        } catch (FileNotFoundException e) {
            Log.w("Tracer", "Could not open log file: " + e.getMessage());
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$uiautomator$core$Tracer$Mode = new int[Mode.values().length];

        static {
            try {
                $SwitchMap$com$android$uiautomator$core$Tracer$Mode[Mode.FILE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$uiautomator$core$Tracer$Mode[Mode.LOGCAT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$uiautomator$core$Tracer$Mode[Mode.ALL.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void closeSinks() {
        Iterator<TracerSink> it = this.mSinks.iterator();
        while (it.hasNext()) {
            it.next().close();
        }
        this.mSinks.clear();
    }

    public void setOutputFilename(String str) {
        this.mOutputFile = new File(str);
    }

    private void doTrace(Object[] objArr) {
        String caller;
        if (this.mCurrentMode == Mode.NONE || (caller = getCaller()) == null) {
            return;
        }
        log(String.format("%s (%s)", caller, join(", ", objArr)));
    }

    private void log(String str) {
        Iterator<TracerSink> it = this.mSinks.iterator();
        while (it.hasNext()) {
            it.next().log(str);
        }
    }

    public boolean isTracingEnabled() {
        return this.mCurrentMode != Mode.NONE;
    }

    public static void trace(Object... objArr) {
        getInstance().doTrace(objArr);
    }

    private static String join(String str, Object[] objArr) {
        if (objArr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(objectToString(objArr[0]));
        for (int i = 1; i < objArr.length; i++) {
            sb.append(str);
            sb.append(objectToString(objArr[i]));
        }
        return sb.toString();
    }

    private static String objectToString(Object obj) {
        if (obj.getClass().isArray()) {
            return obj instanceof Object[] ? Arrays.deepToString(obj) : "[...]";
        }
        return obj.toString();
    }

    private static String getCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length < MIN_STACK_TRACE_LENGTH) {
            return UNKNOWN_METHOD_STRING;
        }
        StackTraceElement stackTraceElement = stackTrace[METHOD_TO_TRACE_LOCATION];
        StackTraceElement stackTraceElement2 = stackTrace[CALLER_LOCATION];
        if (stackTraceElement2.getClassName().startsWith(UIAUTOMATOR_PACKAGE)) {
            return null;
        }
        int iLastIndexOf = stackTraceElement.getClassName().lastIndexOf(46);
        if (iLastIndexOf < 0) {
            iLastIndexOf = 0;
        }
        int i = iLastIndexOf + 1;
        if (i >= stackTraceElement.getClassName().length()) {
            return UNKNOWN_METHOD_STRING;
        }
        return String.format("%s.%s from %s() at %s:%d", stackTraceElement.getClassName().substring(i), stackTraceElement.getMethodName(), stackTraceElement2.getMethodName(), stackTraceElement2.getFileName(), Integer.valueOf(stackTraceElement2.getLineNumber()));
    }
}
