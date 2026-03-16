package android.net;

import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.SamplingDataTracker;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.R;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.AsyncChannel;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

public class MobileDataStateTracker extends BaseNetworkStateTracker {
    private static final boolean DBG = false;
    private static final String TAG = "MobileDataStateTracker";
    private static final int UNKNOWN = Integer.MAX_VALUE;
    private static final boolean VDBG = false;
    private static NetworkDataEntry[] mTheoreticalBWTable = {new NetworkDataEntry(2, 237, 118, Integer.MAX_VALUE), new NetworkDataEntry(1, 48, 40, Integer.MAX_VALUE), new NetworkDataEntry(3, 384, 64, Integer.MAX_VALUE), new NetworkDataEntry(8, 14400, Integer.MAX_VALUE, Integer.MAX_VALUE), new NetworkDataEntry(9, 14400, 5760, Integer.MAX_VALUE), new NetworkDataEntry(10, 14400, 5760, Integer.MAX_VALUE), new NetworkDataEntry(15, 21000, 5760, Integer.MAX_VALUE), new NetworkDataEntry(4, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), new NetworkDataEntry(7, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), new NetworkDataEntry(5, 2468, 153, Integer.MAX_VALUE), new NetworkDataEntry(6, 3072, BluetoothClass.Device.WEARABLE_PAGER, Integer.MAX_VALUE), new NetworkDataEntry(12, 14700, BluetoothClass.Device.WEARABLE_PAGER, Integer.MAX_VALUE), new NetworkDataEntry(11, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), new NetworkDataEntry(13, UserHandle.PER_USER_RANGE, 50000, Integer.MAX_VALUE), new NetworkDataEntry(14, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)};
    private String mApnType;
    private Context mContext;
    private AsyncChannel mDataConnectionTrackerAc;
    private Handler mHandler;
    private LinkProperties mLinkProperties;
    private PhoneConstants.DataState mMobileDataState;
    private NetworkInfo mNetworkInfo;
    private ITelephony mPhoneService;
    private SignalStrength mSignalStrength;
    private Handler mTarget;
    private boolean mTeardownRequested = false;
    private boolean mPrivateDnsRouteSet = false;
    private boolean mDefaultRouteSet = false;
    protected boolean mUserDataEnabled = true;
    protected boolean mPolicyDataEnabled = true;
    private AtomicBoolean mIsCaptivePortal = new AtomicBoolean(false);
    private SamplingDataTracker mSamplingDataTracker = new SamplingDataTracker();
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            MobileDataStateTracker.this.mSignalStrength = signalStrength;
        }
    };

    public MobileDataStateTracker(int netType, String tag) {
        this.mNetworkInfo = new NetworkInfo(netType, TelephonyManager.getDefault().getNetworkType(), tag, TelephonyManager.getDefault().getNetworkTypeName());
        this.mApnType = networkTypeToApnType(netType);
    }

    @Override
    public void startMonitoring(Context context, Handler target) {
        this.mTarget = target;
        this.mContext = context;
        this.mHandler = new MdstHandler(target.getLooper(), this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_DATA_CONNECTION_CONNECTED_TO_PROVISIONING_APN);
        filter.addAction(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED);
        this.mContext.registerReceiver(new MobileDataStateReceiver(), filter);
        this.mMobileDataState = PhoneConstants.DataState.DISCONNECTED;
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        tm.listen(this.mPhoneStateListener, 256);
    }

    static class MdstHandler extends Handler {
        private MobileDataStateTracker mMdst;

        MdstHandler(Looper looper, MobileDataStateTracker mdst) {
            super(looper);
            this.mMdst = mdst;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        this.mMdst.mDataConnectionTrackerAc = (AsyncChannel) msg.obj;
                    }
                    break;
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                    this.mMdst.mDataConnectionTrackerAc = null;
                    break;
            }
        }
    }

    @Override
    public boolean isPrivateDnsRouteSet() {
        return this.mPrivateDnsRouteSet;
    }

    @Override
    public void privateDnsRouteSet(boolean enabled) {
        this.mPrivateDnsRouteSet = enabled;
    }

    @Override
    public NetworkInfo getNetworkInfo() {
        return this.mNetworkInfo;
    }

    @Override
    public boolean isDefaultRouteSet() {
        return this.mDefaultRouteSet;
    }

    @Override
    public void defaultRouteSet(boolean enabled) {
        this.mDefaultRouteSet = enabled;
    }

    public void releaseWakeLock() {
    }

    private void updateLinkProperitesAndCapatilities(Intent intent) {
        this.mLinkProperties = (LinkProperties) intent.getParcelableExtra("linkProperties");
        if (this.mLinkProperties == null) {
            loge("CONNECTED event did not supply link properties.");
            this.mLinkProperties = new LinkProperties();
        }
        this.mLinkProperties.setMtu(this.mContext.getResources().getInteger(R.integer.config_mobile_mtu));
        this.mNetworkCapabilities = (NetworkCapabilities) intent.getParcelableExtra("networkCapabilities");
        if (this.mNetworkCapabilities == null) {
            loge("CONNECTED event did not supply network capabilities.");
            this.mNetworkCapabilities = new NetworkCapabilities();
        }
    }

    private class MobileDataStateReceiver extends BroadcastReceiver {
        private MobileDataStateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TelephonyIntents.ACTION_DATA_CONNECTION_CONNECTED_TO_PROVISIONING_APN)) {
                String apnName = intent.getStringExtra("apn");
                String apnType = intent.getStringExtra("apnType");
                if (TextUtils.equals(MobileDataStateTracker.this.mApnType, apnType)) {
                    MobileDataStateTracker.this.mMobileDataState = PhoneConstants.DataState.CONNECTING;
                    MobileDataStateTracker.this.updateLinkProperitesAndCapatilities(intent);
                    MobileDataStateTracker.this.mNetworkInfo.setIsConnectedToProvisioningNetwork(true);
                    MobileDataStateTracker.this.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, ProxyInfo.LOCAL_EXCL_LIST, apnName);
                    return;
                }
                return;
            }
            if (intent.getAction().equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                String apnType2 = intent.getStringExtra("apnType");
                if (TextUtils.equals(apnType2, MobileDataStateTracker.this.mApnType)) {
                    MobileDataStateTracker.this.mNetworkInfo.setIsConnectedToProvisioningNetwork(false);
                    int oldSubtype = MobileDataStateTracker.this.mNetworkInfo.getSubtype();
                    int newSubType = TelephonyManager.getDefault().getNetworkType();
                    String subTypeName = TelephonyManager.getDefault().getNetworkTypeName();
                    MobileDataStateTracker.this.mNetworkInfo.setSubtype(newSubType, subTypeName);
                    if (newSubType != oldSubtype && MobileDataStateTracker.this.mNetworkInfo.isConnected()) {
                        Message msg = MobileDataStateTracker.this.mTarget.obtainMessage(NetworkStateTracker.EVENT_NETWORK_SUBTYPE_CHANGED, oldSubtype, 0, MobileDataStateTracker.this.mNetworkInfo);
                        msg.sendToTarget();
                    }
                    PhoneConstants.DataState state = (PhoneConstants.DataState) Enum.valueOf(PhoneConstants.DataState.class, intent.getStringExtra("state"));
                    String reason = intent.getStringExtra("reason");
                    String apnName2 = intent.getStringExtra("apn");
                    MobileDataStateTracker.this.mNetworkInfo.setRoaming(intent.getBooleanExtra(PhoneConstants.DATA_NETWORK_ROAMING_KEY, false));
                    MobileDataStateTracker.this.mNetworkInfo.setIsAvailable(intent.getBooleanExtra(PhoneConstants.NETWORK_UNAVAILABLE_KEY, false) ? false : true);
                    if (MobileDataStateTracker.this.mMobileDataState != state) {
                        MobileDataStateTracker.this.mMobileDataState = state;
                        switch (state) {
                            case DISCONNECTED:
                                if (MobileDataStateTracker.this.isTeardownRequested()) {
                                    MobileDataStateTracker.this.setTeardownRequested(false);
                                }
                                MobileDataStateTracker.this.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, reason, apnName2);
                                break;
                            case CONNECTING:
                                MobileDataStateTracker.this.setDetailedState(NetworkInfo.DetailedState.CONNECTING, reason, apnName2);
                                break;
                            case SUSPENDED:
                                MobileDataStateTracker.this.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, reason, apnName2);
                                break;
                            case CONNECTED:
                                MobileDataStateTracker.this.updateLinkProperitesAndCapatilities(intent);
                                MobileDataStateTracker.this.setDetailedState(NetworkInfo.DetailedState.CONNECTED, reason, apnName2);
                                break;
                        }
                        MobileDataStateTracker.this.mSamplingDataTracker.resetSamplingData();
                        return;
                    }
                    if (TextUtils.equals(reason, PhoneConstants.REASON_LINK_PROPERTIES_CHANGED)) {
                        MobileDataStateTracker.this.mLinkProperties = (LinkProperties) intent.getParcelableExtra("linkProperties");
                        if (MobileDataStateTracker.this.mLinkProperties == null) {
                            MobileDataStateTracker.this.loge("No link property in LINK_PROPERTIES change event.");
                            MobileDataStateTracker.this.mLinkProperties = new LinkProperties();
                        }
                        MobileDataStateTracker.this.mNetworkInfo.setDetailedState(MobileDataStateTracker.this.mNetworkInfo.getDetailedState(), reason, MobileDataStateTracker.this.mNetworkInfo.getExtraInfo());
                        Message msg2 = MobileDataStateTracker.this.mTarget.obtainMessage(NetworkStateTracker.EVENT_CONFIGURATION_CHANGED, MobileDataStateTracker.this.mNetworkInfo);
                        msg2.sendToTarget();
                        return;
                    }
                    return;
                }
                return;
            }
            if (intent.getAction().equals(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED)) {
                String apnType3 = intent.getStringExtra("apnType");
                if (TextUtils.equals(apnType3, MobileDataStateTracker.this.mApnType)) {
                    MobileDataStateTracker.this.mNetworkInfo.setIsConnectedToProvisioningNetwork(false);
                    MobileDataStateTracker.this.setDetailedState(NetworkInfo.DetailedState.FAILED, intent.getStringExtra("reason"), intent.getStringExtra("apn"));
                }
            }
        }
    }

    private void getPhoneService(boolean forceRefresh) {
        if (this.mPhoneService == null || forceRefresh) {
            this.mPhoneService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    }

    @Override
    public boolean isAvailable() {
        return this.mNetworkInfo.isAvailable();
    }

    @Override
    public String getTcpBufferSizesPropName() {
        String networkTypeStr = "unknown";
        TelephonyManager tm = new TelephonyManager(this.mContext);
        switch (tm.getNetworkType()) {
            case 1:
                networkTypeStr = "gprs";
                break;
            case 2:
                networkTypeStr = "edge";
                break;
            case 3:
                networkTypeStr = "umts";
                break;
            case 4:
                networkTypeStr = "cdma";
                break;
            case 5:
                networkTypeStr = "evdo";
                break;
            case 6:
                networkTypeStr = "evdo";
                break;
            case 7:
                networkTypeStr = "1xrtt";
                break;
            case 8:
                networkTypeStr = "hsdpa";
                break;
            case 9:
                networkTypeStr = "hsupa";
                break;
            case 10:
                networkTypeStr = "hspa";
                break;
            case 11:
                networkTypeStr = "iden";
                break;
            case 12:
                networkTypeStr = "evdo";
                break;
            case 13:
                networkTypeStr = "lte";
                break;
            case 14:
                networkTypeStr = "ehrpd";
                break;
            case 15:
                networkTypeStr = "hspap";
                break;
            default:
                loge("unknown network type: " + tm.getNetworkType());
                break;
        }
        return "net.tcp.buffersize." + networkTypeStr;
    }

    @Override
    public boolean teardown() {
        setTeardownRequested(true);
        return setEnableApn(this.mApnType, false) != 3;
    }

    public boolean isReady() {
        return this.mDataConnectionTrackerAc != null;
    }

    @Override
    public void captivePortalCheckCompleted(boolean isCaptivePortal) {
        if (this.mIsCaptivePortal.getAndSet(isCaptivePortal) != isCaptivePortal) {
            setEnableFailFastMobileData(isCaptivePortal ? 1 : 0);
        }
    }

    private void setDetailedState(NetworkInfo.DetailedState state, String reason, String extraInfo) {
        if (state != this.mNetworkInfo.getDetailedState()) {
            boolean wasConnecting = this.mNetworkInfo.getState() == NetworkInfo.State.CONNECTING;
            String lastReason = this.mNetworkInfo.getReason();
            if (wasConnecting && state == NetworkInfo.DetailedState.CONNECTED && reason == null && lastReason != null) {
                reason = lastReason;
            }
            this.mNetworkInfo.setDetailedState(state, reason, extraInfo);
            Message msg = this.mTarget.obtainMessage(458752, new NetworkInfo(this.mNetworkInfo));
            msg.sendToTarget();
        }
    }

    @Override
    public void setTeardownRequested(boolean isRequested) {
        this.mTeardownRequested = isRequested;
    }

    @Override
    public boolean isTeardownRequested() {
        return this.mTeardownRequested;
    }

    @Override
    public boolean reconnect() {
        setTeardownRequested(false);
        switch (setEnableApn(this.mApnType, true)) {
            case 0:
                break;
            case 1:
                this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.IDLE, null, null);
                break;
            case 2:
            case 3:
                break;
            default:
                loge("Error in reconnect - unexpected response.");
                break;
        }
        return false;
    }

    @Override
    public boolean setRadio(boolean turnOn) {
        getPhoneService(false);
        int retry = 0;
        while (true) {
            if (retry >= 2) {
                break;
            }
            if (this.mPhoneService == null) {
                loge("Ignoring mobile radio request because could not acquire PhoneService");
                break;
            }
            try {
                return this.mPhoneService.setRadio(turnOn);
            } catch (RemoteException e) {
                if (retry == 0) {
                    getPhoneService(true);
                }
                retry++;
            }
        }
    }

    public void setInternalDataEnable(boolean enabled) {
        AsyncChannel channel = this.mDataConnectionTrackerAc;
        if (channel != null) {
            channel.sendMessage(DctConstants.EVENT_SET_INTERNAL_DATA_ENABLE, enabled ? 1 : 0);
        }
    }

    @Override
    public void setUserDataEnable(boolean enabled) {
        AsyncChannel channel = this.mDataConnectionTrackerAc;
        if (channel != null) {
            channel.sendMessage(DctConstants.CMD_SET_USER_DATA_ENABLE, enabled ? 1 : 0);
            this.mUserDataEnabled = enabled;
        }
    }

    @Override
    public void setPolicyDataEnable(boolean enabled) {
        AsyncChannel channel = this.mDataConnectionTrackerAc;
        if (channel != null) {
            channel.sendMessage(DctConstants.CMD_SET_POLICY_DATA_ENABLE, enabled ? 1 : 0);
            this.mPolicyDataEnabled = enabled;
        }
    }

    public void setEnableFailFastMobileData(int enabled) {
        AsyncChannel channel = this.mDataConnectionTrackerAc;
        if (channel != null) {
            channel.sendMessage(DctConstants.CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA, enabled);
        }
    }

    @Override
    public void setDependencyMet(boolean met) {
        Bundle bundle = Bundle.forPair("apnType", this.mApnType);
        try {
            Message msg = Message.obtain();
            msg.what = DctConstants.CMD_SET_DEPENDENCY_MET;
            msg.arg1 = met ? 1 : 0;
            msg.setData(bundle);
            this.mDataConnectionTrackerAc.sendMessage(msg);
        } catch (NullPointerException e) {
            loge("setDependencyMet: X mAc was null" + e);
        }
    }

    public void enableMobileProvisioning(String url) {
        AsyncChannel channel = this.mDataConnectionTrackerAc;
        if (channel != null) {
            Message msg = Message.obtain();
            msg.what = DctConstants.CMD_ENABLE_MOBILE_PROVISIONING;
            msg.setData(Bundle.forPair(DctConstants.PROVISIONING_URL_KEY, url));
            channel.sendMessage(msg);
        }
    }

    public boolean isProvisioningNetwork() {
        try {
            Message msg = Message.obtain();
            msg.what = DctConstants.CMD_IS_PROVISIONING_APN;
            msg.setData(Bundle.forPair("apnType", this.mApnType));
            Message result = this.mDataConnectionTrackerAc.sendMessageSynchronously(msg);
            return result.arg1 == 1;
        } catch (NullPointerException e) {
            loge("isProvisioningNetwork: X " + e);
            return false;
        }
    }

    public String toString() {
        CharArrayWriter writer = new CharArrayWriter();
        PrintWriter pw = new PrintWriter(writer);
        pw.print("Mobile data state: ");
        pw.println(this.mMobileDataState);
        pw.print("Data enabled: user=");
        pw.print(this.mUserDataEnabled);
        pw.print(", policy=");
        pw.println(this.mPolicyDataEnabled);
        return writer.toString();
    }

    private int setEnableApn(String apnType, boolean enable) {
        getPhoneService(false);
        int retry = 0;
        while (true) {
            if (retry >= 2) {
                break;
            }
            if (this.mPhoneService != null) {
                retry++;
            } else {
                loge("Ignoring feature request because could not acquire PhoneService");
                break;
            }
        }
        loge("Could not " + (enable ? "enable" : "disable") + " APN type \"" + apnType + "\"");
        return 3;
    }

    public static String networkTypeToApnType(int netType) {
        switch (netType) {
            case 0:
                return PhoneConstants.APN_TYPE_DEFAULT;
            case 1:
            case 6:
            case 7:
            case 8:
            case 9:
            case 13:
            default:
                sloge("Error mapping networkType " + netType + " to apnType.");
                return null;
            case 2:
                return PhoneConstants.APN_TYPE_MMS;
            case 3:
                return PhoneConstants.APN_TYPE_SUPL;
            case 4:
                return PhoneConstants.APN_TYPE_DUN;
            case 5:
                return PhoneConstants.APN_TYPE_HIPRI;
            case 10:
                return PhoneConstants.APN_TYPE_FOTA;
            case 11:
                return PhoneConstants.APN_TYPE_IMS;
            case 12:
                return PhoneConstants.APN_TYPE_CBS;
            case 14:
                return PhoneConstants.APN_TYPE_IA;
            case 15:
                return PhoneConstants.APN_TYPE_EMERGENCY;
        }
    }

    @Override
    public LinkProperties getLinkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    @Override
    public void supplyMessenger(Messenger messenger) {
        AsyncChannel ac = new AsyncChannel();
        ac.connect(this.mContext, this.mHandler, messenger);
    }

    private void log(String s) {
        Slog.d(TAG, this.mApnType + ": " + s);
    }

    private void loge(String s) {
        Slog.e(TAG, this.mApnType + ": " + s);
    }

    private static void sloge(String s) {
        Slog.e(TAG, s);
    }

    @Override
    public LinkQualityInfo getLinkQualityInfo() {
        if (this.mNetworkInfo == null || this.mNetworkInfo.getType() == -1) {
            return null;
        }
        MobileLinkQualityInfo li = new MobileLinkQualityInfo();
        li.setNetworkType(this.mNetworkInfo.getType());
        this.mSamplingDataTracker.setCommonLinkQualityInfoFields(li);
        if (this.mNetworkInfo.getSubtype() != 0) {
            li.setMobileNetworkType(this.mNetworkInfo.getSubtype());
            NetworkDataEntry entry = getNetworkDataEntry(this.mNetworkInfo.getSubtype());
            if (entry != null) {
                li.setTheoreticalRxBandwidth(entry.downloadBandwidth);
                li.setTheoreticalRxBandwidth(entry.uploadBandwidth);
                li.setTheoreticalLatency(entry.latency);
            }
            if (this.mSignalStrength != null) {
                li.setNormalizedSignalStrength(getNormalizedSignalStrength(li.getMobileNetworkType(), this.mSignalStrength));
            }
        }
        SignalStrength ss = this.mSignalStrength;
        if (ss != null) {
            li.setRssi(ss.getGsmSignalStrength());
            li.setGsmErrorRate(ss.getGsmBitErrorRate());
            li.setCdmaDbm(ss.getCdmaDbm());
            li.setCdmaEcio(ss.getCdmaEcio());
            li.setEvdoDbm(ss.getEvdoDbm());
            li.setEvdoEcio(ss.getEvdoEcio());
            li.setEvdoSnr(ss.getEvdoSnr());
            li.setLteSignalStrength(ss.getLteSignalStrength());
            li.setLteRsrp(ss.getLteRsrp());
            li.setLteRsrq(ss.getLteRsrq());
            li.setLteRssnr(ss.getLteRssnr());
            li.setLteCqi(ss.getLteCqi());
            return li;
        }
        return li;
    }

    static class NetworkDataEntry {
        public int downloadBandwidth;
        public int latency;
        public int networkType;
        public int uploadBandwidth;

        NetworkDataEntry(int i1, int i2, int i3, int i4) {
            this.networkType = i1;
            this.downloadBandwidth = i2;
            this.uploadBandwidth = i3;
            this.latency = i4;
        }
    }

    private static NetworkDataEntry getNetworkDataEntry(int networkType) {
        NetworkDataEntry[] arr$ = mTheoreticalBWTable;
        for (NetworkDataEntry entry : arr$) {
            if (entry.networkType == networkType) {
                return entry;
            }
        }
        Slog.e(TAG, "Could not find Theoretical BW entry for " + String.valueOf(networkType));
        return null;
    }

    private static int getNormalizedSignalStrength(int networkType, SignalStrength ss) {
        int level;
        switch (networkType) {
            case 1:
            case 2:
            case 3:
            case 8:
            case 9:
            case 10:
            case 15:
                level = ss.getGsmLevel();
                break;
            case 4:
            case 7:
                level = ss.getCdmaLevel();
                break;
            case 5:
            case 6:
            case 12:
                level = ss.getEvdoLevel();
                break;
            case 11:
            case 14:
            default:
                return Integer.MAX_VALUE;
            case 13:
                level = ss.getLteLevel();
                break;
        }
        return (level * 100) / 5;
    }

    @Override
    public void startSampling(SamplingDataTracker.SamplingSnapshot s) {
        this.mSamplingDataTracker.startSampling(s);
    }

    @Override
    public void stopSampling(SamplingDataTracker.SamplingSnapshot s) {
        this.mSamplingDataTracker.stopSampling(s);
    }
}
