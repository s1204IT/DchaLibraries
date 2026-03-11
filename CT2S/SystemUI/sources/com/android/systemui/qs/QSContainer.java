package com.android.systemui.qs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.systemui.R;

public class QSContainer extends FrameLayout {
    private int mHeightOverride;
    private QSPanel mQSPanel;

    public QSContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHeightOverride = -1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateBottom();
    }

    public void setHeightOverride(int heightOverride) {
        this.mHeightOverride = heightOverride;
        updateBottom();
    }

    public int getDesiredHeight() {
        return this.mQSPanel.isClosingDetail() ? this.mQSPanel.getGridHeight() + getPaddingTop() + getPaddingBottom() : getMeasuredHeight();
    }

    private void updateBottom() {
        int height = this.mHeightOverride != -1 ? this.mHeightOverride : getMeasuredHeight();
        setBottom(getTop() + height);
    }
}
