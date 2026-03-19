package com.android.internal.telephony.imsphone;

import android.R;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.LinkProperties;
import android.net.NetworkStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.telecom.Connection;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UssdResponse;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsInfo;
import android.text.TextUtils;
import com.android.ims.ImsEcbmStateListener;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.util.NotificationChannelController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ImsPhone extends ImsPhoneBase {
    public static final int CANCEL_ECM_TIMER = 1;
    protected static final boolean DBG = true;
    protected static final int DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    protected static final int EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED = 52;
    protected static final int EVENT_GET_CALL_BARRING_DONE = 47;
    protected static final int EVENT_GET_CALL_WAITING_DONE = 49;
    protected static final int EVENT_GET_CLIR_DONE = 51;
    protected static final int EVENT_SERVICE_STATE_CHANGED = 53;
    protected static final int EVENT_SET_CALL_BARRING_DONE = 46;
    protected static final int EVENT_SET_CALL_WAITING_DONE = 48;
    protected static final int EVENT_SET_CLIR_DONE = 50;
    protected static final int EVENT_VOICE_CALL_ENDED = 54;
    private static final String LOG_TAG = "ImsPhone";
    public static final int RESTART_ECM_TIMER = 0;
    private static final boolean VDBG = false;
    public ImsPhoneCallTracker mCT;
    private Uri[] mCurrentSubscriberUris;
    public Phone mDefaultPhone;
    protected Registrant mEcmExitRespRegistrant;
    protected Runnable mExitEcmRunnable;
    protected ImsExternalCallTracker mExternalCallTracker;
    private ImsEcbmStateListener mImsEcbmStateListener;
    protected boolean mImsRegistered;
    private String mLastDialString;
    protected ArrayList<ImsPhoneMmiCode> mPendingMMIs;
    protected BroadcastReceiver mResultReceiver;
    protected boolean mRoaming;
    protected ServiceState mSS;
    private final RegistrantList mSilentRedialRegistrants;
    private RegistrantList mSsnRegistrants;
    protected PowerManager.WakeLock mWakeLock;

    @Override
    public void activateCellBroadcastSms(int i, Message message) {
        super.activateCellBroadcastSms(i, message);
    }

    @Override
    public boolean disableDataConnectivity() {
        return super.disableDataConnectivity();
    }

    @Override
    public void disableLocationUpdates() {
        super.disableLocationUpdates();
    }

    @Override
    public boolean enableDataConnectivity() {
        return super.enableDataConnectivity();
    }

    @Override
    public void enableLocationUpdates() {
        super.enableLocationUpdates();
    }

    @Override
    public List getAllCellInfo(WorkSource workSource) {
        return super.getAllCellInfo(workSource);
    }

    @Override
    public void getAvailableNetworks(Message message) {
        super.getAvailableNetworks(message);
    }

    @Override
    public boolean getCallForwardingIndicator() {
        return super.getCallForwardingIndicator();
    }

    @Override
    public void getCellBroadcastSmsConfig(Message message) {
        super.getCellBroadcastSmsConfig(message);
    }

    @Override
    public CellLocation getCellLocation(WorkSource workSource) {
        return super.getCellLocation(workSource);
    }

    @Override
    public List getCurrentDataConnectionList() {
        return super.getCurrentDataConnectionList();
    }

    @Override
    public PhoneInternalInterface.DataActivityState getDataActivityState() {
        return super.getDataActivityState();
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState() {
        return super.getDataConnectionState();
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String str) {
        return super.getDataConnectionState(str);
    }

    @Override
    public boolean getDataRoamingEnabled() {
        return super.getDataRoamingEnabled();
    }

    @Override
    public String getDeviceId() {
        return super.getDeviceId();
    }

    @Override
    public String getDeviceSvn() {
        return super.getDeviceSvn();
    }

    @Override
    public String getEsn() {
        return super.getEsn();
    }

    @Override
    public String getGroupIdLevel1() {
        return super.getGroupIdLevel1();
    }

    @Override
    public String getGroupIdLevel2() {
        return super.getGroupIdLevel2();
    }

    @Override
    public IccCard getIccCard() {
        return super.getIccCard();
    }

    @Override
    public IccFileHandler getIccFileHandler() {
        return super.getIccFileHandler();
    }

    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return super.getIccPhoneBookInterfaceManager();
    }

    @Override
    public boolean getIccRecordsLoaded() {
        return super.getIccRecordsLoaded();
    }

    @Override
    public String getIccSerialNumber() {
        return super.getIccSerialNumber();
    }

    @Override
    public String getImei() {
        return super.getImei();
    }

    @Override
    public String getLine1AlphaTag() {
        return super.getLine1AlphaTag();
    }

    @Override
    public String getLine1Number() {
        return super.getLine1Number();
    }

    @Override
    public LinkProperties getLinkProperties(String str) {
        return super.getLinkProperties(str);
    }

    @Override
    public String getMeid() {
        return super.getMeid();
    }

    @Override
    public boolean getMessageWaitingIndicator() {
        return super.getMessageWaitingIndicator();
    }

    @Override
    public int getPhoneType() {
        return super.getPhoneType();
    }

    @Override
    public SignalStrength getSignalStrength() {
        return super.getSignalStrength();
    }

    @Override
    public String getSubscriberId() {
        return super.getSubscriberId();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        return super.getVoiceMailAlphaTag();
    }

    @Override
    public String getVoiceMailNumber() {
        return super.getVoiceMailNumber();
    }

    @Override
    public boolean handlePinMmi(String str) {
        return super.handlePinMmi(str);
    }

    @Override
    public boolean isDataAllowed() {
        return super.isDataAllowed();
    }

    @Override
    public boolean isDataEnabled() {
        return super.isDataEnabled();
    }

    @Override
    public boolean isUserDataEnabled() {
        return super.isUserDataEnabled();
    }

    @Override
    public void migrateFrom(Phone phone) {
        super.migrateFrom(phone);
    }

    @Override
    public boolean needsOtaServiceProvisioning() {
        return super.needsOtaServiceProvisioning();
    }

    @Override
    public void notifyCallForwardingIndicator() {
        super.notifyCallForwardingIndicator();
    }

    @Override
    public void notifyDisconnect(Connection connection) {
        super.notifyDisconnect(connection);
    }

    @Override
    public void notifyPhoneStateChanged() {
        super.notifyPhoneStateChanged();
    }

    @Override
    public void notifyPreciseCallStateChanged() {
        super.notifyPreciseCallStateChanged();
    }

    @Override
    public void notifySuppServiceFailed(PhoneInternalInterface.SuppService suppService) {
        super.notifySuppServiceFailed(suppService);
    }

    @Override
    public void onTtyModeReceived(int i) {
        super.onTtyModeReceived(i);
    }

    @Override
    public void registerForOnHoldTone(Handler handler, int i, Object obj) {
        super.registerForOnHoldTone(handler, i, obj);
    }

    @Override
    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        super.registerForRingbackTone(handler, i, obj);
    }

    @Override
    public void registerForTtyModeReceived(Handler handler, int i, Object obj) {
        super.registerForTtyModeReceived(handler, i, obj);
    }

    @Override
    public void selectNetworkManually(OperatorInfo operatorInfo, boolean z, Message message) {
        super.selectNetworkManually(operatorInfo, z, message);
    }

    @Override
    public void setCellBroadcastSmsConfig(int[] iArr, Message message) {
        super.setCellBroadcastSmsConfig(iArr, message);
    }

    @Override
    public void setDataRoamingEnabled(boolean z) {
        super.setDataRoamingEnabled(z);
    }

    @Override
    public boolean setLine1Number(String str, String str2, Message message) {
        return super.setLine1Number(str, str2, message);
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message message) {
        super.setNetworkSelectionModeAutomatic(message);
    }

    @Override
    public void setRadioPower(boolean z) {
        super.setRadioPower(z);
    }

    @Override
    public void setUserDataEnabled(boolean z) {
        super.setUserDataEnabled(z);
    }

    @Override
    public void setVoiceMailNumber(String str, String str2, Message message) {
        super.setVoiceMailNumber(str, str2, message);
    }

    @Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
        super.startNetworkScan(networkScanRequest, message);
    }

    @Override
    @VisibleForTesting
    public void startOnHoldTone(Connection connection) {
        super.startOnHoldTone(connection);
    }

    @Override
    public void startRingbackTone() {
        super.startRingbackTone();
    }

    @Override
    public void stopNetworkScan(Message message) {
        super.stopNetworkScan(message);
    }

    @Override
    public void stopOnHoldTone(Connection connection) {
        super.stopOnHoldTone(connection);
    }

    @Override
    public void stopRingbackTone() {
        super.stopRingbackTone();
    }

    @Override
    public void unregisterForOnHoldTone(Handler handler) {
        super.unregisterForOnHoldTone(handler);
    }

    @Override
    public void unregisterForRingbackTone(Handler handler) {
        super.unregisterForRingbackTone(handler);
    }

    @Override
    public void unregisterForTtyModeReceived(Handler handler) {
        super.unregisterForTtyModeReceived(handler);
    }

    @Override
    public void updateServiceLocation() {
        super.updateServiceLocation();
    }

    public static class ImsDialArgs extends PhoneInternalInterface.DialArgs {
        public final int clirMode;
        public final Connection.RttTextStream rttTextStream;

        public static class Builder extends PhoneInternalInterface.DialArgs.Builder<Builder> {
            private int mClirMode = 0;
            private Connection.RttTextStream mRttTextStream;

            public static Builder from(PhoneInternalInterface.DialArgs dialArgs) {
                return new Builder().setUusInfo(dialArgs.uusInfo).setVideoState(dialArgs.videoState).setIntentExtras(dialArgs.intentExtras);
            }

            public static Builder from(ImsDialArgs imsDialArgs) {
                return new Builder().setUusInfo(imsDialArgs.uusInfo).setVideoState(imsDialArgs.videoState).setIntentExtras(imsDialArgs.intentExtras).setRttTextStream(imsDialArgs.rttTextStream).setClirMode(imsDialArgs.clirMode);
            }

            public Builder setRttTextStream(Connection.RttTextStream rttTextStream) {
                this.mRttTextStream = rttTextStream;
                return this;
            }

            public Builder setClirMode(int i) {
                this.mClirMode = i;
                return this;
            }

            @Override
            public ImsDialArgs build() {
                return new ImsDialArgs(this);
            }
        }

        private ImsDialArgs(Builder builder) {
            super(builder);
            this.rttTextStream = builder.mRttTextStream;
            this.clirMode = builder.mClirMode;
        }
    }

    public void setCurrentSubscriberUris(Uri[] uriArr) {
        this.mCurrentSubscriberUris = uriArr;
    }

    @Override
    public Uri[] getCurrentSubscriberUris() {
        return this.mCurrentSubscriberUris;
    }

    protected static class Cf {
        public final boolean mIsCfu;
        public final Message mOnComplete;
        public final String mSetCfNumber;

        public Cf(String str, boolean z, Message message) {
            this.mSetCfNumber = str;
            this.mIsCfu = z;
            this.mOnComplete = message;
        }
    }

    public ImsPhone(Context context, PhoneNotifier phoneNotifier, Phone phone) {
        this(context, phoneNotifier, phone, false);
    }

    @VisibleForTesting
    public ImsPhone(Context context, PhoneNotifier phoneNotifier, Phone phone, boolean z) {
        super(LOG_TAG, context, phoneNotifier, z);
        this.mPendingMMIs = new ArrayList<>();
        this.mSS = new ServiceState();
        this.mSilentRedialRegistrants = new RegistrantList();
        this.mImsRegistered = false;
        this.mRoaming = false;
        this.mSsnRegistrants = new RegistrantList();
        this.mExitEcmRunnable = new Runnable() {
            @Override
            public void run() {
                ImsPhone.this.exitEmergencyCallbackMode();
            }
        };
        this.mImsEcbmStateListener = new ImsEcbmStateListener() {
            public void onECBMEntered() {
                ImsPhone.this.logd("onECBMEntered");
                ImsPhone.this.handleEnterEmergencyCallbackMode();
            }

            public void onECBMExited() {
                ImsPhone.this.logd("onECBMExited");
                ImsPhone.this.handleExitEmergencyCallbackMode();
            }
        };
        this.mResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (getResultCode() == -1) {
                    CharSequence charSequenceExtra = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_TITLE);
                    CharSequence charSequenceExtra2 = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_MESSAGE);
                    CharSequence charSequenceExtra3 = intent.getCharSequenceExtra(Phone.EXTRA_KEY_NOTIFICATION_MESSAGE);
                    Intent intent2 = new Intent("android.intent.action.MAIN");
                    intent2.setClassName("com.android.settings", "com.android.settings.Settings$WifiCallingSettingsActivity");
                    intent2.putExtra(Phone.EXTRA_KEY_ALERT_SHOW, true);
                    intent2.putExtra(Phone.EXTRA_KEY_ALERT_TITLE, charSequenceExtra);
                    intent2.putExtra(Phone.EXTRA_KEY_ALERT_MESSAGE, charSequenceExtra2);
                    PendingIntent activity = PendingIntent.getActivity(ImsPhone.this.mContext, 0, intent2, 134217728);
                    if (BenesseExtension.getDchaState() != 0) {
                        activity = null;
                    }
                    ((NotificationManager) ImsPhone.this.mContext.getSystemService("notification")).notify("wifi_calling", 1, new Notification.Builder(ImsPhone.this.mContext).setSmallIcon(R.drawable.stat_sys_warning).setContentTitle(charSequenceExtra).setContentText(charSequenceExtra3).setAutoCancel(true).setContentIntent(activity).setStyle(new Notification.BigTextStyle().bigText(charSequenceExtra3)).setChannelId(NotificationChannelController.CHANNEL_ID_WFC).build());
                }
            }
        };
        this.mDefaultPhone = phone;
        this.mExternalCallTracker = TelephonyComponentFactory.getInstance().makeImsExternalCallTracker(this);
        this.mCT = TelephonyComponentFactory.getInstance().makeImsPhoneCallTracker(this);
        this.mCT.registerPhoneStateListener(this.mExternalCallTracker);
        this.mExternalCallTracker.setCallPuller(this.mCT);
        this.mSS.setStateOff();
        this.mPhoneId = this.mDefaultPhone.getPhoneId();
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        if (this.mDefaultPhone.getServiceStateTracker() != null) {
            this.mDefaultPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(this, 52, null);
        }
        setServiceState(1);
        this.mDefaultPhone.registerForServiceStateChanged(this, 53, null);
    }

    protected ImsPhone(String str, Context context, PhoneNotifier phoneNotifier, boolean z) {
        super(str, context, phoneNotifier, z);
        this.mPendingMMIs = new ArrayList<>();
        this.mSS = new ServiceState();
        this.mSilentRedialRegistrants = new RegistrantList();
        this.mImsRegistered = false;
        this.mRoaming = false;
        this.mSsnRegistrants = new RegistrantList();
        this.mExitEcmRunnable = new Runnable() {
            @Override
            public void run() {
                ImsPhone.this.exitEmergencyCallbackMode();
            }
        };
        this.mImsEcbmStateListener = new ImsEcbmStateListener() {
            public void onECBMEntered() {
                ImsPhone.this.logd("onECBMEntered");
                ImsPhone.this.handleEnterEmergencyCallbackMode();
            }

            public void onECBMExited() {
                ImsPhone.this.logd("onECBMExited");
                ImsPhone.this.handleExitEmergencyCallbackMode();
            }
        };
        this.mResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (getResultCode() == -1) {
                    CharSequence charSequenceExtra = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_TITLE);
                    CharSequence charSequenceExtra2 = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_MESSAGE);
                    CharSequence charSequenceExtra3 = intent.getCharSequenceExtra(Phone.EXTRA_KEY_NOTIFICATION_MESSAGE);
                    Intent intent2 = new Intent("android.intent.action.MAIN");
                    intent2.setClassName("com.android.settings", "com.android.settings.Settings$WifiCallingSettingsActivity");
                    intent2.putExtra(Phone.EXTRA_KEY_ALERT_SHOW, true);
                    intent2.putExtra(Phone.EXTRA_KEY_ALERT_TITLE, charSequenceExtra);
                    intent2.putExtra(Phone.EXTRA_KEY_ALERT_MESSAGE, charSequenceExtra2);
                    PendingIntent activity = PendingIntent.getActivity(ImsPhone.this.mContext, 0, intent2, 134217728);
                    if (BenesseExtension.getDchaState() != 0) {
                        activity = null;
                    }
                    ((NotificationManager) ImsPhone.this.mContext.getSystemService("notification")).notify("wifi_calling", 1, new Notification.Builder(ImsPhone.this.mContext).setSmallIcon(R.drawable.stat_sys_warning).setContentTitle(charSequenceExtra).setContentText(charSequenceExtra3).setAutoCancel(true).setContentIntent(activity).setStyle(new Notification.BigTextStyle().bigText(charSequenceExtra3)).setChannelId(NotificationChannelController.CHANNEL_ID_WFC).build());
                }
            }
        };
    }

    @Override
    public void dispose() {
        logd("dispose");
        this.mPendingMMIs.clear();
        this.mExternalCallTracker.tearDown();
        this.mCT.unregisterPhoneStateListener(this.mExternalCallTracker);
        this.mCT.unregisterForVoiceCallEnded(this);
        this.mCT.dispose();
        if (this.mDefaultPhone != null && this.mDefaultPhone.getServiceStateTracker() != null) {
            this.mDefaultPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(this);
            this.mDefaultPhone.unregisterForServiceStateChanged(this);
        }
    }

    @Override
    public ServiceState getServiceState() {
        return this.mSS;
    }

    @VisibleForTesting
    public void setServiceState(int i) {
        boolean z;
        synchronized (this) {
            z = this.mSS.getVoiceRegState() != i;
            this.mSS.setVoiceRegState(i);
        }
        updateDataServiceState();
        if (z && this.mDefaultPhone.getServiceStateTracker() != null) {
            this.mDefaultPhone.getServiceStateTracker().onImsServiceStateChanged();
        }
    }

    @Override
    public CallTracker getCallTracker() {
        return this.mCT;
    }

    public ImsExternalCallTracker getExternalCallTracker() {
        return this.mExternalCallTracker;
    }

    @Override
    public List<? extends ImsPhoneMmiCode> getPendingMmiCodes() {
        return this.mPendingMMIs;
    }

    @Override
    public void acceptCall(int i) throws CallStateException {
        this.mCT.acceptCall(i);
    }

    @Override
    public void rejectCall() throws CallStateException {
        this.mCT.rejectCall();
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        this.mCT.switchWaitingOrHoldingAndActive();
    }

    @Override
    public boolean canConference() {
        return this.mCT.canConference();
    }

    @Override
    public boolean canDial() {
        return this.mCT.canDial();
    }

    @Override
    public void conference() {
        this.mCT.conference();
    }

    @Override
    public void clearDisconnected() {
        this.mCT.clearDisconnected();
    }

    @Override
    public boolean canTransfer() {
        return this.mCT.canTransfer();
    }

    @Override
    public void explicitCallTransfer() {
        this.mCT.explicitCallTransfer();
    }

    @Override
    public ImsPhoneCall getForegroundCall() {
        return this.mCT.mForegroundCall;
    }

    @Override
    public ImsPhoneCall getBackgroundCall() {
        return this.mCT.mBackgroundCall;
    }

    @Override
    public ImsPhoneCall getRingingCall() {
        return this.mCT.mRingingCall;
    }

    @Override
    public boolean isImsAvailable() {
        return this.mCT.isImsServiceReady();
    }

    private boolean handleCallDeflectionIncallSupplementaryService(String str) {
        if (str.length() > 1) {
            return false;
        }
        if (getRingingCall().getState() != Call.State.IDLE) {
            logd("MmiCode 0: rejectCall");
            try {
                this.mCT.rejectCall();
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "reject failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.REJECT);
            }
        } else if (getBackgroundCall().getState() != Call.State.IDLE) {
            logd("MmiCode 0: hangupWaitingOrBackground");
            try {
                this.mCT.hangup(getBackgroundCall());
            } catch (CallStateException e2) {
                Rlog.d(LOG_TAG, "hangup failed", e2);
            }
        }
        return true;
    }

    protected void sendUssdResponse(String str, CharSequence charSequence, int i, ResultReceiver resultReceiver) {
        UssdResponse ussdResponse = new UssdResponse(str, charSequence);
        Bundle bundle = new Bundle();
        bundle.putParcelable("USSD_RESPONSE", ussdResponse);
        resultReceiver.send(i, bundle);
    }

    @Override
    public boolean handleUssdRequest(String str, ResultReceiver resultReceiver) throws CallStateException {
        if (this.mPendingMMIs.size() > 0) {
            logi("handleUssdRequest: queue full: " + Rlog.pii(LOG_TAG, str));
            sendUssdResponse(str, null, -1, resultReceiver);
            return true;
        }
        try {
            dialInternal(str, new ImsDialArgs.Builder().build(), resultReceiver);
        } catch (CallStateException e) {
            if (Phone.CS_FALLBACK.equals(e.getMessage())) {
                throw e;
            }
            Rlog.w(LOG_TAG, "Could not execute USSD " + e);
            sendUssdResponse(str, null, -1, resultReceiver);
        } catch (Exception e2) {
            Rlog.w(LOG_TAG, "Could not execute USSD " + e2);
            sendUssdResponse(str, null, -1, resultReceiver);
            return false;
        }
        return true;
    }

    private boolean handleCallWaitingIncallSupplementaryService(String str) {
        int length = str.length();
        if (length > 2) {
            return false;
        }
        ImsPhoneCall foregroundCall = getForegroundCall();
        try {
            if (length > 1) {
                logd("not support 1X SEND");
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.HANGUP);
            } else if (foregroundCall.getState() != Call.State.IDLE) {
                logd("MmiCode 1: hangup foreground");
                this.mCT.hangup(foregroundCall);
            } else {
                logd("MmiCode 1: switchWaitingOrHoldingAndActive");
                this.mCT.switchWaitingOrHoldingAndActive();
            }
        } catch (CallStateException e) {
            Rlog.d(LOG_TAG, "hangup failed", e);
            notifySuppServiceFailed(PhoneInternalInterface.SuppService.HANGUP);
        }
        return true;
    }

    private boolean handleCallHoldIncallSupplementaryService(String str) {
        int length = str.length();
        if (length > 2) {
            return false;
        }
        if (length > 1) {
            logd("separate not supported");
            notifySuppServiceFailed(PhoneInternalInterface.SuppService.SEPARATE);
        } else {
            try {
                if (getRingingCall().getState() != Call.State.IDLE) {
                    logd("MmiCode 2: accept ringing call");
                    this.mCT.acceptCall(0);
                } else {
                    logd("MmiCode 2: switchWaitingOrHoldingAndActive");
                    this.mCT.switchWaitingOrHoldingAndActive();
                }
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "switch failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.SWITCH);
            }
        }
        return true;
    }

    private boolean handleMultipartyIncallSupplementaryService(String str) {
        if (str.length() > 1) {
            return false;
        }
        logd("MmiCode 3: merge calls");
        conference();
        return true;
    }

    private boolean handleEctIncallSupplementaryService(String str) {
        if (str.length() != 1) {
            return false;
        }
        logd("MmiCode 4: not support explicit call transfer");
        notifySuppServiceFailed(PhoneInternalInterface.SuppService.TRANSFER);
        return true;
    }

    private boolean handleCcbsIncallSupplementaryService(String str) {
        if (str.length() > 1) {
            return false;
        }
        logi("MmiCode 5: CCBS not supported!");
        notifySuppServiceFailed(PhoneInternalInterface.SuppService.UNKNOWN);
        return true;
    }

    public void notifySuppSvcNotification(SuppServiceNotification suppServiceNotification) {
        logd("notifySuppSvcNotification: suppSvc = " + suppServiceNotification);
        this.mSsnRegistrants.notifyRegistrants(new AsyncResult((Object) null, suppServiceNotification, (Throwable) null));
    }

    @Override
    public boolean handleInCallMmiCommands(String str) {
        if (!isInCall() || TextUtils.isEmpty(str)) {
            return false;
        }
        switch (str.charAt(0)) {
        }
        return false;
    }

    @Override
    public boolean isInCall() {
        return getForegroundCall().getState().isAlive() || getBackgroundCall().getState().isAlive() || getRingingCall().getState().isAlive();
    }

    @Override
    public boolean isInEcm() {
        return this.mDefaultPhone.isInEcm();
    }

    @Override
    public void setIsInEcm(boolean z) {
        this.mDefaultPhone.setIsInEcm(z);
    }

    public void notifyNewRingingConnection(com.android.internal.telephony.Connection connection) {
        this.mDefaultPhone.notifyNewRingingConnectionP(connection);
    }

    public void notifyUnknownConnection(com.android.internal.telephony.Connection connection) {
        this.mDefaultPhone.notifyUnknownConnectionP(connection);
    }

    @Override
    public void notifyForVideoCapabilityChanged(boolean z) {
        this.mIsVideoCapable = z;
        this.mDefaultPhone.notifyForVideoCapabilityChanged(z);
    }

    @Override
    public com.android.internal.telephony.Connection dial(String str, PhoneInternalInterface.DialArgs dialArgs) throws CallStateException {
        return dialInternal(str, dialArgs, null);
    }

    protected com.android.internal.telephony.Connection dialInternal(String str, PhoneInternalInterface.DialArgs dialArgs, ResultReceiver resultReceiver) throws CallStateException {
        ImsDialArgs.Builder builderFrom;
        String strStripSeparators = PhoneNumberUtils.stripSeparators(str);
        if (handleInCallMmiCommands(strStripSeparators)) {
            return null;
        }
        if (!(dialArgs instanceof ImsDialArgs)) {
            builderFrom = ImsDialArgs.Builder.from(dialArgs);
        } else {
            builderFrom = ImsDialArgs.Builder.from((ImsDialArgs) dialArgs);
        }
        builderFrom.setClirMode(this.mCT.getClirMode());
        if (this.mDefaultPhone.getPhoneType() == 2) {
            return this.mCT.dial(str, builderFrom.build());
        }
        ImsPhoneMmiCode imsPhoneMmiCodeNewFromDialString = ImsPhoneMmiCode.newFromDialString(PhoneNumberUtils.extractNetworkPortionAlt(strStripSeparators), this, resultReceiver);
        logd("dialInternal: dialing w/ mmi '" + imsPhoneMmiCodeNewFromDialString + "'...");
        if (imsPhoneMmiCodeNewFromDialString == null) {
            return this.mCT.dial(str, builderFrom.build());
        }
        if (imsPhoneMmiCodeNewFromDialString.isTemporaryModeCLIR()) {
            builderFrom.setClirMode(imsPhoneMmiCodeNewFromDialString.getCLIRMode());
            return this.mCT.dial(imsPhoneMmiCodeNewFromDialString.getDialingNumber(), builderFrom.build());
        }
        if (!imsPhoneMmiCodeNewFromDialString.isSupportedOverImsPhone()) {
            logi("dialInternal: USSD not supported by IMS; fallback to CS.");
            throw new CallStateException(Phone.CS_FALLBACK);
        }
        this.mPendingMMIs.add(imsPhoneMmiCodeNewFromDialString);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, imsPhoneMmiCodeNewFromDialString, (Throwable) null));
        try {
            imsPhoneMmiCodeNewFromDialString.processCode();
        } catch (CallStateException e) {
            if (Phone.CS_FALLBACK.equals(e.getMessage())) {
                logi("dialInternal: fallback to GSM required.");
                this.mPendingMMIs.remove(imsPhoneMmiCodeNewFromDialString);
                throw e;
            }
        }
        return null;
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("sendDtmf called with invalid character '" + c + "'");
            return;
        }
        if (this.mCT.getState() == PhoneConstants.State.OFFHOOK) {
            this.mCT.sendDtmf(c, null);
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c) && (c < 'A' || c > 'D')) {
            loge("startDtmf called with invalid character '" + c + "'");
            return;
        }
        this.mCT.startDtmf(c);
    }

    @Override
    public void stopDtmf() {
        this.mCT.stopDtmf();
    }

    public void notifyIncomingRing() {
        logd("notifyIncomingRing");
        sendMessage(obtainMessage(14, new AsyncResult((Object) null, (Object) null, (Throwable) null)));
    }

    @Override
    public void setMute(boolean z) {
        this.mCT.setMute(z);
    }

    @Override
    public void setTTYMode(int i, Message message) {
        this.mCT.setTtyMode(i);
    }

    @Override
    public void setUiTTYMode(int i, Message message) {
        this.mCT.setUiTTYMode(i, message);
    }

    @Override
    public boolean getMute() {
        return this.mCT.getMute();
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mCT.getState();
    }

    protected boolean isValidCommandInterfaceCFReason(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    protected boolean isValidCommandInterfaceCFAction(int i) {
        switch (i) {
            case 0:
            case 1:
            case 3:
            case 4:
                return true;
            case 2:
            default:
                return false;
        }
    }

    protected boolean isCfEnable(int i) {
        return i == 1 || i == 3;
    }

    protected int getConditionFromCFReason(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return -1;
        }
    }

    protected int getCFReasonFromCondition(int i) {
        switch (i) {
        }
        return 3;
    }

    protected int getActionFromCFAction(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
            default:
                return -1;
            case 3:
                return 3;
            case 4:
                return 4;
        }
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message message) {
        logd("getCLIR");
        try {
            this.mCT.getUtInterface().queryCLIR(obtainMessage(51, message));
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    @Override
    public void setOutgoingCallerIdDisplay(int i, Message message) {
        logd("setCLIR action= " + i);
        try {
            this.mCT.getUtInterface().updateCLIR(i, obtainMessage(50, i, 0, message));
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    @Override
    public void getCallForwardingOption(int i, Message message) {
        logd("getCallForwardingOption reason=" + i);
        if (!isValidCommandInterfaceCFReason(i)) {
            if (message != null) {
                sendErrorResponse(message);
            }
        } else {
            logd("requesting call forwarding query.");
            try {
                this.mCT.getUtInterface().queryCallForward(getConditionFromCFReason(i), (String) null, obtainMessage(13, message));
            } catch (ImsException e) {
                sendErrorResponse(message, e);
            }
        }
    }

    @Override
    public void setCallForwardingOption(int i, int i2, String str, int i3, Message message) {
        setCallForwardingOption(i, i2, str, 1, i3, message);
    }

    public void setCallForwardingOption(int i, int i2, String str, int i3, int i4, Message message) {
        logd("setCallForwardingOption action=" + i + ", reason=" + i2 + " serviceClass=" + i3);
        if (!isValidCommandInterfaceCFAction(i) || !isValidCommandInterfaceCFReason(i2)) {
            if (message != null) {
                sendErrorResponse(message);
            }
        } else {
            try {
                this.mCT.getUtInterface().updateCallForward(getActionFromCFAction(i), getConditionFromCFReason(i2), str, i3, i4, obtainMessage(12, isCfEnable(i) ? 1 : 0, 0, new Cf(str, GsmMmiCode.isVoiceUnconditionalForwarding(i2, i3), message)));
            } catch (ImsException e) {
                sendErrorResponse(message, e);
            }
        }
    }

    @Override
    public void getCallWaiting(Message message) {
        logd("getCallWaiting");
        try {
            this.mCT.getUtInterface().queryCallWaiting(obtainMessage(49, message));
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    @Override
    public void setCallWaiting(boolean z, Message message) {
        setCallWaiting(z, 1, message);
    }

    public void setCallWaiting(boolean z, int i, Message message) {
        logd("setCallWaiting enable=" + z);
        try {
            this.mCT.getUtInterface().updateCallWaiting(z, i, obtainMessage(48, message));
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    protected int getCBTypeFromFacility(String str) {
        if (CommandsInterface.CB_FACILITY_BAOC.equals(str)) {
            return 2;
        }
        if (CommandsInterface.CB_FACILITY_BAOIC.equals(str)) {
            return 3;
        }
        if (CommandsInterface.CB_FACILITY_BAOICxH.equals(str)) {
            return 4;
        }
        if (CommandsInterface.CB_FACILITY_BAIC.equals(str)) {
            return 1;
        }
        if (CommandsInterface.CB_FACILITY_BAICr.equals(str)) {
            return 5;
        }
        if (CommandsInterface.CB_FACILITY_BA_ALL.equals(str)) {
            return 7;
        }
        if (CommandsInterface.CB_FACILITY_BA_MO.equals(str)) {
            return 8;
        }
        if (CommandsInterface.CB_FACILITY_BA_MT.equals(str)) {
            return 9;
        }
        return 0;
    }

    public void getCallBarring(String str, Message message) {
        getCallBarring(str, message, 0);
    }

    public void getCallBarring(String str, Message message, int i) {
        getCallBarring(str, "", message, i);
    }

    @Override
    public void getCallBarring(String str, String str2, Message message, int i) {
        logd("getCallBarring facility=" + str + ", serviceClass = " + i);
        try {
            this.mCT.getUtInterface().queryCallBarring(getCBTypeFromFacility(str), obtainMessage(47, message), i);
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    public void setCallBarring(String str, boolean z, String str2, Message message) {
        setCallBarring(str, z, str2, message, 0);
    }

    @Override
    public void setCallBarring(String str, boolean z, String str2, Message message, int i) {
        int i2;
        logd("setCallBarring facility=" + str + ", lockState=" + z + ", serviceClass = " + i);
        Message messageObtainMessage = obtainMessage(46, message);
        if (z) {
            i2 = 1;
        } else {
            i2 = 0;
        }
        try {
            this.mCT.getUtInterface().updateCallBarring(getCBTypeFromFacility(str), i2, messageObtainMessage, (String[]) null, i);
        } catch (ImsException e) {
            sendErrorResponse(message, e);
        }
    }

    @Override
    public void sendUssdResponse(String str) {
        logd("sendUssdResponse");
        ImsPhoneMmiCode imsPhoneMmiCodeNewFromUssdUserInput = ImsPhoneMmiCode.newFromUssdUserInput(str, this);
        this.mPendingMMIs.add(imsPhoneMmiCodeNewFromUssdUserInput);
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, imsPhoneMmiCodeNewFromUssdUserInput, (Throwable) null));
        imsPhoneMmiCodeNewFromUssdUserInput.sendUssd(str);
    }

    public void sendUSSD(String str, Message message) {
        this.mCT.sendUSSD(str, message);
    }

    @Override
    public void cancelUSSD() {
        this.mCT.cancelUSSD();
    }

    public void sendErrorResponse(Message message) {
        logd("sendErrorResponse");
        if (message != null) {
            AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
            message.sendToTarget();
        }
    }

    @VisibleForTesting
    public void sendErrorResponse(Message message, Throwable th) {
        logd("sendErrorResponse");
        if (message != null) {
            AsyncResult.forMessage(message, (Object) null, getCommandException(th));
            message.sendToTarget();
        }
    }

    protected CommandException getCommandException(int i, String str) {
        logd("getCommandException code= " + i + ", errorString= " + str);
        CommandException.Error error = CommandException.Error.GENERIC_FAILURE;
        if (i != 241) {
            switch (i) {
                case 801:
                    error = CommandException.Error.REQUEST_NOT_SUPPORTED;
                    break;
                case 802:
                    error = CommandException.Error.RADIO_NOT_AVAILABLE;
                    break;
                default:
                    switch (i) {
                        case 821:
                            error = CommandException.Error.PASSWORD_INCORRECT;
                            break;
                        case 822:
                            error = CommandException.Error.SS_MODIFIED_TO_DIAL;
                            break;
                        case 823:
                            error = CommandException.Error.SS_MODIFIED_TO_USSD;
                            break;
                        case 824:
                            error = CommandException.Error.SS_MODIFIED_TO_SS;
                            break;
                        case 825:
                            error = CommandException.Error.SS_MODIFIED_TO_DIAL_VIDEO;
                            break;
                    }
                    break;
            }
        } else {
            error = CommandException.Error.FDN_CHECK_FAILURE;
        }
        return new CommandException(error, str);
    }

    protected CommandException getCommandException(Throwable th) {
        if (th instanceof ImsException) {
            return getCommandException(((ImsException) th).getCode(), th.getMessage());
        }
        logd("getCommandException generic failure");
        return new CommandException(CommandException.Error.GENERIC_FAILURE);
    }

    protected void onNetworkInitiatedUssd(ImsPhoneMmiCode imsPhoneMmiCode) {
        logd("onNetworkInitiatedUssd");
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, imsPhoneMmiCode, (Throwable) null));
    }

    protected void onIncomingUSSD(int i, String str) {
        logd("onIncomingUSSD ussdMode=" + i);
        int i2 = 0;
        boolean z = i == 1;
        boolean z2 = (i == 0 || i == 1) ? false : true;
        ImsPhoneMmiCode imsPhoneMmiCode = null;
        int size = this.mPendingMMIs.size();
        while (true) {
            if (i2 >= size) {
                break;
            }
            if (!this.mPendingMMIs.get(i2).isPendingUSSD()) {
                i2++;
            } else {
                imsPhoneMmiCode = this.mPendingMMIs.get(i2);
                break;
            }
        }
        if (imsPhoneMmiCode != null) {
            if (z2) {
                imsPhoneMmiCode.onUssdFinishedError();
                return;
            } else {
                imsPhoneMmiCode.onUssdFinished(str, z);
                return;
            }
        }
        if (!z2 && str != null) {
            onNetworkInitiatedUssd(ImsPhoneMmiCode.newNetworkInitiatedUssd(str, z, this));
        }
    }

    public void onMMIDone(ImsPhoneMmiCode imsPhoneMmiCode) {
        logd("onMMIDone: mmi=" + imsPhoneMmiCode);
        if (this.mPendingMMIs.remove(imsPhoneMmiCode) || imsPhoneMmiCode.isUssdRequest() || imsPhoneMmiCode.isSsInfo()) {
            ResultReceiver ussdCallbackReceiver = imsPhoneMmiCode.getUssdCallbackReceiver();
            if (ussdCallbackReceiver != null) {
                sendUssdResponse(imsPhoneMmiCode.getDialString(), imsPhoneMmiCode.getMessage(), imsPhoneMmiCode.getState() == MmiCode.State.COMPLETE ? 100 : -1, ussdCallbackReceiver);
            } else {
                logv("onMMIDone: notifyRegistrants");
                this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, imsPhoneMmiCode, (Throwable) null));
            }
        }
    }

    @Override
    public ArrayList<com.android.internal.telephony.Connection> getHandoverConnection() {
        ArrayList<com.android.internal.telephony.Connection> arrayList = new ArrayList<>();
        arrayList.addAll(getForegroundCall().mConnections);
        arrayList.addAll(getBackgroundCall().mConnections);
        arrayList.addAll(getRingingCall().mConnections);
        if (arrayList.size() > 0) {
            return arrayList;
        }
        return null;
    }

    @Override
    public void notifySrvccState(Call.SrvccState srvccState) {
        this.mCT.notifySrvccState(srvccState);
    }

    public void initiateSilentRedial() {
        this.mSilentRedialRegistrants.notifyRegistrants(new AsyncResult((Object) null, this.mLastDialString, (Throwable) null));
    }

    @Override
    public void registerForSilentRedial(Handler handler, int i, Object obj) {
        this.mSilentRedialRegistrants.addUnique(handler, i, obj);
    }

    @Override
    public void unregisterForSilentRedial(Handler handler) {
        this.mSilentRedialRegistrants.remove(handler);
    }

    @Override
    public void registerForSuppServiceNotification(Handler handler, int i, Object obj) {
        this.mSsnRegistrants.addUnique(handler, i, obj);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler handler) {
        this.mSsnRegistrants.remove(handler);
    }

    @Override
    public int getSubId() {
        return this.mDefaultPhone.getSubId();
    }

    @Override
    public int getPhoneId() {
        return this.mDefaultPhone.getPhoneId();
    }

    protected CallForwardInfo getCallForwardInfo(ImsCallForwardInfo imsCallForwardInfo) {
        CallForwardInfo callForwardInfo = new CallForwardInfo();
        callForwardInfo.status = imsCallForwardInfo.getStatus();
        callForwardInfo.reason = getCFReasonFromCondition(imsCallForwardInfo.getCondition());
        callForwardInfo.serviceClass = 1;
        callForwardInfo.toa = imsCallForwardInfo.getToA();
        callForwardInfo.number = imsCallForwardInfo.getNumber();
        callForwardInfo.timeSeconds = imsCallForwardInfo.getTimeSeconds();
        return callForwardInfo;
    }

    public CallForwardInfo[] handleCfQueryResult(ImsCallForwardInfo[] imsCallForwardInfoArr) {
        CallForwardInfo[] callForwardInfoArr;
        if (imsCallForwardInfoArr != null && imsCallForwardInfoArr.length != 0) {
            callForwardInfoArr = new CallForwardInfo[imsCallForwardInfoArr.length];
        } else {
            callForwardInfoArr = null;
        }
        IccRecords iccRecords = this.mDefaultPhone.getIccRecords();
        if (imsCallForwardInfoArr == null || imsCallForwardInfoArr.length == 0) {
            if (iccRecords != null) {
                setVoiceCallForwardingFlag(iccRecords, 1, false, null);
            }
        } else {
            int length = imsCallForwardInfoArr.length;
            for (int i = 0; i < length; i++) {
                if (imsCallForwardInfoArr[i].getCondition() == 0 && iccRecords != null) {
                    setVoiceCallForwardingFlag(iccRecords, 1, imsCallForwardInfoArr[i].getStatus() == 1, imsCallForwardInfoArr[i].getNumber());
                }
                callForwardInfoArr[i] = getCallForwardInfo(imsCallForwardInfoArr[i]);
            }
        }
        return callForwardInfoArr;
    }

    protected int[] handleCbQueryResult(ImsSsInfo[] imsSsInfoArr) {
        int[] iArr = {0};
        if (imsSsInfoArr[0].getStatus() == 1) {
            iArr[0] = 1;
        }
        return iArr;
    }

    protected int[] handleCwQueryResult(ImsSsInfo[] imsSsInfoArr) {
        int[] iArr = {0, 0};
        if (imsSsInfoArr[0].getStatus() == 1) {
            iArr[0] = 1;
            iArr[1] = 1;
        }
        return iArr;
    }

    protected void sendResponse(Message message, Object obj, Throwable th) {
        if (message != null) {
            CommandException commandException = null;
            if (th != null) {
                commandException = getCommandException(th);
            }
            AsyncResult.forMessage(message, obj, commandException);
            message.sendToTarget();
        }
    }

    protected void updateDataServiceState() {
        if (this.mSS != null && this.mDefaultPhone.getServiceStateTracker() != null && this.mDefaultPhone.getServiceStateTracker().mSS != null) {
            ServiceState serviceState = this.mDefaultPhone.getServiceStateTracker().mSS;
            this.mSS.setDataRegState(serviceState.getDataRegState());
            this.mSS.setRilDataRadioTechnology(serviceState.getRilDataRadioTechnology());
            logd("updateDataServiceState: defSs = " + serviceState + " imsSs = " + this.mSS);
        }
    }

    @Override
    public void handleMessage(Message message) {
        AsyncResult asyncResult = (AsyncResult) message.obj;
        logd("handleMessage what=" + message.what);
        int i = message.what;
        switch (i) {
            case 12:
                IccRecords iccRecords = this.mDefaultPhone.getIccRecords();
                Cf cf = (Cf) asyncResult.userObj;
                if (cf.mIsCfu && asyncResult.exception == null && iccRecords != null) {
                    setVoiceCallForwardingFlag(iccRecords, 1, message.arg1 == 1, cf.mSetCfNumber);
                }
                sendResponse(cf.mOnComplete, null, asyncResult.exception);
                break;
            case 13:
                sendResponse((Message) asyncResult.userObj, asyncResult.exception == null ? handleCfQueryResult((ImsCallForwardInfo[]) asyncResult.result) : null, asyncResult.exception);
                break;
            default:
                switch (i) {
                    case 46:
                    case 48:
                        break;
                    case 47:
                    case 49:
                        if (asyncResult.exception == null) {
                            if (message.what == 47) {
                                objHandleCwQueryResult = handleCbQueryResult((ImsSsInfo[]) asyncResult.result);
                            } else if (message.what == 49) {
                                objHandleCwQueryResult = handleCwQueryResult((ImsSsInfo[]) asyncResult.result);
                            }
                        }
                        sendResponse((Message) asyncResult.userObj, objHandleCwQueryResult, asyncResult.exception);
                        break;
                    case 50:
                        if (asyncResult.exception == null) {
                            saveClirSetting(message.arg1);
                        }
                        break;
                    case 51:
                        Bundle bundle = (Bundle) asyncResult.result;
                        sendResponse((Message) asyncResult.userObj, bundle != null ? bundle.getIntArray(ImsPhoneMmiCode.UT_BUNDLE_KEY_CLIR) : null, asyncResult.exception);
                        break;
                    case 52:
                        logd("EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED");
                        updateDataServiceState();
                        break;
                    case 53:
                        ServiceState serviceState = (ServiceState) ((AsyncResult) message.obj).result;
                        if (this.mRoaming != serviceState.getRoaming()) {
                            logd("Roaming state changed");
                            updateRoamingState(serviceState.getRoaming());
                        }
                        break;
                    case 54:
                        logd("Voice call ended. Handle pending updateRoamingState.");
                        this.mCT.unregisterForVoiceCallEnded(this);
                        boolean currentRoaming = getCurrentRoaming();
                        if (this.mRoaming != currentRoaming) {
                            updateRoamingState(currentRoaming);
                        }
                        break;
                    default:
                        super.handleMessage(message);
                        break;
                }
                sendResponse((Message) asyncResult.userObj, null, asyncResult.exception);
                break;
        }
    }

    @VisibleForTesting
    public ImsEcbmStateListener getImsEcbmStateListener() {
        return this.mImsEcbmStateListener;
    }

    @Override
    public boolean isInEmergencyCall() {
        return this.mCT.isInEmergencyCall();
    }

    protected void sendEmergencyCallbackModeChange() {
        Intent intent = new Intent("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intent.putExtra("phoneinECMState", isInEcm());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        ActivityManager.broadcastStickyIntent(intent, -1);
        logd("sendEmergencyCallbackModeChange: isInEcm=" + isInEcm());
    }

    @Override
    public void exitEmergencyCallbackMode() {
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        logd("exitEmergencyCallbackMode()");
        try {
            this.mCT.getEcbmInterface().exitEmergencyCallbackMode();
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

    private void handleEnterEmergencyCallbackMode() {
        logd("handleEnterEmergencyCallbackMode,mIsPhoneInEcmState= " + isInEcm());
        if (!isInEcm()) {
            setIsInEcm(true);
            sendEmergencyCallbackModeChange();
            postDelayed(this.mExitEcmRunnable, SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L));
            this.mWakeLock.acquire();
        }
    }

    @Override
    public void handleExitEmergencyCallbackMode() {
        logd("handleExitEmergencyCallbackMode: mIsPhoneInEcmState = " + isInEcm());
        if (isInEcm()) {
            setIsInEcm(false);
        }
        removeCallbacks(this.mExitEcmRunnable);
        if (this.mEcmExitRespRegistrant != null) {
            this.mEcmExitRespRegistrant.notifyResult(Boolean.TRUE);
        }
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        sendEmergencyCallbackModeChange();
        ((GsmCdmaPhone) this.mDefaultPhone).notifyEmergencyCallRegistrants(false);
    }

    void handleTimerInEmergencyCallbackMode(int i) {
        switch (i) {
            case 0:
                postDelayed(this.mExitEcmRunnable, SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L));
                ((GsmCdmaPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.FALSE);
                break;
            case 1:
                removeCallbacks(this.mExitEcmRunnable);
                ((GsmCdmaPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.TRUE);
                break;
            default:
                loge("handleTimerInEmergencyCallbackMode, unsupported action " + i);
                break;
        }
    }

    @Override
    public void setOnEcbModeExitResponse(Handler handler, int i, Object obj) {
        this.mEcmExitRespRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler handler) {
        this.mEcmExitRespRegistrant.clear();
    }

    public void onFeatureCapabilityChanged() {
        this.mDefaultPhone.getServiceStateTracker().onImsCapabilityChanged();
    }

    @Override
    public boolean isVolteEnabled() {
        return this.mCT.isVolteEnabled();
    }

    @Override
    public boolean isWifiCallingEnabled() {
        return this.mCT.isVowifiEnabled();
    }

    @Override
    public boolean isVideoEnabled() {
        return this.mCT.isVideoCallEnabled();
    }

    @Override
    public int getImsRegistrationTech() {
        return this.mCT.getImsRegistrationTech();
    }

    @Override
    public Phone getDefaultPhone() {
        return this.mDefaultPhone;
    }

    @Override
    public boolean isImsRegistered() {
        return this.mImsRegistered;
    }

    public void setImsRegistered(boolean z) {
        this.mImsRegistered = z;
    }

    @Override
    public void callEndCleanupHandOverCallIfAny() {
        this.mCT.callEndCleanupHandOverCallIfAny();
    }

    public void processDisconnectReason(ImsReasonInfo imsReasonInfo) {
        if (imsReasonInfo.mCode == 1000 && imsReasonInfo.mExtraMessage != null && ImsManager.getInstance(this.mContext, this.mPhoneId).isWfcEnabledByUser()) {
            processWfcDisconnectForNotification(imsReasonInfo);
        }
    }

    private void processWfcDisconnectForNotification(ImsReasonInfo imsReasonInfo) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            loge("processDisconnectReason: CarrierConfigManager is not ready");
            return;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(getSubId());
        if (configForSubId == null) {
            loge("processDisconnectReason: no config for subId " + getSubId());
            return;
        }
        String[] stringArray = configForSubId.getStringArray("wfc_operator_error_codes_string_array");
        if (stringArray == null) {
            return;
        }
        String[] stringArray2 = this.mContext.getResources().getStringArray(R.array.config_face_acquire_vendor_keyguard_ignorelist);
        String[] stringArray3 = this.mContext.getResources().getStringArray(R.array.config_fillBuiltInDisplayCutoutArray);
        for (int i = 0; i < stringArray.length; i++) {
            String[] strArrSplit = stringArray[i].split("\\|");
            if (strArrSplit.length != 2) {
                loge("Invalid carrier config: " + stringArray[i]);
            } else if (imsReasonInfo.mExtraMessage.startsWith(strArrSplit[0])) {
                int length = strArrSplit[0].length();
                if (!Character.isLetterOrDigit(strArrSplit[0].charAt(length - 1)) || imsReasonInfo.mExtraMessage.length() <= length || !Character.isLetterOrDigit(imsReasonInfo.mExtraMessage.charAt(length))) {
                    CharSequence text = this.mContext.getText(R.string.new_sms_notification_content);
                    int i2 = Integer.parseInt(strArrSplit[1]);
                    if (i2 >= 0 && i2 < stringArray2.length && i2 < stringArray3.length) {
                        String str = imsReasonInfo.mExtraMessage;
                        String str2 = imsReasonInfo.mExtraMessage;
                        if (!stringArray2[i2].isEmpty()) {
                            str = String.format(stringArray2[i2], imsReasonInfo.mExtraMessage);
                        }
                        if (!stringArray3[i2].isEmpty()) {
                            str2 = String.format(stringArray3[i2], imsReasonInfo.mExtraMessage);
                        }
                        Intent intent = new Intent("com.android.ims.REGISTRATION_ERROR");
                        intent.putExtra(Phone.EXTRA_KEY_ALERT_TITLE, text);
                        intent.putExtra(Phone.EXTRA_KEY_ALERT_MESSAGE, str);
                        intent.putExtra(Phone.EXTRA_KEY_NOTIFICATION_MESSAGE, str2);
                        this.mContext.sendOrderedBroadcast(intent, null, this.mResultReceiver, null, -1, null, null);
                        return;
                    }
                    loge("Invalid index: " + stringArray[i]);
                }
            } else {
                continue;
            }
        }
    }

    @Override
    public boolean isUtEnabled() {
        return this.mCT.isUtEnabled();
    }

    @Override
    public void sendEmergencyCallStateChange(boolean z) {
        this.mDefaultPhone.sendEmergencyCallStateChange(z);
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean z) {
        this.mDefaultPhone.setBroadcastEmergencyCallStateChanges(z);
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock() {
        return this.mWakeLock;
    }

    @Override
    public NetworkStats getVtDataUsage(boolean z) {
        return this.mCT.getVtDataUsage(z);
    }

    protected void updateRoamingState(boolean z) {
        if (this.mCT.getState() == PhoneConstants.State.IDLE) {
            logd("updateRoamingState now: " + z);
            this.mRoaming = z;
            ImsManager imsManager = ImsManager.getInstance(this.mContext, this.mPhoneId);
            imsManager.setWfcMode(imsManager.getWfcMode(z), z);
            return;
        }
        logd("updateRoamingState postponed: " + z);
        this.mCT.registerForVoiceCallEnded(this, 54, null);
    }

    private boolean getCurrentRoaming() {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).isNetworkRoaming();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("ImsPhone extends:");
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.flush();
        printWriter.println("ImsPhone:");
        printWriter.println("  mDefaultPhone = " + this.mDefaultPhone);
        printWriter.println("  mPendingMMIs = " + this.mPendingMMIs);
        printWriter.println("  mPostDialHandler = " + this.mPostDialHandler);
        printWriter.println("  mSS = " + this.mSS);
        printWriter.println("  mWakeLock = " + this.mWakeLock);
        printWriter.println("  mIsPhoneInEcmState = " + isInEcm());
        printWriter.println("  mEcmExitRespRegistrant = " + this.mEcmExitRespRegistrant);
        printWriter.println("  mSilentRedialRegistrants = " + this.mSilentRedialRegistrants);
        printWriter.println("  mImsRegistered = " + this.mImsRegistered);
        printWriter.println("  mRoaming = " + this.mRoaming);
        printWriter.println("  mSsnRegistrants = " + this.mSsnRegistrants);
        printWriter.flush();
    }

    protected void logi(String str) {
        Rlog.i(LOG_TAG, "[" + this.mPhoneId + "] " + str);
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, "[" + this.mPhoneId + "] " + str);
    }

    protected void logd(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhoneId + "] " + str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhoneId + "] " + str);
    }
}
