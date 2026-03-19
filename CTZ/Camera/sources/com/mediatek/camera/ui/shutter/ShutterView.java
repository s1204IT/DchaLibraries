package com.mediatek.camera.ui.shutter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.ui.shutter.ShutterButton;

class ShutterView extends RelativeLayout {
    private int mCenterX;
    private TextView mName;
    private int mScrollDistance;
    private ShutterButton mShutter;
    private LogUtil.Tag mTag;
    private OnShutterTextClicked mTextClickedListener;
    private String mType;

    public interface OnShutterTextClicked {
        void onShutterTextClicked(int i);
    }

    public ShutterView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setType(String str) {
        this.mType = str;
        this.mTag = new LogUtil.Tag(ShutterView.class.getSimpleName() + "_" + str);
        LogHelper.d(this.mTag, "setType " + str);
    }

    public void setName(String str) {
        this.mName.setText(str);
        LogHelper.d(this.mTag, "setName " + str);
        this.mShutter.setContentDescription(str);
    }

    public void setDrawable(Drawable drawable) {
        this.mShutter.setImageDrawable(drawable);
    }

    public void setOnShutterButtonListener(ShutterButton.OnShutterButtonListener onShutterButtonListener) {
        this.mShutter.setOnShutterButtonListener(onShutterButtonListener);
    }

    public void setOnShutterTextClickedListener(OnShutterTextClicked onShutterTextClicked) {
        this.mTextClickedListener = onShutterTextClicked;
    }

    public String getType() {
        return this.mType;
    }

    public void onScrolled(int i, int i2, int i3) {
        updateShutterView(i, i2, i3);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (this.mName != null && this.mName.getMeasuredHeight() != 0 && this.mShutter != null && this.mShutter.getMeasuredHeight() != 0) {
            int measuredHeight = this.mName.getMeasuredHeight() + this.mShutter.getMeasuredHeight();
            int iMax = Math.max(this.mName.getMeasuredWidth(), this.mShutter.getMeasuredWidth());
            if (iMax != 0 && measuredHeight != 0) {
                super.onMeasure(View.MeasureSpec.makeMeasureSpec(iMax, 1073741824), View.MeasureSpec.makeMeasureSpec(measuredHeight, 1073741824));
                return;
            } else {
                super.onMeasure(i, i2);
                return;
            }
        }
        super.onMeasure(i, i2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mCenterX = (((i3 - i) + 1) / 2) + i;
        if (getParent() != null) {
            updateShutterView(((View) getParent()).getScrollX(), (((View) getParent()).getWidth() + 1) / 2, 0);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mName = (TextView) findViewById(R.id.shutter_text);
        this.mShutter = (ShutterButton) findViewById(R.id.shutter_button);
        this.mName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ShutterView.this.mTextClickedListener != null) {
                    ShutterView.this.mTextClickedListener.onShutterTextClicked(((Integer) ShutterView.this.getTag()).intValue());
                }
            }
        });
    }

    @Override
    public void setEnabled(boolean z) {
        if (this.mShutter != null) {
            this.mShutter.setEnabled(z);
            this.mShutter.setClickable(z);
        }
        if (this.mName != null) {
            this.mName.setEnabled(z);
            this.mName.setClickable(z);
        }
    }

    public void setTextEnabled(boolean z) {
        if (this.mName != null) {
            this.mName.setEnabled(z);
            this.mName.setClickable(z);
        }
    }

    private void updateShutterView(int i, int i2, int i3) {
        int iAbs = Math.abs((this.mCenterX - i) - i2);
        this.mScrollDistance = i3;
        if (i3 == 0) {
            if (iAbs <= 2) {
                this.mShutter.setAlpha(1.0f);
                this.mShutter.setEnabled(true);
                return;
            } else {
                this.mShutter.setEnabled(false);
                this.mShutter.setAlpha(0.0f);
                return;
            }
        }
        if (iAbs <= 2) {
            this.mShutter.setAlpha(1.0f);
            this.mShutter.setEnabled(true);
        } else if (iAbs < i3) {
            this.mShutter.setAlpha(1.0f - ((iAbs * 1.0f) / i3));
        } else if (iAbs >= i3) {
            this.mShutter.setEnabled(false);
            this.mShutter.setAlpha(0.0f);
        }
    }
}
