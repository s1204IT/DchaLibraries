package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import jp.co.omronsoft.iwnnime.ml.R;

public class FlickSensitivityPreference extends SeekBarPreference {
    private int mFlickSensitivityDefault;
    private int mFlickSensitivityMax;
    private int mFlickSensitivityMin;
    private SeekBar mSeekBar;
    private SharedPreferences mSharedPreferences;

    public FlickSensitivityPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        setDialogTitle(R.string.ti_dialog_flick_sensitivity_title_txt);
        setPositiveButtonText(R.string.ti_dialog_button_ok_txt);
        setNegativeButtonText(R.string.ti_dialog_button_cancel_txt);
        this.mFlickSensitivityMin = context.getResources().getInteger(R.integer.flick_sensitivity_preference_min);
        this.mFlickSensitivityMax = context.getResources().getInteger(R.integer.flick_sensitivity_preference_max);
        this.mFlickSensitivityDefault = context.getResources().getInteger(R.integer.flick_sensitivity_preference_default);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        this.mSeekBar = getSeekBar(view);
        this.mSeekBar.setMax(this.mFlickSensitivityMax - this.mFlickSensitivityMin);
        int setting = Integer.parseInt(this.mSharedPreferences.getString(getKey(), String.valueOf(this.mFlickSensitivityDefault)));
        int sense = Math.abs(setting - this.mFlickSensitivityMax);
        this.mSeekBar.setProgress(sense);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int sense = Math.abs((this.mSeekBar.getProgress() - this.mFlickSensitivityMax) + this.mFlickSensitivityMin);
            int setting = sense + this.mFlickSensitivityMin;
            persistString(String.valueOf(setting));
        }
        super.onDialogClosed(positiveResult);
    }
}
