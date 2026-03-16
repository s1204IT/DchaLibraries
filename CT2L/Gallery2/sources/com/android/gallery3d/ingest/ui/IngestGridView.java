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

    public IngestGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOnClearChoicesListener = null;
    }

    public IngestGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mOnClearChoicesListener = null;
    }

    public void setOnClearChoicesListener(OnClearChoicesListener l) {
        this.mOnClearChoicesListener = l;
    }

    @Override
    public void clearChoices() {
        super.clearChoices();
        if (this.mOnClearChoicesListener != null) {
            this.mOnClearChoicesListener.onClearChoices();
        }
    }
}
