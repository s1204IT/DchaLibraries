package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.voice.VoiceInputListPreference;
import com.mediatek.settings.inputmethod.InputMethodExts;

public class ManageAssist extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private SwitchPreference mContextPref;
    private DefaultAssistPreference mDefaultAssitPref;
    private Handler mHandler = new Handler();
    private SwitchPreference mScreenshotPref;
    private VoiceInputListPreference mVoiceInputPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.manage_assist);
        this.mDefaultAssitPref = (DefaultAssistPreference) findPreference("default_assist");
        this.mDefaultAssitPref.setOnPreferenceChangeListener(this);
        this.mContextPref = (SwitchPreference) findPreference("context");
        this.mContextPref.setChecked(Settings.Secure.getInt(getContentResolver(), "assist_structure_enabled", 1) != 0);
        this.mContextPref.setOnPreferenceChangeListener(this);
        this.mScreenshotPref = (SwitchPreference) findPreference("screenshot");
        this.mScreenshotPref.setOnPreferenceChangeListener(this);
        this.mVoiceInputPref = (VoiceInputListPreference) findPreference("voice_input_settings");
        updateUi();
    }

    @Override
    protected int getMetricsCategory() {
        return 201;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == this.mContextPref) {
            Settings.Secure.putInt(getContentResolver(), "assist_structure_enabled", ((Boolean) newValue).booleanValue() ? 1 : 0);
            postUpdateUi();
            return true;
        }
        if (preference == this.mScreenshotPref) {
            Settings.Secure.putInt(getContentResolver(), "assist_screenshot_enabled", ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        }
        if (preference != this.mDefaultAssitPref) {
            return false;
        }
        String newAssitPackage = (String) newValue;
        if (newAssitPackage == null || newAssitPackage.contentEquals("")) {
            setDefaultAssist("");
            return false;
        }
        String currentPackage = this.mDefaultAssitPref.getValue();
        if (currentPackage == null || !newAssitPackage.contentEquals(currentPackage)) {
            Log.d("ManageAssist", "newAssitPackage : " + newAssitPackage);
            if (!InputMethodExts.displayVoiceWakeupAlert(getContext(), newAssitPackage)) {
                confirmNewAssist(newAssitPackage);
            }
        }
        return false;
    }

    private void postUpdateUi() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ManageAssist.this.updateUi();
            }
        });
    }

    public void updateUi() {
        this.mDefaultAssitPref.refreshAssistApps();
        this.mVoiceInputPref.refreshVoiceInputs();
        ComponentName currentAssist = this.mDefaultAssitPref.getCurrentAssist();
        boolean hasAssistant = currentAssist != null;
        if (hasAssistant) {
            getPreferenceScreen().addPreference(this.mContextPref);
            getPreferenceScreen().addPreference(this.mScreenshotPref);
        } else {
            getPreferenceScreen().removePreference(this.mContextPref);
            getPreferenceScreen().removePreference(this.mScreenshotPref);
        }
        if (isCurrentAssistVoiceService()) {
            getPreferenceScreen().removePreference(this.mVoiceInputPref);
        } else {
            getPreferenceScreen().addPreference(this.mVoiceInputPref);
            this.mVoiceInputPref.setAssistRestrict(currentAssist);
        }
        this.mScreenshotPref.setEnabled(this.mContextPref.isChecked());
        this.mScreenshotPref.setChecked(this.mContextPref.isChecked() && Settings.Secure.getInt(getContentResolver(), "assist_screenshot_enabled", 1) != 0);
    }

    private boolean isCurrentAssistVoiceService() {
        ComponentName currentAssist = this.mDefaultAssitPref.getCurrentAssist();
        ComponentName activeService = this.mVoiceInputPref.getCurrentService();
        if (currentAssist == null && activeService == null) {
            return true;
        }
        if (currentAssist != null) {
            return currentAssist.equals(activeService);
        }
        return false;
    }

    private void confirmNewAssist(final String newAssitPackage) {
        int selected = this.mDefaultAssitPref.findIndexOfValue(newAssitPackage);
        CharSequence appLabel = this.mDefaultAssitPref.getEntries()[selected];
        String title = getString(R.string.assistant_security_warning_title, new Object[]{appLabel});
        String message = getString(R.string.assistant_security_warning, new Object[]{appLabel});
        DialogInterface.OnClickListener onAgree = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ManageAssist.this.setDefaultAssist(newAssitPackage);
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title).setMessage(message).setCancelable(true).setPositiveButton(R.string.assistant_security_warning_agree, onAgree).setNegativeButton(R.string.assistant_security_warning_disagree, (DialogInterface.OnClickListener) null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void setDefaultAssist(String assistPackage) {
        this.mDefaultAssitPref.setValue(assistPackage);
        updateUi();
    }
}
