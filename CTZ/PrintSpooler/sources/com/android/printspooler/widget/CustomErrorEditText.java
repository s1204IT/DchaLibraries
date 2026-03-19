package com.android.printspooler.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.EditText;

public final class CustomErrorEditText extends EditText {
    private CharSequence mError;

    public CustomErrorEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public CharSequence getError() {
        return this.mError;
    }

    @Override
    public void setError(CharSequence charSequence, Drawable drawable) {
        setCompoundDrawables(null, null, drawable, null);
        this.mError = charSequence;
    }
}
