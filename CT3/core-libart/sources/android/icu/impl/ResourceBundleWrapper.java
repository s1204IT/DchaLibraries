package android.icu.impl;

import android.icu.impl.locale.BaseLocale;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ResourceBundleWrapper extends UResourceBundle {
    private static final boolean DEBUG = ICUDebug.enabled("resourceBundleWrapper");
    private ResourceBundle bundle;
    private String localeID = null;
    private String baseName = null;
    private List<String> keys = null;

    private ResourceBundleWrapper(ResourceBundle bundle) {
        this.bundle = null;
        this.bundle = bundle;
    }

    @Override
    protected void setLoadingStatus(int newStatus) {
    }

    @Override
    protected Object handleGetObject(String aKey) {
        Object obj = null;
        for (ResourceBundleWrapper current = this; current != null; current = (ResourceBundleWrapper) current.getParent()) {
            try {
                obj = current.bundle.getObject(aKey);
                break;
            } catch (MissingResourceException e) {
            }
        }
        if (obj == null) {
            throw new MissingResourceException("Can't find resource for bundle " + this.baseName + ", key " + aKey, getClass().getName(), aKey);
        }
        return obj;
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(this.keys);
    }

    private void initKeysVector() {
        this.keys = new ArrayList();
        for (ResourceBundleWrapper current = this; current != null; current = (ResourceBundleWrapper) current.getParent()) {
            Enumeration<String> e = current.bundle.getKeys();
            while (e.hasMoreElements()) {
                String elem = e.nextElement();
                if (!this.keys.contains(elem)) {
                    this.keys.add(elem);
                }
            }
        }
    }

    @Override
    protected String getLocaleID() {
        return this.localeID;
    }

    @Override
    protected String getBaseName() {
        return this.bundle.getClass().getName().replace('.', '/');
    }

    @Override
    public ULocale getULocale() {
        return new ULocale(this.localeID);
    }

    @Override
    public UResourceBundle getParent() {
        return (UResourceBundle) ((ResourceBundle) this).parent;
    }

    public static UResourceBundle getBundleInstance(String baseName, String localeID, ClassLoader root, boolean disableFallback) {
        UResourceBundle b = instantiateBundle(baseName, localeID, root, disableFallback);
        if (b == null) {
            String separator = BaseLocale.SEP;
            if (baseName.indexOf(47) >= 0) {
                separator = "/";
            }
            throw new MissingResourceException("Could not find the bundle " + baseName + separator + localeID, "", "");
        }
        return b;
    }

    protected static synchronized UResourceBundle instantiateBundle(String baseName, String localeID, ClassLoader root, boolean disableFallback) {
        ResourceBundleWrapper b;
        ResourceBundleWrapper b2;
        InputStream stream;
        if (root == null) {
            root = ClassLoaderUtil.getClassLoader();
        }
        final ClassLoader cl = root;
        String name = baseName;
        ULocale defaultLocale = ULocale.getDefault();
        if (localeID.length() != 0) {
            name = baseName + BaseLocale.SEP + localeID;
        }
        b = (ResourceBundleWrapper) loadFromCache(name, defaultLocale);
        if (b == null) {
            ResourceBundleWrapper parent = null;
            int i = localeID.lastIndexOf(95);
            boolean loadFromProperties = false;
            if (i != -1) {
                String locName = localeID.substring(0, i);
                parent = (ResourceBundleWrapper) loadFromCache(baseName + BaseLocale.SEP + locName, defaultLocale);
                if (parent == null) {
                    parent = (ResourceBundleWrapper) instantiateBundle(baseName, locName, cl, disableFallback);
                }
            } else if (localeID.length() > 0 && (parent = (ResourceBundleWrapper) loadFromCache(baseName, defaultLocale)) == null) {
                parent = (ResourceBundleWrapper) instantiateBundle(baseName, "", cl, disableFallback);
            }
            try {
                ResourceBundle bx = (ResourceBundle) cl.loadClass(name).asSubclass(ResourceBundle.class).newInstance();
                b2 = new ResourceBundleWrapper(bx);
                if (parent != null) {
                    try {
                        b2.setParent(parent);
                    } catch (ClassNotFoundException e) {
                        b = b2;
                        loadFromProperties = true;
                        b2 = b;
                    } catch (Exception e2) {
                        e = e2;
                        b = b2;
                        if (DEBUG) {
                            System.out.println("failure");
                        }
                        if (DEBUG) {
                            System.out.println(e);
                            b2 = b;
                        } else {
                            b2 = b;
                        }
                    } catch (NoClassDefFoundError e3) {
                        b = b2;
                        loadFromProperties = true;
                        b2 = b;
                    }
                }
                b2.baseName = baseName;
                b2.localeID = localeID;
            } catch (ClassNotFoundException e4) {
            } catch (Exception e5) {
                e = e5;
            } catch (NoClassDefFoundError e6) {
            }
            if (loadFromProperties) {
                try {
                    final String resName = name.replace('.', '/') + ".properties";
                    stream = (InputStream) AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                        @Override
                        public InputStream run() {
                            if (cl != null) {
                                return cl.getResourceAsStream(resName);
                            }
                            return ClassLoader.getSystemResourceAsStream(resName);
                        }
                    });
                } catch (Exception e7) {
                    e = e7;
                    b = b2;
                }
                try {
                    if (stream != null) {
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
                        try {
                            b = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                            if (parent != null) {
                                try {
                                    b.setParent(parent);
                                } catch (Exception e8) {
                                    try {
                                        bufferedInputStream.close();
                                    } catch (Exception e9) {
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    try {
                                        bufferedInputStream.close();
                                    } catch (Exception e10) {
                                    }
                                    throw th;
                                }
                            }
                            b.baseName = baseName;
                            b.localeID = localeID;
                            try {
                                bufferedInputStream.close();
                            } catch (Exception e11) {
                            }
                        } catch (Exception e12) {
                            b = b2;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } else {
                        b = b2;
                    }
                    if (b == null) {
                        String defaultName = defaultLocale.toString();
                        if (localeID.length() > 0 && localeID.indexOf(95) < 0 && defaultName.indexOf(localeID) == -1 && (b = (ResourceBundleWrapper) loadFromCache(baseName + BaseLocale.SEP + defaultName, defaultLocale)) == null) {
                            b = (ResourceBundleWrapper) instantiateBundle(baseName, defaultName, cl, disableFallback);
                        }
                    }
                    if (b == null) {
                        b = parent;
                    }
                } catch (Exception e13) {
                    e = e13;
                    if (DEBUG) {
                        System.out.println("failure");
                    }
                    if (DEBUG) {
                        System.out.println(e);
                    }
                }
            } else {
                b = b2;
            }
            b = (ResourceBundleWrapper) addToCache(name, defaultLocale, b);
        }
        if (b != null) {
            b.initKeysVector();
        } else if (DEBUG) {
            System.out.println("Returning null for " + baseName + BaseLocale.SEP + localeID);
        }
        return b;
    }
}
