package com.android.launcher3.shortcuts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

public class DeepShortcutTextView extends BubbleTextView {
    private final Rect mDragHandleBounds;
    private final int mDragHandleWidth;
    private Toast mInstructionToast;
    private boolean mShowInstructionToast;

    public DeepShortcutTextView(Context context) {
        this(context, null, 0);
    }

    public DeepShortcutTextView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DeepShortcutTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDragHandleBounds = new Rect();
        this.mShowInstructionToast = false;
        Resources resources = getResources();
        this.mDragHandleWidth = resources.getDimensionPixelSize(R.dimen.popup_padding_end) + resources.getDimensionPixelSize(R.dimen.deep_shortcut_drag_handle_size) + (resources.getDimensionPixelSize(R.dimen.deep_shortcut_drawable_padding) / 2);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        this.mDragHandleBounds.set(0, 0, this.mDragHandleWidth, getMeasuredHeight());
        if (!Utilities.isRtl(getResources())) {
            this.mDragHandleBounds.offset(getMeasuredWidth() - this.mDragHandleBounds.width(), 0);
        }
    }

    @Override
    protected void applyCompoundDrawables(Drawable drawable) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mShowInstructionToast = this.mDragHandleBounds.contains((int) motionEvent.getX(), (int) motionEvent.getY());
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean performClick() {
        if (this.mShowInstructionToast) {
            showToast();
            return true;
        }
        return super.performClick();
    }

    private void showToast() {
        if (this.mInstructionToast != null) {
            this.mInstructionToast.cancel();
        }
        this.mInstructionToast = Toast.makeText(getContext(), Utilities.wrapForTts(getContext().getText(R.string.long_press_shortcut_to_add), getContext().getString(R.string.long_accessible_way_to_add_shortcut)), 0);
        this.mInstructionToast.show();
    }
}
