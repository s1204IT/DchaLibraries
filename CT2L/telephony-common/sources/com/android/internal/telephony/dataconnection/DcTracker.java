package com.android.internal.telephony.dataconnection;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RegistrantList;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Telephony;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.cdma.CDMALTEPhone;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.util.ArrayUtils;
import com.google.android.mms.pdu.CharacterSets;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DcTracker extends DcTrackerBase {
    static final String APN_ID = "apn_id";
    private static final int EVENT_IMSI_READY = 100;
    private static final int POLL_PDP_MILLIS = 5000;
    static final Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID = Uri.parse("content://telephony/carriers/preferapn_no_update/subId/");
    private static final int PROVISIONING_SPINNER_TIMEOUT_MILLIS = 120000;
    private static final String PUPPET_MASTER_RADIO_STRESS_TEST = "gsm.defaultpdpcontext.active";
    protected final String LOG_TAG;
    private RegistrantList mAllDataDisconnectedRegistrants;
    private ApnChangeObserver mApnObserver;
    private AtomicBoolean mAttached;
    private boolean mCanSetPreferApn;
    private boolean mDeregistrationAlarmState;
    private ArrayList<Message> mDisconnectAllCompleteMsgList;
    protected int mDisconnectPendingCount;
    private boolean mEventRegistered;
    private PendingIntent mImsDeregistrationDelayIntent;
    public boolean mImsRegistrationState;
    private boolean mIsDataSuspendedByVoice;
    private final String mProvisionActionName;
    private BroadcastReceiver mProvisionBroadcastReceiver;
    private ProgressDialog mProvisioningSpinner;
    private boolean mReregisterOnReconnectFailure;
    private ApnContext mWaitCleanUpApnContext;

    private enum RetryFailures {
        ALWAYS,
        ONLY_ON_CHANGE
    }

    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver() {
            super(DcTracker.this.mDataConnectionTracker);
        }

        @Override
        public void onChange(boolean selfChange) {
            DcTracker.this.sendMessage(DcTracker.this.obtainMessage(270355));
        }
    }

    public DcTracker(PhoneBase p) {
        super(p);
        this.LOG_TAG = "DCT";
        this.mDisconnectAllCompleteMsgList = new ArrayList<>();
        this.mAllDataDisconnectedRegistrants = new RegistrantList();
        this.mDisconnectPendingCount = 0;
        this.mReregisterOnReconnectFailure = false;
        this.mIsDataSuspendedByVoice = false;
        this.mCanSetPreferApn = false;
        this.mAttached = new AtomicBoolean(false);
        this.mImsRegistrationState = false;
        this.mWaitCleanUpApnContext = null;
        this.mDeregistrationAlarmState = false;
        this.mImsDeregistrationDelayIntent = null;
        this.mEventRegistered = false;
        log("GsmDCT.constructor");
        this.mDataConnectionTracker = this;
        update();
        this.mApnObserver = new ApnChangeObserver();
        p.getContext().getContentResolver().registerContentObserver(Telephony.Carriers.CONTENT_URI, true, this.mApnObserver);
        initApnContexts();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.internal.telephony.data-reconnect." + apnContext.getApnType());
            filter.addAction("com.android.internal.telephony.data-restart-trysetup." + apnContext.getApnType());
            this.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mPhone);
        }
        initEmergencyApnSetting();
        addEmergencyApnSetting();
        this.mProvisionActionName = "com.android.internal.telephony.PROVISION" + p.getPhoneId();
        this.mPhone.getCallTracker().registerForVoiceCallEnded(this, 270344, null);
        this.mPhone.getCallTracker().registerForVoiceCallStarted(this, 270343, null);
    }

    protected void registerForAllEvents() {
        if (this.mEventRegistered) {
            log("All events registered, ignore...");
            return;
        }
        this.mEventRegistered = true;
        this.mPhone.mCi.registerForAvailable(this, 270337, null);
        this.mPhone.mCi.registerForOffOrNotAvailable(this, 270342, null);
        this.mPhone.mCi.registerForDataNetworkStateChanged(this, 270340, null);
        this.mPhone.getServiceStateTracker().registerForDataConnectionAttached(this, 270352, null);
        this.mPhone.getServiceStateTracker().registerForDataConnectionDetached(this, 270345, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOn(this, 270347, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOff(this, 270348, null);
        this.mPhone.getServiceStateTracker().registerForPsRestrictedEnabled(this, 270358, null);
        this.mPhone.getServiceStateTracker().registerForPsRestrictedDisabled(this, 270359, null);
        this.mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(this, 270377, null);
    }

    @Override
    public void dispose() {
        log("DcTracker.dispose");
        if (this.mProvisionBroadcastReceiver != null) {
            this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
            this.mProvisionBroadcastReceiver = null;
        }
        if (this.mProvisioningSpinner != null) {
            this.mProvisioningSpinner.dismiss();
            this.mProvisioningSpinner = null;
        }
        cleanUpAllConnections(true, (String) null);
        super.dispose();
        this.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mApnObserver);
        this.mApnContexts.clear();
        this.mPrioritySortedApnContexts.clear();
        destroyDataConnections();
    }

    protected void unregisterForAllEvents() {
        if (!this.mEventRegistered) {
            log("All events unregistered, ignore...");
            return;
        }
        this.mEventRegistered = false;
        this.mPhone.mCi.unregisterForAvailable(this);
        this.mPhone.mCi.unregisterForOffOrNotAvailable(this);
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            r.unregisterForImsiReady(this);
            this.mIccRecords.set(null);
            r.unregisterForRecordsLoaded(this);
        }
        this.mPhone.mCi.unregisterForDataNetworkStateChanged(this);
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionAttached(this);
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionDetached(this);
        this.mPhone.getServiceStateTracker().unregisterForDataRoamingOn(this);
        this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
        this.mPhone.getServiceStateTracker().unregisterForPsRestrictedEnabled(this);
        this.mPhone.getServiceStateTracker().unregisterForPsRestrictedDisabled(this);
    }

    @Override
    public void incApnRefCount(String name) {
        ApnContext apnContext = this.mApnContexts.get(name);
        if (apnContext != null) {
            apnContext.incRefCount();
        }
    }

    @Override
    public void decApnRefCount(String name) {
        ApnContext apnContext = this.mApnContexts.get(name);
        if (apnContext != null) {
            apnContext.decRefCount();
        }
    }

    @Override
    public boolean isApnSupported(String name) {
        if (name == null) {
            loge("isApnSupported: name=null");
            return false;
        }
        ApnContext apnContext = this.mApnContexts.get(name);
        if (apnContext == null) {
            loge("Request for unsupported mobile name: " + name);
            return false;
        }
        return true;
    }

    @Override
    public int getApnPriority(String name) {
        ApnContext apnContext = this.mApnContexts.get(name);
        if (apnContext == null) {
            loge("Request for unsupported mobile name: " + name);
        }
        return apnContext.priority;
    }

    private void setRadio(boolean on) {
        ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        try {
            phone.setRadio(on);
        } catch (Exception e) {
        }
    }

    private class ProvisionNotificationBroadcastReceiver extends BroadcastReceiver {
        private final String mNetworkOperator;
        private final String mProvisionUrl;

        public ProvisionNotificationBroadcastReceiver(String provisionUrl, String networkOperator) {
            this.mNetworkOperator = networkOperator;
            this.mProvisionUrl = provisionUrl;
        }

        private void setEnableFailFastMobileData(int enabled) {
            DcTracker.this.sendMessage(DcTracker.this.obtainMessage(270372, enabled, 0));
        }

        private void enableMobileProvisioning() {
            Message msg = DcTracker.this.obtainMessage(270373);
            msg.setData(Bundle.forPair("provisioningUrl", this.mProvisionUrl));
            DcTracker.this.sendMessage(msg);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DcTracker.this.mProvisioningSpinner = new ProgressDialog(context);
            DcTracker.this.mProvisioningSpinner.setTitle(this.mNetworkOperator);
            DcTracker.this.mProvisioningSpinner.setMessage(context.getText(R.string.media_route_status_connecting));
            DcTracker.this.mProvisioningSpinner.setIndeterminate(true);
            DcTracker.this.mProvisioningSpinner.setCancelable(true);
            DcTracker.this.mProvisioningSpinner.getWindow().setType(2009);
            DcTracker.this.mProvisioningSpinner.show();
            DcTracker.this.sendMessageDelayed(DcTracker.this.obtainMessage(270378, DcTracker.this.mProvisioningSpinner), 120000L);
            DcTracker.this.setRadio(true);
            setEnableFailFastMobileData(1);
            enableMobileProvisioning();
        }
    }

    @Override
    public boolean isApnTypeActive(String type) {
        ApnContext apnContext = this.mApnContexts.get(type);
        return (apnContext == null || apnContext.getDcAc() == null) ? false : true;
    }

    @Override
    public boolean isDataPossible(String apnType) {
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext == null) {
            return false;
        }
        boolean apnContextIsEnabled = apnContext.isEnabled();
        DctConstants.State apnContextState = apnContext.getState();
        boolean apnTypePossible = (apnContextIsEnabled && apnContextState == DctConstants.State.FAILED) ? false : true;
        boolean isEmergencyApn = apnContext.getApnType().equals("emergency");
        boolean dataAllowed = isEmergencyApn || isDataAllowed();
        boolean possible = dataAllowed && apnTypePossible;
        return possible;
    }

    protected void finalize() {
        log("finalize");
    }

    protected void supplyMessenger() {
        ConnectivityManager cm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        cm.supplyMessenger(0, new Messenger(this));
        cm.supplyMessenger(2, new Messenger(this));
        cm.supplyMessenger(3, new Messenger(this));
        cm.supplyMessenger(4, new Messenger(this));
        cm.supplyMessenger(5, new Messenger(this));
        cm.supplyMessenger(10, new Messenger(this));
        cm.supplyMessenger(11, new Messenger(this));
        cm.supplyMessenger(12, new Messenger(this));
        cm.supplyMessenger(15, new Messenger(this));
    }

    private ApnContext addApnContext(String type, NetworkConfig networkConfig) {
        ApnContext apnContext = new ApnContext(this.mPhone.getContext(), type, "DCT", networkConfig, this);
        this.mApnContexts.put(type, apnContext);
        this.mPrioritySortedApnContexts.add(apnContext);
        return apnContext;
    }

    protected void initApnContexts() {
        ApnContext apnContext;
        log("initApnContexts: E");
        String[] networkConfigStrings = this.mPhone.getContext().getResources().getStringArray(R.array.config_ambientBrighteningThresholds);
        for (String networkConfigString : networkConfigStrings) {
            NetworkConfig networkConfig = new NetworkConfig(networkConfigString);
            switch (networkConfig.type) {
                case 0:
                    apnContext = addApnContext("default", networkConfig);
                    break;
                case 1:
                case 6:
                case 7:
                case 8:
                case 9:
                case 13:
                default:
                    log("initApnContexts: skipping unknown type=" + networkConfig.type);
                    continue;
                    break;
                case 2:
                    apnContext = addApnContext("mms", networkConfig);
                    break;
                case 3:
                    apnContext = addApnContext("supl", networkConfig);
                    break;
                case 4:
                    apnContext = addApnContext("dun", networkConfig);
                    break;
                case 5:
                    apnContext = addApnContext("hipri", networkConfig);
                    break;
                case 10:
                    apnContext = addApnContext("fota", networkConfig);
                    break;
                case 11:
                    apnContext = addApnContext("ims", networkConfig);
                    break;
                case 12:
                    apnContext = addApnContext("cbs", networkConfig);
                    break;
                case 14:
                    apnContext = addApnContext("ia", networkConfig);
                    break;
                case 15:
                    apnContext = addApnContext("emergency", networkConfig);
                    break;
            }
            log("initApnContexts: apnContext=" + apnContext);
        }
        log("initApnContexts: X mApnContexts=" + this.mApnContexts);
    }

    @Override
    public LinkProperties getLinkProperties(String apnType) {
        DcAsyncChannel dcac;
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext != null && (dcac = apnContext.getDcAc()) != null) {
            log("return link properites for " + apnType);
            return dcac.getLinkPropertiesSync();
        }
        log("return new LinkProperties");
        return new LinkProperties();
    }

    @Override
    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        DcAsyncChannel dataConnectionAc;
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext != null && (dataConnectionAc = apnContext.getDcAc()) != null) {
            log("get active pdp is not null, return NetworkCapabilities for " + apnType);
            return dataConnectionAc.getNetworkCapabilitiesSync();
        }
        log("return new NetworkCapabilities");
        return new NetworkCapabilities();
    }

    @Override
    public String[] getActiveApnTypes() {
        log("get all active apn types");
        ArrayList<String> result = new ArrayList<>();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady()) {
                result.add(apnContext.getApnType());
            }
        }
        return (String[]) result.toArray(new String[0]);
    }

    @Override
    public String getActiveApnString(String apnType) {
        ApnSetting apnSetting;
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext == null || (apnSetting = apnContext.getApnSetting()) == null) {
            return null;
        }
        return apnSetting.apn;
    }

    @Override
    public boolean isApnTypeEnabled(String apnType) {
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext == null) {
            return false;
        }
        return apnContext.isEnabled();
    }

    @Override
    protected void setState(DctConstants.State s) {
        log("setState should not be used in GSM" + s);
    }

    @Override
    public DctConstants.State getState(String apnType) {
        ApnContext apnContext = this.mApnContexts.get(apnType);
        return apnContext != null ? apnContext.getState() : DctConstants.State.FAILED;
    }

    @Override
    protected boolean isProvisioningApn(String apnType) {
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.isProvisioningApn();
        }
        return false;
    }

    @Override
    public DctConstants.State getOverallState() {
        boolean isConnecting = false;
        boolean isFailed = true;
        boolean isAnyEnabled = false;
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.isEnabled()) {
                isAnyEnabled = true;
                switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$DctConstants$State[apnContext.getState().ordinal()]) {
                    case 1:
                    case 2:
                        log("overall state is CONNECTED");
                        return DctConstants.State.CONNECTED;
                    case 3:
                    case 4:
                        isConnecting = true;
                        isFailed = false;
                        break;
                    case 5:
                    case 6:
                        isFailed = false;
                        break;
                    default:
                        isAnyEnabled = true;
                        break;
                }
            }
        }
        if (!isAnyEnabled) {
            log("overall state is IDLE");
            return DctConstants.State.IDLE;
        }
        if (isConnecting) {
            log("overall state is CONNECTING");
            return DctConstants.State.CONNECTING;
        }
        if (!isFailed) {
            log("overall state is IDLE");
            return DctConstants.State.IDLE;
        }
        log("overall state is FAILED");
        return DctConstants.State.FAILED;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$State = new int[DctConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.DISCONNECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.RETRYING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.IDLE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.SCANNING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.FAILED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    @Override
    protected boolean isApnTypeAvailable(String type) {
        if (type.equals("dun") && fetchDunApn() != null) {
            return true;
        }
        if (this.mAllApnSettings != null) {
            for (ApnSetting apn : this.mAllApnSettings) {
                if (apn.canHandleType(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean getAnyDataEnabled() {
        boolean z = false;
        synchronized (this.mDataEnabledLock) {
            if (!this.mInternalDataEnabled || !this.mUserDataEnabled || !sPolicyDataEnabled) {
                log("getAnyDataEnabled " + this.mInternalDataEnabled + "," + this.mUserDataEnabled + "," + sPolicyDataEnabled);
            } else {
                Iterator<ApnContext> it = this.mApnContexts.values().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ApnContext apnContext = it.next();
                    if (isDataAllowed(apnContext)) {
                        z = true;
                        break;
                    }
                }
            }
        }
        return z;
    }

    public boolean getAnyDataEnabled(boolean checkUserDataEnabled, boolean checkPolicyDataEnabled) {
        boolean z = false;
        synchronized (this.mDataEnabledLock) {
            if (!this.mInternalDataEnabled || ((checkUserDataEnabled && !this.mUserDataEnabled) || (checkPolicyDataEnabled && !sPolicyDataEnabled))) {
                log("getAnyDataEnabled " + this.mInternalDataEnabled + "," + this.mUserDataEnabled + "," + sPolicyDataEnabled);
            } else {
                Iterator<ApnContext> it = this.mApnContexts.values().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ApnContext apnContext = it.next();
                    if (isDataAllowed(apnContext)) {
                        z = true;
                        break;
                    }
                }
            }
        }
        return z;
    }

    private boolean isDataAllowed(ApnContext apnContext) {
        return apnContext.isReady() && isDataAllowed();
    }

    protected void onDataConnectionDetached() {
        log("onDataConnectionDetached: stop polling and notify detached");
        stopNetStatPoll();
        stopDataStallAlarm();
        notifyDataConnection(Phone.REASON_DATA_DETACHED);
        this.mAttached.set(false);
    }

    private void onDataConnectionAttached() {
        log("onDataConnectionAttached");
        this.mAttached.set(true);
        if (getOverallState() == DctConstants.State.CONNECTED) {
            log("onDataConnectionAttached: start polling notify attached");
            startNetStatPoll();
            startDataStallAlarm(false);
            notifyDataConnection(Phone.REASON_DATA_ATTACHED);
        } else {
            notifyOffApnsOfAvailability(Phone.REASON_DATA_ATTACHED);
        }
        if (this.mAutoAttachOnCreationConfig) {
            this.mAutoAttachOnCreation = true;
        }
        setupDataOnConnectableApns(Phone.REASON_DATA_ATTACHED);
    }

    @Override
    public boolean isDataBlockedByOther() {
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (tm.getMultiSimConfiguration() != TelephonyManager.MultiSimVariants.DSDS) {
            return false;
        }
        int count = tm.getPhoneCount();
        for (int i = 0; i < count; i++) {
            if (i != this.mPhone.getPhoneId()) {
                int[] subIds = SubscriptionManager.getSubId(i);
                if (subIds != null) {
                    if (tm.getCallState(subIds[0]) != 0) {
                        log("Phone " + i + " is not IDLE.");
                        return true;
                    }
                } else {
                    loge("Fail to getSubId, slot ID = " + i);
                }
            }
        }
        return false;
    }

    @Override
    protected boolean isDataAllowed() {
        boolean internalDataEnabled;
        synchronized (this.mDataEnabledLock) {
            internalDataEnabled = this.mInternalDataEnabled;
        }
        boolean attachedState = this.mAttached.get();
        boolean desiredPowerState = this.mPhone.getServiceStateTracker().getDesiredPowerState();
        IccRecords r = this.mIccRecords.get();
        boolean recordsLoaded = false;
        if (r != null) {
            String imsi = r.getIMSI();
            recordsLoaded = imsi != null;
            log("isDataAllowed getRecordsLoaded=" + recordsLoaded);
            log("isDataAllowed imsi=" + imsi);
        }
        boolean psRestricted = this.mIsPsRestricted;
        PhoneConstants.State state = PhoneConstants.State.IDLE;
        if (this.mPhone.getCallTracker() != null) {
            state = this.mPhone.getCallTracker().getState();
        }
        boolean allowed = (attachedState || this.mAutoAttachOnCreation) && recordsLoaded && (state == PhoneConstants.State.IDLE || this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) && !isDataBlockedByOther() && internalDataEnabled && ((!this.mPhone.getServiceState().getDataRoaming() || getDataOnRoamingEnabled()) && !psRestricted && desiredPowerState);
        if (!allowed) {
            String reason = "";
            if (!attachedState && !this.mAutoAttachOnCreation) {
                reason = " - Attached= " + attachedState;
            }
            if (!recordsLoaded) {
                reason = reason + " - IMSI not loaded";
            }
            if (state != PhoneConstants.State.IDLE && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                reason = (reason + " - PhoneState= " + state) + " - Concurrent voice and data not allowed";
            }
            if (isDataBlockedByOther()) {
                reason = (reason + " Other Phone is not IDLE") + " - Voice on other phone, data not allowed";
            }
            if (!internalDataEnabled) {
                reason = reason + " - mInternalDataEnabled= false";
            }
            if (this.mPhone.getServiceState().getDataRoaming() && !getDataOnRoamingEnabled()) {
                reason = reason + " - Roaming and data roaming not enabled";
            }
            if (this.mIsPsRestricted) {
                reason = reason + " - mIsPsRestricted= true";
            }
            if (!desiredPowerState) {
                reason = reason + " - desiredPowerState= false";
            }
            log("isDataAllowed: not allowed due to" + reason);
        }
        return allowed;
    }

    private void setupDataOnConnectableApns(String reason) {
        setupDataOnConnectableApns(reason, RetryFailures.ALWAYS);
    }

    private void setupDataOnConnectableApns(String reason, RetryFailures retryFailures) {
        log("setupDataOnConnectableApns: " + reason);
        ArrayList<ApnSetting> waitingApns = null;
        for (ApnContext apnContext : this.mPrioritySortedApnContexts) {
            log("setupDataOnConnectableApns: apnContext " + apnContext);
            if (apnContext.getState() == DctConstants.State.FAILED) {
                if (retryFailures == RetryFailures.ALWAYS) {
                    apnContext.setState(DctConstants.State.IDLE);
                } else if (!apnContext.isConcurrentVoiceAndDataAllowed() && this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                    apnContext.setState(DctConstants.State.IDLE);
                } else {
                    int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
                    ArrayList<ApnSetting> originalApns = apnContext.getOriginalWaitingApns();
                    if (originalApns != null && !originalApns.isEmpty()) {
                        waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
                        if (originalApns.size() != waitingApns.size() || !originalApns.containsAll(waitingApns)) {
                            apnContext.setState(DctConstants.State.IDLE);
                        }
                    }
                }
            }
            if (apnContext.isConnectable()) {
                log("setupDataOnConnectableApns: isConnectable() call trySetupData");
                apnContext.setReason(reason);
                trySetupData(apnContext, waitingApns);
            }
        }
    }

    private boolean trySetupData(ApnContext apnContext) {
        return trySetupData(apnContext, null);
    }

    private boolean trySetupData(ApnContext apnContext, ArrayList<ApnSetting> waitingApns) {
        log("trySetupData for type:" + apnContext.getApnType() + " due to " + apnContext.getReason() + " apnContext=" + apnContext);
        log("trySetupData with mIsPsRestricted=" + this.mIsPsRestricted);
        if (this.mPhone.getSimulatedRadioControl() != null) {
            apnContext.setState(DctConstants.State.CONNECTED);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            log("trySetupData: X We're on the simulator; assuming connected retValue=true");
            return true;
        }
        boolean isEmergencyApn = apnContext.getApnType().equals("emergency");
        ServiceStateTracker sst = this.mPhone.getServiceStateTracker();
        sst.getDesiredPowerState();
        boolean checkUserDataEnabled = apnContext.getApnType().equals("default");
        boolean checkPolicyDataEnabled = apnContext.getApnType().equals("ims") ? false : true;
        if (apnContext.isConnectable() && (isEmergencyApn || (isDataAllowed(apnContext) && getAnyDataEnabled(checkUserDataEnabled, checkPolicyDataEnabled) && !isEmergency()))) {
            if (apnContext.getState() == DctConstants.State.FAILED) {
                log("trySetupData: make a FAILED ApnContext IDLE so its reusable");
                apnContext.setState(DctConstants.State.IDLE);
            }
            int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
            apnContext.setConcurrentVoiceAndDataAllowed(sst.isConcurrentVoiceAndDataAllowed());
            if (apnContext.getState() == DctConstants.State.IDLE) {
                if (waitingApns == null) {
                    waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
                }
                if (waitingApns.isEmpty()) {
                    notifyNoData(DcFailCause.MISSING_UNKNOWN_APN, apnContext);
                    notifyOffApnsOfAvailability(apnContext.getReason());
                    log("trySetupData: X No APN found retValue=false");
                    return false;
                }
                apnContext.setWaitingApns(waitingApns);
                log("trySetupData: Create from mAllApnSettings : " + apnListToString(this.mAllApnSettings));
            }
            log("trySetupData: call setupData, waitingApns : " + apnListToString(apnContext.getWaitingApns()));
            boolean retValue = setupData(apnContext, radioTech);
            notifyOffApnsOfAvailability(apnContext.getReason());
            log("trySetupData: X retValue=" + retValue);
            return retValue;
        }
        if (!apnContext.getApnType().equals("default") && apnContext.isConnectable()) {
            this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
        }
        notifyOffApnsOfAvailability(apnContext.getReason());
        log("trySetupData: X apnContext not 'ready' retValue=false");
        return false;
    }

    @Override
    protected void notifyOffApnsOfAvailability(String reason) {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!this.mAttached.get() || !apnContext.isReady()) {
                this.mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(), apnContext.getApnType(), PhoneConstants.DataState.DISCONNECTED);
            }
        }
    }

    protected boolean cleanUpAllConnections(boolean tearDown, String reason) {
        log("cleanUpAllConnections: tearDown=" + tearDown + " reason=" + reason);
        boolean didDisconnect = false;
        boolean specificdisable = false;
        if (!TextUtils.isEmpty(reason)) {
            specificdisable = reason.equals(Phone.REASON_DATA_SPECIFIC_DISABLED);
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!apnContext.isDisconnected()) {
                didDisconnect = true;
            }
            if (specificdisable) {
                if (!apnContext.getApnType().equals("ims")) {
                    log("ApnConextType: " + apnContext.getApnType());
                    apnContext.setReason(reason);
                    cleanUpConnection(tearDown, apnContext);
                }
            } else {
                apnContext.setReason(reason);
                cleanUpConnection(tearDown, apnContext);
            }
        }
        stopNetStatPoll();
        stopDataStallAlarm();
        this.mRequestedApnType = "default";
        log("cleanUpConnection: mDisconnectPendingCount = " + this.mDisconnectPendingCount);
        if (tearDown && this.mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
        return didDisconnect;
    }

    @Override
    protected void onCleanUpAllConnections(String cause) {
        cleanUpAllConnections(true, cause);
    }

    @Override
    protected void onCleanUpAllConnectionsWithException(String cause, ArrayList<String> exceptList) {
        log("onCleanUpAllConnectionsWithException: reason=" + cause + ", exceptList = " + exceptList);
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (exceptList == null || exceptList.indexOf(apnContext.getApnType()) == -1) {
                apnContext.setReason(cause);
                cleanUpConnection(true, apnContext);
            }
        }
    }

    @Override
    protected void onCleanUpAllConnectionsExceptIms(String cause) {
        log("cleanUpAllConnectionsExceptIms: reason=" + cause);
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!apnContext.getApnType().equals("ims")) {
                apnContext.setReason(cause);
                cleanUpConnection(true, apnContext);
            }
        }
    }

    protected void cleanUpConnection(boolean tearDown, ApnContext apnContext) {
        if (apnContext == null) {
            log("cleanUpConnection: apn context is null");
            return;
        }
        DcAsyncChannel dcac = apnContext.getDcAc();
        log("cleanUpConnection: E tearDown=" + tearDown + " reason=" + apnContext.getReason() + " apnContext=" + apnContext);
        if (tearDown) {
            if (apnContext.isDisconnected()) {
                apnContext.setState(DctConstants.State.IDLE);
                if (!apnContext.isReady()) {
                    if (dcac != null) {
                        log("cleanUpConnection: teardown, disconnected, !ready apnContext=" + apnContext);
                        dcac.tearDown(apnContext, "", null);
                    }
                    apnContext.setDataConnectionAc(null);
                }
            } else if (dcac != null) {
                if (apnContext.getState() != DctConstants.State.DISCONNECTING) {
                    boolean disconnectAll = false;
                    if ("dun".equals(apnContext.getApnType()) && teardownForDun()) {
                        log("cleanUpConnection: disconnectAll DUN connection");
                        disconnectAll = true;
                    }
                    log("cleanUpConnection: tearing down" + (disconnectAll ? " all" : "") + "apnContext=" + apnContext);
                    Message msg = obtainMessage(270351, apnContext);
                    if (disconnectAll) {
                        apnContext.getDcAc().tearDownAll(apnContext.getReason(), msg);
                    } else {
                        apnContext.getDcAc().tearDown(apnContext, apnContext.getReason(), msg);
                    }
                    apnContext.setState(DctConstants.State.DISCONNECTING);
                    this.mDisconnectPendingCount++;
                }
            } else {
                apnContext.setState(DctConstants.State.IDLE);
                this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            }
        } else {
            if (dcac != null) {
                dcac.reqReset();
            }
            apnContext.setState(DctConstants.State.IDLE);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            apnContext.setDataConnectionAc(null);
        }
        if (dcac != null) {
            cancelReconnectAlarm(apnContext);
        }
        log("cleanUpConnection: X tearDown=" + tearDown + " reason=" + apnContext.getReason() + " apnContext=" + apnContext + " dcac=" + apnContext.getDcAc());
    }

    private boolean teardownForDun() {
        int rilRat = this.mPhone.getServiceState().getRilDataRadioTechnology();
        return ServiceState.isCdma(rilRat) || fetchDunApn() != null;
    }

    private void cancelReconnectAlarm(ApnContext apnContext) {
        PendingIntent intent;
        if (apnContext != null && (intent = apnContext.getReconnectIntent()) != null) {
            AlarmManager am = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
            am.cancel(intent);
            apnContext.setReconnectIntent(null);
        }
    }

    private String[] parseTypes(String types) {
        if (types == null || types.equals("")) {
            String[] result = {CharacterSets.MIMENAME_ANY_CHARSET};
            return result;
        }
        return types.split(",");
    }

    private boolean imsiMatches(String imsiDB, String imsiSIM) {
        int len = imsiDB.length();
        if (len <= 0 || len > imsiSIM.length()) {
            return false;
        }
        for (int idx = 0; idx < len; idx++) {
            char c = imsiDB.charAt(idx);
            if (c != 'x' && c != 'X' && c != imsiSIM.charAt(idx)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean mvnoMatches(IccRecords r, String mvnoType, String mvnoMatchData) {
        if (mvnoType.equalsIgnoreCase("spn")) {
            if (r.getServiceProviderName() != null && r.getServiceProviderName().equalsIgnoreCase(mvnoMatchData)) {
                return true;
            }
        } else if (mvnoType.equalsIgnoreCase("imsi")) {
            String imsiSIM = r.getIMSI();
            if (imsiSIM != null && imsiMatches(mvnoMatchData, imsiSIM)) {
                return true;
            }
        } else if (mvnoType.equalsIgnoreCase("gid")) {
            String gid1 = r.getGid1();
            int mvno_match_data_length = mvnoMatchData.length();
            if (gid1 != null && gid1.length() >= mvno_match_data_length && gid1.substring(0, mvno_match_data_length).equalsIgnoreCase(mvnoMatchData)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isPermanentFail(DcFailCause dcFailCause) {
        return dcFailCause.isPermanentFail() && !(this.mAttached.get() && dcFailCause == DcFailCause.SIGNAL_LOST);
    }

    private ApnSetting makeApnSetting(Cursor cursor) {
        String[] types = parseTypes(cursor.getString(cursor.getColumnIndexOrThrow("type")));
        ApnSetting apn = new ApnSetting(cursor.getInt(cursor.getColumnIndexOrThrow("_id")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)), cursor.getString(cursor.getColumnIndexOrThrow("name")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)), types, cursor.getString(cursor.getColumnIndexOrThrow("protocol")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.ROAMING_PROTOCOL)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.CARRIER_ENABLED)) == 1, cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROFILE_ID)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MODEM_COGNITIVE)) == 1, cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.WAIT_TIME)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS_TIME)), cursor.getInt(cursor.getColumnIndexOrThrow("mtu")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_TYPE)), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_MATCH_DATA)));
        return apn;
    }

    private ArrayList<ApnSetting> createApnList(Cursor cursor) {
        ArrayList<ApnSetting> mnoApns = new ArrayList<>();
        ArrayList<ApnSetting> mvnoApns = new ArrayList<>();
        IccRecords r = this.mIccRecords.get();
        if (cursor.moveToFirst()) {
            do {
                ApnSetting apn = makeApnSetting(cursor);
                if (apn != null) {
                    if (apn.hasMvnoParams()) {
                        if (r != null && mvnoMatches(r, apn.mvnoType, apn.mvnoMatchData)) {
                            mvnoApns.add(apn);
                        }
                    } else {
                        mnoApns.add(apn);
                    }
                }
            } while (cursor.moveToNext());
        }
        ArrayList<ApnSetting> result = mvnoApns.isEmpty() ? mnoApns : mvnoApns;
        log("createApnList: X result=" + result);
        return result;
    }

    private boolean dataConnectionNotInUse(DcAsyncChannel dcac) {
        log("dataConnectionNotInUse: check if dcac is inuse dcac=" + dcac);
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getDcAc() == dcac) {
                log("dataConnectionNotInUse: in use by apnContext=" + apnContext);
                return false;
            }
        }
        log("dataConnectionNotInUse: tearDownAll");
        dcac.tearDownAll("No connection", null);
        log("dataConnectionNotInUse: not in use return true");
        return true;
    }

    private DcAsyncChannel findFreeDataConnection() {
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            if (dcac.isInactiveSync() && dataConnectionNotInUse(dcac)) {
                log("findFreeDataConnection: found free DataConnection= dcac=" + dcac);
                return dcac;
            }
        }
        log("findFreeDataConnection: NO free DataConnection");
        return null;
    }

    private boolean setupData(ApnContext apnContext, int radioTech) {
        ApnSetting dcacApnSetting;
        log("setupData: apnContext=" + apnContext);
        DcAsyncChannel dcac = null;
        ApnSetting apnSetting = apnContext.getNextWaitingApn();
        if (apnSetting == null) {
            log("setupData: return for no apn found!");
            return false;
        }
        int profileId = apnSetting.profileId;
        if (profileId == 0) {
            profileId = getApnProfileID(apnContext.getApnType());
        }
        if ((apnContext.getApnType() != "dun" || !teardownForDun()) && (dcac = checkForCompatibleConnectedApnContext(apnContext)) != null && (dcacApnSetting = dcac.getApnSettingSync()) != null) {
            apnSetting = dcacApnSetting;
        }
        if (dcac == null) {
            if (isOnlySingleDcAllowed(radioTech)) {
                if (isHigherPriorityApnContextActive(apnContext)) {
                    log("setupData: Higher priority ApnContext active.  Ignoring call");
                    return false;
                }
                if (cleanUpAllConnections(true, Phone.REASON_SINGLE_PDN_ARBITRATION)) {
                    log("setupData: Some calls are disconnecting first.  Wait and retry");
                    return false;
                }
                log("setupData: Single pdp. Continue setting up data call.");
            }
            dcac = findFreeDataConnection();
            if (dcac == null) {
                dcac = createDataConnection();
            }
            if (dcac == null) {
                log("setupData: No free DataConnection and couldn't create one, WEIRD");
                return false;
            }
        }
        log("setupData: dcac=" + dcac + " apnSetting=" + apnSetting);
        apnContext.setDataConnectionAc(dcac);
        apnContext.setApnSetting(apnSetting);
        apnContext.setState(DctConstants.State.CONNECTING);
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        Message msg = obtainMessage();
        msg.what = 270336;
        msg.obj = apnContext;
        dcac.bringUp(apnContext, getInitialMaxRetry(), profileId, radioTech, this.mAutoAttachOnCreation, msg);
        log("setupData: initing!");
        return true;
    }

    private void onApnChanged() {
        DctConstants.State overallState = getOverallState();
        boolean isDisconnected = overallState == DctConstants.State.IDLE || overallState == DctConstants.State.FAILED;
        if (this.mPhone instanceof GSMPhone) {
            ((GSMPhone) this.mPhone).updateCurrentCarrierInProvider();
        }
        log("onApnChanged: createAllApnList and cleanUpAllConnections");
        createAllApnList();
        setInitialAttachApn();
        cleanUpConnectionsOnUpdatedApns(isDisconnected ? false : true);
        if (this.mPhone.getSubId() == SubscriptionManager.getDefaultDataSubId()) {
            setupDataOnConnectableApns(Phone.REASON_APN_CHANGED);
        }
    }

    private DcAsyncChannel findDataConnectionAcByCid(int cid) {
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            if (dcac.getCidSync() == cid) {
                return dcac;
            }
        }
        return null;
    }

    @Override
    protected void gotoIdleAndNotifyDataConnection(String reason) {
        log("gotoIdleAndNotifyDataConnection: reason=" + reason);
        notifyDataConnection(reason);
        this.mActiveApn = null;
    }

    private boolean isHigherPriorityApnContextActive(ApnContext apnContext) {
        for (ApnContext otherContext : this.mPrioritySortedApnContexts) {
            if (apnContext.getApnType().equalsIgnoreCase(otherContext.getApnType())) {
                return false;
            }
            if (otherContext.isEnabled() && otherContext.getState() != DctConstants.State.FAILED) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnlySingleDcAllowed(int rilRadioTech) {
        int[] singleDcRats = this.mPhone.getContext().getResources().getIntArray(R.array.config_cameraPrivacyLightColors);
        boolean onlySingleDcAllowed = false;
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("persist.telephony.test.singleDc", false)) {
            onlySingleDcAllowed = true;
        }
        if (singleDcRats != null) {
            for (int i = 0; i < singleDcRats.length && !onlySingleDcAllowed; i++) {
                if (rilRadioTech == singleDcRats[i]) {
                    onlySingleDcAllowed = true;
                }
            }
        }
        log("isOnlySingleDcAllowed(" + rilRadioTech + "): " + onlySingleDcAllowed);
        return onlySingleDcAllowed;
    }

    @Override
    protected void restartRadio() {
        log("restartRadio: ************TURN OFF RADIO**************");
        cleanUpAllConnections(true, Phone.REASON_RADIO_TURNED_OFF);
        this.mPhone.getServiceStateTracker().powerOffRadioSafely(this);
        int reset = Integer.parseInt(SystemProperties.get("net.ppp.reset-by-timeout", "0"));
        SystemProperties.set("net.ppp.reset-by-timeout", String.valueOf(reset + 1));
    }

    private boolean retryAfterDisconnected(ApnContext apnContext) {
        String reason = apnContext.getReason();
        if (!Phone.REASON_RADIO_TURNED_OFF.equals(reason) && (!isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) || !isHigherPriorityApnContextActive(apnContext))) {
            return true;
        }
        return false;
    }

    private void startAlarmForReconnect(int delay, ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        Intent intent = new Intent("com.android.internal.telephony.data-reconnect." + apnType);
        intent.putExtra("reconnect_alarm_extra_reason", apnContext.getReason());
        intent.putExtra("reconnect_alarm_extra_type", apnType);
        int subId = SubscriptionManager.getDefaultDataSubId();
        intent.putExtra("subscription", subId);
        log("startAlarmForReconnect: delay=" + delay + " action=" + intent.getAction() + " apn=" + apnContext);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        apnContext.setReconnectIntent(alarmIntent);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + ((long) delay), alarmIntent);
    }

    private void startAlarmForRestartTrySetup(int delay, ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        Intent intent = new Intent("com.android.internal.telephony.data-restart-trysetup." + apnType);
        intent.putExtra("restart_trysetup_alarm_extra_type", apnType);
        log("startAlarmForRestartTrySetup: delay=" + delay + " action=" + intent.getAction() + " apn=" + apnContext);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        apnContext.setReconnectIntent(alarmIntent);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + ((long) delay), alarmIntent);
    }

    private void notifyNoData(DcFailCause lastFailCauseCode, ApnContext apnContext) {
        log("notifyNoData: type=" + apnContext.getApnType());
        if (isPermanentFail(lastFailCauseCode) && !apnContext.getApnType().equals("default")) {
            this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
        }
    }

    private void onRecordsLoaded() {
        log("onRecordsLoaded: createAllApnList");
        this.mAutoAttachOnCreationConfig = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.lockPatternStyle);
        createAllApnList();
        setInitialAttachApn();
        if (this.mPhone.mCi.getRadioState().isOn()) {
            log("onRecordsLoaded: notifying data availability");
            notifyOffApnsOfAvailability(Phone.REASON_SIM_LOADED);
        }
        setupDataOnConnectableApns(Phone.REASON_SIM_LOADED);
    }

    @Override
    protected void onSetDependencyMet(String apnType, boolean met) {
        ApnContext apnContext;
        if (!"hipri".equals(apnType)) {
            ApnContext apnContext2 = this.mApnContexts.get(apnType);
            if (apnContext2 == null) {
                loge("onSetDependencyMet: ApnContext not found in onSetDependencyMet(" + apnType + ", " + met + ")");
                return;
            }
            applyNewState(apnContext2, apnContext2.isEnabled(), met);
            if (!"default".equals(apnType) || (apnContext = this.mApnContexts.get("hipri")) == null) {
                return;
            }
            applyNewState(apnContext, apnContext.isEnabled(), met);
        }
    }

    private void applyNewState(ApnContext apnContext, boolean enabled, boolean met) {
        boolean cleanup = false;
        boolean trySetup = false;
        log("applyNewState(" + apnContext.getApnType() + ", " + enabled + "(" + apnContext.isEnabled() + "), " + met + "(" + apnContext.getDependencyMet() + "))");
        if (apnContext.isReady()) {
            cleanup = true;
            if (enabled && met) {
                DctConstants.State state = apnContext.getState();
                switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$DctConstants$State[state.ordinal()]) {
                    case 1:
                    case 2:
                    case 4:
                    case 6:
                        log("applyNewState: 'ready' so return");
                        return;
                    case 3:
                        if (apnContext.getApnType().equals("ims")) {
                            log("applyNewState: 'ims is retrying' so return");
                            return;
                        } else {
                            trySetup = true;
                            apnContext.setReason(Phone.REASON_DATA_ENABLED);
                        }
                        break;
                    case 5:
                    case 7:
                        trySetup = true;
                        apnContext.setReason(Phone.REASON_DATA_ENABLED);
                        break;
                }
            } else if (met) {
                apnContext.setReason(Phone.REASON_DATA_DISABLED);
                cleanup = (apnContext.getApnType() == "dun" && teardownForDun()) || apnContext.getApnType() == "mms";
            } else {
                apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_UNMET);
            }
        } else if (enabled && met) {
            if (apnContext.isEnabled()) {
                apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_MET);
            } else {
                apnContext.setReason(Phone.REASON_DATA_ENABLED);
            }
            if (apnContext.getState() == DctConstants.State.FAILED) {
                apnContext.setState(DctConstants.State.IDLE);
            }
            trySetup = true;
        }
        apnContext.setEnabled(enabled);
        apnContext.setDependencyMet(met);
        if (cleanup) {
            cleanUpConnection(true, apnContext);
        }
        if (trySetup) {
            trySetupData(apnContext);
        }
    }

    private DcAsyncChannel checkForCompatibleConnectedApnContext(ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        ApnSetting dunSetting = null;
        if ("dun".equals(apnType)) {
            dunSetting = fetchDunApn();
        }
        log("checkForCompatibleConnectedApnContext: apnContext=" + apnContext);
        DcAsyncChannel potentialDcac = null;
        ApnContext potentialApnCtx = null;
        for (ApnContext curApnCtx : this.mApnContexts.values()) {
            DcAsyncChannel curDcac = curApnCtx.getDcAc();
            log("curDcac: " + curDcac);
            if (curDcac != null) {
                ApnSetting apnSetting = curApnCtx.getApnSetting();
                log("apnSetting: " + apnSetting);
                if (dunSetting != null) {
                    if (dunSetting.equals(apnSetting)) {
                        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$DctConstants$State[curApnCtx.getState().ordinal()]) {
                            case 1:
                                log("checkForCompatibleConnectedApnContext: found dun conn=" + curDcac + " curApnCtx=" + curApnCtx);
                                return curDcac;
                            case 3:
                            case 4:
                            case 6:
                                potentialDcac = curDcac;
                                potentialApnCtx = curApnCtx;
                                break;
                        }
                    } else {
                        continue;
                    }
                } else if (apnSetting != null && apnSetting.canHandleType(apnType)) {
                    switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$DctConstants$State[curApnCtx.getState().ordinal()]) {
                        case 1:
                            log("checkForCompatibleConnectedApnContext: found canHandle conn=" + curDcac + " curApnCtx=" + curApnCtx);
                            return curDcac;
                        case 3:
                        case 4:
                            potentialDcac = curDcac;
                            potentialApnCtx = curApnCtx;
                            break;
                    }
                }
            }
        }
        if (potentialDcac != null) {
            log("checkForCompatibleConnectedApnContext: found potential conn=" + potentialDcac + " curApnCtx=" + potentialApnCtx);
            return potentialDcac;
        }
        log("checkForCompatibleConnectedApnContext: NO conn apnContext=" + apnContext);
        return null;
    }

    @Override
    protected void onEnableApn(int apnId, int enabled) {
        ApnContext apnContext = this.mApnContexts.get(apnIdToType(apnId));
        if (apnContext == null) {
            loge("onEnableApn(" + apnId + ", " + enabled + "): NO ApnContext");
        } else {
            log("onEnableApn: apnContext=" + apnContext + " call applyNewState");
            applyNewState(apnContext, enabled == 1, apnContext.getDependencyMet());
        }
    }

    @Override
    protected boolean onTrySetupData(String reason) {
        log("onTrySetupData: reason=" + reason);
        setupDataOnConnectableApns(reason);
        return true;
    }

    protected boolean onTrySetupData(ApnContext apnContext) {
        log("onTrySetupData: apnContext=" + apnContext);
        return trySetupData(apnContext);
    }

    @Override
    protected void onRoamingOff() {
        log("onRoamingOff");
        if (this.mUserDataEnabled) {
            if (!getDataOnRoamingEnabled()) {
                notifyOffApnsOfAvailability(Phone.REASON_ROAMING_OFF);
                setupDataOnConnectableApns(Phone.REASON_ROAMING_OFF);
            } else {
                notifyDataConnection(Phone.REASON_ROAMING_OFF);
            }
        }
    }

    @Override
    protected void onRoamingOn() {
        log("onRoamingOn");
        if (this.mUserDataEnabled) {
            if (getDataOnRoamingEnabled()) {
                log("onRoamingOn: setup data on roaming");
                setupDataOnConnectableApns(Phone.REASON_ROAMING_ON);
                notifyDataConnection(Phone.REASON_ROAMING_ON);
            } else {
                log("onRoamingOn: Tear down data connection on roaming.");
                cleanUpAllConnections(true, Phone.REASON_ROAMING_ON);
                notifyOffApnsOfAvailability(Phone.REASON_ROAMING_ON);
            }
        }
    }

    @Override
    protected void onRadioAvailable() {
        log("onRadioAvailable");
        if (this.mPhone.getSimulatedRadioControl() != null) {
            notifyDataConnection(null);
            log("onRadioAvailable: We're on the simulator; assuming data is connected");
        }
        IccRecords r = this.mIccRecords.get();
        if (r != null && r.getRecordsLoaded()) {
            notifyOffApnsOfAvailability(null);
        }
        if (getOverallState() != DctConstants.State.IDLE) {
            cleanUpConnection(true, null);
        }
    }

    @Override
    protected void onRadioOffOrNotAvailable() {
        this.mReregisterOnReconnectFailure = false;
        if (this.mPhone.getSimulatedRadioControl() != null) {
            log("We're on the simulator; assuming radio off is meaningless");
        } else {
            log("onRadioOffOrNotAvailable: is off and clean up all connections");
            cleanUpAllConnections(false, Phone.REASON_RADIO_TURNED_OFF);
        }
        notifyOffApnsOfAvailability(null);
    }

    @Override
    protected void completeConnection(ApnContext apnContext) {
        apnContext.isProvisioningApn();
        log("completeConnection: successful, notify the world apnContext=" + apnContext);
        if (this.mIsProvisioning && !TextUtils.isEmpty(this.mProvisioningUrl)) {
            log("completeConnection: MOBILE_PROVISIONING_ACTION url=" + this.mProvisioningUrl);
            Intent newIntent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER");
            newIntent.setData(Uri.parse(this.mProvisioningUrl));
            newIntent.setFlags(272629760);
            try {
                this.mPhone.getContext().startActivity(newIntent);
            } catch (ActivityNotFoundException e) {
                loge("completeConnection: startActivityAsUser failed" + e);
            }
        }
        this.mIsProvisioning = false;
        this.mProvisioningUrl = null;
        if (this.mProvisioningSpinner != null) {
            sendMessage(obtainMessage(270378, this.mProvisioningSpinner));
        }
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        startNetStatPoll();
        startDataStallAlarm(false);
    }

    @Override
    protected void onDataSetupComplete(AsyncResult ar) {
        DcFailCause dcFailCause = DcFailCause.UNKNOWN;
        boolean handleError = false;
        if (ar.userObj instanceof ApnContext) {
            ApnContext apnContext = (ApnContext) ar.userObj;
            if (ar.exception == null) {
                DcAsyncChannel dcac = apnContext.getDcAc();
                if (dcac == null) {
                    log("onDataSetupComplete: no connection to DC, handle as error");
                    DcFailCause cause = DcFailCause.CONNECTION_TO_DATACONNECTIONAC_BROKEN;
                    handleError = true;
                } else {
                    ApnSetting apn = apnContext.getApnSetting();
                    log("onDataSetupComplete: success apn=" + (apn == null ? "unknown" : apn.apn));
                    if (apn != null && apn.proxy != null && apn.proxy.length() != 0) {
                        try {
                            String port = apn.port;
                            if (TextUtils.isEmpty(port)) {
                                port = "8080";
                            }
                            ProxyInfo proxy = new ProxyInfo(apn.proxy, Integer.parseInt(port), null);
                            dcac.setLinkPropertiesHttpProxySync(proxy);
                        } catch (NumberFormatException e) {
                            loge("onDataSetupComplete: NumberFormatException making ProxyProperties (" + apn.port + "): " + e);
                        }
                    }
                    if (TextUtils.equals(apnContext.getApnType(), "default")) {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "true");
                        if (this.mCanSetPreferApn && this.mPreferredApn == null) {
                            log("onDataSetupComplete: PREFERED APN is null");
                            this.mPreferredApn = apn;
                            if (this.mPreferredApn != null) {
                                setPreferredApn(this.mPreferredApn.id);
                            }
                        }
                    } else {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                    }
                    apnContext.setState(DctConstants.State.CONNECTED);
                    boolean isProvApn = apnContext.isProvisioningApn();
                    ConnectivityManager cm = ConnectivityManager.from(this.mPhone.getContext());
                    if (this.mProvisionBroadcastReceiver != null) {
                        this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
                        this.mProvisionBroadcastReceiver = null;
                    }
                    if (!isProvApn || this.mIsProvisioning) {
                        cm.setProvisioningNotificationVisible(false, 0, this.mProvisionActionName);
                        completeConnection(apnContext);
                    } else {
                        log("onDataSetupComplete: successful, BUT send connected to prov apn as mIsProvisioning:" + this.mIsProvisioning + " == false && (isProvisioningApn:" + isProvApn + " == true");
                        this.mProvisionBroadcastReceiver = new ProvisionNotificationBroadcastReceiver(cm.getMobileProvisioningUrl(), TelephonyManager.getDefault().getNetworkOperatorName());
                        this.mPhone.getContext().registerReceiver(this.mProvisionBroadcastReceiver, new IntentFilter(this.mProvisionActionName));
                        cm.setProvisioningNotificationVisible(true, 0, this.mProvisionActionName);
                        setRadio(false);
                        Intent intent = new Intent("android.intent.action.DATA_CONNECTION_CONNECTED_TO_PROVISIONING_APN");
                        intent.putExtra(Telephony.Carriers.APN, apnContext.getApnSetting().apn);
                        intent.putExtra("apnType", apnContext.getApnType());
                        String apnType = apnContext.getApnType();
                        LinkProperties linkProperties = getLinkProperties(apnType);
                        if (linkProperties != null) {
                            intent.putExtra("linkProperties", linkProperties);
                            String iface = linkProperties.getInterfaceName();
                            if (iface != null) {
                                intent.putExtra("iface", iface);
                            }
                        }
                        NetworkCapabilities networkCapabilities = getNetworkCapabilities(apnType);
                        if (networkCapabilities != null) {
                            intent.putExtra("networkCapabilities", networkCapabilities);
                        }
                        this.mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
                    }
                    log("onDataSetupComplete: SETUP complete type=" + apnContext.getApnType() + ", reason:" + apnContext.getReason());
                }
            } else {
                DcFailCause cause2 = (DcFailCause) ar.result;
                ApnSetting apn2 = apnContext.getApnSetting();
                Object[] objArr = new Object[2];
                objArr[0] = apn2 == null ? "unknown" : apn2.apn;
                objArr[1] = cause2;
                log(String.format("onDataSetupComplete: error apn=%s cause=%s", objArr));
                if (cause2.isEventLoggable()) {
                    int cid = getCellLocationId();
                    EventLog.writeEvent(EventLogTags.PDP_SETUP_FAIL, Integer.valueOf(cause2.ordinal()), Integer.valueOf(cid), Integer.valueOf(TelephonyManager.getDefault().getNetworkType()));
                }
                ApnSetting apn3 = apnContext.getApnSetting();
                this.mPhone.notifyPreciseDataConnectionFailed(apnContext.getReason(), apnContext.getApnType(), apn3 != null ? apn3.apn : "unknown", cause2.toString());
                if (isPermanentFail(cause2)) {
                    apnContext.decWaitingApnsPermFailCount();
                }
                apnContext.removeWaitingApn(apnContext.getApnSetting());
                log(String.format("onDataSetupComplete: WaitingApns.size=%d WaitingApnsPermFailureCountDown=%d", Integer.valueOf(apnContext.getWaitingApns().size()), Integer.valueOf(apnContext.getWaitingApnsPermFailCount())));
                handleError = true;
            }
            if (handleError) {
                onDataSetupCompleteError(ar);
            }
            if (!this.mInternalDataEnabled) {
                cleanUpAllConnections(null);
                return;
            }
            return;
        }
        throw new RuntimeException("onDataSetupComplete: No apnContext");
    }

    private int getApnDelay() {
        return this.mFailFast ? SystemProperties.getInt("persist.radio.apn_ff_delay", 3000) : SystemProperties.getInt("persist.radio.apn_delay", 20000);
    }

    @Override
    protected void onDataSetupCompleteError(AsyncResult ar) {
        if (ar.userObj instanceof ApnContext) {
            ApnContext apnContext = (ApnContext) ar.userObj;
            if (apnContext.getWaitingApns().isEmpty()) {
                apnContext.setState(DctConstants.State.FAILED);
                this.mPhone.notifyDataConnection(Phone.REASON_APN_FAILED, apnContext.getApnType());
                if (apnContext.getWaitingApnsPermFailCount() == 0) {
                    log("onDataSetupCompleteError: All APN's had permanent failures, stop retrying");
                    return;
                }
                int delay = getApnDelay();
                log("onDataSetupCompleteError: Not all APN's had permanent failures delay=" + delay);
                startAlarmForRestartTrySetup(delay, apnContext);
                return;
            }
            log("onDataSetupCompleteError: Try next APN");
            apnContext.setState(DctConstants.State.SCANNING);
            startAlarmForReconnect(getApnDelay(), apnContext);
            return;
        }
        throw new RuntimeException("onDataSetupCompleteError: No apnContext");
    }

    @Override
    protected void onDisconnectDone(int connId, AsyncResult ar) {
        if (ar.userObj instanceof ApnContext) {
            ApnContext apnContext = (ApnContext) ar.userObj;
            log("onDisconnectDone: EVENT_DISCONNECT_DONE apnContext=" + apnContext);
            apnContext.setState(DctConstants.State.IDLE);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            if (isDisconnected() && this.mPhone.getServiceStateTracker().processPendingRadioPowerOffAfterDataOff()) {
                log("onDisconnectDone: radio will be turned off, no retries");
                apnContext.setApnSetting(null);
                apnContext.setDataConnectionAc(null);
                if (this.mDisconnectPendingCount > 0) {
                    this.mDisconnectPendingCount--;
                }
                if (this.mDisconnectPendingCount == 0) {
                    notifyDataDisconnectComplete();
                    notifyAllDataDisconnected();
                    return;
                }
                return;
            }
            if (this.mAttached.get() && apnContext.isReady() && retryAfterDisconnected(apnContext)) {
                SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                log("onDisconnectDone: attached, ready and retry after disconnect");
                startAlarmForReconnect(getApnDelay(), apnContext);
            } else {
                boolean restartRadioAfterProvisioning = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.internalMinWidth);
                if (apnContext.isProvisioningApn() && restartRadioAfterProvisioning) {
                    log("onDisconnectDone: restartRadio after provisioning");
                    restartRadio();
                }
                apnContext.setApnSetting(null);
                apnContext.setDataConnectionAc(null);
                if (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology())) {
                    log("onDisconnectDone: isOnlySigneDcAllowed true so setup single apn");
                    setupDataOnConnectableApns(Phone.REASON_SINGLE_PDN_ARBITRATION);
                } else {
                    log("onDisconnectDone: not retrying");
                }
            }
            if (this.mDisconnectPendingCount > 0) {
                this.mDisconnectPendingCount--;
            }
            if (this.mDisconnectPendingCount == 0) {
                apnContext.setConcurrentVoiceAndDataAllowed(this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
                notifyDataDisconnectComplete();
                notifyAllDataDisconnected();
                return;
            }
            return;
        }
        loge("onDisconnectDone: Invalid ar in onDisconnectDone, ignore");
    }

    @Override
    protected void onDisconnectDcRetrying(int connId, AsyncResult ar) {
        if (ar.userObj instanceof ApnContext) {
            ApnContext apnContext = (ApnContext) ar.userObj;
            apnContext.setState(DctConstants.State.RETRYING);
            log("onDisconnectDcRetrying: apnContext=" + apnContext);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            return;
        }
        loge("onDisconnectDcRetrying: Invalid ar in onDisconnectDone, ignore");
    }

    private void handleVoiceCallForOthers(boolean start) {
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (tm.getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDS) {
            int count = tm.getPhoneCount();
            for (int i = 0; i < count; i++) {
                if (i != this.mPhone.getPhoneId()) {
                    Phone phone = PhoneFactory.getPhone(i);
                    if (phone == null) {
                        loge("Fail to get phone " + i);
                    } else if (start) {
                        ((PhoneProxy) phone).suspendDataCallByOtherPhone();
                    } else {
                        ((PhoneProxy) phone).resumeDataCallByOtherPhone();
                    }
                }
            }
        }
    }

    @Override
    protected void onVoiceCallStarted() {
        log("onVoiceCallStarted");
        handleVoiceCallForOthers(true);
        this.mInVoiceCall = true;
        if (isConnected() && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            log("onVoiceCallStarted stop polling");
            this.mIsDataSuspendedByVoice = true;
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection(Phone.REASON_VOICE_CALL_STARTED);
        }
    }

    @Override
    protected void onVoiceCallEnded() {
        log("onVoiceCallEnded");
        this.mInVoiceCall = false;
        if (isConnected()) {
            if (this.mIsDataSuspendedByVoice || !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                this.mIsDataSuspendedByVoice = false;
                startNetStatPoll();
                startDataStallAlarm(false);
                notifyDataConnection(Phone.REASON_VOICE_CALL_ENDED);
            } else {
                resetPollStats();
            }
        }
        setupDataOnConnectableApns(Phone.REASON_VOICE_CALL_ENDED);
        handleVoiceCallForOthers(false);
    }

    @Override
    public void suspendDataCallByOtherPhone() {
        log("suspendDataCallByOtherPhone");
        if (isConnected()) {
            log("suspendDataCallByOtherPhone stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection(Phone.REASON_SUSPENED_BY_OTHER_PHONE);
        }
    }

    @Override
    public void resumeDataCallByOtherPhone() {
        log("resumeDataCallByOtherPhone");
        if (isConnected()) {
            startNetStatPoll();
            startDataStallAlarm(false);
            notifyDataConnection(Phone.REASON_RESUMED_BY_OTHER_PHONE);
        }
        setupDataOnConnectableApns(Phone.REASON_RESUMED_BY_OTHER_PHONE);
    }

    @Override
    protected void onCleanUpConnection(boolean tearDown, int apnId, String reason) {
        log("onCleanUpConnection");
        ApnContext apnContext = this.mApnContexts.get(apnIdToType(apnId));
        if (apnContext != null) {
            apnContext.setReason(reason);
            cleanUpConnection(tearDown, apnContext);
        }
    }

    @Override
    protected boolean isConnected() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getState() == DctConstants.State.CONNECTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDisconnected() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!apnContext.isDisconnected()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void notifyDataConnection(String reason) {
        log("notifyDataConnection: reason=" + reason);
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady()) {
                log("notifyDataConnection: type:" + apnContext.getApnType());
                this.mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(), apnContext.getApnType());
            }
        }
        notifyOffApnsOfAvailability(reason);
    }

    private void createAllApnList() {
        this.mAllApnSettings = new ArrayList<>();
        IccRecords r = this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        if (operator != null) {
            String selection = "numeric = '" + operator + "'";
            log("createAllApnList: selection=" + selection);
            Cursor cursor = this.mPhone.getContext().getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, selection, null, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    this.mAllApnSettings = createApnList(cursor);
                }
                cursor.close();
            }
        }
        addEmergencyApnSetting();
        dedupeApnSettings();
        if (this.mAllApnSettings.isEmpty()) {
            log("createAllApnList: No APN found for carrier: " + operator);
            this.mPreferredApn = null;
        } else {
            this.mPreferredApn = getPreferredApn();
            if (this.mPreferredApn != null && !this.mPreferredApn.numeric.equals(operator)) {
                this.mPreferredApn = null;
                setPreferredApn(-1);
            }
            log("createAllApnList: mPreferredApn=" + this.mPreferredApn);
        }
        log("createAllApnList: X mAllApnSettings=" + this.mAllApnSettings);
        setDataProfilesAsNeeded();
    }

    private void dedupeApnSettings() {
        new ArrayList();
        for (int i = 0; i < this.mAllApnSettings.size() - 1; i++) {
            ApnSetting first = this.mAllApnSettings.get(i);
            int j = i + 1;
            while (j < this.mAllApnSettings.size()) {
                ApnSetting second = this.mAllApnSettings.get(j);
                if (apnsSimilar(first, second)) {
                    ApnSetting newApn = mergeApns(first, second);
                    this.mAllApnSettings.set(i, newApn);
                    first = newApn;
                    this.mAllApnSettings.remove(j);
                } else {
                    j++;
                }
            }
        }
    }

    private boolean apnTypeSameAny(ApnSetting first, ApnSetting second) {
        for (int index1 = 0; index1 < first.types.length; index1++) {
            for (int index2 = 0; index2 < second.types.length; index2++) {
                if (first.types[index1].equals(CharacterSets.MIMENAME_ANY_CHARSET) || second.types[index2].equals(CharacterSets.MIMENAME_ANY_CHARSET) || first.types[index1].equals(second.types[index2])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean apnsSimilar(ApnSetting first, ApnSetting second) {
        return !first.canHandleType("dun") && !second.canHandleType("dun") && Objects.equals(first.apn, second.apn) && !apnTypeSameAny(first, second) && xorEquals(first.proxy, second.proxy) && xorEquals(first.port, second.port) && first.carrierEnabled == second.carrierEnabled && first.bearer == second.bearer && first.profileId == second.profileId && Objects.equals(first.mvnoType, second.mvnoType) && Objects.equals(first.mvnoMatchData, second.mvnoMatchData) && xorEquals(first.mmsc, second.mmsc) && xorEquals(first.mmsProxy, second.mmsProxy) && xorEquals(first.mmsPort, second.mmsPort);
    }

    private boolean xorEquals(String first, String second) {
        return Objects.equals(first, second) || TextUtils.isEmpty(first) || TextUtils.isEmpty(second);
    }

    private ApnSetting mergeApns(ApnSetting dest, ApnSetting src) {
        ArrayList<String> resultTypes = new ArrayList<>();
        resultTypes.addAll(Arrays.asList(dest.types));
        String[] arr$ = src.types;
        for (String srcType : arr$) {
            if (!resultTypes.contains(srcType)) {
                resultTypes.add(srcType);
            }
        }
        String mmsc = TextUtils.isEmpty(dest.mmsc) ? src.mmsc : dest.mmsc;
        String mmsProxy = TextUtils.isEmpty(dest.mmsProxy) ? src.mmsProxy : dest.mmsProxy;
        String mmsPort = TextUtils.isEmpty(dest.mmsPort) ? src.mmsPort : dest.mmsPort;
        String proxy = TextUtils.isEmpty(dest.proxy) ? src.proxy : dest.proxy;
        String port = TextUtils.isEmpty(dest.port) ? src.port : dest.port;
        String protocol = src.protocol.equals("IPV4V6") ? src.protocol : dest.protocol;
        String roamingProtocol = src.roamingProtocol.equals("IPV4V6") ? src.roamingProtocol : dest.roamingProtocol;
        return new ApnSetting(dest.id, dest.numeric, dest.carrier, dest.apn, proxy, port, mmsc, mmsProxy, mmsPort, dest.user, dest.password, dest.authType, (String[]) resultTypes.toArray(new String[0]), protocol, roamingProtocol, dest.carrierEnabled, dest.bearer, dest.profileId, dest.modemCognitive || src.modemCognitive, dest.maxConns, dest.waitTime, dest.maxConnsTime, dest.mtu, dest.mvnoType, dest.mvnoMatchData);
    }

    private DcAsyncChannel createDataConnection() {
        log("createDataConnection E");
        int id = this.mUniqueIdGenerator.getAndIncrement();
        DataConnection conn = DataConnection.makeDataConnection(this.mPhone, id, this, this.mDcTesterFailBringUpAll, this.mDcc);
        this.mDataConnections.put(Integer.valueOf(id), conn);
        DcAsyncChannel dcac = new DcAsyncChannel(conn, "DCT");
        int status = dcac.fullyConnectSync(this.mPhone.getContext(), this, conn.getHandler());
        if (status == 0) {
            this.mDataConnectionAcHashMap.put(Integer.valueOf(dcac.getDataConnectionIdSync()), dcac);
        } else {
            loge("createDataConnection: Could not connect to dcac=" + dcac + " status=" + status);
        }
        log("createDataConnection() X id=" + id + " dc=" + conn);
        return dcac;
    }

    private void destroyDataConnections() {
        if (this.mDataConnections != null) {
            log("destroyDataConnections: clear mDataConnectionList");
            this.mDataConnections.clear();
        } else {
            log("destroyDataConnections: mDataConnecitonList is empty, ignore");
        }
    }

    public ArrayList<ApnSetting> buildWaitingApns(String requestedApnType, int radioTech) {
        boolean usePreferred;
        ApnSetting dun;
        log("buildWaitingApns: E requestedApnType=" + requestedApnType);
        ArrayList<ApnSetting> apnList = new ArrayList<>();
        if (requestedApnType.equals("dun") && (dun = fetchDunApn()) != null) {
            apnList.add(dun);
            log("buildWaitingApns: X added APN_TYPE_DUN apnList=" + apnList);
        } else {
            IccRecords r = this.mIccRecords.get();
            String operator = r != null ? r.getOperatorNumeric() : "";
            try {
                usePreferred = !this.mPhone.getContext().getResources().getBoolean(R.^attr-private.internalMinHeight);
            } catch (Resources.NotFoundException e) {
                log("buildWaitingApns: usePreferred NotFoundException set to true");
                usePreferred = true;
            }
            log("buildWaitingApns: usePreferred=" + usePreferred + " canSetPreferApn=" + this.mCanSetPreferApn + " mPreferredApn=" + this.mPreferredApn + " operator=" + operator + " radioTech=" + radioTech + " IccRecords r=" + r);
            if (usePreferred && this.mCanSetPreferApn && this.mPreferredApn != null && this.mPreferredApn.canHandleType(requestedApnType)) {
                log("buildWaitingApns: Preferred APN:" + operator + ":" + this.mPreferredApn.numeric + ":" + this.mPreferredApn);
                if (this.mPreferredApn.numeric.equals(operator)) {
                    if (this.mPreferredApn.bearer == 0 || this.mPreferredApn.bearer == radioTech) {
                        apnList.add(this.mPreferredApn);
                        log("buildWaitingApns: X added preferred apnList=" + apnList);
                    } else {
                        log("buildWaitingApns: no preferred APN");
                        setPreferredApn(-1);
                        this.mPreferredApn = null;
                    }
                } else {
                    log("buildWaitingApns: no preferred APN");
                    setPreferredApn(-1);
                    this.mPreferredApn = null;
                }
                if (this.mAllApnSettings == null) {
                }
                log("buildWaitingApns: X apnList=" + apnList);
            } else {
                if (this.mAllApnSettings == null) {
                    log("buildWaitingApns: mAllApnSettings=" + this.mAllApnSettings);
                    for (ApnSetting apn : this.mAllApnSettings) {
                        log("buildWaitingApns: apn=" + apn);
                        if (apn.canHandleType(requestedApnType)) {
                            if (apn.bearer == 0 || apn.bearer == radioTech) {
                                log("buildWaitingApns: adding apn=" + apn.toString());
                                apnList.add(apn);
                            } else {
                                log("buildWaitingApns: bearer:" + apn.bearer + " != radioTech:" + radioTech);
                            }
                        } else {
                            log("buildWaitingApns: couldn't handle requesedApnType=" + requestedApnType);
                        }
                    }
                } else {
                    loge("mAllApnSettings is empty!");
                }
                log("buildWaitingApns: X apnList=" + apnList);
            }
        }
        return apnList;
    }

    private String apnListToString(ArrayList<ApnSetting> apns) {
        StringBuilder result = new StringBuilder();
        int size = apns.size();
        for (int i = 0; i < size; i++) {
            result.append('[').append(apns.get(i).toString()).append(']');
        }
        return result.toString();
    }

    private void setPreferredApn(int pos) {
        if (!this.mCanSetPreferApn) {
            log("setPreferredApn: X !canSEtPreferApn");
            return;
        }
        String subId = Long.toString(this.mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        log("setPreferredApn: delete");
        ContentResolver resolver = this.mPhone.getContext().getContentResolver();
        resolver.delete(uri, null, null);
        if (pos >= 0) {
            log("setPreferredApn: insert");
            ContentValues values = new ContentValues();
            values.put(APN_ID, Integer.valueOf(pos));
            resolver.insert(uri, values);
        }
    }

    private ApnSetting getPreferredApn() {
        if (this.mAllApnSettings.isEmpty()) {
            log("getPreferredApn: X not found mAllApnSettings.isEmpty");
            return null;
        }
        String subId = Long.toString(this.mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        Cursor cursor = this.mPhone.getContext().getContentResolver().query(uri, new String[]{"_id", "name", Telephony.Carriers.APN}, null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            this.mCanSetPreferApn = true;
        } else {
            this.mCanSetPreferApn = false;
        }
        log("getPreferredApn: mRequestedApnType=" + this.mRequestedApnType + " cursor=" + cursor + " cursor.count=" + (cursor != null ? cursor.getCount() : 0));
        if (this.mCanSetPreferApn && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int pos = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            for (ApnSetting p : this.mAllApnSettings) {
                log("getPreferredApn: apnSetting=" + p);
                if (p.id == pos && p.canHandleType(this.mRequestedApnType)) {
                    log("getPreferredApn: X found apnSetting" + p);
                    cursor.close();
                    return p;
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        log("getPreferredApn: X not found");
        return null;
    }

    private boolean simChanged() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getImsiChanged();
        }
        return false;
    }

    private void onUpdateRecords() {
        IccRecords r = this.mIccRecords.get();
        log("register SIM records loaded");
        if (r != null) {
            r.registerForRecordsLoaded(this, 270338, null);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        log("handleMessage msg=" + msg);
        if (!this.mPhone.mIsTheCurrentActivePhone || this.mIsDisposed) {
            loge("handleMessage: Ignore GSM msgs since GSM phone is inactive");
            return;
        }
        switch (msg.what) {
            case 100:
                if (simChanged()) {
                    log("sim changed");
                    onUpdateRecords();
                    return;
                } else {
                    onRecordsLoaded();
                    return;
                }
            case 270338:
                onRecordsLoaded();
                return;
            case 270339:
                if (msg.obj instanceof ApnContext) {
                    onTrySetupData((ApnContext) msg.obj);
                    return;
                } else if (msg.obj instanceof String) {
                    onTrySetupData((String) msg.obj);
                    return;
                } else {
                    loge("EVENT_TRY_SETUP request w/o apnContext or String");
                    return;
                }
            case 270345:
                onDataConnectionDetached();
                return;
            case 270352:
                onDataConnectionAttached();
                return;
            case 270354:
                doRecovery();
                return;
            case 270355:
                onApnChanged();
                return;
            case 270358:
                log("EVENT_PS_RESTRICT_ENABLED " + this.mIsPsRestricted);
                stopNetStatPoll();
                stopDataStallAlarm();
                this.mIsPsRestricted = true;
                return;
            case 270359:
                log("EVENT_PS_RESTRICT_DISABLED " + this.mIsPsRestricted);
                this.mIsPsRestricted = false;
                if (isConnected()) {
                    startNetStatPoll();
                    startDataStallAlarm(false);
                    return;
                }
                if (this.mState == DctConstants.State.FAILED) {
                    cleanUpAllConnections(false, Phone.REASON_PS_RESTRICT_ENABLED);
                    this.mReregisterOnReconnectFailure = false;
                }
                ApnContext apnContext = this.mApnContexts.get("default");
                if (apnContext != null) {
                    apnContext.setReason(Phone.REASON_PS_RESTRICT_ENABLED);
                    trySetupData(apnContext);
                    return;
                } else {
                    loge("**** Default ApnContext not found ****");
                    if (Build.IS_DEBUGGABLE) {
                        throw new RuntimeException("Default ApnContext not found");
                    }
                    return;
                }
            case 270360:
                boolean tearDown = msg.arg1 != 0;
                log("EVENT_CLEAN_UP_CONNECTION tearDown=" + tearDown);
                if (msg.obj instanceof ApnContext) {
                    cleanUpConnection(tearDown, (ApnContext) msg.obj);
                    return;
                } else {
                    loge("EVENT_CLEAN_UP_CONNECTION request w/o apn context, call super");
                    super.handleMessage(msg);
                    return;
                }
            case 270363:
                boolean enabled = msg.arg1 == 1;
                onSetInternalDataEnabled(enabled, (Message) msg.obj);
                return;
            case 270365:
                Message mCause = obtainMessage(270365, null);
                if (msg.obj != null && (msg.obj instanceof String)) {
                    mCause.obj = msg.obj;
                }
                super.handleMessage(mCause);
                return;
            case 270377:
                setupDataOnConnectableApns(Phone.REASON_NW_TYPE_CHANGED, RetryFailures.ONLY_ON_CHANGE);
                return;
            case 270378:
                if (this.mProvisioningSpinner == msg.obj) {
                    this.mProvisioningSpinner.dismiss();
                    this.mProvisioningSpinner = null;
                    return;
                }
                return;
            default:
                super.handleMessage(msg);
                return;
        }
    }

    protected int getApnProfileID(String apnType) {
        if (TextUtils.equals(apnType, "ims")) {
            return 2;
        }
        if (TextUtils.equals(apnType, "fota")) {
            return 3;
        }
        if (TextUtils.equals(apnType, "cbs")) {
            return 4;
        }
        if (TextUtils.equals(apnType, "ia")) {
            return 0;
        }
        if (TextUtils.equals(apnType, "dun")) {
            return 1;
        }
        return TextUtils.equals(apnType, "mms") ? 5 : 0;
    }

    private int getCellLocationId() {
        CellLocation loc = this.mPhone.getCellLocation();
        if (loc == null) {
            return -1;
        }
        if (loc instanceof GsmCellLocation) {
            int cid = ((GsmCellLocation) loc).getCid();
            return cid;
        }
        if (!(loc instanceof CdmaCellLocation)) {
            return -1;
        }
        int cid2 = ((CdmaCellLocation) loc).getBaseStationId();
        return cid2;
    }

    private IccRecords getUiccRecords(int appFamily) {
        return this.mUiccController.getIccRecords(this.mPhone.getPhoneId(), appFamily);
    }

    @Override
    protected void onUpdateIcc() {
        IccRecords newIccRecords;
        IccRecords r;
        if (this.mUiccController != null && (r = this.mIccRecords.get()) != (newIccRecords = getUiccRecords(1))) {
            if (r != null) {
                log("Removing stale icc objects.");
                r.unregisterForImsiReady(this);
                this.mIccRecords.set(null);
            }
            if (newIccRecords != null) {
                log("New records found, register IMSI ready");
                this.mIccRecords.set(newIccRecords);
                newIccRecords.registerForImsiReady(this, 100, null);
            }
        }
    }

    public void update() {
        log("update sub = " + this.mPhone.getSubId());
        log("update(): Active DDS, register for all events now!");
        registerForAllEvents();
        onUpdateIcc();
        this.mUserDataEnabled = getDataEnabled();
        if (this.mPhone instanceof CDMALTEPhone) {
            ((CDMALTEPhone) this.mPhone).updateCurrentCarrierInProvider();
            supplyMessenger();
        } else if (this.mPhone instanceof GSMPhone) {
            ((GSMPhone) this.mPhone).updateCurrentCarrierInProvider();
            supplyMessenger();
        } else {
            log("Phone object is not MultiSim. This should not hit!!!!");
        }
    }

    @Override
    public void cleanUpAllConnections(String cause) {
        cleanUpAllConnections(cause, (Message) null);
    }

    public void updateRecords() {
        onUpdateIcc();
    }

    public void cleanUpAllConnections(String cause, Message disconnectAllCompleteMsg) {
        log("cleanUpAllConnections");
        if (disconnectAllCompleteMsg != null) {
            this.mDisconnectAllCompleteMsgList.add(disconnectAllCompleteMsg);
        }
        Message msg = obtainMessage(270365);
        msg.obj = cause;
        sendMessage(msg);
    }

    protected void notifyDataDisconnectComplete() {
        log("notifyDataDisconnectComplete");
        for (Message m : this.mDisconnectAllCompleteMsgList) {
            m.sendToTarget();
        }
        this.mDisconnectAllCompleteMsgList.clear();
    }

    protected void notifyAllDataDisconnected() {
        sEnableFailFastRefCounter = 0;
        this.mFailFast = false;
        this.mAllDataDisconnectedRegistrants.notifyRegistrants();
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        this.mAllDataDisconnectedRegistrants.addUnique(h, what, obj);
        if (isDisconnected()) {
            log("notify All Data Disconnected");
            notifyAllDataDisconnected();
        }
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        this.mAllDataDisconnectedRegistrants.remove(h);
    }

    @Override
    protected void onSetInternalDataEnabled(boolean enable) {
        log("onSetInternalDataEnabled: enabled=" + enable);
        onSetInternalDataEnabled(enable, null);
    }

    protected void onSetInternalDataEnabled(boolean enabled, Message onCompleteMsg) {
        log("onSetInternalDataEnabled: enabled=" + enabled);
        boolean sendOnComplete = true;
        synchronized (this.mDataEnabledLock) {
            this.mInternalDataEnabled = enabled;
            if (enabled) {
                log("onSetInternalDataEnabled: changed to enabled, try to setup data call");
                onTrySetupData(Phone.REASON_DATA_ENABLED);
            } else {
                sendOnComplete = false;
                log("onSetInternalDataEnabled: changed to disabled, cleanUpAllConnections");
                cleanUpAllConnections((String) null, onCompleteMsg);
            }
        }
        if (sendOnComplete && onCompleteMsg != null) {
            onCompleteMsg.sendToTarget();
        }
    }

    public boolean setInternalDataEnabledFlag(boolean enable) {
        log("setInternalDataEnabledFlag(" + enable + ")");
        if (this.mInternalDataEnabled != enable) {
            this.mInternalDataEnabled = enable;
            return true;
        }
        return true;
    }

    @Override
    public boolean setInternalDataEnabled(boolean enable) {
        return setInternalDataEnabled(enable, null);
    }

    public boolean setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        log("setInternalDataEnabled(" + enable + ")");
        Message msg = obtainMessage(270363, onCompleteMsg);
        msg.arg1 = enable ? 1 : 0;
        sendMessage(msg);
        return true;
    }

    @Override
    public void setDataAllowed(boolean enable, Message response) {
        log("setDataAllowed: enable=" + enable);
        mIsCleanupRequired = !enable;
        this.mPhone.mCi.setDataAllowed(enable, response);
        setInternalDataEnabled(enable);
    }

    @Override
    protected void log(String s) {
        Rlog.d("DCT", "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e("DCT", "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DcTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" mReregisterOnReconnectFailure=" + this.mReregisterOnReconnectFailure);
        pw.println(" canSetPreferApn=" + this.mCanSetPreferApn);
        pw.println(" mApnObserver=" + this.mApnObserver);
        pw.println(" getOverallState=" + getOverallState());
        pw.println(" mDataConnectionAsyncChannels=%s\n" + this.mDataConnectionAcHashMap);
        pw.println(" mAttached=" + this.mAttached.get());
    }

    @Override
    public String[] getPcscfAddress(String apnType) {
        ApnContext apnContext;
        log("getPcscfAddress()");
        if (apnType == null) {
            log("apnType is null, return null");
            return null;
        }
        if (TextUtils.equals(apnType, "emergency")) {
            ApnContext apnContext2 = this.mApnContexts.get("emergency");
            apnContext = apnContext2;
        } else if (TextUtils.equals(apnType, "ims")) {
            ApnContext apnContext3 = this.mApnContexts.get("ims");
            apnContext = apnContext3;
        } else {
            log("apnType is invalid, return null");
            return null;
        }
        if (apnContext == null) {
            log("apnContext is null, return null");
            return null;
        }
        DcAsyncChannel dcac = apnContext.getDcAc();
        if (dcac == null) {
            return null;
        }
        String[] result = dcac.getPcscfAddr();
        for (int i = 0; i < result.length; i++) {
            log("Pcscf[" + i + "]: " + result[i]);
        }
        return result;
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        ServiceStateTracker sst;
        log("setImsRegistrationState - mImsRegistrationState(before): " + this.mImsRegistrationState + ", registered(current) : " + registered);
        if (this.mPhone != null && (sst = this.mPhone.getServiceStateTracker()) != null) {
            sst.setImsRegistrationState(registered);
        }
    }

    private void initEmergencyApnSetting() {
        Cursor cursor = this.mPhone.getContext().getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, "type=\"emergency\"", null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                this.mEmergencyApn = makeApnSetting(cursor);
            }
            cursor.close();
        }
    }

    private void addEmergencyApnSetting() {
        if (this.mEmergencyApn != null) {
            if (this.mAllApnSettings == null) {
                this.mAllApnSettings = new ArrayList<>();
                return;
            }
            boolean hasEmergencyApn = false;
            Iterator<ApnSetting> it = this.mAllApnSettings.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ApnSetting apn = it.next();
                if (ArrayUtils.contains(apn.types, "emergency")) {
                    hasEmergencyApn = true;
                    break;
                }
            }
            if (!hasEmergencyApn) {
                this.mAllApnSettings.add(this.mEmergencyApn);
            } else {
                log("addEmergencyApnSetting - E-APN setting is already present");
            }
        }
    }

    private void cleanUpConnectionsOnUpdatedApns(boolean tearDown) {
        log("cleanUpConnectionsOnUpdatedApns: tearDown=" + tearDown);
        if (this.mAllApnSettings.isEmpty()) {
            cleanUpAllConnections(tearDown, Phone.REASON_APN_CHANGED);
        } else {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                boolean cleanUpApn = true;
                ArrayList<ApnSetting> currentWaitingApns = apnContext.getWaitingApns();
                if (currentWaitingApns != null && !apnContext.isDisconnected()) {
                    int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
                    ArrayList<ApnSetting> waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
                    if (waitingApns.size() == currentWaitingApns.size()) {
                        cleanUpApn = false;
                        int i = 0;
                        while (true) {
                            if (i >= waitingApns.size()) {
                                break;
                            }
                            if (currentWaitingApns.get(i).equals(waitingApns.get(i))) {
                                i++;
                            } else {
                                cleanUpApn = true;
                                apnContext.setWaitingApns(waitingApns);
                                break;
                            }
                        }
                    }
                }
                if (cleanUpApn) {
                    apnContext.setReason(Phone.REASON_APN_CHANGED);
                    cleanUpConnection(true, apnContext);
                }
            }
        }
        if (!isConnected()) {
            stopNetStatPoll();
            stopDataStallAlarm();
        }
        this.mRequestedApnType = "default";
        log("mDisconnectPendingCount = " + this.mDisconnectPendingCount);
        if (tearDown && this.mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
    }
}
