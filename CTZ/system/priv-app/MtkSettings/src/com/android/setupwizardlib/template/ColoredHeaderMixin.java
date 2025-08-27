package com.android.setupwizardlib.template;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

/* loaded from: classes.dex */
public class ColoredHeaderMixin extends HeaderMixin {
    public ColoredHeaderMixin(TemplateLayout templateLayout, AttributeSet attributeSet, int i) {
        super(templateLayout, attributeSet, i);
        TypedArray typedArrayObtainStyledAttributes = templateLayout.getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwColoredHeaderMixin, i, 0);
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(R.styleable.SuwColoredHeaderMixin_suwHeaderColor);
        if (colorStateList != null) {
            setColor(colorStateList);
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setColor(ColorStateList colorStateList) {
        TextView textView = getTextView();
        if (textView != null) {
            textView.setTextColor(colorStateList);
        }
    }

    public ColorStateList getColor() {
        TextView textView = getTextView();
        if (textView != null) {
            return textView.getTextColors();
        }
        return null;
    }
}
