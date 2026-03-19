package com.android.internal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.R;

@RemoteViews.RemoteView
public class MediaNotificationView extends FrameLayout {
    private View mActions;
    private View mHeader;
    private int mImagePushIn;
    private View mMainColumn;
    private final int mNotificationContentImageMarginEnd;
    private final int mNotificationContentMarginEnd;
    private ImageView mRightIcon;

    public MediaNotificationView(Context context) {
        this(context, null);
    }

    public MediaNotificationView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MediaNotificationView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        boolean z = false;
        boolean z2 = this.mRightIcon.getVisibility() != 8;
        if (!z2) {
            resetHeaderIndention();
        }
        super.onMeasure(i, i2);
        int mode = View.MeasureSpec.getMode(i);
        this.mImagePushIn = 0;
        if (z2 && mode != 0) {
            int size = View.MeasureSpec.getSize(i) - this.mActions.getMeasuredWidth();
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mRightIcon.getLayoutParams();
            int marginEnd = marginLayoutParams.getMarginEnd();
            int iMax = size - marginEnd;
            int measuredHeight = getMeasuredHeight();
            if (iMax <= measuredHeight) {
                if (iMax < measuredHeight) {
                    iMax = Math.max(0, iMax);
                    this.mImagePushIn = measuredHeight - iMax;
                }
            } else {
                iMax = measuredHeight;
            }
            if (marginLayoutParams.width != measuredHeight || marginLayoutParams.height != measuredHeight) {
                marginLayoutParams.width = measuredHeight;
                marginLayoutParams.height = measuredHeight;
                this.mRightIcon.setLayoutParams(marginLayoutParams);
                z = true;
            }
            ViewGroup.MarginLayoutParams marginLayoutParams2 = (ViewGroup.MarginLayoutParams) this.mMainColumn.getLayoutParams();
            int i3 = iMax + marginEnd;
            int i4 = this.mNotificationContentMarginEnd + i3;
            if (i4 != marginLayoutParams2.getMarginEnd()) {
                marginLayoutParams2.setMarginEnd(i4);
                this.mMainColumn.setLayoutParams(marginLayoutParams2);
                z = true;
            }
            ViewGroup.MarginLayoutParams marginLayoutParams3 = (ViewGroup.MarginLayoutParams) this.mHeader.getLayoutParams();
            if (marginLayoutParams3.getMarginEnd() != i3) {
                marginLayoutParams3.setMarginEnd(i3);
                this.mHeader.setLayoutParams(marginLayoutParams3);
                z = true;
            }
            if (this.mHeader.getPaddingEnd() != this.mNotificationContentImageMarginEnd) {
                this.mHeader.setPaddingRelative(this.mHeader.getPaddingStart(), this.mHeader.getPaddingTop(), this.mNotificationContentImageMarginEnd, this.mHeader.getPaddingBottom());
                z = true;
            }
        }
        if (z) {
            super.onMeasure(i, i2);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mImagePushIn > 0) {
            this.mRightIcon.layout(this.mRightIcon.getLeft() + this.mImagePushIn, this.mRightIcon.getTop(), this.mRightIcon.getRight() + this.mImagePushIn, this.mRightIcon.getBottom());
        }
    }

    private void resetHeaderIndention() {
        if (this.mHeader.getPaddingEnd() != this.mNotificationContentMarginEnd) {
            this.mHeader.setPaddingRelative(this.mHeader.getPaddingStart(), this.mHeader.getPaddingTop(), this.mNotificationContentMarginEnd, this.mHeader.getPaddingBottom());
        }
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mHeader.getLayoutParams();
        marginLayoutParams.setMarginEnd(0);
        if (marginLayoutParams.getMarginEnd() != 0) {
            marginLayoutParams.setMarginEnd(0);
            this.mHeader.setLayoutParams(marginLayoutParams);
        }
    }

    public MediaNotificationView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mNotificationContentMarginEnd = context.getResources().getDimensionPixelSize(R.dimen.notification_content_margin_end);
        this.mNotificationContentImageMarginEnd = context.getResources().getDimensionPixelSize(R.dimen.notification_content_image_margin_end);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mRightIcon = (ImageView) findViewById(R.id.right_icon);
        this.mActions = findViewById(R.id.media_actions);
        this.mHeader = findViewById(R.id.notification_header);
        this.mMainColumn = findViewById(R.id.notification_main_column);
    }
}
