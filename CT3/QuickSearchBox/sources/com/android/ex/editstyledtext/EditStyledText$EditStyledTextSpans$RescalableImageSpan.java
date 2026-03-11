package com.android.ex.editstyledtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.ImageSpan;
import android.util.Log;
import java.io.InputStream;

public class EditStyledText$EditStyledTextSpans$RescalableImageSpan extends ImageSpan {
    private final int MAXWIDTH;
    Uri mContentUri;
    private Context mContext;
    private Drawable mDrawable;
    public int mIntrinsicHeight;
    public int mIntrinsicWidth;

    @Override
    public Drawable getDrawable() {
        Bitmap bitmap;
        if (this.mDrawable != null) {
            return this.mDrawable;
        }
        if (this.mContentUri != null) {
            System.gc();
            try {
                InputStream is = this.mContext.getContentResolver().openInputStream(this.mContentUri);
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, opt);
                is.close();
                InputStream is2 = this.mContext.getContentResolver().openInputStream(this.mContentUri);
                int width = opt.outWidth;
                int height = opt.outHeight;
                this.mIntrinsicWidth = width;
                this.mIntrinsicHeight = height;
                if (opt.outWidth > this.MAXWIDTH) {
                    width = this.MAXWIDTH;
                    height = (this.MAXWIDTH * height) / opt.outWidth;
                    Rect padding = new Rect(0, 0, width, height);
                    bitmap = BitmapFactory.decodeStream(is2, padding, null);
                } else {
                    bitmap = BitmapFactory.decodeStream(is2);
                }
                this.mDrawable = new BitmapDrawable(this.mContext.getResources(), bitmap);
                this.mDrawable.setBounds(0, 0, width, height);
                is2.close();
            } catch (Exception e) {
                Log.e("EditStyledTextSpan", "Failed to loaded content " + this.mContentUri, e);
                return null;
            } catch (OutOfMemoryError e2) {
                Log.e("EditStyledTextSpan", "OutOfMemoryError");
                return null;
            }
        } else {
            this.mDrawable = super.getDrawable();
            rescaleBigImage(this.mDrawable);
            this.mIntrinsicWidth = this.mDrawable.getIntrinsicWidth();
            this.mIntrinsicHeight = this.mDrawable.getIntrinsicHeight();
        }
        return this.mDrawable;
    }

    private void rescaleBigImage(Drawable image) {
        Log.d("EditStyledTextSpan", "--- rescaleBigImage:");
        if (this.MAXWIDTH < 0) {
            return;
        }
        int image_width = image.getIntrinsicWidth();
        int image_height = image.getIntrinsicHeight();
        Log.d("EditStyledTextSpan", "--- rescaleBigImage:" + image_width + "," + image_height + "," + this.MAXWIDTH);
        if (image_width > this.MAXWIDTH) {
            image_width = this.MAXWIDTH;
            image_height = (this.MAXWIDTH * image_height) / image_width;
        }
        image.setBounds(0, 0, image_width, image_height);
    }
}
