package com.android.server.connectivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.CaptivePortal;
import android.net.ICaptivePortal;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.metrics.NetworkEvent;
import android.net.metrics.ValidationProbeEvent;
import android.net.util.Stopwatch;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
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
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.location.LocationFudger;
import com.mediatek.internal.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkMonitor extends StateMachine {
    public static final String ACTION_NETWORK_CONDITIONS_MEASURED = "android.net.conn.NETWORK_CONDITIONS_MEASURED";
    private static final int BASE = 532480;
    private static final int BLAME_FOR_EVALUATION_ATTEMPTS = 5;
    private static final int CAPTIVE_PORTAL_REEVALUATE_DELAY_MS = 600000;
    private static final int CMD_CAPTIVE_PORTAL_APP_FINISHED = 532489;
    private static final int CMD_CAPTIVE_PORTAL_RECHECK = 532492;
    public static final int CMD_FORCE_REEVALUATION = 532488;
    private static final int CMD_LAUNCH_CAPTIVE_PORTAL_APP = 532491;
    private static final int CMD_LINGER_EXPIRED = 532484;
    public static final int CMD_NETWORK_CONNECTED = 532481;
    public static final int CMD_NETWORK_DISCONNECTED = 532487;
    public static final int CMD_NETWORK_LINGER = 532483;
    private static final int CMD_REEVALUATE = 532486;
    private static final boolean DBG = true;
    private static final String DEFAULT_SERVER = "connectivitycheck.gstatic.com";
    private static final String DEFAULT_SERVER_SECONDARY = "captive.apple.com";
    public static final int EVENT_NETWORK_LINGER_COMPLETE = 532485;
    public static final int EVENT_NETWORK_TESTED = 532482;
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
    private static final int IGNORE_REEVALUATE_ATTEMPTS = 5;
    private static final int INITIAL_REEVALUATE_DELAY_MS = 1000;
    private static final int INVALID_UID = -1;
    private static final String LINGER_DELAY_PROPERTY = "persist.netmon.linger";
    private static final int MAX_REEVALUATE_DELAY_MS = 600000;
    public static final int NETWORK_TEST_RESULT_INVALID = 1;
    public static final int NETWORK_TEST_RESULT_VALID = 0;
    private static final String PERMISSION_ACCESS_NETWORK_CONDITIONS = "android.permission.ACCESS_NETWORK_CONDITIONS";
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private final AlarmManager mAlarmManager;
    private final State mCaptivePortalState;
    private final Handler mConnectivityServiceHandler;
    private final Context mContext;
    private final NetworkRequest mDefaultRequest;
    private final State mDefaultState;
    private boolean mDontDisplaySigninNotification;
    private final State mEvaluatingState;
    private final Stopwatch mEvaluationTimer;
    private boolean mIsCaptivePortalCheckEnabled;
    private CustomIntentReceiver mLaunchCaptivePortalAppBroadcastReceiver;
    private final int mLingerDelayMs;
    private int mLingerToken;
    private final State mLingeringState;
    private final State mMaybeNotifyState;
    private final int mNetId;
    private final NetworkAgentInfo mNetworkAgentInfo;
    private int mReevaluateToken;
    private final TelephonyManager mTelephonyManager;
    private int mUidResponsibleForReeval;
    private boolean mUseHttps;
    private boolean mUserDoesNotWant;
    private final State mValidatedState;
    private final WifiManager mWifiManager;
    public boolean systemReady;
    private final LocalLog validationLogs;
    private static final String TAG = NetworkMonitor.class.getSimpleName();
    private static int DEFAULT_LINGER_DELAY_MS = 30000;
    private static boolean mSkipNetworkValidation = true;

    public NetworkMonitor(Context context, Handler handler, NetworkAgentInfo networkAgentInfo, NetworkRequest networkRequest) {
        super(TAG + networkAgentInfo.name());
        this.mLingerToken = 0;
        this.mReevaluateToken = 0;
        this.mUidResponsibleForReeval = -1;
        this.mUserDoesNotWant = false;
        this.mDontDisplaySigninNotification = false;
        this.systemReady = false;
        this.mDefaultState = new DefaultState(this, null);
        this.mValidatedState = new ValidatedState(this, 0 == true ? 1 : 0);
        this.mMaybeNotifyState = new MaybeNotifyState(this, 0 == true ? 1 : 0);
        this.mEvaluatingState = new EvaluatingState(this, 0 == true ? 1 : 0);
        this.mCaptivePortalState = new CaptivePortalState(this, 0 == true ? 1 : 0);
        this.mLingeringState = new LingeringState(this, 0 == true ? 1 : 0);
        this.mLaunchCaptivePortalAppBroadcastReceiver = null;
        this.validationLogs = new LocalLog(20);
        this.mEvaluationTimer = new Stopwatch();
        this.mContext = context;
        this.mConnectivityServiceHandler = handler;
        this.mNetworkAgentInfo = networkAgentInfo;
        this.mNetId = this.mNetworkAgentInfo.network.netId;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mDefaultRequest = networkRequest;
        addState(this.mDefaultState);
        addState(this.mValidatedState, this.mDefaultState);
        addState(this.mMaybeNotifyState, this.mDefaultState);
        addState(this.mEvaluatingState, this.mMaybeNotifyState);
        addState(this.mCaptivePortalState, this.mMaybeNotifyState);
        addState(this.mLingeringState, this.mDefaultState);
        setInitialState(this.mDefaultState);
        this.mLingerDelayMs = SystemProperties.getInt(LINGER_DELAY_PROPERTY, DEFAULT_LINGER_DELAY_MS);
        this.mIsCaptivePortalCheckEnabled = false;
        mSkipNetworkValidation = this.mContext.getResources().getBoolean(R.bool.config_skip_network_validation);
        this.mUseHttps = Settings.Global.getInt(this.mContext.getContentResolver(), "captive_portal_use_https", 0) == 1;
        start();
    }

    protected void log(String s) {
        Log.d(TAG + "/" + this.mNetworkAgentInfo.name(), s);
    }

    private void validationLog(String s) {
        log(s);
        this.validationLogs.log(s);
    }

    public LocalLog.ReadOnlyLocalLog getValidationLogs() {
        return this.validationLogs.readOnlyLocalLog();
    }

    private class DefaultState extends State {
        DefaultState(NetworkMonitor this$0, DefaultState defaultState) {
            this();
        }

        private DefaultState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED:
                    NetworkEvent.logEvent(NetworkMonitor.this.mNetId, 1);
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    break;
                case NetworkMonitor.CMD_NETWORK_LINGER:
                    NetworkMonitor.this.log("Lingering");
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mLingeringState);
                    break;
                case NetworkMonitor.CMD_NETWORK_DISCONNECTED:
                    NetworkEvent.logEvent(NetworkMonitor.this.mNetId, 7);
                    if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver != null) {
                        NetworkMonitor.this.mContext.unregisterReceiver(NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver);
                        NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver = null;
                    }
                    NetworkMonitor.this.quit();
                    break;
                case NetworkMonitor.CMD_FORCE_REEVALUATION:
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK:
                    NetworkMonitor.this.log("Forcing reevaluation for UID " + message.arg1);
                    NetworkMonitor.this.mUidResponsibleForReeval = message.arg1;
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    break;
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED:
                    NetworkMonitor.this.log("CaptivePortal App responded with " + message.arg1);
                    NetworkMonitor.this.mUseHttps = false;
                    switch (message.arg1) {
                        case 0:
                            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_FORCE_REEVALUATION, 0, 0);
                            break;
                        case 1:
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor.this.mUserDoesNotWant = true;
                            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, NetworkMonitor.this.mNetId, null));
                            NetworkMonitor.this.mUidResponsibleForReeval = 0;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                            break;
                        case 2:
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                            break;
                    }
                    break;
            }
            return true;
        }
    }

    private class ValidatedState extends State {
        ValidatedState(NetworkMonitor this$0, ValidatedState validatedState) {
            this();
        }

        private ValidatedState() {
        }

        public void enter() {
            if (NetworkMonitor.this.mEvaluationTimer.isRunning()) {
                NetworkEvent.logValidated(NetworkMonitor.this.mNetId, NetworkMonitor.this.mEvaluationTimer.stop());
                NetworkMonitor.this.mEvaluationTimer.reset();
            }
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 0, NetworkMonitor.this.mNetworkAgentInfo.network.netId, null));
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED:
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                    return true;
                default:
                    return false;
            }
        }
    }

    private class MaybeNotifyState extends State {
        MaybeNotifyState(NetworkMonitor this$0, MaybeNotifyState maybeNotifyState) {
            this();
        }

        private MaybeNotifyState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_LAUNCH_CAPTIVE_PORTAL_APP:
                    Intent intent = new Intent("android.net.conn.CAPTIVE_PORTAL");
                    intent.putExtra("android.net.extra.NETWORK", NetworkMonitor.this.mNetworkAgentInfo.network);
                    intent.putExtra("android.net.extra.CAPTIVE_PORTAL", new CaptivePortal(new ICaptivePortal.Stub() {
                        public void appResponse(int response) {
                            if (response == 2) {
                                NetworkMonitor.this.mContext.enforceCallingPermission("android.permission.CONNECTIVITY_INTERNAL", "CaptivePortal");
                            }
                            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED, response);
                        }
                    }));
                    intent.setFlags(272629760);
                    NetworkMonitor.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            Message message = NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 0, NetworkMonitor.this.mNetworkAgentInfo.network.netId, null);
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(message);
        }
    }

    public static final class CaptivePortalProbeResult {
        static final CaptivePortalProbeResult FAILED = new CaptivePortalProbeResult(599, null);
        final int mHttpResponseCode;
        final String mRedirectUrl;

        public CaptivePortalProbeResult(int httpResponseCode, String redirectUrl) {
            this.mHttpResponseCode = httpResponseCode;
            this.mRedirectUrl = redirectUrl;
        }

        boolean isSuccessful() {
            return this.mHttpResponseCode == 204;
        }

        boolean isPortal() {
            return !isSuccessful() && this.mHttpResponseCode >= 200 && this.mHttpResponseCode <= 399;
        }
    }

    private class EvaluatingState extends State {
        private int mAttempts;
        private int mReevaluateDelayMs;

        EvaluatingState(NetworkMonitor this$0, EvaluatingState evaluatingState) {
            this();
        }

        private EvaluatingState() {
        }

        public void enter() {
            if (!NetworkMonitor.this.mEvaluationTimer.isStarted()) {
                NetworkMonitor.this.mEvaluationTimer.start();
            }
            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_REEVALUATE, NetworkMonitor.this.mReevaluateToken++, 0);
            if (NetworkMonitor.this.mUidResponsibleForReeval != -1) {
                TrafficStats.setThreadStatsUid(NetworkMonitor.this.mUidResponsibleForReeval);
                NetworkMonitor.this.mUidResponsibleForReeval = -1;
            }
            this.mReevaluateDelayMs = 1000;
            this.mAttempts = 0;
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_REEVALUATE:
                    if (message.arg1 != NetworkMonitor.this.mReevaluateToken || NetworkMonitor.this.mUserDoesNotWant) {
                        return true;
                    }
                    if (!NetworkMonitor.this.mDefaultRequest.networkCapabilities.satisfiedByNetworkCapabilities(NetworkMonitor.this.mNetworkAgentInfo.networkCapabilities)) {
                        NetworkMonitor.this.validationLog("Network would not satisfy default request, not validating");
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                        return true;
                    }
                    if (SystemProperties.getInt("gsm.sim.ril.testsim", 0) == 1 || SystemProperties.getInt("gsm.sim.ril.testsim.2", 0) == 1) {
                        NetworkMonitor.this.log("test sim enabled");
                        boolean unused = NetworkMonitor.mSkipNetworkValidation = true;
                    }
                    if (NetworkMonitor.this.mNetworkAgentInfo.networkCapabilities.hasTransport(0) && NetworkMonitor.mSkipNetworkValidation) {
                        NetworkMonitor.this.log("consider Mobile validated directly");
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                        return true;
                    }
                    this.mAttempts++;
                    CaptivePortalProbeResult probeResult = NetworkMonitor.this.isCaptivePortal();
                    if (probeResult.isSuccessful()) {
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                    } else if (probeResult.isPortal()) {
                        NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, NetworkMonitor.this.mNetId, probeResult.mRedirectUrl));
                        NetworkMonitor.this.transitionTo(NetworkMonitor.this.mCaptivePortalState);
                    } else {
                        if (NetworkMonitor.this.mNetworkAgentInfo.networkCapabilities.hasTransport(1) && NetworkMonitor.mSkipNetworkValidation) {
                            NetworkMonitor.this.log("consider Wi-Fi validated directly");
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                            return true;
                        }
                        Message msg = NetworkMonitor.this.obtainMessage(NetworkMonitor.CMD_REEVALUATE, NetworkMonitor.this.mReevaluateToken++, 0);
                        NetworkMonitor.this.sendMessageDelayed(msg, this.mReevaluateDelayMs);
                        NetworkEvent.logEvent(NetworkMonitor.this.mNetId, 3);
                        NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, NetworkMonitor.this.mNetId, probeResult.mRedirectUrl));
                        if (this.mAttempts >= 5) {
                            TrafficStats.clearThreadStatsUid();
                        }
                        this.mReevaluateDelayMs *= 2;
                        if (this.mReevaluateDelayMs > 600000) {
                            this.mReevaluateDelayMs = 600000;
                        }
                    }
                    return true;
                case NetworkMonitor.CMD_NETWORK_DISCONNECTED:
                default:
                    return false;
                case NetworkMonitor.CMD_FORCE_REEVALUATION:
                    return this.mAttempts < 5;
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
            Intent intent = new Intent(this.mAction);
            intent.setPackage(NetworkMonitor.this.mContext.getPackageName());
            return PendingIntent.getBroadcast(NetworkMonitor.this.mContext, 0, intent, 0);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(this.mAction)) {
                NetworkMonitor.this.sendMessage(NetworkMonitor.this.obtainMessage(this.mWhat, this.mToken));
            }
        }
    }

    private class CaptivePortalState extends State {
        private static final String ACTION_LAUNCH_CAPTIVE_PORTAL_APP = "android.net.netmon.launchCaptivePortalApp";

        CaptivePortalState(NetworkMonitor this$0, CaptivePortalState captivePortalState) {
            this();
        }

        private CaptivePortalState() {
        }

        public void enter() {
            if (NetworkMonitor.this.mEvaluationTimer.isRunning()) {
                NetworkEvent.logCaptivePortalFound(NetworkMonitor.this.mNetId, NetworkMonitor.this.mEvaluationTimer.stop());
                NetworkMonitor.this.mEvaluationTimer.reset();
            }
            if (NetworkMonitor.this.mDontDisplaySigninNotification) {
                return;
            }
            if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver == null) {
                NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver = NetworkMonitor.this.new CustomIntentReceiver(ACTION_LAUNCH_CAPTIVE_PORTAL_APP, new Random().nextInt(), NetworkMonitor.CMD_LAUNCH_CAPTIVE_PORTAL_APP);
            }
            Message message = NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 1, NetworkMonitor.this.mNetworkAgentInfo.network.netId, NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver.getPendingIntent());
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(message);
            NetworkMonitor.this.sendMessageDelayed(NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK, 0, LocationFudger.FASTEST_INTERVAL_MS);
        }

        public void exit() {
            NetworkMonitor.this.removeMessages(NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK);
        }
    }

    private class LingeringState extends State {
        private static final String ACTION_LINGER_EXPIRED = "android.net.netmon.lingerExpired";
        private WakeupMessage mWakeupMessage;

        LingeringState(NetworkMonitor this$0, LingeringState lingeringState) {
            this();
        }

        private LingeringState() {
        }

        public void enter() {
            NetworkMonitor.this.mEvaluationTimer.reset();
            String cmdName = "android.net.netmon.lingerExpired." + NetworkMonitor.this.mNetId;
            this.mWakeupMessage = NetworkMonitor.this.makeWakeupMessage(NetworkMonitor.this.mContext, NetworkMonitor.this.getHandler(), cmdName, NetworkMonitor.CMD_LINGER_EXPIRED);
            long wakeupTime = SystemClock.elapsedRealtime() + ((long) NetworkMonitor.this.mLingerDelayMs);
            this.mWakeupMessage.schedule(wakeupTime);
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED:
                    NetworkMonitor.this.log("Unlingered");
                    if (!NetworkMonitor.this.mNetworkAgentInfo.lastValidated) {
                        return false;
                    }
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                    return true;
                case NetworkMonitor.EVENT_NETWORK_TESTED:
                case NetworkMonitor.CMD_NETWORK_LINGER:
                case NetworkMonitor.EVENT_NETWORK_LINGER_COMPLETE:
                case NetworkMonitor.CMD_REEVALUATE:
                case NetworkMonitor.CMD_NETWORK_DISCONNECTED:
                default:
                    return false;
                case NetworkMonitor.CMD_LINGER_EXPIRED:
                    NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_LINGER_COMPLETE, NetworkMonitor.this.mNetworkAgentInfo));
                    return true;
                case NetworkMonitor.CMD_FORCE_REEVALUATION:
                    return true;
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED:
                    return true;
            }
        }

        public void exit() {
            this.mWakeupMessage.cancel();
        }
    }

    private static String getCaptivePortalServerUrl(Context context, boolean isHttps) {
        String server = Settings.Global.getString(context.getContentResolver(), "captive_portal_server");
        if (server == null) {
            server = DEFAULT_SERVER_SECONDARY;
        }
        return (isHttps ? "https" : "http") + "://" + server + "/generate_204";
    }

    public static String getCaptivePortalServerUrl(Context context) {
        return getCaptivePortalServerUrl(context, false);
    }

    protected CaptivePortalProbeResult isCaptivePortal() {
        String hostToResolve;
        CaptivePortalProbeResult result;
        if (BenesseExtension.getDchaState() == 0 && this.mIsCaptivePortalCheckEnabled) {
            URL pacUrl = null;
            URL url = null;
            URL httpsUrl = null;
            ProxyInfo proxyInfo = this.mNetworkAgentInfo.linkProperties.getHttpProxy();
            if (proxyInfo != null && !Uri.EMPTY.equals(proxyInfo.getPacFileUrl())) {
                try {
                    pacUrl = new URL(proxyInfo.getPacFileUrl().toString());
                } catch (MalformedURLException e) {
                    validationLog("Invalid PAC URL: " + proxyInfo.getPacFileUrl().toString());
                    return CaptivePortalProbeResult.FAILED;
                }
            }
            if (pacUrl == null) {
                try {
                    URL httpUrl = new URL(getCaptivePortalServerUrl(this.mContext, false));
                    try {
                        httpsUrl = new URL(getCaptivePortalServerUrl(this.mContext, true));
                        url = httpUrl;
                    } catch (MalformedURLException e2) {
                        validationLog("Bad validation URL: " + getCaptivePortalServerUrl(this.mContext, false));
                        return CaptivePortalProbeResult.FAILED;
                    }
                } catch (MalformedURLException e3) {
                }
            }
            long startTime = SystemClock.elapsedRealtime();
            if (pacUrl != null) {
                hostToResolve = pacUrl.getHost();
            } else if (proxyInfo != null) {
                hostToResolve = proxyInfo.getHost();
            } else {
                hostToResolve = url.getHost();
            }
            if (!TextUtils.isEmpty(hostToResolve)) {
                String probeName = ValidationProbeEvent.getProbeName(0);
                Stopwatch dnsTimer = new Stopwatch().start();
                try {
                    InetAddress[] addresses = this.mNetworkAgentInfo.network.getAllByName(hostToResolve);
                    long dnsLatency = dnsTimer.stop();
                    ValidationProbeEvent.logEvent(this.mNetId, dnsLatency, 0, 1);
                    StringBuffer connectInfo = new StringBuffer(", " + hostToResolve + "=");
                    for (InetAddress address : addresses) {
                        connectInfo.append(address.getHostAddress());
                        if (address != addresses[addresses.length - 1]) {
                            connectInfo.append(",");
                        }
                    }
                    validationLog(probeName + " OK " + dnsLatency + "ms" + ((Object) connectInfo));
                } catch (UnknownHostException e4) {
                    long dnsLatency2 = dnsTimer.stop();
                    ValidationProbeEvent.logEvent(this.mNetId, dnsLatency2, 0, 0);
                    validationLog(probeName + " FAIL " + dnsLatency2 + "ms, " + hostToResolve);
                }
            }
            if (pacUrl != null) {
                result = sendHttpProbe(pacUrl, 3);
            } else if (this.mUseHttps) {
                result = sendParallelHttpProbes(httpsUrl, url);
            } else {
                result = sendHttpProbe(url, 1);
            }
            long endTime = SystemClock.elapsedRealtime();
            sendNetworkConditionsBroadcast(true, result.isPortal(), startTime, endTime);
            return result;
        }
        return new CaptivePortalProbeResult(204, null);
    }

    protected CaptivePortalProbeResult sendHttpProbe(URL url, int probeType) {
        HttpURLConnection urlConnection = null;
        int httpResponseCode = 599;
        String redirectUrl = null;
        Stopwatch probeTimer = new Stopwatch().start();
        try {
            try {
                urlConnection = (HttpURLConnection) this.mNetworkAgentInfo.network.openConnection(url);
                urlConnection.setInstanceFollowRedirects(probeType == 3);
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(10000);
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("Connection", "Close");
                long requestTimestamp = SystemClock.elapsedRealtime();
                httpResponseCode = urlConnection.getResponseCode();
                redirectUrl = urlConnection.getHeaderField("location");
                long responseTimestamp = SystemClock.elapsedRealtime();
                validationLog(ValidationProbeEvent.getProbeName(probeType) + " " + url + " time=" + (responseTimestamp - requestTimestamp) + "ms ret=" + httpResponseCode + " headers=" + urlConnection.getHeaderFields());
                if (httpResponseCode == 200 && urlConnection.getContentLength() == 0) {
                    validationLog("Empty 200 response interpreted as 204 response.");
                    httpResponseCode = 204;
                }
                if (httpResponseCode == 200 && probeType == 3) {
                    validationLog("PAC fetch 200 response interpreted as 204 response.");
                    httpResponseCode = 204;
                }
                String contentType = urlConnection.getContentType();
                if (contentType == null) {
                    log("contentType is null, httpResponseCode = " + httpResponseCode);
                } else if (contentType.contains("text/html")) {
                    InputStreamReader in = new InputStreamReader((InputStream) urlConnection.getContent());
                    BufferedReader buff = new BufferedReader(in);
                    String line = buff.readLine();
                    validationLog("urlConnection.getContent() = " + line);
                    if (httpResponseCode == 200 && line.contains("Success")) {
                        httpResponseCode = 204;
                        log("Internet detected!");
                    }
                }
                sendNetworkConditionsBroadcast(true, httpResponseCode != 204, requestTimestamp, responseTimestamp);
            } catch (IOException e) {
                validationLog("Probably not a portal: exception " + e);
                if (httpResponseCode == 599) {
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            ValidationProbeEvent.logEvent(this.mNetId, probeTimer.stop(), probeType, httpResponseCode);
            return new CaptivePortalProbeResult(httpResponseCode, redirectUrl);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private CaptivePortalProbeResult sendParallelHttpProbes(URL httpsUrl, URL httpUrl) {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<CaptivePortalProbeResult> finalResult = new AtomicReference<>();
        ?? r0 = new Thread(true, httpsUrl, httpUrl, finalResult, latch) {
            private final boolean mIsHttps;
            private volatile CaptivePortalProbeResult mResult;
            final AtomicReference val$finalResult;
            final URL val$httpUrl;
            final URL val$httpsUrl;
            final CountDownLatch val$latch;

            {
                this.val$httpsUrl = httpsUrl;
                this.val$httpUrl = httpUrl;
                this.val$finalResult = finalResult;
                this.val$latch = latch;
                this.mIsHttps = isHttps;
            }

            public CaptivePortalProbeResult getResult() {
                return this.mResult;
            }

            @Override
            public void run() {
                if (this.mIsHttps) {
                    this.mResult = NetworkMonitor.this.sendHttpProbe(this.val$httpsUrl, 2);
                } else {
                    this.mResult = NetworkMonitor.this.sendHttpProbe(this.val$httpUrl, 1);
                }
                if ((this.mIsHttps && this.mResult.isSuccessful()) || (!this.mIsHttps && this.mResult.isPortal())) {
                    this.val$finalResult.compareAndSet(null, this.mResult);
                    this.val$latch.countDown();
                }
                this.val$latch.countDown();
            }
        };
        ?? r7 = new Thread(false, httpsUrl, httpUrl, finalResult, latch) {
            private final boolean mIsHttps;
            private volatile CaptivePortalProbeResult mResult;
            final AtomicReference val$finalResult;
            final URL val$httpUrl;
            final URL val$httpsUrl;
            final CountDownLatch val$latch;

            {
                this.val$httpsUrl = httpsUrl;
                this.val$httpUrl = httpUrl;
                this.val$finalResult = finalResult;
                this.val$latch = latch;
                this.mIsHttps = isHttps;
            }

            public CaptivePortalProbeResult getResult() {
                return this.mResult;
            }

            @Override
            public void run() {
                if (this.mIsHttps) {
                    this.mResult = NetworkMonitor.this.sendHttpProbe(this.val$httpsUrl, 2);
                } else {
                    this.mResult = NetworkMonitor.this.sendHttpProbe(this.val$httpUrl, 1);
                }
                if ((this.mIsHttps && this.mResult.isSuccessful()) || (!this.mIsHttps && this.mResult.isPortal())) {
                    this.val$finalResult.compareAndSet(null, this.mResult);
                    this.val$latch.countDown();
                }
                this.val$latch.countDown();
            }
        };
        r0.start();
        r7.start();
        try {
            latch.await();
            finalResult.compareAndSet(null, r0.getResult());
            return finalResult.get();
        } catch (InterruptedException e) {
            validationLog("Error: probe wait interrupted!");
            return CaptivePortalProbeResult.FAILED;
        }
    }

    private void sendNetworkConditionsBroadcast(boolean responseReceived, boolean isCaptivePortal, long requestTimestampMs, long responseTimestampMs) {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) != 0 && this.systemReady) {
            Intent latencyBroadcast = new Intent(ACTION_NETWORK_CONDITIONS_MEASURED);
            switch (this.mNetworkAgentInfo.networkInfo.getType()) {
                case 0:
                    latencyBroadcast.putExtra(EXTRA_NETWORK_TYPE, this.mTelephonyManager.getNetworkType());
                    List<CellInfo> info = this.mTelephonyManager.getAllCellInfo();
                    if (info == null) {
                        return;
                    }
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

    public static void SetDefaultLingerTime(int time_ms) {
        if (Process.myUid() == 1000) {
            throw new SecurityException("SetDefaultLingerTime only for internal testing.");
        }
        DEFAULT_LINGER_DELAY_MS = time_ms;
    }

    protected WakeupMessage makeWakeupMessage(Context c, Handler h, String s, int i) {
        return new WakeupMessage(c, h, s, i);
    }
}
