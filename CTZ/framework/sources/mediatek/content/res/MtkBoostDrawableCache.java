package mediatek.content.res;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import android.util.Slog;
import java.util.Iterator;

public class MtkBoostDrawableCache {
    private static final boolean DEBUG_CONFIG = false;
    static final String TAG = "MtkBoostDrawableCache";
    private static final ArrayMap<String, LongSparseArray<Drawable.ConstantState>> sBoostDrawableCache = new ArrayMap<>();
    private String mBoostKey = "";

    public void onConfigurationChange(int i) {
        LongSparseArray<Drawable.ConstantState> longSparseArray;
        if (isBoostApp(this.mBoostKey) && (longSparseArray = sBoostDrawableCache.get(this.mBoostKey)) != null) {
            clearBoostDrawableCacheLocked(longSparseArray, i);
            Slog.w(TAG, "Clear boost cache");
        }
    }

    public void clearBoostDrawableCacheLocked(LongSparseArray<Drawable.ConstantState> longSparseArray, int i) {
        int size = longSparseArray.size();
        for (int i2 = 0; i2 < size; i2++) {
            Drawable.ConstantState constantStateValueAt = longSparseArray.valueAt(i2);
            if (constantStateValueAt != null && Configuration.needNewResources(i, constantStateValueAt.getChangingConfigurations())) {
                longSparseArray.setValueAt(i2, null);
            }
        }
    }

    public Drawable getBoostCachedDrawable(Resources resources, long j) {
        LongSparseArray<Drawable.ConstantState> longSparseArray;
        Drawable boostCachedDrawableLocked;
        this.mBoostKey = resources.toString().split("@")[0];
        String str = this.mBoostKey;
        if (isBoostApp(str) && (longSparseArray = sBoostDrawableCache.get(str)) != null && (boostCachedDrawableLocked = getBoostCachedDrawableLocked(resources, j, longSparseArray)) != null) {
            return boostCachedDrawableLocked;
        }
        return null;
    }

    public Drawable getBoostCachedDrawableLocked(Resources resources, long j, LongSparseArray<Drawable.ConstantState> longSparseArray) {
        Drawable.ConstantState constantState = longSparseArray.get(j);
        if (constantState != null) {
            return constantState.newDrawable(resources);
        }
        longSparseArray.delete(j);
        return null;
    }

    public boolean isBoostApp(String str) {
        if (str.equals("android.content.res.Resources")) {
            return false;
        }
        for (String str2 : new String[]{"com.tencent.mm"}) {
            if (str.contains(str2)) {
                return true;
            }
        }
        return false;
    }

    public void putBoostCache(long j, Drawable.ConstantState constantState) {
        if (isBoostApp(this.mBoostKey)) {
            LongSparseArray<Drawable.ConstantState> longSparseArray = sBoostDrawableCache.get(this.mBoostKey);
            if (longSparseArray == null) {
                longSparseArray = new LongSparseArray<>(1);
                sBoostDrawableCache.put(this.mBoostKey, longSparseArray);
                Iterator<String> it = sBoostDrawableCache.keySet().iterator();
                while (it.hasNext()) {
                    Slog.w(TAG, "ResourceKey:" + it.next());
                }
            }
            longSparseArray.put(j, constantState);
            Slog.w(TAG, "CacheKey:" + j + " Resource:" + this.mBoostKey);
        }
    }
}
