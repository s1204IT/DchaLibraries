package gov.nist.core;

import java.util.Properties;

public class LogWriter implements StackLogger {
    private static final String TAG = "SIP_STACK";
    private boolean mEnabled = true;

    @Override
    public void logStackTrace() {
    }

    @Override
    public void logStackTrace(int traceLevel) {
    }

    @Override
    public int getLineCount() {
        return 0;
    }

    @Override
    public void logException(Throwable ex) {
    }

    @Override
    public void logDebug(String message) {
    }

    @Override
    public void logTrace(String message) {
    }

    @Override
    public void logFatalError(String message) {
    }

    @Override
    public void logError(String message) {
    }

    @Override
    public boolean isLoggingEnabled() {
        return this.mEnabled;
    }

    @Override
    public boolean isLoggingEnabled(int logLevel) {
        return this.mEnabled;
    }

    @Override
    public void logError(String message, Exception ex) {
    }

    @Override
    public void logWarning(String string) {
    }

    @Override
    public void logInfo(String string) {
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
    public void setBuildTimeStamp(String buildTimeStamp) {
    }

    @Override
    public void setStackProperties(Properties stackProperties) {
    }

    @Override
    public String getLoggerName() {
        return "Android SIP Logger";
    }
}
