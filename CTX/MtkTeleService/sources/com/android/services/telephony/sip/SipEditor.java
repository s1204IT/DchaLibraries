package com.android.services.telephony.sip;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class SipEditor extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private AdvancedSettings mAdvancedSettings;
    private boolean mDisplayNameSet;
    private boolean mHomeButtonClicked;
    private SipProfile mOldProfile;
    private SipProfileDb mProfileDb;
    private Button mRemoveButton;
    private SipAccountRegistry mSipAccountRegistry;
    private SipPreferences mSipPreferences;
    private boolean mUpdateRequired;

    public static class AlertDialogFragment extends DialogFragment {
        public static AlertDialogFragment newInstance(String str) {
            AlertDialogFragment alertDialogFragment = new AlertDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("message", str);
            alertDialogFragment.setArguments(bundle);
            return alertDialogFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_alert_title).setIconAttribute(R.attr.alertDialogIcon).setMessage(getArguments().getString("message")).setPositiveButton(com.android.phone.R.string.alert_dialog_ok, (DialogInterface.OnClickListener) null).create();
        }
    }

    enum PreferenceKey {
        Username(com.android.phone.R.string.username, 0, com.android.phone.R.string.default_preference_summary_username),
        Password(com.android.phone.R.string.password, 0, com.android.phone.R.string.default_preference_summary_password),
        DomainAddress(com.android.phone.R.string.domain_address, 0, com.android.phone.R.string.default_preference_summary_domain_address),
        DisplayName(com.android.phone.R.string.display_name, 0, com.android.phone.R.string.display_name_summary),
        ProxyAddress(com.android.phone.R.string.proxy_address, 0, com.android.phone.R.string.optional_summary),
        Port(com.android.phone.R.string.port, com.android.phone.R.string.default_port, com.android.phone.R.string.default_port),
        Transport(com.android.phone.R.string.transport, com.android.phone.R.string.default_transport, 0),
        SendKeepAlive(com.android.phone.R.string.send_keepalive, com.android.phone.R.string.sip_system_decide, 0),
        AuthUserName(com.android.phone.R.string.auth_username, 0, com.android.phone.R.string.optional_summary);

        final int defaultSummary;
        final int initValue;
        Preference preference;
        final int text;

        PreferenceKey(int i, int i2, int i3) {
            this.text = i;
            this.initValue = i2;
            this.defaultSummary = i3;
        }

        String getValue() {
            if (this.preference instanceof EditTextPreference) {
                return ((EditTextPreference) this.preference).getText();
            }
            if (this.preference instanceof ListPreference) {
                return ((ListPreference) this.preference).getValue();
            }
            throw new RuntimeException("getValue() for the preference " + this);
        }

        void setValue(String str) {
            if (this.preference instanceof EditTextPreference) {
                getValue();
                ((EditTextPreference) this.preference).setText(str);
                PreferenceKey preferenceKey = Password;
            } else if (this.preference instanceof ListPreference) {
                ((ListPreference) this.preference).setValue(str);
            }
            if (TextUtils.isEmpty(str)) {
                this.preference.setSummary(this.defaultSummary);
                return;
            }
            if (this == Password) {
                this.preference.setSummary(SipEditor.scramble(str));
            } else if (this == DisplayName && str.equals(SipEditor.getDefaultDisplayName())) {
                this.preference.setSummary(this.defaultSummary);
            } else {
                this.preference.setSummary(str);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mHomeButtonClicked = false;
        if (!SipUtil.isPhoneIdle(this)) {
            this.mAdvancedSettings.show();
            getPreferenceScreen().setEnabled(false);
            if (this.mRemoveButton != null) {
                this.mRemoveButton.setEnabled(false);
                return;
            }
            return;
        }
        getPreferenceScreen().setEnabled(true);
        if (this.mRemoveButton != null) {
            this.mRemoveButton.setEnabled(true);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        Parcelable parcelable;
        super.onCreate(bundle);
        this.mSipPreferences = new SipPreferences(this);
        this.mProfileDb = new SipProfileDb(this);
        this.mSipAccountRegistry = SipAccountRegistry.getInstance();
        setContentView(com.android.phone.R.layout.sip_settings_ui);
        addPreferencesFromResource(com.android.phone.R.xml.sip_edit);
        if (bundle == null) {
            parcelable = getIntent().getParcelableExtra("sip_profile");
        } else {
            parcelable = bundle.getParcelable("profile");
        }
        SipProfile sipProfile = (SipProfile) parcelable;
        this.mOldProfile = sipProfile;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int preferenceCount = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            setupPreference(preferenceScreen.getPreference(i));
        }
        if (sipProfile == null) {
            preferenceScreen.setTitle(com.android.phone.R.string.sip_edit_new_title);
        }
        this.mAdvancedSettings = new AdvancedSettings();
        loadPreferencesFromProfile(sipProfile);
    }

    @Override
    public void onPause() {
        if (!isFinishing()) {
            this.mHomeButtonClicked = true;
            validateAndSetResult();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 2, 0, com.android.phone.R.string.sip_menu_discard).setShowAsAction(1);
        menu.add(0, 1, 0, com.android.phone.R.string.sip_menu_save).setShowAsAction(1);
        menu.add(0, 3, 0, com.android.phone.R.string.remove_sip_account).setShowAsAction(0);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(3).setVisible(this.mOldProfile != null);
        menu.findItem(1).setEnabled(this.mUpdateRequired);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId != 16908332) {
            switch (itemId) {
                case 1:
                    validateAndSetResult();
                    return true;
                case 2:
                    finish();
                    return true;
                case 3:
                    setRemovedProfileAndFinish();
                    return true;
                default:
                    return super.onOptionsItemSelected(menuItem);
            }
        }
        finish();
        return true;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            validateAndSetResult();
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    private void saveAndRegisterProfile(SipProfile sipProfile, boolean z) throws IOException {
        if (sipProfile == null) {
            return;
        }
        this.mProfileDb.saveProfile(sipProfile);
        this.mSipAccountRegistry.startSipService(this, sipProfile.getProfileName(), z);
    }

    private void deleteAndUnregisterProfile(SipProfile sipProfile) throws IOException {
        if (sipProfile == null) {
            return;
        }
        this.mProfileDb.deleteProfile(sipProfile);
        this.mSipAccountRegistry.stopSipService(this, sipProfile.getProfileName());
    }

    private void setRemovedProfileAndFinish() {
        setResult(1, new Intent(this, (Class<?>) SipSettings.class));
        Toast.makeText(this, com.android.phone.R.string.removing_account, 0).show();
        replaceProfile(this.mOldProfile, null);
    }

    private void showAlert(Throwable th) {
        String message = th.getMessage();
        if (TextUtils.isEmpty(message)) {
            message = th.toString();
        }
        showAlert(message);
    }

    private void showAlert(String str) {
        if (this.mHomeButtonClicked) {
            return;
        }
        AlertDialogFragment.newInstance(str).show(getFragmentManager(), (String) null);
    }

    private boolean isEditTextEmpty(PreferenceKey preferenceKey) {
        EditTextPreference editTextPreference = (EditTextPreference) preferenceKey.preference;
        return TextUtils.isEmpty(editTextPreference.getText()) || editTextPreference.getSummary().equals(getString(preferenceKey.defaultSummary));
    }

    private void validateAndSetResult() {
        boolean z = true;
        CharSequence title = null;
        for (PreferenceKey preferenceKey : PreferenceKey.values()) {
            Preference preference = preferenceKey.preference;
            if (preference instanceof EditTextPreference) {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                boolean zIsEditTextEmpty = isEditTextEmpty(preferenceKey);
                if (z && !zIsEditTextEmpty) {
                    z = false;
                }
                if (zIsEditTextEmpty) {
                    switch (preferenceKey) {
                        case DisplayName:
                            editTextPreference.setText(getDefaultDisplayName());
                            break;
                        case AuthUserName:
                        case ProxyAddress:
                            break;
                        case Port:
                            editTextPreference.setText(getString(com.android.phone.R.string.default_port));
                            break;
                        default:
                            if (title == null) {
                                title = editTextPreference.getTitle();
                            }
                            break;
                    }
                } else if (preferenceKey == PreferenceKey.Port) {
                    try {
                        int i = Integer.parseInt(PreferenceKey.Port.getValue());
                        if (i < 1000 || i > 65534) {
                            showAlert(getString(com.android.phone.R.string.not_a_valid_port));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        showAlert(getString(com.android.phone.R.string.not_a_valid_port));
                        return;
                    }
                } else {
                    continue;
                }
            }
        }
        if (!this.mUpdateRequired) {
            finish();
            return;
        }
        if (z) {
            showAlert(getString(com.android.phone.R.string.all_empty_alert));
            return;
        }
        if (title != null) {
            showAlert(getString(com.android.phone.R.string.empty_alert, new Object[]{title}));
            return;
        }
        try {
            SipProfile sipProfileCreateSipProfile = createSipProfile();
            Intent intent = new Intent(this, (Class<?>) SipSettings.class);
            intent.putExtra("sip_profile", (Parcelable) sipProfileCreateSipProfile);
            setResult(-1, intent);
            Toast.makeText(this, com.android.phone.R.string.saving_account, 0).show();
            replaceProfile(this.mOldProfile, sipProfileCreateSipProfile);
        } catch (Exception e2) {
            log("validateAndSetResult, can not create new SipProfile, exception: " + e2);
            showAlert(e2);
        }
    }

    private void replaceProfile(final SipProfile sipProfile, final SipProfile sipProfile2) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SipEditor.this.deleteAndUnregisterProfile(sipProfile);
                    SipEditor.this.saveAndRegisterProfile(sipProfile2, sipProfile == null);
                    SipEditor.this.finish();
                } catch (Exception e) {
                    SipEditor.log("replaceProfile, can not save/register new SipProfile, exception: " + e);
                    SipEditor.this.showAlert(e);
                }
            }
        }, "SipEditor").start();
    }

    private String getProfileName() {
        return PreferenceKey.Username.getValue() + "@" + PreferenceKey.DomainAddress.getValue();
    }

    private SipProfile createSipProfile() throws Exception {
        return new SipProfile.Builder(PreferenceKey.Username.getValue(), PreferenceKey.DomainAddress.getValue()).setProfileName(getProfileName()).setPassword(PreferenceKey.Password.getValue()).setOutboundProxy(PreferenceKey.ProxyAddress.getValue()).setProtocol(PreferenceKey.Transport.getValue()).setDisplayName(PreferenceKey.DisplayName.getValue()).setPort(Integer.parseInt(PreferenceKey.Port.getValue())).setSendKeepAlive(isAlwaysSendKeepAlive()).setAutoRegistration(this.mSipPreferences.isReceivingCallsEnabled()).setAuthUserName(PreferenceKey.AuthUserName.getValue()).build();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (!this.mUpdateRequired) {
            this.mUpdateRequired = true;
        }
        if (preference instanceof CheckBoxPreference) {
            invalidateOptionsMenu();
            return true;
        }
        String string = obj == null ? "" : obj.toString();
        if (TextUtils.isEmpty(string)) {
            preference.setSummary(getPreferenceKey(preference).defaultSummary);
        } else if (preference == PreferenceKey.Password.preference) {
            preference.setSummary(scramble(string));
        } else {
            preference.setSummary(string);
        }
        if (preference == PreferenceKey.DisplayName.preference) {
            ((EditTextPreference) preference).setText(string);
            checkIfDisplayNameSet();
        }
        invalidateOptionsMenu();
        return true;
    }

    private PreferenceKey getPreferenceKey(Preference preference) {
        for (PreferenceKey preferenceKey : PreferenceKey.values()) {
            if (preferenceKey.preference == preference) {
                return preferenceKey;
            }
        }
        throw new RuntimeException("not possible to reach here");
    }

    private void loadPreferencesFromProfile(SipProfile sipProfile) {
        int i;
        if (sipProfile != null) {
            try {
                for (PreferenceKey preferenceKey : PreferenceKey.values()) {
                    Method method = SipProfile.class.getMethod("get" + getString(preferenceKey.text), (Class[]) null);
                    if (preferenceKey == PreferenceKey.SendKeepAlive) {
                        if (((Boolean) method.invoke(sipProfile, (Object[]) null)).booleanValue()) {
                            i = com.android.phone.R.string.sip_always_send_keepalive;
                        } else {
                            i = com.android.phone.R.string.sip_system_decide;
                        }
                        preferenceKey.setValue(getString(i));
                    } else {
                        Object objInvoke = method.invoke(sipProfile, (Object[]) null);
                        preferenceKey.setValue(objInvoke == null ? "" : objInvoke.toString());
                    }
                }
                checkIfDisplayNameSet();
                return;
            } catch (Exception e) {
                log("loadPreferencesFromProfile, can not load pref from profile, exception: " + e);
                return;
            }
        }
        for (PreferenceKey preferenceKey2 : PreferenceKey.values()) {
            preferenceKey2.preference.setOnPreferenceChangeListener(this);
            if (preferenceKey2.initValue != 0) {
                preferenceKey2.setValue(getString(preferenceKey2.initValue));
            }
        }
        this.mDisplayNameSet = false;
    }

    private boolean isAlwaysSendKeepAlive() {
        return getString(com.android.phone.R.string.sip_always_send_keepalive).equals(((ListPreference) PreferenceKey.SendKeepAlive.preference).getValue());
    }

    private void setupPreference(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        for (PreferenceKey preferenceKey : PreferenceKey.values()) {
            if (getString(preferenceKey.text).equals(preference.getKey())) {
                preferenceKey.preference = preference;
                return;
            }
        }
    }

    private void checkIfDisplayNameSet() {
        String value = PreferenceKey.DisplayName.getValue();
        this.mDisplayNameSet = (TextUtils.isEmpty(value) || value.equals(getDefaultDisplayName())) ? false : true;
        if (this.mDisplayNameSet) {
            PreferenceKey.DisplayName.preference.setSummary(value);
        } else {
            PreferenceKey.DisplayName.setValue("");
        }
    }

    private static String getDefaultDisplayName() {
        return PreferenceKey.Username.getValue();
    }

    private static String scramble(String str) {
        char[] cArr = new char[str.length()];
        Arrays.fill(cArr, '*');
        return new String(cArr);
    }

    private class AdvancedSettings implements Preference.OnPreferenceClickListener {
        private Preference mAdvancedSettingsTrigger;
        private Preference[] mPreferences;
        private boolean mShowing = false;

        AdvancedSettings() {
            this.mAdvancedSettingsTrigger = SipEditor.this.getPreferenceScreen().findPreference(SipEditor.this.getString(com.android.phone.R.string.advanced_settings));
            this.mAdvancedSettingsTrigger.setOnPreferenceClickListener(this);
            loadAdvancedPreferences();
        }

        private void loadAdvancedPreferences() {
            PreferenceScreen preferenceScreen = SipEditor.this.getPreferenceScreen();
            SipEditor.this.addPreferencesFromResource(com.android.phone.R.xml.sip_advanced_edit);
            PreferenceGroup preferenceGroup = (PreferenceGroup) preferenceScreen.findPreference(SipEditor.this.getString(com.android.phone.R.string.advanced_settings_container));
            preferenceScreen.removePreference(preferenceGroup);
            this.mPreferences = new Preference[preferenceGroup.getPreferenceCount()];
            int preferenceCount = preferenceScreen.getPreferenceCount();
            int length = this.mPreferences.length;
            int i = 0;
            while (i < length) {
                Preference preference = preferenceGroup.getPreference(i);
                preference.setOrder(preferenceCount);
                SipEditor.this.setupPreference(preference);
                this.mPreferences[i] = preference;
                i++;
                preferenceCount++;
            }
        }

        void show() {
            this.mShowing = true;
            this.mAdvancedSettingsTrigger.setSummary(com.android.phone.R.string.advanced_settings_hide);
            PreferenceScreen preferenceScreen = SipEditor.this.getPreferenceScreen();
            for (Preference preference : this.mPreferences) {
                preferenceScreen.addPreference(preference);
            }
        }

        private void hide() {
            this.mShowing = false;
            this.mAdvancedSettingsTrigger.setSummary(com.android.phone.R.string.advanced_settings_show);
            PreferenceScreen preferenceScreen = SipEditor.this.getPreferenceScreen();
            for (Preference preference : this.mPreferences) {
                preferenceScreen.removePreference(preference);
            }
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (!this.mShowing) {
                show();
                return true;
            }
            hide();
            return true;
        }
    }

    private static void log(String str) {
        Log.d("SIP", "[SipEditor] " + str);
    }
}
