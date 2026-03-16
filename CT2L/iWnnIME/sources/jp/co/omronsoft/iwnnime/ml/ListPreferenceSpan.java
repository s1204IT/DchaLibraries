package jp.co.omronsoft.iwnnime.ml;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class ListPreferenceSpan extends ImageSpan {
    private int mImageGap;
    private int mRigthGap;
    private int mStringGap;

    public ListPreferenceSpan(Drawable d, int verticalAlignment, int stringGap, int imageGap, int rightGap) {
        super(d, verticalAlignment);
        this.mStringGap = stringGap;
        this.mImageGap = imageGap;
        this.mRigthGap = rightGap;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        int result = super.getSize(paint, text, start, end, fm);
        if (fm != null) {
            fm.ascent += this.mStringGap;
            fm.top = fm.ascent;
        }
        return this.mRigthGap + result;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        super.draw(canvas, text, start, end, x, top, y, bottom + this.mImageGap, paint);
    }
}
