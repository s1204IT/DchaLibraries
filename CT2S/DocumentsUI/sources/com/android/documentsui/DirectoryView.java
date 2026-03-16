package com.android.documentsui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class DirectoryView extends FrameLayout {
    private float mPosition;
    private int mWidth;

    public DirectoryView(Context context) {
        super(context);
        this.mPosition = 0.0f;
    }

    public DirectoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPosition = 0.0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mWidth = w;
        setPosition(this.mPosition);
    }

    public void setPosition(float position) {
        this.mPosition = position;
        setX(this.mWidth > 0 ? this.mPosition * this.mWidth : 0.0f);
        if (this.mPosition != 0.0f) {
            setTranslationZ(getResources().getDimensionPixelSize(R.dimen.dir_elevation));
        } else {
            setTranslationZ(0.0f);
        }
    }
}
