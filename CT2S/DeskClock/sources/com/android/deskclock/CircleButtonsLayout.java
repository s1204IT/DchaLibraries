package com.android.deskclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class CircleButtonsLayout extends FrameLayout {
    private int mCircleTimerViewId;
    private Context mContext;
    private CircleTimerView mCtv;
    private float mDiamOffset;
    private FrameLayout mLabel;
    private int mLabelId;
    private TextView mLabelText;
    private int mLabelTextId;
    private ImageButton mResetAddButton;
    private int mResetAddButtonId;
    private float mStrokeSize;

    public CircleButtonsLayout(Context context) {
        this(context, null);
        this.mContext = context;
    }

    public CircleButtonsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public void setCircleTimerViewIds(int circleTimerViewId, int stopButtonId, int labelId, int labelTextId) {
        this.mCircleTimerViewId = circleTimerViewId;
        this.mResetAddButtonId = stopButtonId;
        this.mLabelId = labelId;
        this.mLabelTextId = labelTextId;
        float dotStrokeSize = this.mContext.getResources().getDimension(R.dimen.circletimer_dot_size);
        float markerStrokeSize = this.mContext.getResources().getDimension(R.dimen.circletimer_marker_size);
        this.mStrokeSize = this.mContext.getResources().getDimension(R.dimen.circletimer_circle_size);
        this.mDiamOffset = Utils.calculateRadiusOffset(this.mStrokeSize, dotStrokeSize, markerStrokeSize) * 2.0f;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        remeasureViews();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void remeasureViews() {
        if (this.mCtv == null) {
            this.mCtv = (CircleTimerView) findViewById(this.mCircleTimerViewId);
            if (this.mCtv != null) {
                this.mResetAddButton = (ImageButton) findViewById(this.mResetAddButtonId);
                this.mLabel = (FrameLayout) findViewById(this.mLabelId);
                this.mLabelText = (TextView) findViewById(this.mLabelTextId);
            } else {
                return;
            }
        }
        int frameWidth = this.mCtv.getMeasuredWidth();
        int frameHeight = this.mCtv.getMeasuredHeight();
        int minBound = Math.min(frameWidth, frameHeight);
        int circleDiam = (int) (minBound - this.mDiamOffset);
        if (this.mResetAddButton != null) {
            ViewGroup.MarginLayoutParams resetAddParams = (ViewGroup.MarginLayoutParams) this.mResetAddButton.getLayoutParams();
            resetAddParams.bottomMargin = circleDiam / 6;
            if (minBound == frameWidth) {
                resetAddParams.bottomMargin += (frameHeight - frameWidth) / 2;
            }
        }
        if (this.mLabel != null) {
            ViewGroup.MarginLayoutParams labelParams = (ViewGroup.MarginLayoutParams) this.mLabel.getLayoutParams();
            labelParams.topMargin = circleDiam / 6;
            if (minBound == frameWidth) {
                labelParams.topMargin += (frameHeight - frameWidth) / 2;
            }
            int r = circleDiam / 2;
            int y = (frameHeight / 2) - labelParams.topMargin;
            double w = 2.0d * Math.sqrt((r + y) * (r - y));
            this.mLabelText.setMaxWidth((int) w);
        }
    }
}
