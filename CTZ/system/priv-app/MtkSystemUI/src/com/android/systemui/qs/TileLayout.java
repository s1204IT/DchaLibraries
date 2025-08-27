package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class TileLayout extends ViewGroup implements QSPanel.QSTileLayout {
    protected int mCellHeight;
    protected int mCellMarginHorizontal;
    private int mCellMarginTop;
    protected int mCellMarginVertical;
    protected int mCellWidth;
    protected int mColumns;
    private boolean mListening;
    protected final ArrayList<QSPanel.TileRecord> mRecords;
    protected int mSidePadding;

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRecords = new ArrayList<>();
        setFocusableInTouchMode(true);
        updateResources();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public int getOffsetTop(QSPanel.TileRecord tileRecord) {
        return getTop();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void setListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.setListening(this, this.mListening);
        }
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void addTile(QSPanel.TileRecord tileRecord) {
        this.mRecords.add(tileRecord);
        tileRecord.tile.setListening(this, this.mListening);
        addView(tileRecord.tileView);
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void removeTile(QSPanel.TileRecord tileRecord) {
        this.mRecords.remove(tileRecord);
        tileRecord.tile.setListening(this, false);
        removeView(tileRecord.tileView);
    }

    @Override // android.view.ViewGroup
    public void removeAllViews() {
        Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.setListening(this, false);
        }
        this.mRecords.clear();
        super.removeAllViews();
    }

    public boolean updateResources() {
        Resources resources = this.mContext.getResources();
        int iMax = Math.max(1, resources.getInteger(R.integer.quick_settings_num_columns));
        this.mCellHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_tile_height);
        this.mCellMarginHorizontal = resources.getDimensionPixelSize(R.dimen.qs_tile_margin_horizontal);
        this.mCellMarginVertical = resources.getDimensionPixelSize(R.dimen.qs_tile_margin_vertical);
        this.mCellMarginTop = resources.getDimensionPixelSize(R.dimen.qs_tile_margin_top);
        this.mSidePadding = resources.getDimensionPixelOffset(R.dimen.qs_tile_layout_margin_side);
        if (this.mColumns != iMax) {
            this.mColumns = iMax;
            requestLayout();
            return true;
        }
        return false;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int size = this.mRecords.size();
        int size2 = (View.MeasureSpec.getSize(i) - getPaddingStart()) - getPaddingEnd();
        int i3 = ((size + this.mColumns) - 1) / this.mColumns;
        this.mCellWidth = ((size2 - (this.mSidePadding * 2)) - (this.mCellMarginHorizontal * this.mColumns)) / this.mColumns;
        Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
        View viewUpdateAccessibilityOrder = this;
        while (it.hasNext()) {
            QSPanel.TileRecord next = it.next();
            if (next.tileView.getVisibility() != 8) {
                next.tileView.measure(exactly(this.mCellWidth), exactly(this.mCellHeight));
                viewUpdateAccessibilityOrder = next.tileView.updateAccessibilityOrder(viewUpdateAccessibilityOrder);
            }
        }
        int i4 = (i3 != 0 ? this.mCellMarginTop - this.mCellMarginVertical : 0) + ((this.mCellHeight + this.mCellMarginVertical) * i3);
        if (i4 < 0) {
            i4 = 0;
        }
        setMeasuredDimension(size2, i4);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    private static int exactly(int i) {
        return View.MeasureSpec.makeMeasureSpec(i, 1073741824);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        getWidth();
        boolean z2 = getLayoutDirection() == 1;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        while (i5 < this.mRecords.size()) {
            if (i6 == this.mColumns) {
                i7++;
                i6 = 0;
            }
            QSPanel.TileRecord tileRecord = this.mRecords.get(i5);
            int rowTop = getRowTop(i7);
            int columnStart = getColumnStart(z2 ? (this.mColumns - i6) - 1 : i6);
            tileRecord.tileView.layout(columnStart, rowTop, this.mCellWidth + columnStart, tileRecord.tileView.getMeasuredHeight() + rowTop);
            i5++;
            i6++;
        }
    }

    private int getRowTop(int i) {
        return (i * (this.mCellHeight + this.mCellMarginVertical)) + this.mCellMarginTop;
    }

    private int getColumnStart(int i) {
        return getPaddingStart() + this.mSidePadding + (this.mCellMarginHorizontal / 2) + (i * (this.mCellWidth + this.mCellMarginHorizontal));
    }
}
