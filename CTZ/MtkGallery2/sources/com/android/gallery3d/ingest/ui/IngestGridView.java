package com.android.gallery3d.ingest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class IngestGridView extends GridView {
    private OnClearChoicesListener mOnClearChoicesListener;

    public interface OnClearChoicesListener {
        void onClearChoices();
    }

    public IngestGridView(Context context) {
        super(context);
        this.mOnClearChoicesListener = null;
    }

    public IngestGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mOnClearChoicesListener = null;
    }

    public IngestGridView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mOnClearChoicesListener = null;
    }

    public void setOnClearChoicesListener(OnClearChoicesListener onClearChoicesListener) {
        this.mOnClearChoicesListener = onClearChoicesListener;
    }

    @Override
    public void clearChoices() {
        super.clearChoices();
        if (this.mOnClearChoicesListener != null) {
            this.mOnClearChoicesListener.onClearChoices();
        }
    }
}
