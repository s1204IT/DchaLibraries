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

    public FolderSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnItemSelectedListener(this);
    }

    @Override
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener l) {
        throw new RuntimeException("Cannot set an OnItemSelectedListener on a FolderSpinner");
    }

    public void setOnSetSelectionListener(OnSetSelectionListener l) {
        this.mOnSetSelectionListener = l;
    }

    public void setSelectionIgnoringSelectionChange(int position) {
        super.setSelection(position);
    }

    @Override
    public void setSelection(int position) {
        this.mFireSetSelection = true;
        int oldPosition = getSelectedItemPosition();
        super.setSelection(position);
        if (this.mOnSetSelectionListener == null || oldPosition != position) {
            return;
        }
        long id = getAdapter().getItemId(position);
        onItemSelected(this, null, position, id);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!this.mFireSetSelection) {
            return;
        }
        this.mOnSetSelectionListener.onSetSelection(id);
        this.mFireSetSelection = false;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
