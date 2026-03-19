package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.Spanned;

public class BulletSpan implements LeadingMarginSpan, ParcelableSpan {
    private static final int STANDARD_BULLET_RADIUS = 4;
    private static final int STANDARD_COLOR = 0;
    public static final int STANDARD_GAP_WIDTH = 2;
    private Path mBulletPath;
    private final int mBulletRadius;
    private final int mColor;
    private final int mGapWidth;
    private final boolean mWantColor;

    public BulletSpan() {
        this(2, 0, false, 4);
    }

    public BulletSpan(int i) {
        this(i, 0, false, 4);
    }

    public BulletSpan(int i, int i2) {
        this(i, i2, true, 4);
    }

    public BulletSpan(int i, int i2, int i3) {
        this(i, i2, true, i3);
    }

    private BulletSpan(int i, int i2, boolean z, int i3) {
        this.mBulletPath = null;
        this.mGapWidth = i;
        this.mBulletRadius = i3;
        this.mColor = i2;
        this.mWantColor = z;
    }

    public BulletSpan(Parcel parcel) {
        this.mBulletPath = null;
        this.mGapWidth = parcel.readInt();
        this.mWantColor = parcel.readInt() != 0;
        this.mColor = parcel.readInt();
        this.mBulletRadius = parcel.readInt();
    }

    @Override
    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    @Override
    public int getSpanTypeIdInternal() {
        return 8;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeToParcelInternal(parcel, i);
    }

    @Override
    public void writeToParcelInternal(Parcel parcel, int i) {
        parcel.writeInt(this.mGapWidth);
        parcel.writeInt(this.mWantColor ? 1 : 0);
        parcel.writeInt(this.mColor);
        parcel.writeInt(this.mBulletRadius);
    }

    @Override
    public int getLeadingMargin(boolean z) {
        return (2 * this.mBulletRadius) + this.mGapWidth;
    }

    public int getGapWidth() {
        return this.mGapWidth;
    }

    public int getBulletRadius() {
        return this.mBulletRadius;
    }

    public int getColor() {
        return this.mColor;
    }

    @Override
    public void drawLeadingMargin(Canvas canvas, Paint paint, int i, int i2, int i3, int i4, int i5, CharSequence charSequence, int i6, int i7, boolean z, Layout layout) {
        if (((Spanned) charSequence).getSpanStart(this) == i6) {
            Paint.Style style = paint.getStyle();
            int color = 0;
            if (this.mWantColor) {
                color = paint.getColor();
                paint.setColor(this.mColor);
            }
            paint.setStyle(Paint.Style.FILL);
            if (layout != null) {
                i5 -= layout.getLineExtra(layout.getLineForOffset(i6));
            }
            float f = (i3 + i5) / 2.0f;
            float f2 = i + (i2 * this.mBulletRadius);
            if (canvas.isHardwareAccelerated()) {
                if (this.mBulletPath == null) {
                    this.mBulletPath = new Path();
                    this.mBulletPath.addCircle(0.0f, 0.0f, this.mBulletRadius, Path.Direction.CW);
                }
                canvas.save();
                canvas.translate(f2, f);
                canvas.drawPath(this.mBulletPath, paint);
                canvas.restore();
            } else {
                canvas.drawCircle(f2, f, this.mBulletRadius, paint);
            }
            if (this.mWantColor) {
                paint.setColor(color);
            }
            paint.setStyle(style);
        }
    }
}
