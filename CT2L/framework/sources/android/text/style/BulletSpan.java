package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.Spanned;

public class BulletSpan implements LeadingMarginSpan, ParcelableSpan {
    private static final int BULLET_RADIUS = 3;
    public static final int STANDARD_GAP_WIDTH = 2;
    private static Path sBulletPath = null;
    private final int mColor;
    private final int mGapWidth;
    private final boolean mWantColor;

    public BulletSpan() {
        this.mGapWidth = 2;
        this.mWantColor = false;
        this.mColor = 0;
    }

    public BulletSpan(int gapWidth) {
        this.mGapWidth = gapWidth;
        this.mWantColor = false;
        this.mColor = 0;
    }

    public BulletSpan(int gapWidth, int color) {
        this.mGapWidth = gapWidth;
        this.mWantColor = true;
        this.mColor = color;
    }

    public BulletSpan(Parcel src) {
        this.mGapWidth = src.readInt();
        this.mWantColor = src.readInt() != 0;
        this.mColor = src.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return 8;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mGapWidth);
        dest.writeInt(this.mWantColor ? 1 : 0);
        dest.writeInt(this.mColor);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return this.mGapWidth + 6;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout l) {
        if (((Spanned) text).getSpanStart(this) == start) {
            Paint.Style style = p.getStyle();
            int oldcolor = 0;
            if (this.mWantColor) {
                oldcolor = p.getColor();
                p.setColor(this.mColor);
            }
            p.setStyle(Paint.Style.FILL);
            if (c.isHardwareAccelerated()) {
                if (sBulletPath == null) {
                    sBulletPath = new Path();
                    sBulletPath.addCircle(0.0f, 0.0f, 3.6000001f, Path.Direction.CW);
                }
                c.save();
                c.translate((dir * 3) + x, (top + bottom) / 2.0f);
                c.drawPath(sBulletPath, p);
                c.restore();
            } else {
                c.drawCircle((dir * 3) + x, (top + bottom) / 2.0f, 3.0f, p);
            }
            if (this.mWantColor) {
                p.setColor(oldcolor);
            }
            p.setStyle(style);
        }
    }
}
