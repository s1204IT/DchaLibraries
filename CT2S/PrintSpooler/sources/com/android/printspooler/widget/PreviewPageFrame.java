package com.android.printspooler.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import com.android.printspooler.R;

public final class PreviewPageFrame extends LinearLayout {
    private final float mNotSelectedAlpha;
    private final float mNotSelectedElevation;
    private final float mSelectedElevation;
    private final float mSelectedPageAlpha;

    public PreviewPageFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSelectedElevation = this.mContext.getResources().getDimension(R.dimen.selected_page_elevation);
        this.mNotSelectedElevation = this.mContext.getResources().getDimension(R.dimen.unselected_page_elevation);
        this.mSelectedPageAlpha = this.mContext.getResources().getFraction(R.fraction.page_selected_alpha, 1, 1);
        this.mNotSelectedAlpha = this.mContext.getResources().getFraction(R.fraction.page_unselected_alpha, 1, 1);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(CompoundButton.class.getName());
        event.setChecked(isSelected());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(CompoundButton.class.getName());
        info.setSelected(false);
        info.setCheckable(true);
        info.setChecked(isSelected());
    }

    public void setSelected(boolean selected, boolean animate) {
        if (isSelected() != selected) {
            setSelected(selected);
            if (selected) {
                if (animate) {
                    animate().translationZ(this.mSelectedElevation).alpha(this.mSelectedPageAlpha);
                    return;
                } else {
                    setTranslationZ(this.mSelectedElevation);
                    setAlpha(this.mSelectedPageAlpha);
                    return;
                }
            }
            if (animate) {
                animate().translationZ(this.mNotSelectedElevation).alpha(this.mNotSelectedAlpha);
            } else {
                setTranslationZ(this.mNotSelectedElevation);
                setAlpha(this.mNotSelectedAlpha);
            }
        }
    }
}
