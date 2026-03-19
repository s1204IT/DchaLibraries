package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import java.lang.ref.WeakReference;

public final class AttributeCache {
    private static AttributeCache sInstance = null;
    private final Context mContext;

    @GuardedBy("this")
    private final ArrayMap<String, WeakReference<Package>> mPackages = new ArrayMap<>();

    @GuardedBy("this")
    private final Configuration mConfiguration = new Configuration();

    public static final class Package {
        public final Context context;
        private final SparseArray<ArrayMap<int[], Entry>> mMap = new SparseArray<>();

        public Package(Context c) {
            this.context = c;
        }
    }

    public static final class Entry {
        public final TypedArray array;
        public final Context context;

        public Entry(Context c, TypedArray ta) {
            this.context = c;
            this.array = ta;
        }

        void recycle() {
            if (this.array == null) {
                return;
            }
            this.array.recycle();
        }
    }

    public static void init(Context context) {
        if (sInstance != null) {
            return;
        }
        sInstance = new AttributeCache(context);
    }

    public static AttributeCache instance() {
        return sInstance;
    }

    public AttributeCache(Context context) {
        this.mContext = context;
    }

    public void removePackage(String packageName) {
        synchronized (this) {
            WeakReference<Package> ref = this.mPackages.remove(packageName);
            Package r3 = ref != null ? ref.get() : null;
            if (r3 != null) {
                if (r3.mMap != null) {
                    for (int i = 0; i < r3.mMap.size(); i++) {
                        ArrayMap<int[], Entry> map = (ArrayMap) r3.mMap.valueAt(i);
                        for (int j = 0; j < map.size(); j++) {
                            map.valueAt(j).recycle();
                        }
                    }
                }
                Resources res = r3.context.getResources();
                res.flushLayoutCache();
            }
        }
    }

    public void updateConfiguration(Configuration config) {
        synchronized (this) {
            int changes = this.mConfiguration.updateFrom(config);
            if (((-1073741985) & changes) != 0) {
                this.mPackages.clear();
            }
        }
    }

    public Entry get(String packageName, int resId, int[] styleable, int userId) {
        Entry ent;
        synchronized (this) {
            WeakReference<Package> ref = this.mPackages.get(packageName);
            Package pkg = ref != null ? ref.get() : null;
            ArrayMap<int[], Entry> map = null;
            Entry ent2 = null;
            if (pkg != null) {
                map = (ArrayMap) pkg.mMap.get(resId);
                if (map != null) {
                    Entry ent3 = map.get(styleable);
                    ent2 = ent3;
                    if (ent2 != null) {
                        return ent2;
                    }
                }
                ent = ent2;
            } else {
                try {
                    Context context = this.mContext.createPackageContextAsUser(packageName, 0, new UserHandle(userId));
                    if (context == null) {
                        return null;
                    }
                    pkg = new Package(context);
                    this.mPackages.put(packageName, new WeakReference<>(pkg));
                    ent = null;
                } catch (PackageManager.NameNotFoundException e) {
                    return null;
                }
            }
            if (map == null) {
                map = new ArrayMap<>();
                pkg.mMap.put(resId, map);
            }
            try {
                Entry ent4 = new Entry(pkg.context, pkg.context.obtainStyledAttributes(resId, styleable));
                try {
                    map.put(styleable, ent4);
                    return ent4;
                } catch (Resources.NotFoundException e2) {
                    return null;
                }
            } catch (Resources.NotFoundException e3) {
            }
        }
    }
}
