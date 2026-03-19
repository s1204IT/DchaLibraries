package com.android.internal.logging;

import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import dalvik.system.DalvikLogHandler;
import dalvik.system.DalvikLogging;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class AndroidHandler extends Handler implements DalvikLogHandler {
    private static final Formatter THE_FORMATTER = new Formatter() {
        @Override
        public String format(LogRecord logRecord) {
            Throwable thrown = logRecord.getThrown();
            if (thrown != null) {
                StringWriter stringWriter = new StringWriter();
                FastPrintWriter fastPrintWriter = new FastPrintWriter((Writer) stringWriter, false, 256);
                stringWriter.write(logRecord.getMessage());
                stringWriter.write("\n");
                thrown.printStackTrace(fastPrintWriter);
                fastPrintWriter.flush();
                return stringWriter.toString();
            }
            return logRecord.getMessage();
        }
    };

    public AndroidHandler() {
        setFormatter(THE_FORMATTER);
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord logRecord) {
        int androidLevel = getAndroidLevel(logRecord.getLevel());
        String strLoggerNameToTag = DalvikLogging.loggerNameToTag(logRecord.getLoggerName());
        if (!Log.isLoggable(strLoggerNameToTag, androidLevel)) {
            return;
        }
        try {
            Log.println(androidLevel, strLoggerNameToTag, getFormatter().format(logRecord));
        } catch (RuntimeException e) {
            Log.e("AndroidHandler", "Error logging message.", e);
        }
    }

    public void publish(Logger logger, String str, Level level, String str2) {
        int androidLevel = getAndroidLevel(level);
        if (!Log.isLoggable(str, androidLevel)) {
            return;
        }
        try {
            Log.println(androidLevel, str, str2);
        } catch (RuntimeException e) {
            Log.e("AndroidHandler", "Error logging message.", e);
        }
    }

    static int getAndroidLevel(Level level) {
        int iIntValue = level.intValue();
        if (iIntValue >= 1000) {
            return 6;
        }
        if (iIntValue >= 900) {
            return 5;
        }
        if (iIntValue >= 800) {
            return 4;
        }
        return 3;
    }
}
