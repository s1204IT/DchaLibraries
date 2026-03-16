package com.android.services.telephony.sip;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
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
    private List<SipProfile> mSipProfileList;
    private SipSharedPreferences mSipSharedPreferences;
    private int mUid = Process.myUid();

    private class SipPreference extends Preference {
        SipProfile mProfile;

        SipPreference(Context c, SipProfile p) {
            super(c);
            setProfile(p);
        }

        void setProfile(SipProfile p) {
            this.mProfile = p;
            setTitle(SipSettings.this.getProfileName(p));
            updateSummary(SipSettings.this.mSipSharedPreferences.isReceivingCallsEnabled() ? SipSettings.this.getString(R.string.registration_status_checking_status) : SipSettings.this.getString(R.string.registration_status_not_receiving));
        }

        void updateSummary(String registrationStatus) {
            String summary;
            int profileUid = this.mProfile.getCallingUid();
            if (profileUid > 0 && profileUid != SipSettings.this.mUid) {
                summary = SipSettings.this.getString(R.string.third_party_account_summary, new Object[]{SipSettings.this.getPackageNameFromUid(profileUid)});
            } else {
                summary = registrationStatus;
            }
            setSummary(summary);
        }
    }

    private String getPackageNameFromUid(int uid) {
        try {
            String[] pkgs = this.mPackageManager.getPackagesForUid(uid);
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(pkgs[0], 0);
            return ai.loadLabel(this.mPackageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            log("getPackageNameFromUid, cannot find name of uid: " + uid + ", exception: " + e);
            return "uid:" + uid;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSipManager = SipManager.newInstance(this);
        this.mSipSharedPreferences = new SipSharedPreferences(this);
        this.mProfileDb = new SipProfileDb(this);
        this.mPackageManager = getPackageManager();
        setContentView(R.layout.sip_settings_ui);
        addPreferencesFromResource(R.xml.sip_setting);
        this.mSipListContainer = (PreferenceCategory) findPreference("sip_account_list");
        updateProfilesStatus();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterForContextMenu(getListView());
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, final Intent intent) {
        if (resultCode == -1 || resultCode == 1) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (SipSettings.this.mProfile != null) {
                            SipSettings.this.deleteProfile(SipSettings.this.mProfile);
                        }
                        SipProfile profile = (SipProfile) intent.getParcelableExtra("sip_profile");
                        if (resultCode == -1) {
                            SipSettings.this.addProfile(profile);
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

    private String getProfileName(SipProfile profile) {
        String profileName = profile.getProfileName();
        if (TextUtils.isEmpty(profileName)) {
            return profile.getUserName() + "@" + profile.getSipDomain();
        }
        return profileName;
    }

    private void retrieveSipLists() {
        this.mSipPreferenceMap = new LinkedHashMap();
        this.mSipProfileList = this.mProfileDb.retrieveSipProfileList();
        processActiveProfilesFromSipService();
        Collections.sort(this.mSipProfileList, new Comparator<SipProfile>() {
            @Override
            public int compare(SipProfile p1, SipProfile p2) {
                return SipSettings.this.getProfileName(p1).compareTo(SipSettings.this.getProfileName(p2));
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
        if (this.mSipSharedPreferences.isReceivingCallsEnabled()) {
            for (SipProfile p : this.mSipProfileList) {
                if (this.mUid == p.getCallingUid()) {
                    try {
                        this.mSipManager.setRegistrationListener(p.getUriString(), createRegistrationListener());
                    } catch (SipException e) {
                        log("retrieveSipLists, cannot set registration listener: " + e);
                    }
                }
            }
        }
    }

    private void processActiveProfilesFromSipService() {
        SipProfile[] activeList = this.mSipManager.getListOfProfiles();
        for (SipProfile activeProfile : activeList) {
            SipProfile profile = getProfileFromList(activeProfile);
            if (profile == null) {
                this.mSipProfileList.add(activeProfile);
            } else {
                profile.setCallingUid(activeProfile.getCallingUid());
            }
        }
    }

    private SipProfile getProfileFromList(SipProfile activeProfile) {
        for (SipProfile p : this.mSipProfileList) {
            if (p.getUriString().equals(activeProfile.getUriString())) {
                return p;
            }
        }
        return null;
    }

    private void addPreferenceFor(SipProfile p) {
        SipPreference pref = new SipPreference(this, p);
        this.mSipPreferenceMap.put(p.getUriString(), pref);
        this.mSipListContainer.addPreference(pref);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref2) {
                SipSettings.this.handleProfileClick(((SipPreference) pref2).mProfile);
                return true;
            }
        });
    }

    private void handleProfileClick(final SipProfile profile) {
        int uid = profile.getCallingUid();
        if (uid == this.mUid || uid == 0) {
            startSipEditor(profile);
        } else {
            new AlertDialog.Builder(this).setTitle(R.string.alert_dialog_close).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(R.string.close_profile, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int w) {
                    SipSettings.this.deleteProfile(profile);
                    SipSettings.this.unregisterProfile(profile);
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        }
    }

    private void unregisterProfile(final SipProfile p) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSettings.this.mSipManager.close(p.getUriString());
                } catch (Exception e) {
                    SipSettings.log("unregisterProfile, unregister failed, SipService died? Exception: " + e);
                }
            }
        }, "unregisterProfile").start();
    }

    void deleteProfile(SipProfile p) {
        this.mSipProfileList.remove(p);
        SipPreference pref = this.mSipPreferenceMap.remove(p.getUriString());
        this.mSipListContainer.removePreference(pref);
    }

    private void addProfile(SipProfile p) throws IOException {
        try {
            this.mSipManager.setRegistrationListener(p.getUriString(), createRegistrationListener());
        } catch (Exception e) {
            log("addProfile, cannot set registration listener: " + e);
        }
        this.mSipProfileList.add(p);
        addPreferenceFor(p);
    }

    private void startSipEditor(SipProfile profile) {
        this.mProfile = profile;
        Intent intent = new Intent(this, (Class<?>) SipEditor.class);
        intent.putExtra("sip_profile", (Parcelable) profile);
        startActivityForResult(intent, 1);
    }

    private void showRegistrationMessage(final String profileUri, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SipPreference pref = (SipPreference) SipSettings.this.mSipPreferenceMap.get(profileUri);
                if (pref != null) {
                    pref.updateSummary(message);
                }
            }
        });
    }

    private SipRegistrationListener createRegistrationListener() {
        return new SipRegistrationListener() {
            @Override
            public void onRegistrationDone(String profileUri, long expiryTime) {
                SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_done));
            }

            @Override
            public void onRegistering(String profileUri) {
                SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_registering));
            }

            @Override
            public void onRegistrationFailed(String profileUri, int errorCode, String message) {
                switch (errorCode) {
                    case -12:
                        SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_server_unreachable));
                        break;
                    case -11:
                    case -7:
                    case -6:
                    case -5:
                    default:
                        SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_failed_try_later, new Object[]{message}));
                        break;
                    case -10:
                        if (SipManager.isSipWifiOnly(SipSettings.this.getApplicationContext())) {
                            SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_no_wifi_data));
                        } else {
                            SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_no_data));
                        }
                        break;
                    case -9:
                        SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_still_trying));
                        break;
                    case -8:
                        SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_invalid_credentials));
                        break;
                    case -4:
                        SipSettings.this.showRegistrationMessage(profileUri, SipSettings.this.getString(R.string.registration_status_not_running));
                        break;
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, R.string.add_sip_account).setShowAsAction(1);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(1).setEnabled(SipUtil.isPhoneIdle(this));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case 1:
                startSipEditor(null);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static void log(String msg) {
        Log.d("SIP", "[SipSettings] " + msg);
    }
}
