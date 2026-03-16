package com.android.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera2.R;

public class RadioOptions extends TopRightWeightedLayout {
    private Drawable mBackground;
    private OnOptionClickListener mOnOptionClickListener;

    public interface OnOptionClickListener {
        void onOptionClicked(View view);
    }

    public void setOnOptionClickListener(OnOptionClickListener listener) {
        this.mOnOptionClickListener = listener;
    }

    public RadioOptions(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RadioOptions, 0, 0);
        int drawableId = a.getResourceId(0, 0);
        if (drawableId > 0) {
            this.mBackground = context.getResources().getDrawable(drawableId);
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        updateListeners();
    }

    public void updateListeners() {
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View button) {
                RadioOptions.this.setSelectedOptionByView(button);
            }
        };
        for (int i = 0; i < getChildCount(); i++) {
            View button = getChildAt(i);
            button.setOnClickListener(onClickListener);
        }
    }

    public void setSelectedOptionByTag(Object tag) {
        View button = findViewWithTag(tag);
        setSelectedOptionByView(button);
    }

    public void setSeletedOptionById(int id) {
        View button = findViewById(id);
        setSelectedOptionByView(button);
    }

    private void setSelectedOptionByView(View view) {
        if (view != null) {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).setBackground(null);
            }
            view.setBackground(this.mBackground);
            if (this.mOnOptionClickListener != null) {
                this.mOnOptionClickListener.onOptionClicked(view);
            }
        }
    }
}
