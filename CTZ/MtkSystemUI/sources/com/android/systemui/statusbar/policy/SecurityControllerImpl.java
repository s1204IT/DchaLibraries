package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.KeyChain;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SecurityControllerImpl.CACertLoader;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class SecurityControllerImpl extends CurrentUserTracker implements SecurityController {
    private static final boolean DEBUG = Log.isLoggable("SecurityController", 3);
    private static final NetworkRequest REQUEST = new NetworkRequest.Builder().removeCapability(15).removeCapability(13).removeCapability(14).setUids(null).build();
    private final BroadcastReceiver mBroadcastReceiver;

    @GuardedBy("mCallbacks")
    private final ArrayList<SecurityController.SecurityControllerCallback> mCallbacks;
    private final ConnectivityManager mConnectivityManager;
    private final IConnectivityManager mConnectivityManagerService;
    private final Context mContext;
    private int mCurrentUserId;
    private SparseArray<VpnConfig> mCurrentVpns;
    private final DevicePolicyManager mDevicePolicyManager;
    private ArrayMap<Integer, Boolean> mHasCACerts;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private int mVpnUserId;

    public SecurityControllerImpl(Context context) {
        this(context, null);
    }

    public SecurityControllerImpl(Context context, SecurityController.SecurityControllerCallback securityControllerCallback) {
        super(context);
        this.mCallbacks = new ArrayList<>();
        this.mCurrentVpns = new SparseArray<>();
        this.mHasCACerts = new ArrayMap<>();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (SecurityControllerImpl.DEBUG) {
                    Log.d("SecurityController", "onAvailable " + network.netId);
                }
                SecurityControllerImpl.this.updateState();
                SecurityControllerImpl.this.fireCallbacks();
            }

            @Override
            public void onLost(Network network) {
                if (SecurityControllerImpl.DEBUG) {
                    Log.d("SecurityController", "onLost " + network.netId);
                }
                SecurityControllerImpl.this.updateState();
                SecurityControllerImpl.this.fireCallbacks();
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.security.action.TRUST_STORE_CHANGED".equals(intent.getAction())) {
                    SecurityControllerImpl.this.refreshCACerts();
                }
            }
        };
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mConnectivityManagerService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
        addCallback(securityControllerCallback);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.security.action.TRUST_STORE_CHANGED");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)));
        this.mConnectivityManager.registerNetworkCallback(REQUEST, this.mNetworkCallback);
        onUserSwitched(ActivityManager.getCurrentUser());
        startTracking();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("SecurityController state:");
        printWriter.print("  mCurrentVpns={");
        for (int i = 0; i < this.mCurrentVpns.size(); i++) {
            if (i > 0) {
                printWriter.print(", ");
            }
            printWriter.print(this.mCurrentVpns.keyAt(i));
            printWriter.print('=');
            printWriter.print(this.mCurrentVpns.valueAt(i).user);
        }
        printWriter.println("}");
    }

    @Override
    public boolean isDeviceManaged() {
        return this.mDevicePolicyManager.isDeviceManaged();
    }

    @Override
    public CharSequence getDeviceOwnerOrganizationName() {
        return this.mDevicePolicyManager.getDeviceOwnerOrganizationName();
    }

    @Override
    public CharSequence getWorkProfileOrganizationName() {
        int workProfileUserId = getWorkProfileUserId(this.mCurrentUserId);
        if (workProfileUserId == -10000) {
            return null;
        }
        return this.mDevicePolicyManager.getOrganizationNameForUser(workProfileUserId);
    }

    @Override
    public String getPrimaryVpnName() {
        VpnConfig vpnConfig = this.mCurrentVpns.get(this.mVpnUserId);
        if (vpnConfig != null) {
            return getNameForVpnConfig(vpnConfig, new UserHandle(this.mVpnUserId));
        }
        return null;
    }

    private int getWorkProfileUserId(int i) {
        for (UserInfo userInfo : this.mUserManager.getProfiles(i)) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return -10000;
    }

    @Override
    public boolean hasWorkProfile() {
        return getWorkProfileUserId(this.mCurrentUserId) != -10000;
    }

    @Override
    public String getWorkProfileVpnName() {
        VpnConfig vpnConfig;
        int workProfileUserId = getWorkProfileUserId(this.mVpnUserId);
        if (workProfileUserId == -10000 || (vpnConfig = this.mCurrentVpns.get(workProfileUserId)) == null) {
            return null;
        }
        return getNameForVpnConfig(vpnConfig, UserHandle.of(workProfileUserId));
    }

    @Override
    public boolean isNetworkLoggingEnabled() {
        return this.mDevicePolicyManager.isNetworkLoggingEnabled(null);
    }

    @Override
    public boolean isVpnEnabled() {
        for (int i : this.mUserManager.getProfileIdsWithDisabled(this.mVpnUserId)) {
            if (this.mCurrentVpns.get(i) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isVpnBranded() {
        String packageNameForVpnConfig;
        VpnConfig vpnConfig = this.mCurrentVpns.get(this.mVpnUserId);
        if (vpnConfig == null || (packageNameForVpnConfig = getPackageNameForVpnConfig(vpnConfig)) == null) {
            return false;
        }
        return isVpnPackageBranded(packageNameForVpnConfig);
    }

    @Override
    public boolean hasCACertInCurrentUser() {
        Boolean bool = this.mHasCACerts.get(Integer.valueOf(this.mCurrentUserId));
        return bool != null && bool.booleanValue();
    }

    @Override
    public boolean hasCACertInWorkProfile() {
        Boolean bool;
        int workProfileUserId = getWorkProfileUserId(this.mCurrentUserId);
        return (workProfileUserId == -10000 || (bool = this.mHasCACerts.get(Integer.valueOf(workProfileUserId))) == null || !bool.booleanValue()) ? false : true;
    }

    @Override
    public void removeCallback(SecurityController.SecurityControllerCallback securityControllerCallback) {
        synchronized (this.mCallbacks) {
            try {
                if (securityControllerCallback == null) {
                    return;
                }
                if (DEBUG) {
                    Log.d("SecurityController", "removeCallback " + securityControllerCallback);
                }
                this.mCallbacks.remove(securityControllerCallback);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    @Override
    public void addCallback(SecurityController.SecurityControllerCallback securityControllerCallback) {
        synchronized (this.mCallbacks) {
            if (securityControllerCallback != null) {
                try {
                    if (!this.mCallbacks.contains(securityControllerCallback)) {
                        if (DEBUG) {
                            Log.d("SecurityController", "addCallback " + securityControllerCallback);
                        }
                        this.mCallbacks.add(securityControllerCallback);
                    }
                } finally {
                }
            }
        }
    }

    @Override
    public void onUserSwitched(int i) {
        this.mCurrentUserId = i;
        UserInfo userInfo = this.mUserManager.getUserInfo(i);
        if (userInfo.isRestricted()) {
            this.mVpnUserId = userInfo.restrictedProfileParentId;
        } else {
            this.mVpnUserId = this.mCurrentUserId;
        }
        refreshCACerts();
        fireCallbacks();
    }

    public void refreshCACerts() {
        new CACertLoader().execute(Integer.valueOf(this.mCurrentUserId));
        int workProfileUserId = getWorkProfileUserId(this.mCurrentUserId);
        if (workProfileUserId != -10000) {
            new CACertLoader().execute(Integer.valueOf(workProfileUserId));
        }
    }

    private String getNameForVpnConfig(VpnConfig vpnConfig, UserHandle userHandle) {
        if (vpnConfig.legacy) {
            return this.mContext.getString(R.string.legacy_vpn_name);
        }
        String str = vpnConfig.user;
        try {
            return VpnConfig.getVpnLabel(this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle), str).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SecurityController", "Package " + str + " is not present", e);
            return null;
        }
    }

    public void fireCallbacks() {
        synchronized (this.mCallbacks) {
            Iterator<SecurityController.SecurityControllerCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onStateChanged();
            }
        }
    }

    public void updateState() {
        LegacyVpnInfo legacyVpnInfo;
        SparseArray<VpnConfig> sparseArray = new SparseArray<>();
        try {
            for (UserInfo userInfo : this.mUserManager.getUsers()) {
                VpnConfig vpnConfig = this.mConnectivityManagerService.getVpnConfig(userInfo.id);
                if (vpnConfig != null && (!vpnConfig.legacy || ((legacyVpnInfo = this.mConnectivityManagerService.getLegacyVpnInfo(userInfo.id)) != null && legacyVpnInfo.state == 3))) {
                    sparseArray.put(userInfo.id, vpnConfig);
                }
            }
            this.mCurrentVpns = sparseArray;
        } catch (RemoteException e) {
            Log.e("SecurityController", "Unable to list active VPNs", e);
        }
    }

    private String getPackageNameForVpnConfig(VpnConfig vpnConfig) {
        if (vpnConfig.legacy) {
            return null;
        }
        return vpnConfig.user;
    }

    private boolean isVpnPackageBranded(String str) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str, 128);
            if (applicationInfo != null && applicationInfo.metaData != null && applicationInfo.isSystemApp()) {
                return applicationInfo.metaData.getBoolean("com.android.systemui.IS_BRANDED", false);
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    protected class CACertLoader extends AsyncTask<Integer, Void, Pair<Integer, Boolean>> {
        protected CACertLoader() {
        }

        @Override
        public Pair<Integer, Boolean> doInBackground(final Integer... numArr) {
            Throwable th;
            try {
                KeyChain.KeyChainConnection keyChainConnectionBindAsUser = KeyChain.bindAsUser(SecurityControllerImpl.this.mContext, UserHandle.of(numArr[0].intValue()));
                try {
                    Pair<Integer, Boolean> pair = new Pair<>(numArr[0], Boolean.valueOf(!keyChainConnectionBindAsUser.getService().getUserCaAliases().getList().isEmpty()));
                    if (keyChainConnectionBindAsUser != null) {
                        keyChainConnectionBindAsUser.close();
                    }
                    return pair;
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    if (keyChainConnectionBindAsUser != null) {
                    }
                }
            } catch (RemoteException | AssertionError | InterruptedException e) {
                Log.i("SecurityController", e.getMessage());
                new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)).postDelayed(new Runnable() {
                    @Override
                    public final void run() {
                        SecurityControllerImpl.this.new CACertLoader().execute(numArr[0]);
                    }
                }, 30000L);
                return new Pair<>(numArr[0], null);
            }
        }

        @Override
        public void onPostExecute(Pair<Integer, Boolean> pair) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onPostExecute " + pair);
            }
            if (pair.second != null) {
                SecurityControllerImpl.this.mHasCACerts.put((Integer) pair.first, (Boolean) pair.second);
                SecurityControllerImpl.this.fireCallbacks();
            }
        }
    }
}
