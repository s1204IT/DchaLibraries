package com.android.settings.backup;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

/* loaded from: classes.dex */
public class ToggleBackupSettingFragment extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private IBackupManager mBackupManager;
    private Dialog mConfirmDialog;
    private Preference mSummaryPreference;
    protected SwitchBar mSwitchBar;
    protected ToggleSwitch mToggleSwitch;
    private boolean mWaitingForConfirmationDialog = false;

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(preferenceScreenCreatePreferenceScreen);
        this.mSummaryPreference = new Preference(getPrefContext()) { // from class: com.android.settings.backup.ToggleBackupSettingFragment.1
            @Override // android.support.v7.preference.Preference
            public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
                super.onBindViewHolder(preferenceViewHolder);
                ((TextView) preferenceViewHolder.findViewById(R.id.summary)).setText(getSummary());
            }
        };
        this.mSummaryPreference.setPersistent(false);
        this.mSummaryPreference.setLayoutResource(com.android.settings.R.layout.text_description_preference);
        preferenceScreenCreatePreferenceScreen.addPreference(this.mSummaryPreference);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        boolean zIsBackupEnabled;
        super.onViewCreated(view, bundle);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        if (Settings.Secure.getInt(getContentResolver(), "user_full_data_backup_aware", 0) != 0) {
            this.mSummaryPreference.setSummary(com.android.settings.R.string.fullbackup_data_summary);
        } else {
            this.mSummaryPreference.setSummary(com.android.settings.R.string.backup_data_summary);
        }
        try {
            if (this.mBackupManager != null) {
                zIsBackupEnabled = this.mBackupManager.isBackupEnabled();
            } else {
                zIsBackupEnabled = false;
            }
            this.mSwitchBar.setCheckedInternal(zIsBackupEnabled);
        } catch (RemoteException e) {
            this.mSwitchBar.setEnabled(false);
        }
        getActivity().setTitle(com.android.settings.R.string.backup_data_title);
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        this.mSwitchBar.hide();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() { // from class: com.android.settings.backup.ToggleBackupSettingFragment.2
            @Override // com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean z) throws Resources.NotFoundException {
                if (!z) {
                    ToggleBackupSettingFragment.this.showEraseBackupDialog();
                    return true;
                }
                ToggleBackupSettingFragment.this.setBackupEnabled(true);
                ToggleBackupSettingFragment.this.mSwitchBar.setCheckedInternal(true);
                return true;
            }
        });
        this.mSwitchBar.show();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStop() {
        if (this.mConfirmDialog != null && this.mConfirmDialog.isShowing()) {
            this.mConfirmDialog.dismiss();
        }
        this.mConfirmDialog = null;
        super.onStop();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            this.mWaitingForConfirmationDialog = false;
            setBackupEnabled(false);
            this.mSwitchBar.setCheckedInternal(false);
        } else if (i == -2) {
            this.mWaitingForConfirmationDialog = false;
            setBackupEnabled(true);
            this.mSwitchBar.setCheckedInternal(true);
        }
    }

    @Override // android.content.DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialogInterface) {
        if (this.mWaitingForConfirmationDialog) {
            setBackupEnabled(true);
            this.mSwitchBar.setCheckedInternal(true);
        }
    }

    private void showEraseBackupDialog() throws Resources.NotFoundException {
        CharSequence text;
        if (Settings.Secure.getInt(getContentResolver(), "user_full_data_backup_aware", 0) != 0) {
            text = getResources().getText(com.android.settings.R.string.fullbackup_erase_dialog_message);
        } else {
            text = getResources().getText(com.android.settings.R.string.backup_erase_dialog_message);
        }
        this.mWaitingForConfirmationDialog = true;
        this.mConfirmDialog = new AlertDialog.Builder(getActivity()).setMessage(text).setTitle(com.android.settings.R.string.backup_erase_dialog_title).setPositiveButton(R.string.ok, this).setNegativeButton(R.string.cancel, this).setOnDismissListener(this).show();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 81;
    }

    private void setBackupEnabled(boolean z) {
        if (this.mBackupManager != null) {
            try {
                this.mBackupManager.setBackupEnabled(z);
            } catch (RemoteException e) {
                Log.e("ToggleBackupSettingFragment", "Error communicating with BackupManager", e);
            }
        }
    }
}
