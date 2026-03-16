package com.android.documentsui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import java.util.ArrayList;

public class DirectoryContainerView extends FrameLayout {
    private boolean mDisappearingFirst;

    public DirectoryContainerView(Context context) {
        super(context);
        this.mDisappearingFirst = false;
    }

    public DirectoryContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDisappearingFirst = false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        ArrayList<View> disappearing = this.mDisappearingChildren;
        if (this.mDisappearingFirst && disappearing != null) {
            for (int i = 0; i < disappearing.size(); i++) {
                super.drawChild(canvas, disappearing.get(i), getDrawingTime());
            }
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (this.mDisappearingFirst && this.mDisappearingChildren != null && this.mDisappearingChildren.contains(child)) {
            return false;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    public void setDrawDisappearingFirst(boolean disappearingFirst) {
        this.mDisappearingFirst = disappearingFirst;
    }
}
