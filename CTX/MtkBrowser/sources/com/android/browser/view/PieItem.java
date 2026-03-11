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

    public PieItem(View view, int i) {
        this.mView = view;
        this.level = i;
        setAnimationAngle(getAnimationAngle());
        setAlpha(getAlpha());
    }

    public void addItem(PieItem pieItem) {
        if (this.mItems == null) {
            this.mItems = new ArrayList();
        }
        this.mItems.add(pieItem);
    }

    public float getAlpha() {
        if (this.mView != null) {
            return this.mView.getAlpha();
        }
        return 1.0f;
    }

    public float getAnimationAngle() {
        return this.animate;
    }

    public int getInnerRadius() {
        return this.inner;
    }

    public List<PieItem> getItems() {
        return this.mItems;
    }

    public int getLevel() {
        return this.level;
    }

    public int getOuterRadius() {
        return this.outer;
    }

    public PieMenu.PieView getPieView() {
        if (this.mEnabled) {
            return this.mPieView;
        }
        return null;
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

    public View getView() {
        return this.mView;
    }

    public boolean hasItems() {
        return this.mItems != null;
    }

    public boolean isPieView() {
        return this.mPieView != null;
    }

    public boolean isSelected() {
        return this.mSelected;
    }

    public void setAlpha(float f) {
        if (this.mView != null) {
            this.mView.setAlpha(f);
        }
    }

    public void setAnimationAngle(float f) {
        this.animate = f;
    }

    public void setEnabled(boolean z) {
        this.mEnabled = z;
    }

    public void setGeometry(float f, float f2, int i, int i2) {
        this.start = f;
        this.sweep = f2;
        this.inner = i;
        this.outer = i2;
    }

    public void setPieView(PieMenu.PieView pieView) {
        this.mPieView = pieView;
    }

    public void setSelected(boolean z) {
        this.mSelected = z;
        if (this.mView != null) {
            this.mView.setSelected(z);
        }
    }
}
