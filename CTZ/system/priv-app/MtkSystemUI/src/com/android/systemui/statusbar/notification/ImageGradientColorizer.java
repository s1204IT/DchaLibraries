package com.android.systemui.statusbar.notification;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/* loaded from: classes.dex */
public class ImageGradientColorizer {
    public Bitmap colorize(Drawable drawable, int i, boolean z) {
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int iMin = Math.min(intrinsicWidth, intrinsicHeight);
        int i2 = (intrinsicWidth - iMin) / 2;
        int i3 = (intrinsicHeight - iMin) / 2;
        Drawable drawableMutate = drawable.mutate();
        drawableMutate.setBounds(-i2, -i3, intrinsicWidth - i2, intrinsicHeight - i3);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iMin, iMin, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        float fRed = Color.red(i);
        float fGreen = Color.green(i);
        float fBlue = Color.blue(i);
        float f = (((fRed / 255.0f) * 0.2126f) + ((fGreen / 255.0f) * 0.7152f) + ((fBlue / 255.0f) * 0.0722f)) * 255.0f;
        ColorMatrix colorMatrix = new ColorMatrix(new float[]{0.2126f, 0.7152f, 0.0722f, 0.0f, fRed - f, 0.2126f, 0.7152f, 0.0722f, 0.0f, fGreen - f, 0.2126f, 0.7152f, 0.0722f, 0.0f, fBlue - f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f});
        Paint paint = new Paint(1);
        float f2 = iMin;
        paint.setShader(new LinearGradient(0.0f, 0.0f, f2, 0.0f, new int[]{0, Color.argb(0.5f, 1.0f, 1.0f, 1.0f), -16777216}, new float[]{0.0f, 0.4f, 1.0f}, Shader.TileMode.CLAMP));
        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(iMin, iMin, Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmapCreateBitmap2);
        drawableMutate.clearColorFilter();
        drawableMutate.draw(canvas2);
        if (z) {
            canvas2.translate(f2, 0.0f);
            canvas2.scale(-1.0f, 1.0f);
        }
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas2.drawPaint(paint);
        Paint paint2 = new Paint(1);
        paint2.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        paint2.setAlpha(127);
        canvas.drawBitmap(bitmapCreateBitmap2, 0.0f, 0.0f, paint2);
        paint.setShader(new LinearGradient(0.0f, 0.0f, f2, 0.0f, new int[]{0, Color.argb(0.5f, 1.0f, 1.0f, 1.0f), -16777216}, new float[]{0.0f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        canvas2.drawPaint(paint);
        canvas.drawBitmap(bitmapCreateBitmap2, 0.0f, 0.0f, (Paint) null);
        return bitmapCreateBitmap;
    }
}
