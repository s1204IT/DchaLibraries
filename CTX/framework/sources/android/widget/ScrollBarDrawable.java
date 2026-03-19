package android.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.android.internal.widget.ScrollBarUtils;

public class ScrollBarDrawable extends Drawable implements Drawable.Callback {
    private int mAlpha = 255;
    private boolean mAlwaysDrawHorizontalTrack;
    private boolean mAlwaysDrawVerticalTrack;
    private boolean mBoundsChanged;
    private ColorFilter mColorFilter;
    private int mExtent;
    private boolean mHasSetAlpha;
    private boolean mHasSetColorFilter;
    private Drawable mHorizontalThumb;
    private Drawable mHorizontalTrack;
    private boolean mMutated;
    private int mOffset;
    private int mRange;
    private boolean mRangeChanged;
    private boolean mVertical;
    private Drawable mVerticalThumb;
    private Drawable mVerticalTrack;

    public void setAlwaysDrawHorizontalTrack(boolean z) {
        this.mAlwaysDrawHorizontalTrack = z;
    }

    public void setAlwaysDrawVerticalTrack(boolean z) {
        this.mAlwaysDrawVerticalTrack = z;
    }

    public boolean getAlwaysDrawVerticalTrack() {
        return this.mAlwaysDrawVerticalTrack;
    }

    public boolean getAlwaysDrawHorizontalTrack() {
        return this.mAlwaysDrawHorizontalTrack;
    }

