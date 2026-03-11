package com.android.settings.tts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.speech.tts.TextToSpeech;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import com.android.settings.R;
import com.android.settings.SettingsActivity;

public class TtsEnginePreference extends Preference {
    private final TextToSpeech.EngineInfo mEngineInfo;
    private volatile boolean mPreventRadioButtonCallbacks;
    private RadioButton mRadioButton;
    private final CompoundButton.OnCheckedChangeListener mRadioChangeListener;
    private final RadioButtonGroupState mSharedState;

    public interface RadioButtonGroupState {
        Checkable getCurrentChecked();

        String getCurrentKey();

        void setCurrentChecked(Checkable checkable);

        void setCurrentKey(String str);
    }

    public TtsEnginePreference(Context context, TextToSpeech.EngineInfo engineInfo, RadioButtonGroupState radioButtonGroupState, SettingsActivity settingsActivity) {
        super(context);
        this.mRadioChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                TtsEnginePreference.this.onRadioButtonClicked(compoundButton, z);
            }
        };
        setLayoutResource(R.layout.preference_tts_engine);
        this.mSharedState = radioButtonGroupState;
        this.mEngineInfo = engineInfo;
        this.mPreventRadioButtonCallbacks = false;
        setKey(this.mEngineInfo.name);
        setTitle(this.mEngineInfo.label);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (this.mSharedState == null) {
            throw new IllegalStateException("Call to getView() before a call tosetSharedState()");
        }
        RadioButton radioButton = (RadioButton) preferenceViewHolder.findViewById(R.id.tts_engine_radiobutton);
        radioButton.setOnCheckedChangeListener(this.mRadioChangeListener);
        radioButton.setText(this.mEngineInfo.label);
        boolean zEquals = getKey().equals(this.mSharedState.getCurrentKey());
        if (zEquals) {
            this.mSharedState.setCurrentChecked(radioButton);
        }
        this.mPreventRadioButtonCallbacks = true;
        radioButton.setChecked(zEquals);
        this.mPreventRadioButtonCallbacks = false;
        this.mRadioButton = radioButton;
    }

    private boolean shouldDisplayDataAlert() {
        return !this.mEngineInfo.system;
    }

    private void displayDataAlert(DialogInterface.OnClickListener onClickListener, DialogInterface.OnClickListener onClickListener2) {
        Log.i("TtsEnginePreference", "Displaying data alert for :" + this.mEngineInfo.name);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(android.R.string.dialog_alert_title).setMessage(getContext().getString(R.string.tts_engine_security_warning, this.mEngineInfo.label)).setCancelable(true).setPositiveButton(android.R.string.ok, onClickListener).setNegativeButton(android.R.string.cancel, onClickListener2);
        builder.create().show();
    }

    public void onRadioButtonClicked(final CompoundButton compoundButton, boolean z) {
        if (!this.mPreventRadioButtonCallbacks && this.mSharedState.getCurrentChecked() != compoundButton && z) {
            if (shouldDisplayDataAlert()) {
                displayDataAlert(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        TtsEnginePreference.this.makeCurrentEngine(compoundButton);
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        compoundButton.setChecked(false);
                    }
                });
            } else {
                makeCurrentEngine(compoundButton);
            }
        }
    }

    public void makeCurrentEngine(Checkable checkable) {
        if (this.mSharedState.getCurrentChecked() != null) {
            this.mSharedState.getCurrentChecked().setChecked(false);
        }
        this.mSharedState.setCurrentChecked(checkable);
        this.mSharedState.setCurrentKey(getKey());
        callChangeListener(this.mSharedState.getCurrentKey());
    }
}
