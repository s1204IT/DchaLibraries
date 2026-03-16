package com.android.contacts.common.lettertiles;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import com.android.contacts.R;
import junit.framework.Assert;

public class LetterTileDrawable extends Drawable {
    private static Bitmap DEFAULT_BUSINESS_AVATAR;
    private static Bitmap DEFAULT_PERSON_AVATAR;
    private static Bitmap DEFAULT_VOICEMAIL_AVATAR;
    private static TypedArray sColors;
    private static int sDefaultColor;
    private static float sLetterToTileRatio;
    private static int sTileFontColor;
    private String mDisplayName;
    private String mIdentifier;
    private static final Paint sPaint = new Paint();
    private static final Rect sRect = new Rect();
    private static final char[] sFirstChar = new char[1];
    private final String TAG = LetterTileDrawable.class.getSimpleName();
    private int mContactType = 1;
    private float mScale = 1.0f;
    private float mOffset = 0.0f;
    private boolean mIsCircle = false;
    private final Paint mPaint = new Paint();

    public LetterTileDrawable(Resources res) {
        this.mPaint.setFilterBitmap(true);
        this.mPaint.setDither(true);
        if (sColors == null) {
            sColors = res.obtainTypedArray(R.array.letter_tile_colors);
            sDefaultColor = res.getColor(R.color.letter_tile_default_color);
            sTileFontColor = res.getColor(R.color.letter_tile_font_color);
            sLetterToTileRatio = res.getFraction(R.dimen.letter_to_tile_ratio, 1, 1);
            DEFAULT_PERSON_AVATAR = BitmapFactory.decodeResource(res, R.drawable.ic_person_white_120dp);
            DEFAULT_BUSINESS_AVATAR = BitmapFactory.decodeResource(res, R.drawable.ic_business_white_120dp);
            DEFAULT_VOICEMAIL_AVATAR = BitmapFactory.decodeResource(res, R.drawable.ic_voicemail_avatar);
            sPaint.setTypeface(Typeface.create(res.getString(R.string.letter_tile_letter_font_family), 0));
            sPaint.setTextAlign(Paint.Align.CENTER);
            sPaint.setAntiAlias(true);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (isVisible() && !bounds.isEmpty()) {
            drawLetterTile(canvas);
        }
    }

    private void drawBitmap(Bitmap bitmap, int width, int height, Canvas canvas) {
        Rect destRect = copyBounds();
        int halfLength = (int) ((this.mScale * Math.min(destRect.width(), destRect.height())) / 2.0f);
        destRect.set(destRect.centerX() - halfLength, (int) ((destRect.centerY() - halfLength) + (this.mOffset * destRect.height())), destRect.centerX() + halfLength, (int) (destRect.centerY() + halfLength + (this.mOffset * destRect.height())));
        sRect.set(0, 0, width, height);
        canvas.drawBitmap(bitmap, sRect, destRect, this.mPaint);
    }

    private void drawLetterTile(Canvas canvas) {
        sPaint.setColor(pickColor(this.mIdentifier));
        sPaint.setAlpha(this.mPaint.getAlpha());
        Rect bounds = getBounds();
        int minDimension = Math.min(bounds.width(), bounds.height());
        if (this.mIsCircle) {
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), minDimension / 2, sPaint);
        } else {
            canvas.drawRect(bounds, sPaint);
        }
        if (this.mDisplayName != null && isEnglishLetter(this.mDisplayName.charAt(0))) {
            sFirstChar[0] = Character.toUpperCase(this.mDisplayName.charAt(0));
            sPaint.setTextSize(this.mScale * sLetterToTileRatio * minDimension);
            sPaint.getTextBounds(sFirstChar, 0, 1, sRect);
            sPaint.setColor(sTileFontColor);
            canvas.drawText(sFirstChar, 0, 1, bounds.centerX(), (sRect.height() / 2) + bounds.centerY() + (this.mOffset * bounds.height()), sPaint);
            return;
        }
        Bitmap bitmap = getBitmapForContactType(this.mContactType);
        drawBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), canvas);
    }

    public int getColor() {
        return pickColor(this.mIdentifier);
    }

    private int pickColor(String identifier) {
        if (TextUtils.isEmpty(identifier) || this.mContactType == 3) {
            return sDefaultColor;
        }
        int color = Math.abs(identifier.hashCode()) % sColors.length();
        return sColors.getColor(color, sDefaultColor);
    }

    private static Bitmap getBitmapForContactType(int contactType) {
        switch (contactType) {
        }
        return DEFAULT_PERSON_AVATAR;
    }

    private static boolean isEnglishLetter(char c) {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    @Override
    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return -1;
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public void setOffset(float offset) {
        Assert.assertTrue(offset >= -0.5f && offset <= 0.5f);
        this.mOffset = offset;
    }

    public void setContactDetails(String displayName, String identifier) {
        this.mDisplayName = displayName;
        this.mIdentifier = identifier;
    }

    public void setContactType(int contactType) {
        this.mContactType = contactType;
    }

    public void setIsCircular(boolean isCircle) {
        this.mIsCircle = isCircle;
    }
}
