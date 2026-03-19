package android.net;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class NetworkScorerAppManager {
    private static final Intent SCORE_INTENT = new Intent(NetworkScoreManager.ACTION_SCORE_NETWORKS);
    private static final String TAG = "NetworkScorerAppManager";

    private NetworkScorerAppManager() {
    }

    public static class NetworkScorerAppData {
        public final String mConfigurationActivityClassName;
        public final String mPackageName;
        public final int mPackageUid;
        public final CharSequence mScorerName;
        public final String mScoringServiceClassName;

        public NetworkScorerAppData(String packageName, int packageUid, CharSequence scorerName, String configurationActivityClassName, String scoringServiceClassName) {
            this.mScorerName = scorerName;
            this.mPackageName = packageName;
            this.mPackageUid = packageUid;
            this.mConfigurationActivityClassName = configurationActivityClassName;
            this.mScoringServiceClassName = scoringServiceClassName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("NetworkScorerAppData{");
            sb.append("mPackageName='").append(this.mPackageName).append('\'');
            sb.append(", mPackageUid=").append(this.mPackageUid);
            sb.append(", mScorerName=").append(this.mScorerName);
            sb.append(", mConfigurationActivityClassName='").append(this.mConfigurationActivityClassName).append('\'');
            sb.append(", mScoringServiceClassName='").append(this.mScoringServiceClassName).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static Collection<NetworkScorerAppData> getAllValidScorers(Context context) {
        ActivityInfo activityInfo;
        if (UserHandle.getCallingUserId() != 0) {
            return Collections.emptyList();
        }
        List<NetworkScorerAppData> scorers = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> receivers = pm.queryBroadcastReceiversAsUser(SCORE_INTENT, 0, 0);
        for (ResolveInfo receiver : receivers) {
            ActivityInfo receiverInfo = receiver.activityInfo;
            if (receiverInfo != null && Manifest.permission.BROADCAST_NETWORK_PRIVILEGED.equals(receiverInfo.permission) && pm.checkPermission(Manifest.permission.SCORE_NETWORKS, receiverInfo.packageName) == 0) {
                String configurationActivityClassName = null;
                Intent intent = new Intent(NetworkScoreManager.ACTION_CUSTOM_ENABLE);
                intent.setPackage(receiverInfo.packageName);
                List<ResolveInfo> configActivities = pm.queryIntentActivities(intent, 0);
                if (configActivities != null && !configActivities.isEmpty() && (activityInfo = configActivities.get(0).activityInfo) != null) {
                    configurationActivityClassName = activityInfo.name;
                }
                String scoringServiceClassName = null;
                Intent serviceIntent = new Intent(NetworkScoreManager.ACTION_SCORE_NETWORKS);
                serviceIntent.setPackage(receiverInfo.packageName);
                ResolveInfo resolveServiceInfo = pm.resolveService(serviceIntent, 0);
                if (resolveServiceInfo != null && resolveServiceInfo.serviceInfo != null) {
                    scoringServiceClassName = resolveServiceInfo.serviceInfo.name;
                }
                scorers.add(new NetworkScorerAppData(receiverInfo.packageName, receiverInfo.applicationInfo.uid, receiverInfo.loadLabel(pm), configurationActivityClassName, scoringServiceClassName));
            }
        }
        return scorers;
    }

    public static NetworkScorerAppData getActiveScorer(Context context) {
        String scorerPackage = Settings.Global.getString(context.getContentResolver(), Settings.Global.NETWORK_SCORER_APP);
        return getScorer(context, scorerPackage);
    }

    public static boolean setActiveScorer(Context context, String packageName) {
        String oldPackageName = Settings.Global.getString(context.getContentResolver(), Settings.Global.NETWORK_SCORER_APP);
        if (TextUtils.equals(oldPackageName, packageName)) {
            return true;
        }
        Log.i(TAG, "Changing network scorer from " + oldPackageName + " to " + packageName);
        if (packageName == null) {
            Settings.Global.putString(context.getContentResolver(), Settings.Global.NETWORK_SCORER_APP, null);
            return true;
        }
        if (getScorer(context, packageName) != null) {
            Settings.Global.putString(context.getContentResolver(), Settings.Global.NETWORK_SCORER_APP, packageName);
            return true;
        }
        Log.w(TAG, "Requested network scorer is not valid: " + packageName);
        return false;
    }

    public static boolean isCallerActiveScorer(Context context, int callingUid) {
        NetworkScorerAppData defaultApp = getActiveScorer(context);
        return defaultApp != null && callingUid == defaultApp.mPackageUid && context.checkCallingPermission(Manifest.permission.SCORE_NETWORKS) == 0;
    }

    public static NetworkScorerAppData getScorer(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        Collection<NetworkScorerAppData> applications = getAllValidScorers(context);
        for (NetworkScorerAppData app : applications) {
            if (packageName.equals(app.mPackageName)) {
                return app;
            }
        }
        return null;
    }
}
