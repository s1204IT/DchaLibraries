package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.v7.graphics.Palette;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class MediaNotificationProcessor {
    private Palette.Filter mBlackWhiteFilter;
    private final ImageGradientColorizer mColorizer;
    private final Context mContext;
    private float[] mFilteredBackgroundHsl;
    private final Context mPackageContext;

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: NOT 
  (wrap:boolean:0x0000: INVOKE (r0v0 com.android.systemui.statusbar.notification.MediaNotificationProcessor), (r2v0 float[]) DIRECT call: com.android.systemui.statusbar.notification.MediaNotificationProcessor.isWhiteOrBlack(float[]):boolean A[DONT_GENERATE, MD:(float[]):boolean (m), REMOVE, WRAPPED] (LINE:72))
 */
    public static /* synthetic */ boolean lambda$new$0(MediaNotificationProcessor mediaNotificationProcessor, int i, float[] fArr) {
        return !mediaNotificationProcessor.isWhiteOrBlack(fArr);
    }

    public MediaNotificationProcessor(Context context, Context context2) {
        this(context, context2, new ImageGradientColorizer());
    }

    MediaNotificationProcessor(Context context, Context context2, ImageGradientColorizer imageGradientColorizer) {
        this.mFilteredBackgroundHsl = null;
        this.mBlackWhiteFilter = new Palette.Filter() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$MediaNotificationProcessor$FQ2jqCN-nkK3wF0AhHJdzMEAIb4
            @Override // android.support.v7.graphics.Palette.Filter
            public final boolean isAllowed(int i, float[] fArr) {
                return MediaNotificationProcessor.lambda$new$0(this.f$0, i, fArr);
            }
        };
        this.mContext = context;
        this.mPackageContext = context2;
        this.mColorizer = imageGradientColorizer;
    }

    public void processNotification(Notification notification, Notification.Builder builder) {
        int color;
        Icon largeIcon = notification.getLargeIcon();
        if (largeIcon != null) {
            boolean z = true;
            builder.setRebuildStyledRemoteViews(true);
            Drawable drawableLoadDrawable = largeIcon.loadDrawable(this.mPackageContext);
            if (notification.isColorizedMedia()) {
                int intrinsicWidth = drawableLoadDrawable.getIntrinsicWidth();
                int intrinsicHeight = drawableLoadDrawable.getIntrinsicHeight();
                if (intrinsicWidth * intrinsicHeight > 22500) {
                    double dSqrt = Math.sqrt(22500.0f / r4);
                    intrinsicWidth = (int) (intrinsicWidth * dSqrt);
                    intrinsicHeight = (int) (dSqrt * intrinsicHeight);
                }
                Bitmap bitmapCreateBitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmapCreateBitmap);
                drawableLoadDrawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
                drawableLoadDrawable.draw(canvas);
                Palette.Builder builderResizeBitmapArea = Palette.from(bitmapCreateBitmap).setRegion(0, 0, bitmapCreateBitmap.getWidth() / 2, bitmapCreateBitmap.getHeight()).clearFilters().resizeBitmapArea(22500);
                color = findBackgroundColorAndFilter(builderResizeBitmapArea.generate());
                builderResizeBitmapArea.setRegion((int) (bitmapCreateBitmap.getWidth() * 0.4f), 0, bitmapCreateBitmap.getWidth(), bitmapCreateBitmap.getHeight());
                if (this.mFilteredBackgroundHsl != null) {
                    builderResizeBitmapArea.addFilter(new Palette.Filter() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$MediaNotificationProcessor$G1QdSAb4RmghAdo10Gv3JWJ8lmw
                        @Override // android.support.v7.graphics.Palette.Filter
                        public final boolean isAllowed(int i, float[] fArr) {
                            return MediaNotificationProcessor.lambda$processNotification$1(this.f$0, i, fArr);
                        }
                    });
                }
                builderResizeBitmapArea.addFilter(this.mBlackWhiteFilter);
                builder.setColorPalette(color, selectForegroundColor(color, builderResizeBitmapArea.generate()));
            } else {
                color = this.mContext.getColor(R.color.notification_material_background_color);
            }
            ImageGradientColorizer imageGradientColorizer = this.mColorizer;
            if (this.mContext.getResources().getConfiguration().getLayoutDirection() != 1) {
                z = false;
            }
            builder.setLargeIcon(Icon.createWithBitmap(imageGradientColorizer.colorize(drawableLoadDrawable, color, z)));
        }
    }

    public static /* synthetic */ boolean lambda$processNotification$1(MediaNotificationProcessor mediaNotificationProcessor, int i, float[] fArr) {
        float fAbs = Math.abs(fArr[0] - mediaNotificationProcessor.mFilteredBackgroundHsl[0]);
        return fAbs > 10.0f && fAbs < 350.0f;
    }

    private int selectForegroundColor(int i, Palette palette) {
        if (NotificationColorUtil.isColorLight(i)) {
            return selectForegroundColorForSwatches(palette.getDarkVibrantSwatch(), palette.getVibrantSwatch(), palette.getDarkMutedSwatch(), palette.getMutedSwatch(), palette.getDominantSwatch(), -16777216);
        }
        return selectForegroundColorForSwatches(palette.getLightVibrantSwatch(), palette.getVibrantSwatch(), palette.getLightMutedSwatch(), palette.getMutedSwatch(), palette.getDominantSwatch(), -1);
    }

    private int selectForegroundColorForSwatches(Palette.Swatch swatch, Palette.Swatch swatch2, Palette.Swatch swatch3, Palette.Swatch swatch4, Palette.Swatch swatch5, int i) {
        Palette.Swatch swatchSelectVibrantCandidate = selectVibrantCandidate(swatch, swatch2);
        if (swatchSelectVibrantCandidate == null) {
            swatchSelectVibrantCandidate = selectMutedCandidate(swatch4, swatch3);
        }
        if (swatchSelectVibrantCandidate != null) {
            if (swatch5 == swatchSelectVibrantCandidate) {
                return swatchSelectVibrantCandidate.getRgb();
            }
            if (swatchSelectVibrantCandidate.getPopulation() / swatch5.getPopulation() < 0.01f && swatch5.getHsl()[1] > 0.19f) {
                return swatch5.getRgb();
            }
            return swatchSelectVibrantCandidate.getRgb();
        }
        if (hasEnoughPopulation(swatch5)) {
            return swatch5.getRgb();
        }
        return i;
    }

    private Palette.Swatch selectMutedCandidate(Palette.Swatch swatch, Palette.Swatch swatch2) {
        boolean zHasEnoughPopulation = hasEnoughPopulation(swatch);
        boolean zHasEnoughPopulation2 = hasEnoughPopulation(swatch2);
        if (zHasEnoughPopulation && zHasEnoughPopulation2) {
            if (swatch.getHsl()[1] * (swatch.getPopulation() / swatch2.getPopulation()) > swatch2.getHsl()[1]) {
                return swatch;
            }
            return swatch2;
        }
        if (zHasEnoughPopulation) {
            return swatch;
        }
        if (zHasEnoughPopulation2) {
            return swatch2;
        }
        return null;
    }

    private Palette.Swatch selectVibrantCandidate(Palette.Swatch swatch, Palette.Swatch swatch2) {
        boolean zHasEnoughPopulation = hasEnoughPopulation(swatch);
        boolean zHasEnoughPopulation2 = hasEnoughPopulation(swatch2);
        if (zHasEnoughPopulation && zHasEnoughPopulation2) {
            if (swatch.getPopulation() / swatch2.getPopulation() < 1.0f) {
                return swatch2;
            }
            return swatch;
        }
        if (zHasEnoughPopulation) {
            return swatch;
        }
        if (zHasEnoughPopulation2) {
            return swatch2;
        }
        return null;
    }

    private boolean hasEnoughPopulation(Palette.Swatch swatch) {
        return swatch != null && ((double) (((float) swatch.getPopulation()) / 22500.0f)) > 0.002d;
    }

    private int findBackgroundColorAndFilter(Palette palette) {
        Palette.Swatch dominantSwatch = palette.getDominantSwatch();
        if (dominantSwatch == null) {
            this.mFilteredBackgroundHsl = null;
            return -1;
        }
        if (!isWhiteOrBlack(dominantSwatch.getHsl())) {
            this.mFilteredBackgroundHsl = dominantSwatch.getHsl();
            return dominantSwatch.getRgb();
        }
        float population = -1.0f;
        Palette.Swatch swatch = null;
        for (Palette.Swatch swatch2 : palette.getSwatches()) {
            if (swatch2 != dominantSwatch && swatch2.getPopulation() > population && !isWhiteOrBlack(swatch2.getHsl())) {
                population = swatch2.getPopulation();
                swatch = swatch2;
            }
        }
        if (swatch == null) {
            this.mFilteredBackgroundHsl = null;
            return dominantSwatch.getRgb();
        }
        if (dominantSwatch.getPopulation() / population > 2.5f) {
            this.mFilteredBackgroundHsl = null;
            return dominantSwatch.getRgb();
        }
        this.mFilteredBackgroundHsl = swatch.getHsl();
        return swatch.getRgb();
    }

    private boolean isWhiteOrBlack(float[] fArr) {
        return isBlack(fArr) || isWhite(fArr);
    }

    private boolean isBlack(float[] fArr) {
        return fArr[2] <= 0.08f;
    }

    private boolean isWhite(float[] fArr) {
        return fArr[2] >= 0.9f;
    }
}
