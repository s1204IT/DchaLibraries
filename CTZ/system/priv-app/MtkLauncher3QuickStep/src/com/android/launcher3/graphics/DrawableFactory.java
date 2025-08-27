package com.android.launcher3.graphics;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.UiThread;
import android.util.ArrayMap;
import android.util.Log;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AllAppsBackgroundDrawable;

/* loaded from: classes.dex */
public class DrawableFactory {
    private static final Object LOCK = new Object();
    private static final String TAG = "DrawableFactory";
    private static DrawableFactory sInstance;
    private Path mPreloadProgressPath;
    protected final UserHandle mMyUser = Process.myUserHandle();
    protected final ArrayMap<UserHandle, Bitmap> mUserBadges = new ArrayMap<>();

    public static DrawableFactory get(Context context) {
        DrawableFactory drawableFactory;
        synchronized (LOCK) {
            if (sInstance == null) {
                sInstance = (DrawableFactory) Utilities.getOverrideObject(DrawableFactory.class, context.getApplicationContext(), R.string.drawable_factory_class);
            }
            drawableFactory = sInstance;
        }
        return drawableFactory;
    }

    public FastBitmapDrawable newIcon(ItemInfoWithIcon itemInfoWithIcon) {
        FastBitmapDrawable fastBitmapDrawable = new FastBitmapDrawable(itemInfoWithIcon);
        fastBitmapDrawable.setIsDisabled(itemInfoWithIcon.isDisabled());
        return fastBitmapDrawable;
    }

    public FastBitmapDrawable newIcon(BitmapInfo bitmapInfo, ActivityInfo activityInfo) {
        return new FastBitmapDrawable(bitmapInfo);
    }

    public PreloadIconDrawable newPendingIcon(ItemInfoWithIcon itemInfoWithIcon, Context context) {
        if (this.mPreloadProgressPath == null) {
            this.mPreloadProgressPath = getPreloadProgressPath(context);
        }
        return new PreloadIconDrawable(itemInfoWithIcon, this.mPreloadProgressPath, context);
    }

    protected Path getPreloadProgressPath(Context context) {
        if (Utilities.ATLEAST_OREO) {
            try {
                Drawable drawable = context.getDrawable(R.drawable.adaptive_icon_drawable_wrapper);
                drawable.setBounds(0, 0, 100, 100);
                return (Path) drawable.getClass().getMethod("getIconMask", new Class[0]).invoke(drawable, new Object[0]);
            } catch (Exception e) {
                Log.e(TAG, "Error loading mask icon", e);
            }
        }
        Path path = new Path();
        path.moveTo(50.0f, 0.0f);
        path.addArc(0.0f, 0.0f, 100.0f, 100.0f, -90.0f, 360.0f);
        return path;
    }

    public AllAppsBackgroundDrawable getAllAppsBackground(Context context) {
        return new AllAppsBackgroundDrawable(context);
    }

    @UiThread
    public Drawable getBadgeForUser(UserHandle userHandle, Context context) {
        if (this.mMyUser.equals(userHandle)) {
            return null;
        }
        Bitmap userBadge = getUserBadge(userHandle, context);
        FastBitmapDrawable fastBitmapDrawable = new FastBitmapDrawable(userBadge);
        fastBitmapDrawable.setFilterBitmap(true);
        fastBitmapDrawable.setBounds(0, 0, userBadge.getWidth(), userBadge.getHeight());
        return fastBitmapDrawable;
    }

    protected synchronized Bitmap getUserBadge(UserHandle userHandle, Context context) {
        Bitmap bitmap = this.mUserBadges.get(userHandle);
        if (bitmap != null) {
            return bitmap;
        }
        Resources resources = context.getApplicationContext().getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.profile_badge_size);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(dimensionPixelSize, dimensionPixelSize, Bitmap.Config.ARGB_8888);
        Drawable userBadgedDrawableForDensity = context.getPackageManager().getUserBadgedDrawableForDensity(new BitmapDrawable(resources, bitmapCreateBitmap), userHandle, new Rect(0, 0, dimensionPixelSize, dimensionPixelSize), 0);
        if (userBadgedDrawableForDensity instanceof BitmapDrawable) {
            bitmapCreateBitmap = ((BitmapDrawable) userBadgedDrawableForDensity).getBitmap();
        } else {
            bitmapCreateBitmap.eraseColor(0);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            userBadgedDrawableForDensity.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize);
            userBadgedDrawableForDensity.draw(canvas);
            canvas.setBitmap(null);
        }
        this.mUserBadges.put(userHandle, bitmapCreateBitmap);
        return bitmapCreateBitmap;
    }
}
