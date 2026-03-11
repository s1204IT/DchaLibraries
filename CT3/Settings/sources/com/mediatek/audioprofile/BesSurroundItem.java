package com.mediatek.audioprofile;

import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settings.R;

public class BesSurroundItem extends CheckBoxPreference {
    private OnClickListener mListener;

    public interface OnClickListener {
        void onRadioButtonClicked(BesSurroundItem besSurroundItem);
    }

    public BesSurroundItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mListener = null;
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public BesSurroundItem(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.checkBoxPreferenceStyle);
    }

    public BesSurroundItem(Context context) {
        this(context, null);
    }

    void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onClick() {
        if (this.mListener == null) {
            return;
        }
        this.mListener.onRadioButtonClicked(this);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (title == null) {
            return;
        }
        title.setSingleLine(false);
        title.setMaxLines(3);
    }
}
