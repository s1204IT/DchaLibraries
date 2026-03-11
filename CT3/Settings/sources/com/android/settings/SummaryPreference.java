package com.android.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settings.applications.LinearColorBar;

public class SummaryPreference extends Preference {
    private String mAmount;
    private String mEndLabel;
    private int mLeft;
    private float mLeftRatio;
    private int mMiddle;
    private float mMiddleRatio;
    private int mRight;
    private float mRightRatio;
    private String mStartLabel;
    private String mUnits;

    public SummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_summary_preference);
        this.mLeft = context.getColor(R.color.summary_default_start);
        this.mRight = context.getColor(R.color.summary_default_end);
    }

    public void setAmount(String amount) {
        this.mAmount = amount;
        if (this.mAmount == null || this.mUnits == null) {
            return;
        }
        setTitle(TextUtils.expandTemplate(getContext().getText(R.string.storage_size_large), this.mAmount, this.mUnits));
    }

    public void setUnits(String units) {
        this.mUnits = units;
        if (this.mAmount == null || this.mUnits == null) {
            return;
        }
        setTitle(TextUtils.expandTemplate(getContext().getText(R.string.storage_size_large), this.mAmount, this.mUnits));
    }

    public void setLabels(String start, String end) {
        this.mStartLabel = start;
        this.mEndLabel = end;
        notifyChanged();
    }

    public void setRatios(float left, float middle, float right) {
        this.mLeftRatio = left;
        this.mMiddleRatio = middle;
        this.mRightRatio = right;
        notifyChanged();
    }

    public void setColors(int left, int middle, int right) {
        this.mLeft = left;
        this.mMiddle = middle;
        this.mRight = right;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        LinearColorBar colorBar = (LinearColorBar) holder.itemView.findViewById(R.id.color_bar);
        colorBar.setRatios(this.mLeftRatio, this.mMiddleRatio, this.mRightRatio);
        colorBar.setColors(this.mLeft, this.mMiddle, this.mRight);
        if (!TextUtils.isEmpty(this.mStartLabel) || !TextUtils.isEmpty(this.mEndLabel)) {
            holder.findViewById(R.id.label_bar).setVisibility(0);
            ((TextView) holder.findViewById(android.R.id.text1)).setText(this.mStartLabel);
            ((TextView) holder.findViewById(android.R.id.text2)).setText(this.mEndLabel);
            return;
        }
        holder.findViewById(R.id.label_bar).setVisibility(8);
    }
}
