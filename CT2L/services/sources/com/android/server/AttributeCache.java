package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.UserHandle;
import android.util.SparseArray;
import java.util.HashMap;
import java.util.WeakHashMap;

public final class AttributeCache {
    private static AttributeCache sInstance = null;
    private final Context mContext;
    private final WeakHashMap<String, Package> mPackages = new WeakHashMap<>();
    private final Configuration mConfiguration = new Configuration();

    public static final class Package {
        public final Context context;
        private final SparseArray<HashMap<int[], Entry>> mMap = new SparseArray<>();

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

    public void removePackage(String packageName) {
        synchronized (this) {
            this.mPackages.remove(packageName);
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
            Package pkg = this.mPackages.get(packageName);
            HashMap<int[], Entry> map = null;
            Entry ent2 = null;
            if (pkg != null) {
                map = (HashMap) pkg.mMap.get(resId);
                if (map != null && (ent2 = map.get(styleable)) != null) {
                    return ent2;
                }
            } else {
                try {
                    Context context = this.mContext.createPackageContextAsUser(packageName, 0, new UserHandle(userId));
                    if (context == null) {
                        return null;
                    }
                    pkg = new Package(context);
                    this.mPackages.put(packageName, pkg);
                } catch (PackageManager.NameNotFoundException e) {
                    return null;
                }
            }
            if (map == null) {
                map = new HashMap<>();
                pkg.mMap.put(resId, map);
            }
            try {
                ent = new Entry(pkg.context, pkg.context.obtainStyledAttributes(resId, styleable));
            } catch (Resources.NotFoundException e2) {
            }
            try {
                map.put(styleable, ent);
                return ent;
            } catch (Resources.NotFoundException e3) {
                return null;
            }
        }
    }
}
