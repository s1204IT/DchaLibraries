package android.support.v7.widget;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class TintContextWrapper extends ContextWrapper {
    private static final ArrayList<WeakReference<TintContextWrapper>> sCache = new ArrayList<>();
    private Resources mResources;
    private final Resources.Theme mTheme;

    public static Context wrap(@NonNull Context context) {
        if (shouldWrap(context)) {
            int count = sCache.size();
            for (int i = 0; i < count; i++) {
                WeakReference<TintContextWrapper> ref = sCache.get(i);
                TintContextWrapper wrapper = ref != null ? ref.get() : null;
                if (wrapper != null && wrapper.getBaseContext() == context) {
                    return wrapper;
                }
            }
            TintContextWrapper wrapper2 = new TintContextWrapper(context);
            sCache.add(new WeakReference<>(wrapper2));
            return wrapper2;
        }
        return context;
    }

    private static boolean shouldWrap(@NonNull Context context) {
        if ((context instanceof TintContextWrapper) || (context.getResources() instanceof TintResources) || (context.getResources() instanceof VectorEnabledTintResources)) {
            return false;
        }
        return !AppCompatDelegate.isCompatVectorFromResourcesEnabled() || Build.VERSION.SDK_INT <= 20;
    }

    private TintContextWrapper(@NonNull Context base) {
        super(base);
        if (VectorEnabledTintResources.shouldBeUsed()) {
            this.mTheme = getResources().newTheme();
            this.mTheme.setTo(base.getTheme());
        } else {
            this.mTheme = null;
        }
    }

    @Override
    public Resources.Theme getTheme() {
        return this.mTheme == null ? super.getTheme() : this.mTheme;
    }

    @Override
    public void setTheme(int resid) {
        if (this.mTheme == null) {
            super.setTheme(resid);
        } else {
            this.mTheme.applyStyle(resid, true);
        }
    }

    @Override
    public Resources getResources() {
        Resources vectorEnabledTintResources;
        if (this.mResources == null) {
            if (this.mTheme == null) {
                vectorEnabledTintResources = new TintResources(this, super.getResources());
            } else {
                vectorEnabledTintResources = new VectorEnabledTintResources(this, super.getResources());
            }
            this.mResources = vectorEnabledTintResources;
        }
        return this.mResources;
    }
}
