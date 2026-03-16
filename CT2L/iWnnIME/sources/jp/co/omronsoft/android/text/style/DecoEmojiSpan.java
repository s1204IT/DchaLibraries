package jp.co.omronsoft.android.text.style;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.DynamicDrawableSpan;
import android.util.Log;
import java.math.BigDecimal;
import jp.co.omronsoft.android.text.EmojiDrawable;

public class DecoEmojiSpan extends DynamicDrawableSpan {
    private static final String TAG = "DecoEmojiSpan";
    private static final int TEXT_UTILS_DECO_EMOJI_SPAN = 999;
    private int mContentHeight;
    private int mContentKind;
    private int mContentPopFlag;
    private Bitmap mContentPopImage;
    private float mContentTextScaleX;
    private int mContentTextSize;
    private String mContentUri;
    private int mContentWidth;
    private Context mContext;
    private Drawable mDrawable;
    private EmojiDrawable mEmojiDrawable;
    private Bundle mExtras;

    public DecoEmojiSpan(String uri) {
        super(1);
        this.mContentWidth = 0;
        this.mContentHeight = 0;
        this.mContentTextSize = 0;
        this.mContentTextScaleX = 0.0f;
        this.mContentKind = 0;
        this.mContentPopFlag = 0;
        this.mContentPopImage = null;
        this.mExtras = null;
        this.mContext = null;
        this.mEmojiDrawable = null;
        this.mDrawable = null;
        this.mContentUri = uri;
    }

    public DecoEmojiSpan(Context context, String uri, int width, int height, int kind, int textSize, float textScaleX, int popFlag, Bitmap popImage) {
        super(1);
        this.mContentWidth = 0;
        this.mContentHeight = 0;
        this.mContentTextSize = 0;
        this.mContentTextScaleX = 0.0f;
        this.mContentKind = 0;
        this.mContentPopFlag = 0;
        this.mContentPopImage = null;
        this.mExtras = null;
        this.mContext = null;
        this.mEmojiDrawable = null;
        this.mDrawable = null;
        this.mContext = context;
        this.mContentUri = uri;
        this.mContentWidth = width;
        this.mContentHeight = height;
        this.mContentTextSize = textSize;
        this.mContentTextScaleX = textScaleX;
        this.mContentKind = kind;
        this.mContentPopFlag = popFlag;
        this.mContentPopImage = popImage;
    }

