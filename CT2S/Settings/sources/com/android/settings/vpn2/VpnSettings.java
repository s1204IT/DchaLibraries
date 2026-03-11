package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.security.Credentials;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VpnSettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, Handler.Callback, Preference.OnPreferenceClickListener {
    private VpnDialog mDialog;
    private LegacyVpnInfo mInfo;
    private String mSelectedKey;
    private UserManager mUm;
    private boolean mUnavailable;
    private Handler mUpdater;
    private final IConnectivityManager mService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private boolean mUnlocking = false;
    private HashMap<String, VpnPreference> mPreferences = new HashMap<>();

    @Override
    public void onCreate(Bundle savedState) {
        VpnProfile profile;
        super.onCreate(savedState);
        this.mUm = (UserManager) getSystemService("user");
        if (this.mUm.hasUserRestriction("no_config_vpn")) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.vpn_settings2);
        if (savedState != null && (profile = VpnProfile.decode(savedState.getString("VpnKey"), savedState.getByteArray("VpnProfile"))) != null) {
            this.mDialog = new VpnDialog(getActivity(), this, profile, savedState.getBoolean("VpnEditing"));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.vpn, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (SystemProperties.getBoolean("persist.radio.imsregrequired", false)) {
            menu.findItem(R.id.vpn_lockdown).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.vpn_create:
                long millis = System.currentTimeMillis();
                while (this.mPreferences.containsKey(Long.toHexString(millis))) {
                    millis++;
                }
                this.mDialog = new VpnDialog(getActivity(), this, new VpnProfile(Long.toHexString(millis)), true);
                this.mDialog.setOnDismissListener(this);
                this.mDialog.show();
                return true;
            case R.id.vpn_lockdown:
                LockdownConfigFragment.show(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        if (this.mDialog != null) {
            VpnProfile profile = this.mDialog.getProfile();
            savedState.putString("VpnKey", profile.key);
            savedState.putByteArray("VpnProfile", profile.encode());
            savedState.putBoolean("VpnEditing", this.mDialog.isEditing());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mUnavailable) {
            TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.vpn_settings_not_available);
                return;
            }
            return;
        }
        boolean pickLockdown = getActivity().getIntent().getBooleanExtra("android.net.vpn.PICK_LOCKDOWN", false);
        if (pickLockdown) {
            LockdownConfigFragment.show(this);
        }
        if (!this.mKeyStore.isUnlocked()) {
            if (!this.mUnlocking) {
                Credentials.getInstance().unlock(getActivity());
            } else {
                finishFragment();
            }
            this.mUnlocking = this.mUnlocking ? false : true;
            return;
        }
        this.mUnlocking = false;
        if (this.mPreferences.size() == 0) {
            PreferenceGroup group = getPreferenceScreen();
            Context context = getActivity();
            List<VpnProfile> profiles = loadVpnProfiles(this.mKeyStore, new int[0]);
            for (VpnProfile profile : profiles) {
                VpnPreference pref = new VpnPreference(context, profile);
                pref.setOnPreferenceClickListener(this);
                this.mPreferences.put(profile.key, pref);
                group.addPreference(pref);
            }
        }
        if (this.mDialog != null) {
            this.mDialog.setOnDismissListener(this);
            this.mDialog.show();
        }
        if (this.mUpdater == null) {
            this.mUpdater = new Handler(this);
        }
        this.mUpdater.sendEmptyMessage(0);
        registerForContextMenu(getListView());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.mUnavailable) {
            if (this.mDialog != null) {
                this.mDialog.setOnDismissListener(null);
                this.mDialog.dismiss();
            }
            if (getView() != null) {
                unregisterForContextMenu(getListView());
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.mDialog = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        if (button == -1) {
            VpnProfile profile = this.mDialog.getProfile();
            this.mKeyStore.put("VPN_" + profile.key, profile.encode(), -1, 1);
            VpnPreference preference = this.mPreferences.get(profile.key);
            if (preference != null) {
                disconnect(profile.key);
                preference.update(profile);
            } else {
                VpnPreference preference2 = new VpnPreference(getActivity(), profile);
                preference2.setOnPreferenceClickListener(this);
                this.mPreferences.put(profile.key, preference2);
                getPreferenceScreen().addPreference(preference2);
            }
            if (!this.mDialog.isEditing()) {
                try {
                    connect(profile);
                } catch (Exception e) {
                    Log.e("VpnSettings", "connect", e);
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        if (this.mDialog != null) {
            Log.v("VpnSettings", "onCreateContextMenu() is called when mDialog != null");
            return;
        }
        if (info instanceof AdapterView.AdapterContextMenuInfo) {
            Preference preference = (Preference) getListView().getItemAtPosition(((AdapterView.AdapterContextMenuInfo) info).position);
            if (preference instanceof VpnPreference) {
                VpnProfile profile = ((VpnPreference) preference).getProfile();
                this.mSelectedKey = profile.key;
                menu.setHeaderTitle(profile.name);
                menu.add(0, R.string.vpn_menu_edit, 0, R.string.vpn_menu_edit);
                menu.add(0, R.string.vpn_menu_delete, 0, R.string.vpn_menu_delete);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (this.mDialog != null) {
            Log.v("VpnSettings", "onContextItemSelected() is called when mDialog != null");
            return false;
        }
        VpnPreference preference = this.mPreferences.get(this.mSelectedKey);
        if (preference == null) {
            Log.v("VpnSettings", "onContextItemSelected() is called but no preference is found");
            return false;
        }
        switch (item.getItemId()) {
            case R.string.vpn_menu_edit:
                this.mDialog = new VpnDialog(getActivity(), this, preference.getProfile(), true);
                this.mDialog.setOnDismissListener(this);
                this.mDialog.show();
                break;
            case R.string.vpn_menu_delete:
                disconnect(this.mSelectedKey);
                getPreferenceScreen().removePreference(preference);
                this.mPreferences.remove(this.mSelectedKey);
                this.mKeyStore.delete("VPN_" + this.mSelectedKey);
                break;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (this.mDialog != null) {
            Log.v("VpnSettings", "onPreferenceClick() is called when mDialog != null");
        } else {
            if (preference instanceof VpnPreference) {
                VpnProfile profile = ((VpnPreference) preference).getProfile();
                if (this.mInfo != null && profile.key.equals(this.mInfo.key) && this.mInfo.state == 3) {
                    try {
                        this.mInfo.intent.send();
                    } catch (Exception e) {
                        this.mDialog = new VpnDialog(getActivity(), this, profile, false);
                        this.mDialog.setOnDismissListener(this);
                        this.mDialog.show();
                    }
                }
                this.mDialog = new VpnDialog(getActivity(), this, profile, false);
            } else {
                long millis = System.currentTimeMillis();
                while (this.mPreferences.containsKey(Long.toHexString(millis))) {
                    millis++;
                }
                this.mDialog = new VpnDialog(getActivity(), this, new VpnProfile(Long.toHexString(millis)), true);
            }
            this.mDialog.setOnDismissListener(this);
            this.mDialog.show();
        }
        return true;
    }

    @Override
    public boolean handleMessage(Message message) {
        VpnPreference preference;
        this.mUpdater.removeMessages(0);
        if (isResumed()) {
            try {
                LegacyVpnInfo info = this.mService.getLegacyVpnInfo();
                if (this.mInfo != null) {
                    VpnPreference preference2 = this.mPreferences.get(this.mInfo.key);
                    if (preference2 != null) {
                        preference2.update(-1);
                    }
                    this.mInfo = null;
                }
                if (info != null && (preference = this.mPreferences.get(info.key)) != null) {
                    preference.update(info.state);
                    this.mInfo = info;
                }
            } catch (Exception e) {
            }
            this.mUpdater.sendEmptyMessageDelayed(0, 1000L);
            return true;
        }
        return true;
    }

    private void connect(VpnProfile profile) throws Exception {
        try {
            this.mService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, 1).show();
        }
    }

    private void disconnect(String key) {
        if (this.mInfo != null && key.equals(this.mInfo.key)) {
            try {
                this.mService.prepareVpn("[Legacy VPN]", "[Legacy VPN]");
            } catch (Exception e) {
            }
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_vpn;
    }

    private static class VpnPreference extends Preference {
        private VpnProfile mProfile;
        private int mState;

        VpnPreference(Context context, VpnProfile profile) {
            super(context);
            this.mState = -1;
            setPersistent(false);
            setOrder(0);
            this.mProfile = profile;
            update();
        }

        VpnProfile getProfile() {
            return this.mProfile;
        }

        void update(VpnProfile profile) {
            this.mProfile = profile;
            update();
        }

        void update(int state) {
            this.mState = state;
            update();
        }

        void update() {
            if (this.mState < 0) {
                String[] types = getContext().getResources().getStringArray(R.array.vpn_types_long);
                setSummary(types[this.mProfile.type]);
            } else {
                String[] states = getContext().getResources().getStringArray(R.array.vpn_states);
                setSummary(states[this.mState]);
            }
            setTitle(this.mProfile.name);
            notifyHierarchyChanged();
        }

        @Override
        public int compareTo(Preference preference) {
            if (!(preference instanceof VpnPreference)) {
                return -1;
            }
            VpnPreference another = (VpnPreference) preference;
            int result = another.mState - this.mState;
            if (result != 0) {
                return result;
            }
            int result2 = this.mProfile.name.compareTo(another.mProfile.name);
            if (result2 != 0) {
                return result2;
            }
            int result3 = this.mProfile.type - another.mProfile.type;
            if (result3 == 0) {
                return this.mProfile.key.compareTo(another.mProfile.key);
            }
            return result3;
        }
    }

    public static class LockdownConfigFragment extends DialogFragment {
        private int mCurrentIndex;
        private List<VpnProfile> mProfiles;
        private List<CharSequence> mTitles;

        private static class TitleAdapter extends ArrayAdapter<CharSequence> {
            public TitleAdapter(Context context, List<CharSequence> objects) {
                super(context, android.R.layout.notification_2025_template_expanded_inbox, android.R.id.text1, objects);
            }
        }

        public static void show(VpnSettings parent) {
            if (parent.isAdded()) {
                LockdownConfigFragment dialog = new LockdownConfigFragment();
                dialog.show(parent.getFragmentManager(), "lockdown");
            }
        }

        private static String getStringOrNull(KeyStore keyStore, String key) {
            byte[] value = keyStore.get("LOCKDOWN_VPN");
            if (value == null) {
                return null;
            }
            return new String(value);
        }

        private void initProfiles(KeyStore keyStore, Resources res) {
            String lockdownKey = getStringOrNull(keyStore, "LOCKDOWN_VPN");
            this.mProfiles = VpnSettings.loadVpnProfiles(keyStore, 0);
            this.mTitles = Lists.newArrayList();
            this.mTitles.add(res.getText(R.string.vpn_lockdown_none));
            this.mCurrentIndex = 0;
            for (VpnProfile profile : this.mProfiles) {
                if (TextUtils.equals(profile.key, lockdownKey)) {
                    this.mCurrentIndex = this.mTitles.size();
                }
                this.mTitles.add(profile.name);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final KeyStore keyStore = KeyStore.getInstance();
            initProfiles(keyStore, context.getResources());
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
            builder.setTitle(R.string.vpn_menu_lockdown);
            View view = dialogInflater.inflate(R.layout.vpn_lockdown_editor, (ViewGroup) null, false);
            final ListView listView = (ListView) view.findViewById(android.R.id.list);
            listView.setChoiceMode(1);
            listView.setAdapter((ListAdapter) new TitleAdapter(context, this.mTitles));
            listView.setItemChecked(this.mCurrentIndex, true);
            builder.setView(view);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int newIndex = listView.getCheckedItemPosition();
                    if (LockdownConfigFragment.this.mCurrentIndex != newIndex) {
                        if (newIndex != 0) {
                            VpnProfile profile = (VpnProfile) LockdownConfigFragment.this.mProfiles.get(newIndex - 1);
                            if (!profile.isValidLockdownProfile()) {
                                Toast.makeText(context, R.string.vpn_lockdown_config_error, 1).show();
                                return;
                            }
                            keyStore.put("LOCKDOWN_VPN", profile.key.getBytes(), -1, 1);
                        } else {
                            keyStore.delete("LOCKDOWN_VPN");
                        }
                        ConnectivityManager.from(LockdownConfigFragment.this.getActivity()).updateLockdownVpn();
                    }
                }
            });
            return builder.create();
        }
    }

    public static List<VpnProfile> loadVpnProfiles(KeyStore keyStore, int... excludeTypes) {
        ArrayList<VpnProfile> result = Lists.newArrayList();
        String[] keys = keyStore.saw("VPN_");
        if (keys != null) {
            for (String key : keys) {
                VpnProfile profile = VpnProfile.decode(key, keyStore.get("VPN_" + key));
                if (profile != null && !ArrayUtils.contains(excludeTypes, profile.type)) {
                    result.add(profile);
                }
            }
        }
        return result;
    }
}
