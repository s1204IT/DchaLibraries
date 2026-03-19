package android.net;

import android.annotation.SystemApi;
import android.content.Context;
import android.net.INetworkScoreService;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

@SystemApi
public class NetworkScoreManager {
    public static final String ACTION_CHANGE_ACTIVE = "android.net.scoring.CHANGE_ACTIVE";
    public static final String ACTION_CUSTOM_ENABLE = "android.net.scoring.CUSTOM_ENABLE";
    public static final String ACTION_RECOMMEND_NETWORKS = "android.net.action.RECOMMEND_NETWORKS";
    public static final String ACTION_SCORER_CHANGED = "android.net.scoring.SCORER_CHANGED";
    public static final String ACTION_SCORE_NETWORKS = "android.net.scoring.SCORE_NETWORKS";
    public static final int CACHE_FILTER_CURRENT_NETWORK = 1;
    public static final int CACHE_FILTER_NONE = 0;
    public static final int CACHE_FILTER_SCAN_RESULTS = 2;
    public static final String EXTRA_NETWORKS_TO_SCORE = "networksToScore";
    public static final String EXTRA_NEW_SCORER = "newScorer";
    public static final String EXTRA_PACKAGE_NAME = "packageName";
    public static final String NETWORK_AVAILABLE_NOTIFICATION_CHANNEL_ID_META_DATA = "android.net.wifi.notification_channel_id_network_available";
    public static final int RECOMMENDATIONS_ENABLED_FORCED_OFF = -1;
    public static final int RECOMMENDATIONS_ENABLED_OFF = 0;
    public static final int RECOMMENDATIONS_ENABLED_ON = 1;
    public static final String RECOMMENDATION_SERVICE_LABEL_META_DATA = "android.net.scoring.recommendation_service_label";
    public static final String USE_OPEN_WIFI_PACKAGE_META_DATA = "android.net.wifi.use_open_wifi_package";
    private final Context mContext;
    private final INetworkScoreService mService = INetworkScoreService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.NETWORK_SCORE_SERVICE));

    @Retention(RetentionPolicy.SOURCE)
    public @interface CacheUpdateFilter {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RecommendationsEnabledSetting {
    }

    public NetworkScoreManager(Context context) throws ServiceManager.ServiceNotFoundException {
        this.mContext = context;
    }

    public String getActiveScorerPackage() {
        try {
            return this.mService.getActiveScorerPackage();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkScorerAppData getActiveScorer() {
        try {
            return this.mService.getActiveScorer();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<NetworkScorerAppData> getAllValidScorers() {
        try {
            return this.mService.getAllValidScorers();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean updateScores(ScoredNetwork[] scoredNetworkArr) throws SecurityException {
        try {
            return this.mService.updateScores(scoredNetworkArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean clearScores() throws SecurityException {
        try {
            return this.mService.clearScores();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean setActiveScorer(String str) throws SecurityException {
        try {
            return this.mService.setActiveScorer(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableScoring() throws SecurityException {
        try {
            this.mService.disableScoring();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean requestScores(NetworkKey[] networkKeyArr) throws SecurityException {
        try {
            return this.mService.requestScores(networkKeyArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void registerNetworkScoreCache(int i, INetworkScoreCache iNetworkScoreCache) {
        registerNetworkScoreCache(i, iNetworkScoreCache, 0);
    }

    public void registerNetworkScoreCache(int i, INetworkScoreCache iNetworkScoreCache, int i2) {
        try {
            this.mService.registerNetworkScoreCache(i, iNetworkScoreCache, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterNetworkScoreCache(int i, INetworkScoreCache iNetworkScoreCache) {
        try {
            this.mService.unregisterNetworkScoreCache(i, iNetworkScoreCache);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isCallerActiveScorer(int i) {
        try {
            return this.mService.isCallerActiveScorer(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
