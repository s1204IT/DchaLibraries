package com.android.phone.settings;

import android.content.Context;
import android.preference.Preference;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.phone.R;

public class TextViewPreference extends Preference {
    private CharSequence mText;
    private int mTextResourceId;
    private TextView mTextView;

    public TextViewPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTextResourceId = 0;
        setLayoutResource(R.layout.text_view_preference);
    }

    public TextViewPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TextViewPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.preferenceStyle, 0);
    }

    public TextViewPreference(Context context) {
        super(context, null);
        this.mTextResourceId = 0;
        setLayoutResource(R.layout.text_view_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.mTextView = (TextView) view.findViewById(R.id.text);
        if (this.mTextResourceId != 0) {
            setTitle(this.mTextResourceId);
        } else if (this.mText != null) {
            setTitle(this.mText);
        } else if (getTitleRes() != 0) {
            setTitle(getTitleRes());
        }
    }

    @Override
    public void setTitle(CharSequence charSequence) {
        this.mTextResourceId = 0;
        this.mText = charSequence;
        if (this.mTextView == null) {
            return;
        }
        this.mTextView.setMovementMethod(LinkMovementMethod.getInstance());
        this.mTextView.setText(charSequence);
    }

    @Override
    public void setTitle(int i) {
        this.mTextResourceId = i;
        setTitle(Html.fromHtml(getContext().getString(i), 63));
    }
}
