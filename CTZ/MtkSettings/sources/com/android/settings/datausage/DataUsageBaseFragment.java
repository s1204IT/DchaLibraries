package com.android.settings.datausage;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.datausage.TemplatePreference;
import com.android.settingslib.NetworkPolicyEditor;
import com.mediatek.settings.sim.SimHotSwapHandler;

public abstract class DataUsageBaseFragment extends DashboardFragment {
    private SimHotSwapHandler mSimHotSwapHandler;
    protected final TemplatePreference.NetworkServices services = new TemplatePreference.NetworkServices();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Context context = getContext();
        this.services.mNetworkService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.services.mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
        this.services.mPolicyManager = (NetworkPolicyManager) context.getSystemService("netpolicy");
        this.services.mPolicyEditor = new NetworkPolicyEditor(this.services.mPolicyManager);
        this.services.mTelephonyManager = TelephonyManager.from(context);
        this.services.mSubscriptionManager = SubscriptionManager.from(context);
        this.services.mUserManager = UserManager.get(context);
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                if (DataUsageBaseFragment.this.getActivity() != null) {
                    Log.d("DataUsageBase", "onSimHotSwap, finish Activity~~");
                    DataUsageBaseFragment.this.getActivity().finish();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        this.services.mPolicyEditor.read();
    }

    protected boolean isAdmin() {
        return this.services.mUserManager.isAdminUser();
    }

    public boolean hasEthernet(Context context) {
        long totalBytes;
        boolean zIsNetworkSupported = ConnectivityManager.from(context).isNetworkSupported(9);
        try {
            INetworkStatsSession iNetworkStatsSessionOpenSession = this.services.mStatsService.openSession();
            if (iNetworkStatsSessionOpenSession != null) {
                totalBytes = iNetworkStatsSessionOpenSession.getSummaryForNetwork(NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE).getTotalBytes();
                TrafficStats.closeQuietly(iNetworkStatsSessionOpenSession);
            } else {
                totalBytes = 0;
            }
            return zIsNetworkSupported && totalBytes > 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
    }
}
