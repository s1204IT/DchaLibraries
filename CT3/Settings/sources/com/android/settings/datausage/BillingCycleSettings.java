package com.android.settings.datausage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import com.android.settings.R;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

public class BillingCycleSettings extends DataUsageBase implements Preference.OnPreferenceChangeListener {
    private Preference mBillingCycle;
    private Preference mDataLimit;
    private DataUsageController mDataUsageController;
    private Preference mDataWarning;
    private SwitchPreference mEnableDataLimit;
    private NetworkTemplate mNetworkTemplate;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mDataUsageController = new DataUsageController(getContext());
        Bundle args = getArguments();
        this.mNetworkTemplate = args.getParcelable("network_template");
        addPreferencesFromResource(R.xml.billing_cycle);
        this.mBillingCycle = findPreference("billing_cycle");
        this.mDataWarning = findPreference("data_warning");
        this.mEnableDataLimit = (SwitchPreference) findPreference("set_data_limit");
        this.mEnableDataLimit.setOnPreferenceChangeListener(this);
        this.mDataLimit = findPreference("data_limit");
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePrefs();
    }

    public void updatePrefs() {
        NetworkPolicy policy = this.services.mPolicyEditor.getPolicy(this.mNetworkTemplate);
        Preference preference = this.mBillingCycle;
        Object[] objArr = new Object[1];
        objArr[0] = Integer.valueOf(policy != null ? policy.cycleDay : 1);
        preference.setSummary(getString(R.string.billing_cycle_summary, objArr));
        this.mDataWarning.setSummary(Formatter.formatFileSize(getContext(), policy != null ? policy.warningBytes : 2147483648L));
        if (policy != null && policy.limitBytes != -1) {
            this.mDataLimit.setSummary(Formatter.formatFileSize(getContext(), policy.limitBytes));
            this.mDataLimit.setEnabled(true);
            this.mEnableDataLimit.setChecked(true);
        } else {
            this.mDataLimit.setSummary((CharSequence) null);
            this.mDataLimit.setEnabled(false);
            this.mEnableDataLimit.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == this.mBillingCycle) {
            CycleEditorFragment.show(this);
            return true;
        }
        if (preference == this.mDataWarning) {
            BytesEditorFragment.show(this, false);
            return true;
        }
        if (preference == this.mDataLimit) {
            BytesEditorFragment.show(this, true);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (this.mEnableDataLimit == preference) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            if (enabled) {
                ConfirmLimitFragment.show(this);
                return true;
            }
            setPolicyLimitBytes(-1L);
            return true;
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return 342;
    }

    public void setPolicyLimitBytes(long limitBytes) {
        Log.d("BillingCycleSettings", "setPolicyLimitBytes()");
        this.services.mPolicyEditor.setPolicyLimitBytes(this.mNetworkTemplate, limitBytes);
        updatePrefs();
    }

    public static class BytesEditorFragment extends DialogFragment implements DialogInterface.OnClickListener {
        private View mView;

        public static void show(BillingCycleSettings parent, boolean isLimit) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mNetworkTemplate);
                args.putBoolean("limit", isLimit);
                BytesEditorFragment dialog = new BytesEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "warningEditor");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            LayoutInflater dialogInflater = LayoutInflater.from(context);
            boolean isLimit = getArguments().getBoolean("limit");
            this.mView = dialogInflater.inflate(R.layout.data_usage_bytes_editor, (ViewGroup) null, false);
            setupPicker((EditText) this.mView.findViewById(R.id.bytes), (Spinner) this.mView.findViewById(R.id.size_spinner));
            return new AlertDialog.Builder(context).setTitle(isLimit ? R.string.data_usage_limit_editor_title : R.string.data_usage_warning_editor_title).setView(this.mView).setPositiveButton(R.string.data_usage_cycle_editor_positive, this).create();
        }

        private void setupPicker(EditText bytesPicker, Spinner type) {
            BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            NetworkPolicyEditor editor = target.services.mPolicyEditor;
            NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            boolean isLimit = getArguments().getBoolean("limit");
            long bytes = isLimit ? editor.getPolicyLimitBytes(template) : editor.getPolicyWarningBytes(template);
            if (isLimit) {
            }
            if (bytes > 1.6106127E9f) {
                bytesPicker.setText(formatText(bytes / 1.0737418E9f));
                type.setSelection(1);
            } else {
                bytesPicker.setText(formatText(bytes / 1048576.0f));
                type.setSelection(0);
            }
        }

        private String formatText(float v) {
            return String.valueOf(Math.round(v * 100.0f) / 100.0f);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != -1) {
                return;
            }
            BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            NetworkPolicyEditor editor = target.services.mPolicyEditor;
            NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            boolean isLimit = getArguments().getBoolean("limit");
            EditText bytesField = (EditText) this.mView.findViewById(R.id.bytes);
            Spinner spinner = (Spinner) this.mView.findViewById(R.id.size_spinner);
            String bytesString = bytesField.getText().toString();
            if (bytesString.isEmpty()) {
                bytesString = "0";
            }
            long bytes = (long) (Float.valueOf(bytesString).floatValue() * (spinner.getSelectedItemPosition() == 0 ? 1048576L : 1073741824L));
            Log.d("BillingCycleSettings", "onClick, isLimit = " + isLimit + " bytes = " + bytes);
            if (isLimit) {
                editor.setPolicyLimitBytes(template, bytes);
            } else {
                editor.setPolicyWarningBytes(template, bytes);
            }
            target.updatePrefs();
        }
    }

    public static class CycleEditorFragment extends DialogFragment implements DialogInterface.OnClickListener {
        private NumberPicker mCycleDayPicker;

        public static void show(BillingCycleSettings parent) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mNetworkTemplate);
                CycleEditorFragment dialog = new CycleEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "cycleEditor");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            NetworkPolicyEditor editor = target.services.mPolicyEditor;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
            View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, (ViewGroup) null, false);
            this.mCycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);
            NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            int cycleDay = editor.getPolicyCycleDay(template);
            this.mCycleDayPicker.setMinValue(1);
            this.mCycleDayPicker.setMaxValue(31);
            this.mCycleDayPicker.setValue(cycleDay);
            this.mCycleDayPicker.setWrapSelectorWheel(true);
            return builder.setTitle(R.string.data_usage_cycle_editor_title).setView(view).setPositiveButton(R.string.data_usage_cycle_editor_positive, this).create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            NetworkTemplate template = getArguments().getParcelable("template");
            BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            NetworkPolicyEditor editor = target.services.mPolicyEditor;
            this.mCycleDayPicker.clearFocus();
            int cycleDay = this.mCycleDayPicker.getValue();
            String cycleTimezone = new Time().timezone;
            Log.d("BillingCycleSettings", "onClick, cycleDay = " + cycleDay + ", cycleTimezone = " + cycleTimezone);
            editor.setPolicyCycleDay(template, cycleDay, cycleTimezone);
            target.updatePrefs();
        }
    }

    public static class ConfirmLimitFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public static void show(BillingCycleSettings parent) {
            NetworkPolicy policy;
            if (parent.isAdded() && (policy = parent.services.mPolicyEditor.getPolicy(parent.mNetworkTemplate)) != null) {
                Resources res = parent.getResources();
                long minLimitBytes = (long) (policy.warningBytes * 1.2f);
                CharSequence message = res.getString(R.string.data_usage_limit_dialog_mobile);
                long limitBytes = Math.max(5368709120L, minLimitBytes);
                Bundle args = new Bundle();
                args.putCharSequence("message", message);
                args.putLong("limitBytes", limitBytes);
                ConfirmLimitFragment dialog = new ConfirmLimitFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmLimit");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            CharSequence message = getArguments().getCharSequence("message");
            return new AlertDialog.Builder(context).setTitle(R.string.data_usage_limit_dialog_title).setMessage(message).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            if (which != -1) {
                if (target != null) {
                    target.updatePrefs();
                }
            } else {
                long limitBytes = getArguments().getLong("limitBytes");
                if (target == null) {
                    return;
                }
                Log.d("BillingCycleSettings", "onClick, limitBytes = " + limitBytes);
                target.setPolicyLimitBytes(limitBytes);
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            if (target == null) {
                return;
            }
            target.updatePrefs();
        }
    }
}
