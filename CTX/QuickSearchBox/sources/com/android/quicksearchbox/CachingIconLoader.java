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

    private static class Entry extends CachedLater<Drawable.ConstantState> implements Consumer<Drawable> {
        private boolean mCreateRequested;
        private NowOrLater<Drawable> mDrawable;
        private boolean mGotDrawable;

        private void getLater() {
            NowOrLater<Drawable> nowOrLater = this.mDrawable;
            this.mDrawable = null;
            nowOrLater.getLater(this);
        }

        @Override
        public boolean consume(Drawable drawable) {
            store(drawable == null ? null : drawable.getConstantState());
            return true;
        }

        @Override
        protected void create() {
            synchronized (this) {
                if (!this.mCreateRequested) {
                    this.mCreateRequested = true;
                    if (this.mGotDrawable) {
                        getLater();
                    }
                }
            }
        }

        public void set(NowOrLater<Drawable> nowOrLater) {
            synchronized (this) {
                if (this.mGotDrawable) {
                    throw new IllegalStateException("set() may only be called once.");
                }
                this.mGotDrawable = true;
                this.mDrawable = nowOrLater;
                if (this.mCreateRequested) {
                    getLater();
                }
            }
        }
    }

    public CachingIconLoader(IconLoader iconLoader) {
        this.mWrapped = iconLoader;
    }

    private NowOrLater<Drawable.ConstantState> queryCache(String str) {
        Entry entry;
        synchronized (this) {
            entry = this.mIconCache.get(str);
        }
        return entry;
    }

    private void storeInIconCache(String str, Entry entry) {
        synchronized (this) {
            if (entry != null) {
                this.mIconCache.put(str, entry);
            }
        }
    }

    @Override
    public NowOrLater<Drawable> getIcon(String str) {
        NowOrLater<Drawable.ConstantState> nowOrLaterQueryCache;
        Entry entry = null;
        if (TextUtils.isEmpty(str) || "0".equals(str)) {
            return new Now(null);
        }
        synchronized (this) {
            nowOrLaterQueryCache = queryCache(str);
            if (nowOrLaterQueryCache == null) {
                Entry entry2 = new Entry();
                storeInIconCache(str, entry2);
                entry = entry2;
            }
        }
        if (nowOrLaterQueryCache != null) {
            return new NowOrLaterWrapper<Drawable.ConstantState, Drawable>(this, nowOrLaterQueryCache) {
                final CachingIconLoader this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public Drawable get(Drawable.ConstantState constantState) {
                    if (constantState == null) {
                        return null;
                    }
                    return constantState.newDrawable();
                }
            };
        }
        NowOrLater<Drawable> icon = this.mWrapped.getIcon(str);
        entry.set(icon);
        storeInIconCache(str, entry);
        return icon;
    }

    @Override
    public Uri getIconUri(String str) {
        return this.mWrapped.getIconUri(str);
    }
}
