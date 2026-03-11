package com.android.launcher3.graphics;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import com.android.launcher3.AppInfo;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.IconCache;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.BitmapRenderer;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.util.Provider;
import com.android.launcher3.util.Themes;

public class LauncherIcons implements AutoCloseable {
    private static final int DEFAULT_WRAPPER_BACKGROUND = -1;
    private static LauncherIcons sPool;
    public static final Object sPoolSync = new Object();
    private final Canvas mCanvas;
    private final Context mContext;
    private final int mFillResIconDpi;
    private final int mIconBitmapSize;
    private IconNormalizer mNormalizer;
    private final PackageManager mPm;
    private ShadowGenerator mShadowGenerator;
    private Drawable mWrapperIcon;
    private LauncherIcons next;
    private final Rect mOldBounds = new Rect();
    private int mWrapperBackgroundColor = -1;

    public static LauncherIcons obtain(Context context) {
        synchronized (sPoolSync) {
            if (sPool != null) {
                LauncherIcons launcherIcons = sPool;
                sPool = launcherIcons.next;
                launcherIcons.next = null;
                return launcherIcons;
            }
            return new LauncherIcons(context);
        }
    }

    public void recycle() {
        synchronized (sPoolSync) {
            this.mWrapperBackgroundColor = -1;
            this.next = sPool;
            sPool = this;
        }
    }

    @Override
    public void close() {
        recycle();
    }

