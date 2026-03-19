package gov.nist.core;

import java.util.Properties;

public class LogWriter implements StackLogger {
    private static final String TAG = "SIP_STACK";
    private boolean mEnabled = true;

    @Override
    public void logStackTrace() {
    }

    @Override
    public void logStackTrace(int i) {
    }

    @Override
    public int getLineCount() {
        return 0;
    }

    @Override
    public void logException(Throwable th) {
    }

    @Override
    public void logDebug(String str) {
    }

    @Override
    public void logTrace(String str) {
    }

    @Override
    public void logFatalError(String str) {
    }

    @Override
    public void logError(String str) {
    }

    @Override
    public boolean isLoggingEnabled() {
        return this.mEnabled;
    }

    @Override
    public boolean isLoggingEnabled(int i) {
        return this.mEnabled;
    }

    @Override
    public void logError(String str, Exception exc) {
    }

    @Override
    public void logWarning(String str) {
    }

    @Override
    public void logInfo(String str) {
    }

    @Override
    public void disableLogging() {
        this.mEnabled = false;
    }

    @Override
    public void enableLogging() {
        this.mEnabled = true;
    }

    @Override
    public void setBuildTimeStamp(String str) {
    }

    @Override
    public void setStackProperties(Properties properties) {
    }

    @Override
    public String getLoggerName() {
        return "Android SIP Logger";
    }
}
