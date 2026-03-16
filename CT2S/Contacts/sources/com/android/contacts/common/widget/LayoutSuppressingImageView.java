package com.android.contacts.common.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class LayoutSuppressingImageView extends ImageView {
    public LayoutSuppressingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void requestLayout() {
        forceLayout();
    }
}
