package com.android.settings.print;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PrintJobSettingsFragment extends SettingsPreferenceFragment {
    private Drawable mListDivider;
    private Preference mMessagePreference;
    private PrintJob mPrintJob;
    private PrintJobId mPrintJobId;
    private Preference mPrintJobPreference;
    private final PrintManager.PrintJobStateChangeListener mPrintJobStateChangeListener = new PrintManager.PrintJobStateChangeListener() {
        public void onPrintJobStateChanged(PrintJobId printJobId) {
            PrintJobSettingsFragment.this.updateUi();
        }
    };
    private PrintManager mPrintManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.print_job_settings);
        this.mPrintJobPreference = findPreference("print_job_preference");
        this.mMessagePreference = findPreference("print_job_message_preference");
        this.mPrintManager = ((PrintManager) getActivity().getSystemService("print")).getGlobalPrintManagerForUser(getActivity().getUserId());
        getActivity().getActionBar().setTitle(R.string.print_print_job);
        processArguments();
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mPrintManager.addPrintJobStateChangeListener(this.mPrintJobStateChangeListener);
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mPrintManager.removePrintJobStateChangeListener(this.mPrintJobStateChangeListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        PrintJob printJob = getPrintJob();
        if (printJob != null) {
            if (!printJob.getInfo().isCancelling()) {
                MenuItem cancel = menu.add(0, 1, 0, getString(R.string.print_cancel));
                cancel.setShowAsAction(1);
            }
            if (printJob.isFailed()) {
                MenuItem restart = menu.add(0, 2, 0, getString(R.string.print_restart));
                restart.setShowAsAction(1);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                getPrintJob().cancel();
                finish();
                return true;
            case 2:
                getPrintJob().restart();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void processArguments() {
        String printJobId = getArguments().getString("EXTRA_PRINT_JOB_ID");
        this.mPrintJobId = PrintJobId.unflattenFromString(printJobId);
        if (this.mPrintJobId == null) {
            finish();
        }
    }

    private PrintJob getPrintJob() {
        if (this.mPrintJob == null) {
            this.mPrintJob = this.mPrintManager.getPrintJob(this.mPrintJobId);
        }
        return this.mPrintJob;
    }

    public void updateUi() {
        PrintJob printJob = getPrintJob();
        if (printJob == null) {
            finish();
            return;
        }
        if (printJob.isCancelled() || printJob.isCompleted()) {
            finish();
            return;
        }
        PrintJobInfo info = printJob.getInfo();
        switch (info.getState()) {
            case 2:
            case 3:
                if (!printJob.getInfo().isCancelling()) {
                    this.mPrintJobPreference.setTitle(getString(R.string.print_printing_state_title_template, new Object[]{info.getLabel()}));
                } else {
                    this.mPrintJobPreference.setTitle(getString(R.string.print_cancelling_state_title_template, new Object[]{info.getLabel()}));
                }
                break;
            case 4:
                if (!printJob.getInfo().isCancelling()) {
                    this.mPrintJobPreference.setTitle(getString(R.string.print_blocked_state_title_template, new Object[]{info.getLabel()}));
                } else {
                    this.mPrintJobPreference.setTitle(getString(R.string.print_cancelling_state_title_template, new Object[]{info.getLabel()}));
                }
                break;
            case 6:
                this.mPrintJobPreference.setTitle(getString(R.string.print_failed_state_title_template, new Object[]{info.getLabel()}));
                break;
        }
        this.mPrintJobPreference.setSummary(getString(R.string.print_job_summary, new Object[]{info.getPrinterName(), DateUtils.formatSameDayTime(info.getCreationTime(), info.getCreationTime(), 3, 3)}));
        switch (info.getState()) {
            case 2:
            case 3:
                this.mPrintJobPreference.setIcon(R.drawable.ic_print);
                break;
            case 4:
            case 6:
                this.mPrintJobPreference.setIcon(R.drawable.ic_print_error);
                break;
        }
        String stateReason = info.getStateReason();
        if (!TextUtils.isEmpty(stateReason)) {
            if (getPreferenceScreen().findPreference("print_job_message_preference") == null) {
                getPreferenceScreen().addPreference(this.mMessagePreference);
            }
            this.mMessagePreference.setSummary(stateReason);
            getListView().setDivider(null);
        } else {
            getPreferenceScreen().removePreference(this.mMessagePreference);
            getListView().setDivider(this.mListDivider);
        }
        getActivity().invalidateOptionsMenu();
    }
}
