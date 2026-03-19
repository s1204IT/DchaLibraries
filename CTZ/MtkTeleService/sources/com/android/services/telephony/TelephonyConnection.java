package com.android.services.telephony;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telecom.CallAudioState;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import android.telecom.VideoProfile;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Pair;
import com.android.ims.ImsCall;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.ImsUtil;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneConnection;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.services.telephony.MtkGsmCdmaConnection;
import com.mediatek.services.telephony.MtkTelephonyConnectionServiceUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import mediatek.telecom.MtkConnection;

public abstract class TelephonyConnection extends Connection implements Holdable {
    private static final int EVENT_CDMA_CALL_ACCEPTED = 1001;
    private static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 1000;
    protected static final int MSG_CDMA_VOICE_PRIVACY_OFF = 16;
    protected static final int MSG_CDMA_VOICE_PRIVACY_ON = 15;
    protected static final int MSG_CONFERENCE_MERGE_FAILED = 6;
    protected static final int MSG_CONNECTION_EXTRAS_CHANGED = 12;
    protected static final int MSG_DISCONNECT = 4;
    protected static final int MSG_HANDOVER_STATE_CHANGED = 3;
    protected static final int MSG_HANGUP = 17;
    protected static final int MSG_MULTIPARTY_STATE_CHANGED = 5;
    protected static final int MSG_ON_HOLD_TONE = 14;
    protected static final int MSG_PRECISE_CALL_STATE_CHANGED = 1;
    protected static final int MSG_RINGBACK_TONE = 2;
    protected static final int MSG_SET_AUDIO_QUALITY = 10;
    protected static final int MSG_SET_CONFERENCE_PARTICIPANTS = 11;
    protected static final int MSG_SET_ORIGNAL_CONNECTION_CAPABILITIES = 13;
    protected static final int MSG_SET_VIDEO_PROVIDER = 9;
    protected static final int MSG_SET_VIDEO_STATE = 8;
    protected static final int MSG_SUPP_SERVICE_NOTIFY = 7;
    private static final int MTK_EVENT_BASE = 1000;
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private static final String TAG = "TelephonyConn";
    private static final boolean TELDBG;
    private static final Map<String, String> sExtrasMap;
    private PhoneAccountHandle mAccountHandle;
    private boolean mHasHighDefAudio;
    private boolean mIsCarrierVideoConferencingSupported;
    private boolean mIsCdmaVoicePrivacyEnabled;
    private boolean mIsConferenceSupported;
    private boolean mIsHoldable;
    private boolean mIsManageImsConferenceCallSupported;
    protected final boolean mIsOutgoing;
    private boolean mIsUsingAssistedDialing;
    private boolean mIsVideoPauseSupported;
    private boolean mIsWifi;
    protected com.android.internal.telephony.Connection mOriginalConnection;
    private int mOriginalConnectionCapabilities;
    private boolean mShowPreciseFailedCause;
    private boolean mTreatAsEmergencyCall;
    private boolean mWasImsConnection;
    private static final boolean SENLOG = TextUtils.equals(Build.TYPE, "user");
    private static final boolean SDBG = !TextUtils.equals(Build.TYPE, "user");
    protected boolean mDtmfRequestIsStarted = false;
    private boolean mIsLocallyDisconnecting = false;
    private SrvccPendingAction mPendingAction = SrvccPendingAction.SRVCC_PENDING_NONE;
    protected final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            int disconnectCause;
            int i = message.what;
            switch (i) {
                case 1:
                    Log.v(TelephonyConnection.this, "MSG_PRECISE_CALL_STATE_CHANGED", new Object[0]);
                    TelephonyConnection.this.updateState();
                    break;
                case 2:
                    Log.v(TelephonyConnection.this, "MSG_RINGBACK_TONE", new Object[0]);
                    if (TelephonyConnection.this.getOriginalConnection() != TelephonyConnection.this.getForegroundConnection()) {
                        Log.v(TelephonyConnection.this, "handleMessage, original connection is not foreground connection, skipping", new Object[0]);
                    } else {
                        TelephonyConnection.this.setRingbackRequested(((Boolean) ((AsyncResult) message.obj).result).booleanValue());
                    }
                    break;
                case 3:
                    Log.v(TelephonyConnection.this, "MSG_HANDOVER_STATE_CHANGED", new Object[0]);
                    com.android.internal.telephony.Connection connection = (com.android.internal.telephony.Connection) ((AsyncResult) message.obj).result;
                    if (TelephonyConnection.this.mOriginalConnection != null) {
                        if (connection != null) {
                            if ((connection.getAddress() != null && TelephonyConnection.this.mOriginalConnection.getAddress() != null && TelephonyConnection.this.mOriginalConnection.getAddress().contains(connection.getAddress())) || connection.getState() == TelephonyConnection.this.mOriginalConnection.getStateBeforeHandover()) {
                                Log.d(TelephonyConnection.this, "SettingOriginalConnection " + TelephonyConnection.this.mOriginalConnection.toString() + " with " + connection.toString(), new Object[0]);
                                TelephonyConnection.this.removePropertyVoLte();
                                if (TelephonyConnection.this.mHandler.hasMessages(17)) {
                                    Log.i(TelephonyConnection.this, "MSG_HANGUP not handled in SRVCC", new Object[0]);
                                    TelephonyConnection.this.mPendingAction = SrvccPendingAction.SRVCC_PENDING_HANGUP_CALL;
                                }
                                TelephonyConnection.this.setOriginalConnection(connection);
                                TelephonyConnection.this.mWasImsConnection = false;
                                TelephonyConnection.this.trySrvccPendingAction();
                                break;
                            }
                        }
                    } else {
                        Log.w(TelephonyConnection.this, "MSG_HANDOVER_STATE_CHANGED: mOriginalConnection==null - invalid state (not cleaned up)", new Object[0]);
                        break;
                    }
                    break;
                case 4:
                    if (TelephonyConnection.this.mOriginalConnection != null) {
                        disconnectCause = TelephonyConnection.this.mOriginalConnection.getDisconnectCause();
                    } else {
                        disconnectCause = 0;
                    }
                    Log.i(this, "Receives MSG_DISCONNECT, cause=" + disconnectCause, new Object[0]);
                    if (TelephonyConnection.this.mOriginalConnection == null || disconnectCause != 380) {
                        if (!TelephonyConnection.this.mIsLocallyDisconnecting && disconnectCause != 0 && disconnectCause != 14 && disconnectCause != 2 && disconnectCause != 3) {
                            Log.i(this, "ECC retry: check whether need to retry, connectionState=" + TelephonyConnection.this.mConnectionState, new Object[0]);
                            boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(TelephonyConnection.this.mOriginalConnection.getAddress());
                            if (TelephonyConnection.this.mTreatAsEmergencyCall && MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() && ((TelephonyConnection.this.mConnectionState.isDialing() || TelephonyConnection.this.mConnectionState == Call.State.IDLE) && !MtkTelephonyConnectionServiceUtil.getInstance().eccRetryTimeout() && zIsEmergencyNumber)) {
                                Log.i(this, "ECC retry: meet retry condition, state=" + TelephonyConnection.this.mConnectionState, new Object[0]);
                                TelephonyConnection.this.close();
                                MtkTelephonyConnectionServiceUtil.getInstance().performEccRetry();
                            }
                        }
                    } else {
                        try {
                            TelephonyConnection.this.setOriginalConnection(TelephonyConnection.this.getPhone().dial(TelephonyConnection.this.mOriginalConnection.getOrigDialString(), new PhoneInternalInterface.DialArgs.Builder().build()));
                            TelephonyConnection.this.notifyEcc();
                        } catch (CallStateException e) {
                            Log.e((Object) TelephonyConnection.this, (Throwable) e, "Fail to redial as ECC", new Object[0]);
                        }
                    }
                    TelephonyConnection.this.updateState();
                    break;
                case 5:
                    boolean zBooleanValue = ((Boolean) message.obj).booleanValue();
                    Object[] objArr = new Object[1];
                    objArr[0] = zBooleanValue ? "Y" : "N";
                    Log.i(this, "Update multiparty state to %s", objArr);
                    TelephonyConnection.this.mIsMultiParty = zBooleanValue;
                    if (zBooleanValue) {
                        TelephonyConnection.this.notifyConferenceStarted();
                    }
                    break;
                case 6:
                    TelephonyConnection.this.notifyConferenceMergeFailed();
                    break;
                case 7:
                    Phone phone = TelephonyConnection.this.getPhone();
                    TelephonyConnection telephonyConnection = TelephonyConnection.this;
                    StringBuilder sb = new StringBuilder();
                    sb.append("MSG_SUPP_SERVICE_NOTIFY on phoneId : ");
                    sb.append(phone != null ? Integer.toString(phone.getPhoneId()) : "null");
                    Log.v(telephonyConnection, sb.toString(), new Object[0]);
                    if (message.obj != null && ((AsyncResult) message.obj).result != null) {
                        SuppServiceNotification suppServiceNotification = (SuppServiceNotification) ((AsyncResult) message.obj).result;
                        if (TelephonyConnection.this.mOriginalConnection != null) {
                            TelephonyConnection.this.handleSuppServiceNotification(suppServiceNotification);
                        }
                        break;
                    }
                    break;
                case 8:
                    int iIntValue = ((Integer) message.obj).intValue();
                    String str = SystemProperties.get("persist.vendor.operator.optr");
                    Log.i(this, "operator: " + str + " mWasImsConnection: " + TelephonyConnection.this.mWasImsConnection, new Object[0]);
                    if (TelephonyConnection.this.mWasImsConnection && str != null && str.equals("OP01") && iIntValue == 0 && TelephonyConnection.this.getVideoState() != 0) {
                        TelephonyConnection.this.getPhone().getContext();
                        Log.d(this, "Video call change to vocie call", new Object[0]);
                    }
                    TelephonyConnection.this.setVideoState(iIntValue);
                    TelephonyConnection.this.refreshConferenceSupported();
                    TelephonyConnection.this.refreshDisableAddCall();
                    TelephonyConnection.this.updateConnectionProperties();
                    break;
                case 9:
                    TelephonyConnection.this.setVideoProvider((Connection.VideoProvider) message.obj);
                    break;
                case 10:
                    TelephonyConnection.this.setAudioQuality(((Integer) message.obj).intValue());
                    break;
                case 11:
                    TelephonyConnection.this.updateConferenceParticipants((List) message.obj);
                    break;
                case 12:
                    TelephonyConnection.this.updateExtras((Bundle) message.obj);
                    break;
                case 13:
                    TelephonyConnection.this.setOriginalConnectionCapabilities(message.arg1);
                    break;
                case 14:
                    Pair pair = (Pair) ((AsyncResult) message.obj).result;
                    boolean zBooleanValue2 = ((Boolean) pair.second).booleanValue();
                    if (((com.android.internal.telephony.Connection) pair.first) == TelephonyConnection.this.mOriginalConnection) {
                        if (zBooleanValue2) {
                            TelephonyConnection.this.sendConnectionEvent("android.telecom.event.ON_HOLD_TONE_START", null);
                        } else {
                            TelephonyConnection.this.sendConnectionEvent("android.telecom.event.ON_HOLD_TONE_END", null);
                        }
                    }
                    break;
                case 15:
                    Log.d(this, "MSG_CDMA_VOICE_PRIVACY_ON received", new Object[0]);
                    TelephonyConnection.this.setCdmaVoicePrivacy(true);
                    break;
                case 16:
                    Log.d(this, "MSG_CDMA_VOICE_PRIVACY_OFF received", new Object[0]);
                    TelephonyConnection.this.setCdmaVoicePrivacy(false);
                    break;
                case 17:
                    TelephonyConnection.this.hangup(((Integer) message.obj).intValue());
                    break;
                default:
                    switch (i) {
                        case TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR:
                            TelephonyConnection.this.notifyConnectionLost();
                            TelephonyConnection.this.onLocalDisconnected();
                            break;
                        case TelephonyConnection.EVENT_CDMA_CALL_ACCEPTED:
                            Log.i(this, "Receives EVENT_CDMA_CALL_ACCEPTED", new Object[0]);
                            TelephonyConnection.this.updateConnectionCapabilities();
                            TelephonyConnection.this.fireOnCdmaCallAccepted();
                            break;
                    }
                    break;
            }
        }
    };
    private final Connection.PostDialListener mPostDialListener = new Connection.PostDialListener() {
        public void onPostDialWait() {
            Log.v(TelephonyConnection.this, "onPostDialWait", new Object[0]);
            if (TelephonyConnection.this.mOriginalConnection != null) {
                TelephonyConnection.this.setPostDialWait(TelephonyConnection.this.mOriginalConnection.getRemainingPostDialString());
            }
        }

        public void onPostDialChar(char c) {
            Log.v(TelephonyConnection.this, "onPostDialChar: %s", Character.valueOf(c));
            if (TelephonyConnection.this.mOriginalConnection != null) {
                TelephonyConnection.this.setNextPostDialChar(c);
            }
        }
    };
    protected Connection.Listener mOriginalConnectionListener = new MtkImsPhoneConnection.MtkListenerBase() {
        public void onVideoStateChanged(int i) {
            TelephonyConnection.this.mHandler.obtainMessage(8, Integer.valueOf(i)).sendToTarget();
        }

        public void onConnectionCapabilitiesChanged(int i) {
            TelephonyConnection.this.mHandler.obtainMessage(13, i, 0).sendToTarget();
        }

        public void onVideoProviderChanged(Connection.VideoProvider videoProvider) {
            TelephonyConnection.this.mHandler.obtainMessage(9, videoProvider).sendToTarget();
        }

        public void onWifiChanged(boolean z) {
            TelephonyConnection.this.setWifi(z);
        }

        public void onAudioQualityChanged(int i) {
            TelephonyConnection.this.mHandler.obtainMessage(10, Integer.valueOf(i)).sendToTarget();
        }

        public void onConferenceParticipantsChanged(List<ConferenceParticipant> list) {
            TelephonyConnection.this.mHandler.obtainMessage(11, list).sendToTarget();
        }

        public void onMultipartyStateChanged(boolean z) {
            TelephonyConnection.this.handleMultipartyStateChange(z);
        }

        public void onConferenceMergedFailed() {
            TelephonyConnection.this.handleConferenceMergeFailed();
        }

        public void onExtrasChanged(Bundle bundle) {
            TelephonyConnection.this.mHandler.obtainMessage(12, bundle).sendToTarget();
        }

        public void onExitedEcmMode() {
            TelephonyConnection.this.handleExitedEcmMode();
        }

        public void onCallPullFailed(com.android.internal.telephony.Connection connection) {
            if (connection == null) {
                return;
            }
            Log.i(this, "onCallPullFailed - pull failed; swapping back to call: %s", connection);
            TelephonyConnection.this.sendConnectionEvent("android.telecom.event.CALL_PULL_FAILED", null);
            TelephonyConnection.this.setOriginalConnection(connection);
            TelephonyConnection.this.setActiveInternal();
        }

        public void onHandoverToWifiFailed() {
            TelephonyConnection.this.sendConnectionEvent("android.telephony.event.EVENT_HANDOVER_TO_WIFI_FAILED", null);
        }

        public void onConnectionEvent(String str, Bundle bundle) {
            TelephonyConnection.this.sendConnectionEvent(str, bundle);
        }

        public void onRttModifyRequestReceived() {
            TelephonyConnection.this.sendRemoteRttRequest();
        }

        public void onRttModifyResponseReceived(int i) {
            TelephonyConnection.this.updateConnectionProperties();
            if (i == 1) {
                TelephonyConnection.this.sendRttInitiationSuccess();
            } else {
                TelephonyConnection.this.sendRttInitiationFailure(i);
            }
        }

        public void onDisconnect(int i) {
            Log.i(this, "onDisconnect: callId=%s, cause=%s", TelephonyConnection.this.getTelecomCallId(), DisconnectCause.toString(i));
            TelephonyConnection.this.mHandler.obtainMessage(4).sendToTarget();
        }

        public void onRttInitiated() {
            TelephonyConnection.this.updateConnectionProperties();
            TelephonyConnection.this.sendRttInitiationSuccess();
        }

        public void onRttTerminated() {
            TelephonyConnection.this.updateConnectionProperties();
            TelephonyConnection.this.sendRttSessionRemotelyTerminated();
        }

        public void onDeviceSwitched(boolean z) {
            TelephonyConnection.this.sendConnectionEvent(z ? "mediatek.telecom.event.EVENT_DEVICE_SWITCH_SUCCESS" : "mediatek.telecom.event.EVENT_DEVICE_SWITCH_FAILED", null);
        }

        public void onConferenceParticipantsInvited(boolean z) {
            TelephonyConnection.this.notifyConferenceParticipantsInvited(z);
        }

        public void onConferenceConnectionsConfigured(ArrayList<com.android.internal.telephony.Connection> arrayList) {
            TelephonyConnection.this.notifyConferenceConnectionsConfigured(arrayList);
        }
    };
    private Call.State mConnectionState = Call.State.IDLE;
    private Bundle mOriginalConnectionExtras = new Bundle();
    private boolean mIsStateOverridden = false;
    private Call.State mOriginalConnectionState = Call.State.IDLE;
    private Call.State mConnectionOverriddenState = Call.State.IDLE;
    private Connection.RttTextStream mRttTextStream = null;
    private boolean mIsMultiParty = false;
    private final Set<TelephonyConnectionListener> mTelephonyListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));

    private enum SrvccPendingAction {
        SRVCC_PENDING_NONE,
        SRVCC_PENDING_ANSWER_CALL,
        SRVCC_PENDING_HOLD_CALL,
        SRVCC_PENDING_UNHOLD_CALL,
        SRVCC_PENDING_HANGUP_CALL
    }

    public abstract TelephonyConnection cloneConnection();

    static {
        TELDBG = SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
        sExtrasMap = createExtrasMap();
    }

    private void handleSuppServiceNotification(SuppServiceNotification suppServiceNotification) {
        Log.i(this, "handleSuppServiceNotification: type=%d, code=%d", Integer.valueOf(suppServiceNotification.notificationType), Integer.valueOf(suppServiceNotification.code));
        if (suppServiceNotification.notificationType == 0 && suppServiceNotification.code == 2) {
            sendConnectionEvent("android.telephony.event.EVENT_CALL_FORWARDED", null);
        }
        sendSuppServiceNotificationEvent(suppServiceNotification.notificationType, suppServiceNotification.code);
    }

    private void sendSuppServiceNotificationEvent(int i, int i2) {
        Bundle bundle = new Bundle();
        bundle.putInt("android.telephony.extra.NOTIFICATION_TYPE", i);
        bundle.putInt("android.telephony.extra.NOTIFICATION_CODE", i2);
        bundle.putCharSequence("android.telephony.extra.NOTIFICATION_MESSAGE", getSuppServiceMessage(i, i2));
        sendConnectionEvent("android.telephony.event.EVENT_SUPPLEMENTARY_SERVICE_NOTIFICATION", bundle);
    }

    private CharSequence getSuppServiceMessage(int i, int i2) {
        int i3 = R.string.supp_service_closed_user_group_call;
        if (i == 0) {
            switch (i2) {
                case 0:
                case 1:
                    i3 = R.string.supp_service_call_forwarding_active;
                    break;
                case 2:
                    i3 = R.string.supp_service_notification_call_forwarded;
                    break;
                case 3:
                    i3 = R.string.supp_service_notification_call_waiting;
                    break;
                case 4:
                    break;
                case 5:
                    i3 = R.string.supp_service_outgoing_calls_barred;
                    break;
                case 6:
                    i3 = R.string.supp_service_incoming_calls_barred;
                    break;
                case 7:
                    i3 = R.string.supp_service_clir_suppression_rejected;
                    break;
                case 8:
                    i3 = R.string.supp_service_notification_call_deflected;
                    break;
                default:
                    i3 = -1;
                    break;
            }
        } else if (i == 1) {
            switch (i2) {
                case 0:
                    i3 = R.string.supp_service_forwarded_call;
                    break;
                case 1:
                    break;
                case 2:
                    i3 = R.string.supp_service_call_on_hold;
                    break;
                case 3:
                    i3 = R.string.supp_service_call_resumed;
                    break;
                case 4:
                    i3 = R.string.supp_service_conference_call;
                    break;
                case 5:
                    i3 = R.string.supp_service_held_call_released;
                    break;
                case 6:
                default:
                    i3 = -1;
                    break;
                case 7:
                    i3 = R.string.supp_service_additional_ect_connecting;
                    break;
                case 8:
                    i3 = R.string.supp_service_additional_ect_connected;
                    break;
                case 9:
                    i3 = R.string.supp_service_deflected_call;
                    break;
                case 10:
                    i3 = R.string.supp_service_additional_call_forwarded;
                    break;
            }
        }
        if (i3 != -1 && getPhone() != null && getPhone().getContext() != null) {
            return getPhone().getContext().getText(i3);
        }
        return null;
    }

    public boolean isCarrierVideoConferencingSupported() {
        return this.mIsCarrierVideoConferencingSupported;
    }

    public static abstract class TelephonyConnectionListener {
        public void onOriginalConnectionConfigured(TelephonyConnection telephonyConnection) {
        }

        public void onOriginalConnectionRetry(TelephonyConnection telephonyConnection, boolean z) {
        }

        public void onDeviceSwitched(boolean z) {
        }

        public void onConferenceParticipantsInvited(boolean z) {
        }

        public void onConferenceConnectionsConfigured(ArrayList<com.android.internal.telephony.Connection> arrayList) {
        }
    }

    protected TelephonyConnection(com.android.internal.telephony.Connection connection, String str, boolean z) {
        this.mIsOutgoing = z;
        setTelecomCallId(str);
        if (connection != null) {
            setOriginalConnection(connection);
        }
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState callAudioState) {
        if (getPhone() != null) {
            getPhone().setEchoSuppressionEnabled();
        }
    }

    @Override
    public void onStateChanged(int i) {
        Log.v(this, "onStateChanged, state: " + android.telecom.Connection.stateToString(i), new Object[0]);
        updateStatusHints();
    }

    @Override
    public void onDisconnect() {
        Log.v(this, "onDisconnect", new Object[0]);
        this.mHandler.obtainMessage(17, 3).sendToTarget();
    }

    public void onDisconnectConferenceParticipant(Uri uri) {
        Log.v(this, "onDisconnectConferenceParticipant %s", uri);
        if (this.mOriginalConnection == null) {
            return;
        }
        this.mOriginalConnection.onDisconnectConferenceParticipant(uri);
    }

    @Override
    public void onSeparate() {
        Log.v(this, "onSeparate", new Object[0]);
        if (this.mOriginalConnection != null) {
            try {
                this.mOriginalConnection.separate();
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Call to Connection.separate failed with exception", new Object[0]);
            }
        }
    }

    @Override
    public void onAbort() {
        Log.v(this, "onAbort", new Object[0]);
        this.mHandler.obtainMessage(17, 3).sendToTarget();
    }

    @Override
    public void onHold() {
        performHold();
    }

    @Override
    public void onUnhold() {
        performUnhold();
    }

    @Override
    public void onAnswer(int i) {
        Log.v(this, "onAnswer", new Object[0]);
        if (isValidRingingCall() && getPhone() != null) {
            try {
                getPhone().acceptCall(i);
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Failed to accept call.", new Object[0]);
                if (e.getError() == 3) {
                    this.mPendingAction = SrvccPendingAction.SRVCC_PENDING_ANSWER_CALL;
                }
            }
        }
    }

    @Override
    public void onDeflect(Uri uri) {
        Log.v(this, "onDeflect", new Object[0]);
        if (this.mOriginalConnection != null && isValidRingingCall()) {
            if (uri == null) {
                Log.w(this, "call deflect address uri is null", new Object[0]);
                return;
            }
            String scheme = uri.getScheme();
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            if (!"voicemail".equals(scheme)) {
                if (!"tel".equals(scheme)) {
                    Log.w(this, "onDeflect, address scheme is not of type tel instead: " + scheme, new Object[0]);
                    return;
                }
                if (PhoneNumberUtils.isUriNumber(schemeSpecificPart)) {
                    Log.w(this, "Invalid deflect address. Not a legal PSTN number.", new Object[0]);
                    return;
                }
                String strConvertAndStrip = PhoneNumberUtils.convertAndStrip(schemeSpecificPart);
                if (TextUtils.isEmpty(strConvertAndStrip)) {
                    Log.w(this, "Empty deflect number obtained from address uri", new Object[0]);
                    return;
                }
                try {
                    this.mOriginalConnection.deflect(strConvertAndStrip);
                    return;
                } catch (CallStateException e) {
                    Log.e((Object) this, (Throwable) e, "Failed to deflect call.", new Object[0]);
                    return;
                }
            }
            Log.w(this, "Cannot deflect to voicemail uri", new Object[0]);
        }
    }

    @Override
    public void onReject() {
        Log.v(this, "onReject", new Object[0]);
        if (isValidRingingCall()) {
            this.mHandler.obtainMessage(17, 16).sendToTarget();
        }
        super.onReject();
    }

    @Override
    public void onPostDialContinue(boolean z) {
        Log.v(this, "onPostDialContinue, proceed: " + z, new Object[0]);
        if (this.mOriginalConnection != null) {
            if (z) {
                this.mOriginalConnection.proceedAfterWaitChar();
            } else {
                this.mOriginalConnection.cancelPostDial();
            }
        }
    }

    @Override
    public void onPullExternalCall() {
        if ((getConnectionProperties() & 16) != 16) {
            Log.w(this, "onPullExternalCall - cannot pull non-external call", new Object[0]);
        } else if (this.mOriginalConnection != null) {
            this.mOriginalConnection.pullExternalCall();
        }
    }

    @Override
    public void onStartRtt(Connection.RttTextStream rttTextStream) {
        if (isImsConnection()) {
            ImsPhoneConnection imsPhoneConnection = this.mOriginalConnection;
            if (imsPhoneConnection.isRttEnabledForCall()) {
                imsPhoneConnection.setCurrentRttTextStream(rttTextStream);
                return;
            } else {
                imsPhoneConnection.sendRttModifyRequest(rttTextStream);
                return;
            }
        }
        Log.w(this, "onStartRtt - not in IMS, so RTT cannot be enabled.", new Object[0]);
    }

    @Override
    public void onStopRtt() {
        Log.i(this, "MTK support RTT downgrade.", new Object[0]);
        ExtensionManager.getRttUtilExt().onStopRtt(isImsConnection(), this.mOriginalConnection);
    }

    @Override
    public void handleRttUpgradeResponse(Connection.RttTextStream rttTextStream) {
        if (!isImsConnection()) {
            Log.w(this, "handleRttUpgradeResponse - not in IMS, so RTT cannot be enabled.", new Object[0]);
        } else {
            this.mOriginalConnection.sendRttModifyResponse(rttTextStream);
        }
    }

    public void performHold() {
        Log.v(this, "performHold", new Object[0]);
        if (Call.State.ACTIVE == this.mConnectionState) {
            Log.v(this, "Holding active call", new Object[0]);
            try {
                Phone phone = this.mOriginalConnection.getCall().getPhone();
                if (phone.getRingingCall().getState() != Call.State.WAITING) {
                    phone.switchHoldingAndActive();
                    return;
                }
                return;
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Exception occurred while trying to put call on hold.", new Object[0]);
                if (e.getError() == 3) {
                    this.mPendingAction = SrvccPendingAction.SRVCC_PENDING_HOLD_CALL;
                    return;
                }
                return;
            }
        }
        Log.w(this, "Cannot put a call that is not currently active on hold.", new Object[0]);
    }

    public void performUnhold() {
        Log.v(this, "performUnhold", new Object[0]);
        if (Call.State.HOLDING == this.mConnectionState) {
            try {
                if (!hasMultipleTopLevelCalls()) {
                    this.mOriginalConnection.getCall().getPhone().switchHoldingAndActive();
                } else if (!hasTwoHoldingCall()) {
                    Log.i(this, "Skipping unhold command for %s", this);
                }
                return;
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Exception occurred while trying to release call from hold.", new Object[0]);
                if (e.getError() == 3) {
                    this.mPendingAction = SrvccPendingAction.SRVCC_PENDING_UNHOLD_CALL;
                    return;
                }
                return;
            }
        }
        Log.w(this, "Cannot release a call that is not already on hold from hold.", new Object[0]);
    }

    public void performConference(android.telecom.Connection connection) {
        Log.d(this, "performConference - %s", this);
        if (getPhone() != null) {
            try {
                getPhone().conference();
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Failed to conference call.", new Object[0]);
            }
        }
    }

    protected int buildConnectionCapabilities() {
        int i;
        if (this.mOriginalConnection != null && this.mOriginalConnection.isIncoming()) {
            i = 262144;
        } else {
            i = 0;
        }
        if (!shouldTreatAsEmergencyCall() && isImsConnection() && canHoldImsCalls()) {
            i |= 2;
            if (this.mIsHoldable && (getState() == 4 || getState() == 5)) {
                i |= 1;
            }
        }
        Log.d(this, "buildConnectionCapabilities: isHoldable = " + this.mIsHoldable + " State = " + getState() + " capabilities = " + i, new Object[0]);
        return i;
    }

    protected final void updateConnectionCapabilities() {
        boolean z = false;
        int iApplyConferenceTerminationCapabilities = applyConferenceTerminationCapabilities(changeBitmask(changeBitmask(applyOriginalConnectionCapabilities(buildConnectionCapabilities()), 1048576, this.mIsVideoPauseSupported && isVideoCapable()), 16777216, isExternalConnection() && isPullable()));
        if (isImsConnection() && canDeflectImsCalls()) {
            z = true;
        }
        int iApplyVideoRingtoneCapabilities = applyVideoRingtoneCapabilities(this.mOriginalConnectionCapabilities, changeBitmask(iApplyConferenceTerminationCapabilities, 33554432, z));
        if (getConnectionCapabilities() != iApplyVideoRingtoneCapabilities) {
            setConnectionCapabilities(iApplyVideoRingtoneCapabilities);
        }
    }

    protected int buildConnectionProperties() {
        Phone phone = getPhone();
        if (phone != null && phone.isInEcm()) {
            return 1;
        }
        return 0;
    }

    protected final void updateConnectionProperties() {
        int iUpdatePropertyVoLte = updatePropertyVoLte(changeBitmask(changeBitmask(changeBitmask(changeBitmask(changeBitmask(changeBitmask(buildConnectionProperties(), 4, hasHighDefAudioProperty()), 8, this.mIsWifi), 16, isExternalConnection()), 32, this.mIsCdmaVoicePrivacyEnabled), 512, this.mIsUsingAssistedDialing), 256, isRtt()));
        if (getConnectionProperties() != iUpdatePropertyVoLte) {
            setConnectionProperties(iUpdatePropertyVoLte);
        }
    }

    protected final void updateAddress() {
        updateConnectionCapabilities();
        updateConnectionProperties();
        if (this.mOriginalConnection != null) {
            Uri addressFromNumber = getAddressFromNumber(this.mOriginalConnection.getAddress());
            int numberPresentation = this.mOriginalConnection.getNumberPresentation();
            if (!Objects.equals(addressFromNumber, getAddress()) || numberPresentation != getAddressPresentation()) {
                Log.v(this, "updateAddress, address changed", new Object[0]);
                if ((getConnectionProperties() & 64) != 0) {
                    addressFromNumber = null;
                }
                setAddress(addressFromNumber, numberPresentation);
            }
            String strFilterCnapName = filterCnapName(this.mOriginalConnection.getCnapName());
            int cnapNamePresentation = this.mOriginalConnection.getCnapNamePresentation();
            if (!Objects.equals(strFilterCnapName, getCallerDisplayName()) || cnapNamePresentation != getCallerDisplayNamePresentation()) {
                Log.v(this, "updateAddress, caller display name changed", new Object[0]);
                setCallerDisplayName(strFilterCnapName, cnapNamePresentation);
            }
            if (PhoneNumberUtils.isEmergencyNumber(this.mOriginalConnection.getAddress())) {
                this.mTreatAsEmergencyCall = true;
            }
            refreshConferenceSupported();
        }
    }

    protected void onRemovedFromCallService() {
    }

    public void setOriginalConnection(com.android.internal.telephony.Connection connection) {
        Log.v(this, "new TelephonyConnection, originalConnection: " + connection, new Object[0]);
        clearOriginalConnection();
        this.mOriginalConnectionExtras.clear();
        this.mOriginalConnection = connection;
        this.mOriginalConnection.setTelecomCallId(getTelecomCallId());
        getPhone().registerForPreciseCallStateChanged(this.mHandler, 1, (Object) null);
        getPhone().registerForHandoverStateChanged(this.mHandler, 3, (Object) null);
        getPhone().registerForRingbackTone(this.mHandler, 2, (Object) null);
        getPhone().registerForSuppServiceNotification(this.mHandler, 7, (Object) null);
        getPhone().registerForOnHoldTone(this.mHandler, 14, (Object) null);
        getPhone().registerForInCallVoicePrivacyOn(this.mHandler, 15, (Object) null);
        getPhone().registerForInCallVoicePrivacyOff(this.mHandler, 16, (Object) null);
        getPhone().registerForRadioOffOrNotAvailable(this.mHandler, TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR, (Object) null);
        if (getPhone() instanceof MtkGsmCdmaPhone) {
            getPhone().registerForCdmaCallAccepted(this.mHandler, EVENT_CDMA_CALL_ACCEPTED, (Object) null);
        }
        this.mOriginalConnection.addPostDialListener(this.mPostDialListener);
        this.mOriginalConnection.addListener(this.mOriginalConnectionListener);
        registerSuppMessageManager(getPhone(), this.mOriginalConnection);
        setVideoState(this.mOriginalConnection.getVideoState());
        setOriginalConnectionCapabilities(this.mOriginalConnection.getConnectionCapabilities());
        setWifi(this.mOriginalConnection.isWifi());
        setAudioModeIsVoip(this.mOriginalConnection.getAudioModeIsVoip());
        setVideoProvider(this.mOriginalConnection.getVideoProvider());
        setAudioQuality(this.mOriginalConnection.getAudioQuality());
        setTechnologyTypeExtra();
        Bundle connectionExtras = this.mOriginalConnection.getConnectionExtras();
        this.mHandler.obtainMessage(12, connectionExtras != null ? new Bundle(connectionExtras) : null).sendToTarget();
        if (PhoneNumberUtils.isEmergencyNumber(this.mOriginalConnection.getAddress())) {
            this.mTreatAsEmergencyCall = true;
        }
        if (isImsConnection()) {
            this.mWasImsConnection = true;
        }
        this.mIsMultiParty = this.mOriginalConnection.isMultiparty();
        Bundle bundle = new Bundle();
        ArrayList arrayList = new ArrayList();
        if (this.mOriginalConnection.isActiveCallDisconnectedOnAnswer()) {
            bundle.putBoolean("android.telecom.extra.ANSWERING_DROPS_FG_CALL", true);
        } else {
            arrayList.add("android.telecom.extra.ANSWERING_DROPS_FG_CALL");
        }
        if (shouldSetDisableAddCallExtra()) {
            bundle.putBoolean("android.telecom.extra.DISABLE_ADD_CALL", true);
        } else {
            arrayList.add("android.telecom.extra.DISABLE_ADD_CALL");
        }
        putExtras(bundle);
        removeExtras(arrayList);
        updateState();
        if (this.mOriginalConnection == null) {
            Log.w(this, "original Connection was nulled out as part of setOriginalConnection. " + connection, new Object[0]);
        }
        fireOnOriginalConnectionConfigured();
    }

    private String filterCnapName(final String str) {
        String[] stringArray = null;
        if (str == null) {
            return null;
        }
        PersistableBundle carrierConfig = getCarrierConfig();
        if (carrierConfig != null) {
            stringArray = carrierConfig.getStringArray("filtered_cnap_names_string_array");
        }
        if (stringArray != null && Arrays.asList(stringArray).stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((String) obj).equals(str.toUpperCase());
            }
        }).count() > 0) {
            Log.i(this, "filterCnapName: Filtered CNAP Name: " + str, new Object[0]);
            return "";
        }
        return str;
    }

    private void setTechnologyTypeExtra() {
        if (getPhone() != null) {
            putExtra("android.telecom.extra.CALL_TECHNOLOGY_TYPE", getPhone().getPhoneType());
        }
    }

    private void refreshDisableAddCall() {
        if (shouldSetDisableAddCallExtra()) {
            putExtra("android.telecom.extra.DISABLE_ADD_CALL", true);
        } else {
            removeExtras("android.telecom.extra.DISABLE_ADD_CALL");
        }
    }

    private boolean shouldSetDisableAddCallExtra() {
        ImsPhone phone;
        boolean zIsWfcEnabled;
        boolean zWasVideoCall;
        boolean zIsVideoCall;
        if (this.mOriginalConnection == null || this.mOriginalConnection.shouldAllowAddCallDuringVideoCall() || (phone = getPhone()) == null) {
            return false;
        }
        if (phone instanceof ImsPhone) {
            ImsPhone imsPhone = phone;
            if (imsPhone.getForegroundCall() != null && imsPhone.getForegroundCall().getImsCall() != null) {
                ImsCall imsCall = imsPhone.getForegroundCall().getImsCall();
                zIsVideoCall = imsCall.isVideoCall();
                zWasVideoCall = imsCall.wasVideoCall();
            } else {
                zWasVideoCall = false;
                zIsVideoCall = false;
            }
            zIsWfcEnabled = ImsUtil.isWfcEnabled(phone.getContext());
        } else {
            zIsWfcEnabled = false;
            zWasVideoCall = false;
            zIsVideoCall = false;
        }
        if (zIsVideoCall) {
            return true;
        }
        return zWasVideoCall && this.mIsWifi && !zIsWfcEnabled;
    }

    private boolean hasHighDefAudioProperty() {
        if (!this.mHasHighDefAudio) {
            return false;
        }
        boolean zIsVideo = VideoProfile.isVideo(getVideoState());
        PersistableBundle carrierConfig = getCarrierConfig();
        boolean z = carrierConfig != null && carrierConfig.getBoolean("wifi_calls_can_be_hd_audio");
        boolean z2 = carrierConfig != null && carrierConfig.getBoolean("video_calls_can_be_hd_audio");
        boolean z3 = carrierConfig != null && carrierConfig.getBoolean("gsm_cdma_calls_can_be_hd_audio");
        if (!(carrierConfig != null && carrierConfig.getBoolean("display_hd_audio_property_bool"))) {
            return false;
        }
        if (isGsmCdmaConnection() && !z3) {
            return false;
        }
        if (!zIsVideo || z2) {
            return !this.mIsWifi || z;
        }
        return false;
    }

    private boolean canHoldImsCalls() {
        PersistableBundle carrierConfig = getCarrierConfig();
        return !doesDeviceRespectHoldCarrierConfig() || carrierConfig == null || carrierConfig.getBoolean("allow_hold_in_ims_call");
    }

    PersistableBundle getCarrierConfig() {
        Phone phone = getPhone();
        if (phone == null) {
            return null;
        }
        return PhoneGlobals.getInstance().getCarrierConfigForSubId(phone.getSubId());
    }

    private boolean canDeflectImsCalls() {
        PersistableBundle carrierConfig = getCarrierConfig();
        return carrierConfig != null && carrierConfig.getBoolean("carrier_allow_deflect_ims_call_bool") && isValidRingingCall();
    }

    private boolean doesDeviceRespectHoldCarrierConfig() {
        Phone phone = getPhone();
        if (phone == null) {
            return true;
        }
        return phone.getContext().getResources().getBoolean(android.R.^attr-private.disableChildrenWhenDisabled);
    }

    protected boolean shouldTreatAsEmergencyCall() {
        return this.mTreatAsEmergencyCall;
    }

    void clearOriginalConnection() {
        if (this.mOriginalConnection != null) {
            if (getPhone() != null) {
                if (this.mDtmfRequestIsStarted) {
                    onStopDtmfTone();
                    this.mDtmfRequestIsStarted = false;
                }
                getPhone().unregisterForPreciseCallStateChanged(this.mHandler);
                getPhone().unregisterForRingbackTone(this.mHandler);
                getPhone().unregisterForHandoverStateChanged(this.mHandler);
                getPhone().unregisterForDisconnect(this.mHandler);
                getPhone().unregisterForSuppServiceNotification(this.mHandler);
                getPhone().unregisterForOnHoldTone(this.mHandler);
                getPhone().unregisterForInCallVoicePrivacyOn(this.mHandler);
                getPhone().unregisterForInCallVoicePrivacyOff(this.mHandler);
                getPhone().unregisterForRadioOffOrNotAvailable(this.mHandler);
                if (getPhone() instanceof MtkGsmCdmaPhone) {
                    getPhone().unregisterForCdmaCallAccepted(this.mHandler);
                } else if (getPhone() instanceof MtkImsPhone) {
                    getPhone().getDefaultPhone().unregisterForSpeechCodecInfo(this.mHandler);
                }
                unregisterSuppMessageManager(getPhone(), this.mOriginalConnection);
            }
            this.mOriginalConnection.removePostDialListener(this.mPostDialListener);
            this.mOriginalConnection.removeListener(this.mOriginalConnectionListener);
            this.mHandler.removeCallbacksAndMessages(null);
            this.mOriginalConnection = null;
        }
    }

    protected void hangup(int i) {
        this.mIsLocallyDisconnecting = true;
        if (this.mOriginalConnection != null) {
            try {
                if (isValidRingingCall()) {
                    Call call = getCall();
                    if (call != null) {
                        call.hangup();
                    } else {
                        Log.w(this, "Attempting to hangup a connection without backing call.", new Object[0]);
                    }
                    return;
                }
                this.mOriginalConnection.hangup();
                return;
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Call to Connection.hangup failed with exception", new Object[0]);
                this.mPendingAction = SrvccPendingAction.SRVCC_PENDING_HANGUP_CALL;
                this.mIsLocallyDisconnecting = false;
                return;
            }
        }
        if (this.mTreatAsEmergencyCall) {
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.d(this, "ECC Retry : clear ECC param", new Object[0]);
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
        }
        if (getState() == 6) {
            Log.i(this, "hangup called on an already disconnected call!", new Object[0]);
            close();
        } else {
            setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(3, "Local Disconnect before connection established."));
            close();
        }
    }

    public com.android.internal.telephony.Connection getOriginalConnection() {
        return this.mOriginalConnection;
    }

    public Call getCall() {
        if (this.mOriginalConnection != null) {
            return this.mOriginalConnection.getCall();
        }
        return null;
    }

    public Phone getPhone() {
        Call call = getCall();
        if (call != null) {
            return call.getPhone();
        }
        return null;
    }

    private boolean hasMultipleTopLevelCalls() {
        int i;
        int i2;
        Phone phone = getPhone();
        if (phone == null) {
            i = 0;
        } else {
            if (phone.getRingingCall().isIdle()) {
                i2 = 0;
            } else {
                i2 = 1;
            }
            if (!phone.getForegroundCall().isIdle()) {
                i2++;
            }
            if (!phone.getBackgroundCall().isIdle()) {
                i = i2 + 1;
            } else {
                i = i2;
            }
        }
        return i > 1;
    }

    private com.android.internal.telephony.Connection getForegroundConnection() {
        if (getPhone() != null) {
            return getPhone().getForegroundCall().getEarliestConnection();
        }
        return null;
    }

    public List<ConferenceParticipant> getConferenceParticipants() {
        if (this.mOriginalConnection == null) {
            Log.v(this, "Null mOriginalConnection, cannot get conf participants.", new Object[0]);
            return null;
        }
        return this.mOriginalConnection.getConferenceParticipants();
    }

    private boolean isValidRingingCall() {
        if (getPhone() == null) {
            Log.v(this, "isValidRingingCall, phone is null", new Object[0]);
            return false;
        }
        Call ringingCall = getPhone().getRingingCall();
        if (!ringingCall.getState().isRinging()) {
            Log.v(this, "isValidRingingCall, ringing call is not in ringing state", new Object[0]);
            return false;
        }
        if (ringingCall.getEarliestConnection() != this.mOriginalConnection) {
            Log.v(this, "isValidRingingCall, ringing call connection does not match", new Object[0]);
            return false;
        }
        Log.v(this, "isValidRingingCall, returning true", new Object[0]);
        return true;
    }

    protected void updateExtras(Bundle bundle) {
        if (this.mOriginalConnection != null) {
            if (bundle != null) {
                if (!areBundlesEqual(this.mOriginalConnectionExtras, bundle)) {
                    if (Log.DEBUG) {
                        Log.d(this, "Updating extras:", new Object[0]);
                        for (String str : bundle.keySet()) {
                            Object obj = bundle.get(str);
                            if (obj instanceof String) {
                                Log.d(this, "updateExtras Key=" + Log.pii(str) + " value=" + Log.pii((String) obj), new Object[0]);
                            }
                        }
                    }
                    this.mOriginalConnectionExtras.clear();
                    this.mOriginalConnectionExtras.putAll(bundle);
                    for (String str2 : this.mOriginalConnectionExtras.keySet()) {
                        if (sExtrasMap.containsKey(str2)) {
                            this.mOriginalConnectionExtras.putString(sExtrasMap.get(str2), bundle.getString(str2));
                            this.mOriginalConnectionExtras.remove(str2);
                        }
                    }
                    putExtras(this.mOriginalConnectionExtras);
                    return;
                }
                Log.d(this, "Extras update not required", new Object[0]);
                return;
            }
            Log.d(this, "updateExtras extras: " + Log.pii(bundle), new Object[0]);
        }
    }

    private static boolean areBundlesEqual(Bundle bundle, Bundle bundle2) {
        if (bundle == null || bundle2 == null) {
            return bundle == bundle2;
        }
        if (bundle.size() != bundle2.size()) {
            return false;
        }
        for (String str : bundle.keySet()) {
            if (str != null && !Objects.equals(bundle.get(str), bundle2.get(str))) {
                return false;
            }
        }
        return true;
    }

    public void setStateOverride(Call.State state) {
        this.mIsStateOverridden = true;
        this.mConnectionOverriddenState = state;
        this.mOriginalConnectionState = this.mOriginalConnection.getState();
        updateStateInternal();
    }

    public void resetStateOverride() {
        this.mIsStateOverridden = false;
        updateStateInternal();
    }

    void updateStateInternal() {
        Call.State state;
        if (this.mOriginalConnection == null) {
        }
        if (this.mIsStateOverridden && this.mOriginalConnectionState == this.mOriginalConnection.getState()) {
            state = this.mConnectionOverriddenState;
        } else {
            state = this.mOriginalConnection.getState();
        }
        int disconnectCause = this.mOriginalConnection.getDisconnectCause();
        Log.v(this, "Update state from %s to %s for %s", this.mConnectionState, state, getTelecomCallId());
        boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(this.mOriginalConnection.getAddress());
        if (this.mTreatAsEmergencyCall && !this.mIsLocallyDisconnecting && this.mOriginalConnection.getState() == Call.State.DISCONNECTED && ((this.mConnectionState.isDialing() || this.mConnectionState == Call.State.IDLE) && zIsEmergencyNumber)) {
            Log.i(this, "ECC retry: remote DISCONNECTED, state=" + this.mConnectionState + ", cause=" + disconnectCause, new Object[0]);
            if (disconnectCause != 2 && disconnectCause != 3 && disconnectCause != 14 && disconnectCause != 0 && MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() && !MtkTelephonyConnectionServiceUtil.getInstance().eccRetryTimeout()) {
                state = this.mConnectionState;
                Log.i(this, "ECC retry: meet retry condition, keep state=" + state, new Object[0]);
            }
        }
        if (this.mConnectionState != state) {
            this.mConnectionState = state;
            switch (AnonymousClass4.$SwitchMap$com$android$internal$telephony$Call$State[state.ordinal()]) {
                case 2:
                    if (this.mTreatAsEmergencyCall && MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                        Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                        MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                    }
                    setActiveInternal();
                    break;
                case 3:
                    setOnHold();
                    break;
                case 4:
                case 5:
                    if (this.mOriginalConnection != null && this.mOriginalConnection.isPulledCall()) {
                        setPulling();
                    } else {
                        setDialing();
                        if (state == Call.State.ALERTING) {
                            notifyPhoneAlertingState();
                        }
                    }
                    break;
                case 6:
                case 7:
                    setRinging();
                    break;
                case 8:
                    if (this.mTreatAsEmergencyCall) {
                        if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                            Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                            MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                        }
                        MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                    }
                    if (shouldTreatAsEmergencyCall() && (disconnectCause == 63 || disconnectCause == 64)) {
                        fireOnOriginalConnectionRetryDial(disconnectCause == 64);
                    } else {
                        int preciseDisconnectCause = -1;
                        if (this.mShowPreciseFailedCause) {
                            preciseDisconnectCause = this.mOriginalConnection.getPreciseDisconnectCause();
                        }
                        setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(this.mOriginalConnection.getDisconnectCause(), preciseDisconnectCause, this.mOriginalConnection.getVendorDisconnectCause()));
                        close();
                    }
                    break;
                case 9:
                    this.mIsLocallyDisconnecting = true;
                    break;
            }
        }
    }

    public void updateState() {
        if (this.mOriginalConnection == null) {
            return;
        }
        updateStateInternal();
        updateStatusHints();
        updateAddress();
        updateMultiparty();
        refreshDisableAddCall();
    }

    private void updateMultiparty() {
        if (this.mOriginalConnection != null && this.mIsMultiParty != this.mOriginalConnection.isMultiparty()) {
            this.mIsMultiParty = this.mOriginalConnection.isMultiparty();
            if (this.mIsMultiParty) {
                notifyConferenceStarted();
            }
        }
    }

    private void handleConferenceMergeFailed() {
        this.mHandler.obtainMessage(6).sendToTarget();
    }

    private void handleMultipartyStateChange(boolean z) {
        Object[] objArr = new Object[1];
        objArr[0] = z ? "Y" : "N";
        Log.i(this, "Update multiparty state to %s", objArr);
        this.mHandler.obtainMessage(5, Boolean.valueOf(z)).sendToTarget();
    }

    protected void setActiveInternal() {
        if (getState() == 4) {
            Log.w(this, "Should not be called if this is already ACTIVE", new Object[0]);
            return;
        }
        if (getConnectionService() != null) {
            for (android.telecom.Connection connection : getConnectionService().getAllConnections()) {
                if (connection != this && (connection instanceof TelephonyConnection)) {
                    TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                    if (telephonyConnection.getState() == 4) {
                        telephonyConnection.updateState();
                    }
                }
            }
        }
        setActive();
    }

    private void close() {
        Log.v(this, "close", new Object[0]);
        clearOriginalConnection();
        destroy();
    }

    private boolean isVideoCapable() {
        return can(this.mOriginalConnectionCapabilities, 4) && can(this.mOriginalConnectionCapabilities, 8);
    }

    private boolean isExternalConnection() {
        return can(this.mOriginalConnectionCapabilities, 16);
    }

    private boolean isRtt() {
        return this.mOriginalConnection != null && this.mOriginalConnection.getPhoneType() == 5 && (this.mOriginalConnection instanceof ImsPhoneConnection) && this.mOriginalConnection.isRttEnabledForCall();
    }

    private boolean isPullable() {
        return can(this.mOriginalConnectionCapabilities, 16) && can(this.mOriginalConnectionCapabilities, 32);
    }

    private void setCdmaVoicePrivacy(boolean z) {
        if (this.mIsCdmaVoicePrivacyEnabled != z) {
            this.mIsCdmaVoicePrivacyEnabled = z;
            updateConnectionProperties();
        }
    }

    private int applyConferenceTerminationCapabilities(int i) {
        if (!isImsConnection()) {
            return i | 8192 | 4096;
        }
        return i;
    }

    public void setOriginalConnectionCapabilities(int i) {
        this.mOriginalConnectionCapabilities = i;
        updateConnectionCapabilities();
        updateConnectionProperties();
    }

    public int applyOriginalConnectionCapabilities(int i) {
        return changeBitmask(changeBitmask(changeBitmask(i, 8388608, !can(this.mOriginalConnectionCapabilities, 3)), 3072, can(this.mOriginalConnectionCapabilities, 8)), 768, can(this.mOriginalConnectionCapabilities, 4));
    }

    public void setWifi(boolean z) {
        this.mIsWifi = z;
        updateConnectionProperties();
        updateStatusHints();
        refreshDisableAddCall();
    }

    boolean isWifi() {
        return this.mIsWifi;
    }

    boolean isOutgoingCall() {
        return this.mIsOutgoing;
    }

    public void setAudioQuality(int i) {
        this.mHasHighDefAudio = i == 2;
        updateConnectionProperties();
    }

    public void resetStateForConference() {
        if (getState() == 5) {
            resetStateOverride();
        }
    }

    public boolean setHoldingForConference() {
        if (getState() == 4) {
            setStateOverride(Call.State.HOLDING);
            return true;
        }
        return false;
    }

    public void setRttTextStream(Connection.RttTextStream rttTextStream) {
        this.mRttTextStream = rttTextStream;
    }

    public Connection.RttTextStream getRttTextStream() {
        return this.mRttTextStream;
    }

    public void setVideoPauseSupported(boolean z) {
        this.mIsVideoPauseSupported = z;
    }

    public boolean getVideoPauseSupported() {
        return this.mIsVideoPauseSupported;
    }

    public void setConferenceSupported(boolean z) {
        this.mIsConferenceSupported = z;
    }

    public boolean isConferenceSupported() {
        return this.mIsConferenceSupported;
    }

    public void setManageImsConferenceCallSupported(boolean z) {
        this.mIsManageImsConferenceCallSupported = z;
    }

    public boolean isManageImsConferenceCallSupported() {
        return this.mIsManageImsConferenceCallSupported;
    }

    public void setShowPreciseFailedCause(boolean z) {
        this.mShowPreciseFailedCause = z;
    }

    protected boolean isImsConnection() {
        com.android.internal.telephony.Connection originalConnection = getOriginalConnection();
        return originalConnection != null && originalConnection.getPhoneType() == 5;
    }

    protected boolean isGsmCdmaConnection() {
        Phone phone = getPhone();
        if (phone != null) {
            switch (phone.getPhoneType()) {
            }
            return false;
        }
        return false;
    }

    public boolean wasImsConnection() {
        return this.mWasImsConnection;
    }

    boolean getIsUsingAssistedDialing() {
        return this.mIsUsingAssistedDialing;
    }

    void setIsUsingAssistedDialing(Boolean bool) {
        this.mIsUsingAssistedDialing = bool.booleanValue();
        updateConnectionProperties();
    }

    private static Uri getAddressFromNumber(String str) {
        if (str == null) {
            str = "";
        }
        return Uri.fromParts("tel", str, null);
    }

    private int changeBitmask(int i, int i2, boolean z) {
        if (z) {
            return i | i2;
        }
        return i & (~i2);
    }

    private void updateStatusHints() {
        int i;
        boolean zIsValidRingingCall = isValidRingingCall();
        if (this.mIsWifi && (zIsValidRingingCall || getState() == 4)) {
            if (zIsValidRingingCall) {
                i = R.string.status_hint_label_incoming_wifi_call;
            } else {
                i = R.string.status_hint_label_wifi_call;
            }
            Context context = getPhone().getContext();
            setStatusHints(new StatusHints(context.getString(i), Icon.createWithResource(context.getResources(), R.drawable.ic_signal_wifi_4_bar_24dp), null));
            return;
        }
        setStatusHints(null);
    }

    public final TelephonyConnection addTelephonyConnectionListener(TelephonyConnectionListener telephonyConnectionListener) {
        this.mTelephonyListeners.add(telephonyConnectionListener);
        if (this.mOriginalConnection != null) {
            fireOnOriginalConnectionConfigured();
        }
        return this;
    }

    public final TelephonyConnection removeTelephonyConnectionListener(TelephonyConnectionListener telephonyConnectionListener) {
        if (telephonyConnectionListener != null) {
            this.mTelephonyListeners.remove(telephonyConnectionListener);
        }
        return this;
    }

    @Override
    public void setHoldable(boolean z) {
        this.mIsHoldable = z;
        updateConnectionCapabilities();
    }

    @Override
    public boolean isChildHoldable() {
        return getConference() != null;
    }

    public boolean isHoldable() {
        return this.mIsHoldable;
    }

    private final void fireOnOriginalConnectionConfigured() {
        Iterator<TelephonyConnectionListener> it = this.mTelephonyListeners.iterator();
        while (it.hasNext()) {
            it.next().onOriginalConnectionConfigured(this);
        }
    }

    private final void fireOnOriginalConnectionRetryDial(boolean z) {
        Iterator<TelephonyConnectionListener> it = this.mTelephonyListeners.iterator();
        while (it.hasNext()) {
            it.next().onOriginalConnectionRetry(this, z);
        }
    }

    protected void handleExitedEcmMode() {
        updateConnectionProperties();
        updateConnectionCapabilities();
    }

    private void refreshConferenceSupported() {
        boolean zIsWfcEnabled;
        PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle;
        boolean zIsVideo = VideoProfile.isVideo(getVideoState());
        ImsPhone phone = getPhone();
        boolean z = false;
        if (phone == null) {
            Log.w(this, "refreshConferenceSupported = false; phone is null", new Object[0]);
            if (isConferenceSupported()) {
                setConferenceSupported(false);
                notifyConferenceSupportedChanged(false);
                return;
            }
            return;
        }
        boolean z2 = phone.getPhoneType() == 5;
        if (z2) {
            zIsWfcEnabled = ImsUtil.isWfcEnabled(phone.getContext());
        } else {
            zIsWfcEnabled = false;
        }
        if (z2) {
            phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phone.getDefaultPhone());
        } else {
            phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle((Phone) phone);
        }
        TelecomAccountRegistry telecomAccountRegistry = TelecomAccountRegistry.getInstance(getPhone().getContext());
        boolean zIsMergeCallSupported = telecomAccountRegistry.isMergeCallSupported(phoneAccountHandleMakePstnPhoneAccountHandle);
        boolean zIsMergeImsCallSupported = telecomAccountRegistry.isMergeImsCallSupported(phoneAccountHandleMakePstnPhoneAccountHandle);
        this.mIsCarrierVideoConferencingSupported = telecomAccountRegistry.isVideoConferencingSupported(phoneAccountHandleMakePstnPhoneAccountHandle);
        boolean zIsMergeOfWifiCallsAllowedWhenVoWifiOff = telecomAccountRegistry.isMergeOfWifiCallsAllowedWhenVoWifiOff(phoneAccountHandleMakePstnPhoneAccountHandle);
        Log.v(this, "refreshConferenceSupported : isConfSupp=%b, isImsConfSupp=%b, isVidConfSupp=%b, isMergeOfWifiAllowed=%b, isWifi=%b, isVoWifiEnabled=%b", Boolean.valueOf(zIsMergeCallSupported), Boolean.valueOf(zIsMergeImsCallSupported), Boolean.valueOf(this.mIsCarrierVideoConferencingSupported), Boolean.valueOf(zIsMergeOfWifiCallsAllowedWhenVoWifiOff), Boolean.valueOf(isWifi()), Boolean.valueOf(zIsWfcEnabled));
        if (this.mTreatAsEmergencyCall) {
            Log.d(this, "refreshConferenceSupported = false; emergency call", new Object[0]);
        } else if (!zIsMergeCallSupported || (z2 && !zIsMergeImsCallSupported)) {
            Log.d(this, "refreshConferenceSupported = false; carrier doesn't support conf.", new Object[0]);
        } else if (zIsVideo && !this.mIsCarrierVideoConferencingSupported) {
            Log.d(this, "refreshConferenceSupported = false; video conf not supported.", new Object[0]);
        } else if (!zIsMergeOfWifiCallsAllowedWhenVoWifiOff && isWifi() && !zIsWfcEnabled) {
            Log.d(this, "refreshConferenceSupported = false; can't merge wifi calls when voWifi off.", new Object[0]);
        } else {
            Log.d(this, "refreshConferenceSupported = true.", new Object[0]);
            z = true;
        }
        if (z != isConferenceSupported()) {
            setConferenceSupported(z);
            notifyConferenceSupportedChanged(z);
        }
    }

    private static Map<String, String> createExtrasMap() {
        HashMap map = new HashMap();
        map.put("ChildNum", "android.telecom.extra.CHILD_ADDRESS");
        map.put("DisplayText", "android.telecom.extra.CALL_SUBJECT");
        return Collections.unmodifiableMap(map);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[TelephonyConnection objId:");
        sb.append(System.identityHashCode(this));
        sb.append(" telecomCallID:");
        sb.append(getTelecomCallId());
        sb.append(" type:");
        if (isImsConnection()) {
            sb.append("ims");
        } else {
            boolean z = this instanceof MtkGsmCdmaConnection;
            if (z && ((MtkGsmCdmaConnection) this).getPhoneType() == 1) {
                sb.append("gsm");
            } else if (z && ((MtkGsmCdmaConnection) this).getPhoneType() == 2) {
                sb.append("cdma");
            }
        }
        sb.append(" state:");
        sb.append(android.telecom.Connection.stateToString(getState()));
        sb.append(" capabilities:");
        sb.append(MtkConnection.capabilitiesToString(getConnectionCapabilities()));
        sb.append(" properties:");
        sb.append(MtkConnection.propertiesToString(getConnectionProperties()));
        sb.append(" address:");
        sb.append(Log.pii(getAddress()));
        sb.append(" originalConnection:");
        sb.append(this.mOriginalConnection);
        sb.append(" partOfConf:");
        if (getConference() == null) {
            sb.append("N");
        } else {
            sb.append("Y");
        }
        sb.append(" confSupported:");
        sb.append(this.mIsConferenceSupported ? "Y" : "N");
        sb.append("]");
        return sb.toString();
    }

    public PhoneAccountHandle getAccountHandle() {
        return this.mAccountHandle;
    }

    public void setAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        this.mAccountHandle = phoneAccountHandle;
    }

    public final void notifyConnectionLost() {
        sendConnectionEvent("mediatek.telecom.event.CONNECTION_LOST", null);
    }

    private void notifyPhoneAlertingState() {
        Log.d(this, "notifyPhoneAlertingState", new Object[0]);
        sendConnectionEvent("mediatek.telecom.event.EVENT_CALL_ALERTING_NOTIFICATION", null);
    }

    protected void fireOnCallState() {
        updateState();
    }

    public void onHangupAll() {
        log("onHangupAll");
        if (this.mOriginalConnection != null) {
            try {
                MtkGsmCdmaPhone phone = getPhone();
                if (phone != null && (phone instanceof MtkGsmCdmaPhone)) {
                    phone.hangupAll();
                } else if (phone != null && (phone instanceof MtkImsPhone)) {
                    ((MtkImsPhone) phone).hangupAll();
                } else {
                    Log.w(TAG, "Attempting to hangupAll a connection without backing phone.", new Object[0]);
                }
            } catch (CallStateException e) {
                Log.e(TAG, (Throwable) e, "Call to phone.hangupAll() failed with exception", new Object[0]);
            }
        }
    }

    void onLocalDisconnected() {
        log("mOriginalConnection is null, local disconnect the call");
        if (this.mTreatAsEmergencyCall) {
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "ECC retry: clear ECC param", new Object[0]);
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
        }
        setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(3));
        close();
        updateConnectionCapabilities();
    }

    private void fireOnCdmaCallAccepted() {
        Log.i(this, "fireOnCdmaCallAccepted: %s", stateToString(getState()));
        sendConnectionEvent("mediatek.telecom.event.CDMA_CALL_ACCEPTED", null);
    }

    protected void resetTreatAsEmergencyCall() {
        this.mTreatAsEmergencyCall = false;
    }

    protected void setEmergencyCall(boolean z) {
        this.mTreatAsEmergencyCall = z;
        if (z) {
            Log.i(this, "ECC retry: set call as emergency call", new Object[0]);
        }
    }

    private void log(String str) {
        Log.d(TAG, str, new Object[0]);
    }

    public final void notifyActionFailed(int i) {
        Log.i(this, "notifyActionFailed action = " + i, new Object[0]);
        sendConnectionEvent("mediatek.telecom.event.OPERATION_FAILED", MtkConnection.ConnectionEventHelper.buildParamsForOperationFailed(i));
    }

    public void notifySSNotificationToast(int i, int i2, int i3, String str, int i4) {
        Log.i(this, "notifySSNotificationToast notiType = " + i + " type = " + i2 + " code = " + i3 + " number = " + str + " index = " + i4, new Object[0]);
        sendConnectionEvent("mediatek.telecom.event.SS_NOTIFICATION", MtkConnection.ConnectionEventHelper.buildParamsForSsNotification(i, i2, i3, str, i4));
    }

    public void notifyNumberUpdate(String str) {
        Log.i(this, "notifyNumberUpdate number = " + str, new Object[0]);
        if (!TextUtils.isEmpty(str)) {
            sendConnectionEvent("mediatek.telecom.event.NUMBER_UPDATED", MtkConnection.ConnectionEventHelper.buildParamsForNumberUpdated(str));
        }
    }

    public void notifyIncomingInfoUpdate(int i, String str, int i2) {
        Log.i(this, "notifyIncomingInfoUpdate type = " + i + " alphaid = " + str + " cliValidity = " + i2, new Object[0]);
        sendConnectionEvent("mediatek.telecom.event.INCOMING_INFO_UPDATED", MtkConnection.ConnectionEventHelper.buildParamsForIncomingInfoUpdated(i, str, i2));
    }

    public void onExplicitCallTransfer() {
        log("onExplicitCallTransfer");
        try {
            this.mOriginalConnection.getCall().getPhone().explicitCallTransfer();
        } catch (CallStateException e) {
            Log.e(TAG, (Throwable) e, "Exception occurred while trying to do ECT.", new Object[0]);
        }
    }

    public void onExplicitCallTransfer(String str, int i) {
        Log.v(this, "onExplicitCallTransfer", new Object[0]);
        MtkImsPhone phone = this.mOriginalConnection.getCall().getPhone();
        if (phone instanceof MtkImsPhone) {
            phone.explicitCallTransfer(str, i);
        }
    }

    public void onDeviceSwitch(String str, String str2) {
        Log.v(this, "onDeviceSwitch", new Object[0]);
        MtkImsPhone phone = this.mOriginalConnection.getCall().getPhone();
        if (phone instanceof MtkImsPhone) {
            phone.deviceSwitch(str, str2);
        }
    }

    public void onCancelDeviceSwitch() {
        Log.v(this, "onCancelDeviceSwitch", new Object[0]);
        MtkImsPhone phone = this.mOriginalConnection.getCall().getPhone();
        if (phone instanceof MtkImsPhone) {
            phone.cancelDeviceSwitch();
        }
    }

    static class AnonymousClass4 {
        static final int[] $SwitchMap$com$android$internal$telephony$Call$State;

        static {
            try {
                $SwitchMap$com$android$services$telephony$TelephonyConnection$SrvccPendingAction[SrvccPendingAction.SRVCC_PENDING_ANSWER_CALL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$services$telephony$TelephonyConnection$SrvccPendingAction[SrvccPendingAction.SRVCC_PENDING_HOLD_CALL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$services$telephony$TelephonyConnection$SrvccPendingAction[SrvccPendingAction.SRVCC_PENDING_UNHOLD_CALL.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$services$telephony$TelephonyConnection$SrvccPendingAction[SrvccPendingAction.SRVCC_PENDING_HANGUP_CALL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$com$android$internal$telephony$Call$State = new int[Call.State.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.ACTIVE.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.HOLDING.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DIALING.ordinal()] = 4;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.ALERTING.ordinal()] = 5;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.INCOMING.ordinal()] = 6;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.WAITING.ordinal()] = 7;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DISCONNECTED.ordinal()] = 8;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$Call$State[Call.State.DISCONNECTING.ordinal()] = 9;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    private void trySrvccPendingAction() {
        Log.d(this, "trySrvccPendingAction(): " + this.mPendingAction, new Object[0]);
        switch (this.mPendingAction) {
            case SRVCC_PENDING_ANSWER_CALL:
                onAnswer(0);
                break;
            case SRVCC_PENDING_HOLD_CALL:
                performHold();
                break;
            case SRVCC_PENDING_UNHOLD_CALL:
                performUnhold();
                break;
            case SRVCC_PENDING_HANGUP_CALL:
                hangup(3);
                break;
        }
        this.mPendingAction = SrvccPendingAction.SRVCC_PENDING_NONE;
    }

    private boolean hasTwoHoldingCall() throws CallStateException {
        int i;
        boolean zHasPendingUpdate;
        boolean zHasPendingUpdate2;
        ImsPhone phone = getPhone();
        if (phone != null) {
            i = !phone.getRingingCall().isIdle() ? 1 : 0;
            if (!phone.getForegroundCall().isIdle()) {
                i++;
            }
            if (!phone.getBackgroundCall().isIdle()) {
                i++;
            }
        } else {
            i = 0;
        }
        if (i != 2 || !(phone instanceof MtkImsPhone) || !(this.mOriginalConnection instanceof MtkImsPhoneConnection) || phone.getForegroundCall().getState() != Call.State.HOLDING || phone.getBackgroundCall().getState() != Call.State.HOLDING) {
            return false;
        }
        log("hasTwoHoldingCall(): unhold Ims call");
        ImsPhone imsPhone = phone;
        ImsCall imsCall = imsPhone.getForegroundCall().getImsCall();
        if (imsCall != null) {
            zHasPendingUpdate = imsCall.hasPendingUpdate();
        } else {
            zHasPendingUpdate = false;
        }
        ImsCall imsCall2 = imsPhone.getBackgroundCall().getImsCall();
        if (imsCall2 != null) {
            zHasPendingUpdate2 = imsCall2.hasPendingUpdate();
        } else {
            zHasPendingUpdate2 = false;
        }
        if (zHasPendingUpdate || zHasPendingUpdate2) {
            log("holding call has pending action, fgPendingUpdate: " + zHasPendingUpdate + ", bgPendingUpdate: " + zHasPendingUpdate2);
            return false;
        }
        this.mOriginalConnection.unhold();
        return true;
    }

    private int updatePropertyVoLte(int i) {
        int iChangeBitmask = changeBitmask(i, 32768, isImsConnection());
        log("updatePropertyVoLte: " + MtkConnection.propertiesToString(iChangeBitmask));
        return iChangeBitmask;
    }

    private void removePropertyVoLte() {
        int iChangeBitmask = changeBitmask(getConnectionProperties(), 32768, false);
        Log.d(this, "removePropertyVoLte: %s", MtkConnection.propertiesToString(iChangeBitmask));
        if (getConnectionProperties() != iChangeBitmask) {
            setConnectionProperties(iChangeBitmask);
        }
    }

    private int applyVideoRingtoneCapabilities(int i, int i2) {
        if (getState() == 3) {
            boolean zCan = can(i, 64);
            int iChangeBitmask = changeBitmask(i2, Integer.MIN_VALUE, zCan);
            log("applyVideoRingtoneCapabilities: " + zCan);
            return iChangeBitmask;
        }
        return i2;
    }

    public void performInviteConferenceParticipants(List<String> list) {
        if (this.mOriginalConnection == null) {
            Log.e(TAG, (Throwable) new CallStateException(), "no orginal connection to inviteParticipants", new Object[0]);
        } else if (!isImsConnection()) {
            Log.e(TAG, (Throwable) new CallStateException(), "CS connection doesn't support invite!", new Object[0]);
        } else {
            this.mOriginalConnection.inviteConferenceParticipants(list);
        }
    }

    protected void notifyConferenceParticipantsInvited(boolean z) {
        Iterator<TelephonyConnectionListener> it = this.mTelephonyListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceParticipantsInvited(z);
        }
    }

    private void notifyConferenceConnectionsConfigured(ArrayList<com.android.internal.telephony.Connection> arrayList) {
        Iterator<TelephonyConnectionListener> it = this.mTelephonyListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceConnectionsConfigured(arrayList);
        }
    }

    private void registerSuppMessageManager(Phone phone, com.android.internal.telephony.Connection connection) {
        Log.d(this, "registerSuppMessageManager: " + phone, new Object[0]);
        if (!(phone instanceof MtkImsPhone)) {
            return;
        }
        MtkTelephonyConnectionServiceUtil.getInstance().registerSuppMessageForImsPhone(phone, connection);
    }

    private void unregisterSuppMessageManager(Phone phone, com.android.internal.telephony.Connection connection) {
        Log.d(this, "unregisterSuppMessageManager: " + phone, new Object[0]);
        if (!(phone instanceof MtkImsPhone)) {
            return;
        }
        MtkTelephonyConnectionServiceUtil.getInstance().unregisterSuppMessageForImsPhone(phone, connection);
    }

    public void notifyEcc() {
        sendConnectionEvent("mediatek.telecom.event.EVENT_VOLTE_MARKED_AS_EMERGENCY", null);
    }
}
