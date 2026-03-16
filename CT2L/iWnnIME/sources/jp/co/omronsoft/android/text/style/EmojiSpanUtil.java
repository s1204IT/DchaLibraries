package jp.co.omronsoft.android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

public class EmojiSpanUtil {
    private static final boolean DEBUG = false;
    private static final String TAG = "EmojiSpanUtil";

    public static void drawTextState(Canvas canvas, float x, int top, int y, int bottom, Paint paint) {
        if (paint instanceof TextPaint) {
            TextPaint tp = (TextPaint) paint;
            float right = x + tp.getTextSize();
            if (tp.bgColor != 0) {
                int c = tp.getColor();
                Paint.Style s = tp.getStyle();
                tp.setColor(tp.bgColor);
                tp.setStyle(Paint.Style.FILL);
                canvas.drawRect(x, top, right, bottom, tp);
                paint.setStyle(s);
                paint.setColor(c);
            }
            if (tp.isUnderlineText()) {
                canvas.save();
                canvas.clipRect(x, top, right, bottom);
                canvas.drawText("\u3000\u3000", x, tp.baselineShift + y, tp);
                canvas.restore();
            }
        }
    }
}
