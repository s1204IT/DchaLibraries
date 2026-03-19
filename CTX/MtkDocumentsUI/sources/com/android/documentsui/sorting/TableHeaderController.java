package com.android.documentsui.sorting;

import android.R;
import android.view.View;
import com.android.documentsui.sorting.SortController;
import com.android.documentsui.sorting.SortModel;

public final class TableHeaderController implements SortController.WidgetController {
    static final boolean $assertionsDisabled = false;
    private final HeaderCell mDateCell;
    private final HeaderCell mFileTypeCell;
    private final SortModel mModel;
    private final HeaderCell mSizeCell;
    private final HeaderCell mSummaryCell;
    private View mTableHeader;
    private final HeaderCell mTitleCell;
    private final View.OnClickListener mOnCellClickListener = new View.OnClickListener() {
        @Override
        public final void onClick(View view) {
            this.f$0.onCellClicked(view);
        }
    };
    private final SortModel.UpdateListener mModelListener = new SortModel.UpdateListener() {
        @Override
        public final void onModelUpdate(SortModel sortModel, int i) {
            this.f$0.onModelUpdate(sortModel, i);
        }
    };

    private TableHeaderController(SortModel sortModel, View view) {
        this.mModel = sortModel;
        this.mTableHeader = view;
        this.mTitleCell = (HeaderCell) view.findViewById(R.id.title);
        this.mSummaryCell = (HeaderCell) view.findViewById(R.id.summary);
        this.mSizeCell = (HeaderCell) view.findViewById(com.android.documentsui.R.id.size);
        this.mFileTypeCell = (HeaderCell) view.findViewById(com.android.documentsui.R.id.file_type);
        this.mDateCell = (HeaderCell) view.findViewById(com.android.documentsui.R.id.date);
        onModelUpdate(this.mModel, -1);
        this.mModel.addListener(this.mModelListener);
    }

    private void onModelUpdate(SortModel sortModel, int i) {
        bindCell(this.mTitleCell, R.id.title);
        bindCell(this.mSummaryCell, R.id.summary);
        bindCell(this.mSizeCell, com.android.documentsui.R.id.size);
        bindCell(this.mFileTypeCell, com.android.documentsui.R.id.file_type);
        bindCell(this.mDateCell, com.android.documentsui.R.id.date);
    }

    @Override
    public void setVisibility(int i) {
        this.mTableHeader.setVisibility(i);
    }

    @Override
    public void destroy() {
        this.mModel.removeListener(this.mModelListener);
    }

    private void bindCell(HeaderCell headerCell, int i) {
        SortDimension dimensionById = this.mModel.getDimensionById(i);
        headerCell.setTag(dimensionById);
        headerCell.onBind(dimensionById);
        if (dimensionById.getVisibility() == 0 && dimensionById.getSortCapability() != 0) {
            headerCell.setOnClickListener(this.mOnCellClickListener);
        } else {
            headerCell.setOnClickListener(null);
        }
    }

    private void onCellClicked(View view) {
        SortDimension sortDimension = (SortDimension) view.getTag();
        this.mModel.sortByUser(sortDimension.getId(), sortDimension.getNextDirection());
    }

    public static TableHeaderController create(SortModel sortModel, View view) {
        if (view == null) {
            return null;
        }
        return new TableHeaderController(sortModel, view);
    }
}
