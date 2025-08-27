package com.android.launcher3.keyboard;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.android.launcher3.keyboard.FocusIndicatorHelper;

/* loaded from: classes.dex */
public class FocusedItemDecorator extends RecyclerView.ItemDecoration {
    private FocusIndicatorHelper mHelper;

    public FocusedItemDecorator(View view) {
        this.mHelper = new FocusIndicatorHelper.SimpleFocusIndicatorHelper(view);
    }

    public View.OnFocusChangeListener getFocusListener() {
        return this.mHelper;
    }

    @Override // android.support.v7.widget.RecyclerView.ItemDecoration
    public void onDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.State state) {
        this.mHelper.draw(canvas);
    }
}