    public void setExtras(Bundle extras) {
        this.mExtras = extras;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setEmojiDrawable(EmojiDrawable emoji) {
        this.mEmojiDrawable = emoji;
    }

    public String getURI() {
        return this.mContentUri;
    }

    public int getWidth() {
        return this.mContentWidth;
    }

    public int getHeight() {
        return this.mContentHeight;
    }

    public int getTextSize() {
        return this.mContentTextSize;
    }

    public float getTextScaleX() {
        return this.mContentTextScaleX;
    }

    public boolean isTypePicture() {
        return this.mContentKind == 4;
    }

    public int getPopFlag() {
        return this.mContentPopFlag;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        float scaleY = 1.0f;
        if (this.mEmojiDrawable != null) {
            if ((text instanceof Spanned) && (paint instanceof TextPaint)) {
                Spanned spannedText = (Spanned) text;
                CharacterStyle[] styles = (CharacterStyle[]) spannedText.getSpans(start, end, CharacterStyle.class);
                for (CharacterStyle span : styles) {
                    span.updateDrawState((TextPaint) paint);
                }
                EmojiSpanUtil.drawTextState(canvas, x, top, y, bottom, paint);
            }
            if (this.mEmojiDrawable.getPictureScale()) {
                int i = this.mContentKind;
                EmojiDrawable emojiDrawable = this.mEmojiDrawable;
                if (i != 4) {
                    int i2 = this.mContentKind;
                    EmojiDrawable emojiDrawable2 = this.mEmojiDrawable;
                    if (i2 != 4) {
                        scaleY = this.mContentTextSize / this.mContentHeight;
                    }
                }
            }
            BigDecimal height = new BigDecimal(this.mContentHeight * scaleY);
            BigDecimal height2 = height.setScale(0, 0);
            Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            int ascent = 0;
            if (y != bottom) {
                ascent = fm.top - fm.ascent;
            }
            this.mEmojiDrawable.drawDecoEmoji(x, (bottom - height2.intValue()) + ascent, this.mContentUri, canvas, paint, this.mContext, this.mContentWidth, this.mContentHeight, this.mContentKind, this.mContentPopImage);
            return;
        }
        Log.e(TAG, "draw parameter is fail mEmojiDrawable = " + this.mEmojiDrawable);
    }

    @Override
    public int getVerticalAlignment() {
        return 0;
    }

    @Override
    public Drawable getDrawable() {
        if (this.mContentUri != null && this.mDrawable == null) {
            float scaleX = 1.0f;
            float scaleY = 1.0f;
            this.mDrawable = new BitmapDrawable(this.mContext.getResources(), Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565));
            int emojiWidth = this.mContentWidth;
            int emojiHeight = this.mContentHeight;
            if (this.mEmojiDrawable.getPictureScale()) {
                int i = this.mContentKind;
                EmojiDrawable emojiDrawable = this.mEmojiDrawable;
                if (i != 4) {
                    int i2 = this.mContentKind;
                    EmojiDrawable emojiDrawable2 = this.mEmojiDrawable;
                    if (i2 != 4) {
                        scaleX = (this.mContentTextSize / emojiWidth) * this.mContentTextScaleX;
                        scaleY = this.mContentTextSize / emojiHeight;
                        EmojiDrawable emojiDrawable3 = this.mEmojiDrawable;
                        if (emojiHeight > 20) {
                            EmojiDrawable emojiDrawable4 = this.mEmojiDrawable;
                            float tmpScaleY = emojiHeight / 20.0f;
                            EmojiDrawable emojiDrawable5 = this.mEmojiDrawable;
                            float tmpScaleX = emojiWidth / 20.0f;
                            scaleX = (((this.mContentTextSize / emojiWidth) * this.mContentTextScaleX) * tmpScaleX) / tmpScaleY;
                        } else {
                            EmojiDrawable emojiDrawable6 = this.mEmojiDrawable;
                            if (emojiWidth > 20) {
                                EmojiDrawable emojiDrawable7 = this.mEmojiDrawable;
                                float tmpScaleX2 = emojiWidth / 20.0f;
                                scaleX = (this.mContentTextSize / emojiWidth) * this.mContentTextScaleX * tmpScaleX2;
                            }
                        }
                    }
                    BigDecimal drawWidth = new BigDecimal(emojiWidth * scaleX);
                    BigDecimal drawWidth2 = drawWidth.setScale(0, 0);
                    BigDecimal drawHeight = new BigDecimal(emojiHeight * scaleY);
                    this.mDrawable.setBounds(0, 0, drawWidth2.intValue(), drawHeight.setScale(0, 0).intValue());
                    if (this.mContentPopFlag == 1) {
                        resizePopImage(this.mContentPopImage);
                    } else {
                        this.mContentPopImage = null;
                    }
                }
            }
        }
        return this.mDrawable;
    }

    private void resizePopImage(Bitmap popImage) {
        float scaleX;
        if (this.mDrawable != null) {
            int emojiWidth = this.mDrawable.getIntrinsicWidth();
            int emojiHeight = this.mDrawable.getIntrinsicHeight();
            if (emojiHeight > 20) {
                scaleX = (emojiHeight / 20.0f) / 2.0f;
            } else if (emojiWidth > 20) {
                scaleX = (emojiWidth / 20.0f) / 2.0f;
            } else {
                scaleX = (popImage.getHeight() / 20.0f) / 2.0f;
            }
            int resizeWidth = (int) (popImage.getWidth() * scaleX);
            int resizeHeight = (int) (popImage.getHeight() * scaleX);
            if (this.mContentPopImage == null || resizeHeight != this.mContentPopImage.getHeight()) {
                this.mContentPopImage = Bitmap.createScaledBitmap(popImage, resizeWidth, resizeHeight, true);
            }
        }
    }

    public String getSource() {
        return this.mContentUri.toString();
    }
}
