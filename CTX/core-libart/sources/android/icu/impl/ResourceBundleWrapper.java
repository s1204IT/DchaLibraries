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

public final class ResourceBundleWrapper extends UResourceBundle {
    private static CacheBase<String, ResourceBundleWrapper, Loader> BUNDLE_CACHE = new SoftCache<String, ResourceBundleWrapper, Loader>() {
        @Override
        protected ResourceBundleWrapper createInstance(String str, Loader loader) {
            return loader.load();
        }
    };
    private static final boolean DEBUG = ICUDebug.enabled("resourceBundleWrapper");
    private String baseName;
    private ResourceBundle bundle;
    private List<String> keys;
    private String localeID;

    private static abstract class Loader {
        abstract ResourceBundleWrapper load();

        private Loader() {
        }
    }

    private ResourceBundleWrapper(ResourceBundle resourceBundle) {
        this.bundle = null;
        this.localeID = null;
        this.baseName = null;
        this.keys = null;
        this.bundle = resourceBundle;
    }

    @Override
    protected Object handleGetObject(String str) {
        Object object;
        ResourceBundleWrapper resourceBundleWrapper = this;
        while (true) {
            if (resourceBundleWrapper != null) {
                try {
                    object = resourceBundleWrapper.bundle.getObject(str);
                    break;
                } catch (MissingResourceException e) {
                    resourceBundleWrapper = (ResourceBundleWrapper) resourceBundleWrapper.getParent();
                }
            } else {
                object = null;
                break;
            }
        }
        if (object == null) {
            throw new MissingResourceException("Can't find resource for bundle " + this.baseName + ", key " + str, getClass().getName(), str);
        }
        return object;
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(this.keys);
    }

