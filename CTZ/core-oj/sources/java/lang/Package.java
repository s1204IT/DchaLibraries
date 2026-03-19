package java.lang;

import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import sun.net.www.ParseUtil;
import sun.reflect.CallerSensitive;

public class Package implements AnnotatedElement {
    private final String implTitle;
    private final String implVendor;
    private final String implVersion;
    private final transient ClassLoader loader;
    private transient Class<?> packageInfo;
    private final String pkgName;
    private final URL sealBase;
    private final String specTitle;
    private final String specVendor;
    private final String specVersion;
    private static Map<String, Package> pkgs = new HashMap(31);
    private static Map<String, URL> urls = new HashMap(10);
    private static Map<String, Manifest> mans = new HashMap(10);

    private static native String getSystemPackage0(String str);

    private static native String[] getSystemPackages0();

    public String getName() {
        return this.pkgName;
    }

    public String getSpecificationTitle() {
        return this.specTitle;
    }

    public String getSpecificationVersion() {
        return this.specVersion;
    }

    public String getSpecificationVendor() {
        return this.specVendor;
    }

    public String getImplementationTitle() {
        return this.implTitle;
    }

    public String getImplementationVersion() {
        return this.implVersion;
    }

    public String getImplementationVendor() {
        return this.implVendor;
    }

    public boolean isSealed() {
        return this.sealBase != null;
    }

    public boolean isSealed(URL url) {
        return url.equals(this.sealBase);
    }

    public boolean isCompatibleWith(String str) throws NumberFormatException {
        if (this.specVersion == null || this.specVersion.length() < 1) {
            throw new NumberFormatException("Empty version string");
        }
        String[] strArrSplit = this.specVersion.split("\\.", -1);
        int[] iArr = new int[strArrSplit.length];
        for (int i = 0; i < strArrSplit.length; i++) {
            iArr[i] = Integer.parseInt(strArrSplit[i]);
            if (iArr[i] < 0) {
                throw NumberFormatException.forInputString("" + iArr[i]);
            }
        }
        String[] strArrSplit2 = str.split("\\.", -1);
        int[] iArr2 = new int[strArrSplit2.length];
        for (int i2 = 0; i2 < strArrSplit2.length; i2++) {
            iArr2[i2] = Integer.parseInt(strArrSplit2[i2]);
            if (iArr2[i2] < 0) {
                throw NumberFormatException.forInputString("" + iArr2[i2]);
            }
        }
        int iMax = Math.max(iArr2.length, iArr.length);
        int i3 = 0;
        while (i3 < iMax) {
            int i4 = i3 < iArr2.length ? iArr2[i3] : 0;
            int i5 = i3 < iArr.length ? iArr[i3] : 0;
            if (i5 < i4) {
                return false;
            }
            if (i5 > i4) {
                return true;
            }
            i3++;
        }
        return true;
    }

    @CallerSensitive
    public static Package getPackage(String str) {
        ClassLoader callingClassLoader = VMStack.getCallingClassLoader();
        if (callingClassLoader != null) {
            return callingClassLoader.getPackage(str);
        }
        return getSystemPackage(str);
    }

    @CallerSensitive
    public static Package[] getPackages() {
        ClassLoader callingClassLoader = VMStack.getCallingClassLoader();
        if (callingClassLoader != null) {
            return callingClassLoader.getPackages();
        }
        return getSystemPackages();
    }

    static Package getPackage(Class<?> cls) {
        String name = cls.getName();
        int iLastIndexOf = name.lastIndexOf(46);
        if (iLastIndexOf != -1) {
            String strSubstring = name.substring(0, iLastIndexOf);
            ClassLoader classLoader = cls.getClassLoader();
            if (classLoader != null) {
                return classLoader.getPackage(strSubstring);
            }
            return getSystemPackage(strSubstring);
        }
        return null;
    }

    public int hashCode() {
        return this.pkgName.hashCode();
    }

