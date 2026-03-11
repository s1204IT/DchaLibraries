package com.android.settings.localepicker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settings.R;

class LocaleDragCell extends RelativeLayout {
    private CheckBox mCheckbox;
    private ImageView mDragHandle;
    private TextView mLabel;
    private TextView mLocalized;
    private TextView mMiniLabel;

    public LocaleDragCell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLabel = (TextView) findViewById(R.id.label);
        this.mLocalized = (TextView) findViewById(R.id.l10nWarn);
        this.mMiniLabel = (TextView) findViewById(R.id.miniLabel);
        this.mCheckbox = (CheckBox) findViewById(R.id.checkbox);
        this.mDragHandle = (ImageView) findViewById(R.id.dragHandle);
    }

    public void setShowHandle(boolean showHandle) {
        this.mDragHandle.setVisibility(showHandle ? 0 : 4);
        invalidate();
        requestLayout();
    }

    public void setShowCheckbox(boolean showCheckbox) {
        if (showCheckbox) {
            this.mCheckbox.setVisibility(0);
            this.mLabel.setVisibility(4);
        } else {
            this.mCheckbox.setVisibility(4);
            this.mLabel.setVisibility(0);
        }
        invalidate();
        requestLayout();
    }

    public void setChecked(boolean checked) {
        this.mCheckbox.setChecked(checked);
    }

    public void setShowMiniLabel(boolean showMiniLabel) {
        this.mMiniLabel.setVisibility(showMiniLabel ? 0 : 8);
        invalidate();
        requestLayout();
    }

    public void setMiniLabel(String miniLabelText) {
        this.mMiniLabel.setText(miniLabelText);
        invalidate();
    }

    public void setLabelAndDescription(String labelText, String description) {
        this.mLabel.setText(labelText);
        this.mCheckbox.setText(labelText);
        this.mLabel.setContentDescription(description);
        this.mCheckbox.setContentDescription(description);
        invalidate();
    }

    public void setLocalized(boolean localized) {
        this.mLocalized.setVisibility(localized ? 8 : 0);
        invalidate();
    }

    public ImageView getDragHandle() {
        return this.mDragHandle;
    }

    public CheckBox getCheckbox() {
        return this.mCheckbox;
    }
}
