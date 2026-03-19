package android.app;

import android.os.Trace;
import android.util.ArrayMap;
import com.android.internal.os.PathClassLoaderFactory;
import dalvik.system.PathClassLoader;

class ApplicationLoaders {
    private static final ApplicationLoaders gApplicationLoaders = new ApplicationLoaders();
    private final ArrayMap<String, ClassLoader> mLoaders = new ArrayMap<>();

    private static native void setupVulkanLayerPath(ClassLoader classLoader, String str);

    ApplicationLoaders() {
    }

    public static ApplicationLoaders getDefault() {
        return gApplicationLoaders;
    }

    public ClassLoader getClassLoader(String zip, int targetSdkVersion, boolean isBundled, String librarySearchPath, String libraryPermittedPath, ClassLoader parent) {
        ClassLoader baseParent = ClassLoader.getSystemClassLoader().getParent();
        synchronized (this.mLoaders) {
            if (parent == null) {
                parent = baseParent;
            }
            if (parent == baseParent) {
                ClassLoader loader = this.mLoaders.get(zip);
                if (loader != null) {
                    return loader;
                }
                Trace.traceBegin(64L, zip);
                PathClassLoader pathClassloader = PathClassLoaderFactory.createClassLoader(zip, librarySearchPath, libraryPermittedPath, parent, targetSdkVersion, isBundled);
                Trace.traceEnd(64L);
                Trace.traceBegin(64L, "setupVulkanLayerPath");
                setupVulkanLayerPath(pathClassloader, librarySearchPath);
                Trace.traceEnd(64L);
                this.mLoaders.put(zip, pathClassloader);
                return pathClassloader;
            }
            Trace.traceBegin(64L, zip);
            PathClassLoader pathClassloader2 = new PathClassLoader(zip, parent);
            Trace.traceEnd(64L);
            return pathClassloader2;
        }
    }

    void addPath(ClassLoader classLoader, String dexPath) {
        if (!(classLoader instanceof PathClassLoader)) {
            throw new IllegalStateException("class loader is not a PathClassLoader");
        }
        PathClassLoader baseDexClassLoader = (PathClassLoader) classLoader;
        baseDexClassLoader.addDexPath(dexPath);
    }
}
