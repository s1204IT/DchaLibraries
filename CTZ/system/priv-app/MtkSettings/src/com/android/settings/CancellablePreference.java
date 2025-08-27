package com.android.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/* loaded from: classes.dex */
public class CancellablePreference extends Preference implements View.OnClickListener {
    private boolean mCancellable;
    private OnCancelListener mListener;

    public interface OnCancelListener {
        void onCancel(CancellablePreference cancellablePreference);
    }

    public CancellablePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setWidgetLayoutResource(R.layout.cancel_pref_widget);
    }

    public void setCancellable(boolean z) {
        this.mCancellable = z;
        notifyChanged();
    }

    public void setOnCancelListener(OnCancelListener onCancelListener) {
        this.mListener = onCancelListener;
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        ImageView imageView = (ImageView) preferenceViewHolder.findViewById(R.id.cancel);
        imageView.setVisibility(this.mCancellable ? 0 : 4);
        imageView.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (this.mListener != null) {
            this.mListener.onCancel(this);
        }
    }
}