    public String toString() {
        String str;
        String str2;
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion > 0 && targetSdkVersion <= 24) {
            return "package " + this.pkgName;
        }
        String str3 = this.specTitle;
        String str4 = this.specVersion;
        if (str3 != null && str3.length() > 0) {
            str = ", " + str3;
        } else {
            str = "";
        }
        if (str4 != null && str4.length() > 0) {
            str2 = ", version " + str4;
        } else {
            str2 = "";
        }
        return "package " + this.pkgName + str + str2;
    }

    private Class<?> getPackageInfo() {
        if (this.packageInfo == null) {
            try {
                this.packageInfo = Class.forName(this.pkgName + ".package-info", false, this.loader);
            } catch (ClassNotFoundException e) {
                this.packageInfo = C1PackageInfoProxy.class;
            }
        }
        return this.packageInfo;
    }

    class C1PackageInfoProxy {
        C1PackageInfoProxy() {
        }
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> cls) {
        return (A) getPackageInfo().getAnnotation(cls);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> cls) {
        return super.isAnnotationPresent(cls);
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> cls) {
        return (A[]) getPackageInfo().getAnnotationsByType(cls);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getPackageInfo().getAnnotations();
    }

    @Override
    public <A extends Annotation> A getDeclaredAnnotation(Class<A> cls) {
        return (A) getPackageInfo().getDeclaredAnnotation(cls);
    }

    @Override
    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> cls) {
        return (A[]) getPackageInfo().getDeclaredAnnotationsByType(cls);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getPackageInfo().getDeclaredAnnotations();
    }

    Package(String str, String str2, String str3, String str4, String str5, String str6, String str7, URL url, ClassLoader classLoader) {
        this.pkgName = str;
        this.implTitle = str5;
        this.implVersion = str6;
        this.implVendor = str7;
        this.specTitle = str2;
        this.specVersion = str3;
        this.specVendor = str4;
        this.sealBase = url;
        this.loader = classLoader;
    }

    private Package(String str, Manifest manifest, URL url, ClassLoader classLoader) {
        String value;
        String value2;
        String value3;
        String value4;
        String value5;
        String value6;
        String value7;
        Attributes attributes = manifest.getAttributes(str.replace('.', '/').concat("/"));
        if (attributes != null) {
            value2 = attributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
            value3 = attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
            value4 = attributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            value5 = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            value6 = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            value7 = attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            value = attributes.getValue(Attributes.Name.SEALED);
        } else {
            value = null;
            value2 = null;
            value3 = null;
            value4 = null;
            value5 = null;
            value6 = null;
            value7 = null;
        }
        Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null) {
            value2 = value2 == null ? mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE) : value2;
            value3 = value3 == null ? mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION) : value3;
            value4 = value4 == null ? mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR) : value4;
            value5 = value5 == null ? mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE) : value5;
            value6 = value6 == null ? mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION) : value6;
            value7 = value7 == null ? mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR) : value7;
            if (value == null) {
                value = mainAttributes.getValue(Attributes.Name.SEALED);
            }
        }
        url = "true".equalsIgnoreCase(value) ? url : null;
        this.pkgName = str;
        this.specTitle = value2;
        this.specVersion = value3;
        this.specVendor = value4;
        this.implTitle = value5;
        this.implVersion = value6;
        this.implVendor = value7;
        this.sealBase = url;
        this.loader = classLoader;
    }

    static Package getSystemPackage(String str) {
        Package packageDefineSystemPackage;
        String strConcat;
        String systemPackage0;
        synchronized (pkgs) {
            packageDefineSystemPackage = pkgs.get(str);
            if (packageDefineSystemPackage == null && (systemPackage0 = getSystemPackage0((strConcat = str.replace('.', '/').concat("/")))) != null) {
                packageDefineSystemPackage = defineSystemPackage(strConcat, systemPackage0);
            }
        }
        return packageDefineSystemPackage;
    }

    static Package[] getSystemPackages() {
        Package[] packageArr;
        String[] systemPackages0 = getSystemPackages0();
        synchronized (pkgs) {
            for (int i = 0; i < systemPackages0.length; i++) {
                defineSystemPackage(systemPackages0[i], getSystemPackage0(systemPackages0[i]));
            }
            packageArr = (Package[]) pkgs.values().toArray(new Package[pkgs.size()]);
        }
        return packageArr;
    }

    private static Package defineSystemPackage(final String str, final String str2) {
        return (Package) AccessController.doPrivileged(new PrivilegedAction<Package>() {
            @Override
            public Package run() throws MalformedURLException {
                Package r1;
                String str3 = str;
                URL urlFileToEncodedURL = (URL) Package.urls.get(str2);
                if (urlFileToEncodedURL == null) {
                    File file = new File(str2);
                    try {
                        urlFileToEncodedURL = ParseUtil.fileToEncodedURL(file);
                    } catch (MalformedURLException e) {
                    }
                    if (urlFileToEncodedURL != null) {
                        Package.urls.put(str2, urlFileToEncodedURL);
                        if (file.isFile()) {
                            Package.mans.put(str2, Package.loadManifest(str2));
                        }
                    }
                }
                URL url = urlFileToEncodedURL;
                String strReplace = str3.substring(0, str3.length() - 1).replace('/', '.');
                Manifest manifest = (Manifest) Package.mans.get(str2);
                if (manifest != null) {
                    r1 = new Package(strReplace, manifest, url, null);
                } else {
                    r1 = new Package(strReplace, null, null, null, null, null, null, null, null);
                }
                Package.pkgs.put(strReplace, r1);
                return r1;
            }
        });
    }

    private static Manifest loadManifest(String str) throws Exception {
        Throwable th;
        Throwable th2;
        Throwable th3;
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            try {
                JarInputStream jarInputStream = new JarInputStream(fileInputStream, false);
                try {
                    Manifest manifest = jarInputStream.getManifest();
                    $closeResource(null, jarInputStream);
                    $closeResource(null, fileInputStream);
                    return manifest;
                } catch (Throwable th4) {
                    th = th4;
                    th3 = null;
                    $closeResource(th3, jarInputStream);
                    throw th;
                }
            } catch (Throwable th5) {
                try {
                    throw th5;
                } catch (Throwable th6) {
                    th = th5;
                    th2 = th6;
                    $closeResource(th, fileInputStream);
                    throw th2;
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }
}