    public void setParameters(int i, int i2, int i3, boolean z) {
        if (this.mVertical != z) {
            this.mVertical = z;
            this.mBoundsChanged = true;
        }
        if (this.mRange != i || this.mOffset != i2 || this.mExtent != i3) {
            this.mRange = i;
            this.mOffset = i2;
            this.mExtent = i3;
            this.mRangeChanged = true;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        boolean z;
        boolean z2 = this.mVertical;
        int i = this.mExtent;
        int i2 = this.mRange;
        boolean z3 = true;
        if (i <= 0 || i2 <= i) {
            z3 = z2 ? this.mAlwaysDrawVerticalTrack : this.mAlwaysDrawHorizontalTrack;
            z = false;
        } else {
            z = true;
        }
        Rect bounds = getBounds();
        if (canvas.quickReject(bounds.left, bounds.top, bounds.right, bounds.bottom, Canvas.EdgeType.AA)) {
            return;
        }
        if (z3) {
            drawTrack(canvas, bounds, z2);
        }
        if (z) {
            int iHeight = z2 ? bounds.height() : bounds.width();
            int thumbLength = ScrollBarUtils.getThumbLength(iHeight, z2 ? bounds.width() : bounds.height(), i, i2);
            drawThumb(canvas, bounds, ScrollBarUtils.getThumbOffset(iHeight, thumbLength, i, i2, this.mOffset), thumbLength, z2);
        }
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        this.mBoundsChanged = true;
    }

    @Override
    public boolean isStateful() {
        return (this.mVerticalTrack != null && this.mVerticalTrack.isStateful()) || (this.mVerticalThumb != null && this.mVerticalThumb.isStateful()) || ((this.mHorizontalTrack != null && this.mHorizontalTrack.isStateful()) || ((this.mHorizontalThumb != null && this.mHorizontalThumb.isStateful()) || super.isStateful()));
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        boolean zOnStateChange = super.onStateChange(iArr);
        if (this.mVerticalTrack != null) {
            zOnStateChange |= this.mVerticalTrack.setState(iArr);
        }
        if (this.mVerticalThumb != null) {
            zOnStateChange |= this.mVerticalThumb.setState(iArr);
        }
        if (this.mHorizontalTrack != null) {
            zOnStateChange |= this.mHorizontalTrack.setState(iArr);
        }
        if (this.mHorizontalThumb != null) {
            return zOnStateChange | this.mHorizontalThumb.setState(iArr);
        }
        return zOnStateChange;
    }

    private void drawTrack(Canvas canvas, Rect rect, boolean z) {
        Drawable drawable;
        if (z) {
            drawable = this.mVerticalTrack;
        } else {
            drawable = this.mHorizontalTrack;
        }
        if (drawable != null) {
            if (this.mBoundsChanged) {
                drawable.setBounds(rect);
            }
            drawable.draw(canvas);
        }
    }

    private void drawThumb(Canvas canvas, Rect rect, int i, int i2, boolean z) {
        boolean z2 = this.mRangeChanged || this.mBoundsChanged;
        if (z) {
            if (this.mVerticalThumb != null) {
                Drawable drawable = this.mVerticalThumb;
                if (z2) {
                    drawable.setBounds(rect.left, rect.top + i, rect.right, rect.top + i + i2);
                }
                drawable.draw(canvas);
                return;
            }
            return;
        }
        if (this.mHorizontalThumb != null) {
            Drawable drawable2 = this.mHorizontalThumb;
            if (z2) {
                drawable2.setBounds(rect.left + i, rect.top, rect.left + i + i2, rect.bottom);
            }
            drawable2.draw(canvas);
        }
    }

    public void setVerticalThumbDrawable(Drawable drawable) {
        if (this.mVerticalThumb != null) {
            this.mVerticalThumb.setCallback(null);
        }
        propagateCurrentState(drawable);
        this.mVerticalThumb = drawable;
    }

    public void setVerticalTrackDrawable(Drawable drawable) {
        if (this.mVerticalTrack != null) {
            this.mVerticalTrack.setCallback(null);
        }
        propagateCurrentState(drawable);
        this.mVerticalTrack = drawable;
    }

    public void setHorizontalThumbDrawable(Drawable drawable) {
        if (this.mHorizontalThumb != null) {
            this.mHorizontalThumb.setCallback(null);
        }
        propagateCurrentState(drawable);
        this.mHorizontalThumb = drawable;
    }

    public void setHorizontalTrackDrawable(Drawable drawable) {
        if (this.mHorizontalTrack != null) {
            this.mHorizontalTrack.setCallback(null);
        }
        propagateCurrentState(drawable);
        this.mHorizontalTrack = drawable;
    }

    private void propagateCurrentState(Drawable drawable) {
        if (drawable != null) {
            if (this.mMutated) {
                drawable.mutate();
            }
            drawable.setState(getState());
            drawable.setCallback(this);
            if (this.mHasSetAlpha) {
                drawable.setAlpha(this.mAlpha);
            }
            if (this.mHasSetColorFilter) {
                drawable.setColorFilter(this.mColorFilter);
            }
        }
    }

    public int getSize(boolean z) {
        if (z) {
            if (this.mVerticalTrack != null) {
                return this.mVerticalTrack.getIntrinsicWidth();
            }
            if (this.mVerticalThumb != null) {
                return this.mVerticalThumb.getIntrinsicWidth();
            }
            return 0;
        }
        if (this.mHorizontalTrack != null) {
            return this.mHorizontalTrack.getIntrinsicHeight();
        }
        if (this.mHorizontalThumb != null) {
            return this.mHorizontalThumb.getIntrinsicHeight();
        }
        return 0;
    }

    @Override
    public ScrollBarDrawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            if (this.mVerticalTrack != null) {
                this.mVerticalTrack.mutate();
            }
            if (this.mVerticalThumb != null) {
                this.mVerticalThumb.mutate();
            }
            if (this.mHorizontalTrack != null) {
                this.mHorizontalTrack.mutate();
            }
            if (this.mHorizontalThumb != null) {
                this.mHorizontalThumb.mutate();
            }
            this.mMutated = true;
        }
        return this;
    }

    @Override
    public void setAlpha(int i) {
        this.mAlpha = i;
        this.mHasSetAlpha = true;
        if (this.mVerticalTrack != null) {
            this.mVerticalTrack.setAlpha(i);
        }
        if (this.mVerticalThumb != null) {
            this.mVerticalThumb.setAlpha(i);
        }
        if (this.mHorizontalTrack != null) {
            this.mHorizontalTrack.setAlpha(i);
        }
        if (this.mHorizontalThumb != null) {
            this.mHorizontalThumb.setAlpha(i);
        }
    }

    @Override
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mColorFilter = colorFilter;
        this.mHasSetColorFilter = true;
        if (this.mVerticalTrack != null) {
            this.mVerticalTrack.setColorFilter(colorFilter);
        }
        if (this.mVerticalThumb != null) {
            this.mVerticalThumb.setColorFilter(colorFilter);
        }
        if (this.mHorizontalTrack != null) {
            this.mHorizontalTrack.setColorFilter(colorFilter);
        }
        if (this.mHorizontalThumb != null) {
            this.mHorizontalThumb.setColorFilter(colorFilter);
        }
    }

    @Override
    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable drawable, Runnable runnable, long j) {
        scheduleSelf(runnable, j);
    }

    @Override
    public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
        unscheduleSelf(runnable);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ScrollBarDrawable: range=");
        sb.append(this.mRange);
        sb.append(" offset=");
        sb.append(this.mOffset);
        sb.append(" extent=");
        sb.append(this.mExtent);
        sb.append(this.mVertical ? " V" : " H");
        return sb.toString();
    }
}
