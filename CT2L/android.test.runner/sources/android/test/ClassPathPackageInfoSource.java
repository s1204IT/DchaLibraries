package android.test;

import android.util.Log;
import com.google.android.collect.Maps;
import com.google.android.collect.Sets;
import dalvik.system.DexFile;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPathPackageInfoSource {
    private static final String CLASS_EXTENSION = ".class";
    private static final ClassLoader CLASS_LOADER = ClassPathPackageInfoSource.class.getClassLoader();
    private static String[] apkPaths;
    private ClassLoader classLoader;
    private final SimpleCache<String, ClassPathPackageInfo> cache = new SimpleCache<String, ClassPathPackageInfo>() {
        @Override
        protected ClassPathPackageInfo load(String pkgName) {
            return ClassPathPackageInfoSource.this.createPackageInfo(pkgName);
        }
    };
    private final Map<File, Set<String>> jarFiles = Maps.newHashMap();
    private final String[] classPath = getClassPath();

    ClassPathPackageInfoSource() {
    }

    public static void setApkPaths(String[] apkPaths2) {
        apkPaths = apkPaths2;
    }

    public ClassPathPackageInfo getPackageInfo(String pkgName) {
        return this.cache.get(pkgName);
    }

    private ClassPathPackageInfo createPackageInfo(String packageName) throws Throwable {
        Set<String> subpackageNames = new TreeSet<>();
        Set<String> classNames = new TreeSet<>();
        Set<Class<?>> topLevelClasses = Sets.newHashSet();
        findClasses(packageName, classNames, subpackageNames);
        for (String className : classNames) {
            if (!className.endsWith(".R") && !className.endsWith(".Manifest")) {
                try {
                    topLevelClasses.add(Class.forName(className, false, this.classLoader != null ? this.classLoader : CLASS_LOADER));
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    Log.w("ClassPathPackageInfoSource", "Cannot load class. Make sure it is in your apk. Class name: '" + className + "'. Message: " + e.getMessage(), e);
                }
            }
        }
        return new ClassPathPackageInfo(this, packageName, subpackageNames, topLevelClasses);
    }

    private void findClasses(String packageName, Set<String> classNames, Set<String> subpackageNames) throws Throwable {
        String packagePrefix = packageName + '.';
        packagePrefix.replace('.', '/');
        String[] arr$ = this.classPath;
        for (String entryName : arr$) {
            File classPathEntry = new File(entryName);
            if (classPathEntry.exists()) {
                try {
                    if (entryName.endsWith(".apk")) {
                        findClassesInApk(entryName, packageName, classNames, subpackageNames);
                    } else {
                        String[] arr$2 = apkPaths;
                        for (String apkPath : arr$2) {
                            File file = new File(apkPath);
                            scanForApkFiles(file, packageName, classNames, subpackageNames);
                        }
                    }
                } catch (IOException e) {
                    throw new AssertionError("Can't read classpath entry " + entryName + ": " + e.getMessage());
                }
            }
        }
    }

    private void scanForApkFiles(File source, String packageName, Set<String> classNames, Set<String> subpackageNames) throws Throwable {
        if (source.getPath().endsWith(".apk")) {
            findClassesInApk(source.getPath(), packageName, classNames, subpackageNames);
            return;
        }
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                scanForApkFiles(file, packageName, classNames, subpackageNames);
            }
        }
    }

    private void findClassesInDirectory(File classDir, String packagePrefix, String pathPrefix, Set<String> classNames, Set<String> subpackageNames) throws IOException {
        File directory = new File(classDir, pathPrefix);
        if (directory.exists()) {
            File[] arr$ = directory.listFiles();
            for (File f : arr$) {
                String name = f.getName();
                if (name.endsWith(CLASS_EXTENSION) && isToplevelClass(name)) {
                    classNames.add(packagePrefix + getClassName(name));
                } else if (f.isDirectory()) {
                    subpackageNames.add(packagePrefix + name);
                }
            }
        }
    }

    private void findClassesInJar(File jarFile, String pathPrefix, Set<String> classNames, Set<String> subpackageNames) throws IOException {
        Set<String> entryNames = getJarEntries(jarFile);
        if (entryNames.contains(pathPrefix)) {
            int prefixLength = pathPrefix.length();
            for (String entryName : entryNames) {
                if (entryName.startsWith(pathPrefix) && entryName.endsWith(CLASS_EXTENSION)) {
                    int index = entryName.indexOf(47, prefixLength);
                    if (index >= 0) {
                        String p = entryName.substring(0, index).replace('/', '.');
                        subpackageNames.add(p);
                    } else if (isToplevelClass(entryName)) {
                        classNames.add(getClassName(entryName).replace('/', '.'));
                    }
                }
            }
        }
    }

    private void findClassesInApk(String apkPath, String packageName, Set<String> classNames, Set<String> subpackageNames) throws Throwable {
        DexFile dexFile;
        DexFile dexFile2 = null;
        try {
            dexFile = new DexFile(apkPath);
        } catch (IOException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            Enumeration<String> apkClassNames = dexFile.entries();
            while (apkClassNames.hasMoreElements()) {
                String className = apkClassNames.nextElement();
                if (className.startsWith(packageName)) {
                    String subPackageName = packageName;
                    int lastPackageSeparator = className.lastIndexOf(46);
                    if (lastPackageSeparator > 0) {
                        subPackageName = className.substring(0, lastPackageSeparator);
                    }
                    if (subPackageName.length() > packageName.length()) {
                        subpackageNames.add(subPackageName);
                    } else if (isToplevelClass(className)) {
                        classNames.add(className);
                    }
                }
            }
            if (dexFile != null) {
            }
        } catch (IOException e2) {
            dexFile2 = dexFile;
            if (dexFile2 != null) {
            }
        } catch (Throwable th2) {
            th = th2;
            dexFile2 = dexFile;
            if (dexFile2 != null) {
            }
            throw th;
        }
    }

    private Set<String> getJarEntries(File jarFile) throws IOException {
        Set<String> entryNames = this.jarFiles.get(jarFile);
        if (entryNames == null) {
            entryNames = Sets.newHashSet();
            ZipFile zipFile = new ZipFile(jarFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.endsWith(CLASS_EXTENSION)) {
                    entryNames.add(entryName);
                    int lastIndex = entryName.lastIndexOf(47);
                    do {
                        String packageName = entryName.substring(0, lastIndex + 1);
                        entryNames.add(packageName);
                        lastIndex = entryName.lastIndexOf(47, lastIndex - 1);
                    } while (lastIndex > 0);
                }
            }
            this.jarFiles.put(jarFile, entryNames);
        }
        return entryNames;
    }

    private static boolean isToplevelClass(String fileName) {
        return fileName.indexOf(36) < 0;
    }

    private static String getClassName(String className) {
        int classNameEnd = className.length() - CLASS_EXTENSION.length();
        return className.substring(0, classNameEnd);
    }

    private static String[] getClassPath() {
        String classPath = System.getProperty("java.class.path");
        String separator = System.getProperty("path.separator", ":");
        return classPath.split(Pattern.quote(separator));
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
