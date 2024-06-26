package com.android.systemui.recents.views.grid;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import com.android.systemui.R;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.shared.recents.model.TaskStack;
/* loaded from: classes.dex */
public class TaskViewFocusFrame extends View implements ViewTreeObserver.OnGlobalFocusChangeListener {
    private TaskStackView mSv;
    private TaskGridLayoutAlgorithm mTaskGridLayoutAlgorithm;

    public TaskViewFocusFrame(Context context) {
        this(context, null);
    }

    public TaskViewFocusFrame(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskViewFocusFrame(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TaskViewFocusFrame(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        setBackground(this.mContext.getDrawable(R.drawable.recents_grid_task_view_focus_frame_background));
        setFocusable(false);
        hide();
    }

    public TaskViewFocusFrame(Context context, TaskStackView taskStackView, TaskGridLayoutAlgorithm taskGridLayoutAlgorithm) {
        this(context);
        this.mSv = taskStackView;
        this.mTaskGridLayoutAlgorithm = taskGridLayoutAlgorithm;
    }

    public void measure() {
        int focusFrameThickness = this.mTaskGridLayoutAlgorithm.getFocusFrameThickness();
        Rect taskGridRect = this.mTaskGridLayoutAlgorithm.getTaskGridRect();
        int i = focusFrameThickness * 2;
        measure(View.MeasureSpec.makeMeasureSpec(taskGridRect.width() + i, 1073741824), View.MeasureSpec.makeMeasureSpec(taskGridRect.height() + i, 1073741824));
    }

    public void layout() {
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    public void resize() {
        if (this.mSv.useGridLayout()) {
            this.mTaskGridLayoutAlgorithm.updateTaskGridRect(this.mSv.getStack().getTaskCount());
            measure();
            requestLayout();
        }
    }

    public void moveGridTaskViewFocus(View view) {
        if (this.mSv.useGridLayout()) {
            if (view instanceof GridTaskView) {
                int[] iArr = new int[2];
                view.getLocationInWindow(iArr);
                int focusFrameThickness = this.mTaskGridLayoutAlgorithm.getFocusFrameThickness();
                setTranslationX(iArr[0] - focusFrameThickness);
                setTranslationY(iArr[1] - focusFrameThickness);
                show();
                return;
            }
            hide();
        }
    }

    @Override // android.view.ViewTreeObserver.OnGlobalFocusChangeListener
    public void onGlobalFocusChanged(View view, View view2) {
        if (!this.mSv.useGridLayout()) {
            return;
        }
        if (view2 == null) {
            moveGridTaskViewFocus(null);
        } else if (view == null) {
            TaskStack stack = this.mSv.getStack();
            int taskCount = stack.getTaskCount();
            int indexOfTask = stack.indexOfTask(this.mSv.getFocusedTask());
            this.mSv.setFocusedTask(indexOfTask == -1 ? taskCount - 1 : indexOfTask % taskCount, false, true);
        }
    }

    private void show() {
        setAlpha(1.0f);
    }

    private void hide() {
        setAlpha(0.0f);
    }
}
