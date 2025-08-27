package com.android.settings.applications.defaultapps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;

/* loaded from: classes.dex */
public abstract class DefaultAppPickerFragment extends RadioButtonPickerFragment {
    protected PackageManagerWrapper mPm;

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mPm = new PackageManagerWrapper(context.getPackageManager());
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.widget.RadioButtonPreference.OnClickListener
    public void onRadioButtonClicked(RadioButtonPreference radioButtonPreference) {
        String key = radioButtonPreference.getKey();
        CharSequence confirmationMessage = getConfirmationMessage(getCandidate(key));
        Activity activity = getActivity();
        if (TextUtils.isEmpty(confirmationMessage)) {
            super.onRadioButtonClicked(radioButtonPreference);
        } else if (activity != null) {
            newConfirmationDialogFragment(key, confirmationMessage).show(activity.getFragmentManager(), "DefaultAppConfirm");
        }
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected void onRadioButtonConfirmed(String str) {
        this.mMetricsFeatureProvider.action(getContext(), 1000, str, Pair.create(833, Integer.valueOf(getMetricsCategory())));
        super.onRadioButtonConfirmed(str);
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    public void bindPreferenceExtra(RadioButtonPreference radioButtonPreference, String str, CandidateInfo candidateInfo, String str2, String str3) {
        if (!(candidateInfo instanceof DefaultAppInfo)) {
            return;
        }
        if (TextUtils.equals(str3, str)) {
            radioButtonPreference.setSummary(R.string.system_app);
            return;
        }
        DefaultAppInfo defaultAppInfo = (DefaultAppInfo) candidateInfo;
        if (!TextUtils.isEmpty(defaultAppInfo.summary)) {
            radioButtonPreference.setSummary(defaultAppInfo.summary);
        }
    }

    protected ConfirmationDialogFragment newConfirmationDialogFragment(String str, CharSequence charSequence) {
        ConfirmationDialogFragment confirmationDialogFragment = new ConfirmationDialogFragment();
        confirmationDialogFragment.init(this, str, charSequence);
        return confirmationDialogFragment;
    }

    protected CharSequence getConfirmationMessage(CandidateInfo candidateInfo) {
        return null;
    }

    public static class ConfirmationDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        private DialogInterface.OnClickListener mCancelListener;

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 791;
        }

        public void init(DefaultAppPickerFragment defaultAppPickerFragment, String str, CharSequence charSequence) {
            Bundle bundle = new Bundle();
            bundle.putString("extra_key", str);
            bundle.putCharSequence("extra_message", charSequence);
            setArguments(bundle);
            setTargetFragment(defaultAppPickerFragment, 0);
        }

        public void setCancelListener(DialogInterface.OnClickListener onClickListener) {
            this.mCancelListener = onClickListener;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setMessage(getArguments().getCharSequence("extra_message")).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this.mCancelListener).create();
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            Fragment targetFragment = getTargetFragment();
            if (targetFragment instanceof DefaultAppPickerFragment) {
                ((DefaultAppPickerFragment) targetFragment).onRadioButtonConfirmed(getArguments().getString("extra_key"));
            }
        }
    }
}
