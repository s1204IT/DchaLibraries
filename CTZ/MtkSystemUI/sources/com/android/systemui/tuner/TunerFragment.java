package com.android.systemui.tuner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.plugins.PluginPrefs;

public class TunerFragment extends PreferenceFragment {
    private static final CharSequence KEY_DOZE = "doze";
    private static final String[] DEBUG_ONLY = {"nav_bar", "lockscreen", "picture_in_picture"};

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        addPreferencesFromResource(R.xml.tuner_prefs);
        if (!PluginPrefs.hasPlugins(getContext())) {
            getPreferenceScreen().removePreference(findPreference("plugins"));
        }
        if (!alwaysOnAvailable()) {
            getPreferenceScreen().removePreference(findPreference(KEY_DOZE));
        }
        if (!Build.IS_DEBUGGABLE) {
            for (int i = 0; i < DEBUG_ONLY.length; i++) {
                Preference preferenceFindPreference = findPreference(DEBUG_ONLY[i]);
                if (preferenceFindPreference != null) {
                    getPreferenceScreen().removePreference(preferenceFindPreference);
                }
            }
        }
        if (Settings.Secure.getInt(getContext().getContentResolver(), "seen_tuner_warning", 0) == 0 && getFragmentManager().findFragmentByTag("tuner_warning") == null) {
            new TunerWarningFragment().show(getFragmentManager(), "tuner_warning");
        }
    }

    private boolean alwaysOnAvailable() {
        return new AmbientDisplayConfiguration(getContext()).alwaysOnAvailable();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.system_ui_tuner);
        MetricsLogger.visibility(getContext(), 227, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        MetricsLogger.visibility(getContext(), 227, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 2, 0, R.string.remove_from_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 2) {
            TunerService.showResetRequest(getContext(), new Runnable() {
                @Override
                public void run() {
                    if (TunerFragment.this.getActivity() != null) {
                        TunerFragment.this.getActivity().finish();
                    }
                }
            });
            return true;
        }
        if (itemId == 16908332) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class TunerWarningFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getContext()).setTitle(R.string.tuner_warning_title).setMessage(R.string.tuner_warning).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Settings.Secure.putInt(TunerWarningFragment.this.getContext().getContentResolver(), "seen_tuner_warning", 1);
                }
            }).show();
        }
    }
}
