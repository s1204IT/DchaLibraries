package com.android.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera.MultiToggleImageButton;

public class ToggleImageButton extends MultiToggleImageButton {
    private OnStateChangeListener mOnStateChangeListener;

    public interface OnStateChangeListener {
        void stateChanged(View view, boolean z);
    }

    public ToggleImageButton(Context context) {
        super(context);
    }

    public ToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ToggleImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.mOnStateChangeListener = onStateChangeListener;
    }

    public void setState(boolean state) {
        super.setState(state ? 1 : 0);
    }

    @Override
    protected void init() {
        super.init();
        super.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View v, int state) {
                if (ToggleImageButton.this.mOnStateChangeListener != null) {
                    ToggleImageButton.this.mOnStateChangeListener.stateChanged(v, state == 1);
                }
            }
        });
    }
}
