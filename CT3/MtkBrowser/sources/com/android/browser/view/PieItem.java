package com.android.browser.view;

import android.view.View;
import com.android.browser.view.PieMenu;
import java.util.ArrayList;
import java.util.List;

public class PieItem {
    private float animate;
    private int inner;
    private int level;
    private boolean mEnabled = true;
    private List<PieItem> mItems;
    private PieMenu.PieView mPieView;
    private boolean mSelected;
    private View mView;
    private int outer;
    private float start;
    private float sweep;

    public PieItem(View view, int level) {
        this.mView = view;
        this.level = level;
        setAnimationAngle(getAnimationAngle());
        setAlpha(getAlpha());
    }

    public boolean hasItems() {
        return this.mItems != null;
    }

    public List<PieItem> getItems() {
        return this.mItems;
    }

    public void addItem(PieItem item) {
        if (this.mItems == null) {
            this.mItems = new ArrayList();
        }
        this.mItems.add(item);
    }

    public void setAlpha(float alpha) {
        if (this.mView == null) {
            return;
        }
        this.mView.setAlpha(alpha);
    }

    public float getAlpha() {
        if (this.mView != null) {
            return this.mView.getAlpha();
        }
        return 1.0f;
    }

    public void setAnimationAngle(float a) {
        this.animate = a;
    }

    public float getAnimationAngle() {
        return this.animate;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public void setSelected(boolean s) {
        this.mSelected = s;
        if (this.mView == null) {
            return;
        }
        this.mView.setSelected(s);
    }

    public boolean isSelected() {
        return this.mSelected;
    }

    public int getLevel() {
        return this.level;
    }

    public void setGeometry(float st, float sw, int inside, int outside) {
        this.start = st;
        this.sweep = sw;
        this.inner = inside;
        this.outer = outside;
    }

    public float getStart() {
        return this.start;
    }

    public float getStartAngle() {
        return this.start + this.animate;
    }

    public float getSweep() {
        return this.sweep;
    }

    public int getInnerRadius() {
        return this.inner;
    }

    public int getOuterRadius() {
        return this.outer;
    }

    public boolean isPieView() {
        return this.mPieView != null;
    }

    public View getView() {
        return this.mView;
    }

    public void setPieView(PieMenu.PieView sym) {
        this.mPieView = sym;
    }

    public PieMenu.PieView getPieView() {
        if (this.mEnabled) {
            return this.mPieView;
        }
        return null;
    }
}
