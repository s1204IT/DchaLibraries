package com.android.internal.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.Pair;
import java.util.Arrays;
import java.util.WeakHashMap;

public class NotificationColorUtil {
    private static final String TAG = "NotificationColorUtil";
    private static NotificationColorUtil sInstance;
    private static final Object sLock = new Object();
    private final int mGrayscaleIconMaxSize;
    private final ImageUtils mImageUtils = new ImageUtils();
    private final WeakHashMap<Bitmap, Pair<Boolean, Integer>> mGrayscaleBitmapCache = new WeakHashMap<>();

    public static NotificationColorUtil getInstance(Context context) {
        NotificationColorUtil notificationColorUtil;
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new NotificationColorUtil(context);
            }
            notificationColorUtil = sInstance;
        }
        return notificationColorUtil;
    }

    private NotificationColorUtil(Context context) {
        this.mGrayscaleIconMaxSize = context.getResources().getDimensionPixelSize(17104901);
    }

    public boolean isGrayscaleIcon(Bitmap bitmap) {
        boolean result;
        int generationId;
        if (bitmap.getWidth() > this.mGrayscaleIconMaxSize || bitmap.getHeight() > this.mGrayscaleIconMaxSize) {
            return false;
        }
        synchronized (sLock) {
            Pair<Boolean, Integer> cached = this.mGrayscaleBitmapCache.get(bitmap);
            if (cached != null && cached.second.intValue() == bitmap.getGenerationId()) {
                result = cached.first.booleanValue();
            } else {
                synchronized (this.mImageUtils) {
                    result = this.mImageUtils.isGrayscale(bitmap);
                    generationId = bitmap.getGenerationId();
                }
                synchronized (sLock) {
                    this.mGrayscaleBitmapCache.put(bitmap, Pair.create(Boolean.valueOf(result), Integer.valueOf(generationId)));
                }
            }
        }
        return result;
    }

    public boolean isGrayscaleIcon(Drawable d) {
        if (d == null) {
            return false;
        }
        if (d instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) d;
            return bd.getBitmap() != null && isGrayscaleIcon(bd.getBitmap());
        }
        if (!(d instanceof AnimationDrawable)) {
            return d instanceof VectorDrawable;
        }
        AnimationDrawable ad = (AnimationDrawable) d;
        int count = ad.getNumberOfFrames();
        return count > 0 && isGrayscaleIcon(ad.getFrame(0));
    }

    public boolean isGrayscaleIcon(Context context, int drawableResId) {
        if (drawableResId == 0) {
            return false;
        }
        try {
            return isGrayscaleIcon(context.getDrawable(drawableResId));
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Drawable not found: " + drawableResId);
            return false;
        }
    }

    public CharSequence invertCharSequenceColors(CharSequence charSequence) {
        if (!(charSequence instanceof Spanned)) {
            return charSequence;
        }
        Spanned ss = (Spanned) charSequence;
        Object[] spans = ss.getSpans(0, ss.length(), Object.class);
        SpannableStringBuilder builder = new SpannableStringBuilder(ss.toString());
        for (Object span : spans) {
            Object resultSpan = span;
            if (span instanceof TextAppearanceSpan) {
                resultSpan = processTextAppearanceSpan((TextAppearanceSpan) span);
            }
            builder.setSpan(resultSpan, ss.getSpanStart(span), ss.getSpanEnd(span), ss.getSpanFlags(span));
        }
        return builder;
    }

    private TextAppearanceSpan processTextAppearanceSpan(TextAppearanceSpan span) {
        ColorStateList colorStateList = span.getTextColor();
        if (colorStateList != null) {
            int[] colors = colorStateList.getColors();
            boolean changed = false;
            for (int i = 0; i < colors.length; i++) {
                if (ImageUtils.isGrayscale(colors[i])) {
                    if (!changed) {
                        colors = Arrays.copyOf(colors, colors.length);
                    }
                    colors[i] = processColor(colors[i]);
                    changed = true;
                }
            }
            if (changed) {
                return new TextAppearanceSpan(span.getFamily(), span.getTextStyle(), span.getTextSize(), new ColorStateList(colorStateList.getStates(), colors), span.getLinkTextColor());
            }
        }
        return span;
    }

    private int processColor(int color) {
        return Color.argb(Color.alpha(color), 255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color));
    }
}
