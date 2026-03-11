package android.support.design.circularreveal.coordinatorlayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.design.circularreveal.CircularRevealHelper;
import android.support.design.circularreveal.CircularRevealWidget;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

public class CircularRevealCoordinatorLayout extends CoordinatorLayout implements CircularRevealWidget {
    private final CircularRevealHelper helper;

    public CircularRevealCoordinatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.helper = new CircularRevealHelper(this);
    }

    @Override
    public void buildCircularRevealCache() {
        this.helper.buildCircularRevealCache();
    }

    @Override
    public void destroyCircularRevealCache() {
        this.helper.destroyCircularRevealCache();
    }

    @Override
    public void setRevealInfo(CircularRevealWidget.RevealInfo revealInfo) {
        this.helper.setRevealInfo(revealInfo);
    }

    @Override
    public CircularRevealWidget.RevealInfo getRevealInfo() {
        return this.helper.getRevealInfo();
    }

    @Override
    public void setCircularRevealScrimColor(int color) {
        this.helper.setCircularRevealScrimColor(color);
    }

    @Override
    public int getCircularRevealScrimColor() {
        return this.helper.getCircularRevealScrimColor();
    }

    @Override
    public void setCircularRevealOverlayDrawable(Drawable drawable) {
        this.helper.setCircularRevealOverlayDrawable(drawable);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.helper != null) {
            this.helper.draw(canvas);
        } else {
            super.draw(canvas);
        }
    }

    @Override
    public void actualDraw(Canvas canvas) {
        super.draw(canvas);
    }

    @Override
    public boolean isOpaque() {
        if (this.helper != null) {
            return this.helper.isOpaque();
        }
        return super.isOpaque();
    }

    @Override
    public boolean actualIsOpaque() {
        return super.isOpaque();
    }
}
