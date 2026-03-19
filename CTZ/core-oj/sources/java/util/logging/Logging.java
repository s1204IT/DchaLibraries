package java.util.logging;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

class Logging implements LoggingMXBean {
    private static LogManager logManager = LogManager.getLogManager();
    private static String EMPTY_STRING = "";

    Logging() {
    }

    @Override
    public List<String> getLoggerNames() {
        Enumeration<String> loggerNames = logManager.getLoggerNames();
        ArrayList arrayList = new ArrayList();
        while (loggerNames.hasMoreElements()) {
            arrayList.add(loggerNames.nextElement());
        }
        return arrayList;
    }

    @Override
    public String getLoggerLevel(String str) {
        Logger logger = logManager.getLogger(str);
        if (logger == null) {
            return null;
        }
        Level level = logger.getLevel();
        if (level == null) {
            return EMPTY_STRING;
        }
        return level.getLevelName();
    }

    @Override
    public void setLoggerLevel(String str, String str2) {
        if (str == null) {
            throw new NullPointerException("loggerName is null");
        }
        Logger logger = logManager.getLogger(str);
        if (logger == null) {
            throw new IllegalArgumentException("Logger " + str + "does not exist");
        }
        Level levelFindLevel = null;
        if (str2 != null && (levelFindLevel = Level.findLevel(str2)) == null) {
            throw new IllegalArgumentException("Unknown level \"" + str2 + "\"");
        }
        logger.setLevel(levelFindLevel);
    }

    @Override
    public String getParentLoggerName(String str) {
        Logger logger = logManager.getLogger(str);
        if (logger == null) {
            return null;
        }
        Logger parent = logger.getParent();
        if (parent == null) {
            return EMPTY_STRING;
        }
        return parent.getName();
    }
}
