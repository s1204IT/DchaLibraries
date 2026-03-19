package com.android.settings.datausage;

import android.content.Context;
import android.net.INetworkPolicyListener;
import android.net.NetworkPolicyManager;
import android.os.RemoteException;
import android.util.Pair;
import android.util.SparseIntArray;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;
import java.util.ArrayList;

public class DataSaverBackend {
    private boolean mBlacklistInitialized;
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final NetworkPolicyManager mPolicyManager;
    private boolean mWhitelistInitialized;
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private SparseIntArray mUidPolicies = new SparseIntArray();
    private final INetworkPolicyListener mPolicyListener = new AnonymousClass1();

    public interface Listener {
        void onBlacklistStatusChanged(int i, boolean z);

        void onDataSaverChanged(boolean z);

        void onWhitelistStatusChanged(int i, boolean z);
    }

    public DataSaverBackend(Context context) {
        this.mContext = context;
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        this.mPolicyManager = NetworkPolicyManager.from(context);
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
        if (this.mListeners.size() == 1) {
            this.mPolicyManager.registerListener(this.mPolicyListener);
        }
        listener.onDataSaverChanged(isDataSaverEnabled());
    }

    public void remListener(Listener listener) {
        this.mListeners.remove(listener);
        if (this.mListeners.size() == 0) {
            this.mPolicyManager.unregisterListener(this.mPolicyListener);
        }
    }

    public boolean isDataSaverEnabled() {
        return this.mPolicyManager.getRestrictBackground();
    }

    public void setDataSaverEnabled(boolean z) {
        this.mPolicyManager.setRestrictBackground(z);
        this.mMetricsFeatureProvider.action(this.mContext, 394, z ? 1 : 0);
    }

    public void refreshWhitelist() {
        for (int size = this.mUidPolicies.size() - 1; size >= 0; size--) {
            if (this.mUidPolicies.valueAt(size) == 4) {
                this.mUidPolicies.removeAt(size);
            }
        }
        this.mWhitelistInitialized = false;
        loadWhitelist();
    }

    public void setIsWhitelisted(int i, String str, boolean z) {
        int i2 = z ? 4 : 0;
        this.mPolicyManager.setUidPolicy(i, i2);
        this.mUidPolicies.put(i, i2);
        if (z) {
            this.mMetricsFeatureProvider.action(this.mContext, 395, str, new Pair[0]);
        }
    }

    public boolean isWhitelisted(int i) {
        loadWhitelist();
        return this.mUidPolicies.get(i, 0) == 4;
    }

    public int getWhitelistedCount() {
        loadWhitelist();
        int i = 0;
        for (int i2 = 0; i2 < this.mUidPolicies.size(); i2++) {
            if (this.mUidPolicies.valueAt(i2) == 4) {
                i++;
            }
        }
        return i;
    }

    private void loadWhitelist() {
        if (this.mWhitelistInitialized) {
            return;
        }
        for (int i : this.mPolicyManager.getUidsWithPolicy(4)) {
            this.mUidPolicies.put(i, 4);
        }
        this.mWhitelistInitialized = true;
    }

    public void refreshBlacklist() {
        for (int size = this.mUidPolicies.size() - 1; size >= 0; size--) {
            if (this.mUidPolicies.valueAt(size) == 1) {
                this.mUidPolicies.removeAt(size);
            }
        }
        this.mBlacklistInitialized = false;
        loadBlacklist();
    }

    public void setIsBlacklisted(int i, String str, boolean z) {
        this.mPolicyManager.setUidPolicy(i, z ? 1 : 0);
        this.mUidPolicies.put(i, z ? 1 : 0);
        if (z) {
            this.mMetricsFeatureProvider.action(this.mContext, 396, str, new Pair[0]);
        }
    }

    public boolean isBlacklisted(int i) {
        loadBlacklist();
        return this.mUidPolicies.get(i, 0) == 1;
    }

    private void loadBlacklist() {
        if (this.mBlacklistInitialized) {
            return;
        }
        for (int i : this.mPolicyManager.getUidsWithPolicy(1)) {
            this.mUidPolicies.put(i, 1);
        }
        this.mBlacklistInitialized = true;
    }

    private void handleRestrictBackgroundChanged(boolean z) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            this.mListeners.get(i).onDataSaverChanged(z);
        }
    }

    private void handleWhitelistChanged(int i, boolean z) {
        for (int i2 = 0; i2 < this.mListeners.size(); i2++) {
            this.mListeners.get(i2).onWhitelistStatusChanged(i, z);
        }
    }

    private void handleBlacklistChanged(int i, boolean z) {
        for (int i2 = 0; i2 < this.mListeners.size(); i2++) {
            this.mListeners.get(i2).onBlacklistStatusChanged(i, z);
        }
    }

    private void handleUidPoliciesChanged(int i, int i2) {
        loadWhitelist();
        loadBlacklist();
        int i3 = this.mUidPolicies.get(i, 0);
        if (i2 == 0) {
            this.mUidPolicies.delete(i);
        } else {
            this.mUidPolicies.put(i, i2);
        }
        boolean z = i3 == 4;
        boolean z2 = i3 == 1;
        boolean z3 = i2 == 4;
        boolean z4 = i2 == 1;
        if (z != z3) {
            handleWhitelistChanged(i, z3);
        }
        if (z2 != z4) {
            handleBlacklistChanged(i, z4);
        }
    }

    class AnonymousClass1 extends INetworkPolicyListener.Stub {
        AnonymousClass1() {
        }

        public void onUidRulesChanged(int i, int i2) throws RemoteException {
        }

        public void onUidPoliciesChanged(final int i, final int i2) {
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    DataSaverBackend.this.handleUidPoliciesChanged(i, i2);
                }
            });
        }

        public void onMeteredIfacesChanged(String[] strArr) throws RemoteException {
        }

        public void onRestrictBackgroundChanged(final boolean z) throws RemoteException {
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    DataSaverBackend.this.handleRestrictBackgroundChanged(z);
                }
            });
        }

        public void onSubscriptionOverride(int i, int i2, int i3) {
        }
    }
}
