package com.android.services.telephony.sip;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.phone.R;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SipSettings extends PreferenceActivity {
    private PackageManager mPackageManager;
    private SipProfile mProfile;
    private SipProfileDb mProfileDb;
    private PreferenceCategory mSipListContainer;
    private SipManager mSipManager;
    private Map<String, SipPreference> mSipPreferenceMap;
    private SipPreferences mSipPreferences;
    private List<SipProfile> mSipProfileList;
    private int mUid = Process.myUid();

    private class SipPreference extends Preference {
        SipProfile mProfile;

        SipPreference(Context context, SipProfile sipProfile) {
            super(context);
            setProfile(sipProfile);
        }

        void setProfile(SipProfile sipProfile) {
            String string;
            this.mProfile = sipProfile;
            setTitle(SipSettings.this.getProfileName(sipProfile));
            if (SipSettings.this.mSipPreferences.isReceivingCallsEnabled()) {
                string = SipSettings.this.getString(R.string.registration_status_checking_status);
            } else {
                string = SipSettings.this.getString(R.string.registration_status_not_receiving);
            }
            updateSummary(string);
        }

        void updateSummary(String str) {
            int callingUid = this.mProfile.getCallingUid();
            if (callingUid > 0 && callingUid != SipSettings.this.mUid) {
                str = SipSettings.this.getString(R.string.third_party_account_summary, new Object[]{SipSettings.this.getPackageNameFromUid(callingUid)});
            }
            setSummary(str);
        }
    }

    private String getPackageNameFromUid(int i) {
        try {
            return this.mPackageManager.getApplicationInfo(this.mPackageManager.getPackagesForUid(i)[0], 0).loadLabel(this.mPackageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            log("getPackageNameFromUid, cannot find name of uid: " + i + ", exception: " + e);
            return "uid:" + i;
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSipManager = SipManager.newInstance(this);
        this.mSipPreferences = new SipPreferences(this);
        this.mProfileDb = new SipProfileDb(this);
        this.mPackageManager = getPackageManager();
        setContentView(R.layout.sip_settings_ui);
        addPreferencesFromResource(R.xml.sip_setting);
        this.mSipListContainer = (PreferenceCategory) findPreference("sip_account_list");
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfilesStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterForContextMenu(getListView());
    }

    @Override
    protected void onActivityResult(int i, final int i2, final Intent intent) {
        if (i2 == -1 || i2 == 1) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (SipSettings.this.mProfile != null) {
                            SipSettings.this.deleteProfile(SipSettings.this.mProfile);
                        }
                        SipProfile sipProfile = (SipProfile) intent.getParcelableExtra("sip_profile");
                        if (i2 == -1) {
                            SipSettings.this.addProfile(sipProfile);
                        }
                        SipSettings.this.updateProfilesStatus();
                    } catch (IOException e) {
                        SipSettings.log("onActivityResult, can not handle the profile:  " + e);
                    }
                }
            }.start();
        }
    }

    private void updateProfilesStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSettings.this.retrieveSipLists();
                } catch (Exception e) {
                    SipSettings.log("updateProfilesStatus, exception: " + e);
                }
            }
        }).start();
    }

    private String getProfileName(SipProfile sipProfile) {
        String profileName = sipProfile.getProfileName();
        if (TextUtils.isEmpty(profileName)) {
            return sipProfile.getUserName() + "@" + sipProfile.getSipDomain();
        }
        return profileName;
    }

    private void retrieveSipLists() {
        this.mSipPreferenceMap = new LinkedHashMap();
        this.mSipProfileList = this.mProfileDb.retrieveSipProfileList();
        processActiveProfilesFromSipService();
        Collections.sort(this.mSipProfileList, new Comparator<SipProfile>() {
            @Override
            public int compare(SipProfile sipProfile, SipProfile sipProfile2) {
                return SipSettings.this.getProfileName(sipProfile).compareTo(SipSettings.this.getProfileName(sipProfile2));
            }
        });
        this.mSipListContainer.removeAll();
        if (this.mSipProfileList.isEmpty()) {
            getPreferenceScreen().removePreference(this.mSipListContainer);
        } else {
            getPreferenceScreen().addPreference(this.mSipListContainer);
            Iterator<SipProfile> it = this.mSipProfileList.iterator();
            while (it.hasNext()) {
                addPreferenceFor(it.next());
            }
        }
        if (this.mSipPreferences.isReceivingCallsEnabled()) {
            for (SipProfile sipProfile : this.mSipProfileList) {
                if (this.mUid == sipProfile.getCallingUid()) {
                    try {
                        this.mSipManager.setRegistrationListener(sipProfile.getUriString(), createRegistrationListener());
                    } catch (SipException e) {
                        log("retrieveSipLists, cannot set registration listener: " + e);
                    }
                }
            }
        }
    }

    private void processActiveProfilesFromSipService() {
        SipProfile[] listOfProfiles = new SipProfile[0];
        try {
            listOfProfiles = this.mSipManager.getListOfProfiles();
        } catch (SipException e) {
            log("SipManager could not retrieve SIP profiles: " + e);
        }
        for (SipProfile sipProfile : listOfProfiles) {
            SipProfile profileFromList = getProfileFromList(sipProfile);
            if (profileFromList == null) {
                this.mSipProfileList.add(sipProfile);
            } else {
                profileFromList.setCallingUid(sipProfile.getCallingUid());
            }
        }
    }

    private SipProfile getProfileFromList(SipProfile sipProfile) {
        for (SipProfile sipProfile2 : this.mSipProfileList) {
            if (sipProfile2.getUriString().equals(sipProfile.getUriString())) {
                return sipProfile2;
            }
        }
        return null;
    }

    private void addPreferenceFor(SipProfile sipProfile) {
        SipPreference sipPreference = new SipPreference(this, sipProfile);
        this.mSipPreferenceMap.put(sipProfile.getUriString(), sipPreference);
        this.mSipListContainer.addPreference(sipPreference);
        sipPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SipSettings.this.handleProfileClick(((SipPreference) preference).mProfile);
                return true;
            }
        });
    }

    private void handleProfileClick(final SipProfile sipProfile) {
        int callingUid = sipProfile.getCallingUid();
        if (callingUid == this.mUid || callingUid == 0) {
            startSipEditor(sipProfile);
        } else {
            new AlertDialog.Builder(this).setTitle(R.string.alert_dialog_close).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(R.string.close_profile, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SipSettings.this.deleteProfile(sipProfile);
                    SipSettings.this.unregisterProfile(sipProfile);
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        }
    }

    private void unregisterProfile(final SipProfile sipProfile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSettings.this.mSipManager.close(sipProfile.getUriString());
                } catch (Exception e) {
                    SipSettings.log("unregisterProfile, unregister failed, SipService died? Exception: " + e);
                }
            }
        }, "unregisterProfile").start();
    }

    void deleteProfile(SipProfile sipProfile) {
        this.mSipProfileList.remove(sipProfile);
        SipPreference sipPreferenceRemove = this.mSipPreferenceMap.remove(sipProfile.getUriString());
        if (sipPreferenceRemove != null) {
            this.mSipListContainer.removePreference(sipPreferenceRemove);
        }
    }

    private void addProfile(SipProfile sipProfile) throws IOException {
        try {
            this.mSipManager.setRegistrationListener(sipProfile.getUriString(), createRegistrationListener());
        } catch (Exception e) {
            log("addProfile, cannot set registration listener: " + e);
        }
        this.mSipProfileList.add(sipProfile);
        addPreferenceFor(sipProfile);
    }

    private void startSipEditor(SipProfile sipProfile) {
        this.mProfile = sipProfile;
        Intent intent = new Intent(this, (Class<?>) SipEditor.class);
        intent.putExtra("sip_profile", (Parcelable) sipProfile);
        startActivityForResult(intent, 1);
    }

    private void showRegistrationMessage(final String str, final String str2) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SipPreference sipPreference = (SipPreference) SipSettings.this.mSipPreferenceMap.get(str);
                if (sipPreference != null) {
                    sipPreference.updateSummary(str2);
                }
            }
        });
    }

    private SipRegistrationListener createRegistrationListener() {
        return new SipRegistrationListener() {
            @Override
            public void onRegistrationDone(String str, long j) {
                SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_done));
            }

            @Override
            public void onRegistering(String str) {
                SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_registering));
            }

            @Override
            public void onRegistrationFailed(String str, int i, String str2) {
                if (i == -12) {
                    SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_server_unreachable));
                }
                if (i == -4) {
                    SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_not_running));
                    return;
                }
                switch (i) {
                    case -10:
                        if (SipManager.isSipWifiOnly(SipSettings.this.getApplicationContext())) {
                            SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_no_wifi_data));
                        } else {
                            SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_no_data));
                        }
                        break;
                    case -9:
                        SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_still_trying));
                        break;
                    case -8:
                        SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_invalid_credentials));
                        break;
                    default:
                        SipSettings.this.showRegistrationMessage(str, SipSettings.this.getString(R.string.registration_status_failed_try_later, new Object[]{str2}));
                        break;
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuItemAdd = menu.add(0, 1, 0, R.string.add_sip_account);
        menuItemAdd.setIcon(R.drawable.ic_add_24dp);
        menuItemAdd.setShowAsAction(1);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(1).setEnabled(SipUtil.isPhoneIdle(this));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 1) {
            startSipEditor(null);
            return true;
        }
        if (itemId == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private static void log(String str) {
        Log.d("SIP", "[SipSettings] " + str);
    }
}
