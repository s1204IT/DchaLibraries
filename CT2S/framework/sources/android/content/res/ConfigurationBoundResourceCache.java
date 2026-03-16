package android.content.res;

import android.content.res.Resources;
import android.net.ProxyInfo;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import java.lang.ref.WeakReference;

public class ConfigurationBoundResourceCache<T> {
    private final ArrayMap<String, LongSparseArray<WeakReference<ConstantState<T>>>> mCache = new ArrayMap<>();
    final Resources mResources;

    public ConfigurationBoundResourceCache(Resources resources) {
        this.mResources = resources;
    }

    public void put(long key, Resources.Theme theme, ConstantState<T> constantState) {
        if (constantState != null) {
            String themeKey = theme == null ? ProxyInfo.LOCAL_EXCL_LIST : theme.getKey();
            synchronized (this) {
                LongSparseArray<WeakReference<ConstantState<T>>> themedCache = this.mCache.get(themeKey);
                if (themedCache == null) {
                    themedCache = new LongSparseArray<>(1);
                    this.mCache.put(themeKey, themedCache);
                }
                themedCache.put(key, new WeakReference<>(constantState));
            }
        }
    }

    public T get(long key, Resources.Theme theme) {
        String themeKey = theme != null ? theme.getKey() : ProxyInfo.LOCAL_EXCL_LIST;
        synchronized (this) {
            LongSparseArray<WeakReference<ConstantState<T>>> themedCache = this.mCache.get(themeKey);
            if (themedCache == null) {
                return null;
            }
            WeakReference<ConstantState<T>> wr = themedCache.get(key);
            if (wr == null) {
                return null;
            }
            ConstantState<T> constantState = wr.get();
            if (constantState != null) {
                return constantState.newInstance(this.mResources, theme);
            }
            synchronized (this) {
                themedCache.delete(key);
            }
            return null;
        }
    }

    public void onConfigurationChange(int configChanges) {
        synchronized (this) {
            int size = this.mCache.size();
            for (int i = size - 1; i >= 0; i--) {
                LongSparseArray<WeakReference<ConstantState<T>>> themeCache = this.mCache.valueAt(i);
                onConfigurationChangeInt(themeCache, configChanges);
                if (themeCache.size() == 0) {
                    this.mCache.removeAt(i);
                }
            }
        }
    }

    private void onConfigurationChangeInt(LongSparseArray<WeakReference<ConstantState<T>>> themeCache, int configChanges) {
        int size = themeCache.size();
        for (int i = size - 1; i >= 0; i--) {
            WeakReference<ConstantState<T>> wr = themeCache.valueAt(i);
            ConstantState<T> constantState = wr.get();
            if (constantState == null || Configuration.needNewResources(configChanges, constantState.getChangingConfigurations())) {
                themeCache.removeAt(i);
            }
        }
    }
}
