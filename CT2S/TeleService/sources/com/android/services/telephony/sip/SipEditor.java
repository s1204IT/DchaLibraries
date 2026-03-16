package com.android.services.telephony.sip;

import android.app.AlertDialog;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import com.android.phone.R;
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
    private SipSharedPreferences mSharedPreferences;
    private SipAccountRegistry mSipAccountRegistry;
    private boolean mUpdateRequired;

    enum PreferenceKey {
        Username(R.string.username, 0, R.string.default_preference_summary),
        Password(R.string.password, 0, R.string.default_preference_summary),
        DomainAddress(R.string.domain_address, 0, R.string.default_preference_summary),
        DisplayName(R.string.display_name, 0, R.string.display_name_summary),
        ProxyAddress(R.string.proxy_address, 0, R.string.optional_summary),
        Port(R.string.port, R.string.default_port, R.string.default_port),
        Transport(R.string.transport, R.string.default_transport, 0),
        SendKeepAlive(R.string.send_keepalive, R.string.sip_system_decide, 0),
        AuthUserName(R.string.auth_username, 0, R.string.optional_summary);

        final int defaultSummary;
        final int initValue;
        Preference preference;
        final int text;

        PreferenceKey(int text, int initValue, int defaultSummary) {
            this.text = text;
            this.initValue = initValue;
            this.defaultSummary = defaultSummary;
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

        void setValue(String value) {
            if (this.preference instanceof EditTextPreference) {
                getValue();
                ((EditTextPreference) this.preference).setText(value);
                if (this != Password) {
                }
            } else if (this.preference instanceof ListPreference) {
                ((ListPreference) this.preference).setValue(value);
            }
            if (TextUtils.isEmpty(value)) {
                this.preference.setSummary(this.defaultSummary);
                return;
            }
            if (this == Password) {
                this.preference.setSummary(SipEditor.scramble(value));
            } else if (this == DisplayName && value.equals(SipEditor.getDefaultDisplayName())) {
                this.preference.setSummary(this.defaultSummary);
            } else {
                this.preference.setSummary(value);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSharedPreferences = new SipSharedPreferences(this);
        this.mProfileDb = new SipProfileDb(this);
        this.mSipAccountRegistry = SipAccountRegistry.getInstance();
        setContentView(R.layout.sip_settings_ui);
        addPreferencesFromResource(R.xml.sip_edit);
        SipProfile p = (SipProfile) (savedInstanceState == null ? getIntent().getParcelableExtra("sip_profile") : savedInstanceState.getParcelable("profile"));
        this.mOldProfile = p;
        PreferenceGroup screen = getPreferenceScreen();
        int n = screen.getPreferenceCount();
        for (int i = 0; i < n; i++) {
            setupPreference(screen.getPreference(i));
        }
        if (p == null) {
            screen.setTitle(R.string.sip_edit_new_title);
        }
        this.mAdvancedSettings = new AdvancedSettings();
        loadPreferencesFromProfile(p);
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
        menu.add(0, 2, 0, R.string.sip_menu_discard).setShowAsAction(1);
        menu.add(0, 1, 0, R.string.sip_menu_save).setShowAsAction(1);
        menu.add(0, 3, 0, R.string.remove_sip_account).setShowAsAction(0);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem removeMenu = menu.findItem(3);
        removeMenu.setVisible(this.mOldProfile != null);
        menu.findItem(1).setEnabled(this.mUpdateRequired);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                validateAndSetResult();
                return true;
            case 2:
                finish();
                return true;
            case 3:
                setRemovedProfileAndFinish();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
                validateAndSetResult();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void saveAndRegisterProfile(SipProfile p) throws IOException {
        if (p != null) {
            this.mProfileDb.saveProfile(p);
            this.mSipAccountRegistry.startSipService(this, p.getUriString());
        }
    }

    private void deleteAndUnregisterProfile(SipProfile p) throws IOException {
        if (p != null) {
            this.mProfileDb.deleteProfile(p);
            this.mSipAccountRegistry.stopSipService(this, p.getUriString());
        }
    }

    private void setRemovedProfileAndFinish() {
        Intent intent = new Intent(this, (Class<?>) SipSettings.class);
        setResult(1, intent);
        Toast.makeText(this, R.string.removing_account, 0).show();
        replaceProfile(this.mOldProfile, null);
    }

    private void showAlert(Throwable e) {
        String msg = e.getMessage();
        if (TextUtils.isEmpty(msg)) {
            msg = e.toString();
        }
        showAlert(msg);
    }

    private void showAlert(final String message) {
        if (!this.mHomeButtonClicked) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(SipEditor.this).setTitle(android.R.string.dialog_alert_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(message).setPositiveButton(R.string.alert_dialog_ok, (DialogInterface.OnClickListener) null).show();
                }
            });
        }
    }

    private boolean isEditTextEmpty(PreferenceKey key) {
        EditTextPreference pref = (EditTextPreference) key.preference;
        return TextUtils.isEmpty(pref.getText()) || pref.getSummary().equals(getString(key.defaultSummary));
    }

    private void validateAndSetResult() {
        int port;
        boolean allEmpty = true;
        CharSequence firstEmptyFieldTitle = null;
        PreferenceKey[] arr$ = PreferenceKey.values();
        for (PreferenceKey key : arr$) {
            Preference p = key.preference;
            if (p instanceof EditTextPreference) {
                EditTextPreference pref = (EditTextPreference) p;
                boolean fieldEmpty = isEditTextEmpty(key);
                if (allEmpty && !fieldEmpty) {
                    allEmpty = false;
                }
                if (fieldEmpty) {
                    switch (key) {
                        case DisplayName:
                            pref.setText(getDefaultDisplayName());
                            break;
                        case AuthUserName:
                        case ProxyAddress:
                            break;
                        case Port:
                            pref.setText(getString(R.string.default_port));
                            break;
                        default:
                            if (firstEmptyFieldTitle == null) {
                                firstEmptyFieldTitle = pref.getTitle();
                            }
                            break;
                    }
                } else if (key == PreferenceKey.Port && ((port = Integer.parseInt(PreferenceKey.Port.getValue())) < 1000 || port > 65534)) {
                    showAlert(getString(R.string.not_a_valid_port));
                    return;
                }
            }
        }
        if (allEmpty || !this.mUpdateRequired) {
            finish();
            return;
        }
        if (firstEmptyFieldTitle != null) {
            showAlert(getString(R.string.empty_alert, new Object[]{firstEmptyFieldTitle}));
            return;
        }
        try {
            SipProfile profile = createSipProfile();
            Intent intent = new Intent(this, (Class<?>) SipSettings.class);
            intent.putExtra("sip_profile", (Parcelable) profile);
            setResult(-1, intent);
            Toast.makeText(this, R.string.saving_account, 0).show();
            replaceProfile(this.mOldProfile, profile);
        } catch (Exception e) {
            log("validateAndSetResult, can not create new SipProfile, exception: " + e);
            showAlert(e);
        }
    }

    private void replaceProfile(final SipProfile oldProfile, final SipProfile newProfile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SipEditor.this.deleteAndUnregisterProfile(oldProfile);
                    SipEditor.this.saveAndRegisterProfile(newProfile);
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
        return new SipProfile.Builder(PreferenceKey.Username.getValue(), PreferenceKey.DomainAddress.getValue()).setProfileName(getProfileName()).setPassword(PreferenceKey.Password.getValue()).setOutboundProxy(PreferenceKey.ProxyAddress.getValue()).setProtocol(PreferenceKey.Transport.getValue()).setDisplayName(PreferenceKey.DisplayName.getValue()).setPort(Integer.parseInt(PreferenceKey.Port.getValue())).setSendKeepAlive(isAlwaysSendKeepAlive()).setAutoRegistration(this.mSharedPreferences.isReceivingCallsEnabled()).setAuthUserName(PreferenceKey.AuthUserName.getValue()).build();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (!this.mUpdateRequired) {
            this.mUpdateRequired = true;
        }
        if (pref instanceof CheckBoxPreference) {
            invalidateOptionsMenu();
        } else {
            String value = newValue == null ? "" : newValue.toString();
            if (TextUtils.isEmpty(value)) {
                pref.setSummary(getPreferenceKey(pref).defaultSummary);
            } else if (pref == PreferenceKey.Password.preference) {
                pref.setSummary(scramble(value));
            } else {
                pref.setSummary(value);
            }
            if (pref == PreferenceKey.DisplayName.preference) {
                ((EditTextPreference) pref).setText(value);
                checkIfDisplayNameSet();
            }
            invalidateOptionsMenu();
        }
        return true;
    }

    private PreferenceKey getPreferenceKey(Preference pref) {
        PreferenceKey[] arr$ = PreferenceKey.values();
        for (PreferenceKey key : arr$) {
            if (key.preference == pref) {
                return key;
            }
        }
        throw new RuntimeException("not possible to reach here");
    }

    private void loadPreferencesFromProfile(SipProfile p) {
        if (p != null) {
            try {
                PreferenceKey[] arr$ = PreferenceKey.values();
                for (PreferenceKey key : arr$) {
                    Method meth = SipProfile.class.getMethod("get" + getString(key.text), (Class[]) null);
                    if (key == PreferenceKey.SendKeepAlive) {
                        key.setValue(getString(((Boolean) meth.invoke(p, (Object[]) null)).booleanValue() ? R.string.sip_always_send_keepalive : R.string.sip_system_decide));
                    } else {
                        Object value = meth.invoke(p, (Object[]) null);
                        key.setValue(value == null ? "" : value.toString());
                    }
                }
                checkIfDisplayNameSet();
                return;
            } catch (Exception e) {
                log("loadPreferencesFromProfile, can not load pref from profile, exception: " + e);
                return;
            }
        }
        PreferenceKey[] arr$2 = PreferenceKey.values();
        for (PreferenceKey key2 : arr$2) {
            key2.preference.setOnPreferenceChangeListener(this);
            if (key2.initValue != 0) {
                key2.setValue(getString(key2.initValue));
            }
        }
        this.mDisplayNameSet = false;
    }

    private boolean isAlwaysSendKeepAlive() {
        ListPreference pref = (ListPreference) PreferenceKey.SendKeepAlive.preference;
        return getString(R.string.sip_always_send_keepalive).equals(pref.getValue());
    }

    private void setupPreference(Preference pref) {
        pref.setOnPreferenceChangeListener(this);
        PreferenceKey[] arr$ = PreferenceKey.values();
        for (PreferenceKey key : arr$) {
            String name = getString(key.text);
            if (name.equals(pref.getKey())) {
                key.preference = pref;
                return;
            }
        }
    }

    private void checkIfDisplayNameSet() {
        String displayName = PreferenceKey.DisplayName.getValue();
        this.mDisplayNameSet = (TextUtils.isEmpty(displayName) || displayName.equals(getDefaultDisplayName())) ? false : true;
        if (this.mDisplayNameSet) {
            PreferenceKey.DisplayName.preference.setSummary(displayName);
        } else {
            PreferenceKey.DisplayName.setValue("");
        }
    }

    private static String getDefaultDisplayName() {
        return PreferenceKey.Username.getValue();
    }

    private static String scramble(String s) {
        char[] cc = new char[s.length()];
        Arrays.fill(cc, '*');
        return new String(cc);
    }

    private class AdvancedSettings implements Preference.OnPreferenceClickListener {
        private Preference mAdvancedSettingsTrigger;
        private Preference[] mPreferences;
        private boolean mShowing = false;

        AdvancedSettings() {
            this.mAdvancedSettingsTrigger = SipEditor.this.getPreferenceScreen().findPreference(SipEditor.this.getString(R.string.advanced_settings));
            this.mAdvancedSettingsTrigger.setOnPreferenceClickListener(this);
            loadAdvancedPreferences();
        }

        private void loadAdvancedPreferences() {
            PreferenceGroup screen = SipEditor.this.getPreferenceScreen();
            SipEditor.this.addPreferencesFromResource(R.xml.sip_advanced_edit);
            PreferenceGroup group = (PreferenceGroup) screen.findPreference(SipEditor.this.getString(R.string.advanced_settings_container));
            screen.removePreference(group);
            this.mPreferences = new Preference[group.getPreferenceCount()];
            int order = screen.getPreferenceCount();
            int i = 0;
            int n = this.mPreferences.length;
            int order2 = order;
            while (i < n) {
                Preference pref = group.getPreference(i);
                pref.setOrder(order2);
                SipEditor.this.setupPreference(pref);
                this.mPreferences[i] = pref;
                i++;
                order2++;
            }
        }

        void show() {
            this.mShowing = true;
            this.mAdvancedSettingsTrigger.setSummary(R.string.advanced_settings_hide);
            PreferenceGroup screen = SipEditor.this.getPreferenceScreen();
            Preference[] arr$ = this.mPreferences;
            for (Preference pref : arr$) {
                screen.addPreference(pref);
            }
        }

        private void hide() {
            this.mShowing = false;
            this.mAdvancedSettingsTrigger.setSummary(R.string.advanced_settings_show);
            PreferenceGroup screen = SipEditor.this.getPreferenceScreen();
            Preference[] arr$ = this.mPreferences;
            for (Preference pref : arr$) {
                screen.removePreference(pref);
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

    private static void log(String msg) {
        Log.d("SIP", "[SipEditor] " + msg);
    }
}
