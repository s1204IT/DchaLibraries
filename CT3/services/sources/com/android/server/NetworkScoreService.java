package com.android.server;

import android.R;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.INetworkScoreCache;
import android.net.INetworkScoreService;
import android.net.NetworkScorerAppManager;
import android.net.ScoredNetwork;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkScoreService extends INetworkScoreService.Stub {
    private static final boolean DBG = false;
    private static final String TAG = "NetworkScoreService";
    private final Context mContext;

    @GuardedBy("mPackageMonitorLock")
    private NetworkScorerPackageMonitor mPackageMonitor;
    private ScoringServiceConnection mServiceConnection;
    private final Object mPackageMonitorLock = new Object[0];
    private BroadcastReceiver mUserIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (userId == -10000 || !"android.intent.action.USER_UNLOCKED".equals(action)) {
                return;
            }
            NetworkScoreService.this.onUserUnlocked(userId);
        }
    };
    private final Map<Integer, INetworkScoreCache> mScoreCaches = new HashMap();

    private class NetworkScorerPackageMonitor extends PackageMonitor {
        final String mRegisteredPackage;

        NetworkScorerPackageMonitor(NetworkScoreService this$0, String mRegisteredPackage, NetworkScorerPackageMonitor networkScorerPackageMonitor) {
            this(mRegisteredPackage);
        }

        private NetworkScorerPackageMonitor(String mRegisteredPackage) {
            this.mRegisteredPackage = mRegisteredPackage;
        }

        public void onPackageAdded(String packageName, int uid) {
            evaluateBinding(packageName, true);
        }

        public void onPackageRemoved(String packageName, int uid) {
            evaluateBinding(packageName, true);
        }

        public void onPackageModified(String packageName) {
            evaluateBinding(packageName, false);
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            if (doit) {
                for (String packageName : packages) {
                    evaluateBinding(packageName, true);
                }
            }
            return super.onHandleForceStop(intent, packages, uid, doit);
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            evaluateBinding(packageName, true);
        }

        private void evaluateBinding(String scorerPackageName, boolean forceUnbind) {
            if (!this.mRegisteredPackage.equals(scorerPackageName)) {
                return;
            }
            NetworkScorerAppManager.NetworkScorerAppData activeScorer = NetworkScorerAppManager.getActiveScorer(NetworkScoreService.this.mContext);
            if (activeScorer == null) {
                Log.i(NetworkScoreService.TAG, "Package " + this.mRegisteredPackage + " is no longer valid, disabling scoring.");
                NetworkScoreService.this.setScorerInternal(null);
            } else {
                if (activeScorer.mScoringServiceClassName == null) {
                    NetworkScoreService.this.unbindFromScoringServiceIfNeeded();
                    return;
                }
                if (forceUnbind) {
                    NetworkScoreService.this.unbindFromScoringServiceIfNeeded();
                }
                NetworkScoreService.this.bindToScoringServiceIfNeeded(activeScorer);
            }
        }
    }

    public NetworkScoreService(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.SYSTEM, filter, null, null);
    }

    void systemReady() {
        ContentResolver cr = this.mContext.getContentResolver();
        if (Settings.Global.getInt(cr, "network_scoring_provisioned", 0) == 0) {
            String defaultPackage = this.mContext.getResources().getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_IN_PROGRESS);
            if (!TextUtils.isEmpty(defaultPackage)) {
                NetworkScorerAppManager.setActiveScorer(this.mContext, defaultPackage);
            }
            Settings.Global.putInt(cr, "network_scoring_provisioned", 1);
        }
        registerPackageMonitorIfNeeded();
    }

    void systemRunning() {
        bindToScoringServiceIfNeeded();
    }

    private void onUserUnlocked(int userId) {
        registerPackageMonitorIfNeeded();
        bindToScoringServiceIfNeeded();
    }

    private void registerPackageMonitorIfNeeded() {
        NetworkScorerAppManager.NetworkScorerAppData scorer = NetworkScorerAppManager.getActiveScorer(this.mContext);
        synchronized (this.mPackageMonitorLock) {
            if (this.mPackageMonitor != null) {
                this.mPackageMonitor.unregister();
                this.mPackageMonitor = null;
            }
            if (scorer != null) {
                this.mPackageMonitor = new NetworkScorerPackageMonitor(this, scorer.mPackageName, null);
                this.mPackageMonitor.register(this.mContext, null, UserHandle.SYSTEM, false);
            }
        }
    }

    private void bindToScoringServiceIfNeeded() {
        NetworkScorerAppManager.NetworkScorerAppData scorerData = NetworkScorerAppManager.getActiveScorer(this.mContext);
        bindToScoringServiceIfNeeded(scorerData);
    }

    private void bindToScoringServiceIfNeeded(NetworkScorerAppManager.NetworkScorerAppData scorerData) {
        if (scorerData != null && scorerData.mScoringServiceClassName != null) {
            ComponentName componentName = new ComponentName(scorerData.mPackageName, scorerData.mScoringServiceClassName);
            if (this.mServiceConnection != null && !this.mServiceConnection.mComponentName.equals(componentName)) {
                unbindFromScoringServiceIfNeeded();
            }
            if (this.mServiceConnection == null) {
                this.mServiceConnection = new ScoringServiceConnection(componentName);
            }
            this.mServiceConnection.connect(this.mContext);
            return;
        }
        unbindFromScoringServiceIfNeeded();
    }

    private void unbindFromScoringServiceIfNeeded() {
        if (this.mServiceConnection != null) {
            this.mServiceConnection.disconnect(this.mContext);
        }
        this.mServiceConnection = null;
    }

    public boolean updateScores(ScoredNetwork[] networks) {
        if (!NetworkScorerAppManager.isCallerActiveScorer(this.mContext, getCallingUid())) {
            throw new SecurityException("Caller with UID " + getCallingUid() + " is not the active scorer.");
        }
        Map<Integer, List<ScoredNetwork>> networksByType = new HashMap<>();
        for (ScoredNetwork network : networks) {
            List<ScoredNetwork> networkList = networksByType.get(Integer.valueOf(network.networkKey.type));
            if (networkList == null) {
                networkList = new ArrayList<>();
                networksByType.put(Integer.valueOf(network.networkKey.type), networkList);
            }
            networkList.add(network);
        }
        for (Map.Entry<Integer, List<ScoredNetwork>> entry : networksByType.entrySet()) {
            INetworkScoreCache scoreCache = this.mScoreCaches.get(entry.getKey());
            if (scoreCache != null) {
                try {
                    scoreCache.updateScores(entry.getValue());
                } catch (RemoteException e) {
                    if (Log.isLoggable(TAG, 2)) {
                        Log.v(TAG, "Unable to update scores of type " + entry.getKey(), e);
                    }
                }
            } else if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "No scorer registered for type " + entry.getKey() + ", discarding");
            }
        }
        return true;
    }

    public boolean clearScores() {
        if (NetworkScorerAppManager.isCallerActiveScorer(this.mContext, getCallingUid()) || this.mContext.checkCallingOrSelfPermission("android.permission.BROADCAST_NETWORK_PRIVILEGED") == 0) {
            clearInternal();
            return true;
        }
        throw new SecurityException("Caller is neither the active scorer nor the scorer manager.");
    }

    public boolean setActiveScorer(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SCORE_NETWORKS", TAG);
        return setScorerInternal(packageName);
    }

    public void disableScoring() {
        if (NetworkScorerAppManager.isCallerActiveScorer(this.mContext, getCallingUid()) || this.mContext.checkCallingOrSelfPermission("android.permission.BROADCAST_NETWORK_PRIVILEGED") == 0) {
            setScorerInternal(null);
            return;
        }
        throw new SecurityException("Caller is neither the active scorer nor the scorer manager.");
    }

    private boolean setScorerInternal(String packageName) {
        long token = Binder.clearCallingIdentity();
        try {
            unbindFromScoringServiceIfNeeded();
            clearInternal();
            NetworkScorerAppManager.NetworkScorerAppData prevScorer = NetworkScorerAppManager.getActiveScorer(this.mContext);
            boolean result = NetworkScorerAppManager.setActiveScorer(this.mContext, packageName);
            bindToScoringServiceIfNeeded();
            if (result) {
                registerPackageMonitorIfNeeded();
                Intent intent = new Intent("android.net.scoring.SCORER_CHANGED");
                if (prevScorer != null) {
                    intent.setPackage(prevScorer.mPackageName);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
                }
                if (packageName != null) {
                    intent.putExtra("newScorer", packageName);
                    intent.setPackage(packageName);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
                }
            }
            return result;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void clearInternal() {
        Set<INetworkScoreCache> cachesToClear = getScoreCaches();
        for (INetworkScoreCache scoreCache : cachesToClear) {
            try {
                scoreCache.clearScores();
            } catch (RemoteException e) {
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "Unable to clear scores", e);
                }
            }
        }
    }

    public void registerNetworkScoreCache(int networkType, INetworkScoreCache scoreCache) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BROADCAST_NETWORK_PRIVILEGED", TAG);
        synchronized (this.mScoreCaches) {
            if (this.mScoreCaches.containsKey(Integer.valueOf(networkType))) {
                throw new IllegalArgumentException("Score cache already registered for type " + networkType);
            }
            this.mScoreCaches.put(Integer.valueOf(networkType), scoreCache);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        NetworkScorerAppManager.NetworkScorerAppData currentScorer = NetworkScorerAppManager.getActiveScorer(this.mContext);
        if (currentScorer == null) {
            writer.println("Scoring is disabled.");
            return;
        }
        writer.println("Current scorer: " + currentScorer.mPackageName);
        for (INetworkScoreCache scoreCache : getScoreCaches()) {
            try {
                scoreCache.asBinder().dump(fd, args);
            } catch (RemoteException e) {
                writer.println("Unable to dump score cache");
                if (Log.isLoggable(TAG, 2)) {
                    Log.v(TAG, "Unable to dump score cache", e);
                }
            }
        }
        if (this.mServiceConnection != null) {
            this.mServiceConnection.dump(fd, writer, args);
        } else {
            writer.println("ScoringServiceConnection: null");
        }
        writer.flush();
    }

    private Set<INetworkScoreCache> getScoreCaches() {
        HashSet hashSet;
        synchronized (this.mScoreCaches) {
            hashSet = new HashSet(this.mScoreCaches.values());
        }
        return hashSet;
    }

    private static class ScoringServiceConnection implements ServiceConnection {
        private final ComponentName mComponentName;
        private boolean mBound = false;
        private boolean mConnected = false;

        ScoringServiceConnection(ComponentName componentName) {
            this.mComponentName = componentName;
        }

        void connect(Context context) {
            if (this.mBound) {
                return;
            }
            Intent service = new Intent();
            service.setComponent(this.mComponentName);
            this.mBound = context.bindServiceAsUser(service, this, 67108865, UserHandle.SYSTEM);
            if (this.mBound) {
                return;
            }
            Log.w(NetworkScoreService.TAG, "Bind call failed for " + service);
        }

        void disconnect(Context context) {
            try {
                if (!this.mBound) {
                    return;
                }
                this.mBound = false;
                context.unbindService(this);
            } catch (RuntimeException e) {
                Log.e(NetworkScoreService.TAG, "Unbind failed.", e);
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.mConnected = false;
        }

        public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            writer.println("ScoringServiceConnection: " + this.mComponentName + ", bound: " + this.mBound + ", connected: " + this.mConnected);
        }
    }
}
