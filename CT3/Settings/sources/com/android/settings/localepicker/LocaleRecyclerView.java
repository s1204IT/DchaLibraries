package com.android.settings.localepicker;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

class LocaleRecyclerView extends RecyclerView {
    public LocaleRecyclerView(Context context) {
        super(context);
    }

    public LocaleRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LocaleRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        LocaleDragAndDropAdapter adapter;
        if (e.getAction() == 1 && (adapter = (LocaleDragAndDropAdapter) getAdapter()) != null) {
            adapter.doTheUpdate();
        }
        return super.onTouchEvent(e);
    }
}
