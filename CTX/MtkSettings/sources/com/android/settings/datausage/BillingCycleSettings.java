package com.android.settings.datausage;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.format.Time;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

public class BillingCycleSettings extends DataUsageBase implements Preference.OnPreferenceChangeListener, DataUsageEditController {

    @VisibleForTesting
    static final String KEY_SET_DATA_LIMIT = "set_data_limit";
    private Preference mBillingCycle;
    private Preference mDataLimit;
    private DataUsageController mDataUsageController;
    private Preference mDataWarning;
    private SwitchPreference mEnableDataLimit;
    private SwitchPreference mEnableDataWarning;
    private NetworkTemplate mNetworkTemplate;

    @VisibleForTesting
    void setUpForTest(NetworkPolicyEditor networkPolicyEditor, Preference preference, Preference preference2, Preference preference3, SwitchPreference switchPreference, SwitchPreference switchPreference2) {
        this.services.mPolicyEditor = networkPolicyEditor;
        this.mBillingCycle = preference;
        this.mDataLimit = preference2;
        this.mDataWarning = preference3;
        this.mEnableDataLimit = switchPreference;
        this.mEnableDataWarning = switchPreference2;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mDataUsageController = new DataUsageController(getContext());
        this.mNetworkTemplate = getArguments().getParcelable("network_template");
        addPreferencesFromResource(R.xml.billing_cycle);
        this.mBillingCycle = findPreference("billing_cycle");
        this.mEnableDataWarning = (SwitchPreference) findPreference("set_data_warning");
        this.mEnableDataWarning.setOnPreferenceChangeListener(this);
        this.mDataWarning = findPreference("data_warning");
        this.mEnableDataLimit = (SwitchPreference) findPreference(KEY_SET_DATA_LIMIT);
        this.mEnableDataLimit.setOnPreferenceChangeListener(this);
        this.mDataLimit = findPreference("data_limit");
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.data_warning_footnote);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePrefs();
    }

    @VisibleForTesting
    void updatePrefs() {
        int policyCycleDay = this.services.mPolicyEditor.getPolicyCycleDay(this.mNetworkTemplate);
        if (!FeatureFlagUtils.isEnabled(getContext(), "settings_data_usage_v2") && policyCycleDay != -1) {
            this.mBillingCycle.setSummary(getString(R.string.billing_cycle_fragment_summary, new Object[]{Integer.valueOf(policyCycleDay)}));
        } else {
            this.mBillingCycle.setSummary((CharSequence) null);
        }
        long policyWarningBytes = this.services.mPolicyEditor.getPolicyWarningBytes(this.mNetworkTemplate);
        if (policyWarningBytes != -1) {
            this.mDataWarning.setSummary(DataUsageUtils.formatDataUsage(getContext(), policyWarningBytes));
            this.mDataWarning.setEnabled(true);
            this.mEnableDataWarning.setChecked(true);
        } else {
            this.mDataWarning.setSummary((CharSequence) null);
            this.mDataWarning.setEnabled(false);
            this.mEnableDataWarning.setChecked(false);
        }
        long policyLimitBytes = this.services.mPolicyEditor.getPolicyLimitBytes(this.mNetworkTemplate);
        if (policyLimitBytes != -1) {
            this.mDataLimit.setSummary(DataUsageUtils.formatDataUsage(getContext(), policyLimitBytes));
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
            BytesEditorFragment.show((DataUsageEditController) this, false);
            return true;
        }
        if (preference == this.mDataLimit) {
            BytesEditorFragment.show((DataUsageEditController) this, true);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mEnableDataLimit == preference) {
            Log.d("BillingCycleSettings", "onDataLimitPreferenceChange newValue: " + obj);
            if (!((Boolean) obj).booleanValue()) {
                setPolicyLimitBytes(-1L);
                return true;
            }
            ConfirmLimitFragment.show(this);
            return false;
        }
        if (this.mEnableDataWarning != preference) {
            return false;
        }
        if (((Boolean) obj).booleanValue()) {
            setPolicyWarningBytes(this.mDataUsageController.getDefaultWarningLevel());
        } else {
            setPolicyWarningBytes(-1L);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return 342;
    }

    @VisibleForTesting
    void setPolicyLimitBytes(long j) {
        this.services.mPolicyEditor.setPolicyLimitBytes(this.mNetworkTemplate, j);
        updatePrefs();
    }

    private void setPolicyWarningBytes(long j) {
        this.services.mPolicyEditor.setPolicyWarningBytes(this.mNetworkTemplate, j);
        updatePrefs();
    }

    @Override
    public NetworkPolicyEditor getNetworkPolicyEditor() {
        return this.services.mPolicyEditor;
    }

    @Override
    public NetworkTemplate getNetworkTemplate() {
        return this.mNetworkTemplate;
    }

    @Override
    public void updateDataUsage() {
        updatePrefs();
    }

    public static class BytesEditorFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        private View mView;

        public static void show(DataUsageEditController dataUsageEditController, boolean z) {
            if (!(dataUsageEditController instanceof Fragment)) {
                return;
            }
            Fragment fragment = (Fragment) dataUsageEditController;
            if (!fragment.isAdded()) {
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable("template", dataUsageEditController.getNetworkTemplate());
            bundle.putBoolean("limit", z);
            BytesEditorFragment bytesEditorFragment = new BytesEditorFragment();
            bytesEditorFragment.setArguments(bundle);
            bytesEditorFragment.setTargetFragment(fragment, 0);
            bytesEditorFragment.show(fragment.getFragmentManager(), "warningEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            LayoutInflater layoutInflaterFrom = LayoutInflater.from(activity);
            boolean z = getArguments().getBoolean("limit");
            this.mView = layoutInflaterFrom.inflate(R.layout.data_usage_bytes_editor, (ViewGroup) null, false);
            setupPicker((EditText) this.mView.findViewById(R.id.bytes), (Spinner) this.mView.findViewById(R.id.size_spinner));
            return new AlertDialog.Builder(activity).setTitle(z ? R.string.data_usage_limit_editor_title : R.string.data_usage_warning_editor_title).setView(this.mView).setPositiveButton(R.string.data_usage_cycle_editor_positive, this).create();
        }

        private void setupPicker(EditText editText, Spinner spinner) {
            NetworkPolicyEditor networkPolicyEditor = ((DataUsageEditController) getTargetFragment()).getNetworkPolicyEditor();
            NetworkTemplate networkTemplate = (NetworkTemplate) getArguments().getParcelable("template");
            float policyLimitBytes = getArguments().getBoolean("limit") ? networkPolicyEditor.getPolicyLimitBytes(networkTemplate) : networkPolicyEditor.getPolicyWarningBytes(networkTemplate);
            if (policyLimitBytes > 1.6106127E9f) {
                String text = formatText(policyLimitBytes / 1.0737418E9f);
                editText.setText(text);
                editText.setSelection(0, text.length());
                spinner.setSelection(1);
                return;
            }
            String text2 = formatText(policyLimitBytes / 1048576.0f);
            editText.setText(text2);
            editText.setSelection(0, text2.length());
            spinner.setSelection(0);
        }

        private String formatText(float f) {
            return String.valueOf(Math.round(f * 100.0f) / 100.0f);
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i != -1) {
                return;
            }
            DataUsageEditController dataUsageEditController = (DataUsageEditController) getTargetFragment();
            NetworkPolicyEditor networkPolicyEditor = dataUsageEditController.getNetworkPolicyEditor();
            NetworkTemplate networkTemplate = (NetworkTemplate) getArguments().getParcelable("template");
            boolean z = getArguments().getBoolean("limit");
            EditText editText = (EditText) this.mView.findViewById(R.id.bytes);
            Spinner spinner = (Spinner) this.mView.findViewById(R.id.size_spinner);
            String string = editText.getText().toString();
            if (string.isEmpty() || string.equals(".")) {
                string = "0";
            }
            long jMin = Math.min(53687091200000L, (long) (Float.valueOf(string).floatValue() * (spinner.getSelectedItemPosition() == 0 ? 1048576L : 1073741824L)));
            Log.d("BillingCycleSettings", "onClick, isLimit = " + z + " correctedBytes = " + jMin);
            if (z) {
                networkPolicyEditor.setPolicyLimitBytes(networkTemplate, jMin);
            } else {
                networkPolicyEditor.setPolicyWarningBytes(networkTemplate, jMin);
            }
            dataUsageEditController.updateDataUsage();
        }

        @Override
        public int getMetricsCategory() {
            return 550;
        }
    }

    public static class CycleEditorFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        private NumberPicker mCycleDayPicker;

        public static void show(BillingCycleSettings billingCycleSettings) {
            if (billingCycleSettings.isAdded()) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("template", billingCycleSettings.mNetworkTemplate);
                CycleEditorFragment cycleEditorFragment = new CycleEditorFragment();
                cycleEditorFragment.setArguments(bundle);
                cycleEditorFragment.setTargetFragment(billingCycleSettings, 0);
                cycleEditorFragment.show(billingCycleSettings.getFragmentManager(), "cycleEditor");
            }
        }

        @Override
        public int getMetricsCategory() {
            return 549;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            NetworkPolicyEditor networkPolicyEditor = ((DataUsageEditController) getTargetFragment()).getNetworkPolicyEditor();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            View viewInflate = LayoutInflater.from(builder.getContext()).inflate(R.layout.data_usage_cycle_editor, (ViewGroup) null, false);
            this.mCycleDayPicker = (NumberPicker) viewInflate.findViewById(R.id.cycle_day);
            int policyCycleDay = networkPolicyEditor.getPolicyCycleDay((NetworkTemplate) getArguments().getParcelable("template"));
            this.mCycleDayPicker.setMinValue(1);
            this.mCycleDayPicker.setMaxValue(31);
            this.mCycleDayPicker.setValue(policyCycleDay);
            this.mCycleDayPicker.setWrapSelectorWheel(true);
            return builder.setTitle(R.string.data_usage_cycle_editor_title).setView(viewInflate).setPositiveButton(R.string.data_usage_cycle_editor_positive, this).create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            NetworkTemplate parcelable = getArguments().getParcelable("template");
            DataUsageEditController dataUsageEditController = (DataUsageEditController) getTargetFragment();
            NetworkPolicyEditor networkPolicyEditor = dataUsageEditController.getNetworkPolicyEditor();
            this.mCycleDayPicker.clearFocus();
            int value = this.mCycleDayPicker.getValue();
            String str = new Time().timezone;
            Log.d("BillingCycleSettings", "onClick, cycleDay = " + value + ", cycleTimezone = " + str);
            networkPolicyEditor.setPolicyCycleDay(parcelable, value, str);
            dataUsageEditController.updateDataUsage();
        }
    }

    public static class ConfirmLimitFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {

        @VisibleForTesting
        static final String EXTRA_LIMIT_BYTES = "limitBytes";

        public static void show(BillingCycleSettings billingCycleSettings) {
            if (billingCycleSettings.isAdded()) {
                if (billingCycleSettings.services.mPolicyEditor.getPolicy(billingCycleSettings.mNetworkTemplate) == null) {
                    Log.d("BillingCycleSettings", "NetworkPolicy is null, Cannot ShowDialog");
                    return;
                }
                billingCycleSettings.getResources();
                long jMax = Math.max(5368709120L, (long) (r0.warningBytes * 1.2f));
                Bundle bundle = new Bundle();
                bundle.putLong(EXTRA_LIMIT_BYTES, jMax);
                ConfirmLimitFragment confirmLimitFragment = new ConfirmLimitFragment();
                confirmLimitFragment.setArguments(bundle);
                confirmLimitFragment.setTargetFragment(billingCycleSettings, 0);
                Log.d("BillingCycleSettings", "show ConfirmLimitFragment");
                confirmLimitFragment.show(billingCycleSettings.getFragmentManager(), "confirmLimit");
                return;
            }
            Log.d("BillingCycleSettings", "Parent not added, Cannot ShowDialog");
        }

        @Override
        public int getMetricsCategory() {
            return 551;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.data_usage_limit_dialog_title).setMessage(R.string.data_usage_limit_dialog_mobile).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            BillingCycleSettings billingCycleSettings = (BillingCycleSettings) getTargetFragment();
            if (i != -1) {
                if (billingCycleSettings != null) {
                    billingCycleSettings.updatePrefs();
                    return;
                }
                return;
            }
            long j = getArguments().getLong(EXTRA_LIMIT_BYTES);
            if (billingCycleSettings != null) {
                Log.d("BillingCycleSettings", "onClick, limitBytes = " + j);
                billingCycleSettings.setPolicyLimitBytes(j);
            }
            billingCycleSettings.getPreferenceManager().getSharedPreferences().edit().putBoolean(BillingCycleSettings.KEY_SET_DATA_LIMIT, true).apply();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            BillingCycleSettings billingCycleSettings = (BillingCycleSettings) getTargetFragment();
            if (billingCycleSettings != null) {
                billingCycleSettings.updatePrefs();
            }
        }
    }
}
