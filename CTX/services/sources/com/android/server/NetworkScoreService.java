package com.android.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.net.INetworkRecommendationProvider;
import android.net.INetworkScoreCache;
import android.net.INetworkScoreService;
import android.net.NetworkKey;
import android.net.NetworkScorerAppData;
import android.net.ScoredNetwork;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.DumpUtils;
import com.android.server.NetworkScoreService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class NetworkScoreService extends INetworkScoreService.Stub {
    private static final boolean DBG;
    private static final String TAG = "NetworkScoreService";
    private static final boolean VERBOSE;
    private final Context mContext;
    private final Handler mHandler;
    private BroadcastReceiver mLocationModeReceiver;
    private final NetworkScorerAppManager mNetworkScorerAppManager;

    @GuardedBy("mPackageMonitorLock")
    private NetworkScorerPackageMonitor mPackageMonitor;
    private final Object mPackageMonitorLock;
    private final DispatchingContentObserver mRecommendationSettingsObserver;

    @GuardedBy("mScoreCaches")
    private final Map<Integer, RemoteCallbackList<INetworkScoreCache>> mScoreCaches;
    private final Function<NetworkScorerAppData, ScoringServiceConnection> mServiceConnProducer;

    @GuardedBy("mServiceConnectionLock")
    private ScoringServiceConnection mServiceConnection;
    private final Object mServiceConnectionLock;
    private final ContentObserver mUseOpenWifiPackageObserver;
    private BroadcastReceiver mUserIntentReceiver;

    static {
        boolean z = false;
        DBG = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 3);
        if (Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 2)) {
            z = true;
        }
        VERBOSE = z;
    }

    public static final class Lifecycle extends SystemService {
        private final NetworkScoreService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new NetworkScoreService(context);
        }

        @Override
        public void onStart() {
            Log.i(NetworkScoreService.TAG, "Registering network_score");
            publishBinderService("network_score", this.mService);
        }

        @Override
        public void onBootPhase(int i) {
            if (i == 500) {
                this.mService.systemReady();
            } else if (i == 1000) {
                this.mService.systemRunning();
            }
        }
    }

    private class NetworkScorerPackageMonitor extends PackageMonitor {
        final String mPackageToWatch;

        private NetworkScorerPackageMonitor(String str) {
            this.mPackageToWatch = str;
        }

        public void onPackageAdded(String str, int i) {
            evaluateBinding(str, true);
        }

        public void onPackageRemoved(String str, int i) {
            evaluateBinding(str, true);
        }

        public void onPackageModified(String str) {
            evaluateBinding(str, false);
        }

        public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
            if (z) {
                for (String str : strArr) {
                    evaluateBinding(str, true);
                }
            }
            return super.onHandleForceStop(intent, strArr, i, z);
        }

        public void onPackageUpdateFinished(String str, int i) {
            evaluateBinding(str, true);
        }

        private void evaluateBinding(String str, boolean z) {
            if (this.mPackageToWatch.equals(str)) {
                if (NetworkScoreService.DBG) {
                    Log.d(NetworkScoreService.TAG, "Evaluating binding for: " + str + ", forceUnbind=" + z);
                }
                NetworkScorerAppData activeScorer = NetworkScoreService.this.mNetworkScorerAppManager.getActiveScorer();
                if (activeScorer == null) {
                    if (NetworkScoreService.DBG) {
                        Log.d(NetworkScoreService.TAG, "No active scorers available.");
                    }
                    NetworkScoreService.this.refreshBinding();
                    return;
                }
                if (z) {
                    NetworkScoreService.this.unbindFromScoringServiceIfNeeded();
                }
                if (NetworkScoreService.DBG) {
                    Log.d(NetworkScoreService.TAG, "Binding to " + activeScorer.getRecommendationServiceComponent() + " if needed.");
                }
                NetworkScoreService.this.bindToScoringServiceIfNeeded(activeScorer);
            }
        }
    }

    @VisibleForTesting
    public static class DispatchingContentObserver extends ContentObserver {
        private final Context mContext;
        private final Handler mHandler;
        private final Map<Uri, Integer> mUriEventMap;

        public DispatchingContentObserver(Context context, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mHandler = handler;
            this.mUriEventMap = new ArrayMap();
        }

        void observe(Uri uri, int i) {
            this.mUriEventMap.put(uri, Integer.valueOf(i));
            this.mContext.getContentResolver().registerContentObserver(uri, false, this);
        }

        @Override
        public void onChange(boolean z) {
            onChange(z, null);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (NetworkScoreService.DBG) {
                Log.d(NetworkScoreService.TAG, String.format("onChange(%s, %s)", Boolean.valueOf(z), uri));
            }
            Integer num = this.mUriEventMap.get(uri);
            if (num != null) {
                this.mHandler.obtainMessage(num.intValue()).sendToTarget();
                return;
            }
            Log.w(NetworkScoreService.TAG, "No matching event to send for URI = " + uri);
        }
    }

    public NetworkScoreService(Context context) {
        this(context, new NetworkScorerAppManager(context), new Function() {
            @Override
            public final Object apply(Object obj) {
                return new NetworkScoreService.ScoringServiceConnection((NetworkScorerAppData) obj);
            }
        }, Looper.myLooper());
    }

    @VisibleForTesting
    NetworkScoreService(Context context, NetworkScorerAppManager networkScorerAppManager, Function<NetworkScorerAppData, ScoringServiceConnection> function, Looper looper) {
        this.mPackageMonitorLock = new Object();
        this.mServiceConnectionLock = new Object();
        this.mUserIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (NetworkScoreService.DBG) {
                    Log.d(NetworkScoreService.TAG, "Received " + action + " for userId " + intExtra);
                }
                if (intExtra != -10000 && "android.intent.action.USER_UNLOCKED".equals(action)) {
                    NetworkScoreService.this.onUserUnlocked(intExtra);
                }
            }
        };
        this.mLocationModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.location.MODE_CHANGED".equals(intent.getAction())) {
                    NetworkScoreService.this.refreshBinding();
                }
            }
        };
        this.mContext = context;
        this.mNetworkScorerAppManager = networkScorerAppManager;
        this.mScoreCaches = new ArrayMap();
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.SYSTEM, new IntentFilter("android.intent.action.USER_UNLOCKED"), null, null);
        this.mHandler = new ServiceHandler(looper);
        this.mContext.registerReceiverAsUser(this.mLocationModeReceiver, UserHandle.SYSTEM, new IntentFilter("android.location.MODE_CHANGED"), null, this.mHandler);
        this.mRecommendationSettingsObserver = new DispatchingContentObserver(context, this.mHandler);
        this.mServiceConnProducer = function;
        this.mUseOpenWifiPackageObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z, Uri uri, int i) {
                if (Settings.Global.getUriFor("use_open_wifi_package").equals(uri)) {
                    String string = Settings.Global.getString(NetworkScoreService.this.mContext.getContentResolver(), "use_open_wifi_package");
                    if (!TextUtils.isEmpty(string)) {
                        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).grantDefaultPermissionsToDefaultUseOpenWifiApp(string, i);
                    }
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("use_open_wifi_package"), false, this.mUseOpenWifiPackageObserver);
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setUseOpenWifiAppPackagesProvider(new PackageManagerInternal.PackagesProvider() {
            public String[] getPackages(int i) {
                String string = Settings.Global.getString(NetworkScoreService.this.mContext.getContentResolver(), "use_open_wifi_package");
                if (!TextUtils.isEmpty(string)) {
                    return new String[]{string};
                }
                return null;
            }
        });
    }

    void systemReady() {
        if (DBG) {
            Log.d(TAG, "systemReady");
        }
        registerRecommendationSettingsObserver();
    }

    void systemRunning() {
        if (DBG) {
            Log.d(TAG, "systemRunning");
        }
    }

    @VisibleForTesting
    void onUserUnlocked(int i) {
        if (DBG) {
            Log.d(TAG, "onUserUnlocked(" + i + ")");
        }
        refreshBinding();
    }

    private void refreshBinding() {
        if (DBG) {
            Log.d(TAG, "refreshBinding()");
        }
        this.mNetworkScorerAppManager.updateState();
        this.mNetworkScorerAppManager.migrateNetworkScorerAppSettingIfNeeded();
        registerPackageMonitorIfNeeded();
        bindToScoringServiceIfNeeded();
    }

    private void registerRecommendationSettingsObserver() {
        this.mRecommendationSettingsObserver.observe(Settings.Global.getUriFor("network_recommendations_package"), 1);
        this.mRecommendationSettingsObserver.observe(Settings.Global.getUriFor("network_recommendations_enabled"), 2);
    }

    private void registerPackageMonitorIfNeeded() {
        if (DBG) {
            Log.d(TAG, "registerPackageMonitorIfNeeded()");
        }
        NetworkScorerAppData activeScorer = this.mNetworkScorerAppManager.getActiveScorer();
        synchronized (this.mPackageMonitorLock) {
            if (this.mPackageMonitor != null && (activeScorer == null || !activeScorer.getRecommendationServicePackageName().equals(this.mPackageMonitor.mPackageToWatch))) {
                if (DBG) {
                    Log.d(TAG, "Unregistering package monitor for " + this.mPackageMonitor.mPackageToWatch);
                }
                this.mPackageMonitor.unregister();
                this.mPackageMonitor = null;
            }
            if (activeScorer != null && this.mPackageMonitor == null) {
                this.mPackageMonitor = new NetworkScorerPackageMonitor(activeScorer.getRecommendationServicePackageName());
                this.mPackageMonitor.register(this.mContext, null, UserHandle.SYSTEM, false);
                if (DBG) {
                    Log.d(TAG, "Registered package monitor for " + this.mPackageMonitor.mPackageToWatch);
                }
            }
        }
    }

    private void bindToScoringServiceIfNeeded() {
        if (DBG) {
            Log.d(TAG, "bindToScoringServiceIfNeeded");
        }
        bindToScoringServiceIfNeeded(this.mNetworkScorerAppManager.getActiveScorer());
    }

    private void bindToScoringServiceIfNeeded(NetworkScorerAppData networkScorerAppData) {
        if (DBG) {
            Log.d(TAG, "bindToScoringServiceIfNeeded(" + networkScorerAppData + ")");
        }
        if (networkScorerAppData != null) {
            synchronized (this.mServiceConnectionLock) {
                if (this.mServiceConnection != null && !this.mServiceConnection.getAppData().equals(networkScorerAppData)) {
                    unbindFromScoringServiceIfNeeded();
                }
                if (this.mServiceConnection == null) {
                    this.mServiceConnection = this.mServiceConnProducer.apply(networkScorerAppData);
                }
                this.mServiceConnection.bind(this.mContext);
            }
            return;
        }
        unbindFromScoringServiceIfNeeded();
    }

    private void unbindFromScoringServiceIfNeeded() {
        if (DBG) {
            Log.d(TAG, "unbindFromScoringServiceIfNeeded");
        }
        synchronized (this.mServiceConnectionLock) {
            if (this.mServiceConnection != null) {
                this.mServiceConnection.unbind(this.mContext);
                if (DBG) {
                    Log.d(TAG, "Disconnected from: " + this.mServiceConnection.getAppData().getRecommendationServiceComponent());
                }
            }
            this.mServiceConnection = null;
        }
        clearInternal();
    }

    public boolean updateScores(ScoredNetwork[] scoredNetworkArr) {
        RemoteCallbackList<INetworkScoreCache> remoteCallbackList;
        if (!isCallerActiveScorer(getCallingUid())) {
            throw new SecurityException("Caller with UID " + getCallingUid() + " is not the active scorer.");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ArrayMap arrayMap = new ArrayMap();
            for (ScoredNetwork scoredNetwork : scoredNetworkArr) {
                List arrayList = (List) arrayMap.get(Integer.valueOf(scoredNetwork.networkKey.type));
                if (arrayList == null) {
                    arrayList = new ArrayList();
                    arrayMap.put(Integer.valueOf(scoredNetwork.networkKey.type), arrayList);
                }
                arrayList.add(scoredNetwork);
            }
            Iterator it = arrayMap.entrySet().iterator();
            while (true) {
                boolean z = true;
                if (!it.hasNext()) {
                    return true;
                }
                Map.Entry entry = (Map.Entry) it.next();
                synchronized (this.mScoreCaches) {
                    remoteCallbackList = this.mScoreCaches.get(entry.getKey());
                    if (remoteCallbackList != null && remoteCallbackList.getRegisteredCallbackCount() != 0) {
                        z = false;
                    }
                }
                if (z) {
                    if (Log.isLoggable(TAG, 2)) {
                        Log.v(TAG, "No scorer registered for type " + entry.getKey() + ", discarding");
                    }
                } else {
                    sendCacheUpdateCallback(FilteringCacheUpdatingConsumer.create(this.mContext, (List) entry.getValue(), ((Integer) entry.getKey()).intValue()), Collections.singleton(remoteCallbackList));
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @VisibleForTesting
    static class FilteringCacheUpdatingConsumer implements BiConsumer<INetworkScoreCache, Object> {
        private final Context mContext;
        private UnaryOperator<List<ScoredNetwork>> mCurrentNetworkFilter;
        private final int mNetworkType;
        private UnaryOperator<List<ScoredNetwork>> mScanResultsFilter;
        private final List<ScoredNetwork> mScoredNetworkList;

        static FilteringCacheUpdatingConsumer create(Context context, List<ScoredNetwork> list, int i) {
            return new FilteringCacheUpdatingConsumer(context, list, i, null, null);
        }

        @VisibleForTesting
        FilteringCacheUpdatingConsumer(Context context, List<ScoredNetwork> list, int i, UnaryOperator<List<ScoredNetwork>> unaryOperator, UnaryOperator<List<ScoredNetwork>> unaryOperator2) {
            this.mContext = context;
            this.mScoredNetworkList = list;
            this.mNetworkType = i;
            this.mCurrentNetworkFilter = unaryOperator;
            this.mScanResultsFilter = unaryOperator2;
        }

        @Override
        public void accept(INetworkScoreCache iNetworkScoreCache, Object obj) {
            int iIntValue;
            if (obj instanceof Integer) {
                iIntValue = ((Integer) obj).intValue();
            } else {
                iIntValue = 0;
            }
            try {
                List<ScoredNetwork> listFilterScores = filterScores(this.mScoredNetworkList, iIntValue);
                if (!listFilterScores.isEmpty()) {
                    iNetworkScoreCache.updateScores(listFilterScores);
                }
            } catch (RemoteException e) {
                if (NetworkScoreService.VERBOSE) {
                    Log.v(NetworkScoreService.TAG, "Unable to update scores of type " + this.mNetworkType, e);
                }
            }
        }

        private List<ScoredNetwork> filterScores(List<ScoredNetwork> list, int i) {
            switch (i) {
                case 0:
                    return list;
                case 1:
                    if (this.mCurrentNetworkFilter == null) {
                        this.mCurrentNetworkFilter = new CurrentNetworkScoreCacheFilter(new WifiInfoSupplier(this.mContext));
                    }
                    return (List) this.mCurrentNetworkFilter.apply(list);
                case 2:
                    if (this.mScanResultsFilter == null) {
                        this.mScanResultsFilter = new ScanResultsScoreCacheFilter(new ScanResultsSupplier(this.mContext));
                    }
                    return (List) this.mScanResultsFilter.apply(list);
                default:
                    Log.w(NetworkScoreService.TAG, "Unknown filter type: " + i);
                    return list;
            }
        }
    }

    private static class WifiInfoSupplier implements Supplier<WifiInfo> {
        private final Context mContext;

        WifiInfoSupplier(Context context) {
            this.mContext = context;
        }

        @Override
        public WifiInfo get() {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(WifiManager.class);
            if (wifiManager != null) {
                return wifiManager.getConnectionInfo();
            }
            Log.w(NetworkScoreService.TAG, "WifiManager is null, failed to return the WifiInfo.");
            return null;
        }
    }

    private static class ScanResultsSupplier implements Supplier<List<ScanResult>> {
        private final Context mContext;

        ScanResultsSupplier(Context context) {
            this.mContext = context;
        }

        @Override
        public List<ScanResult> get() {
            WifiScanner wifiScanner = (WifiScanner) this.mContext.getSystemService(WifiScanner.class);
            if (wifiScanner != null) {
                return wifiScanner.getSingleScanResults();
            }
            Log.w(NetworkScoreService.TAG, "WifiScanner is null, failed to return scan results.");
            return Collections.emptyList();
        }
    }

    @VisibleForTesting
    static class CurrentNetworkScoreCacheFilter implements UnaryOperator<List<ScoredNetwork>> {
        private final NetworkKey mCurrentNetwork;

        CurrentNetworkScoreCacheFilter(Supplier<WifiInfo> supplier) {
            this.mCurrentNetwork = NetworkKey.createFromWifiInfo(supplier.get());
        }

        @Override
        public List<ScoredNetwork> apply(List<ScoredNetwork> list) {
            if (this.mCurrentNetwork == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            for (int i = 0; i < list.size(); i++) {
                ScoredNetwork scoredNetwork = list.get(i);
                if (scoredNetwork.networkKey.equals(this.mCurrentNetwork)) {
                    return Collections.singletonList(scoredNetwork);
                }
            }
            return Collections.emptyList();
        }
    }

    @VisibleForTesting
    static class ScanResultsScoreCacheFilter implements UnaryOperator<List<ScoredNetwork>> {
        private final Set<NetworkKey> mScanResultKeys;

        ScanResultsScoreCacheFilter(Supplier<List<ScanResult>> supplier) {
            List<ScanResult> list = supplier.get();
            int size = list.size();
            this.mScanResultKeys = new ArraySet(size);
            for (int i = 0; i < size; i++) {
                NetworkKey networkKeyCreateFromScanResult = NetworkKey.createFromScanResult(list.get(i));
                if (networkKeyCreateFromScanResult != null) {
                    this.mScanResultKeys.add(networkKeyCreateFromScanResult);
                }
            }
        }

        @Override
        public List<ScoredNetwork> apply(List<ScoredNetwork> list) {
            if (this.mScanResultKeys.isEmpty() || list.isEmpty()) {
                return Collections.emptyList();
            }
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < list.size(); i++) {
                ScoredNetwork scoredNetwork = list.get(i);
                if (this.mScanResultKeys.contains(scoredNetwork.networkKey)) {
                    arrayList.add(scoredNetwork);
                }
            }
            return arrayList;
        }
    }

    public boolean clearScores() {
        enforceSystemOrIsActiveScorer(getCallingUid());
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            clearInternal();
            return true;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean setActiveScorer(String str) {
        enforceSystemOrHasScoreNetworks();
        return this.mNetworkScorerAppManager.setActiveScorer(str);
    }

    public boolean isCallerActiveScorer(int i) {
        boolean z;
        synchronized (this.mServiceConnectionLock) {
            z = this.mServiceConnection != null && this.mServiceConnection.getAppData().packageUid == i;
        }
        return z;
    }

    private void enforceSystemOnly() throws SecurityException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.REQUEST_NETWORK_SCORES", "Caller must be granted REQUEST_NETWORK_SCORES.");
    }

    private void enforceSystemOrHasScoreNetworks() throws SecurityException {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.REQUEST_NETWORK_SCORES") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.SCORE_NETWORKS") != 0) {
            throw new SecurityException("Caller is neither the system process or a network scorer.");
        }
    }

    private void enforceSystemOrIsActiveScorer(int i) throws SecurityException {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.REQUEST_NETWORK_SCORES") != 0 && !isCallerActiveScorer(i)) {
            throw new SecurityException("Caller is neither the system process or the active network scorer.");
        }
    }

    public String getActiveScorerPackage() {
        enforceSystemOrHasScoreNetworks();
        synchronized (this.mServiceConnectionLock) {
            if (this.mServiceConnection != null) {
                return this.mServiceConnection.getPackageName();
            }
            return null;
        }
    }

    public NetworkScorerAppData getActiveScorer() {
        enforceSystemOnly();
        synchronized (this.mServiceConnectionLock) {
            if (this.mServiceConnection != null) {
                return this.mServiceConnection.getAppData();
            }
            return null;
        }
    }

    public List<NetworkScorerAppData> getAllValidScorers() {
        enforceSystemOnly();
        return this.mNetworkScorerAppManager.getAllValidScorers();
    }

    public void disableScoring() {
        enforceSystemOrIsActiveScorer(getCallingUid());
    }

    private void clearInternal() {
        sendCacheUpdateCallback(new BiConsumer<INetworkScoreCache, Object>() {
            @Override
            public void accept(INetworkScoreCache iNetworkScoreCache, Object obj) {
                try {
                    iNetworkScoreCache.clearScores();
                } catch (RemoteException e) {
                    if (Log.isLoggable(NetworkScoreService.TAG, 2)) {
                        Log.v(NetworkScoreService.TAG, "Unable to clear scores", e);
                    }
                }
            }
        }, getScoreCacheLists());
    }

    public void registerNetworkScoreCache(int i, INetworkScoreCache iNetworkScoreCache, int i2) {
        enforceSystemOnly();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mScoreCaches) {
                RemoteCallbackList<INetworkScoreCache> remoteCallbackList = this.mScoreCaches.get(Integer.valueOf(i));
                if (remoteCallbackList == null) {
                    remoteCallbackList = new RemoteCallbackList<>();
                    this.mScoreCaches.put(Integer.valueOf(i), remoteCallbackList);
                }
                if (!remoteCallbackList.register(iNetworkScoreCache, Integer.valueOf(i2))) {
                    if (remoteCallbackList.getRegisteredCallbackCount() == 0) {
                        this.mScoreCaches.remove(Integer.valueOf(i));
                    }
                    if (Log.isLoggable(TAG, 2)) {
                        Log.v(TAG, "Unable to register NetworkScoreCache for type " + i);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void unregisterNetworkScoreCache(int i, INetworkScoreCache iNetworkScoreCache) {
        enforceSystemOnly();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mScoreCaches) {
                RemoteCallbackList<INetworkScoreCache> remoteCallbackList = this.mScoreCaches.get(Integer.valueOf(i));
                if (remoteCallbackList == null || !remoteCallbackList.unregister(iNetworkScoreCache)) {
                    if (Log.isLoggable(TAG, 2)) {
                        Log.v(TAG, "Unable to unregister NetworkScoreCache for type " + i);
                    }
                } else if (remoteCallbackList.getRegisteredCallbackCount() == 0) {
                    this.mScoreCaches.remove(Integer.valueOf(i));
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean requestScores(NetworkKey[] networkKeyArr) {
        enforceSystemOnly();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            INetworkRecommendationProvider recommendationProvider = getRecommendationProvider();
            if (recommendationProvider != null) {
                try {
                    recommendationProvider.requestScores(networkKeyArr);
                    return true;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to request scores.", e);
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected void dump(final FileDescriptor fileDescriptor, final PrintWriter printWriter, final String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                NetworkScorerAppData activeScorer = this.mNetworkScorerAppManager.getActiveScorer();
                if (activeScorer == null) {
                    printWriter.println("Scoring is disabled.");
                    return;
                }
                printWriter.println("Current scorer: " + activeScorer);
                sendCacheUpdateCallback(new BiConsumer<INetworkScoreCache, Object>() {
                    @Override
                    public void accept(INetworkScoreCache iNetworkScoreCache, Object obj) {
                        try {
                            TransferPipe.dumpAsync(iNetworkScoreCache.asBinder(), fileDescriptor, strArr);
                        } catch (RemoteException | IOException e) {
                            printWriter.println("Failed to dump score cache: " + e);
                        }
                    }
                }, getScoreCacheLists());
                synchronized (this.mServiceConnectionLock) {
                    if (this.mServiceConnection != null) {
                        this.mServiceConnection.dump(fileDescriptor, printWriter, strArr);
                    } else {
                        printWriter.println("ScoringServiceConnection: null");
                    }
                }
                printWriter.flush();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private Collection<RemoteCallbackList<INetworkScoreCache>> getScoreCacheLists() {
        ArrayList arrayList;
        synchronized (this.mScoreCaches) {
            arrayList = new ArrayList(this.mScoreCaches.values());
        }
        return arrayList;
    }

    private void sendCacheUpdateCallback(BiConsumer<INetworkScoreCache, Object> biConsumer, Collection<RemoteCallbackList<INetworkScoreCache>> collection) {
        Iterator<RemoteCallbackList<INetworkScoreCache>> it = collection.iterator();
        while (it.hasNext()) {
            RemoteCallbackList<INetworkScoreCache> next = it.next();
            synchronized (next) {
                int iBeginBroadcast = next.beginBroadcast();
                for (int i = 0; i < iBeginBroadcast; i++) {
                    try {
                        biConsumer.accept((INetworkScoreCache) next.getBroadcastItem(i), next.getBroadcastCookie(i));
                    } finally {
                    }
                }
            }
        }
    }

    private INetworkRecommendationProvider getRecommendationProvider() {
        synchronized (this.mServiceConnectionLock) {
            if (this.mServiceConnection != null) {
                return this.mServiceConnection.getRecommendationProvider();
            }
            return null;
        }
    }

    @VisibleForTesting
    public static class ScoringServiceConnection implements ServiceConnection {
        private final NetworkScorerAppData mAppData;
        private volatile boolean mBound = false;
        private volatile boolean mConnected = false;
        private volatile INetworkRecommendationProvider mRecommendationProvider;

        ScoringServiceConnection(NetworkScorerAppData networkScorerAppData) {
            this.mAppData = networkScorerAppData;
        }

        @VisibleForTesting
        public void bind(Context context) {
            if (!this.mBound) {
                Intent intent = new Intent("android.net.action.RECOMMEND_NETWORKS");
                intent.setComponent(this.mAppData.getRecommendationServiceComponent());
                this.mBound = context.bindServiceAsUser(intent, this, 67108865, UserHandle.SYSTEM);
                if (this.mBound) {
                    if (NetworkScoreService.DBG) {
                        Log.d(NetworkScoreService.TAG, "ScoringServiceConnection bound.");
                    }
                } else {
                    Log.w(NetworkScoreService.TAG, "Bind call failed for " + intent);
                    context.unbindService(this);
                }
            }
        }

        @VisibleForTesting
        public void unbind(Context context) {
            try {
                if (this.mBound) {
                    this.mBound = false;
                    context.unbindService(this);
                    if (NetworkScoreService.DBG) {
                        Log.d(NetworkScoreService.TAG, "ScoringServiceConnection unbound.");
                    }
                }
            } catch (RuntimeException e) {
                Log.e(NetworkScoreService.TAG, "Unbind failed.", e);
            }
            this.mConnected = false;
            this.mRecommendationProvider = null;
        }

        @VisibleForTesting
        public NetworkScorerAppData getAppData() {
            return this.mAppData;
        }

        @VisibleForTesting
        public INetworkRecommendationProvider getRecommendationProvider() {
            return this.mRecommendationProvider;
        }

        @VisibleForTesting
        public String getPackageName() {
            return this.mAppData.getRecommendationServiceComponent().getPackageName();
        }

        @VisibleForTesting
        public boolean isAlive() {
            return this.mBound && this.mConnected;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (NetworkScoreService.DBG) {
                Log.d(NetworkScoreService.TAG, "ScoringServiceConnection: " + componentName.flattenToString());
            }
            this.mConnected = true;
            this.mRecommendationProvider = INetworkRecommendationProvider.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (NetworkScoreService.DBG) {
                Log.d(NetworkScoreService.TAG, "ScoringServiceConnection, disconnected: " + componentName.flattenToString());
            }
            this.mConnected = false;
            this.mRecommendationProvider = null;
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.println("ScoringServiceConnection: " + this.mAppData.getRecommendationServiceComponent() + ", bound: " + this.mBound + ", connected: " + this.mConnected);
        }
    }

    @VisibleForTesting
    public final class ServiceHandler extends Handler {
        public static final int MSG_RECOMMENDATIONS_PACKAGE_CHANGED = 1;
        public static final int MSG_RECOMMENDATION_ENABLED_SETTING_CHANGED = 2;

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 1:
                case 2:
                    NetworkScoreService.this.refreshBinding();
                    break;
                default:
                    Log.w(NetworkScoreService.TAG, "Unknown message: " + i);
                    break;
            }
        }
    }
}
