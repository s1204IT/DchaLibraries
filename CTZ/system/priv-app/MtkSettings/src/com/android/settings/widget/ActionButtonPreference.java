package com.android.settings.widget;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;

/* loaded from: classes.dex */
public class ActionButtonPreference extends Preference {
    private final ButtonInfo mButton1Info;
    private final ButtonInfo mButton2Info;

    public ActionButtonPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mButton1Info = new ButtonInfo();
        this.mButton2Info = new ButtonInfo();
        init();
    }

    public ActionButtonPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mButton1Info = new ButtonInfo();
        this.mButton2Info = new ButtonInfo();
        init();
    }

    public ActionButtonPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mButton1Info = new ButtonInfo();
        this.mButton2Info = new ButtonInfo();
        init();
    }

    public ActionButtonPreference(Context context) {
        super(context);
        this.mButton1Info = new ButtonInfo();
        this.mButton2Info = new ButtonInfo();
        init();
    }

    private void init() {
        setLayoutResource(R.layout.two_action_buttons);
        setSelectable(false);
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.setDividerAllowedAbove(false);
        preferenceViewHolder.setDividerAllowedBelow(false);
        this.mButton1Info.mPositiveButton = (Button) preferenceViewHolder.findViewById(R.id.button1_positive);
        this.mButton1Info.mNegativeButton = (Button) preferenceViewHolder.findViewById(R.id.button1_negative);
        this.mButton2Info.mPositiveButton = (Button) preferenceViewHolder.findViewById(R.id.button2_positive);
        this.mButton2Info.mNegativeButton = (Button) preferenceViewHolder.findViewById(R.id.button2_negative);
        this.mButton1Info.setUpButton();
        this.mButton2Info.setUpButton();
    }

    public ActionButtonPreference setButton1Text(int i) {
        String string = getContext().getString(i);
        if (!TextUtils.equals(string, this.mButton1Info.mText)) {
            this.mButton1Info.mText = string;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton1Enabled(boolean z) {
        if (z != this.mButton1Info.mIsEnabled) {
            this.mButton1Info.mIsEnabled = z;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Text(int i) {
        String string = getContext().getString(i);
        if (!TextUtils.equals(string, this.mButton2Info.mText)) {
            this.mButton2Info.mText = string;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Enabled(boolean z) {
        if (z != this.mButton2Info.mIsEnabled) {
            this.mButton2Info.mIsEnabled = z;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton1OnClickListener(View.OnClickListener onClickListener) {
        if (onClickListener != this.mButton1Info.mListener) {
            this.mButton1Info.mListener = onClickListener;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2OnClickListener(View.OnClickListener onClickListener) {
        if (onClickListener != this.mButton2Info.mListener) {
            this.mButton2Info.mListener = onClickListener;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton1Positive(boolean z) {
        if (z != this.mButton1Info.mIsPositive) {
            this.mButton1Info.mIsPositive = z;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Positive(boolean z) {
        if (z != this.mButton2Info.mIsPositive) {
            this.mButton2Info.mIsPositive = z;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton1Visible(boolean z) {
        if (z != this.mButton1Info.mIsVisible) {
            this.mButton1Info.mIsVisible = z;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Visible(boolean z) {
        if (z != this.mButton2Info.mIsVisible) {
            this.mButton2Info.mIsVisible = z;
            notifyChanged();
        }
        return this;
    }

    static class ButtonInfo {
        private View.OnClickListener mListener;
        private Button mNegativeButton;
        private Button mPositiveButton;
        private CharSequence mText;
        private boolean mIsPositive = true;
        private boolean mIsEnabled = true;
        private boolean mIsVisible = true;

        ButtonInfo() {
        }

        void setUpButton() {
            setUpButton(this.mPositiveButton);
            setUpButton(this.mNegativeButton);
            if (!this.mIsVisible) {
                this.mPositiveButton.setVisibility(4);
                this.mNegativeButton.setVisibility(4);
            } else if (this.mIsPositive) {
                this.mPositiveButton.setVisibility(0);
                this.mNegativeButton.setVisibility(4);
            } else {
                this.mPositiveButton.setVisibility(4);
                this.mNegativeButton.setVisibility(0);
            }
        }

        private void setUpButton(Button button) {
            button.setText(this.mText);
            button.setOnClickListener(this.mListener);
            button.setEnabled(this.mIsEnabled);
        }
    }
}
