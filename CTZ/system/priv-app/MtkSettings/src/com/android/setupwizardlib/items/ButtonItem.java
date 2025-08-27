package com.android.setupwizardlib.items;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.setupwizardlib.R;

/* loaded from: classes.dex */
public class ButtonItem extends AbstractItem implements View.OnClickListener {
    private Button mButton;
    private boolean mEnabled;
    private OnClickListener mListener;
    private CharSequence mText;
    private int mTheme;

    public interface OnClickListener {
        void onClick(ButtonItem buttonItem);
    }

    public ButtonItem() {
        this.mEnabled = true;
        this.mTheme = R.style.SuwButtonItem;
    }

    public ButtonItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEnabled = true;
        this.mTheme = R.style.SuwButtonItem;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwButtonItem);
        this.mEnabled = typedArrayObtainStyledAttributes.getBoolean(R.styleable.SuwButtonItem_android_enabled, true);
        this.mText = typedArrayObtainStyledAttributes.getText(R.styleable.SuwButtonItem_android_text);
        this.mTheme = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwButtonItem_android_theme, R.style.SuwButtonItem);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override // com.android.setupwizardlib.items.AbstractItem, com.android.setupwizardlib.items.ItemHierarchy
    public int getCount() {
        return 0;
    }

    @Override // com.android.setupwizardlib.items.IItem
    public boolean isEnabled() {
        return this.mEnabled;
    }

    @Override // com.android.setupwizardlib.items.IItem
    public int getLayoutResource() {
        return 0;
    }

    @Override // com.android.setupwizardlib.items.IItem
    public final void onBindView(View view) {
        throw new UnsupportedOperationException("Cannot bind to ButtonItem's view");
    }

    protected Button createButton(ViewGroup viewGroup) {
        if (this.mButton == null) {
            Context context = viewGroup.getContext();
            if (this.mTheme != 0) {
                context = new ContextThemeWrapper(context, this.mTheme);
            }
            this.mButton = createButton(context);
            this.mButton.setOnClickListener(this);
        } else if (this.mButton.getParent() instanceof ViewGroup) {
            ((ViewGroup) this.mButton.getParent()).removeView(this.mButton);
        }
        this.mButton.setEnabled(this.mEnabled);
        this.mButton.setText(this.mText);
        this.mButton.setId(getViewId());
        return this.mButton;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (this.mListener != null) {
            this.mListener.onClick(this);
        }
    }

    @SuppressLint({"InflateParams"})
    private Button createButton(Context context) {
        return (Button) LayoutInflater.from(context).inflate(R.layout.suw_button, (ViewGroup) null, false);
    }
}
