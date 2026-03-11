package com.android.settings.vpn2;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.KeyStore;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.settings.GearPreference;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VpnSettings extends RestrictedSettingsFragment implements Handler.Callback, Preference.OnPreferenceClickListener {
    private static final NetworkRequest VPN_REQUEST = new NetworkRequest.Builder().removeCapability(15).removeCapability(13).removeCapability(14).build();
    private Map<AppVpnInfo, AppPreference> mAppPreferences;
    private LegacyVpnInfo mConnectedLegacyVpn;
    private ConnectivityManager mConnectivityManager;
    private final IConnectivityManager mConnectivityService;
    private GearPreference.OnGearClickListener mGearListener;
    private final KeyStore mKeyStore;
    private Map<String, LegacyVpnPreference> mLegacyVpnPreferences;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private boolean mUnavailable;
    private Handler mUpdater;
    private UserManager mUserManager;

    public VpnSettings() {
        super("no_config_vpn");
        this.mConnectivityService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
        this.mKeyStore = KeyStore.getInstance();
        this.mLegacyVpnPreferences = new ArrayMap();
        this.mAppPreferences = new ArrayMap();
        this.mGearListener = new GearPreference.OnGearClickListener() {
            @Override
            public void onGearClick(GearPreference p) {
                if (p instanceof LegacyVpnPreference) {
                    LegacyVpnPreference pref = (LegacyVpnPreference) p;
                    ConfigDialogFragment.show(VpnSettings.this, pref.getProfile(), true, true);
                } else {
                    if (!(p instanceof AppPreference)) {
                        return;
                    }
                    AppPreference pref2 = (AppPreference) p;
                    AppManagementFragment.show(VpnSettings.this.getPrefContext(), pref2);
                }
            }
        };
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (VpnSettings.this.mUpdater == null) {
                    return;
                }
                VpnSettings.this.mUpdater.sendEmptyMessage(0);
            }

            @Override
            public void onLost(Network network) {
                if (VpnSettings.this.mUpdater == null) {
                    return;
                }
                VpnSettings.this.mUpdater.sendEmptyMessage(0);
            }
        };
    }

    @Override
    protected int getMetricsCategory() {
        return 100;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mUserManager = (UserManager) getSystemService("user");
        this.mConnectivityManager = (ConnectivityManager) getSystemService("connectivity");
        this.mUnavailable = isUiRestricted();
        setHasOptionsMenu(!this.mUnavailable);
        addPreferencesFromResource(R.xml.vpn_settings2);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.vpn, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (int i = 0; i < menu.size(); i++) {
            if (isUiRestrictedByOnlyAdmin()) {
                RestrictedLockUtils.setMenuItemAsDisabledByAdmin(getPrefContext(), menu.getItem(i), getRestrictionEnforcedAdmin());
            } else {
                menu.getItem(i).setEnabled(!this.mUnavailable);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.vpn_create:
                long millis = System.currentTimeMillis();
                while (this.mLegacyVpnPreferences.containsKey(Long.toHexString(millis))) {
                    millis++;
                }
                VpnProfile profile = new VpnProfile(Long.toHexString(millis));
                ConfigDialogFragment.show(this, profile, true, false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.vpn_settings_not_available);
            }
            getPreferenceScreen().removeAll();
        } else {
            getEmptyTextView().setText(R.string.vpn_no_vpns_added);
            this.mConnectivityManager.registerNetworkCallback(VPN_REQUEST, this.mNetworkCallback);
            if (this.mUpdater == null) {
                this.mUpdater = new Handler(this);
            }
            this.mUpdater.sendEmptyMessage(0);
        }
    }

    @Override
    public void onPause() {
        Log.d("VpnSettings", "onPause, mUnavailable:" + this.mUnavailable + " mUpdater!=null:" + (this.mUpdater != null));
        if (this.mUnavailable) {
            super.onPause();
            return;
        }
        this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
        if (this.mUpdater != null) {
            Log.d("VpnSettings", "removeCallbacksAndMessages");
            this.mUpdater.removeCallbacksAndMessages(null);
        }
        super.onPause();
    }

    @Override
    public boolean handleMessage(Message message) {
        this.mUpdater.removeMessages(0);
        if (getActivity() == null) {
            Log.d("VpnSettings", "getActivity is null");
            return true;
        }
        final List<VpnProfile> vpnProfiles = loadVpnProfiles(this.mKeyStore, new int[0]);
        final List<AppVpnInfo> vpnApps = getVpnApps(getActivity(), true);
        final Map<String, LegacyVpnInfo> connectedLegacyVpns = getConnectedLegacyVpns();
        final Set<AppVpnInfo> connectedAppVpns = getConnectedAppVpns();
        final Set<AppVpnInfo> alwaysOnAppVpnInfos = getAlwaysOnAppVpnInfos();
        final String lockdownVpnKey = VpnUtils.getLockdownVpn();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!VpnSettings.this.isAdded()) {
                    return;
                }
                Set<Preference> updates = new ArraySet<>();
                for (VpnProfile profile : vpnProfiles) {
                    LegacyVpnPreference p = VpnSettings.this.findOrCreatePreference(profile);
                    if (connectedLegacyVpns.containsKey(profile.key)) {
                        p.setState(((LegacyVpnInfo) connectedLegacyVpns.get(profile.key)).state);
                    } else {
                        p.setState(LegacyVpnPreference.STATE_NONE);
                    }
                    p.setAlwaysOn(lockdownVpnKey != null ? lockdownVpnKey.equals(profile.key) : false);
                    updates.add(p);
                }
                for (AppVpnInfo app : vpnApps) {
                    AppPreference p2 = VpnSettings.this.findOrCreatePreference(app);
                    if (connectedAppVpns.contains(app)) {
                        p2.setState(3);
                    } else {
                        p2.setState(AppPreference.STATE_DISCONNECTED);
                    }
                    p2.setAlwaysOn(alwaysOnAppVpnInfos.contains(app));
                    updates.add(p2);
                }
                VpnSettings.this.mLegacyVpnPreferences.values().retainAll(updates);
                VpnSettings.this.mAppPreferences.values().retainAll(updates);
                PreferenceGroup vpnGroup = VpnSettings.this.getPreferenceScreen();
                for (int i = vpnGroup.getPreferenceCount() - 1; i >= 0; i--) {
                    Preference p3 = vpnGroup.getPreference(i);
                    if (updates.contains(p3)) {
                        updates.remove(p3);
                    } else {
                        vpnGroup.removePreference(p3);
                    }
                }
                for (Preference pref : updates) {
                    vpnGroup.addPreference(pref);
                }
            }
        });
        this.mUpdater.sendEmptyMessageDelayed(0, 1000L);
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof LegacyVpnPreference) {
            VpnProfile profile = ((LegacyVpnPreference) preference).getProfile();
            if (this.mConnectedLegacyVpn != null && profile.key.equals(this.mConnectedLegacyVpn.key) && this.mConnectedLegacyVpn.state == 3) {
                try {
                    if (this.mConnectedLegacyVpn.intent == null) {
                        Toast.makeText(getActivity(), R.string.lockdown_vpn_already_connected, 1).show();
                        return true;
                    }
                    this.mConnectedLegacyVpn.intent.send();
                    return true;
                } catch (Exception e) {
                    Log.w("VpnSettings", "Starting config intent failed", e);
                }
            }
            ConfigDialogFragment.show(this, profile, false, true);
            return true;
        }
        if (preference instanceof AppPreference) {
            AppPreference pref = (AppPreference) preference;
            boolean connected = pref.getState() == 3;
            if (!connected) {
                try {
                    UserHandle user = UserHandle.of(pref.getUserId());
                    Context userContext = getActivity().createPackageContextAsUser(getActivity().getPackageName(), 0, user);
                    PackageManager pm = userContext.getPackageManager();
                    Intent appIntent = pm.getLaunchIntentForPackage(pref.getPackageName());
                    if (appIntent != null) {
                        userContext.startActivityAsUser(appIntent, user);
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException nnfe) {
                    Log.w("VpnSettings", "VPN provider does not exist: " + pref.getPackageName(), nnfe);
                }
            }
            PackageInfo pkgInfo = pref.getPackageInfo();
            AppDialogFragment.show(this, pkgInfo, pref.getLabel(), false, connected);
            return true;
        }
        return false;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_vpn;
    }

    public LegacyVpnPreference findOrCreatePreference(VpnProfile profile) {
        LegacyVpnPreference pref = this.mLegacyVpnPreferences.get(profile.key);
        if (pref == null) {
            pref = new LegacyVpnPreference(getPrefContext());
            pref.setOnGearClickListener(this.mGearListener);
            pref.setOnPreferenceClickListener(this);
            this.mLegacyVpnPreferences.put(profile.key, pref);
        }
        pref.setProfile(profile);
        return pref;
    }

    public AppPreference findOrCreatePreference(AppVpnInfo app) {
        AppPreference pref = this.mAppPreferences.get(app);
        if (pref == null) {
            AppPreference pref2 = new AppPreference(getPrefContext(), app.userId, app.packageName);
            pref2.setOnGearClickListener(this.mGearListener);
            pref2.setOnPreferenceClickListener(this);
            this.mAppPreferences.put(app, pref2);
            return pref2;
        }
        return pref;
    }

    private Map<String, LegacyVpnInfo> getConnectedLegacyVpns() {
        try {
            this.mConnectedLegacyVpn = this.mConnectivityService.getLegacyVpnInfo(UserHandle.myUserId());
            if (this.mConnectedLegacyVpn != null) {
                return Collections.singletonMap(this.mConnectedLegacyVpn.key, this.mConnectedLegacyVpn);
            }
        } catch (RemoteException e) {
            Log.e("VpnSettings", "Failure updating VPN list with connected legacy VPNs", e);
        }
        return Collections.emptyMap();
    }

    private Set<AppVpnInfo> getConnectedAppVpns() {
        Set<AppVpnInfo> connections = new ArraySet<>();
        try {
            for (UserHandle profile : this.mUserManager.getUserProfiles()) {
                VpnConfig config = this.mConnectivityService.getVpnConfig(profile.getIdentifier());
                if (config != null && !config.legacy) {
                    connections.add(new AppVpnInfo(profile.getIdentifier(), config.user));
                }
            }
        } catch (RemoteException e) {
            Log.e("VpnSettings", "Failure updating VPN list with connected app VPNs", e);
        }
        return connections;
    }

    private Set<AppVpnInfo> getAlwaysOnAppVpnInfos() {
        Set<AppVpnInfo> result = new ArraySet<>();
        for (UserHandle profile : this.mUserManager.getUserProfiles()) {
            int profileId = profile.getIdentifier();
            String packageName = this.mConnectivityManager.getAlwaysOnVpnPackageForUser(profileId);
            if (packageName != null) {
                result.add(new AppVpnInfo(profileId, packageName));
            }
        }
        return result;
    }

    static List<AppVpnInfo> getVpnApps(Context context, boolean includeProfiles) {
        Set<Integer> profileIds;
        List<AppVpnInfo> result = Lists.newArrayList();
        if (includeProfiles) {
            profileIds = new ArraySet<>();
            for (UserHandle profile : UserManager.get(context).getUserProfiles()) {
                profileIds.add(Integer.valueOf(profile.getIdentifier()));
            }
        } else {
            profileIds = Collections.singleton(Integer.valueOf(UserHandle.myUserId()));
        }
        AppOpsManager aom = (AppOpsManager) context.getSystemService("appops");
        List<AppOpsManager.PackageOps> apps = aom.getPackagesForOps(new int[]{47});
        if (apps != null) {
            for (AppOpsManager.PackageOps pkg : apps) {
                int userId = UserHandle.getUserId(pkg.getUid());
                if (profileIds.contains(Integer.valueOf(userId))) {
                    boolean allowed = false;
                    for (AppOpsManager.OpEntry op : pkg.getOps()) {
                        if (op.getOp() == 47 && op.getMode() == 0) {
                            allowed = true;
                        }
                    }
                    if (allowed) {
                        result.add(new AppVpnInfo(userId, pkg.getPackageName()));
                    }
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    static List<VpnProfile> loadVpnProfiles(KeyStore keyStore, int... excludeTypes) {
        ArrayList<VpnProfile> result = Lists.newArrayList();
        for (String key : keyStore.list("VPN_")) {
            VpnProfile profile = VpnProfile.decode(key, keyStore.get("VPN_" + key));
            if (profile != null && !ArrayUtils.contains(excludeTypes, profile.type)) {
                result.add(profile);
            }
        }
        return result;
    }
}
