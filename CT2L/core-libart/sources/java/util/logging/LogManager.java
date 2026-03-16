package java.util.logging;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import libcore.io.IoUtils;

public class LogManager {
    public static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";
    static LogManager manager;
    private static final LoggingPermission perm = new LoggingPermission("control", null);
    private Hashtable<String, Logger> loggers = new Hashtable<>();
    private Properties props = new Properties();
    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    static {
        String className = System.getProperty("java.util.logging.manager");
        if (className != null) {
            manager = (LogManager) getInstanceByClass(className);
        }
        if (manager == null) {
            manager = new LogManager();
        }
        try {
            manager.readConfiguration();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Logger root = new Logger("", null);
        root.setLevel(Level.INFO);
        Logger.global.setParent(root);
        manager.addLogger(root);
        manager.addLogger(Logger.global);
    }

    public static LoggingMXBean getLoggingMXBean() {
        throw new UnsupportedOperationException();
    }

    protected LogManager() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LogManager.this.reset();
            }
        });
    }

    public void checkAccess() {
    }

    public synchronized boolean addLogger(Logger logger) {
        boolean z;
        String name = logger.getName();
        if (this.loggers.get(name) != null) {
            z = false;
        } else {
            addToFamilyTree(logger, name);
            this.loggers.put(name, logger);
            logger.setManager(this);
            z = true;
        }
        return z;
    }

    private void addToFamilyTree(Logger logger, String name) {
        Logger parent = null;
        String parentName = name;
        do {
            int lastSeparator = parentName.lastIndexOf(46);
            if (lastSeparator == -1) {
                break;
            }
            parentName = parentName.substring(0, lastSeparator);
            Logger parent2 = this.loggers.get(parentName);
            parent = parent2;
            if (parent != null) {
                setParent(logger, parent);
                break;
            } else if (getProperty(parentName + ".level") != null) {
                break;
            }
        } while (getProperty(parentName + ".handlers") == null);
        parent = Logger.getLogger(parentName);
        setParent(logger, parent);
        if (parent == null) {
            Logger parent3 = this.loggers.get("");
            parent = parent3;
            if (parent != null) {
                setParent(logger, parent);
            }
        }
        String nameDot = name + '.';
        Collection<Logger> allLoggers = this.loggers.values();
        for (Logger child : allLoggers) {
            Logger oldParent = child.getParent();
            if (parent == oldParent && (name.length() == 0 || child.getName().startsWith(nameDot))) {
                child.setParent(logger);
                if (oldParent != null) {
                    oldParent.children.remove(child);
                }
            }
        }
    }

    public synchronized Logger getLogger(String name) {
        return this.loggers.get(name);
    }

    public synchronized Enumeration<String> getLoggerNames() {
        return this.loggers.keys();
    }

    public static LogManager getLogManager() {
        return manager;
    }

    public String getProperty(String name) {
        return this.props.getProperty(name);
    }

    public void readConfiguration() throws IOException {
        InputStream input;
        String configClassName = System.getProperty("java.util.logging.config.class");
        if (configClassName == null || getInstanceByClass(configClassName) == null) {
            String configFile = System.getProperty("java.util.logging.config.file");
            if (configFile == null) {
                configFile = System.getProperty("java.home") + File.separator + "lib" + File.separator + "logging.properties";
            }
            InputStream input2 = null;
            try {
                try {
                    InputStream input3 = new FileInputStream(configFile);
                    input = input3;
                } catch (IOException exception) {
                    input = LogManager.class.getResourceAsStream("logging.properties");
                    if (input == null) {
                        throw exception;
                    }
                }
                readConfiguration(new BufferedInputStream(input2));
            } finally {
                IoUtils.closeQuietly(input2);
            }
        }
    }

    static Object getInstanceByClass(String className) {
        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(className);
            return clazz.newInstance();
        } catch (Exception e) {
            try {
                Class<?> clazz2 = Thread.currentThread().getContextClassLoader().loadClass(className);
                return clazz2.newInstance();
            } catch (Exception innerE) {
                System.err.println("Loading class '" + className + "' failed");
                System.err.println(innerE);
                return null;
            }
        }
    }

    private synchronized void readConfigurationImpl(InputStream ins) throws IOException {
        reset();
        this.props.load(ins);
        Logger root = this.loggers.get("");
        if (root != null) {
            root.setManager(this);
        }
        String configs = this.props.getProperty("config");
        if (configs != null) {
            StringTokenizer st = new StringTokenizer(configs, " ");
            while (st.hasMoreTokens()) {
                String configerName = st.nextToken();
                getInstanceByClass(configerName);
            }
        }
        Collection<Logger> allLoggers = this.loggers.values();
        for (Logger logger : allLoggers) {
            String property = this.props.getProperty(logger.getName() + ".level");
            if (property != null) {
                logger.setLevel(Level.parse(property));
            }
        }
        this.listeners.firePropertyChange((String) null, (Object) null, (Object) null);
    }

    public void readConfiguration(InputStream ins) throws IOException {
        checkAccess();
        readConfigurationImpl(ins);
    }

    public synchronized void reset() {
        checkAccess();
        this.props = new Properties();
        Enumeration<String> names = getLoggerNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Logger logger = getLogger(name);
            if (logger != null) {
                logger.reset();
            }
        }
        Logger root = this.loggers.get("");
        if (root != null) {
            root.setLevel(Level.INFO);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (l == null) {
            throw new NullPointerException("l == null");
        }
        checkAccess();
        this.listeners.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        checkAccess();
        this.listeners.removePropertyChangeListener(l);
    }

    synchronized Logger getOrCreate(String name, String resourceBundleName) {
        Logger result;
        result = getLogger(name);
        if (result == null) {
            result = new Logger(name, resourceBundleName);
            addLogger(result);
        }
        return result;
    }

    synchronized void setParent(Logger logger, Logger newParent) {
        logger.parent = newParent;
        if (logger.levelObjVal == null) {
            setLevelRecursively(logger, null);
        }
        newParent.children.add(logger);
        logger.updateDalvikLogHandler();
    }

    synchronized void setLevelRecursively(Logger logger, Level newLevel) {
        int previous = logger.levelIntVal;
        logger.levelObjVal = newLevel;
        if (newLevel == null) {
            logger.levelIntVal = logger.parent != null ? logger.parent.levelIntVal : Level.INFO.intValue();
        } else {
            logger.levelIntVal = newLevel.intValue();
        }
        if (previous != logger.levelIntVal) {
            for (Logger child : logger.children) {
                if (child.levelObjVal == null) {
                    setLevelRecursively(child, null);
                }
            }
        }
    }
}
