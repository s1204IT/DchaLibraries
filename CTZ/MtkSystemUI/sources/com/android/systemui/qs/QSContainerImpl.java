package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.qs.customize.QSCustomizer;

public class QSContainerImpl extends FrameLayout {
    private View mBackground;
    private View mBackgroundGradient;
    private QuickStatusBarHeader mHeader;
    private int mHeightOverride;
    private QSCustomizer mQSCustomizer;
    private View mQSDetail;
    private View mQSFooter;
    private QSPanel mQSPanel;
    private boolean mQsDisabled;
    private float mQsExpansion;
    private int mSideMargins;
    private final Point mSizePoint;
    private View mStatusBarBackground;

    public QSContainerImpl(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSizePoint = new Point();
        this.mHeightOverride = -1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
        this.mQSDetail = findViewById(R.id.qs_detail);
        this.mHeader = (QuickStatusBarHeader) findViewById(R.id.header);
        this.mQSCustomizer = (QSCustomizer) findViewById(R.id.qs_customize);
        this.mQSFooter = findViewById(R.id.qs_footer);
        this.mBackground = findViewById(R.id.quick_settings_background);
        this.mStatusBarBackground = findViewById(R.id.quick_settings_status_bar_background);
        this.mBackgroundGradient = findViewById(R.id.quick_settings_gradient_view);
        this.mSideMargins = getResources().getDimensionPixelSize(R.dimen.notification_side_paddings);
        setClickable(true);
        setImportantForAccessibility(2);
        setMargins();
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (configuration.orientation == 2) {
            this.mBackgroundGradient.setVisibility(4);
            this.mStatusBarBackground.setVisibility(4);
        } else {
            this.mBackgroundGradient.setVisibility(0);
            this.mStatusBarBackground.setVisibility(0);
        }
        updateResources();
        this.mSizePoint.set(0, 0);
    }

    @Override
    public boolean performClick() {
        return true;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        this.mQSPanel.measure(i, View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i2), 0));
        int measuredWidth = this.mQSPanel.getMeasuredWidth();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mQSPanel.getLayoutParams();
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(layoutParams.topMargin + layoutParams.bottomMargin + this.mQSPanel.getMeasuredHeight(), 1073741824));
        this.mQSCustomizer.measure(i, View.MeasureSpec.makeMeasureSpec(getDisplayHeight(), 1073741824));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateExpansion();
    }

    public void disable(int i, int i2, boolean z) {
        boolean z2 = true;
        if ((i2 & 1) == 0) {
            z2 = false;
        }
        if (z2 == this.mQsDisabled) {
            return;
        }
        this.mQsDisabled = z2;
        this.mBackgroundGradient.setVisibility(this.mQsDisabled ? 8 : 0);
        this.mBackground.setVisibility(this.mQsDisabled ? 8 : 0);
    }

    private void updateResources() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mQSPanel.getLayoutParams();
        layoutParams.topMargin = this.mContext.getResources().getDimensionPixelSize(android.R.dimen.emphasized_button_stroke_width);
        this.mQSPanel.setLayoutParams(layoutParams);
    }

    public void setHeightOverride(int i) {
        this.mHeightOverride = i;
        updateExpansion();
    }

    public void updateExpansion() {
        int iCalculateContainerHeight = calculateContainerHeight();
        setBottom(getTop() + iCalculateContainerHeight);
        this.mQSDetail.setBottom(getTop() + iCalculateContainerHeight);
        this.mQSFooter.setTranslationY(iCalculateContainerHeight - this.mQSFooter.getHeight());
        this.mBackground.setTop(this.mQSPanel.getTop());
        this.mBackground.setBottom(iCalculateContainerHeight);
    }

    protected int calculateContainerHeight() {
        return this.mQSCustomizer.isCustomizing() ? this.mQSCustomizer.getHeight() : Math.round(this.mQsExpansion * ((this.mHeightOverride != -1 ? this.mHeightOverride : getMeasuredHeight()) - this.mHeader.getHeight())) + this.mHeader.getHeight();
    }

    public void setExpansion(float f) {
        this.mQsExpansion = f;
        updateExpansion();
    }

    private void setMargins() {
        setMargins(this.mQSDetail);
        setMargins(this.mBackground);
        setMargins(this.mQSFooter);
        this.mQSPanel.setMargins(this.mSideMargins);
        this.mHeader.setMargins(this.mSideMargins);
    }

    private void setMargins(View view) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.rightMargin = this.mSideMargins;
        layoutParams.leftMargin = this.mSideMargins;
    }

    private int getDisplayHeight() {
        if (this.mSizePoint.y == 0) {
            getDisplay().getRealSize(this.mSizePoint);
        }
        return this.mSizePoint.y;
    }
}
