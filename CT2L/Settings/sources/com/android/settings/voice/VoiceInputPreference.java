package com.android.settings.voice;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import com.android.settings.R;
import com.android.settings.voice.VoiceInputHelper;

public final class VoiceInputPreference extends Preference {
    private final CharSequence mAlertText;
    private final CharSequence mAppLabel;
    private final CharSequence mLabel;
    private volatile boolean mPreventRadioButtonCallbacks;
    private RadioButton mRadioButton;
    private final CompoundButton.OnCheckedChangeListener mRadioChangeListener;
    private final ComponentName mSettingsComponent;
    private View mSettingsIcon;
    private final RadioButtonGroupState mSharedState;

    public interface RadioButtonGroupState {
        Checkable getCurrentChecked();

        String getCurrentKey();

        void setCurrentChecked(Checkable checkable);

        void setCurrentKey(String str);
    }

    public VoiceInputPreference(Context context, VoiceInputHelper.BaseInfo info, CharSequence summary, CharSequence alertText, RadioButtonGroupState state) {
        super(context);
        this.mRadioChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                VoiceInputPreference.this.onRadioButtonClicked(buttonView, isChecked);
            }
        };
        setLayoutResource(R.layout.preference_tts_engine);
        this.mSharedState = state;
        this.mLabel = info.label;
        this.mAppLabel = info.appLabel;
        this.mAlertText = alertText;
        this.mSettingsComponent = info.settings;
        this.mPreventRadioButtonCallbacks = false;
        setKey(info.key);
        setTitle(info.label);
        setSummary(summary);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        if (this.mSharedState == null) {
            throw new IllegalStateException("Call to getView() before a call tosetSharedState()");
        }
        View view = super.getView(convertView, parent);
        final RadioButton rb = (RadioButton) view.findViewById(R.id.tts_engine_radiobutton);
        rb.setOnCheckedChangeListener(this.mRadioChangeListener);
        boolean isChecked = getKey().equals(this.mSharedState.getCurrentKey());
        if (isChecked) {
            this.mSharedState.setCurrentChecked(rb);
        }
        this.mPreventRadioButtonCallbacks = true;
        rb.setChecked(isChecked);
        this.mPreventRadioButtonCallbacks = false;
        this.mRadioButton = rb;
        View textLayout = view.findViewById(R.id.tts_engine_pref_text);
        textLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!rb.isChecked()) {
                    VoiceInputPreference.this.onRadioButtonClicked(rb, true);
                }
            }
        });
        this.mSettingsIcon = view.findViewById(R.id.tts_engine_settings);
        this.mSettingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(VoiceInputPreference.this.mSettingsComponent);
                VoiceInputPreference.this.getContext().startActivity(new Intent(intent));
            }
        });
        updateCheckedState(isChecked);
        return view;
    }

    private boolean shouldDisplayAlert() {
        return this.mAlertText != null;
    }

    private void displayAlert(DialogInterface.OnClickListener positiveOnClickListener, final DialogInterface.OnClickListener negativeOnClickListener) {
        Log.i("VoiceInputPreference", "Displaying data alert for :" + getKey());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String msg = String.format(getContext().getResources().getConfiguration().locale, this.mAlertText.toString(), this.mAppLabel);
        builder.setTitle(android.R.string.dialog_alert_title).setMessage(msg).setCancelable(true).setPositiveButton(android.R.string.ok, positiveOnClickListener).setNegativeButton(android.R.string.cancel, negativeOnClickListener).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                negativeOnClickListener.onClick(dialog, -2);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void doClick() {
        this.mRadioButton.performClick();
    }

    void updateCheckedState(boolean isChecked) {
        if (this.mSettingsComponent != null) {
            this.mSettingsIcon.setVisibility(0);
            if (isChecked) {
                this.mSettingsIcon.setEnabled(true);
                this.mSettingsIcon.setAlpha(1.0f);
                return;
            } else {
                this.mSettingsIcon.setEnabled(false);
                this.mSettingsIcon.setAlpha(0.4f);
                return;
            }
        }
        this.mSettingsIcon.setVisibility(8);
    }

    void onRadioButtonClicked(final CompoundButton buttonView, boolean isChecked) {
        if (!this.mPreventRadioButtonCallbacks) {
            if (this.mSharedState.getCurrentChecked() == buttonView) {
                updateCheckedState(isChecked);
                return;
            }
            if (isChecked) {
                if (shouldDisplayAlert()) {
                    displayAlert(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            VoiceInputPreference.this.makeCurrentChecked(buttonView);
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            buttonView.setChecked(false);
                        }
                    });
                    return;
                } else {
                    makeCurrentChecked(buttonView);
                    return;
                }
            }
            updateCheckedState(isChecked);
        }
    }

    void makeCurrentChecked(Checkable current) {
        if (this.mSharedState.getCurrentChecked() != null) {
            this.mSharedState.getCurrentChecked().setChecked(false);
        }
        this.mSharedState.setCurrentChecked(current);
        this.mSharedState.setCurrentKey(getKey());
        updateCheckedState(true);
        callChangeListener(this.mSharedState.getCurrentKey());
        current.setChecked(true);
    }
}
