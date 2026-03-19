package java.util.logging;

import java.util.List;
import sun.util.logging.LoggingProxy;

class LoggingProxyImpl implements LoggingProxy {
    static final LoggingProxy INSTANCE = new LoggingProxyImpl();

    private LoggingProxyImpl() {
    }

    @Override
    public Object getLogger(String str) {
        return Logger.getPlatformLogger(str);
    }

    @Override
    public Object getLevel(Object obj) {
        return ((Logger) obj).getLevel();
    }

    @Override
    public void setLevel(Object obj, Object obj2) {
        ((Logger) obj).setLevel((Level) obj2);
    }

    @Override
    public boolean isLoggable(Object obj, Object obj2) {
        return ((Logger) obj).isLoggable((Level) obj2);
    }

    @Override
    public void log(Object obj, Object obj2, String str) {
        ((Logger) obj).log((Level) obj2, str);
    }

    @Override
    public void log(Object obj, Object obj2, String str, Throwable th) {
        ((Logger) obj).log((Level) obj2, str, th);
    }

    @Override
    public void log(Object obj, Object obj2, String str, Object... objArr) {
        ((Logger) obj).log((Level) obj2, str, objArr);
    }

    @Override
    public List<String> getLoggerNames() {
        return LogManager.getLoggingMXBean().getLoggerNames();
    }

    @Override
    public String getLoggerLevel(String str) {
        return LogManager.getLoggingMXBean().getLoggerLevel(str);
    }

    @Override
    public void setLoggerLevel(String str, String str2) {
        LogManager.getLoggingMXBean().setLoggerLevel(str, str2);
    }

    @Override
    public String getParentLoggerName(String str) {
        return LogManager.getLoggingMXBean().getParentLoggerName(str);
    }

    @Override
    public Object parseLevel(String str) {
        Level levelFindLevel = Level.findLevel(str);
        if (levelFindLevel == null) {
            throw new IllegalArgumentException("Unknown level \"" + str + "\"");
        }
        return levelFindLevel;
    }

    @Override
    public String getLevelName(Object obj) {
        return ((Level) obj).getLevelName();
    }

    @Override
    public int getLevelValue(Object obj) {
        return ((Level) obj).intValue();
    }

    @Override
    public String getProperty(String str) {
        return LogManager.getLogManager().getProperty(str);
    }
}
