package com.android.settings.wifi;

import android.R;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settings.LinkifyUtils;

/* loaded from: classes.dex */
public class LinkablePreference extends Preference {
    private LinkifyUtils.OnClickListener mClickListener;
    private CharSequence mContentDescription;
    private CharSequence mContentTitle;

    public LinkablePreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setSelectable(false);
    }

    public LinkablePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setSelectable(false);
    }

    public LinkablePreference(Context context) {
        super(context);
        setSelectable(false);
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.title);
        if (textView == null) {
            return;
        }
        textView.setSingleLine(false);
        if (this.mContentTitle == null || this.mClickListener == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.mContentTitle);
        if (this.mContentDescription != null) {
            sb.append("\n\n");
            sb.append(this.mContentDescription);
        }
        if (LinkifyUtils.linkify(textView, sb, this.mClickListener) && this.mContentTitle != null) {
            Spannable spannable = (Spannable) textView.getText();
            spannable.setSpan(new TextAppearanceSpan(getContext(), R.style.TextAppearance.Medium), 0, this.mContentTitle.length(), 17);
            textView.setText(spannable);
            textView.setMovementMethod(new LinkMovementMethod());
        }
    }

    public void setText(CharSequence charSequence, CharSequence charSequence2, LinkifyUtils.OnClickListener onClickListener) {
        this.mContentTitle = charSequence;
        this.mContentDescription = charSequence2;
        this.mClickListener = onClickListener;
        super.setTitle(charSequence);
    }

    @Override // android.support.v7.preference.Preference
    public void setTitle(int i) {
        this.mContentTitle = null;
        this.mContentDescription = null;
        super.setTitle(i);
    }

    @Override // android.support.v7.preference.Preference
    public void setTitle(CharSequence charSequence) {
        this.mContentTitle = null;
        this.mContentDescription = null;
        super.setTitle(charSequence);
    }
}
