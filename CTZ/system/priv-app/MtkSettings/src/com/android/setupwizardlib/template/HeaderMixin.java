package com.android.setupwizardlib.template;

import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

/* loaded from: classes.dex */
public class HeaderMixin implements Mixin {
    private TemplateLayout mTemplateLayout;

    public HeaderMixin(TemplateLayout templateLayout, AttributeSet attributeSet, int i) {
        this.mTemplateLayout = templateLayout;
        TypedArray typedArrayObtainStyledAttributes = templateLayout.getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwHeaderMixin, i, 0);
        CharSequence text = typedArrayObtainStyledAttributes.getText(R.styleable.SuwHeaderMixin_suwHeaderText);
        if (text != null) {
            setText(text);
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    public TextView getTextView() {
        return (TextView) this.mTemplateLayout.findManagedViewById(R.id.suw_layout_title);
    }

    public void setText(int i) {
        TextView textView = getTextView();
        if (textView != null) {
            textView.setText(i);
        }
    }

    public void setText(CharSequence charSequence) {
        TextView textView = getTextView();
        if (textView != null) {
            textView.setText(charSequence);
        }
    }

    public CharSequence getText() {
        TextView textView = getTextView();
        if (textView != null) {
            return textView.getText();
        }
        return null;
    }
}
