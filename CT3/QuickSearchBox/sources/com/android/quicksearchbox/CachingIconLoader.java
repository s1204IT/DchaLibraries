package com.android.quicksearchbox;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import com.android.quicksearchbox.util.CachedLater;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Now;
import com.android.quicksearchbox.util.NowOrLater;
import com.android.quicksearchbox.util.NowOrLaterWrapper;
import java.util.WeakHashMap;

public class CachingIconLoader implements IconLoader {
    private final WeakHashMap<String, Entry> mIconCache = new WeakHashMap<>();
    private final IconLoader mWrapped;

    public CachingIconLoader(IconLoader wrapped) {
        this.mWrapped = wrapped;
    }

    @Override
    public NowOrLater<Drawable> getIcon(String drawableId) throws Throwable {
        if (TextUtils.isEmpty(drawableId) || "0".equals(drawableId)) {
            return new Now(null);
        }
        Entry entry = null;
        synchronized (this) {
            try {
                NowOrLater<Drawable.ConstantState> drawableState = queryCache(drawableId);
                if (drawableState == null) {
                    Entry newEntry = new Entry();
                    try {
                        storeInIconCache(drawableId, newEntry);
                        entry = newEntry;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (drawableState != null) {
                    return new NowOrLaterWrapper<Drawable.ConstantState, Drawable>(drawableState) {
                        @Override
                        public Drawable get(Drawable.ConstantState value) {
                            if (value == null) {
                                return null;
                            }
                            return value.newDrawable();
                        }
                    };
                }
                NowOrLater<Drawable> drawable = this.mWrapped.getIcon(drawableId);
                entry.set(drawable);
                storeInIconCache(drawableId, entry);
                return drawable;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public Uri getIconUri(String drawableId) {
        return this.mWrapped.getIconUri(drawableId);
    }

    private synchronized NowOrLater<Drawable.ConstantState> queryCache(String drawableId) {
        NowOrLater<Drawable.ConstantState> cached;
        cached = this.mIconCache.get(drawableId);
        return cached;
    }

    private synchronized void storeInIconCache(String resourceUri, Entry drawable) {
        if (drawable != null) {
            this.mIconCache.put(resourceUri, drawable);
        }
    }

    private static class Entry extends CachedLater<Drawable.ConstantState> implements Consumer<Drawable> {
        private boolean mCreateRequested;
        private NowOrLater<Drawable> mDrawable;
        private boolean mGotDrawable;

        public synchronized void set(NowOrLater<Drawable> drawable) {
            if (this.mGotDrawable) {
                throw new IllegalStateException("set() may only be called once.");
            }
            this.mGotDrawable = true;
            this.mDrawable = drawable;
            if (this.mCreateRequested) {
                getLater();
            }
        }

        @Override
        protected synchronized void create() {
            if (!this.mCreateRequested) {
                this.mCreateRequested = true;
                if (this.mGotDrawable) {
                    getLater();
                }
            }
        }

        private void getLater() {
            NowOrLater<Drawable> drawable = this.mDrawable;
            this.mDrawable = null;
            drawable.getLater(this);
        }

        @Override
        public boolean consume(Drawable value) {
            store(value != null ? value.getConstantState() : null);
            return true;
        }
    }
}
