package android.app;

import android.os.Trace;
import android.util.ArrayMap;
import dalvik.system.PathClassLoader;

class ApplicationLoaders {
    private static final ApplicationLoaders gApplicationLoaders = new ApplicationLoaders();
    private final ArrayMap<String, ClassLoader> mLoaders = new ArrayMap<>();

    ApplicationLoaders() {
    }

    public static ApplicationLoaders getDefault() {
        return gApplicationLoaders;
    }

    public ClassLoader getClassLoader(String zip, String libPath, ClassLoader parent) {
        ClassLoader pathClassLoader;
        ClassLoader baseParent = ClassLoader.getSystemClassLoader().getParent();
        synchronized (this.mLoaders) {
            if (parent == null) {
                parent = baseParent;
            }
            if (parent == baseParent) {
                ClassLoader loader = this.mLoaders.get(zip);
                if (loader != null) {
                    pathClassLoader = loader;
                } else {
                    Trace.traceBegin(64L, zip);
                    pathClassLoader = new PathClassLoader(zip, libPath, parent);
                    Trace.traceEnd(64L);
                    this.mLoaders.put(zip, pathClassLoader);
                }
            } else {
                Trace.traceBegin(64L, zip);
                pathClassLoader = new PathClassLoader(zip, parent);
                Trace.traceEnd(64L);
            }
        }
        return pathClassLoader;
    }
}
