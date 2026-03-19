package com.android.internal.telephony.sip;

import android.content.Context;
import android.media.AudioManager;
import android.net.LinkProperties;
import android.net.rtp.AudioGroup;
import android.net.sip.SipAudioCall;
import android.net.sip.SipErrorCode;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.WorkSource;
import android.telephony.CellLocation;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccInternalInterface;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.nano.TelephonyProto;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class SipPhone extends SipPhoneBase {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SipPhone";
    private static final int TIMEOUT_ANSWER_CALL = 8;
    private static final int TIMEOUT_HOLD_CALL = 15;
    private static final long TIMEOUT_HOLD_PROCESSING = 1000;
    private static final int TIMEOUT_MAKE_CALL = 15;
    private static final boolean VDBG = false;
    private SipCall mBackgroundCall;
    private SipCall mForegroundCall;
    private SipProfile mProfile;
    private SipCall mRingingCall;
    private SipManager mSipManager;
    private long mTimeOfLastValidHoldRequest;

    @Override
    public void activateCellBroadcastSms(int i, Message message) {
        super.activateCellBroadcastSms(i, message);
    }

    @Override
    public boolean canDial() {
        return super.canDial();
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
    public void getAvailableNetworks(Message message) {
        super.getAvailableNetworks(message);
    }

    @Override
    public void getCallBarring(String str, String str2, Message message, int i) {
        super.getCallBarring(str, str2, message, i);
    }

    @Override
    public boolean getCallForwardingIndicator() {
        return super.getCallForwardingIndicator();
    }

    @Override
    public void getCallForwardingOption(int i, Message message) {
        super.getCallForwardingOption(i, message);
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
    public List getPendingMmiCodes() {
        return super.getPendingMmiCodes();
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
    public PhoneConstants.State getState() {
        return super.getState();
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
    public boolean handleInCallMmiCommands(String str) {
        return super.handleInCallMmiCommands(str);
    }

    @Override
    public boolean handlePinMmi(String str) {
        return super.handlePinMmi(str);
    }

    @Override
    public boolean handleUssdRequest(String str, ResultReceiver resultReceiver) {
        return super.handleUssdRequest(str, resultReceiver);
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
    public boolean isVideoEnabled() {
        return super.isVideoEnabled();
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
    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        super.registerForRingbackTone(handler, i, obj);
    }

    @Override
    public void registerForSuppServiceNotification(Handler handler, int i, Object obj) {
        super.registerForSuppServiceNotification(handler, i, obj);
    }

    @Override
    public void saveClirSetting(int i) {
        super.saveClirSetting(i);
    }

    @Override
    public void selectNetworkManually(OperatorInfo operatorInfo, boolean z, Message message) {
        super.selectNetworkManually(operatorInfo, z, message);
    }

    @Override
    public void sendEmergencyCallStateChange(boolean z) {
        super.sendEmergencyCallStateChange(z);
    }

    @Override
    public void sendUssdResponse(String str) {
        super.sendUssdResponse(str);
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean z) {
        super.setBroadcastEmergencyCallStateChanges(z);
    }

    @Override
    public void setCallBarring(String str, boolean z, String str2, Message message, int i) {
        super.setCallBarring(str, z, str2, message, i);
    }

    @Override
    public void setCallForwardingOption(int i, int i2, String str, int i3, Message message) {
        super.setCallForwardingOption(i, i2, str, i3, message);
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
    public void setOnPostDialCharacter(Handler handler, int i, Object obj) {
        super.setOnPostDialCharacter(handler, i, obj);
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
    public void startRingbackTone() {
        super.startRingbackTone();
    }

    @Override
    public void stopNetworkScan(Message message) {
        super.stopNetworkScan(message);
    }

    @Override
    public void stopRingbackTone() {
        super.stopRingbackTone();
    }

    @Override
    public void unregisterForRingbackTone(Handler handler) {
        super.unregisterForRingbackTone(handler);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler handler) {
        super.unregisterForSuppServiceNotification(handler);
    }

    @Override
    public void updateServiceLocation() {
        super.updateServiceLocation();
    }

    SipPhone(Context context, PhoneNotifier phoneNotifier, SipProfile sipProfile) {
        super("SIP:" + sipProfile.getUriString(), context, phoneNotifier);
        this.mRingingCall = new SipCall();
        this.mForegroundCall = new SipCall();
        this.mBackgroundCall = new SipCall();
        this.mTimeOfLastValidHoldRequest = System.currentTimeMillis();
        log("new SipPhone: " + hidePii(sipProfile.getUriString()));
        this.mRingingCall = new SipCall();
        this.mForegroundCall = new SipCall();
        this.mBackgroundCall = new SipCall();
        this.mProfile = sipProfile;
        this.mSipManager = SipManager.newInstance(context);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SipPhone) {
            return this.mProfile.getUriString().equals(((SipPhone) obj).mProfile.getUriString());
        }
        return false;
    }

    public String getSipUri() {
        return this.mProfile.getUriString();
    }

    public boolean equals(SipPhone sipPhone) {
        return getSipUri().equals(sipPhone.getSipUri());
    }

    public Connection takeIncomingCall(Object obj) {
        synchronized (SipPhone.class) {
            if (!(obj instanceof SipAudioCall)) {
                log("takeIncomingCall: ret=null, not a SipAudioCall");
                return null;
            }
            if (this.mRingingCall.getState().isAlive()) {
                log("takeIncomingCall: ret=null, ringingCall not alive");
                return null;
            }
            if (this.mForegroundCall.getState().isAlive() && this.mBackgroundCall.getState().isAlive()) {
                log("takeIncomingCall: ret=null, foreground and background both alive");
                return null;
            }
            try {
                SipAudioCall sipAudioCall = (SipAudioCall) obj;
                log("takeIncomingCall: taking call from: " + hidePii(sipAudioCall.getPeerProfile().getUriString()));
                if (sipAudioCall.getLocalProfile().getUriString().equals(this.mProfile.getUriString())) {
                    SipConnection sipConnectionInitIncomingCall = this.mRingingCall.initIncomingCall(sipAudioCall, this.mForegroundCall.getState().isAlive());
                    if (sipAudioCall.getState() != 3) {
                        log("    takeIncomingCall: call cancelled !!");
                        this.mRingingCall.reset();
                        sipConnectionInitIncomingCall = null;
                    }
                    return sipConnectionInitIncomingCall;
                }
            } catch (Exception e) {
                log("    takeIncomingCall: exception e=" + e);
                this.mRingingCall.reset();
            }
            log("takeIncomingCall: NOT taking !!");
            return null;
        }
    }

    @Override
    public void acceptCall(int i) throws CallStateException {
        synchronized (SipPhone.class) {
            if (this.mRingingCall.getState() != Call.State.INCOMING && this.mRingingCall.getState() != Call.State.WAITING) {
                log("acceptCall: throw CallStateException(\"phone not ringing\")");
                throw new CallStateException("phone not ringing");
            }
            log("acceptCall: accepting");
            this.mRingingCall.setMute(false);
            this.mRingingCall.acceptCall();
        }
    }

    @Override
    public void rejectCall() throws CallStateException {
        synchronized (SipPhone.class) {
            if (this.mRingingCall.getState().isRinging()) {
                log("rejectCall: rejecting");
                this.mRingingCall.rejectCall();
            } else {
                log("rejectCall: throw CallStateException(\"phone not ringing\")");
                throw new CallStateException("phone not ringing");
            }
        }
    }

    @Override
    public Connection dial(String str, PhoneInternalInterface.DialArgs dialArgs) throws CallStateException {
        Connection connectionDialInternal;
        synchronized (SipPhone.class) {
            connectionDialInternal = dialInternal(str, dialArgs.videoState);
        }
        return connectionDialInternal;
    }

    private Connection dialInternal(String str, int i) throws CallStateException {
        log("dialInternal: dialString=" + hidePii(str));
        clearDisconnected();
        if (!canDial()) {
            throw new CallStateException("dialInternal: cannot dial in current state");
        }
        if (this.mForegroundCall.getState() == Call.State.ACTIVE) {
            switchHoldingAndActive();
        }
        if (this.mForegroundCall.getState() != Call.State.IDLE) {
            throw new CallStateException("cannot dial in current state");
        }
        this.mForegroundCall.setMute(false);
        try {
            return this.mForegroundCall.dial(str);
        } catch (SipException e) {
            loge("dialInternal: ", e);
            throw new CallStateException("dial error: " + e);
        }
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        if (!isHoldTimeoutExpired()) {
            log("switchHoldingAndActive: Disregarded! Under 1000 ms...");
            return;
        }
        log("switchHoldingAndActive: switch fg and bg");
        synchronized (SipPhone.class) {
            this.mForegroundCall.switchWith(this.mBackgroundCall);
            if (this.mBackgroundCall.getState().isAlive()) {
                this.mBackgroundCall.hold();
            }
            if (this.mForegroundCall.getState().isAlive()) {
                this.mForegroundCall.unhold();
            }
        }
    }

    @Override
    public boolean canConference() {
        log("canConference: ret=true");
        return true;
    }

    @Override
    public void conference() throws CallStateException {
        synchronized (SipPhone.class) {
            if (this.mForegroundCall.getState() != Call.State.ACTIVE || this.mForegroundCall.getState() != Call.State.ACTIVE) {
                throw new CallStateException("wrong state to merge calls: fg=" + this.mForegroundCall.getState() + ", bg=" + this.mBackgroundCall.getState());
            }
            log("conference: merge fg & bg");
            this.mForegroundCall.merge(this.mBackgroundCall);
        }
    }

    public void conference(Call call) throws CallStateException {
        synchronized (SipPhone.class) {
            if (!(call instanceof SipCall)) {
                throw new CallStateException("expect " + SipCall.class + ", cannot merge with " + call.getClass());
            }
            this.mForegroundCall.merge((SipCall) call);
        }
    }

    @Override
    public boolean canTransfer() {
        return false;
    }

    @Override
    public void explicitCallTransfer() {
    }

    @Override
    public void clearDisconnected() {
        synchronized (SipPhone.class) {
            this.mRingingCall.clearDisconnected();
            this.mForegroundCall.clearDisconnected();
            this.mBackgroundCall.clearDisconnected();
            updatePhoneState();
            notifyPreciseCallStateChanged();
        }
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("sendDtmf called with invalid character '" + c + "'");
            return;
        }
        if (this.mForegroundCall.getState().isAlive()) {
            synchronized (SipPhone.class) {
                this.mForegroundCall.sendDtmf(c);
            }
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("startDtmf called with invalid character '" + c + "'");
            return;
        }
        sendDtmf(c);
    }

    @Override
    public void stopDtmf() {
    }

    public void sendBurstDtmf(String str) {
        loge("sendBurstDtmf() is a CDMA method");
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message message) {
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    @Override
    public void setOutgoingCallerIdDisplay(int i, Message message) {
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    @Override
    public void getCallWaiting(Message message) {
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    @Override
    public void setCallWaiting(boolean z, Message message) {
        loge("call waiting not supported");
    }

    @Override
    public void setEchoSuppressionEnabled() {
        synchronized (SipPhone.class) {
            if (((AudioManager) this.mContext.getSystemService("audio")).getParameters("ec_supported").contains("off")) {
                this.mForegroundCall.setAudioGroupMode();
            }
        }
    }

    @Override
    public void setMute(boolean z) {
        synchronized (SipPhone.class) {
            this.mForegroundCall.setMute(z);
        }
    }

    @Override
    public boolean getMute() {
        if (this.mForegroundCall.getState().isAlive()) {
            return this.mForegroundCall.getMute();
        }
        return this.mBackgroundCall.getMute();
    }

    @Override
    public Call getForegroundCall() {
        return this.mForegroundCall;
    }

    @Override
    public Call getBackgroundCall() {
        return this.mBackgroundCall;
    }

    @Override
    public Call getRingingCall() {
        return this.mRingingCall;
    }

    @Override
    public ServiceState getServiceState() {
        return super.getServiceState();
    }

    private String getUriString(SipProfile sipProfile) {
        return sipProfile.getUserName() + "@" + getSipDomain(sipProfile);
    }

    private String getSipDomain(SipProfile sipProfile) {
        String sipDomain = sipProfile.getSipDomain();
        if (sipDomain.endsWith(":5060")) {
            return sipDomain.substring(0, sipDomain.length() - 5);
        }
        return sipDomain;
    }

    private static Call.State getCallStateFrom(SipAudioCall sipAudioCall) {
        if (sipAudioCall.isOnHold()) {
            return Call.State.HOLDING;
        }
        int state = sipAudioCall.getState();
        if (state == 0) {
            return Call.State.IDLE;
        }
        switch (state) {
            case 3:
            case 4:
                return Call.State.INCOMING;
            case 5:
                return Call.State.DIALING;
            case 6:
                return Call.State.ALERTING;
            case 7:
                return Call.State.DISCONNECTING;
            case 8:
                return Call.State.ACTIVE;
            default:
                slog("illegal connection state: " + state);
                return Call.State.DISCONNECTED;
        }
    }

    private synchronized boolean isHoldTimeoutExpired() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis - this.mTimeOfLastValidHoldRequest > TIMEOUT_HOLD_PROCESSING) {
            this.mTimeOfLastValidHoldRequest = jCurrentTimeMillis;
            return true;
        }
        return false;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private static void slog(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private void loge(String str, Exception exc) {
        Rlog.e(LOG_TAG, str, exc);
    }

    private class SipCall extends SipCallBase {
        private static final boolean SC_DBG = true;
        private static final String SC_TAG = "SipCall";
        private static final boolean SC_VDBG = false;

        private SipCall() {
        }

        void reset() {
            log("reset");
            this.mConnections.clear();
            setState(Call.State.IDLE);
        }

        void switchWith(SipCall sipCall) {
            log("switchWith");
            synchronized (SipPhone.class) {
                SipCall sipCall2 = SipPhone.this.new SipCall();
                sipCall2.takeOver(this);
                takeOver(sipCall);
                sipCall.takeOver(sipCall2);
            }
        }

        private void takeOver(SipCall sipCall) {
            log("takeOver");
            this.mConnections = sipCall.mConnections;
            this.mState = sipCall.mState;
            Iterator<Connection> it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) it.next()).changeOwner(this);
            }
        }

        @Override
        public Phone getPhone() {
            return SipPhone.this;
        }

        @Override
        public List<Connection> getConnections() {
            ArrayList<Connection> arrayList;
            synchronized (SipPhone.class) {
                arrayList = this.mConnections;
            }
            return arrayList;
        }

        Connection dial(String str) throws SipException {
            String strReplaceFirst;
            log("dial: num=xxx");
            if (!str.contains("@")) {
                String strQuote = Pattern.quote(SipPhone.this.mProfile.getUserName() + "@");
                strReplaceFirst = SipPhone.this.mProfile.getUriString().replaceFirst(strQuote, str + "@");
            } else {
                strReplaceFirst = str;
            }
            try {
                SipConnection sipConnection = SipPhone.this.new SipConnection(this, new SipProfile.Builder(strReplaceFirst).build(), str);
                sipConnection.dial();
                this.mConnections.add(sipConnection);
                setState(Call.State.DIALING);
                return sipConnection;
            } catch (ParseException e) {
                throw new SipException("dial", e);
            }
        }

        @Override
        public void hangup() throws CallStateException {
            synchronized (SipPhone.class) {
                if (this.mState.isAlive()) {
                    log("hangup: call " + getState() + ": " + this + " on phone " + getPhone());
                    setState(Call.State.DISCONNECTING);
                    CallStateException e = null;
                    Iterator<Connection> it = this.mConnections.iterator();
                    while (it.hasNext()) {
                        try {
                            it.next().hangup();
                        } catch (CallStateException e2) {
                            e = e2;
                        }
                    }
                    if (e != null) {
                        throw e;
                    }
                } else {
                    log("hangup: dead call " + getState() + ": " + this + " on phone " + getPhone());
                }
            }
        }

        SipConnection initIncomingCall(SipAudioCall sipAudioCall, boolean z) {
            SipConnection sipConnection = new SipConnection(SipPhone.this, this, sipAudioCall.getPeerProfile());
            this.mConnections.add(sipConnection);
            Call.State state = z ? Call.State.WAITING : Call.State.INCOMING;
            sipConnection.initIncomingCall(sipAudioCall, state);
            setState(state);
            SipPhone.this.notifyNewRingingConnectionP(sipConnection);
            return sipConnection;
        }

        void rejectCall() throws CallStateException {
            log("rejectCall:");
            hangup();
        }

        void acceptCall() throws CallStateException {
            log("acceptCall: accepting");
            if (this != SipPhone.this.mRingingCall) {
                throw new CallStateException("acceptCall() in a non-ringing call");
            }
            if (this.mConnections.size() != 1) {
                throw new CallStateException("acceptCall() in a conf call");
            }
            ((SipConnection) this.mConnections.get(0)).acceptCall();
        }

        private boolean isSpeakerOn() {
            return Boolean.valueOf(((AudioManager) SipPhone.this.mContext.getSystemService("audio")).isSpeakerphoneOn()).booleanValue();
        }

        void setAudioGroupMode() {
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup == null) {
                log("setAudioGroupMode: audioGroup == null ignore");
                return;
            }
            int mode = audioGroup.getMode();
            if (this.mState == Call.State.HOLDING) {
                audioGroup.setMode(0);
            } else if (getMute()) {
                audioGroup.setMode(1);
            } else if (isSpeakerOn()) {
                audioGroup.setMode(3);
            } else {
                audioGroup.setMode(2);
            }
            log(String.format("setAudioGroupMode change: %d --> %d", Integer.valueOf(mode), Integer.valueOf(audioGroup.getMode())));
        }

        void hold() throws CallStateException {
            log("hold:");
            setState(Call.State.HOLDING);
            Iterator<Connection> it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) it.next()).hold();
            }
            setAudioGroupMode();
        }

        void unhold() throws CallStateException {
            log("unhold:");
            setState(Call.State.ACTIVE);
            AudioGroup audioGroup = new AudioGroup();
            Iterator<Connection> it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) it.next()).unhold(audioGroup);
            }
            setAudioGroupMode();
        }

        void setMute(boolean z) {
            log("setMute: muted=" + z);
            Iterator<Connection> it = this.mConnections.iterator();
            while (it.hasNext()) {
                ((SipConnection) it.next()).setMute(z);
            }
        }

        boolean getMute() {
            boolean mute = this.mConnections.isEmpty() ? false : ((SipConnection) this.mConnections.get(0)).getMute();
            log("getMute: ret=" + mute);
            return mute;
        }

        void merge(SipCall sipCall) throws CallStateException {
            log("merge:");
            AudioGroup audioGroup = getAudioGroup();
            for (Connection connection : (Connection[]) sipCall.mConnections.toArray(new Connection[sipCall.mConnections.size()])) {
                SipConnection sipConnection = (SipConnection) connection;
                add(sipConnection);
                if (sipConnection.getState() == Call.State.HOLDING) {
                    sipConnection.unhold(audioGroup);
                }
            }
            sipCall.setState(Call.State.IDLE);
        }

        private void add(SipConnection sipConnection) {
            log("add:");
            SipCall call = sipConnection.getCall();
            if (call == this) {
                return;
            }
            if (call != null) {
                call.mConnections.remove(sipConnection);
            }
            this.mConnections.add(sipConnection);
            sipConnection.changeOwner(this);
        }

        void sendDtmf(char c) {
            log("sendDtmf: c=" + c);
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup == null) {
                log("sendDtmf: audioGroup == null, ignore c=" + c);
                return;
            }
            audioGroup.sendDtmf(convertDtmf(c));
        }

        private int convertDtmf(char c) {
            int i = c - '0';
            if (i < 0 || i > 9) {
                if (c == '#') {
                    return 11;
                }
                if (c == '*') {
                    return 10;
                }
                switch (c) {
                    case 'A':
                        return 12;
                    case 'B':
                        return 13;
                    case TelephonyProto.RilErrno.RIL_E_INVALID_RESPONSE:
                        return 14;
                    case 'D':
                        return 15;
                    default:
                        throw new IllegalArgumentException("invalid DTMF char: " + ((int) c));
                }
            }
            return i;
        }

        @Override
        protected void setState(Call.State state) {
            if (this.mState != state) {
                log("setState: cur state" + this.mState + " --> " + state + ": " + this + ": on phone " + getPhone() + " " + this.mConnections.size());
                if (state == Call.State.ALERTING) {
                    this.mState = state;
                    SipPhone.this.startRingbackTone();
                } else if (this.mState == Call.State.ALERTING) {
                    SipPhone.this.stopRingbackTone();
                }
                this.mState = state;
                SipPhone.this.updatePhoneState();
                SipPhone.this.notifyPreciseCallStateChanged();
            }
        }

        void onConnectionStateChanged(SipConnection sipConnection) {
            log("onConnectionStateChanged: conn=" + sipConnection);
            if (this.mState != Call.State.ACTIVE) {
                setState(sipConnection.getState());
            }
        }

        void onConnectionEnded(SipConnection sipConnection) {
            log("onConnectionEnded: conn=" + sipConnection);
            if (this.mState != Call.State.DISCONNECTED) {
                boolean z = true;
                log("---check connections: " + this.mConnections.size());
                Iterator<Connection> it = this.mConnections.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Connection next = it.next();
                    log("   state=" + next.getState() + ": " + next);
                    if (next.getState() != Call.State.DISCONNECTED) {
                        z = false;
                        break;
                    }
                }
                if (z) {
                    setState(Call.State.DISCONNECTED);
                }
            }
            SipPhone.this.notifyDisconnectP(sipConnection);
        }

        private AudioGroup getAudioGroup() {
            if (this.mConnections.isEmpty()) {
                return null;
            }
            return ((SipConnection) this.mConnections.get(0)).getAudioGroup();
        }

        private void log(String str) {
            Rlog.d(SC_TAG, str);
        }
    }

    private class SipConnection extends SipConnectionBase {
        private static final boolean SCN_DBG = true;
        private static final String SCN_TAG = "SipConnection";
        private SipAudioCallAdapter mAdapter;
        private boolean mIncoming;
        private String mOriginalNumber;
        private SipCall mOwner;
        private SipProfile mPeer;
        private SipAudioCall mSipAudioCall;
        private Call.State mState;

        public SipConnection(SipCall sipCall, SipProfile sipProfile, String str) {
            super(str);
            this.mState = Call.State.IDLE;
            this.mIncoming = false;
            this.mAdapter = new SipAudioCallAdapter() {
                {
                    SipPhone sipPhone = SipPhone.this;
                }

                @Override
                protected void onCallEnded(int i) {
                    String str2;
                    if (SipConnection.this.getDisconnectCause() != 3) {
                        SipConnection.this.setDisconnectCause(i);
                    }
                    synchronized (SipPhone.class) {
                        SipConnection.this.setState(Call.State.DISCONNECTED);
                        SipAudioCall sipAudioCall = SipConnection.this.mSipAudioCall;
                        SipConnection.this.mSipAudioCall = null;
                        if (sipAudioCall == null) {
                            str2 = "";
                        } else {
                            str2 = sipAudioCall.getState() + ", ";
                        }
                        SipConnection.this.log("[SipAudioCallAdapter] onCallEnded: " + SipPhone.hidePii(SipConnection.this.mPeer.getUriString()) + ": " + str2 + "cause: " + SipConnection.this.getDisconnectCause() + ", on phone " + SipConnection.this.getPhone());
                        if (sipAudioCall != null) {
                            sipAudioCall.setListener(null);
                            sipAudioCall.close();
                        }
                        SipConnection.this.mOwner.onConnectionEnded(SipConnection.this);
                    }
                }

                @Override
                public void onCallEstablished(SipAudioCall sipAudioCall) {
                    onChanged(sipAudioCall);
                    if (SipConnection.this.mState == Call.State.ACTIVE) {
                        sipAudioCall.startAudio();
                    }
                }

                @Override
                public void onCallHeld(SipAudioCall sipAudioCall) {
                    onChanged(sipAudioCall);
                    if (SipConnection.this.mState == Call.State.HOLDING) {
                        sipAudioCall.startAudio();
                    }
                }

                @Override
                public void onChanged(SipAudioCall sipAudioCall) {
                    synchronized (SipPhone.class) {
                        Call.State callStateFrom = SipPhone.getCallStateFrom(sipAudioCall);
                        if (SipConnection.this.mState == callStateFrom) {
                            return;
                        }
                        if (callStateFrom != Call.State.INCOMING) {
                            if (SipConnection.this.mOwner == SipPhone.this.mRingingCall) {
                                if (SipPhone.this.mRingingCall.getState() == Call.State.WAITING) {
                                    try {
                                        SipPhone.this.switchHoldingAndActive();
                                    } catch (CallStateException e) {
                                        onCallEnded(3);
                                        return;
                                    }
                                }
                                SipPhone.this.mForegroundCall.switchWith(SipPhone.this.mRingingCall);
                            }
                            SipConnection.this.setState(callStateFrom);
                        } else {
                            SipConnection.this.setState(SipConnection.this.mOwner.getState());
                        }
                        SipConnection.this.mOwner.onConnectionStateChanged(SipConnection.this);
                        SipConnection.this.log("onChanged: " + SipPhone.hidePii(SipConnection.this.mPeer.getUriString()) + ": " + SipConnection.this.mState + " on phone " + SipConnection.this.getPhone());
                    }
                }

                @Override
                protected void onError(int i) {
                    SipConnection.this.log("onError: " + i);
                    onCallEnded(i);
                }
            };
            this.mOwner = sipCall;
            this.mPeer = sipProfile;
            this.mOriginalNumber = str;
        }

        public SipConnection(SipPhone sipPhone, SipCall sipCall, SipProfile sipProfile) {
            this(sipCall, sipProfile, sipPhone.getUriString(sipProfile));
        }

        @Override
        public String getCnapName() {
            String displayName = this.mPeer.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                return null;
            }
            return displayName;
        }

        @Override
        public int getNumberPresentation() {
            return 1;
        }

        void initIncomingCall(SipAudioCall sipAudioCall, Call.State state) {
            setState(state);
            this.mSipAudioCall = sipAudioCall;
            sipAudioCall.setListener(this.mAdapter);
            this.mIncoming = true;
        }

        void acceptCall() throws CallStateException {
            try {
                this.mSipAudioCall.answerCall(8);
            } catch (SipException e) {
                throw new CallStateException("acceptCall(): " + e);
            }
        }

        void changeOwner(SipCall sipCall) {
            this.mOwner = sipCall;
        }

        AudioGroup getAudioGroup() {
            if (this.mSipAudioCall == null) {
                return null;
            }
            return this.mSipAudioCall.getAudioGroup();
        }

        void dial() throws SipException {
            setState(Call.State.DIALING);
            this.mSipAudioCall = SipPhone.this.mSipManager.makeAudioCall(SipPhone.this.mProfile, this.mPeer, (SipAudioCall.Listener) null, 15);
            this.mSipAudioCall.setListener(this.mAdapter);
        }

        void hold() throws CallStateException {
            setState(Call.State.HOLDING);
            try {
                this.mSipAudioCall.holdCall(15);
            } catch (SipException e) {
                throw new CallStateException("hold(): " + e);
            }
        }

        void unhold(AudioGroup audioGroup) throws CallStateException {
            this.mSipAudioCall.setAudioGroup(audioGroup);
            setState(Call.State.ACTIVE);
            try {
                this.mSipAudioCall.continueCall(15);
            } catch (SipException e) {
                throw new CallStateException("unhold(): " + e);
            }
        }

        void setMute(boolean z) {
            if (this.mSipAudioCall != null && z != this.mSipAudioCall.isMuted()) {
                StringBuilder sb = new StringBuilder();
                sb.append("setState: prev muted=");
                sb.append(!z);
                sb.append(" new muted=");
                sb.append(z);
                log(sb.toString());
                this.mSipAudioCall.toggleMute();
            }
        }

        boolean getMute() {
            if (this.mSipAudioCall == null) {
                return false;
            }
            return this.mSipAudioCall.isMuted();
        }

        @Override
        protected void setState(Call.State state) {
            if (state == this.mState) {
                return;
            }
            super.setState(state);
            this.mState = state;
        }

        @Override
        public Call.State getState() {
            return this.mState;
        }

        @Override
        public boolean isIncoming() {
            return this.mIncoming;
        }

        @Override
        public String getAddress() {
            return this.mOriginalNumber;
        }

        @Override
        public SipCall getCall() {
            return this.mOwner;
        }

        @Override
        protected Phone getPhone() {
            return this.mOwner.getPhone();
        }

        @Override
        public void hangup() throws CallStateException {
            synchronized (SipPhone.class) {
                log("hangup: conn=" + SipPhone.hidePii(this.mPeer.getUriString()) + ": " + this.mState + ": on phone " + getPhone().getPhoneName());
                if (this.mState.isAlive()) {
                    int i = 16;
                    try {
                        try {
                            SipAudioCall sipAudioCall = this.mSipAudioCall;
                            if (sipAudioCall != null) {
                                sipAudioCall.setListener(null);
                                sipAudioCall.endCall();
                            }
                        } catch (SipException e) {
                            throw new CallStateException("hangup(): " + e);
                        }
                    } finally {
                        SipAudioCallAdapter sipAudioCallAdapter = this.mAdapter;
                        if (this.mState != Call.State.INCOMING && this.mState != Call.State.WAITING) {
                            i = 3;
                        }
                        sipAudioCallAdapter.onCallEnded(i);
                    }
                }
            }
        }

        @Override
        public void separate() throws CallStateException {
            SipCall sipCall;
            synchronized (SipPhone.class) {
                if (getPhone() == SipPhone.this) {
                    sipCall = (SipCall) SipPhone.this.getBackgroundCall();
                } else {
                    sipCall = (SipCall) SipPhone.this.getForegroundCall();
                }
                if (sipCall.getState() != Call.State.IDLE) {
                    throw new CallStateException("cannot put conn back to a call in non-idle state: " + sipCall.getState());
                }
                log("separate: conn=" + this.mPeer.getUriString() + " from " + this.mOwner + " back to " + sipCall);
                Phone phone = getPhone();
                AudioGroup audioGroup = sipCall.getAudioGroup();
                sipCall.add(this);
                this.mSipAudioCall.setAudioGroup(audioGroup);
                phone.switchHoldingAndActive();
                SipCall sipCall2 = (SipCall) SipPhone.this.getForegroundCall();
                this.mSipAudioCall.startAudio();
                sipCall2.onConnectionStateChanged(this);
            }
        }

        @Override
        public void deflect(String str) throws CallStateException {
            throw new CallStateException("deflect is not supported for SipPhone");
        }

        private void log(String str) {
            Rlog.d(SCN_TAG, str);
        }
    }

    private abstract class SipAudioCallAdapter extends SipAudioCall.Listener {
        private static final boolean SACA_DBG = true;
        private static final String SACA_TAG = "SipAudioCallAdapter";

        protected abstract void onCallEnded(int i);

        protected abstract void onError(int i);

        private SipAudioCallAdapter() {
        }

        @Override
        public void onCallEnded(SipAudioCall sipAudioCall) {
            int i;
            log("onCallEnded: call=" + sipAudioCall);
            if (sipAudioCall.isInCall()) {
                i = 2;
            } else {
                i = 1;
            }
            onCallEnded(i);
        }

        @Override
        public void onCallBusy(SipAudioCall sipAudioCall) {
            log("onCallBusy: call=" + sipAudioCall);
            onCallEnded(4);
        }

        @Override
        public void onError(SipAudioCall sipAudioCall, int i, String str) {
            log("onError: call=" + sipAudioCall + " code=" + SipErrorCode.toString(i) + ": " + str);
            switch (i) {
                case IccInternalInterface.ERROR_ICC_PROVIDER_EMAIL_FULL:
                    onError(9);
                    break;
                case IccInternalInterface.ERROR_ICC_PROVIDER_ADN_LIST_NOT_EXIST:
                    onError(11);
                    break;
                case IccInternalInterface.ERROR_ICC_PROVIDER_GENERIC_FAILURE:
                    onError(14);
                    break;
                case -9:
                case -4:
                default:
                    onError(36);
                    break;
                case -8:
                    onError(10);
                    break;
                case -7:
                    onError(8);
                    break;
                case -6:
                    onError(7);
                    break;
                case -5:
                case -3:
                    onError(13);
                    break;
                case -2:
                    onError(12);
                    break;
            }
        }

        private void log(String str) {
            Rlog.d(SACA_TAG, str);
        }
    }

    public static String hidePii(String str) {
        return "xxxxx";
    }
}
