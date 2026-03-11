package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;

public class TileLayout extends ViewGroup implements QSPanel.QSTileLayout {
    protected int mCellHeight;
    protected int mCellMargin;
    private int mCellMarginTop;
    protected int mCellWidth;
    protected int mColumns;
    private boolean mListening;
    protected final ArrayList<QSPanel.TileRecord> mRecords;

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRecords = new ArrayList<>();
        setFocusableInTouchMode(true);
        updateResources();
    }

    @Override
    public int getOffsetTop(QSPanel.TileRecord tile) {
        return getTop();
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        for (QSPanel.TileRecord record : this.mRecords) {
            record.tile.setListening(this, this.mListening);
        }
    }

    @Override
    public void addTile(QSPanel.TileRecord tile) {
        this.mRecords.add(tile);
        tile.tile.setListening(this, this.mListening);
        addView(tile.tileView);
    }

    @Override
    public void removeTile(QSPanel.TileRecord tile) {
        this.mRecords.remove(tile);
        tile.tile.setListening(this, false);
        removeView(tile.tileView);
    }

    @Override
    public void removeAllViews() {
        for (QSPanel.TileRecord record : this.mRecords) {
            record.tile.setListening(this, false);
        }
        this.mRecords.clear();
        super.removeAllViews();
    }

    public boolean updateResources() {
        Resources res = this.mContext.getResources();
        int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        this.mCellHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_tile_height);
        this.mCellMargin = res.getDimensionPixelSize(R.dimen.qs_tile_margin);
        this.mCellMarginTop = res.getDimensionPixelSize(R.dimen.qs_tile_margin_top);
        if (this.mColumns != columns) {
            this.mColumns = columns;
            requestLayout();
            return true;
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int numTiles = this.mRecords.size();
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int rows = ((this.mColumns + numTiles) - 1) / this.mColumns;
        this.mCellWidth = (width - (this.mCellMargin * (this.mColumns + 1))) / this.mColumns;
        View previousView = this;
        for (QSPanel.TileRecord record : this.mRecords) {
            if (record.tileView.getVisibility() != 8) {
                record.tileView.measure(exactly(this.mCellWidth), exactly(this.mCellHeight));
                previousView = record.tileView.updateAccessibilityOrder(previousView);
            }
        }
        setMeasuredDimension(width, ((this.mCellHeight + this.mCellMargin) * rows) + (this.mCellMarginTop - this.mCellMargin));
    }

    private static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int right;
        int w = getWidth();
        boolean isRtl = getLayoutDirection() == 1;
        int row = 0;
        int column = 0;
        int i = 0;
        while (i < this.mRecords.size()) {
            if (column == this.mColumns) {
                row++;
                column -= this.mColumns;
            }
            QSPanel.TileRecord record = this.mRecords.get(i);
            int left = getColumnStart(column);
            int top = getRowTop(row);
            if (isRtl) {
                right = w - left;
                left = right - this.mCellWidth;
            } else {
                right = left + this.mCellWidth;
            }
            record.tileView.layout(left, top, right, record.tileView.getMeasuredHeight() + top);
            i++;
            column++;
        }
    }

    private int getRowTop(int row) {
        return ((this.mCellHeight + this.mCellMargin) * row) + this.mCellMarginTop;
    }

    private int getColumnStart(int column) {
        return ((this.mCellWidth + this.mCellMargin) * column) + this.mCellMargin;
    }
}
