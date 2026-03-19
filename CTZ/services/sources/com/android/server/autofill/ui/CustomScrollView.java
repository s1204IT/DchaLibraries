package com.android.server.autofill.ui;

import android.R;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.TypedValue;
import android.widget.ScrollView;
import com.android.server.autofill.Helper;

public class CustomScrollView extends ScrollView {
    private static final String TAG = "CustomScrollView";
    private int mHeight;
    private int mWidth;

    public CustomScrollView(Context context) {
        super(context);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    public CustomScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    public CustomScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    public CustomScrollView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (getChildCount() == 0) {
            Slog.e(TAG, "no children");
        } else {
            calculateDimensions();
            setMeasuredDimension(this.mWidth, this.mHeight);
        }
    }

    private void calculateDimensions() {
        if (this.mWidth != -1) {
            return;
        }
        TypedValue typedValue = new TypedValue();
        Point point = new Point();
        Context context = getContext();
        context.getDisplay().getSize(point);
        context.getTheme().resolveAttribute(R.bool.auto_data_switch_ping_test_before_switch, typedValue, true);
        int measuredHeight = getChildAt(0).getMeasuredHeight();
        int fraction = (int) typedValue.getFraction(point.y, point.y);
        this.mWidth = point.x;
        this.mHeight = Math.min(measuredHeight, fraction);
        if (Helper.sDebug) {
            Slog.d(TAG, "calculateDimensions(): maxHeight=" + fraction + ", childHeight=" + measuredHeight + ", w=" + this.mWidth + ", h=" + this.mHeight);
        }
    }
}
