package sun.security.jca;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.ProviderException;
import sun.security.util.Debug;
import sun.security.util.PropertyExpander;

final class ProviderConfig {
    private static final int MAX_LOAD_TRIES = 30;
    private static final String P11_SOL_ARG = "${java.home}/lib/security/sunpkcs11-solaris.cfg";
    private static final String P11_SOL_NAME = "sun.security.pkcs11.SunPKCS11";
    private final String argument;
    private final String className;
    private boolean isLoading;
    private volatile Provider provider;
    private int tries;
    private static final Debug debug = Debug.getInstance("jca", "ProviderConfig");
    private static final Class[] CL_STRING = {String.class};

    ProviderConfig(String str, String str2) {
        if (str.equals(P11_SOL_NAME) && str2.equals(P11_SOL_ARG)) {
            checkSunPKCS11Solaris();
        }
        this.className = str;
        this.argument = expand(str2);
    }

    ProviderConfig(String str) {
        this(str, "");
    }

    ProviderConfig(Provider provider) {
        this.className = provider.getClass().getName();
        this.argument = "";
        this.provider = provider;
    }

    private void checkSunPKCS11Solaris() {
        if (((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                if (!new File("/usr/lib/libpkcs11.so").exists()) {
                    return Boolean.FALSE;
                }
                if ("false".equalsIgnoreCase(System.getProperty("sun.security.pkcs11.enable-solaris"))) {
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
        })) == Boolean.FALSE) {
            this.tries = 30;
        }
    }

    private boolean hasArgument() {
        return this.argument.length() != 0;
    }

    private boolean shouldLoad() {
        return this.tries < 30;
    }

    private void disableLoad() {
        this.tries = 30;
    }

    boolean isLoaded() {
        return this.provider != null;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProviderConfig)) {
            return false;
        }
        ProviderConfig providerConfig = (ProviderConfig) obj;
        return this.className.equals(providerConfig.className) && this.argument.equals(providerConfig.argument);
    }

    public int hashCode() {
        return this.className.hashCode() + this.argument.hashCode();
    }

    public String toString() {
        if (hasArgument()) {
            return this.className + "('" + this.argument + "')";
        }
        return this.className;
    }

    synchronized Provider getProvider() {
        Provider provider = this.provider;
        if (provider != null) {
            return provider;
        }
        if (!shouldLoad()) {
            return null;
        }
        if (this.isLoading) {
            if (debug != null) {
                debug.println("Recursion loading provider: " + ((Object) this));
                new Exception("Call trace").printStackTrace();
            }
            return null;
        }
        try {
            this.isLoading = true;
            this.tries++;
            Provider providerDoLoadProvider = doLoadProvider();
            this.isLoading = false;
            this.provider = providerDoLoadProvider;
            return providerDoLoadProvider;
        } catch (Throwable th) {
            this.isLoading = false;
            throw th;
        }
    }

    private Provider doLoadProvider() {
        return (Provider) AccessController.doPrivileged(new PrivilegedAction<Provider>() {
            @Override
            public Provider run() {
                if (ProviderConfig.debug != null) {
                    ProviderConfig.debug.println("Loading provider: " + ((Object) ProviderConfig.this));
                }
                try {
                    return ProviderConfig.this.initProvider(ProviderConfig.this.className, Object.class.getClassLoader());
                } catch (Exception e) {
                    try {
                        return ProviderConfig.this.initProvider(ProviderConfig.this.className, ClassLoader.getSystemClassLoader());
                    } catch (Exception e2) {
                        e = e2;
                        if (e instanceof InvocationTargetException) {
                            e = ((InvocationTargetException) e).getCause();
                        }
                        if (ProviderConfig.debug != null) {
                            ProviderConfig.debug.println("Error loading provider " + ((Object) ProviderConfig.this));
                            e.printStackTrace();
                        }
                        if (e instanceof ProviderException) {
                            throw ((ProviderException) e);
                        }
                        if (e instanceof UnsupportedOperationException) {
                            ProviderConfig.this.disableLoad();
                            return null;
                        }
                        return null;
                    }
                }
            }
        });
    }

    private Provider initProvider(String str, ClassLoader classLoader) throws Exception {
        Class<?> cls;
        Object objNewInstance;
        if (classLoader != null) {
            cls = classLoader.loadClass(str);
        } else {
            cls = Class.forName(str);
        }
        if (!hasArgument()) {
            objNewInstance = cls.newInstance();
        } else {
            objNewInstance = cls.getConstructor(CL_STRING).newInstance(this.argument);
        }
        if (objNewInstance instanceof Provider) {
            if (debug != null) {
                debug.println("Loaded provider " + objNewInstance);
            }
            return (Provider) objNewInstance;
        }
        if (debug != null) {
            debug.println(str + " is not a provider");
        }
        disableLoad();
        return null;
    }

    private static String expand(final String str) {
        if (!str.contains("${")) {
            return str;
        }
        return (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    return PropertyExpander.expand(str);
                } catch (GeneralSecurityException e) {
                    throw new ProviderException(e);
                }
            }
        });
    }
}
