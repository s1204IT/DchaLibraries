package com.android.browser.addbookmark;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

public class FolderSpinner extends Spinner implements AdapterView.OnItemSelectedListener {
    private boolean mFireSetSelection;
    private OnSetSelectionListener mOnSetSelectionListener;

    public interface OnSetSelectionListener {
        void onSetSelection(long j);
    }

    public FolderSpinner(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        super.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (this.mFireSetSelection) {
            this.mOnSetSelectionListener.onSetSelection(j);
            this.mFireSetSelection = false;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
        throw new RuntimeException("Cannot set an OnItemSelectedListener on a FolderSpinner");
    }

    public void setOnSetSelectionListener(OnSetSelectionListener onSetSelectionListener) {
        this.mOnSetSelectionListener = onSetSelectionListener;
    }

    @Override
    public void setSelection(int i) {
        this.mFireSetSelection = true;
        int selectedItemPosition = getSelectedItemPosition();
        super.setSelection(i);
        if (this.mOnSetSelectionListener == null || selectedItemPosition != i) {
            return;
        }
        onItemSelected(this, null, i, getAdapter().getItemId(i));
    }

    public void setSelectionIgnoringSelectionChange(int i) {
        super.setSelection(i);
    }
}
