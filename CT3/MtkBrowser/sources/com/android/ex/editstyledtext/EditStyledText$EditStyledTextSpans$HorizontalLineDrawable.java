package com.android.ex.editstyledtext;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

public class EditStyledText$EditStyledTextSpans$HorizontalLineDrawable extends ShapeDrawable {
    private static boolean DBG_HL = false;
    private Spannable mSpannable;
    private int mWidth;

    @Override
    public void draw(Canvas canvas) {
        renewColor();
        Rect rect = new Rect(0, 9, this.mWidth, 11);
        canvas.drawRect(rect, getPaint());
    }

    public void renewBounds(int width) {
        if (DBG_HL) {
            Log.d("EditStyledTextSpan", "--- renewBounds:" + width);
        }
        if (width > 20) {
            width -= 20;
        }
        this.mWidth = width;
        setBounds(0, 0, width, 20);
    }

    private void renewColor(int color) {
        if (DBG_HL) {
            Log.d("EditStyledTextSpan", "--- renewColor:" + color);
        }
        getPaint().setColor(color);
    }

    private void renewColor() {
        EditStyledText$EditStyledTextSpans$HorizontalLineSpan parent = getParentSpan();
        Spannable text = this.mSpannable;
        int start = text.getSpanStart(parent);
        int end = text.getSpanEnd(parent);
        ForegroundColorSpan[] spans = (ForegroundColorSpan[]) text.getSpans(start, end, ForegroundColorSpan.class);
        if (DBG_HL) {
            Log.d("EditStyledTextSpan", "--- renewColor:" + spans.length);
        }
        if (spans.length <= 0) {
            return;
        }
        renewColor(spans[spans.length - 1].getForegroundColor());
    }

    private EditStyledText$EditStyledTextSpans$HorizontalLineSpan getParentSpan() {
        Spannable text = this.mSpannable;
        EditStyledText$EditStyledTextSpans$HorizontalLineSpan[] images = (EditStyledText$EditStyledTextSpans$HorizontalLineSpan[]) text.getSpans(0, text.length(), EditStyledText$EditStyledTextSpans$HorizontalLineSpan.class);
        if (images.length > 0) {
            for (EditStyledText$EditStyledTextSpans$HorizontalLineSpan image : images) {
                if (image.getDrawable() == this) {
                    return image;
                }
            }
        }
        Log.e("EditStyledTextSpan", "---renewBounds: Couldn't find");
        return null;
    }
}
