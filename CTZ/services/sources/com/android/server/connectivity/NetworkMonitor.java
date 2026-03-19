package com.android.server.connectivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.CaptivePortal;
import android.net.ICaptivePortal;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.captiveportal.CaptivePortalProbeResult;
import android.net.captiveportal.CaptivePortalProbeSpec;
import android.net.dns.ResolvUtil;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.NetworkEvent;
import android.net.metrics.ValidationProbeEvent;
import android.net.util.Stopwatch;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.DnsManager;
import com.android.server.slice.SliceClientPermissions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetworkMonitor extends StateMachine {
    private static final int BASE = 532480;
    private static final int BLAME_FOR_EVALUATION_ATTEMPTS = 5;
    private static final int CAPTIVE_PORTAL_REEVALUATE_DELAY_MS = 600000;
    private static final int CMD_CAPTIVE_PORTAL_APP_FINISHED = 532489;
    private static final int CMD_CAPTIVE_PORTAL_RECHECK = 532492;
    private static final int CMD_EVALUATE_PRIVATE_DNS = 532495;
    private static final int CMD_FORCE_REEVALUATION = 532488;
    public static final int CMD_LAUNCH_CAPTIVE_PORTAL_APP = 532491;
    public static final int CMD_NETWORK_CONNECTED = 532481;
    public static final int CMD_NETWORK_DISCONNECTED = 532487;
    private static final int CMD_PRIVATE_DNS_SETTINGS_CHANGED = 532493;
    private static final int CMD_REEVALUATE = 532486;
    private static final boolean DBG = true;
    private static final String DEFAULT_FALLBACK_URL = "http://www.google.com/gen_204";
    private static final String DEFAULT_HTTPS_URL = "https://www.google.com/generate_204";
    private static final String DEFAULT_HTTP_URL = "http://connectivitycheck.gstatic.com/generate_204";
    private static final String DEFAULT_OTHER_FALLBACK_URLS = "http://play.googleapis.com/generate_204";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.32 Safari/537.36";
    public static final int EVENT_NETWORK_TESTED = 532482;
    public static final int EVENT_PRIVATE_DNS_CONFIG_RESOLVED = 532494;
    public static final int EVENT_PROVISIONING_NOTIFICATION = 532490;
    private static final String FALLBACK_URL_FOR_CHINA = "http://developers.google.cn/generate_204";
    private static final int IGNORE_REEVALUATE_ATTEMPTS = 5;
    private static final int INITIAL_REEVALUATE_DELAY_MS = 1000;
    private static final int INVALID_UID = -1;
    private static final int MAX_REEVALUATE_DELAY_MS = 600000;
    public static final int NETWORK_TEST_RESULT_INVALID = 1;
    public static final int NETWORK_TEST_RESULT_VALID = 0;
    private static final int NO_UID = 0;
    private static final int NUM_VALIDATION_LOG_LINES = 20;
    private static final int PROBE_TIMEOUT_MS = 3000;
    private static final String SECONDARY_HTTP_URL = "http://captive.apple.com";
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final String TAG = NetworkMonitor.class.getSimpleName();
    private static final boolean VDBG = true;
    private final CaptivePortalProbeSpec[] mCaptivePortalFallbackSpecs;
    private final URL[] mCaptivePortalFallbackUrls;
    private final URL mCaptivePortalHttpUrl;
    private final URL mCaptivePortalHttpsUrl;
    private final State mCaptivePortalState;
    private final String mCaptivePortalUserAgent;
    private final Handler mConnectivityServiceHandler;
    private final Context mContext;
    private final NetworkRequest mDefaultRequest;
    private final State mDefaultState;
    private boolean mDontDisplaySigninNotification;
    private final State mEvaluatingPrivateDnsState;
    private final State mEvaluatingState;
    private final Stopwatch mEvaluationTimer;

    @VisibleForTesting
    protected boolean mIsCaptivePortalCheckEnabled;
    private CaptivePortalProbeResult mLastPortalProbeResult;
    private CustomIntentReceiver mLaunchCaptivePortalAppBroadcastReceiver;
    private final State mMaybeNotifyState;
    private final IpConnectivityLog mMetricsLog;
    private final int mNetId;
    private final Network mNetwork;
    private final NetworkAgentInfo mNetworkAgentInfo;
    private int mNextFallbackUrlIndex;
    private String mPrivateDnsProviderHostname;
    private int mReevaluateToken;
    private final NetworkMonitorSettings mSettings;
    private final TelephonyManager mTelephonyManager;
    private int mUidResponsibleForReeval;
    private boolean mUseHttps;
    private boolean mUserDoesNotWant;
    private final State mValidatedState;
    private int mValidations;
    private final WifiManager mWifiManager;
    public boolean systemReady;
    private final LocalLog validationLogs;

    @VisibleForTesting
    public interface NetworkMonitorSettings {
        public static final NetworkMonitorSettings DEFAULT = new DefaultNetworkMonitorSettings();

        int getSetting(Context context, String str, int i);

        String getSetting(Context context, String str, String str2);
    }

    static int access$2208(NetworkMonitor networkMonitor) {
        int i = networkMonitor.mValidations;
        networkMonitor.mValidations = i + 1;
        return i;
    }

    static int access$2804(NetworkMonitor networkMonitor) {
        int i = networkMonitor.mReevaluateToken + 1;
        networkMonitor.mReevaluateToken = i;
        return i;
    }

    enum EvaluationResult {
        VALIDATED(true),
        CAPTIVE_PORTAL(false);

        final boolean isValidated;

        EvaluationResult(boolean z) {
            this.isValidated = z;
        }
    }

    enum ValidationStage {
        FIRST_VALIDATION(true),
        REVALIDATION(false);

        final boolean isFirstValidation;

        ValidationStage(boolean z) {
            this.isFirstValidation = z;
        }
    }

    public NetworkMonitor(Context context, Handler handler, NetworkAgentInfo networkAgentInfo, NetworkRequest networkRequest) {
        this(context, handler, networkAgentInfo, networkRequest, new IpConnectivityLog(), NetworkMonitorSettings.DEFAULT);
    }

    @VisibleForTesting
    protected NetworkMonitor(Context context, Handler handler, NetworkAgentInfo networkAgentInfo, NetworkRequest networkRequest, IpConnectivityLog ipConnectivityLog, NetworkMonitorSettings networkMonitorSettings) {
        super(TAG + networkAgentInfo.name());
        this.mReevaluateToken = 0;
        this.mUidResponsibleForReeval = -1;
        this.mPrivateDnsProviderHostname = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        this.mValidations = 0;
        this.mUserDoesNotWant = false;
        this.mDontDisplaySigninNotification = false;
        this.systemReady = false;
        this.mDefaultState = new DefaultState();
        this.mValidatedState = new ValidatedState();
        this.mMaybeNotifyState = new MaybeNotifyState();
        this.mEvaluatingState = new EvaluatingState();
        this.mCaptivePortalState = new CaptivePortalState();
        this.mEvaluatingPrivateDnsState = new EvaluatingPrivateDnsState();
        this.mLaunchCaptivePortalAppBroadcastReceiver = null;
        this.validationLogs = new LocalLog(20);
        this.mEvaluationTimer = new Stopwatch();
        this.mLastPortalProbeResult = CaptivePortalProbeResult.FAILED;
        this.mNextFallbackUrlIndex = 0;
        setDbg(true);
        this.mContext = context;
        this.mMetricsLog = ipConnectivityLog;
        this.mConnectivityServiceHandler = handler;
        this.mSettings = networkMonitorSettings;
        this.mNetworkAgentInfo = networkAgentInfo;
        this.mNetwork = new OneAddressPerFamilyNetwork(networkAgentInfo.network());
        this.mNetId = this.mNetwork.netId;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mDefaultRequest = networkRequest;
        addState(this.mDefaultState);
        addState(this.mMaybeNotifyState, this.mDefaultState);
        addState(this.mEvaluatingState, this.mMaybeNotifyState);
        addState(this.mCaptivePortalState, this.mMaybeNotifyState);
        addState(this.mEvaluatingPrivateDnsState, this.mDefaultState);
        addState(this.mValidatedState, this.mDefaultState);
        setInitialState(this.mDefaultState);
        this.mIsCaptivePortalCheckEnabled = getIsCaptivePortalCheckEnabled();
        this.mUseHttps = getUseHttpsValidation();
        this.mCaptivePortalUserAgent = getCaptivePortalUserAgent();
        this.mCaptivePortalHttpsUrl = makeURL(getCaptivePortalServerHttpsUrl());
        this.mCaptivePortalHttpUrl = makeURL(getCaptivePortalServerHttpUrl(networkMonitorSettings, context));
        this.mCaptivePortalFallbackUrls = makeCaptivePortalFallbackUrls();
        this.mCaptivePortalFallbackSpecs = makeCaptivePortalFallbackProbeSpecs();
        start();
    }

    public void forceReevaluation(int i) {
        sendMessage(CMD_FORCE_REEVALUATION, i, 0);
    }

    public void notifyPrivateDnsSettingsChanged(DnsManager.PrivateDnsConfig privateDnsConfig) {
        removeMessages(CMD_PRIVATE_DNS_SETTINGS_CHANGED);
        sendMessage(CMD_PRIVATE_DNS_SETTINGS_CHANGED, privateDnsConfig);
    }

    protected void log(String str) {
        Log.d(TAG + SliceClientPermissions.SliceAuthority.DELIMITER + this.mNetworkAgentInfo.name(), str);
    }

    private void validationLog(int i, Object obj, String str) {
        validationLog(String.format("%s %s %s", ValidationProbeEvent.getProbeName(i), obj, str));
    }

    private void validationLog(String str) {
        log(str);
        this.validationLogs.log(str);
    }

    public LocalLog.ReadOnlyLocalLog getValidationLogs() {
        return this.validationLogs.readOnlyLocalLog();
    }

    private ValidationStage validationStage() {
        return this.mValidations == 0 ? ValidationStage.FIRST_VALIDATION : ValidationStage.REVALIDATION;
    }

    @VisibleForTesting
    public boolean isValidationRequired() {
        return this.mDefaultRequest.networkCapabilities.satisfiedByNetworkCapabilities(this.mNetworkAgentInfo.networkCapabilities);
    }

    public boolean isPrivateDnsValidationRequired() {
        return isValidationRequired() || this.mNetworkAgentInfo.isVPN();
    }

    private void notifyNetworkTestResultInvalid(Object obj) {
        this.mConnectivityServiceHandler.sendMessage(obtainMessage(EVENT_NETWORK_TESTED, 1, this.mNetId, obj));
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED:
                    NetworkMonitor.this.logNetworkEvent(1);
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    return true;
                case NetworkMonitor.CMD_NETWORK_DISCONNECTED:
                    NetworkMonitor.this.logNetworkEvent(7);
                    if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver != null) {
                        NetworkMonitor.this.mContext.unregisterReceiver(NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver);
                        NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver = null;
                    }
                    NetworkMonitor.this.quit();
                    return true;
                case NetworkMonitor.CMD_FORCE_REEVALUATION:
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK:
                    NetworkMonitor.this.log("Forcing reevaluation for UID " + message.arg1);
                    NetworkMonitor.this.mUidResponsibleForReeval = message.arg1;
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    return true;
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED:
                    NetworkMonitor.this.log("CaptivePortal App responded with " + message.arg1);
                    NetworkMonitor.this.mUseHttps = false;
                    switch (message.arg1) {
                        case 0:
                            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_FORCE_REEVALUATION, 0, 0);
                            return true;
                        case 1:
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor.this.mUserDoesNotWant = true;
                            NetworkMonitor.this.notifyNetworkTestResultInvalid(null);
                            NetworkMonitor.this.mUidResponsibleForReeval = 0;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                            return true;
                        case 2:
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingPrivateDnsState);
                            return true;
                        default:
                            return true;
                    }
                case NetworkMonitor.CMD_PRIVATE_DNS_SETTINGS_CHANGED:
                    DnsManager.PrivateDnsConfig privateDnsConfig = (DnsManager.PrivateDnsConfig) message.obj;
                    if (!NetworkMonitor.this.isPrivateDnsValidationRequired() || privateDnsConfig == null || !privateDnsConfig.inStrictMode()) {
                        NetworkMonitor.this.mPrivateDnsProviderHostname = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        break;
                    } else {
                        NetworkMonitor.this.mPrivateDnsProviderHostname = privateDnsConfig.hostname;
                        NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_EVALUATE_PRIVATE_DNS);
                        break;
                    }
                default:
                    return true;
            }
        }
    }

    private class ValidatedState extends State {
        private ValidatedState() {
        }

        public void enter() {
            NetworkMonitor.this.maybeLogEvaluationResult(NetworkMonitor.this.networkEventType(NetworkMonitor.this.validationStage(), EvaluationResult.VALIDATED));
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 0, NetworkMonitor.this.mNetId, null));
            NetworkMonitor.access$2208(NetworkMonitor.this);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 532481) {
                NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                return true;
            }
            if (i == NetworkMonitor.CMD_EVALUATE_PRIVATE_DNS) {
                NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingPrivateDnsState);
                return true;
            }
            return false;
        }
    }

    private class MaybeNotifyState extends State {
        private MaybeNotifyState() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 532491) {
                Intent intent = new Intent("android.net.conn.CAPTIVE_PORTAL");
                intent.putExtra("android.net.extra.NETWORK", new Network(NetworkMonitor.this.mNetwork));
                intent.putExtra("android.net.extra.CAPTIVE_PORTAL", new CaptivePortal(new ICaptivePortal.Stub() {
                    public void appResponse(int i) {
                        if (i == 2) {
                            NetworkMonitor.this.mContext.enforceCallingPermission("android.permission.CONNECTIVITY_INTERNAL", "CaptivePortal");
                        }
                        NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED, i);
                    }
                }));
                CaptivePortalProbeResult captivePortalProbeResult = NetworkMonitor.this.mLastPortalProbeResult;
                intent.putExtra("android.net.extra.CAPTIVE_PORTAL_URL", captivePortalProbeResult.detectUrl);
                if (captivePortalProbeResult.probeSpec != null) {
                    intent.putExtra("android.net.extra.CAPTIVE_PORTAL_PROBE_SPEC", captivePortalProbeResult.probeSpec.getEncodedSpec());
                }
                intent.putExtra("android.net.extra.CAPTIVE_PORTAL_USER_AGENT", NetworkMonitor.this.mCaptivePortalUserAgent);
                intent.setFlags(272629760);
                NetworkMonitor.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                return true;
            }
            return false;
        }

        public void exit() {
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 0, NetworkMonitor.this.mNetId, null));
        }
    }

    private class EvaluatingState extends State {
        private int mAttempts;
        private int mReevaluateDelayMs;

        private EvaluatingState() {
        }

        public void enter() {
            if (!NetworkMonitor.this.mEvaluationTimer.isStarted()) {
                NetworkMonitor.this.mEvaluationTimer.start();
            }
            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_REEVALUATE, NetworkMonitor.access$2804(NetworkMonitor.this), 0);
            if (NetworkMonitor.this.mUidResponsibleForReeval != -1) {
                TrafficStats.setThreadStatsUid(NetworkMonitor.this.mUidResponsibleForReeval);
                NetworkMonitor.this.mUidResponsibleForReeval = -1;
            }
            this.mReevaluateDelayMs = 1000;
            this.mAttempts = 0;
        }

        public boolean processMessage(Message message) throws Throwable {
            int i = message.what;
            if (i != NetworkMonitor.CMD_REEVALUATE) {
                return i == NetworkMonitor.CMD_FORCE_REEVALUATION && this.mAttempts < 5;
            }
            if (message.arg1 != NetworkMonitor.this.mReevaluateToken || NetworkMonitor.this.mUserDoesNotWant) {
                return true;
            }
            if (!NetworkMonitor.this.isValidationRequired()) {
                if (NetworkMonitor.this.isPrivateDnsValidationRequired()) {
                    NetworkMonitor.this.validationLog("Network would not satisfy default request, resolving private DNS");
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingPrivateDnsState);
                } else {
                    NetworkMonitor.this.validationLog("Network would not satisfy default request, not validating");
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                }
                return true;
            }
            if (SystemProperties.getInt("vendor.gsm.sim.ril.testsim", 0) == 1 || SystemProperties.getInt("vendor.gsm.sim.ril.testsim.2", 0) == 1) {
                NetworkMonitor.this.log("test sim enabled, make it validated directly");
                NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                return true;
            }
            this.mAttempts++;
            CaptivePortalProbeResult captivePortalProbeResultIsCaptivePortal = NetworkMonitor.this.isCaptivePortal();
            if (captivePortalProbeResultIsCaptivePortal.isSuccessful()) {
                NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingPrivateDnsState);
            } else if (captivePortalProbeResultIsCaptivePortal.isPortal()) {
                NetworkMonitor.this.notifyNetworkTestResultInvalid(captivePortalProbeResultIsCaptivePortal.redirectUrl);
                NetworkMonitor.this.mLastPortalProbeResult = captivePortalProbeResultIsCaptivePortal;
                NetworkMonitor.this.transitionTo(NetworkMonitor.this.mCaptivePortalState);
            } else {
                NetworkMonitor.this.sendMessageDelayed(NetworkMonitor.this.obtainMessage(NetworkMonitor.CMD_REEVALUATE, NetworkMonitor.access$2804(NetworkMonitor.this), 0), this.mReevaluateDelayMs);
                NetworkMonitor.this.logNetworkEvent(3);
                NetworkMonitor.this.notifyNetworkTestResultInvalid(captivePortalProbeResultIsCaptivePortal.redirectUrl);
                if (this.mAttempts >= 5) {
                    TrafficStats.clearThreadStatsUid();
                }
                this.mReevaluateDelayMs *= 2;
                if (this.mReevaluateDelayMs > 600000) {
                    this.mReevaluateDelayMs = 600000;
                }
            }
            return true;
        }

        public void exit() {
            TrafficStats.clearThreadStatsUid();
        }
    }

    private class CustomIntentReceiver extends BroadcastReceiver {
        private final String mAction;
        private final int mToken;
        private final int mWhat;

        CustomIntentReceiver(String str, int i, int i2) {
            this.mToken = i;
            this.mWhat = i2;
            this.mAction = str + "_" + NetworkMonitor.this.mNetId + "_" + i;
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

        private CaptivePortalState() {
        }

        public void enter() {
            NetworkMonitor.this.maybeLogEvaluationResult(NetworkMonitor.this.networkEventType(NetworkMonitor.this.validationStage(), EvaluationResult.CAPTIVE_PORTAL));
            if (NetworkMonitor.this.mDontDisplaySigninNotification) {
                return;
            }
            if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver == null) {
                NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver = NetworkMonitor.this.new CustomIntentReceiver(ACTION_LAUNCH_CAPTIVE_PORTAL_APP, new Random().nextInt(), NetworkMonitor.CMD_LAUNCH_CAPTIVE_PORTAL_APP);
            }
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 1, NetworkMonitor.this.mNetId, NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver.getPendingIntent()));
            NetworkMonitor.this.sendMessageDelayed(NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK, 0, 600000L);
            NetworkMonitor.access$2208(NetworkMonitor.this);
        }

        public void exit() {
            NetworkMonitor.this.removeMessages(NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK);
        }
    }

    private class EvaluatingPrivateDnsState extends State {
        private DnsManager.PrivateDnsConfig mPrivateDnsConfig;
        private int mPrivateDnsReevalDelayMs;

        private EvaluatingPrivateDnsState() {
        }

        public void enter() {
            this.mPrivateDnsReevalDelayMs = 1000;
            this.mPrivateDnsConfig = null;
            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_EVALUATE_PRIVATE_DNS);
        }

        public boolean processMessage(Message message) {
            if (message.what == NetworkMonitor.CMD_EVALUATE_PRIVATE_DNS) {
                if (inStrictMode()) {
                    if (!isStrictModeHostnameResolved()) {
                        resolveStrictModeHostname();
                        if (isStrictModeHostnameResolved()) {
                            notifyPrivateDnsConfigResolved();
                        } else {
                            handlePrivateDnsEvaluationFailure();
                            return true;
                        }
                    }
                    if (!sendPrivateDnsProbe()) {
                        handlePrivateDnsEvaluationFailure();
                        return true;
                    }
                }
                NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                return true;
            }
            return false;
        }

        private boolean inStrictMode() {
            return !TextUtils.isEmpty(NetworkMonitor.this.mPrivateDnsProviderHostname);
        }

        private boolean isStrictModeHostnameResolved() {
            return this.mPrivateDnsConfig != null && this.mPrivateDnsConfig.hostname.equals(NetworkMonitor.this.mPrivateDnsProviderHostname) && this.mPrivateDnsConfig.ips.length > 0;
        }

        private void resolveStrictModeHostname() {
            try {
                this.mPrivateDnsConfig = new DnsManager.PrivateDnsConfig(NetworkMonitor.this.mPrivateDnsProviderHostname, NetworkMonitor.this.resolveAllLocally(NetworkMonitor.this.mNetwork, NetworkMonitor.this.mPrivateDnsProviderHostname, 0));
            } catch (UnknownHostException e) {
                this.mPrivateDnsConfig = null;
            }
        }

        private void notifyPrivateDnsConfigResolved() {
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PRIVATE_DNS_CONFIG_RESOLVED, 0, NetworkMonitor.this.mNetId, this.mPrivateDnsConfig));
        }

        private void handlePrivateDnsEvaluationFailure() {
            NetworkMonitor.this.notifyNetworkTestResultInvalid(null);
            NetworkMonitor.this.sendMessageDelayed(NetworkMonitor.CMD_EVALUATE_PRIVATE_DNS, this.mPrivateDnsReevalDelayMs);
            this.mPrivateDnsReevalDelayMs *= 2;
            if (this.mPrivateDnsReevalDelayMs > 600000) {
                this.mPrivateDnsReevalDelayMs = 600000;
            }
        }

        private boolean sendPrivateDnsProbe() {
            try {
                InetAddress[] allByName = NetworkMonitor.this.getAllByName(NetworkMonitor.this.mNetworkAgentInfo.network(), UUID.randomUUID().toString().substring(0, 8) + "-dnsotls-ds.metric.gstatic.com");
                if (allByName != null) {
                    return allByName.length > 0;
                }
                return false;
            } catch (UnknownHostException e) {
                return false;
            }
        }
    }

    private static class OneAddressPerFamilyNetwork extends Network {
        public OneAddressPerFamilyNetwork(Network network) {
            super(network);
        }

        @Override
        public InetAddress[] getAllByName(String str) throws UnknownHostException {
            List<InetAddress> listAsList = Arrays.asList(ResolvUtil.blockingResolveAllLocally(this, str));
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            linkedHashMap.put(((InetAddress) listAsList.get(0)).getClass(), (InetAddress) listAsList.get(0));
            Collections.shuffle(listAsList);
            for (InetAddress inetAddress : listAsList) {
                linkedHashMap.put(inetAddress.getClass(), inetAddress);
            }
            return (InetAddress[]) linkedHashMap.values().toArray(new InetAddress[linkedHashMap.size()]);
        }
    }

    public boolean getIsCaptivePortalCheckEnabled() {
        return this.mSettings.getSetting(this.mContext, "captive_portal_mode", 1) != 0;
    }

    public boolean getUseHttpsValidation() {
        return this.mSettings.getSetting(this.mContext, "captive_portal_use_https", 0) == 1;
    }

    private String getCaptivePortalServerHttpsUrl() {
        return this.mSettings.getSetting(this.mContext, "captive_portal_https_url", DEFAULT_HTTPS_URL);
    }

    public static String getCaptivePortalServerHttpUrl(Context context) {
        return getCaptivePortalServerHttpUrl(NetworkMonitorSettings.DEFAULT, context);
    }

    public static String getCaptivePortalServerHttpUrl(NetworkMonitorSettings networkMonitorSettings, Context context) {
        return networkMonitorSettings.getSetting(context, "captive_portal_http_url", SECONDARY_HTTP_URL);
    }

    private URL[] makeCaptivePortalFallbackUrls() {
        try {
            String str = this.mSettings.getSetting(this.mContext, "captive_portal_fallback_url", DEFAULT_FALLBACK_URL) + "," + this.mSettings.getSetting(this.mContext, "captive_portal_other_fallback_urls", DEFAULT_OTHER_FALLBACK_URLS);
            ArrayList arrayList = new ArrayList();
            for (String str2 : str.split(",")) {
                URL urlMakeURL = makeURL(str2);
                if (urlMakeURL != null) {
                    arrayList.add(urlMakeURL);
                }
            }
            if (arrayList.isEmpty()) {
                Log.e(TAG, String.format("could not create any url from %s", str));
            }
            return (URL[]) arrayList.toArray(new URL[arrayList.size()]);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing configured fallback URLs", e);
            return new URL[0];
        }
    }

    private CaptivePortalProbeSpec[] makeCaptivePortalFallbackProbeSpecs() {
        try {
            String setting = this.mSettings.getSetting(this.mContext, "captive_portal_fallback_probe_specs", (String) null);
            if (TextUtils.isEmpty(setting)) {
                return null;
            }
            return CaptivePortalProbeSpec.parseCaptivePortalProbeSpecs(setting);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing configured fallback probe specs", e);
            return null;
        }
    }

    private String getCaptivePortalUserAgent() {
        return this.mSettings.getSetting(this.mContext, "captive_portal_user_agent", DEFAULT_USER_AGENT);
    }

    private URL nextFallbackUrl() {
        if (this.mCaptivePortalFallbackUrls.length == 0) {
            return null;
        }
        int iAbs = Math.abs(this.mNextFallbackUrlIndex) % this.mCaptivePortalFallbackUrls.length;
        this.mNextFallbackUrlIndex += new Random().nextInt();
        return this.mCaptivePortalFallbackUrls[iAbs];
    }

    private CaptivePortalProbeSpec nextFallbackSpec() {
        if (ArrayUtils.isEmpty(this.mCaptivePortalFallbackSpecs)) {
            return null;
        }
        return this.mCaptivePortalFallbackSpecs[Math.abs(new Random().nextInt()) % this.mCaptivePortalFallbackSpecs.length];
    }

    @VisibleForTesting
    protected CaptivePortalProbeResult isCaptivePortal() throws Throwable {
        URL urlMakeURL;
        CaptivePortalProbeResult captivePortalProbeResultSendDnsAndHttpProbes;
        if (!this.mIsCaptivePortalCheckEnabled) {
            validationLog("Validation disabled.");
            return CaptivePortalProbeResult.SUCCESS;
        }
        URL url = this.mCaptivePortalHttpsUrl;
        URL url2 = this.mCaptivePortalHttpUrl;
        ProxyInfo httpProxy = this.mNetworkAgentInfo.linkProperties.getHttpProxy();
        if (httpProxy != null && !Uri.EMPTY.equals(httpProxy.getPacFileUrl())) {
            urlMakeURL = makeURL(httpProxy.getPacFileUrl().toString());
            if (urlMakeURL == null) {
                return CaptivePortalProbeResult.FAILED;
            }
        } else {
            urlMakeURL = null;
        }
        if (urlMakeURL == null && (url2 == null || url == null)) {
            return CaptivePortalProbeResult.FAILED;
        }
        SystemClock.elapsedRealtime();
        if (urlMakeURL != null) {
            captivePortalProbeResultSendDnsAndHttpProbes = sendDnsAndHttpProbes(null, urlMakeURL, 3);
        } else if (this.mUseHttps) {
            captivePortalProbeResultSendDnsAndHttpProbes = sendParallelHttpProbes(httpProxy, url, url2);
        } else {
            captivePortalProbeResultSendDnsAndHttpProbes = sendDnsAndHttpProbes(httpProxy, url2, 1);
        }
        SystemClock.elapsedRealtime();
        return captivePortalProbeResultSendDnsAndHttpProbes;
    }

    private CaptivePortalProbeResult sendDnsAndHttpProbes(ProxyInfo proxyInfo, URL url, int i) throws Throwable {
        sendDnsProbe(proxyInfo != null ? proxyInfo.getHost() : url.getHost());
        CaptivePortalProbeResult captivePortalProbeResultSendHttpProbe = sendHttpProbe(url, i, null);
        if (captivePortalProbeResultSendHttpProbe.isFailed()) {
            validationLog("trying to use fallback URL(1)");
            URL urlMakeURL = makeURL(FALLBACK_URL_FOR_CHINA);
            if (urlMakeURL != null) {
                captivePortalProbeResultSendHttpProbe = sendHttpProbe(urlMakeURL, 1, null);
            }
        }
        if (captivePortalProbeResultSendHttpProbe.isFailed()) {
            validationLog("trying to use fallback URL(2)");
            URL urlNextFallbackUrl = nextFallbackUrl();
            if (urlNextFallbackUrl != null) {
                return sendHttpProbe(urlNextFallbackUrl, 4, null);
            }
            return captivePortalProbeResultSendHttpProbe;
        }
        return captivePortalProbeResultSendHttpProbe;
    }

    private void sendDnsProbe(String str) {
        String str2;
        int i;
        if (TextUtils.isEmpty(str)) {
            return;
        }
        ValidationProbeEvent.getProbeName(0);
        Stopwatch stopwatchStart = new Stopwatch().start();
        try {
            InetAddress[] allByName = getAllByName(this.mNetwork, str);
            StringBuffer stringBuffer = new StringBuffer();
            for (InetAddress inetAddress : allByName) {
                stringBuffer.append(',');
                stringBuffer.append(inetAddress.getHostAddress());
            }
            str2 = "OK " + stringBuffer.substring(1);
            i = 1;
        } catch (UnknownHostException e) {
            str2 = "FAIL";
            i = 0;
        }
        long jStop = stopwatchStart.stop();
        validationLog(0, str, String.format("%dms %s", Long.valueOf(jStop), str2));
        logValidationProbe(jStop, 0, i);
    }

    @VisibleForTesting
    protected CaptivePortalProbeResult sendHttpProbe(URL url, int i, CaptivePortalProbeSpec captivePortalProbeSpec) throws Throwable {
        int i2;
        String headerField;
        ?? r10;
        int i3;
        String contentType;
        ?? Contains;
        ?? r7;
        Stopwatch stopwatchStart = new Stopwatch().start();
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-190);
        ?? r72 = 0;
        r72 = 0;
        r72 = 0;
        try {
            try {
                r10 = (HttpURLConnection) this.mNetwork.openConnection(url);
                try {
                    try {
                        r10.setInstanceFollowRedirects(i == 3);
                        r10.setConnectTimeout(10000);
                        r10.setReadTimeout(10000);
                        r10.setUseCaches(false);
                        if (this.mCaptivePortalUserAgent != null) {
                            r10.setRequestProperty("User-Agent", this.mCaptivePortalUserAgent);
                        }
                        String string = r10.getRequestProperties().toString();
                        r10.setRequestProperty("Connection", "Close");
                        long jElapsedRealtime = SystemClock.elapsedRealtime();
                        int responseCode = r10.getResponseCode();
                        try {
                            headerField = r10.getHeaderField("location");
                        } catch (IOException e) {
                            e = e;
                            headerField = null;
                        } catch (Exception e2) {
                            e = e2;
                            headerField = null;
                        }
                        try {
                            validationLog(i, url, "time=" + (SystemClock.elapsedRealtime() - jElapsedRealtime) + "ms ret=" + responseCode + " request=" + string + " headers=" + r10.getHeaderFields());
                            if (responseCode != 200) {
                                i3 = responseCode;
                                try {
                                    contentType = r10.getContentType();
                                    if (contentType == null) {
                                        StringBuilder sb = new StringBuilder();
                                        Contains = "contentType is null, httpResponseCode = ";
                                        sb.append("contentType is null, httpResponseCode = ");
                                        sb.append(i3);
                                        log(sb.toString());
                                    } else {
                                        Contains = contentType.contains("text/html");
                                        if (Contains != 0) {
                                            Contains = new BufferedReader(new InputStreamReader((InputStream) r10.getContent())).readLine();
                                            validationLog("urlConnection.getContent() = " + Contains);
                                            try {
                                                if (i3 == 200 && Contains == 0) {
                                                    log("Internet detected!");
                                                    r7 = Contains;
                                                } else if (i3 == 200 && (Contains = Contains.contains("Success")) != 0) {
                                                    log("Internet detected!");
                                                    r7 = Contains;
                                                } else if (i3 == 200 && this.mNetworkAgentInfo.networkInfo.getType() == 0) {
                                                    log("Internet detected!");
                                                    r7 = Contains;
                                                }
                                                i2 = 204;
                                                r72 = r7;
                                                if (r10 != 0) {
                                                    r10.disconnect();
                                                }
                                            } catch (IOException e3) {
                                                e = e3;
                                                r72 = r10;
                                                i2 = 204;
                                                validationLog(i, url, "Probe failed with exception " + e);
                                                r72 = r72;
                                                if (r72 != 0) {
                                                    r72.disconnect();
                                                }
                                            } catch (Exception e4) {
                                                e = e4;
                                                r72 = r10;
                                                i2 = 204;
                                                validationLog(i, url, "Probably not a portal: exception " + e);
                                                r72 = r72;
                                                if (r72 != 0) {
                                                }
                                            }
                                        }
                                    }
                                    i2 = i3;
                                    r72 = Contains;
                                    if (r10 != 0) {
                                    }
                                } catch (IOException e5) {
                                    e = e5;
                                    i2 = i3;
                                    r72 = r10;
                                    validationLog(i, url, "Probe failed with exception " + e);
                                    r72 = r72;
                                    if (r72 != 0) {
                                    }
                                    TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                                    int i4 = i2;
                                    logValidationProbe(stopwatchStart.stop(), i, i4);
                                    if (captivePortalProbeSpec == null) {
                                    }
                                } catch (Exception e6) {
                                    e = e6;
                                    i2 = i3;
                                    r72 = r10;
                                    validationLog(i, url, "Probably not a portal: exception " + e);
                                    r72 = r72;
                                    if (r72 != 0) {
                                    }
                                    TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                                    int i42 = i2;
                                    logValidationProbe(stopwatchStart.stop(), i, i42);
                                    if (captivePortalProbeSpec == null) {
                                    }
                                }
                            } else {
                                if (i == 3) {
                                    validationLog(i, url, "PAC fetch 200 response interpreted as 204 response.");
                                } else if (r10.getContentLengthLong() == 0) {
                                    validationLog(i, url, "200 response with Content-length=0 interpreted as 204 response.");
                                } else {
                                    if (r10.getContentLengthLong() == -1 && r10.getInputStream().read() == -1) {
                                        validationLog(i, url, "Empty 200 response interpreted as 204 response.");
                                    }
                                    i3 = responseCode;
                                    contentType = r10.getContentType();
                                    if (contentType == null) {
                                    }
                                    i2 = i3;
                                    r72 = Contains;
                                    if (r10 != 0) {
                                    }
                                }
                                i3 = 204;
                                contentType = r10.getContentType();
                                if (contentType == null) {
                                }
                                i2 = i3;
                                r72 = Contains;
                                if (r10 != 0) {
                                }
                            }
                        } catch (IOException e7) {
                            e = e7;
                            r72 = r10;
                            i2 = responseCode;
                            validationLog(i, url, "Probe failed with exception " + e);
                            r72 = r72;
                            if (r72 != 0) {
                            }
                            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                            int i422 = i2;
                            logValidationProbe(stopwatchStart.stop(), i, i422);
                            if (captivePortalProbeSpec == null) {
                            }
                        } catch (Exception e8) {
                            e = e8;
                            r72 = r10;
                            i2 = responseCode;
                            validationLog(i, url, "Probably not a portal: exception " + e);
                            r72 = r72;
                            if (r72 != 0) {
                            }
                            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                            int i4222 = i2;
                            logValidationProbe(stopwatchStart.stop(), i, i4222);
                            if (captivePortalProbeSpec == null) {
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (r10 != 0) {
                            r10.disconnect();
                        }
                        TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                        throw th;
                    }
                } catch (IOException e9) {
                    e = e9;
                    i2 = 599;
                    headerField = null;
                } catch (Exception e10) {
                    e = e10;
                    i2 = 599;
                    headerField = null;
                }
            } catch (Throwable th2) {
                th = th2;
                r10 = r72;
            }
        } catch (IOException e11) {
            e = e11;
            i2 = 599;
            headerField = null;
        } catch (Exception e12) {
            e = e12;
            i2 = 599;
            headerField = null;
        }
        TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
        int i42222 = i2;
        logValidationProbe(stopwatchStart.stop(), i, i42222);
        return captivePortalProbeSpec == null ? new CaptivePortalProbeResult(i42222, headerField, url.toString()) : captivePortalProbeSpec.getResult(i42222, headerField);
    }

    private CaptivePortalProbeResult sendParallelHttpProbes(ProxyInfo proxyInfo, URL url, URL url2) throws Throwable {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        ?? r8 = new Thread(true, proxyInfo, url, url2, countDownLatch) {
            private final boolean mIsHttps;
            private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
            final URL val$httpUrl;
            final URL val$httpsUrl;
            final CountDownLatch val$latch;
            final ProxyInfo val$proxy;

            {
                this.val$proxy = proxyInfo;
                this.val$httpsUrl = url;
                this.val$httpUrl = url2;
                this.val$latch = countDownLatch;
                this.mIsHttps = z;
            }

            public CaptivePortalProbeResult result() {
                return this.mResult;
            }

            @Override
            public void run() {
                if (this.mIsHttps) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpsUrl, 2);
                } else {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
                }
                if ((this.mIsHttps && this.mResult.isSuccessful()) || (!this.mIsHttps && this.mResult.isPortal())) {
                    while (this.val$latch.getCount() > 0) {
                        this.val$latch.countDown();
                    }
                }
                this.val$latch.countDown();
            }
        };
        ?? r9 = new Thread(false, proxyInfo, url, url2, countDownLatch) {
            private final boolean mIsHttps;
            private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
            final URL val$httpUrl;
            final URL val$httpsUrl;
            final CountDownLatch val$latch;
            final ProxyInfo val$proxy;

            {
                this.val$proxy = proxyInfo;
                this.val$httpsUrl = url;
                this.val$httpUrl = url2;
                this.val$latch = countDownLatch;
                this.mIsHttps = z;
            }

            public CaptivePortalProbeResult result() {
                return this.mResult;
            }

            @Override
            public void run() {
                if (this.mIsHttps) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpsUrl, 2);
                } else {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
                }
                if ((this.mIsHttps && this.mResult.isSuccessful()) || (!this.mIsHttps && this.mResult.isPortal())) {
                    while (this.val$latch.getCount() > 0) {
                        this.val$latch.countDown();
                    }
                }
                this.val$latch.countDown();
            }
        };
        try {
            r8.start();
            r9.start();
            countDownLatch.await(3000L, TimeUnit.MILLISECONDS);
            CaptivePortalProbeResult captivePortalProbeResultResult = r8.result();
            CaptivePortalProbeResult captivePortalProbeResultResult2 = r9.result();
            if (captivePortalProbeResultResult2.isPortal()) {
                return captivePortalProbeResultResult2;
            }
            if (captivePortalProbeResultResult.isPortal() || captivePortalProbeResultResult.isSuccessful()) {
                return captivePortalProbeResultResult;
            }
            CaptivePortalProbeSpec captivePortalProbeSpecNextFallbackSpec = nextFallbackSpec();
            URL url3 = captivePortalProbeSpecNextFallbackSpec != null ? captivePortalProbeSpecNextFallbackSpec.getUrl() : nextFallbackUrl();
            if (url3 != null) {
                CaptivePortalProbeResult captivePortalProbeResultSendHttpProbe = sendHttpProbe(url3, 4, captivePortalProbeSpecNextFallbackSpec);
                if (captivePortalProbeResultSendHttpProbe.isPortal()) {
                    return captivePortalProbeResultSendHttpProbe;
                }
            }
            try {
                r9.join();
                if (r9.result().isPortal()) {
                    return r9.result();
                }
                r8.join();
                return r8.result();
            } catch (InterruptedException e) {
                validationLog("Error: http or https probe wait interrupted!");
                return CaptivePortalProbeResult.FAILED;
            }
        } catch (InterruptedException e2) {
            validationLog("Error: probes wait interrupted!");
            return CaptivePortalProbeResult.FAILED;
        }
    }

    @VisibleForTesting
    protected InetAddress[] getAllByName(Network network, String str) throws UnknownHostException {
        return network.getAllByName(str);
    }

    @VisibleForTesting
    protected InetAddress[] resolveAllLocally(Network network, String str, int i) throws UnknownHostException {
        return ResolvUtil.blockingResolveAllLocally(network, str, i);
    }

    private URL makeURL(String str) {
        if (str != null) {
            try {
                return new URL(str);
            } catch (MalformedURLException e) {
                validationLog("Bad URL: " + str);
                return null;
            }
        }
        return null;
    }

    private void logNetworkEvent(int i) {
        this.mMetricsLog.log(this.mNetId, this.mNetworkAgentInfo.networkCapabilities.getTransportTypes(), new NetworkEvent(i));
    }

    private int networkEventType(ValidationStage validationStage, EvaluationResult evaluationResult) {
        if (validationStage.isFirstValidation) {
            if (evaluationResult.isValidated) {
                return 8;
            }
            return 10;
        }
        if (evaluationResult.isValidated) {
            return 9;
        }
        return 11;
    }

    private void maybeLogEvaluationResult(int i) {
        if (this.mEvaluationTimer.isRunning()) {
            this.mMetricsLog.log(this.mNetId, this.mNetworkAgentInfo.networkCapabilities.getTransportTypes(), new NetworkEvent(i, this.mEvaluationTimer.stop()));
            this.mEvaluationTimer.reset();
        }
    }

    private void logValidationProbe(long j, int i, int i2) {
        int[] transportTypes = this.mNetworkAgentInfo.networkCapabilities.getTransportTypes();
        boolean z = validationStage().isFirstValidation;
        ValidationProbeEvent validationProbeEvent = new ValidationProbeEvent();
        validationProbeEvent.probeType = ValidationProbeEvent.makeProbeType(i, z);
        validationProbeEvent.returnCode = i2;
        validationProbeEvent.durationMs = j;
        this.mMetricsLog.log(this.mNetId, transportTypes, validationProbeEvent);
    }

    @VisibleForTesting
    public static class DefaultNetworkMonitorSettings implements NetworkMonitorSettings {
        @Override
        public int getSetting(Context context, String str, int i) {
            return Settings.Global.getInt(context.getContentResolver(), str, i);
        }

        @Override
        public String getSetting(Context context, String str, String str2) {
            String string = Settings.Global.getString(context.getContentResolver(), str);
            return string != null ? string : str2;
        }
    }
}