    private void initKeysVector() {
        this.keys = new ArrayList();
        for (ResourceBundleWrapper resourceBundleWrapper = this; resourceBundleWrapper != null; resourceBundleWrapper = (ResourceBundleWrapper) resourceBundleWrapper.getParent()) {
            Enumeration<String> keys = resourceBundleWrapper.bundle.getKeys();
            while (keys.hasMoreElements()) {
                String strNextElement = keys.nextElement();
                if (!this.keys.contains(strNextElement)) {
                    this.keys.add(strNextElement);
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
        return (UResourceBundle) this.parent;
    }

    public static ResourceBundleWrapper getBundleInstance(String str, String str2, ClassLoader classLoader, boolean z) {
        ResourceBundleWrapper resourceBundleWrapperInstantiateBundle;
        if (classLoader == null) {
            classLoader = ClassLoaderUtil.getClassLoader();
        }
        if (z) {
            resourceBundleWrapperInstantiateBundle = instantiateBundle(str, str2, null, classLoader, z);
        } else {
            resourceBundleWrapperInstantiateBundle = instantiateBundle(str, str2, ULocale.getDefault().getBaseName(), classLoader, z);
        }
        if (resourceBundleWrapperInstantiateBundle == null) {
            throw new MissingResourceException("Could not find the bundle " + str + (str.indexOf(47) >= 0 ? "/" : BaseLocale.SEP) + str2, "", "");
        }
        return resourceBundleWrapperInstantiateBundle;
    }

    private static boolean localeIDStartsWithLangSubtag(String str, String str2) {
        return str.startsWith(str2) && (str.length() == str2.length() || str.charAt(str2.length()) == '_');
    }

    private static ResourceBundleWrapper instantiateBundle(final String str, final String str2, final String str3, final ClassLoader classLoader, final boolean z) {
        final String str4;
        String str5;
        if (!str2.isEmpty()) {
            str4 = str + '_' + str2;
        } else {
            str4 = str;
        }
        if (!z) {
            str5 = str4 + '#' + str3;
        } else {
            str5 = str4;
        }
        return BUNDLE_CACHE.getInstance(str5, new Loader() {
            {
                super();
            }

            @Override
            public ResourceBundleWrapper load() throws Throwable {
                boolean z2;
                ResourceBundleWrapper resourceBundleWrapperInstantiateBundle;
                ResourceBundleWrapper resourceBundleWrapper;
                ResourceBundleWrapper resourceBundleWrapperInstantiateBundle2;
                ResourceBundleWrapper resourceBundleWrapper2;
                int iLastIndexOf = str2.lastIndexOf(95);
                boolean z3 = true;
                if (iLastIndexOf != -1) {
                    resourceBundleWrapperInstantiateBundle = ResourceBundleWrapper.instantiateBundle(str, str2.substring(0, iLastIndexOf), str3, classLoader, z);
                    z2 = false;
                } else if (str2.isEmpty()) {
                    z2 = false;
                    resourceBundleWrapperInstantiateBundle = null;
                } else {
                    resourceBundleWrapperInstantiateBundle = ResourceBundleWrapper.instantiateBundle(str, "", str3, classLoader, z);
                    z2 = true;
                }
                try {
                    resourceBundleWrapper = new ResourceBundleWrapper((ResourceBundle) classLoader.loadClass(str4).asSubclass(ResourceBundle.class).newInstance());
                    if (resourceBundleWrapperInstantiateBundle != null) {
                        try {
                            resourceBundleWrapper.setParent(resourceBundleWrapperInstantiateBundle);
                        } catch (ClassNotFoundException e) {
                        } catch (Exception e2) {
                            e = e2;
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("failure");
                            }
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println(e);
                            }
                        } catch (NoClassDefFoundError e3) {
                        }
                    }
                    resourceBundleWrapper.baseName = str;
                    resourceBundleWrapper.localeID = str2;
                } catch (ClassNotFoundException e4) {
                    resourceBundleWrapper = null;
                } catch (Exception e5) {
                    e = e5;
                    resourceBundleWrapper = null;
                } catch (NoClassDefFoundError e6) {
                    resourceBundleWrapper = null;
                }
                z3 = false;
                if (z3) {
                    try {
                        final String str6 = str4.replace('.', '/') + ".properties";
                        InputStream inputStream = (InputStream) AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                            @Override
                            public InputStream run() {
                                return classLoader.getResourceAsStream(str6);
                            }
                        });
                        if (inputStream != null) {
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                            try {
                                resourceBundleWrapper2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                                if (resourceBundleWrapperInstantiateBundle != null) {
                                    try {
                                        resourceBundleWrapper2.setParent(resourceBundleWrapperInstantiateBundle);
                                    } catch (Exception e7) {
                                        resourceBundleWrapper = resourceBundleWrapper2;
                                        try {
                                            bufferedInputStream.close();
                                        } catch (Exception e8) {
                                        }
                                    } catch (Throwable th) {
                                        th = th;
                                        try {
                                            bufferedInputStream.close();
                                        } catch (Exception e9) {
                                        }
                                        try {
                                            throw th;
                                        } catch (Exception e10) {
                                            e = e10;
                                            resourceBundleWrapper = resourceBundleWrapper2;
                                            if (ResourceBundleWrapper.DEBUG) {
                                            }
                                            if (ResourceBundleWrapper.DEBUG) {
                                            }
                                            if (resourceBundleWrapper == null) {
                                            }
                                            return resourceBundleWrapper;
                                        }
                                    }
                                }
                                resourceBundleWrapper2.baseName = str;
                                resourceBundleWrapper2.localeID = str2;
                                try {
                                    bufferedInputStream.close();
                                } catch (Exception e11) {
                                }
                                resourceBundleWrapper = resourceBundleWrapper2;
                            } catch (Exception e12) {
                            } catch (Throwable th2) {
                                th = th2;
                                resourceBundleWrapper2 = resourceBundleWrapper;
                            }
                            resourceBundleWrapperInstantiateBundle2 = (resourceBundleWrapper == null || z || str2.isEmpty() || str2.indexOf(95) >= 0 || ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str2)) ? resourceBundleWrapper : ResourceBundleWrapper.instantiateBundle(str, str3, str3, classLoader, z);
                            if (resourceBundleWrapperInstantiateBundle2 != null) {
                                resourceBundleWrapper = resourceBundleWrapperInstantiateBundle2;
                            } else {
                                if (z2) {
                                    try {
                                        if (!z) {
                                        }
                                    } catch (Exception e13) {
                                        e = e13;
                                        resourceBundleWrapper = resourceBundleWrapperInstantiateBundle2;
                                        if (ResourceBundleWrapper.DEBUG) {
                                            System.out.println("failure");
                                        }
                                        if (ResourceBundleWrapper.DEBUG) {
                                            System.out.println(e);
                                        }
                                    }
                                }
                                resourceBundleWrapper = resourceBundleWrapperInstantiateBundle;
                            }
                        } else if (resourceBundleWrapper == null) {
                            if (resourceBundleWrapperInstantiateBundle2 != null) {
                            }
                        }
                    } catch (Exception e14) {
                        e = e14;
                    }
                }
                if (resourceBundleWrapper == null) {
                    resourceBundleWrapper.initKeysVector();
                } else if (ResourceBundleWrapper.DEBUG) {
                    System.out.println("Returning null for " + str + BaseLocale.SEP + str2);
                }
                return resourceBundleWrapper;
            }
        });
    }
}
