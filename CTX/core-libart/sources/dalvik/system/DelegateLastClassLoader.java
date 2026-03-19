package dalvik.system;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import sun.misc.CompoundEnumeration;

public final class DelegateLastClassLoader extends PathClassLoader {
    public DelegateLastClassLoader(String str, ClassLoader classLoader) {
        super(str, classLoader);
    }

    public DelegateLastClassLoader(String str, String str2, ClassLoader classLoader) {
        super(str, str2, classLoader);
    }

    @Override
    protected Class<?> loadClass(String str, boolean z) throws ClassNotFoundException {
        Class<?> clsFindLoadedClass = findLoadedClass(str);
        if (clsFindLoadedClass != null) {
            return clsFindLoadedClass;
        }
        try {
            return Object.class.getClassLoader().loadClass(str);
        } catch (ClassNotFoundException e) {
            try {
                return findClass(str);
            } catch (ClassNotFoundException e2) {
                try {
                    return getParent().loadClass(str);
                } catch (ClassNotFoundException e3) {
                    throw e2;
                }
            }
        }
    }

    @Override
    public URL getResource(String str) {
        URL resource = Object.class.getClassLoader().getResource(str);
        if (resource != null) {
            return resource;
        }
        URL urlFindResource = findResource(str);
        if (urlFindResource != null) {
            return urlFindResource;
        }
        ClassLoader parent = getParent();
        if (parent == null) {
            return null;
        }
        return parent.getResource(str);
    }

    @Override
    public Enumeration<URL> getResources(String str) throws IOException {
        Enumeration[] enumerationArr = new Enumeration[3];
        enumerationArr[0] = Object.class.getClassLoader().getResources(str);
        enumerationArr[1] = findResources(str);
        enumerationArr[2] = getParent() == null ? null : getParent().getResources(str);
        return new CompoundEnumeration(enumerationArr);
    }
}
