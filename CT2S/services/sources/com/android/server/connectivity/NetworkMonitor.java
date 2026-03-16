package com.android.server.connectivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.location.LocationFudger;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

public class NetworkMonitor extends StateMachine {
    private static final String ACTION_CAPTIVE_PORTAL_LOGGED_IN = "android.net.netmon.captive_portal_logged_in";
    public static final String ACTION_NETWORK_CONDITIONS_MEASURED = "android.net.conn.NETWORK_CONDITIONS_MEASURED";
    private static final int BASE = 532480;
    public static final int CAPTIVE_PORTAL_APP_RETURN_APPEASED = 0;
    public static final int CAPTIVE_PORTAL_APP_RETURN_UNWANTED = 1;
    public static final int CAPTIVE_PORTAL_APP_RETURN_WANTED_AS_IS = 2;
    private static final int CMD_CAPTIVE_PORTAL_APP_FINISHED = 532489;
    public static final int CMD_FORCE_REEVALUATION = 532488;
    private static final int CMD_LINGER_EXPIRED = 532484;
    public static final int CMD_NETWORK_CONNECTED = 532481;
    public static final int CMD_NETWORK_DISCONNECTED = 532487;
    public static final int CMD_NETWORK_LINGER = 532483;
    private static final int CMD_REEVALUATE = 532486;
    private static final boolean DBG = true;
    private static final int DEFAULT_LINGER_DELAY_MS = 30000;
    private static final int DEFAULT_REEVALUATE_DELAY_MS = 5000;
    private static final String DEFAULT_SERVER = "connectivitycheck.android.com";
    private static final int EVENT_APP_BYPASSED_CAPTIVE_PORTAL = 532491;
    private static final int EVENT_APP_INDICATES_SIGN_IN_IMPOSSIBLE = 532493;
    public static final int EVENT_NETWORK_LINGER_COMPLETE = 532485;
    public static final int EVENT_NETWORK_TESTED = 532482;
    private static final int EVENT_NO_APP_RESPONSE = 532492;
    public static final int EVENT_PROVISIONING_NOTIFICATION = 532490;
    public static final String EXTRA_BSSID = "extra_bssid";
    public static final String EXTRA_CELL_ID = "extra_cellid";
    public static final String EXTRA_CONNECTIVITY_TYPE = "extra_connectivity_type";
    public static final String EXTRA_IS_CAPTIVE_PORTAL = "extra_is_captive_portal";
    public static final String EXTRA_NETWORK_TYPE = "extra_network_type";
    public static final String EXTRA_REQUEST_TIMESTAMP_MS = "extra_request_timestamp_ms";
    public static final String EXTRA_RESPONSE_RECEIVED = "extra_response_received";
    public static final String EXTRA_RESPONSE_TIMESTAMP_MS = "extra_response_timestamp_ms";
    public static final String EXTRA_SSID = "extra_ssid";
    private static final int INITIAL_ATTEMPTS = 3;
    private static final int INVALID_UID = -1;
    private static final String LINGER_DELAY_PROPERTY = "persist.netmon.linger";
    private static final String LOGGED_IN_RESULT = "result";
    public static final int NETWORK_TEST_RESULT_INVALID = 1;
    public static final int NETWORK_TEST_RESULT_VALID = 0;
    private static final int PERIODIC_ATTEMPTS = 1;
    private static final String PERMISSION_ACCESS_NETWORK_CONDITIONS = "android.permission.ACCESS_NETWORK_CONDITIONS";
    private static final int REEVALUATE_ATTEMPTS = 1;
    private static final String REEVALUATE_DELAY_PROPERTY = "persist.netmon.reeval_delay";
    private static final int REEVALUATE_PAUSE_MS = 600000;
    private static final String RESPONSE_TOKEN = "response_token";
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final String TAG = "NetworkMonitor";
    private final AlarmManager mAlarmManager;
    private CaptivePortalLoggedInBroadcastReceiver mCaptivePortalLoggedInBroadcastReceiver;
    private String mCaptivePortalLoggedInResponseToken;
    private final State mCaptivePortalState;
    private final Handler mConnectivityServiceHandler;
    private final Context mContext;
    private final NetworkRequest mDefaultRequest;
    private final State mDefaultState;
    private final State mEvaluatingState;
    private boolean mIsCaptivePortalCheckEnabled;
    private final int mLingerDelayMs;
    private int mLingerToken;
    private final State mLingeringState;
    private int mMaxAttempts;
    private final State mMaybeNotifyState;
    private final NetworkAgentInfo mNetworkAgentInfo;
    private final State mOfflineState;
    private final int mReevaluateDelayMs;
    private int mReevaluateToken;
    private String mServer;
    private final TelephonyManager mTelephonyManager;
    private int mUidResponsibleForReeval;
    private boolean mUserDoesNotWant;
    private final State mValidatedState;
    private final WifiManager mWifiManager;
    public boolean systemReady;

