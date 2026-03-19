package android.content.res;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import java.lang.ref.WeakReference;

public class CompatResources extends Resources {
    private WeakReference<Context> mContext;

    public CompatResources(ClassLoader classLoader) {
        super(classLoader);
        this.mContext = new WeakReference<>(null);
    }

    public void setContext(Context context) {
        this.mContext = new WeakReference<>(context);
    }

    @Override
    public Drawable getDrawable(int i) throws Resources.NotFoundException {
        return getDrawable(i, getTheme());
    }

    @Override
    public Drawable getDrawableForDensity(int i, int i2) throws Resources.NotFoundException {
        return getDrawableForDensity(i, i2, getTheme());
    }

    @Override
    public int getColor(int i) throws Resources.NotFoundException {
        return getColor(i, getTheme());
    }

    @Override
    public ColorStateList getColorStateList(int i) throws Resources.NotFoundException {
        return getColorStateList(i, getTheme());
    }

    private Resources.Theme getTheme() {
        Context context = this.mContext.get();
        if (context != null) {
            return context.getTheme();
        }
        return null;
    }
}
