package com.android.quickstep;

import android.R;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.LruCache;
import android.util.SparseArray;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.graphics.BitmapInfo;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.systemui.shared.recents.model.IconLoader;
import com.android.systemui.shared.recents.model.TaskKeyLruCache;

@TargetApi(26)
/* loaded from: classes.dex */
public class NormalizedIconLoader extends IconLoader {
    private final SparseArray<BitmapInfo> mDefaultIcons;
    private final DrawableFactory mDrawableFactory;
    private LauncherIcons mLauncherIcons;

    public NormalizedIconLoader(Context context, TaskKeyLruCache<Drawable> taskKeyLruCache, LruCache<ComponentName, ActivityInfo> lruCache) {
        super(context, taskKeyLruCache, lruCache);
        this.mDefaultIcons = new SparseArray<>();
        this.mDrawableFactory = DrawableFactory.get(context);
    }

    @Override // com.android.systemui.shared.recents.model.IconLoader
    public Drawable getDefaultIcon(int i) {
        FastBitmapDrawable fastBitmapDrawable;
        synchronized (this.mDefaultIcons) {
            BitmapInfo bitmapInfo = this.mDefaultIcons.get(i);
            if (bitmapInfo == null) {
                bitmapInfo = getBitmapInfo(Resources.getSystem().getDrawable(R.drawable.sym_def_app_icon), i, 0, false);
                this.mDefaultIcons.put(i, bitmapInfo);
            }
            fastBitmapDrawable = new FastBitmapDrawable(bitmapInfo);
        }
        return fastBitmapDrawable;
    }

    @Override // com.android.systemui.shared.recents.model.IconLoader
    protected Drawable createBadgedDrawable(Drawable drawable, int i, ActivityManager.TaskDescription taskDescription) {
        return new FastBitmapDrawable(getBitmapInfo(drawable, i, taskDescription.getPrimaryColor(), false));
    }

    private synchronized BitmapInfo getBitmapInfo(Drawable drawable, int i, int i2, boolean z) {
        if (this.mLauncherIcons == null) {
            this.mLauncherIcons = LauncherIcons.obtain(this.mContext);
        }
        this.mLauncherIcons.setWrapperBackgroundColor(i2);
        return this.mLauncherIcons.createBadgedIconBitmap(drawable, UserHandle.of(i), 26, z);
    }

    @Override // com.android.systemui.shared.recents.model.IconLoader
    protected Drawable getBadgedActivityIcon(ActivityInfo activityInfo, int i, ActivityManager.TaskDescription taskDescription) {
        return this.mDrawableFactory.newIcon(getBitmapInfo(activityInfo.loadUnbadgedIcon(this.mContext.getPackageManager()), i, taskDescription.getPrimaryColor(), activityInfo.applicationInfo.isInstantApp()), activityInfo);
    }
}
