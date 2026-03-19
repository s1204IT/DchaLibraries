package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.LruCache;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;

public final class AttributeCache {
    private static final int CACHE_SIZE = 4;
    private static AttributeCache sInstance = null;
    private final Context mContext;

    @GuardedBy("this")
    private final LruCache<String, Package> mPackages = new LruCache<>(4);

    @GuardedBy("this")
    private final Configuration mConfiguration = new Configuration();

    public static final class Package {
        public final Context context;
        private final SparseArray<ArrayMap<int[], Entry>> mMap = new SparseArray<>();

        public Package(Context context) {
            this.context = context;
        }
    }

    public static final class Entry {
        public final TypedArray array;
        public final Context context;

        public Entry(Context context, TypedArray typedArray) {
            this.context = context;
            this.array = typedArray;
        }

        void recycle() {
            if (this.array != null) {
                this.array.recycle();
            }
        }
    }

    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new AttributeCache(context);
        }
    }

    public static AttributeCache instance() {
        return sInstance;
    }

    public AttributeCache(Context context) {
        this.mContext = context;
    }

    public void removePackage(String str) {
        synchronized (this) {
            Package packageRemove = this.mPackages.remove(str);
            if (packageRemove != null) {
                for (int i = 0; i < packageRemove.mMap.size(); i++) {
                    ArrayMap arrayMap = (ArrayMap) packageRemove.mMap.valueAt(i);
                    for (int i2 = 0; i2 < arrayMap.size(); i2++) {
                        ((Entry) arrayMap.valueAt(i2)).recycle();
                    }
                }
                packageRemove.context.getResources().flushLayoutCache();
            }
        }
    }

    public void updateConfiguration(Configuration configuration) {
        synchronized (this) {
            if ((this.mConfiguration.updateFrom(configuration) & (-1073741985)) != 0) {
                this.mPackages.evictAll();
            }
        }
    }

    public Entry get(String str, int i, int[] iArr, int i2) {
        ArrayMap arrayMap;
        Entry entry;
        synchronized (this) {
            Package r0 = this.mPackages.get(str);
            if (r0 != null) {
                arrayMap = (ArrayMap) r0.mMap.get(i);
                if (arrayMap != null && (entry = (Entry) arrayMap.get(iArr)) != null) {
                    return entry;
                }
            } else {
                try {
                    Context contextCreatePackageContextAsUser = this.mContext.createPackageContextAsUser(str, 0, new UserHandle(i2));
                    if (contextCreatePackageContextAsUser == null) {
                        return null;
                    }
                    r0 = new Package(contextCreatePackageContextAsUser);
                    this.mPackages.put(str, r0);
                    arrayMap = null;
                } catch (PackageManager.NameNotFoundException e) {
                    return null;
                }
            }
            if (arrayMap == null) {
                arrayMap = new ArrayMap();
                r0.mMap.put(i, arrayMap);
            }
            try {
                Entry entry2 = new Entry(r0.context, r0.context.obtainStyledAttributes(i, iArr));
                arrayMap.put(iArr, entry2);
                return entry2;
            } catch (Resources.NotFoundException e2) {
                return null;
            }
        }
    }
}
