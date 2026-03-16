package jp.co.omronsoft.android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;
import android.util.Log;
import jp.co.omronsoft.android.text.EmojiDrawable;

public class VendorEmojiSpan extends ReplacementSpan {
    private static final boolean DEBUG = false;
    private static final String TAG = "VendorEmojiSpan";
    private EmojiDrawable mEmojiDrawable = null;

    public void setEmojiDrawable(EmojiDrawable emoji) {
        this.mEmojiDrawable = emoji;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) paint.getTextSize();
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        EmojiDrawable drawable = this.mEmojiDrawable;
        if (drawable != null) {
            if ((text instanceof Spanned) && (paint instanceof TextPaint)) {
                Spanned spannedText = (Spanned) text;
                CharacterStyle[] styles = (CharacterStyle[]) spannedText.getSpans(start, end, CharacterStyle.class);
                for (CharacterStyle span : styles) {
                    span.updateDrawState((TextPaint) paint);
                }
                EmojiSpanUtil.drawTextState(canvas, x, top, y, bottom, paint);
            }
            drawable.setCanvas(canvas);
            String textString = text.toString();
            drawable.setEmoji(textString.codePointAt(start));
            drawable.drawEmoji(x, y, paint);
            return;
        }
        Log.e(TAG, "draw parameter is fail mEmojiDrawable = " + this.mEmojiDrawable);
    }
}