    private LauncherIcons(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPm = this.mContext.getPackageManager();
        InvariantDeviceProfile idp = LauncherAppState.getIDP(this.mContext);
        this.mFillResIconDpi = idp.fillResIconDpi;
        this.mIconBitmapSize = idp.iconBitmapSize;
        this.mCanvas = new Canvas();
        this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    public ShadowGenerator getShadowGenerator() {
        if (this.mShadowGenerator == null) {
            this.mShadowGenerator = new ShadowGenerator(this.mContext);
        }
        return this.mShadowGenerator;
    }

    public IconNormalizer getNormalizer() {
        if (this.mNormalizer == null) {
            this.mNormalizer = new IconNormalizer(this.mContext);
        }
        return this.mNormalizer;
    }

    public BitmapInfo createIconBitmap(Intent.ShortcutIconResource shortcutIconResource) {
        try {
            Resources resourcesForApplication = this.mPm.getResourcesForApplication(shortcutIconResource.packageName);
            if (resourcesForApplication != null) {
                return createBadgedIconBitmap(resourcesForApplication.getDrawableForDensity(resourcesForApplication.getIdentifier(shortcutIconResource.resourceName, null, null), this.mFillResIconDpi), Process.myUserHandle(), 0);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public BitmapInfo createIconBitmap(Bitmap bitmap) {
        if (this.mIconBitmapSize == bitmap.getWidth() && this.mIconBitmapSize == bitmap.getHeight()) {
            return BitmapInfo.fromBitmap(bitmap);
        }
        return BitmapInfo.fromBitmap(createIconBitmap(new BitmapDrawable(this.mContext.getResources(), bitmap), 1.0f));
    }

    public BitmapInfo createBadgedIconBitmap(Drawable drawable, UserHandle userHandle, int i) {
        return createBadgedIconBitmap(drawable, userHandle, i, false);
    }

    public BitmapInfo createBadgedIconBitmap(Drawable drawable, UserHandle userHandle, int i, boolean z) {
        Bitmap bitmapCreateIconBitmap;
        float[] fArr = new float[1];
        Drawable drawableNormalizeAndWrapToAdaptiveIcon = normalizeAndWrapToAdaptiveIcon(drawable, i, null, fArr);
        Bitmap bitmapCreateIconBitmap2 = createIconBitmap(drawableNormalizeAndWrapToAdaptiveIcon, fArr[0]);
        if (Utilities.ATLEAST_OREO && (drawableNormalizeAndWrapToAdaptiveIcon instanceof AdaptiveIconDrawable)) {
            this.mCanvas.setBitmap(bitmapCreateIconBitmap2);
            getShadowGenerator().recreateIcon(Bitmap.createBitmap(bitmapCreateIconBitmap2), this.mCanvas);
            this.mCanvas.setBitmap(null);
        }
        if (userHandle != null && !Process.myUserHandle().equals(userHandle)) {
            Drawable userBadgedIcon = this.mPm.getUserBadgedIcon(new FixedSizeBitmapDrawable(bitmapCreateIconBitmap2), userHandle);
            if (userBadgedIcon instanceof BitmapDrawable) {
                bitmapCreateIconBitmap = ((BitmapDrawable) userBadgedIcon).getBitmap();
            } else {
                bitmapCreateIconBitmap = createIconBitmap(userBadgedIcon, 1.0f);
            }
            bitmapCreateIconBitmap2 = bitmapCreateIconBitmap;
        } else if (z) {
            badgeWithDrawable(bitmapCreateIconBitmap2, this.mContext.getDrawable(R.drawable.ic_instant_app_badge));
        }
        return BitmapInfo.fromBitmap(bitmapCreateIconBitmap2);
    }

    public Bitmap createScaledBitmapWithoutShadow(Drawable drawable, int i) {
        RectF rectF = new RectF();
        float[] fArr = new float[1];
        return createIconBitmap(normalizeAndWrapToAdaptiveIcon(drawable, i, rectF, fArr), Math.min(fArr[0], ShadowGenerator.getScaleForBounds(rectF)));
    }

    public void setWrapperBackgroundColor(int i) {
        if (Color.alpha(i) < 255) {
            i = -1;
        }
        this.mWrapperBackgroundColor = i;
    }

    private Drawable normalizeAndWrapToAdaptiveIcon(Drawable drawable, int i, RectF rectF, float[] fArr) {
        float scale;
        if (Utilities.ATLEAST_OREO && i >= 26) {
            boolean[] zArr = new boolean[1];
            if (this.mWrapperIcon == null) {
                this.mWrapperIcon = this.mContext.getDrawable(R.drawable.adaptive_icon_drawable_wrapper).mutate();
            }
            AdaptiveIconDrawable adaptiveIconDrawable = (AdaptiveIconDrawable) this.mWrapperIcon;
            adaptiveIconDrawable.setBounds(0, 0, 1, 1);
            scale = getNormalizer().getScale(drawable, rectF, adaptiveIconDrawable.getIconMask(), zArr);
            if (Utilities.ATLEAST_OREO && !zArr[0] && !(drawable instanceof AdaptiveIconDrawable)) {
                FixedScaleDrawable fixedScaleDrawable = (FixedScaleDrawable) adaptiveIconDrawable.getForeground();
                fixedScaleDrawable.setDrawable(drawable);
                fixedScaleDrawable.setScale(scale);
                scale = getNormalizer().getScale(adaptiveIconDrawable, rectF, null, null);
                ((ColorDrawable) adaptiveIconDrawable.getBackground()).setColor(this.mWrapperBackgroundColor);
                drawable = adaptiveIconDrawable;
            }
        } else {
            scale = getNormalizer().getScale(drawable, rectF, null, null);
        }
        fArr[0] = scale;
        return drawable;
    }

    public void badgeWithDrawable(Bitmap bitmap, Drawable drawable) {
        this.mCanvas.setBitmap(bitmap);
        badgeWithDrawable(this.mCanvas, drawable);
        this.mCanvas.setBitmap(null);
    }

    private void badgeWithDrawable(Canvas canvas, Drawable drawable) {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.profile_badge_size);
        drawable.setBounds(this.mIconBitmapSize - dimensionPixelSize, this.mIconBitmapSize - dimensionPixelSize, this.mIconBitmapSize, this.mIconBitmapSize);
        drawable.draw(canvas);
    }

    private Bitmap createIconBitmap(Drawable drawable, float f) {
        BitmapDrawable bitmapDrawable;
        Bitmap bitmap;
        int i = this.mIconBitmapSize;
        int i2 = this.mIconBitmapSize;
        if (drawable instanceof PaintDrawable) {
            PaintDrawable paintDrawable = (PaintDrawable) drawable;
            paintDrawable.setIntrinsicWidth(i);
            paintDrawable.setIntrinsicHeight(i2);
        } else if ((drawable instanceof BitmapDrawable) && (bitmap = (bitmapDrawable = (BitmapDrawable) drawable).getBitmap()) != null && bitmap.getDensity() == 0) {
            bitmapDrawable.setTargetDensity(this.mContext.getResources().getDisplayMetrics());
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth > 0 && intrinsicHeight > 0) {
            float f2 = intrinsicWidth / intrinsicHeight;
            if (intrinsicWidth > intrinsicHeight) {
                i2 = (int) (i / f2);
            } else if (intrinsicHeight > intrinsicWidth) {
                i = (int) (i2 * f2);
            }
        }
        int i3 = this.mIconBitmapSize;
        int i4 = this.mIconBitmapSize;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i3, i4, Bitmap.Config.ARGB_8888);
        this.mCanvas.setBitmap(bitmapCreateBitmap);
        int i5 = (i3 - i) / 2;
        int i6 = (i4 - i2) / 2;
        this.mOldBounds.set(drawable.getBounds());
        if (Utilities.ATLEAST_OREO && (drawable instanceof AdaptiveIconDrawable)) {
            int iMax = Math.max((int) Math.ceil(0.010416667f * i3), Math.max(i5, i6));
            int iMax2 = Math.max(i, i2) + iMax;
            drawable.setBounds(iMax, iMax, iMax2, iMax2);
        } else {
            drawable.setBounds(i5, i6, i + i5, i2 + i6);
        }
        this.mCanvas.save();
        this.mCanvas.scale(f, f, i3 / 2, i4 / 2);
        drawable.draw(this.mCanvas);
        this.mCanvas.restore();
        drawable.setBounds(this.mOldBounds);
        this.mCanvas.setBitmap(null);
        return bitmapCreateBitmap;
    }

    public BitmapInfo createShortcutIcon(ShortcutInfoCompat shortcutInfoCompat) {
        return createShortcutIcon(shortcutInfoCompat, true);
    }

    public BitmapInfo createShortcutIcon(ShortcutInfoCompat shortcutInfoCompat, boolean z) {
        return createShortcutIcon(shortcutInfoCompat, z, null);
    }

    public BitmapInfo createShortcutIcon(ShortcutInfoCompat shortcutInfoCompat, boolean z, @Nullable Provider<Bitmap> provider) {
        final Bitmap bitmapCreateScaledBitmapWithoutShadow;
        Bitmap bitmap;
        Drawable shortcutIconDrawable = DeepShortcutManager.getInstance(this.mContext).getShortcutIconDrawable(shortcutInfoCompat, this.mFillResIconDpi);
        IconCache iconCache = LauncherAppState.getInstance(this.mContext).getIconCache();
        if (shortcutIconDrawable != null) {
            bitmapCreateScaledBitmapWithoutShadow = createScaledBitmapWithoutShadow(shortcutIconDrawable, 0);
        } else {
            if (provider != null && (bitmap = provider.get()) != null) {
                return createIconBitmap(bitmap);
            }
            bitmapCreateScaledBitmapWithoutShadow = iconCache.getDefaultIcon(Process.myUserHandle()).icon;
        }
        BitmapInfo bitmapInfo = new BitmapInfo();
        if (!z) {
            bitmapInfo.color = Themes.getColorAccent(this.mContext);
            bitmapInfo.icon = bitmapCreateScaledBitmapWithoutShadow;
            return bitmapInfo;
        }
        final ItemInfoWithIcon shortcutInfoBadge = getShortcutInfoBadge(shortcutInfoCompat, iconCache);
        bitmapInfo.color = shortcutInfoBadge.iconColor;
        bitmapInfo.icon = BitmapRenderer.createHardwareBitmap(this.mIconBitmapSize, this.mIconBitmapSize, new BitmapRenderer.Renderer() {
            @Override
            public final void draw(Canvas canvas) {
                LauncherIcons.lambda$createShortcutIcon$0(this.f$0, bitmapCreateScaledBitmapWithoutShadow, shortcutInfoBadge, canvas);
            }
        });
        return bitmapInfo;
    }

    public static void lambda$createShortcutIcon$0(LauncherIcons launcherIcons, Bitmap bitmap, ItemInfoWithIcon itemInfoWithIcon, Canvas canvas) {
        launcherIcons.getShadowGenerator().recreateIcon(bitmap, canvas);
        launcherIcons.badgeWithDrawable(canvas, new FastBitmapDrawable(itemInfoWithIcon));
    }

    public ItemInfoWithIcon getShortcutInfoBadge(ShortcutInfoCompat shortcutInfoCompat, IconCache iconCache) {
        ComponentName activity = shortcutInfoCompat.getActivity();
        String badgePackage = shortcutInfoCompat.getBadgePackage(this.mContext);
        boolean z = !badgePackage.equals(shortcutInfoCompat.getPackage());
        if (activity != null && !z) {
            AppInfo appInfo = new AppInfo();
            appInfo.user = shortcutInfoCompat.getUserHandle();
            appInfo.componentName = activity;
            appInfo.intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(activity);
            iconCache.getTitleAndIcon(appInfo, false);
            return appInfo;
        }
        PackageItemInfo packageItemInfo = new PackageItemInfo(badgePackage);
        iconCache.getTitleAndIconForApp(packageItemInfo, false);
        return packageItemInfo;
    }

    private static class FixedSizeBitmapDrawable extends BitmapDrawable {
        public FixedSizeBitmapDrawable(Bitmap bitmap) {
            super((Resources) null, bitmap);
        }

        @Override
        public int getIntrinsicHeight() {
            return getBitmap().getWidth();
        }

        @Override
        public int getIntrinsicWidth() {
            return getBitmap().getWidth();
        }
    }
}
