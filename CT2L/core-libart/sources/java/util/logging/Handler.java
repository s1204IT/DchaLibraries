package java.util.logging;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import javax.xml.transform.OutputKeys;

public abstract class Handler {
    private static final Level DEFAULT_LEVEL = Level.ALL;
    private ErrorManager errorMan = new ErrorManager();
    private Level level = DEFAULT_LEVEL;
    private String encoding = null;
    private Filter filter = null;
    private Formatter formatter = null;
    private String prefix = getClass().getName();

    public abstract void close();

    public abstract void flush();

    public abstract void publish(LogRecord logRecord);

    protected Handler() {
    }

    private Object getDefaultInstance(String className) {
        if (className == null) {
            return null;
        }
        try {
            Object result = Class.forName(className).newInstance();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getCustomizeInstance(String className) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        Class<?> c = loader.loadClass(className);
        return c.newInstance();
    }

    void printInvalidPropMessage(String key, String value, Exception e) {
        String msg = "Invalid property value for " + this.prefix + ":" + key + "/" + value;
        this.errorMan.error(msg, e, 0);
    }

    void initProperties(String defaultLevel, String defaultFilter, String defaultFormatter, String defaultEncoding) {
        LogManager manager = LogManager.getLogManager();
        String filterName = manager.getProperty(this.prefix + ".filter");
        if (filterName != null) {
            try {
                this.filter = (Filter) getCustomizeInstance(filterName);
            } catch (Exception e1) {
                printInvalidPropMessage("filter", filterName, e1);
                this.filter = (Filter) getDefaultInstance(defaultFilter);
            }
        } else {
            this.filter = (Filter) getDefaultInstance(defaultFilter);
        }
        String levelName = manager.getProperty(this.prefix + ".level");
        if (levelName != null) {
            try {
                this.level = Level.parse(levelName);
            } catch (Exception e) {
                printInvalidPropMessage("level", levelName, e);
                this.level = Level.parse(defaultLevel);
            }
        } else {
            this.level = Level.parse(defaultLevel);
        }
        String formatterName = manager.getProperty(this.prefix + ".formatter");
        if (formatterName != null) {
            try {
                this.formatter = (Formatter) getCustomizeInstance(formatterName);
            } catch (Exception e2) {
                printInvalidPropMessage("formatter", formatterName, e2);
                this.formatter = (Formatter) getDefaultInstance(defaultFormatter);
            }
        } else {
            this.formatter = (Formatter) getDefaultInstance(defaultFormatter);
        }
        String encodingName = manager.getProperty(this.prefix + ".encoding");
        try {
            internalSetEncoding(encodingName);
        } catch (UnsupportedEncodingException e3) {
            printInvalidPropMessage(OutputKeys.ENCODING, encodingName, e3);
        }
    }

    public String getEncoding() {
        return this.encoding;
    }

    public ErrorManager getErrorManager() {
        LogManager.getLogManager().checkAccess();
        return this.errorMan;
    }

    public Filter getFilter() {
        return this.filter;
    }

    public Formatter getFormatter() {
        return this.formatter;
    }

    public Level getLevel() {
        return this.level;
    }

    public boolean isLoggable(LogRecord record) {
        if (record == null) {
            throw new NullPointerException("record == null");
        }
        if (this.level.intValue() != Level.OFF.intValue() && record.getLevel().intValue() >= this.level.intValue()) {
            return this.filter == null || this.filter.isLoggable(record);
        }
        return false;
    }

    protected void reportError(String msg, Exception ex, int code) {
        this.errorMan.error(msg, ex, code);
    }

    void internalSetEncoding(String newEncoding) throws UnsupportedEncodingException {
        if (newEncoding == null) {
            this.encoding = null;
        } else {
            if (Charset.isSupported(newEncoding)) {
                this.encoding = newEncoding;
                return;
            }
            throw new UnsupportedEncodingException(newEncoding);
        }
    }

    public void setEncoding(String charsetName) throws UnsupportedEncodingException {
        LogManager.getLogManager().checkAccess();
        internalSetEncoding(charsetName);
    }

    public void setErrorManager(ErrorManager newErrorManager) {
        LogManager.getLogManager().checkAccess();
        if (newErrorManager == null) {
            throw new NullPointerException("newErrorManager == null");
        }
        this.errorMan = newErrorManager;
    }

    public void setFilter(Filter newFilter) {
        LogManager.getLogManager().checkAccess();
        this.filter = newFilter;
    }

    void internalSetFormatter(Formatter newFormatter) {
        if (newFormatter == null) {
            throw new NullPointerException("newFormatter == null");
        }
        this.formatter = newFormatter;
    }

    public void setFormatter(Formatter newFormatter) {
        LogManager.getLogManager().checkAccess();
        internalSetFormatter(newFormatter);
    }

    public void setLevel(Level newLevel) {
        if (newLevel == null) {
            throw new NullPointerException("newLevel == null");
        }
        LogManager.getLogManager().checkAccess();
        this.level = newLevel;
    }
}
