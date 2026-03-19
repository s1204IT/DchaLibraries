package com.android.internal.telephony.imsphone;

import android.R;
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.ims.ImsCallForwardInfo;
import com.android.ims.ImsCallForwardInfoEx;
import com.android.ims.ImsEcbm;
import com.android.ims.ImsEcbmStateListener;
import com.android.ims.ImsException;
import com.android.ims.ImsMultiEndpoint;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsSsInfo;
import com.android.ims.ImsUtInterface;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallForwardInfoEx;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.SuppSrvRequest;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.TelephonyEventLog;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.google.android.mms.pdu.CharacterSets;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImsPhone extends ImsPhoneBase {
    static final int CANCEL_ECM_TIMER = 1;
    private static final boolean DBG = true;
    private static final int DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    private static final int EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED = 50;
    private static final int EVENT_GET_CALL_BARRING_DONE = 45;
    private static final int EVENT_GET_CALL_WAITING_DONE = 47;
    private static final int EVENT_GET_CLIR_DONE = 49;
    private static final int EVENT_SET_CALL_BARRING_DONE = 44;
    private static final int EVENT_SET_CALL_WAITING_DONE = 46;
    private static final int EVENT_SET_CLIR_DONE = 48;
    private static final String LOG_TAG = "ImsPhone";
    static final int RESTART_ECM_TIMER = 0;
    public static final String USSD_DURING_IMS_INCALL = "ussd_during_ims_incall";
    public static final String UT_BUNDLE_KEY_CLIR = "queryClir";
    private static final boolean VDBG = false;
    ImsPhoneCallTracker mCT;
    Phone mDefaultPhone;
    private String mDialString;
    private Registrant mEcmExitRespRegistrant;
    private Runnable mExitEcmRunnable;
    ImsExternalCallTracker mExternalCallTracker;
    private ImsEcbmStateListener mImsEcbmStateListener;
    ImsMultiEndpoint mImsMultiEndpoint;
    private boolean mImsRegistered;
    private boolean mIsPhoneInEcmState;
    private String mLastDialString;
    private ArrayList<ImsPhoneMmiCode> mPendingMMIs;
    private BroadcastReceiver mResultReceiver;
    private ServiceState mSS;
    private final RegistrantList mSilentRedialRegistrants;
    private RegistrantList mSsnRegistrants;
    public boolean mUssiCSFB;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void activateCellBroadcastSms(int activate, Message response) {
        super.activateCellBroadcastSms(activate, response);
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
    public List getAllCellInfo() {
        return super.getAllCellInfo();
    }

    @Override
    public void getAvailableNetworks(Message response) {
        super.getAvailableNetworks(response);
    }

    @Override
    public boolean getCallForwardingIndicator() {
        return super.getCallForwardingIndicator();
    }

    @Override
    public void getCellBroadcastSmsConfig(Message response) {
        super.getCellBroadcastSmsConfig(response);
    }

    @Override
    public CellLocation getCellLocation() {
        return super.getCellLocation();
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
    public void getDataCallList(Message response) {
        super.getDataCallList(response);
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState() {
        return super.getDataConnectionState();
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        return super.getDataConnectionState(apnType);
    }

    @Override
    public boolean getDataEnabled() {
        return super.getDataEnabled();
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
    public LinkProperties getLinkProperties(String apnType) {
        return super.getLinkProperties(apnType);
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
    public void getNeighboringCids(Message response) {
        super.getNeighboringCids(response);
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
    public boolean handlePinMmi(String dialString) {
        return super.handlePinMmi(dialString);
    }

    @Override
    public boolean isDataConnectivityPossible() {
        return super.isDataConnectivityPossible();
    }

    @Override
    public void migrateFrom(Phone from) {
        super.migrateFrom(from);
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
    public void notifyDisconnect(Connection cn) {
        super.notifyDisconnect(cn);
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
    public void onTtyModeReceived(int mode) {
        super.onTtyModeReceived(mode);
    }

    @Override
    public void registerForOnHoldTone(Handler h, int what, Object obj) {
        super.registerForOnHoldTone(h, what, obj);
    }

    @Override
    public void registerForRingbackTone(Handler h, int what, Object obj) {
        super.registerForRingbackTone(h, what, obj);
    }

    @Override
    public void registerForTtyModeReceived(Handler h, int what, Object obj) {
        super.registerForTtyModeReceived(h, what, obj);
    }

    @Override
    public void saveClirSetting(int commandInterfaceCLIRMode) {
        super.saveClirSetting(commandInterfaceCLIRMode);
    }

    @Override
    public void selectNetworkManually(OperatorInfo network, boolean persistSelection, Message response) {
        super.selectNetworkManually(network, persistSelection, response);
    }

    @Override
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response) {
        super.setCellBroadcastSmsConfig(configValuesArray, response);
    }

    @Override
    public void setDataEnabled(boolean enable) {
        super.setDataEnabled(enable);
    }

    @Override
    public void setDataRoamingEnabled(boolean enable) {
        super.setDataRoamingEnabled(enable);
    }

    @Override
    public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
        return super.setLine1Number(alphaTag, number, onComplete);
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message response) {
        super.setNetworkSelectionModeAutomatic(response);
    }

    @Override
    public void setRadioPower(boolean power) {
        super.setRadioPower(power);
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceMailNumber, Message onComplete) {
        super.setVoiceMailNumber(alphaTag, voiceMailNumber, onComplete);
    }

    @Override
    public void startRingbackTone() {
        super.startRingbackTone();
    }

    @Override
    public void stopRingbackTone() {
        super.stopRingbackTone();
    }

    @Override
    public void unregisterForOnHoldTone(Handler h) {
        super.unregisterForOnHoldTone(h);
    }

    @Override
    public void unregisterForRingbackTone(Handler h) {
        super.unregisterForRingbackTone(h);
    }

    @Override
    public void unregisterForTtyModeReceived(Handler h) {
        super.unregisterForTtyModeReceived(h);
    }

    @Override
    public void updateServiceLocation() {
        super.updateServiceLocation();
    }

    private static class Cf {
        final boolean mIsCfu;
        final Message mOnComplete;
        final String mSetCfNumber;

        Cf(String cfNumber, boolean isCfu, Message onComplete) {
            this.mSetCfNumber = cfNumber;
            this.mIsCfu = isCfu;
            this.mOnComplete = onComplete;
        }
    }

    public ImsPhone(Context context, PhoneNotifier notifier, Phone defaultPhone) {
        this(context, notifier, defaultPhone, false);
    }

    public ImsPhone(Context context, PhoneNotifier notifier, Phone defaultPhone, boolean unitTestMode) {
        super(LOG_TAG, context, notifier, unitTestMode);
        this.mUssiCSFB = false;
        this.mPendingMMIs = new ArrayList<>();
        this.mSS = new ServiceState();
        this.mSilentRedialRegistrants = new RegistrantList();
        this.mImsRegistered = false;
        this.mSsnRegistrants = new RegistrantList();
        this.mExitEcmRunnable = new Runnable() {
            @Override
            public void run() {
                ImsPhone.this.exitEmergencyCallbackMode();
            }
        };
        this.mImsEcbmStateListener = new ImsEcbmStateListener() {
            public void onECBMEntered() {
                Rlog.d(ImsPhone.LOG_TAG, "onECBMEntered");
                ImsPhone.this.handleEnterEmergencyCallbackMode();
            }

            public void onECBMExited() {
                Rlog.d(ImsPhone.LOG_TAG, "onECBMExited");
                ImsPhone.this.handleExitEmergencyCallbackMode();
            }
        };
        this.mResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (getResultCode() != -1) {
                    return;
                }
                Rlog.d(ImsPhone.LOG_TAG, "Receive registration broadcast!");
                CharSequence title = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_TITLE);
                CharSequence messageAlert = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_MESSAGE);
                CharSequence messageNotification = intent.getCharSequenceExtra("notificationMessage");
                Intent resultIntent = new Intent("android.intent.action.MAIN");
                resultIntent.setClassName("com.android.settings", "com.android.settings.Settings$WifiCallingSettingsActivity");
                resultIntent.putExtra(Phone.EXTRA_KEY_ALERT_SHOW, true);
                resultIntent.putExtra(Phone.EXTRA_KEY_ALERT_TITLE, title);
                resultIntent.putExtra(Phone.EXTRA_KEY_ALERT_MESSAGE, messageAlert);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(ImsPhone.this.mContext, 0, resultIntent, 134217728);
                if (BenesseExtension.getDchaState() != 0) {
                    resultPendingIntent = null;
                }
                Notification notification = new Notification.Builder(ImsPhone.this.mContext).setSmallIcon(R.drawable.stat_sys_warning).setContentTitle(title).setContentText(messageNotification).setAutoCancel(true).setContentIntent(resultPendingIntent).setStyle(new Notification.BigTextStyle().bigText(messageNotification)).build();
                NotificationManager notificationManager = (NotificationManager) ImsPhone.this.mContext.getSystemService("notification");
                notificationManager.notify("wifi_calling", 1, notification);
            }
        };
        this.mDefaultPhone = defaultPhone;
        this.mSS.setStateOff();
        this.mCT = TelephonyComponentFactory.getInstance().makeImsPhoneCallTracker(this);
        this.mExternalCallTracker = TelephonyComponentFactory.getInstance().makeImsExternalCallTracker(this, this.mCT);
        try {
            this.mImsMultiEndpoint = this.mCT.getMultiEndpointInterface();
            this.mImsMultiEndpoint.setExternalCallStateListener(this.mExternalCallTracker.getExternalCallStateListener());
        } catch (ImsException e) {
            Rlog.i(LOG_TAG, "ImsMultiEndpointInterface is not available.");
        }
        this.mPhoneId = this.mDefaultPhone.getPhoneId();
        this.mIsPhoneInEcmState = Boolean.parseBoolean(TelephonyManager.getTelephonyProperty(this.mPhoneId, "ril.cdma.inecmmode", "false"));
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        if (this.mDefaultPhone.getServiceStateTracker() != null) {
            this.mDefaultPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(this, 50, null);
        }
        updateDataServiceState();
    }

    @Override
    public void dispose() {
        Rlog.d(LOG_TAG, "dispose");
        this.mPendingMMIs.clear();
        this.mCT.dispose();
        if (this.mDefaultPhone == null || this.mDefaultPhone.getServiceStateTracker() == null) {
            return;
        }
        this.mDefaultPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(this);
    }

    @Override
    public ServiceState getServiceState() {
        return this.mSS;
    }

    void setServiceState(int state) {
        this.mSS.setVoiceRegState(state);
        updateDataServiceState();
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
        Rlog.d(LOG_TAG, "getPendingMmiCodes");
        dumpPendingMmi();
        return this.mPendingMMIs;
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        this.mCT.acceptCall(videoState);
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
    public void explicitCallTransfer(String number, int type) {
        this.mCT.unattendedCallTransfer(number, type);
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

    private boolean handleCallDeflectionIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }
        if (getRingingCall().getState() != Call.State.IDLE) {
            Rlog.d(LOG_TAG, "MmiCode 0: rejectCall");
            try {
                this.mCT.rejectCall();
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "reject failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.REJECT);
            }
        } else if (getBackgroundCall().getState() != Call.State.IDLE) {
            Rlog.d(LOG_TAG, "MmiCode 0: hangupWaitingOrBackground");
            try {
                this.mCT.hangup(getBackgroundCall());
            } catch (CallStateException e2) {
                Rlog.d(LOG_TAG, "hangup failed", e2);
            }
        }
        return true;
    }

    private boolean handleCallWaitingIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len > 2) {
            return false;
        }
        ImsPhoneCall call = getForegroundCall();
        try {
            if (len > 1) {
                Rlog.d(LOG_TAG, "not support 1X SEND");
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.HANGUP);
            } else if (call.getState() != Call.State.IDLE) {
                Rlog.d(LOG_TAG, "MmiCode 1: hangup foreground");
                this.mCT.hangup(call);
            } else {
                Rlog.d(LOG_TAG, "MmiCode 1: switchWaitingOrHoldingAndActive");
                this.mCT.switchWaitingOrHoldingAndActive();
            }
        } catch (CallStateException e) {
            Rlog.d(LOG_TAG, "hangup failed", e);
            notifySuppServiceFailed(PhoneInternalInterface.SuppService.HANGUP);
        }
        return true;
    }

    private boolean handleCallHoldIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len > 2) {
            return false;
        }
        if (len > 1) {
            Rlog.d(LOG_TAG, "separate not supported");
            notifySuppServiceFailed(PhoneInternalInterface.SuppService.SEPARATE);
        } else {
            try {
                if (getRingingCall().getState() != Call.State.IDLE) {
                    Rlog.d(LOG_TAG, "MmiCode 2: accept ringing call");
                    this.mCT.acceptCall(2);
                } else {
                    Rlog.d(LOG_TAG, "MmiCode 2: switchWaitingOrHoldingAndActive");
                    this.mCT.switchWaitingOrHoldingAndActive();
                }
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "switch failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.SWITCH);
            }
        }
        return true;
    }

    private boolean handleMultipartyIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }
        Rlog.d(LOG_TAG, "MmiCode 3: merge calls");
        conference();
        return true;
    }

    private boolean handleEctIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len != 1) {
            return false;
        }
        Rlog.d(LOG_TAG, "MmiCode 4: not support explicit call transfer");
        notifySuppServiceFailed(PhoneInternalInterface.SuppService.TRANSFER);
        return true;
    }

    private boolean handleCcbsIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }
        Rlog.i(LOG_TAG, "MmiCode 5: CCBS not supported!");
        notifySuppServiceFailed(PhoneInternalInterface.SuppService.UNKNOWN);
        return true;
    }

    public void notifySuppSvcNotification(SuppServiceNotification suppSvc) {
        Rlog.d(LOG_TAG, "notifySuppSvcNotification: suppSvc = " + suppSvc);
        AsyncResult ar = new AsyncResult((Object) null, suppSvc, (Throwable) null);
        this.mSsnRegistrants.notifyRegistrants(ar);
    }

    @Override
    public boolean handleInCallMmiCommands(String dialString) {
        if (!isInCall() || TextUtils.isEmpty(dialString)) {
            return false;
        }
        char ch = dialString.charAt(0);
        switch (ch) {
            case EVENT_SET_CLIR_DONE:
                boolean result = handleCallDeflectionIncallSupplementaryService(dialString);
                break;
            case '1':
                boolean result2 = handleCallWaitingIncallSupplementaryService(dialString);
                break;
            case '2':
                boolean result3 = handleCallHoldIncallSupplementaryService(dialString);
                break;
            case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                boolean result4 = handleMultipartyIncallSupplementaryService(dialString);
                break;
            case RadioNVItems.RIL_NV_CDMA_BC10:
                boolean result5 = handleEctIncallSupplementaryService(dialString);
                break;
            case '5':
                boolean result6 = handleCcbsIncallSupplementaryService(dialString);
                break;
        }
        return false;
    }

    private boolean isUssdDuringInCall(ImsPhoneMmiCode mmi) {
        if (mmi == null || !mmi.isUssdNumber()) {
            return false;
        }
        return isInCall();
    }

    @Override
    boolean isInCall() {
        Call.State foregroundCallState = getForegroundCall().getState();
        Call.State backgroundCallState = getBackgroundCall().getState();
        Call.State ringingCallState = getRingingCall().getState();
        if (foregroundCallState.isAlive() || backgroundCallState.isAlive()) {
            return true;
        }
        return ringingCallState.isAlive();
    }

    public void notifyNewRingingConnection(Connection c) {
        this.mDefaultPhone.notifyNewRingingConnectionP(c);
    }

    void notifyUnknownConnection(Connection c) {
        this.mDefaultPhone.notifyUnknownConnectionP(c);
    }

    @Override
    public void notifyForVideoCapabilityChanged(boolean isVideoCapable) {
        this.mIsVideoCapable = isVideoCapable;
        this.mDefaultPhone.notifyForVideoCapabilityChanged(isVideoCapable);
    }

    @Override
    public Connection dial(String dialString, int videoState) throws CallStateException {
        return dialInternal(dialString, videoState, null);
    }

    @Override
    public Connection dial(String dialString, UUSInfo uusInfo, int videoState, Bundle intentExtras) throws CallStateException {
        return dialInternal(dialString, videoState, intentExtras);
    }

    private Connection dialInternal(String dialString, int videoState, Bundle intentExtras) throws CallStateException {
        String newDialString = dialString;
        if (!PhoneNumberUtils.isUriNumber(dialString)) {
            newDialString = PhoneNumberUtils.stripSeparators(dialString);
        }
        if (handleInCallMmiCommands(newDialString)) {
            return null;
        }
        if (this.mDefaultPhone.getPhoneType() == 2) {
            return this.mCT.dial(dialString, videoState, intentExtras);
        }
        String networkPortion = dialString;
        if (!PhoneNumberUtils.isUriNumber(dialString)) {
            networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
        }
        ImsPhoneMmiCode mmi = ImsPhoneMmiCode.newFromDialString(networkPortion, this);
        Rlog.d(LOG_TAG, "dialing w/ mmi '" + mmi + "'...");
        if (isUssdDuringInCall(mmi)) {
            Rlog.d(LOG_TAG, "USSD during in-call, ignore this operation!");
            throw new CallStateException(USSD_DURING_IMS_INCALL);
        }
        this.mDialString = dialString;
        if (mmi == null) {
            return this.mCT.dial(dialString, videoState, intentExtras);
        }
        if (mmi.isTemporaryModeCLIR()) {
            return this.mCT.dial(mmi.getDialingNumber(), mmi.getCLIRMode(), videoState, intentExtras);
        }
        if (!mmi.isSupportedOverImsPhone()) {
            this.mDefaultPhone.setCsFallbackStatus(1);
            throw new CallStateException(Phone.CS_FALLBACK);
        }
        if (this.mUssiCSFB) {
            Rlog.d(LOG_TAG, "USSI CSFB");
            this.mUssiCSFB = false;
            throw new CallStateException(Phone.CS_FALLBACK);
        }
        this.mPendingMMIs.add(mmi);
        Rlog.d(LOG_TAG, "dialInternal: " + dialString + ", mmi=" + mmi);
        dumpPendingMmi();
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        mmi.processCode();
        return null;
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            Rlog.e(LOG_TAG, "sendDtmf called with invalid character '" + c + "'");
        } else {
            if (this.mCT.getState() != PhoneConstants.State.OFFHOOK) {
                return;
            }
            this.mCT.sendDtmf(c, null);
        }
    }

    @Override
    public void startDtmf(char c) {
        boolean z = true;
        if (!PhoneNumberUtils.is12Key(c) && (c < 'A' || c > 'D')) {
            z = false;
        }
        if (!z) {
            Rlog.e(LOG_TAG, "startDtmf called with invalid character '" + c + "'");
        } else {
            this.mCT.startDtmf(c);
        }
    }

    @Override
    public void stopDtmf() {
        this.mCT.stopDtmf();
    }

    public void notifyIncomingRing() {
        Rlog.d(LOG_TAG, "notifyIncomingRing");
        AsyncResult ar = new AsyncResult((Object) null, (Object) null, (Throwable) null);
        sendMessage(obtainMessage(14, ar));
    }

    @Override
    public void setMute(boolean muted) {
        this.mCT.setMute(muted);
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        this.mCT.setUiTTYMode(uiTtyMode, onComplete);
    }

    @Override
    public boolean getMute() {
        return this.mCT.getMute();
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mCT.getState();
    }

    public void handleMmiCodeCsfb(int reason, ImsPhoneMmiCode mmi) {
        Rlog.d(LOG_TAG, "handleMmiCodeCsfb: reason = " + reason + ", mDialString = " + this.mDialString + ", mmi=" + mmi);
        removeMmi(mmi);
        if (reason == 830) {
            this.mDefaultPhone.setCsFallbackStatus(2);
        } else if (reason == 831) {
            this.mDefaultPhone.setCsFallbackStatus(1);
        }
        SuppSrvRequest ss = SuppSrvRequest.obtain(15, null);
        ss.mParcel.writeString(this.mDialString);
        Message msgCSFB = this.mDefaultPhone.obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START, ss);
        this.mDefaultPhone.sendMessage(msgCSFB);
    }

    private boolean isValidCommandInterfaceCFReason(int commandInterfaceCFReason) {
        switch (commandInterfaceCFReason) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return true;
            default:
                return false;
        }
    }

    private boolean isValidCommandInterfaceCFAction(int commandInterfaceCFAction) {
        switch (commandInterfaceCFAction) {
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

    private boolean isCfEnable(int action) {
        return action == 1 || action == 3;
    }

    private int getConditionFromCFReason(int reason) {
        switch (reason) {
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
            case 6:
                return 6;
            default:
                return -1;
        }
    }

    private int getCFReasonFromCondition(int condition) {
        switch (condition) {
        }
        return 3;
    }

    private int getActionFromCFAction(int action) {
        switch (action) {
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
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        Rlog.d(LOG_TAG, "getCLIR");
        Message resp = obtainMessage(49, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.queryCLIR(resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @Override
    public void setOutgoingCallerIdDisplay(int clirMode, Message onComplete) {
        Rlog.d(LOG_TAG, "setCLIR action= " + clirMode);
        Message resp = obtainMessage(EVENT_SET_CLIR_DONE, clirMode, 0, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.updateCLIR(clirMode, resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        Rlog.d(LOG_TAG, "getCallForwardingOption reason=" + commandInterfaceCFReason);
        if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            Rlog.d(LOG_TAG, "requesting call forwarding query.");
            if (commandInterfaceCFReason == 0) {
                ((GsmCdmaPhone) this.mDefaultPhone).setSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
            }
            Message resp = obtainMessage(13, onComplete);
            try {
                ImsUtInterface ut = this.mCT.getUtInterface();
                ut.queryCallForward(getConditionFromCFReason(commandInterfaceCFReason), (String) null, resp);
                return;
            } catch (ImsException e) {
                sendErrorResponse(onComplete, e);
                return;
            }
        }
        if (onComplete == null) {
            return;
        }
        sendErrorResponse(onComplete);
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message onComplete) {
        setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, 1, timerSeconds, onComplete);
    }

    public void setCallForwardingOption(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int serviceClass, int timerSeconds, Message onComplete) {
        String getNumber;
        Rlog.d(LOG_TAG, "setCallForwardingOption action=" + commandInterfaceCFAction + ", reason=" + commandInterfaceCFReason + " serviceClass=" + serviceClass);
        if (isValidCommandInterfaceCFAction(commandInterfaceCFAction) && isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            if ((dialingNumber == null || dialingNumber.isEmpty()) && this.mDefaultPhone != null && this.mDefaultPhone.getPhoneType() == 1 && (this.mDefaultPhone instanceof GsmCdmaPhone) && ((GsmCdmaPhone) this.mDefaultPhone).isSupportSaveCFNumber() && isCfEnable(commandInterfaceCFAction) && (getNumber = ((GsmCdmaPhone) this.mDefaultPhone).getCFPreviousDialNumber(commandInterfaceCFReason)) != null && !getNumber.isEmpty()) {
                dialingNumber = getNumber;
            }
            Cf cf = new Cf(dialingNumber, commandInterfaceCFReason == 0, onComplete);
            Message resp = obtainMessage(12, isCfEnable(commandInterfaceCFAction) ? 1 : 0, 0, cf);
            try {
                ImsUtInterface ut = this.mCT.getUtInterface();
                ut.updateCallForward(getActionFromCFAction(commandInterfaceCFAction), getConditionFromCFReason(commandInterfaceCFReason), dialingNumber, serviceClass, timerSeconds, resp);
                return;
            } catch (ImsException e) {
                sendErrorResponse(onComplete, e);
                return;
            }
        }
        if (onComplete == null) {
            return;
        }
        sendErrorResponse(onComplete);
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        Rlog.d(LOG_TAG, "getCallWaiting");
        Message resp = obtainMessage(47, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.queryCallWaiting(resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        setCallWaiting(enable, 1, onComplete);
    }

    public void setCallWaiting(boolean enable, int serviceClass, Message onComplete) {
        Rlog.d(LOG_TAG, "setCallWaiting enable=" + enable);
        Message resp = obtainMessage(46, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.updateCallWaiting(enable, serviceClass, resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    private int getCBTypeFromFacility(String facility) {
        if (CommandsInterface.CB_FACILITY_BAOC.equals(facility)) {
            return 2;
        }
        if (CommandsInterface.CB_FACILITY_BAOIC.equals(facility)) {
            return 3;
        }
        if (CommandsInterface.CB_FACILITY_BAOICxH.equals(facility)) {
            return 4;
        }
        if (CommandsInterface.CB_FACILITY_BAIC.equals(facility)) {
            return 1;
        }
        if (CommandsInterface.CB_FACILITY_BAICr.equals(facility)) {
            return 5;
        }
        if (CommandsInterface.CB_FACILITY_BA_ALL.equals(facility)) {
            return 7;
        }
        if (CommandsInterface.CB_FACILITY_BA_MO.equals(facility)) {
            return 8;
        }
        if (CommandsInterface.CB_FACILITY_BA_MT.equals(facility)) {
            return 9;
        }
        return 0;
    }

    public void getCallBarring(String facility, Message onComplete) {
        Rlog.d(LOG_TAG, "getCallBarring facility=" + facility);
        Message resp = obtainMessage(EVENT_GET_CALL_BARRING_DONE, onComplete);
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.queryCallBarring(getCBTypeFromFacility(facility), resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    public void setCallBarring(String facility, boolean lockState, String password, Message onComplete) {
        int action;
        Rlog.d(LOG_TAG, "setCallBarring facility=" + facility + ", lockState=" + lockState);
        Message resp = obtainMessage(44, onComplete);
        if (lockState) {
            action = 1;
        } else {
            action = 0;
        }
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            ut.updateCallBarring(getCBTypeFromFacility(facility), action, resp, (String[]) null);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    private static class CfEx {
        final boolean mIsCfu;
        final Message mOnComplete;
        final String mSetCfNumber;
        final long[] mSetTimeSlot;

        CfEx(String cfNumber, long[] cfTimeSlot, boolean isCfu, Message onComplete) {
            this.mSetCfNumber = cfNumber;
            this.mSetTimeSlot = cfTimeSlot;
            this.mIsCfu = isCfu;
            this.mOnComplete = onComplete;
        }
    }

    @Override
    public void getCallForwardInTimeSlot(int commandInterfaceCFReason, Message onComplete) {
        Rlog.d(LOG_TAG, "getCallForwardInTimeSlot reason = " + commandInterfaceCFReason);
        if (commandInterfaceCFReason == 0) {
            Rlog.d(LOG_TAG, "requesting call forwarding in a time slot query.");
            ((GsmCdmaPhone) this.mDefaultPhone).setSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
            Message resp = obtainMessage(CharacterSets.ISO_8859_13, onComplete);
            try {
                ImsUtInterface ut = this.mCT.getUtInterface();
                ut.queryCallForwardInTimeSlot(getConditionFromCFReason(commandInterfaceCFReason), resp);
                return;
            } catch (ImsException e) {
                sendErrorResponse(onComplete, e);
                return;
            }
        }
        if (onComplete == null) {
            return;
        }
        sendErrorResponse(onComplete);
    }

    @Override
    public void setCallForwardInTimeSlot(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, long[] timeSlot, Message onComplete) {
        Rlog.d(LOG_TAG, "setCallForwardInTimeSlot action = " + commandInterfaceCFAction + ", reason = " + commandInterfaceCFReason);
        if (isValidCommandInterfaceCFAction(commandInterfaceCFAction) && commandInterfaceCFReason == 0) {
            CfEx cfEx = new CfEx(dialingNumber, timeSlot, true, onComplete);
            Message resp = obtainMessage(CharacterSets.ISO_8859_14, commandInterfaceCFAction, 0, cfEx);
            try {
                ImsUtInterface ut = this.mCT.getUtInterface();
                ut.updateCallForwardInTimeSlot(getActionFromCFAction(commandInterfaceCFAction), getConditionFromCFReason(commandInterfaceCFReason), dialingNumber, timerSeconds, timeSlot, resp);
                return;
            } catch (ImsException e) {
                sendErrorResponse(onComplete, e);
                return;
            }
        }
        if (onComplete == null) {
            return;
        }
        sendErrorResponse(onComplete);
    }

    private CallForwardInfoEx[] handleCfInTimeSlotQueryResult(ImsCallForwardInfoEx[] infos) {
        CallForwardInfoEx[] cfInfos = null;
        if (infos != null && infos.length != 0) {
            cfInfos = new CallForwardInfoEx[infos.length];
        }
        IccRecords r = this.mDefaultPhone.getIccRecords();
        if (infos == null || infos.length == 0) {
            if (r != null) {
                setVoiceCallForwardingFlag(r, 1, false, null);
                ((GsmCdmaPhone) this.mDefaultPhone).setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
            }
        } else {
            int s = infos.length;
            for (int i = 0; i < s; i++) {
                if (infos[i].mCondition == 0 && r != null) {
                    setVoiceCallForwardingFlag(r, 1, infos[i].mStatus == 1, infos[i].mNumber);
                    String mode = infos[i].mStatus == 1 ? "enabled_ut_cfu_mode_on" : "enabled_ut_cfu_mode_off";
                    ((GsmCdmaPhone) this.mDefaultPhone).setSystemProperty("persist.radio.ut.cfu.mode", mode);
                    saveTimeSlot(infos[i].mTimeSlot);
                }
                cfInfos[i] = getCallForwardInfoEx(infos[i]);
            }
        }
        return cfInfos;
    }

    private CallForwardInfoEx getCallForwardInfoEx(ImsCallForwardInfoEx info) {
        CallForwardInfoEx cfInfo = new CallForwardInfoEx();
        cfInfo.status = info.mStatus;
        cfInfo.reason = getCFReasonFromCondition(info.mCondition);
        cfInfo.serviceClass = info.mServiceClass;
        cfInfo.toa = info.mToA;
        cfInfo.number = info.mNumber;
        cfInfo.timeSeconds = info.mTimeSeconds;
        cfInfo.timeSlot = info.mTimeSlot;
        return cfInfo;
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        Rlog.d(LOG_TAG, "sendUssdResponse");
        ImsPhoneMmiCode mmi = ImsPhoneMmiCode.newFromUssdUserInput(ussdMessge, this);
        this.mPendingMMIs.add(mmi);
        Rlog.d(LOG_TAG, "sendUssdResponse: " + ussdMessge + ", mmi=" + mmi);
        dumpPendingMmi();
        this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
        mmi.sendUssd(ussdMessge);
    }

    public void sendUSSD(String ussdString, Message response) {
        this.mCT.sendUSSD(ussdString, response);
    }

    @Override
    public void cancelUSSD() {
        this.mCT.cancelUSSD();
    }

    private void sendErrorResponse(Message onComplete) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (onComplete == null) {
            return;
        }
        AsyncResult.forMessage(onComplete, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
        onComplete.sendToTarget();
    }

    void sendErrorResponse(Message onComplete, Throwable e) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (onComplete == null) {
            return;
        }
        AsyncResult.forMessage(onComplete, (Object) null, getCommandException(e));
        onComplete.sendToTarget();
    }

    private CommandException getCommandException(int code, String errorString) {
        Rlog.d(LOG_TAG, "getCommandException code= " + code + ", errorString= " + errorString);
        CommandException.Error error = CommandException.Error.GENERIC_FAILURE;
        switch (code) {
            case 801:
                error = CommandException.Error.REQUEST_NOT_SUPPORTED;
                break;
            case 802:
                error = CommandException.Error.RADIO_NOT_AVAILABLE;
                break;
            case 821:
                error = CommandException.Error.PASSWORD_INCORRECT;
                break;
            case 830:
                error = CommandException.Error.UT_XCAP_403_FORBIDDEN;
                break;
            case 831:
                error = CommandException.Error.UT_UNKNOWN_HOST;
                break;
            case 833:
                if (((GsmCdmaPhone) this.mDefaultPhone).isEnableXcapHttpResponse409()) {
                    Rlog.d(LOG_TAG, "getCommandException UT_XCAP_409_CONFLICT");
                    error = CommandException.Error.UT_XCAP_409_CONFLICT;
                }
                break;
        }
        return new CommandException(error, errorString);
    }

    private CommandException getCommandException(Throwable e) {
        if (e instanceof ImsException) {
            CommandException ex = getCommandException(((ImsException) e).getCode(), e.getMessage());
            return ex;
        }
        Rlog.d(LOG_TAG, "getCommandException generic failure");
        CommandException ex2 = new CommandException(CommandException.Error.GENERIC_FAILURE);
        return ex2;
    }

    private void onNetworkInitiatedUssd(ImsPhoneMmiCode mmi) {
        Rlog.d(LOG_TAG, "onNetworkInitiatedUssd");
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
    }

    void onIncomingUSSD(int ussdMode, String ussdMessage) {
        Rlog.d(LOG_TAG, "onIncomingUSSD ussdMode=" + ussdMode);
        boolean isUssdRequest = ussdMode == 1;
        boolean isUssdError = (ussdMode == 0 || ussdMode == 1) ? false : true;
        ImsPhoneMmiCode found = null;
        int i = 0;
        int s = this.mPendingMMIs.size();
        while (true) {
            if (i >= s) {
                break;
            }
            if (!this.mPendingMMIs.get(i).isPendingUSSD()) {
                i++;
            } else {
                ImsPhoneMmiCode found2 = this.mPendingMMIs.get(i);
                found = found2;
                break;
            }
        }
        if (found != null) {
            if (isUssdError) {
                this.mUssiCSFB = true;
                found.onUssdFinishedError();
                return;
            } else {
                found.onUssdFinished(ussdMessage, isUssdRequest);
                return;
            }
        }
        if (isUssdError || ussdMessage == null) {
            return;
        }
        ImsPhoneMmiCode mmi = ImsPhoneMmiCode.newNetworkInitiatedUssd(ussdMessage, isUssdRequest, this);
        onNetworkInitiatedUssd(mmi);
    }

    public void onMMIDone(ImsPhoneMmiCode mmi) {
        Rlog.d(LOG_TAG, "onMMIDone: " + mmi + ", mUssiCSFB=" + this.mUssiCSFB);
        dumpPendingMmi();
        if (this.mUssiCSFB) {
            String dialString = mmi.getUssdDialString();
            Message msgCSFB = this.mDefaultPhone.obtainMessage(TelephonyEventLog.TAG_IMS_CALL_RECEIVE, dialString);
            this.mDefaultPhone.sendMessage(msgCSFB);
            this.mPendingMMIs.remove(mmi);
            return;
        }
        if (!this.mPendingMMIs.remove(mmi) && !mmi.isUssdRequest()) {
            return;
        }
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
    }

    public void removeMmi(ImsPhoneMmiCode mmi) {
        Rlog.d(LOG_TAG, "removeMmi: " + mmi);
        dumpPendingMmi();
        this.mPendingMMIs.remove(mmi);
    }

    public void dumpPendingMmi() {
        int size = this.mPendingMMIs.size();
        if (size == 0) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: none");
            return;
        }
        for (int i = 0; i < size; i++) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: " + this.mPendingMMIs.get(i));
        }
    }

    @Override
    public ArrayList<Connection> getHandoverConnection() {
        ArrayList<Connection> connList = new ArrayList<>();
        connList.addAll(getForegroundCall().mConnections);
        connList.addAll(getBackgroundCall().mConnections);
        connList.addAll(getRingingCall().mConnections);
        if (connList.size() > 0) {
            return connList;
        }
        return null;
    }

    @Override
    public void notifySrvccState(Call.SrvccState state) {
        this.mCT.notifySrvccState(state);
    }

    void initiateSilentRedial() {
        String result = this.mLastDialString;
        AsyncResult ar = new AsyncResult((Object) null, result, (Throwable) null);
        if (ar == null) {
            return;
        }
        this.mSilentRedialRegistrants.notifyRegistrants(ar);
    }

    @Override
    public void registerForSilentRedial(Handler h, int what, Object obj) {
        this.mSilentRedialRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSilentRedial(Handler h) {
        this.mSilentRedialRegistrants.remove(h);
    }

    @Override
    public void registerForSuppServiceNotification(Handler h, int what, Object obj) {
        this.mSsnRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        this.mSsnRegistrants.remove(h);
    }

    @Override
    public int getSubId() {
        return this.mDefaultPhone.getSubId();
    }

    @Override
    public int getPhoneId() {
        return this.mDefaultPhone.getPhoneId();
    }

    private CallForwardInfo getCallForwardInfo(ImsCallForwardInfo info) {
        CallForwardInfo cfInfo = new CallForwardInfo();
        cfInfo.status = info.mStatus;
        cfInfo.reason = getCFReasonFromCondition(info.mCondition);
        cfInfo.serviceClass = 1;
        cfInfo.toa = info.mToA;
        cfInfo.number = info.mNumber;
        cfInfo.timeSeconds = info.mTimeSeconds;
        return cfInfo;
    }

    private CallForwardInfo[] handleCfQueryResult(ImsCallForwardInfo[] infos) {
        CallForwardInfo[] cfInfos = null;
        if (infos != null && infos.length != 0) {
            cfInfos = new CallForwardInfo[infos.length];
        }
        IccRecords r = this.mDefaultPhone.getIccRecords();
        if (infos == null || infos.length == 0) {
            if (r != null) {
                setVoiceCallForwardingFlag(r, 1, false, null);
            }
        } else {
            int s = infos.length;
            for (int i = 0; i < s; i++) {
                if (infos[i].mCondition == 0 && r != null) {
                    setVoiceCallForwardingFlag(r, 1, infos[i].mStatus == 1, infos[i].mNumber);
                    String mode = infos[i].mStatus == 1 ? "enabled_ut_cfu_mode_on" : "enabled_ut_cfu_mode_off";
                    ((GsmCdmaPhone) this.mDefaultPhone).setSystemProperty("persist.radio.ut.cfu.mode", mode);
                }
                cfInfos[i] = getCallForwardInfo(infos[i]);
            }
        }
        return cfInfos;
    }

    private int[] handleCbQueryResult(ImsSsInfo[] infos) {
        int[] cbInfos = {infos[0].mStatus};
        return cbInfos;
    }

    private int[] handleCwQueryResult(ImsSsInfo[] infos) {
        int[] cwInfos = {0, 0};
        if (infos[0].mStatus == 1) {
            cwInfos[0] = 1;
            cwInfos[1] = 1;
        }
        return cwInfos;
    }

    private void sendResponse(Message onComplete, Object result, Throwable e) {
        if (onComplete == null) {
            return;
        }
        CommandException ex = null;
        if (e != null) {
            ex = getCommandException(e);
        }
        AsyncResult.forMessage(onComplete, result, ex);
        onComplete.sendToTarget();
    }

    private void updateDataServiceState() {
        if (this.mSS == null || this.mDefaultPhone.getServiceStateTracker() == null || this.mDefaultPhone.getServiceStateTracker().mSS == null) {
            return;
        }
        ServiceState ss = this.mDefaultPhone.getServiceStateTracker().mSS;
        this.mSS.setDataRegState(ss.getDataRegState());
        this.mSS.setRilDataRadioTechnology(ss.getRilDataRadioTechnology());
        Rlog.d(LOG_TAG, "updateDataServiceState: defSs = " + ss + " imsSs = " + this.mSS);
    }

    @Override
    public void handleMessage(Message msg) {
        ImsException imsException;
        ImsException imsException2;
        ImsException imsException3;
        Message resp;
        ImsException imsException4;
        Message resp2;
        AsyncResult ar = (AsyncResult) msg.obj;
        Rlog.d(LOG_TAG, "handleMessage what=" + msg.what);
        switch (msg.what) {
            case 12:
                IccRecords r = this.mDefaultPhone.getIccRecords();
                Cf cf = (Cf) ar.userObj;
                int cfAction = msg.arg1;
                int cfReason = msg.arg2;
                int cfEnable = isCfEnable(cfAction) ? 1 : 0;
                if (cf.mIsCfu && ar.exception == null && r != null) {
                    if (((GsmCdmaPhone) this.mDefaultPhone).queryCFUAgainAfterSet() && cfReason == 0) {
                        if (ar.result == null) {
                            Rlog.i(LOG_TAG, "arResult is null.");
                        } else {
                            Rlog.d(LOG_TAG, "[EVENT_SET_CALL_FORWARD_DONE check cfinfo.");
                            handleCfQueryResult((ImsCallForwardInfo[]) ar.result);
                        }
                    } else {
                        setVoiceCallForwardingFlag(r, 1, cfEnable == 1, cf.mSetCfNumber);
                    }
                }
                if (this.mDefaultPhone != null && this.mDefaultPhone.getPhoneType() == 1 && (this.mDefaultPhone instanceof GsmCdmaPhone) && ((GsmCdmaPhone) this.mDefaultPhone).isSupportSaveCFNumber() && ar.exception == null) {
                    if (cfEnable == 1) {
                        boolean ret = ((GsmCdmaPhone) this.mDefaultPhone).applyCFSharePreference(cfReason, cf.mSetCfNumber);
                        if (!ret) {
                            Rlog.d(LOG_TAG, "applySharePreference false.");
                        }
                    }
                    if (cfAction == 4) {
                        ((GsmCdmaPhone) this.mDefaultPhone).clearCFSharePreference(cfReason);
                    }
                }
                sendResponse(cf.mOnComplete, null, ar.exception);
                break;
            case 13:
                CallForwardInfo[] cfInfos = null;
                if (ar.exception == null) {
                    cfInfos = handleCfQueryResult((ImsCallForwardInfo[]) ar.result);
                }
                sendResponse((Message) ar.userObj, cfInfos, ar.exception);
                break;
            case 44:
                if (((GsmCdmaPhone) this.mDefaultPhone).isOpTransferXcap404() && ar.exception != null && (ar.exception instanceof ImsException) && msg.what == 44 && (imsException3 = ar.exception) != null && imsException3.getCode() == 832 && (resp = (Message) ar.userObj) != null) {
                    AsyncResult.forMessage(resp, (Object) null, new CommandException(CommandException.Error.UT_XCAP_404_NOT_FOUND));
                    resp.sendToTarget();
                }
                sendResponse((Message) ar.userObj, null, ar.exception);
                break;
            case EVENT_GET_CALL_BARRING_DONE:
                if (((GsmCdmaPhone) this.mDefaultPhone).isOpTransferXcap404() && ar.exception != null && (ar.exception instanceof ImsException) && (imsException4 = ar.exception) != null && imsException4.getCode() == 832 && (resp2 = (Message) ar.userObj) != null) {
                    AsyncResult.forMessage(resp2, (Object) null, new CommandException(CommandException.Error.UT_XCAP_404_NOT_FOUND));
                    resp2.sendToTarget();
                }
                int[] ssInfos = null;
                if (ar.exception == null) {
                    if (msg.what == EVENT_GET_CALL_BARRING_DONE) {
                        ssInfos = handleCbQueryResult((ImsSsInfo[]) ar.result);
                    } else if (msg.what == 47) {
                        ssInfos = handleCwQueryResult((ImsSsInfo[]) ar.result);
                    }
                }
                sendResponse((Message) ar.userObj, ssInfos, ar.exception);
                break;
            case 46:
                sendResponse((Message) ar.userObj, null, ar.exception);
                break;
            case 47:
                int[] ssInfos2 = null;
                if (ar.exception == null) {
                }
                sendResponse((Message) ar.userObj, ssInfos2, ar.exception);
                break;
            case EVENT_SET_CLIR_DONE:
                if (ar.exception == null) {
                    saveClirSetting(msg.arg1);
                }
                if (((GsmCdmaPhone) this.mDefaultPhone).isOpTransferXcap404()) {
                    AsyncResult.forMessage(resp, (Object) null, new CommandException(CommandException.Error.UT_XCAP_404_NOT_FOUND));
                    resp.sendToTarget();
                }
                sendResponse((Message) ar.userObj, null, ar.exception);
                break;
            case 49:
                Bundle ssInfo = (Bundle) ar.result;
                int[] clirInfo = null;
                if (ssInfo != null) {
                    clirInfo = ssInfo.getIntArray("queryClir");
                }
                sendResponse((Message) ar.userObj, clirInfo, ar.exception);
                break;
            case 50:
                Rlog.d(LOG_TAG, "EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED");
                updateDataServiceState();
                break;
            case CharacterSets.ISO_8859_13:
                CallForwardInfoEx[] cfInfosEx = null;
                if (ar.exception == null) {
                    cfInfosEx = handleCfInTimeSlotQueryResult((ImsCallForwardInfoEx[]) ar.result);
                }
                if (ar.exception != null && (ar.exception instanceof ImsException) && (imsException2 = ar.exception) != null && imsException2.getCode() == 830) {
                    this.mDefaultPhone.setCsFallbackStatus(2);
                    Message resp3 = (Message) ar.userObj;
                    if (resp3 != null) {
                        AsyncResult.forMessage(resp3, cfInfosEx, new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED));
                        resp3.sendToTarget();
                    }
                }
                sendResponse((Message) ar.userObj, cfInfosEx, ar.exception);
                break;
            case CharacterSets.ISO_8859_14:
                IccRecords records = this.mDefaultPhone.getIccRecords();
                CfEx cfEx = (CfEx) ar.userObj;
                if (cfEx.mIsCfu && ar.exception == null && records != null) {
                    setVoiceCallForwardingFlag(records, 1, (isCfEnable(msg.arg1) ? 1 : 0) == 1, cfEx.mSetCfNumber);
                    saveTimeSlot(cfEx.mSetTimeSlot);
                }
                if (ar.exception != null && (ar.exception instanceof ImsException) && (imsException = ar.exception) != null && imsException.getCode() == 830) {
                    this.mDefaultPhone.setCsFallbackStatus(2);
                    Message resp4 = cfEx.mOnComplete;
                    if (resp4 != null) {
                        AsyncResult.forMessage(resp4, (Object) null, new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED));
                        resp4.sendToTarget();
                    }
                }
                sendResponse(cfEx.mOnComplete, null, ar.exception);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    public ImsEcbmStateListener getImsEcbmStateListener() {
        return this.mImsEcbmStateListener;
    }

    @Override
    public boolean isInEmergencyCall() {
        return this.mCT.isInEmergencyCall();
    }

    @Override
    public boolean isInEcm() {
        return this.mIsPhoneInEcmState;
    }

    private void sendEmergencyCallbackModeChange() {
        Intent intent = new Intent("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intent.putExtra("phoneinECMState", this.mIsPhoneInEcmState);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
        Rlog.d(LOG_TAG, "sendEmergencyCallbackModeChange");
    }

    @Override
    public void exitEmergencyCallbackMode() {
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        Rlog.d(LOG_TAG, "exitEmergencyCallbackMode()");
        try {
            ImsEcbm ecbm = this.mCT.getEcbmInterface();
            ecbm.exitEmergencyCallbackMode();
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

    private void handleEnterEmergencyCallbackMode() {
        Rlog.d(LOG_TAG, "handleEnterEmergencyCallbackMode,mIsPhoneInEcmState= " + this.mIsPhoneInEcmState);
        if (this.mIsPhoneInEcmState) {
            return;
        }
        this.mIsPhoneInEcmState = true;
        sendEmergencyCallbackModeChange();
        TelephonyManager.setTelephonyProperty(this.mPhoneId, "ril.cdma.inecmmode", "true");
        long delayInMillis = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
        postDelayed(this.mExitEcmRunnable, delayInMillis);
        this.mWakeLock.acquire();
    }

    private void handleExitEmergencyCallbackMode() {
        Rlog.d(LOG_TAG, "handleExitEmergencyCallbackMode: mIsPhoneInEcmState = " + this.mIsPhoneInEcmState);
        removeCallbacks(this.mExitEcmRunnable);
        if (this.mEcmExitRespRegistrant != null) {
            this.mEcmExitRespRegistrant.notifyResult(Boolean.TRUE);
        }
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (this.mIsPhoneInEcmState) {
            this.mIsPhoneInEcmState = false;
            TelephonyManager.setTelephonyProperty(this.mPhoneId, "ril.cdma.inecmmode", "false");
        }
        sendEmergencyCallbackModeChange();
    }

    void handleTimerInEmergencyCallbackMode(int action) {
        switch (action) {
            case 0:
                long delayInMillis = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
                postDelayed(this.mExitEcmRunnable, delayInMillis);
                ((GsmCdmaPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.FALSE);
                break;
            case 1:
                removeCallbacks(this.mExitEcmRunnable);
                ((GsmCdmaPhone) this.mDefaultPhone).notifyEcbmTimerReset(Boolean.TRUE);
                break;
            default:
                Rlog.e(LOG_TAG, "handleTimerInEmergencyCallbackMode, unsupported action " + action);
                break;
        }
    }

    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        this.mEcmExitRespRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
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
    public Phone getDefaultPhone() {
        return this.mDefaultPhone;
    }

    @Override
    public boolean isImsRegistered() {
        return this.mImsRegistered;
    }

    public void setImsRegistered(boolean value) {
        this.mImsRegistered = value;
        if (!this.mImsRegistered) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        notificationManager.cancel("wifi_calling", 1);
    }

    @Override
    public void callEndCleanupHandOverCallIfAny() {
        this.mCT.callEndCleanupHandOverCallIfAny();
    }

    public void processDisconnectReason(ImsReasonInfo imsReasonInfo) {
        if (imsReasonInfo.mCode != 1000 || imsReasonInfo.mExtraMessage == null) {
            return;
        }
        CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configManager == null) {
            Rlog.e(LOG_TAG, "processDisconnectReason: CarrierConfigManager is not ready");
            return;
        }
        PersistableBundle pb = configManager.getConfigForSubId(getSubId());
        if (pb == null) {
            Rlog.e(LOG_TAG, "processDisconnectReason: no config for subId " + getSubId());
            return;
        }
        String[] wfcOperatorErrorCodes = pb.getStringArray("wfc_operator_error_codes_string_array");
        if (wfcOperatorErrorCodes == null) {
            return;
        }
        String[] wfcOperatorErrorAlertMessages = this.mContext.getResources().getStringArray(R.array.config_displayCutoutApproximationRectArray);
        String[] wfcOperatorErrorNotificationMessages = this.mContext.getResources().getStringArray(R.array.config_displayCutoutPathArray);
        for (int i = 0; i < wfcOperatorErrorCodes.length; i++) {
            String[] codes = wfcOperatorErrorCodes[i].split("\\|");
            if (codes.length != 2) {
                Rlog.e(LOG_TAG, "Invalid carrier config: " + wfcOperatorErrorCodes[i]);
            } else if (imsReasonInfo.mExtraMessage.startsWith(codes[0])) {
                int codeStringLength = codes[0].length();
                char lastChar = codes[0].charAt(codeStringLength - 1);
                if (Character.isLetterOrDigit(lastChar) && imsReasonInfo.mExtraMessage.length() > codeStringLength) {
                    char nextChar = imsReasonInfo.mExtraMessage.charAt(codeStringLength);
                    if (Character.isLetterOrDigit(nextChar)) {
                        continue;
                    }
                } else {
                    CharSequence title = this.mContext.getText(R.string.accessibility_autoclick_scroll_up);
                    int idx = Integer.parseInt(codes[1]);
                    if (idx < 0 || idx >= wfcOperatorErrorAlertMessages.length || idx >= wfcOperatorErrorNotificationMessages.length) {
                        Rlog.e(LOG_TAG, "Invalid index: " + wfcOperatorErrorCodes[i]);
                    } else {
                        String str = imsReasonInfo.mExtraMessage;
                        String str2 = imsReasonInfo.mExtraMessage;
                        if (!wfcOperatorErrorAlertMessages[idx].isEmpty()) {
                            str = wfcOperatorErrorAlertMessages[idx];
                        }
                        if (!wfcOperatorErrorNotificationMessages[idx].isEmpty()) {
                            str2 = wfcOperatorErrorNotificationMessages[idx];
                        }
                        Intent intent = new Intent("com.android.ims.REGISTRATION_ERROR");
                        intent.putExtra(Phone.EXTRA_KEY_ALERT_TITLE, title);
                        intent.putExtra(Phone.EXTRA_KEY_ALERT_MESSAGE, (CharSequence) str);
                        intent.putExtra("notificationMessage", (CharSequence) str2);
                        this.mContext.sendOrderedBroadcast(intent, null, this.mResultReceiver, null, -1, null, null);
                        return;
                    }
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
    public void sendEmergencyCallStateChange(boolean callActive) {
        this.mDefaultPhone.sendEmergencyCallStateChange(callActive);
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean broadcast) {
        this.mDefaultPhone.setBroadcastEmergencyCallStateChanges(broadcast);
    }

    public PowerManager.WakeLock getWakeLock() {
        return this.mWakeLock;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ImsPhone extends:");
        super.dump(fd, pw, args);
        pw.flush();
        pw.println("ImsPhone:");
        pw.println("  mDefaultPhone = " + this.mDefaultPhone);
        pw.println("  mPendingMMIs = " + this.mPendingMMIs);
        pw.println("  mPostDialHandler = " + this.mPostDialHandler);
        pw.println("  mSS = " + this.mSS);
        pw.println("  mWakeLock = " + this.mWakeLock);
        pw.println("  mIsPhoneInEcmState = " + this.mIsPhoneInEcmState);
        pw.println("  mEcmExitRespRegistrant = " + this.mEcmExitRespRegistrant);
        pw.println("  mSilentRedialRegistrants = " + this.mSilentRedialRegistrants);
        pw.println("  mImsRegistered = " + this.mImsRegistered);
        pw.println("  mSsnRegistrants = " + this.mSsnRegistrants);
        pw.flush();
    }

    @Override
    public Connection dial(List<String> numbers, int videoState) throws CallStateException {
        return this.mCT.dial(numbers, videoState);
    }

    @Override
    public void hangupAll() throws CallStateException {
        Rlog.d(LOG_TAG, "hangupAll");
        this.mCT.hangupAll();
    }

    @Override
    public boolean isFeatureSupported(Phone.FeatureType feature) {
        if (feature != Phone.FeatureType.VOLTE_ENHANCED_CONFERENCE && feature != Phone.FeatureType.VIDEO_RESTRICTION) {
            return feature == Phone.FeatureType.VOLTE_CONF_REMOVE_MEMBER;
        }
        List<String> voLteEnhancedConfMccMncList = Arrays.asList("46000", "46002", "46004", "46007", "46008");
        IccRecords iccRecords = this.mDefaultPhone.getIccRecords();
        if (iccRecords == null) {
            Rlog.d(LOG_TAG, "isFeatureSupported(" + feature + ") no iccRecords");
            return false;
        }
        String mccMnc = iccRecords.getOperatorNumeric();
        boolean ret = voLteEnhancedConfMccMncList.contains(mccMnc);
        Rlog.d(LOG_TAG, "isFeatureSupported(" + feature + "): ret = " + ret + " current mccMnc = " + mccMnc);
        return ret;
    }
}
