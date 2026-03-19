package android.filterfw.core;

import android.util.Log;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Iterator;

public class FilterFactory {
    private static FilterFactory mSharedFactory;
    private HashSet<String> mPackages = new HashSet<>();
    private static ClassLoader mCurrentClassLoader = Thread.currentThread().getContextClassLoader();
    private static HashSet<String> mLibraries = new HashSet<>();
    private static Object mClassLoaderGuard = new Object();
    private static final String TAG = "FilterFactory";
    private static boolean mLogVerbose = Log.isLoggable(TAG, 2);

    public static FilterFactory sharedFactory() {
        if (mSharedFactory == null) {
            mSharedFactory = new FilterFactory();
        }
        return mSharedFactory;
    }

    public static void addFilterLibrary(String str) {
        if (mLogVerbose) {
            Log.v(TAG, "Adding filter library " + str);
        }
        synchronized (mClassLoaderGuard) {
            if (mLibraries.contains(str)) {
                if (mLogVerbose) {
                    Log.v(TAG, "Library already added");
                }
            } else {
                mLibraries.add(str);
                mCurrentClassLoader = new PathClassLoader(str, mCurrentClassLoader);
            }
        }
    }

    public void addPackage(String str) {
        if (mLogVerbose) {
            Log.v(TAG, "Adding package " + str);
        }
        this.mPackages.add(str);
    }

    public Filter createFilterByClassName(String str, String str2) throws Throwable {
        if (mLogVerbose) {
            Log.v(TAG, "Looking up class " + str);
        }
        Class<?> cls = null;
        Iterator<String> it = this.mPackages.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            String next = it.next();
            try {
                if (mLogVerbose) {
                    Log.v(TAG, "Trying " + next + "." + str);
                }
            } catch (ClassNotFoundException e) {
            }
            synchronized (mClassLoaderGuard) {
                try {
                    Class<?> clsLoadClass = mCurrentClassLoader.loadClass(next + "." + str);
                    try {
                        if (clsLoadClass == null) {
                            cls = clsLoadClass;
                        } else {
                            cls = clsLoadClass;
                            break;
                        }
                    } catch (Throwable th) {
                        th = th;
                        cls = clsLoadClass;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }

    public Filter createFilterByClass(Class cls, String str) {
        try {
            cls.asSubclass(Filter.class);
            try {
                Constructor constructor = cls.getConstructor(String.class);
                Filter filter = null;
                try {
                    filter = (Filter) constructor.newInstance(str);
                } catch (Throwable th) {
                }
                if (filter == null) {
                    throw new IllegalArgumentException("Could not construct the filter '" + str + "'!");
                }
                return filter;
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("The filter class '" + cls + "' does not have a constructor of the form <init>(String name)!");
            }
        } catch (ClassCastException e2) {
            throw new IllegalArgumentException("Attempting to allocate class '" + cls + "' which is not a subclass of Filter!");
        }
    }
}