    static int access$2704(NetworkMonitor x0) {
        int i = x0.mReevaluateToken + 1;
        x0.mReevaluateToken = i;
        return i;
    }

    public NetworkMonitor(Context context, Handler handler, NetworkAgentInfo networkAgentInfo, NetworkRequest defaultRequest) {
        super(TAG + networkAgentInfo.name());
        this.mLingerToken = 0;
        this.mReevaluateToken = 0;
        this.mUidResponsibleForReeval = -1;
        this.mIsCaptivePortalCheckEnabled = false;
        this.mUserDoesNotWant = false;
        this.systemReady = false;
        this.mDefaultState = new DefaultState();
        this.mOfflineState = new OfflineState();
        this.mValidatedState = new ValidatedState();
        this.mMaybeNotifyState = new MaybeNotifyState();
        this.mEvaluatingState = new EvaluatingState();
        this.mCaptivePortalState = new CaptivePortalState();
        this.mLingeringState = new LingeringState();
        this.mCaptivePortalLoggedInBroadcastReceiver = null;
        this.mCaptivePortalLoggedInResponseToken = null;
        this.mContext = context;
        this.mConnectivityServiceHandler = handler;
        this.mNetworkAgentInfo = networkAgentInfo;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mDefaultRequest = defaultRequest;
        addState(this.mDefaultState);
        addState(this.mOfflineState, this.mDefaultState);
        addState(this.mValidatedState, this.mDefaultState);
        addState(this.mMaybeNotifyState, this.mDefaultState);
        addState(this.mEvaluatingState, this.mMaybeNotifyState);
        addState(this.mCaptivePortalState, this.mMaybeNotifyState);
        addState(this.mLingeringState, this.mDefaultState);
        setInitialState(this.mDefaultState);
        this.mServer = Settings.Global.getString(this.mContext.getContentResolver(), "captive_portal_server");
        if (this.mServer == null) {
            this.mServer = DEFAULT_SERVER;
        }
        this.mLingerDelayMs = SystemProperties.getInt(LINGER_DELAY_PROPERTY, DEFAULT_LINGER_DELAY_MS);
        this.mReevaluateDelayMs = SystemProperties.getInt(REEVALUATE_DELAY_PROPERTY, DEFAULT_REEVALUATE_DELAY_MS);
        this.mIsCaptivePortalCheckEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "captive_portal_detection_enabled", SystemProperties.getInt("ro.conn.detection.enable", 0)) == 1;
        this.mCaptivePortalLoggedInResponseToken = String.valueOf(new Random().nextLong());
        start();
    }

    protected void log(String s) {
        Log.d("NetworkMonitor/" + this.mNetworkAgentInfo.name(), s);
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message message) {
            NetworkMonitor.this.log(getName() + message.toString());
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED:
                    NetworkMonitor.this.log("Connected");
                    NetworkMonitor.this.mMaxAttempts = 3;
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    return NetworkMonitor.DBG;
                case NetworkMonitor.EVENT_NETWORK_TESTED:
                case NetworkMonitor.CMD_LINGER_EXPIRED:
                case NetworkMonitor.EVENT_NETWORK_LINGER_COMPLETE:
                case NetworkMonitor.CMD_REEVALUATE:
                default:
                    return NetworkMonitor.DBG;
                case NetworkMonitor.CMD_NETWORK_LINGER:
                    NetworkMonitor.this.log("Lingering");
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mLingeringState);
                    return NetworkMonitor.DBG;
                case NetworkMonitor.CMD_NETWORK_DISCONNECTED:
                    NetworkMonitor.this.log("Disconnected - quitting");
                    if (NetworkMonitor.this.mCaptivePortalLoggedInBroadcastReceiver != null) {
                        NetworkMonitor.this.mContext.unregisterReceiver(NetworkMonitor.this.mCaptivePortalLoggedInBroadcastReceiver);
                        NetworkMonitor.this.mCaptivePortalLoggedInBroadcastReceiver = null;
                    }
                    NetworkMonitor.this.quit();
                    return NetworkMonitor.DBG;
                case NetworkMonitor.CMD_FORCE_REEVALUATION:
                    NetworkMonitor.this.log("Forcing reevaluation");
                    NetworkMonitor.this.mUidResponsibleForReeval = message.arg1;
                    NetworkMonitor.this.mMaxAttempts = message.arg2 != 0 ? message.arg2 : 1;
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    return NetworkMonitor.DBG;
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED:
                    NetworkMonitor.this.mCaptivePortalLoggedInResponseToken = String.valueOf(new Random().nextLong());
                    switch (message.arg1) {
                        case 0:
                        case 2:
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                            break;
                        case 1:
                            NetworkMonitor.this.mUserDoesNotWant = NetworkMonitor.DBG;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mOfflineState);
                            break;
                    }
                    break;
            }
        }
    }

    private class OfflineState extends State {
        private OfflineState() {
        }

        public void enter() {
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, 0, NetworkMonitor.this.mNetworkAgentInfo));
            if (!NetworkMonitor.this.mUserDoesNotWant) {
                NetworkMonitor.this.sendMessageDelayed(NetworkMonitor.CMD_FORCE_REEVALUATION, 0, 1, LocationFudger.FASTEST_INTERVAL_MS);
            }
        }

        public boolean processMessage(Message message) {
            NetworkMonitor.this.log(getName() + message.toString());
            switch (message.what) {
                case NetworkMonitor.CMD_FORCE_REEVALUATION:
                    if (NetworkMonitor.this.mUserDoesNotWant) {
                    }
                    break;
            }
            return false;
        }

        public void exit() {
            NetworkMonitor.this.removeMessages(NetworkMonitor.CMD_FORCE_REEVALUATION);
        }
    }

    private class ValidatedState extends State {
        private ValidatedState() {
        }

        public void enter() {
            NetworkMonitor.this.log("Validated");
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 0, 0, NetworkMonitor.this.mNetworkAgentInfo));
        }

        public boolean processMessage(Message message) {
            NetworkMonitor.this.log(getName() + message.toString());
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED:
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                    return NetworkMonitor.DBG;
                default:
                    return false;
            }
        }
    }

    private class MaybeNotifyState extends State {
        private MaybeNotifyState() {
        }

        public void exit() {
            Message message = NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 0, NetworkMonitor.this.mNetworkAgentInfo.network.netId, null);
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(message);
        }
    }

    private class EvaluatingState extends State {
        private int mAttempt;

        private EvaluatingState() {
        }

        public void enter() {
            this.mAttempt = 1;
            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_REEVALUATE, NetworkMonitor.access$2704(NetworkMonitor.this), 0);
            if (NetworkMonitor.this.mUidResponsibleForReeval != -1) {
                TrafficStats.setThreadStatsUid(NetworkMonitor.this.mUidResponsibleForReeval);
                NetworkMonitor.this.mUidResponsibleForReeval = -1;
            }
        }

        public boolean processMessage(Message message) {
            NetworkMonitor.this.log(getName() + message.toString());
            switch (message.what) {
                case NetworkMonitor.CMD_REEVALUATE:
                    if (message.arg1 != NetworkMonitor.this.mReevaluateToken) {
                        return NetworkMonitor.DBG;
                    }
                    if (!NetworkMonitor.this.mDefaultRequest.networkCapabilities.satisfiedByNetworkCapabilities(NetworkMonitor.this.mNetworkAgentInfo.networkCapabilities)) {
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                        return NetworkMonitor.DBG;
                    }
                    int httpResponseCode = NetworkMonitor.this.isCaptivePortal();
                    if (httpResponseCode == 204) {
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                        return NetworkMonitor.DBG;
                    }
                    if (httpResponseCode >= 200 && httpResponseCode <= 399) {
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mCaptivePortalState);
                        return NetworkMonitor.DBG;
                    }
                    int i = this.mAttempt + 1;
                    this.mAttempt = i;
                    if (i > NetworkMonitor.this.mMaxAttempts) {
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mOfflineState);
                        return NetworkMonitor.DBG;
                    }
                    if (NetworkMonitor.this.mReevaluateDelayMs < 0) {
                        return NetworkMonitor.DBG;
                    }
                    Message msg = NetworkMonitor.this.obtainMessage(NetworkMonitor.CMD_REEVALUATE, NetworkMonitor.access$2704(NetworkMonitor.this), 0);
                    NetworkMonitor.this.sendMessageDelayed(msg, NetworkMonitor.this.mReevaluateDelayMs);
                    return NetworkMonitor.DBG;
                case NetworkMonitor.CMD_NETWORK_DISCONNECTED:
                default:
                    return false;
                case NetworkMonitor.CMD_FORCE_REEVALUATION:
                    return NetworkMonitor.DBG;
            }
        }

        public void exit() {
            TrafficStats.clearThreadStatsUid();
        }
    }

    private class CustomIntentReceiver extends BroadcastReceiver {
        private final String mAction;
        private final int mToken;
        private final int mWhat;

        CustomIntentReceiver(String action, int token, int what) {
            this.mToken = token;
            this.mWhat = what;
            this.mAction = action + "_" + NetworkMonitor.this.mNetworkAgentInfo.network.netId + "_" + token;
            NetworkMonitor.this.mContext.registerReceiver(this, new IntentFilter(this.mAction));
        }

        public PendingIntent getPendingIntent() {
            return PendingIntent.getBroadcast(NetworkMonitor.this.mContext, 0, new Intent(this.mAction), 0);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(this.mAction)) {
                NetworkMonitor.this.sendMessage(NetworkMonitor.this.obtainMessage(this.mWhat, this.mToken));
            }
        }
    }

    private class CaptivePortalLoggedInBroadcastReceiver extends BroadcastReceiver {
        private CaptivePortalLoggedInBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Integer.parseInt(intent.getStringExtra("android.intent.extra.TEXT")) == NetworkMonitor.this.mNetworkAgentInfo.network.netId && NetworkMonitor.this.mCaptivePortalLoggedInResponseToken.equals(intent.getStringExtra(NetworkMonitor.RESPONSE_TOKEN))) {
                NetworkMonitor.this.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED, Integer.parseInt(intent.getStringExtra(NetworkMonitor.LOGGED_IN_RESULT)), 0));
            }
        }
    }

    private class CaptivePortalState extends State {
        private CaptivePortalState() {
        }

        public void enter() {
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, 0, NetworkMonitor.this.mNetworkAgentInfo));
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setData(Uri.fromParts("netid", Integer.toString(NetworkMonitor.this.mNetworkAgentInfo.network.netId), NetworkMonitor.this.mCaptivePortalLoggedInResponseToken));
            intent.setComponent(new ComponentName("com.android.captiveportallogin", "com.android.captiveportallogin.CaptivePortalLoginActivity"));
            intent.setFlags(272629760);
            if (NetworkMonitor.this.mCaptivePortalLoggedInBroadcastReceiver == null) {
                NetworkMonitor.this.mCaptivePortalLoggedInBroadcastReceiver = new CaptivePortalLoggedInBroadcastReceiver();
                IntentFilter filter = new IntentFilter(NetworkMonitor.ACTION_CAPTIVE_PORTAL_LOGGED_IN);
                NetworkMonitor.this.mContext.registerReceiver(NetworkMonitor.this.mCaptivePortalLoggedInBroadcastReceiver, filter);
            }
            Message message = NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 1, NetworkMonitor.this.mNetworkAgentInfo.network.netId, PendingIntent.getActivity(NetworkMonitor.this.mContext, 0, intent, 0));
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(message);
        }

        public boolean processMessage(Message message) {
            NetworkMonitor.this.log(getName() + message.toString());
            return false;
        }
    }

    private class LingeringState extends State {
        private static final String ACTION_LINGER_EXPIRED = "android.net.netmon.lingerExpired";
        private CustomIntentReceiver mBroadcastReceiver;
        private PendingIntent mIntent;

        private LingeringState() {
        }

        public void enter() {
            NetworkMonitor.this.mLingerToken = new Random().nextInt();
            this.mBroadcastReceiver = NetworkMonitor.this.new CustomIntentReceiver(ACTION_LINGER_EXPIRED, NetworkMonitor.this.mLingerToken, NetworkMonitor.CMD_LINGER_EXPIRED);
            this.mIntent = this.mBroadcastReceiver.getPendingIntent();
            long wakeupTime = SystemClock.elapsedRealtime() + ((long) NetworkMonitor.this.mLingerDelayMs);
            NetworkMonitor.this.mAlarmManager.setWindow(2, wakeupTime, NetworkMonitor.this.mLingerDelayMs / 6, this.mIntent);
        }

        public boolean processMessage(Message message) {
            NetworkMonitor.this.log(getName() + message.toString());
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED:
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                    break;
                case NetworkMonitor.CMD_LINGER_EXPIRED:
                    if (message.arg1 == NetworkMonitor.this.mLingerToken) {
                        NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_LINGER_COMPLETE, NetworkMonitor.this.mNetworkAgentInfo));
                    }
                    break;
            }
            return NetworkMonitor.DBG;
        }

        public void exit() {
            NetworkMonitor.this.mAlarmManager.cancel(this.mIntent);
            NetworkMonitor.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        }
    }

    private int isCaptivePortal() {
        int dcha_state = BenesseExtension.getDchaState();
        if (dcha_state != 0 || !this.mIsCaptivePortalCheckEnabled) {
            return 204;
        }
        HttpURLConnection urlConnection = null;
        int httpResponseCode = 599;
        try {
            try {
                URL url = new URL("http", this.mServer, "/generate_204");
                boolean fetchPac = false;
                ProxyInfo proxyInfo = this.mNetworkAgentInfo.linkProperties.getHttpProxy();
                if (proxyInfo != null && !Uri.EMPTY.equals(proxyInfo.getPacFileUrl())) {
                    url = new URL(proxyInfo.getPacFileUrl().toString());
                    fetchPac = DBG;
                }
                log("Checking " + url.toString() + " on " + this.mNetworkAgentInfo.networkInfo.getExtraInfo());
                urlConnection = (HttpURLConnection) this.mNetworkAgentInfo.network.openConnection(url);
                urlConnection.setInstanceFollowRedirects(fetchPac);
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);
                urlConnection.setUseCaches(false);
                long requestTimestamp = SystemClock.elapsedRealtime();
                urlConnection.getInputStream();
                long responseTimestamp = SystemClock.elapsedRealtime();
                httpResponseCode = urlConnection.getResponseCode();
                log("isCaptivePortal: ret=" + httpResponseCode + " headers=" + urlConnection.getHeaderFields());
                if (httpResponseCode == 200 && urlConnection.getContentLength() == 0) {
                    log("Empty 200 response interpreted as 204 response.");
                    httpResponseCode = 204;
                }
                if (httpResponseCode == 200 && fetchPac) {
                    log("PAC fetch 200 response interpreted as 204 response.");
                    httpResponseCode = 204;
                }
                sendNetworkConditionsBroadcast(DBG, httpResponseCode != 204 ? DBG : false, requestTimestamp, responseTimestamp);
                if (urlConnection != null) {
                    urlConnection.disconnect();
                    return httpResponseCode;
                }
                return httpResponseCode;
            } catch (IOException e) {
                log("Probably not a portal: exception " + e);
                if (httpResponseCode == 599) {
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                    return httpResponseCode;
                }
                return httpResponseCode;
            }
        } catch (Throwable th) {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            throw th;
        }
    }

    private void sendNetworkConditionsBroadcast(boolean responseReceived, boolean isCaptivePortal, long requestTimestampMs, long responseTimestampMs) {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 0) {
            log("Don't send network conditions - lacking user consent.");
            return;
        }
        if (this.systemReady) {
            Intent latencyBroadcast = new Intent(ACTION_NETWORK_CONDITIONS_MEASURED);
            switch (this.mNetworkAgentInfo.networkInfo.getType()) {
                case 0:
                    latencyBroadcast.putExtra(EXTRA_NETWORK_TYPE, this.mTelephonyManager.getNetworkType());
                    List<CellInfo> info = this.mTelephonyManager.getAllCellInfo();
                    if (info != null) {
                        int numRegisteredCellInfo = 0;
                        for (CellInfo cellInfo : info) {
                            if (cellInfo.isRegistered()) {
                                numRegisteredCellInfo++;
                                if (numRegisteredCellInfo > 1) {
                                    log("more than one registered CellInfo.  Can't tell which is active.  Bailing.");
                                    return;
                                }
                                if (cellInfo instanceof CellInfoCdma) {
                                    CellIdentityCdma cellId = ((CellInfoCdma) cellInfo).getCellIdentity();
                                    latencyBroadcast.putExtra(EXTRA_CELL_ID, cellId);
                                } else if (cellInfo instanceof CellInfoGsm) {
                                    CellIdentityGsm cellId2 = ((CellInfoGsm) cellInfo).getCellIdentity();
                                    latencyBroadcast.putExtra(EXTRA_CELL_ID, cellId2);
                                } else if (cellInfo instanceof CellInfoLte) {
                                    CellIdentityLte cellId3 = ((CellInfoLte) cellInfo).getCellIdentity();
                                    latencyBroadcast.putExtra(EXTRA_CELL_ID, cellId3);
                                } else if (cellInfo instanceof CellInfoWcdma) {
                                    CellIdentityWcdma cellId4 = ((CellInfoWcdma) cellInfo).getCellIdentity();
                                    latencyBroadcast.putExtra(EXTRA_CELL_ID, cellId4);
                                } else {
                                    logw("Registered cellinfo is unrecognized");
                                    return;
                                }
                            }
                        }
                    } else {
                        return;
                    }
                    break;
                case 1:
                    WifiInfo currentWifiInfo = this.mWifiManager.getConnectionInfo();
                    if (currentWifiInfo != null) {
                        latencyBroadcast.putExtra(EXTRA_SSID, currentWifiInfo.getSSID());
                        latencyBroadcast.putExtra(EXTRA_BSSID, currentWifiInfo.getBSSID());
                    } else {
                        logw("network info is TYPE_WIFI but no ConnectionInfo found");
                        return;
                    }
                    break;
                default:
                    return;
            }
            latencyBroadcast.putExtra(EXTRA_CONNECTIVITY_TYPE, this.mNetworkAgentInfo.networkInfo.getType());
            latencyBroadcast.putExtra(EXTRA_RESPONSE_RECEIVED, responseReceived);
            latencyBroadcast.putExtra(EXTRA_REQUEST_TIMESTAMP_MS, requestTimestampMs);
            if (responseReceived) {
                latencyBroadcast.putExtra(EXTRA_IS_CAPTIVE_PORTAL, isCaptivePortal);
                latencyBroadcast.putExtra(EXTRA_RESPONSE_TIMESTAMP_MS, responseTimestampMs);
            }
            this.mContext.sendBroadcastAsUser(latencyBroadcast, UserHandle.CURRENT, PERMISSION_ACCESS_NETWORK_CONDITIONS);
        }
    }
}
