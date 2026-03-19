package android.content.res;

import android.content.res.Resources;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import java.lang.ref.WeakReference;

abstract class ThemedResourceCache<T> {
    private LongSparseArray<WeakReference<T>> mNullThemedEntries;
    private ArrayMap<Resources.ThemeKey, LongSparseArray<WeakReference<T>>> mThemedEntries;
    private LongSparseArray<WeakReference<T>> mUnthemedEntries;

    protected abstract boolean shouldInvalidateEntry(T t, int i);

    ThemedResourceCache() {
    }

    public void put(long j, Resources.Theme theme, T t) {
        put(j, theme, t, true);
    }

    public void put(long j, Resources.Theme theme, T t, boolean z) {
        LongSparseArray<WeakReference<T>> themedLocked;
        if (t == null) {
            return;
        }
        synchronized (this) {
            try {
                if (!z) {
                    themedLocked = getUnthemedLocked(true);
                } else {
                    themedLocked = getThemedLocked(theme, true);
                }
                if (themedLocked != null) {
                    themedLocked.put(j, new WeakReference<>(t));
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public T get(long j, Resources.Theme theme) {
        WeakReference<T> weakReference;
        WeakReference<T> weakReference2;
        synchronized (this) {
            LongSparseArray<WeakReference<T>> themedLocked = getThemedLocked(theme, false);
            if (themedLocked != null && (weakReference2 = themedLocked.get(j)) != null) {
                return weakReference2.get();
            }
            LongSparseArray<WeakReference<T>> unthemedLocked = getUnthemedLocked(false);
            if (unthemedLocked != null && (weakReference = unthemedLocked.get(j)) != null) {
                return weakReference.get();
            }
            return null;
        }
    }

    public void onConfigurationChange(int i) {
        prune(i);
    }

    private LongSparseArray<WeakReference<T>> getThemedLocked(Resources.Theme theme, boolean z) {
        if (theme == null) {
            if (this.mNullThemedEntries == null && z) {
                this.mNullThemedEntries = new LongSparseArray<>(1);
            }
            return this.mNullThemedEntries;
        }
        if (this.mThemedEntries == null) {
            if (z) {
                this.mThemedEntries = new ArrayMap<>(1);
            } else {
                return null;
            }
        }
        Resources.ThemeKey key = theme.getKey();
        LongSparseArray<WeakReference<T>> longSparseArray = this.mThemedEntries.get(key);
        if (longSparseArray == null && z) {
            LongSparseArray<WeakReference<T>> longSparseArray2 = new LongSparseArray<>(1);
            this.mThemedEntries.put(key.m18clone(), longSparseArray2);
            return longSparseArray2;
        }
        return longSparseArray;
    }

    private LongSparseArray<WeakReference<T>> getUnthemedLocked(boolean z) {
        if (this.mUnthemedEntries == null && z) {
            this.mUnthemedEntries = new LongSparseArray<>(1);
        }
        return this.mUnthemedEntries;
    }

    private boolean prune(int i) {
        boolean z;
        synchronized (this) {
            z = true;
            if (this.mThemedEntries != null) {
                for (int size = this.mThemedEntries.size() - 1; size >= 0; size--) {
                    if (pruneEntriesLocked(this.mThemedEntries.valueAt(size), i)) {
                        this.mThemedEntries.removeAt(size);
                    }
                }
            }
            pruneEntriesLocked(this.mNullThemedEntries, i);
            pruneEntriesLocked(this.mUnthemedEntries, i);
            if (this.mThemedEntries != null || this.mNullThemedEntries != null || this.mUnthemedEntries != null) {
                z = false;
            }
        }
        return z;
    }

    private boolean pruneEntriesLocked(LongSparseArray<WeakReference<T>> longSparseArray, int i) {
        if (longSparseArray == null) {
            return true;
        }
        for (int size = longSparseArray.size() - 1; size >= 0; size--) {
            WeakReference<T> weakReferenceValueAt = longSparseArray.valueAt(size);
            if (weakReferenceValueAt == null || pruneEntryLocked(weakReferenceValueAt.get(), i)) {
                longSparseArray.removeAt(size);
            }
        }
        return longSparseArray.size() == 0;
    }

    private boolean pruneEntryLocked(T t, int i) {
        return t == null || (i != 0 && shouldInvalidateEntry(t, i));
    }
}
