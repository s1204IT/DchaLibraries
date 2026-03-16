package com.android.printspooler.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.EditText;

public final class CustomErrorEditText extends EditText {
    private CharSequence mError;

    public CustomErrorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getError() {
        return this.mError;
    }

    @Override
    public void setError(CharSequence error, Drawable icon) {
        setCompoundDrawables(null, null, icon, null);
        this.mError = error;
    }
}
