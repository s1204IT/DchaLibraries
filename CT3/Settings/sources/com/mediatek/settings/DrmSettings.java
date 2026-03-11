package com.mediatek.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.drm.DrmManagerClient;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class DrmSettings extends SettingsPreferenceFragment {
    private static DrmManagerClient sClient;
    private static Preference sPreferenceReset;
    private Context mContext;

    @Override
    protected int getMetricsCategory() {
        return 81;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.drm_settings);
        sPreferenceReset = findPreference("drm_settings");
        this.mContext = getActivity();
        sClient = new DrmManagerClient(this.mContext);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        switch (id) {
            case 1000:
                builder.setMessage(getResources().getString(R.string.drm_reset_dialog_msg));
                builder.setTitle(getResources().getString(R.string.drm_settings_title));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (DrmSettings.sClient == null) {
                            return;
                        }
                        int result = DrmSettings.sClient.removeAllRights();
                        if (result == 0) {
                            Toast.makeText(DrmSettings.this.mContext, R.string.drm_reset_toast_msg, 0).show();
                            DrmSettings.sPreferenceReset.setEnabled(false);
                        } else {
                            Log.d("DrmSettings", "removeAllRights fail!");
                        }
                        DrmManagerClient unused = DrmSettings.sClient = null;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
                AlertDialog dialog = builder.create();
                return dialog;
            default:
                return null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == sPreferenceReset) {
            showDialog(1000);
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sClient = null;
    }
}
