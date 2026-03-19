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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.IccProvider;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
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
    public void activateCellBroadcastSms(int activate, Message response) {
        super.activateCellBroadcastSms(activate, response);
    }

    @Override
    public boolean canDial() {
        return super.canDial();
    }

    @Override
    public Connection dial(String dialString, UUSInfo uusInfo, int videoState, Bundle intentExtras) {
        return super.dial(dialString, uusInfo, videoState, intentExtras);
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
    public void getAvailableNetworks(Message response) {
        super.getAvailableNetworks(response);
    }

    @Override
    public boolean getCallForwardingIndicator() {
        return super.getCallForwardingIndicator();
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        super.getCallForwardingOption(commandInterfaceCFReason, onComplete);
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
    public boolean handleInCallMmiCommands(String dialString) {
        return super.handleInCallMmiCommands(dialString);
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
    public void registerForRingbackTone(Handler h, int what, Object obj) {
        super.registerForRingbackTone(h, what, obj);
    }

    @Override
    public void registerForSuppServiceNotification(Handler h, int what, Object obj) {
        super.registerForSuppServiceNotification(h, what, obj);
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
    public void sendEmergencyCallStateChange(boolean callActive) {
        super.sendEmergencyCallStateChange(callActive);
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        super.sendUssdResponse(ussdMessge);
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean broadcast) {
        super.setBroadcastEmergencyCallStateChanges(broadcast);
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message onComplete) {
        super.setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, timerSeconds, onComplete);
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
    public void setOnPostDialCharacter(Handler h, int what, Object obj) {
        super.setOnPostDialCharacter(h, what, obj);
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
    public void unregisterForRingbackTone(Handler h) {
        super.unregisterForRingbackTone(h);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        super.unregisterForSuppServiceNotification(h);
    }

    @Override
    public void updateServiceLocation() {
        super.updateServiceLocation();
    }

    SipPhone(Context context, PhoneNotifier notifier, SipProfile profile) {
        super("SIP:" + profile.getUriString(), context, notifier);
        SipCall sipCall = null;
        this.mRingingCall = new SipCall(this, sipCall);
        this.mForegroundCall = new SipCall(this, sipCall);
        this.mBackgroundCall = new SipCall(this, sipCall);
        this.mTimeOfLastValidHoldRequest = System.currentTimeMillis();
        log("new SipPhone: " + hidePii(profile.getUriString()));
        this.mRingingCall = new SipCall(this, sipCall);
        this.mForegroundCall = new SipCall(this, sipCall);
        this.mBackgroundCall = new SipCall(this, sipCall);
        this.mProfile = profile;
        this.mSipManager = SipManager.newInstance(context);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SipPhone)) {
            return false;
        }
        SipPhone that = (SipPhone) o;
        return this.mProfile.getUriString().equals(that.mProfile.getUriString());
    }

    public String getSipUri() {
        return this.mProfile.getUriString();
    }

    public boolean equals(SipPhone phone) {
        return getSipUri().equals(phone.getSipUri());
    }

    public Connection takeIncomingCall(Object incomingCall) {
        synchronized (SipPhone.class) {
            if (!(incomingCall instanceof SipAudioCall)) {
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
                SipAudioCall sipAudioCall = (SipAudioCall) incomingCall;
                log("takeIncomingCall: taking call from: " + sipAudioCall.getPeerProfile().getUriString());
                String localUri = sipAudioCall.getLocalProfile().getUriString();
                if (localUri.equals(this.mProfile.getUriString())) {
                    boolean makeCallWait = this.mForegroundCall.getState().isAlive();
                    SipConnection connection = this.mRingingCall.initIncomingCall(sipAudioCall, makeCallWait);
                    if (sipAudioCall.getState() != 3) {
                        log("    takeIncomingCall: call cancelled !!");
                        this.mRingingCall.reset();
                        connection = null;
                    }
                    return connection;
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
    public void acceptCall(int videoState) throws CallStateException {
        synchronized (SipPhone.class) {
            if (this.mRingingCall.getState() == Call.State.INCOMING || this.mRingingCall.getState() == Call.State.WAITING) {
                log("acceptCall: accepting");
                this.mRingingCall.setMute(false);
                this.mRingingCall.acceptCall();
            } else {
                log("acceptCall: throw CallStateException(\"phone not ringing\")");
                throw new CallStateException("phone not ringing");
            }
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
    public Connection dial(String dialString, int videoState) throws CallStateException {
        Connection connectionDialInternal;
        synchronized (SipPhone.class) {
            connectionDialInternal = dialInternal(dialString, videoState);
        }
        return connectionDialInternal;
    }

    private Connection dialInternal(String dialString, int videoState) throws CallStateException {
        log("dialInternal: dialString=" + hidePii(dialString));
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
            Connection c = this.mForegroundCall.dial(dialString);
            return c;
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

    public void conference(Call that) throws CallStateException {
        synchronized (SipPhone.class) {
            if (!(that instanceof SipCall)) {
                throw new CallStateException("expect " + SipCall.class + ", cannot merge with " + that.getClass());
            }
            this.mForegroundCall.merge((SipCall) that);
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
        } else {
            if (!this.mForegroundCall.getState().isAlive()) {
                return;
            }
            synchronized (SipPhone.class) {
                this.mForegroundCall.sendDtmf(c);
            }
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("startDtmf called with invalid character '" + c + "'");
        } else {
            sendDtmf(c);
        }
    }

    @Override
    public void stopDtmf() {
    }

    public void sendBurstDtmf(String dtmfString) {
        loge("sendBurstDtmf() is a CDMA method");
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        AsyncResult.forMessage(onComplete, (Object) null, (Throwable) null);
        onComplete.sendToTarget();
    }

    @Override
    public void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode, Message onComplete) {
        AsyncResult.forMessage(onComplete, (Object) null, (Throwable) null);
        onComplete.sendToTarget();
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        AsyncResult.forMessage(onComplete, (Object) null, (Throwable) null);
        onComplete.sendToTarget();
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        loge("call waiting not supported");
    }

    @Override
    public void setEchoSuppressionEnabled() {
        synchronized (SipPhone.class) {
            AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
            String echoSuppression = audioManager.getParameters("ec_supported");
            if (echoSuppression.contains("off")) {
                this.mForegroundCall.setAudioGroupMode();
            }
        }
    }

    @Override
    public void setMute(boolean muted) {
        synchronized (SipPhone.class) {
            this.mForegroundCall.setMute(muted);
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

    private String getUriString(SipProfile p) {
        return p.getUserName() + "@" + getSipDomain(p);
    }

    private String getSipDomain(SipProfile p) {
        String domain = p.getSipDomain();
        if (domain.endsWith(":5060")) {
            return domain.substring(0, domain.length() - 5);
        }
        return domain;
    }

    private static Call.State getCallStateFrom(SipAudioCall sipAudioCall) {
        if (sipAudioCall.isOnHold()) {
            return Call.State.HOLDING;
        }
        int sessionState = sipAudioCall.getState();
        switch (sessionState) {
            case 0:
                return Call.State.IDLE;
            case 1:
            case 2:
            default:
                slog("illegal connection state: " + sessionState);
                return Call.State.DISCONNECTED;
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
        }
    }

    private synchronized boolean isHoldTimeoutExpired() {
        long currTime = System.currentTimeMillis();
        if (currTime - this.mTimeOfLastValidHoldRequest > TIMEOUT_HOLD_PROCESSING) {
            this.mTimeOfLastValidHoldRequest = currTime;
            return true;
        }
        return false;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private static void slog(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    private void loge(String s, Exception e) {
        Rlog.e(LOG_TAG, s, e);
    }

    private class SipCall extends SipCallBase {
        private static final boolean SC_DBG = true;
        private static final String SC_TAG = "SipCall";
        private static final boolean SC_VDBG = false;

        SipCall(SipPhone this$0, SipCall sipCall) {
            this();
        }

        private SipCall() {
        }

        void reset() {
            log("reset");
            this.mConnections.clear();
            setState(Call.State.IDLE);
        }

        void switchWith(SipCall that) {
            log("switchWith");
            synchronized (SipPhone.class) {
                SipCall tmp = SipPhone.this.new SipCall();
                tmp.takeOver(this);
                takeOver(that);
                that.takeOver(tmp);
            }
        }

        private void takeOver(SipCall that) {
            log("takeOver");
            this.mConnections = that.mConnections;
            this.mState = that.mState;
            for (Connection c : this.mConnections) {
                ((SipConnection) c).changeOwner(this);
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

        Connection dial(String originalNumber) throws SipException {
            log("dial: num=xxx");
            String calleeSipUri = originalNumber;
            if (!originalNumber.contains("@")) {
                String replaceStr = Pattern.quote(SipPhone.this.mProfile.getUserName() + "@");
                calleeSipUri = SipPhone.this.mProfile.getUriString().replaceFirst(replaceStr, originalNumber + "@");
            }
            try {
                SipProfile callee = new SipProfile.Builder(calleeSipUri).build();
                SipConnection c = SipPhone.this.new SipConnection(this, callee, originalNumber);
                c.dial();
                this.mConnections.add(c);
                setState(Call.State.DIALING);
                return c;
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
                    CallStateException excp = null;
                    for (Connection c : this.mConnections) {
                        try {
                            c.hangup();
                        } catch (CallStateException e) {
                            excp = e;
                        }
                    }
                    if (excp != null) {
                        throw excp;
                    }
                } else {
                    log("hangup: dead call " + getState() + ": " + this + " on phone " + getPhone());
                }
            }
        }

        SipConnection initIncomingCall(SipAudioCall sipAudioCall, boolean makeCallWait) {
            SipProfile callee = sipAudioCall.getPeerProfile();
            SipConnection c = new SipConnection(SipPhone.this, this, callee);
            this.mConnections.add(c);
            Call.State newState = makeCallWait ? Call.State.WAITING : Call.State.INCOMING;
            c.initIncomingCall(sipAudioCall, newState);
            setState(newState);
            SipPhone.this.notifyNewRingingConnectionP(c);
            return c;
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
            Boolean ret = Boolean.valueOf(((AudioManager) SipPhone.this.mContext.getSystemService("audio")).isSpeakerphoneOn());
            return ret.booleanValue();
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
            for (Connection c : this.mConnections) {
                ((SipConnection) c).hold();
            }
            setAudioGroupMode();
        }

        void unhold() throws CallStateException {
            log("unhold:");
            setState(Call.State.ACTIVE);
            AudioGroup audioGroup = new AudioGroup();
            for (Connection c : this.mConnections) {
                ((SipConnection) c).unhold(audioGroup);
            }
            setAudioGroupMode();
        }

        void setMute(boolean muted) {
            log("setMute: muted=" + muted);
            for (Connection c : this.mConnections) {
                ((SipConnection) c).setMute(muted);
            }
        }

        boolean getMute() {
            boolean mute;
            if (this.mConnections.isEmpty()) {
                mute = false;
            } else {
                mute = ((SipConnection) this.mConnections.get(0)).getMute();
            }
            log("getMute: ret=" + mute);
            return mute;
        }

        void merge(SipCall that) throws CallStateException {
            log("merge:");
            AudioGroup audioGroup = getAudioGroup();
            Connection[] cc = (Connection[]) that.mConnections.toArray(new Connection[that.mConnections.size()]);
            for (Connection c : cc) {
                SipConnection conn = (SipConnection) c;
                add(conn);
                if (conn.getState() == Call.State.HOLDING) {
                    conn.unhold(audioGroup);
                }
            }
            that.setState(Call.State.IDLE);
        }

        private void add(SipConnection conn) {
            log("add:");
            SipCall call = conn.getCall();
            if (call == this) {
                return;
            }
            if (call != null) {
                call.mConnections.remove(conn);
            }
            this.mConnections.add(conn);
            conn.changeOwner(this);
        }

        void sendDtmf(char c) {
            log("sendDtmf: c=" + c);
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup == null) {
                log("sendDtmf: audioGroup == null, ignore c=" + c);
            } else {
                audioGroup.sendDtmf(convertDtmf(c));
            }
        }

        private int convertDtmf(char c) {
            int code = c - '0';
            if (code < 0 || code > 9) {
                switch (c) {
                    case '#':
                        return 11;
                    case '*':
                        return 10;
                    case CallFailCause.BEARER_NOT_IMPLEMENT:
                        return 12;
                    case 'B':
                        return 13;
                    case 'C':
                        return 14;
                    case CallFailCause.ACM_LIMIT_EXCEEDED:
                        return 15;
                    default:
                        throw new IllegalArgumentException("invalid DTMF char: " + ((int) c));
                }
            }
            return code;
        }

        @Override
        protected void setState(Call.State newState) {
            if (this.mState != newState) {
                log("setState: cur state" + this.mState + " --> " + newState + ": " + this + ": on phone " + getPhone() + " " + this.mConnections.size());
                if (newState == Call.State.ALERTING) {
                    this.mState = newState;
                    SipPhone.this.startRingbackTone();
                } else if (this.mState == Call.State.ALERTING) {
                    SipPhone.this.stopRingbackTone();
                }
                this.mState = newState;
                SipPhone.this.updatePhoneState();
                SipPhone.this.notifyPreciseCallStateChanged();
            }
        }

        void onConnectionStateChanged(SipConnection conn) {
            log("onConnectionStateChanged: conn=" + conn);
            if (this.mState == Call.State.ACTIVE) {
                return;
            }
            setState(conn.getState());
        }

        void onConnectionEnded(SipConnection conn) {
            log("onConnectionEnded: conn=" + conn);
            if (this.mState != Call.State.DISCONNECTED) {
                boolean allConnectionsDisconnected = true;
                log("---check connections: " + this.mConnections.size());
                Iterator c$iterator = this.mConnections.iterator();
                while (true) {
                    if (!c$iterator.hasNext()) {
                        break;
                    }
                    Connection c = (Connection) c$iterator.next();
                    log("   state=" + c.getState() + ": " + c);
                    if (c.getState() != Call.State.DISCONNECTED) {
                        allConnectionsDisconnected = false;
                        break;
                    }
                }
                if (allConnectionsDisconnected) {
                    setState(Call.State.DISCONNECTED);
                }
            }
            SipPhone.this.notifyDisconnectP(conn);
        }

        private AudioGroup getAudioGroup() {
            if (this.mConnections.isEmpty()) {
                return null;
            }
            return ((SipConnection) this.mConnections.get(0)).getAudioGroup();
        }

        private void log(String s) {
            Rlog.d(SC_TAG, s);
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

        public SipConnection(SipCall owner, SipProfile callee, String originalNumber) {
            super(originalNumber);
            this.mState = Call.State.IDLE;
            this.mIncoming = false;
            this.mAdapter = new SipAudioCallAdapter(SipPhone.this) {
                {
                    SipAudioCallAdapter sipAudioCallAdapter = null;
                }

                @Override
                protected void onCallEnded(int cause) {
                    if (SipConnection.this.getDisconnectCause() != 3) {
                        SipConnection.this.setDisconnectCause(cause);
                    }
                    synchronized (SipPhone.class) {
                        SipConnection.this.setState(Call.State.DISCONNECTED);
                        SipAudioCall sipAudioCall = SipConnection.this.mSipAudioCall;
                        SipConnection.this.mSipAudioCall = null;
                        String sessionState = sipAudioCall == null ? UsimPBMemInfo.STRING_NOT_SET : sipAudioCall.getState() + ", ";
                        SipConnection.this.log("[SipAudioCallAdapter] onCallEnded: " + SipPhone.hidePii(SipConnection.this.mPeer.getUriString()) + ": " + sessionState + "cause: " + SipConnection.this.getDisconnectCause() + ", on phone " + SipConnection.this.getPhone());
                        if (sipAudioCall != null) {
                            sipAudioCall.setListener(null);
                            sipAudioCall.close();
                        }
                        SipConnection.this.mOwner.onConnectionEnded(SipConnection.this);
                    }
                }

                @Override
                public void onCallEstablished(SipAudioCall call) {
                    onChanged(call);
                    if (SipConnection.this.mState == Call.State.ACTIVE) {
                        call.startAudio();
                    }
                }

                @Override
                public void onCallHeld(SipAudioCall call) {
                    onChanged(call);
                    if (SipConnection.this.mState == Call.State.HOLDING) {
                        call.startAudio();
                    }
                }

                @Override
                public void onChanged(SipAudioCall call) {
                    synchronized (SipPhone.class) {
                        Call.State newState = SipPhone.getCallStateFrom(call);
                        if (SipConnection.this.mState == newState) {
                            return;
                        }
                        if (newState == Call.State.INCOMING) {
                            SipConnection.this.setState(SipConnection.this.mOwner.getState());
                        } else {
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
                            SipConnection.this.setState(newState);
                        }
                        SipConnection.this.mOwner.onConnectionStateChanged(SipConnection.this);
                        SipConnection.this.log("onChanged: " + SipConnection.this.mPeer.getUriString() + ": " + SipConnection.this.mState + " on phone " + SipConnection.this.getPhone());
                    }
                }

                @Override
                protected void onError(int cause) {
                    SipConnection.this.log("onError: " + cause);
                    onCallEnded(cause);
                }
            };
            this.mOwner = owner;
            this.mPeer = callee;
            this.mOriginalNumber = originalNumber;
        }

        public SipConnection(SipPhone this$0, SipCall owner, SipProfile callee) {
            this(owner, callee, this$0.getUriString(callee));
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

        void initIncomingCall(SipAudioCall sipAudioCall, Call.State newState) {
            setState(newState);
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

        void changeOwner(SipCall owner) {
            this.mOwner = owner;
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

        void setMute(boolean muted) {
            if (this.mSipAudioCall == null || muted == this.mSipAudioCall.isMuted()) {
                return;
            }
            log("setState: prev muted=" + (!muted) + " new muted=" + muted);
            this.mSipAudioCall.toggleMute();
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
                log("hangup: conn=" + this.mPeer.getUriString() + ": " + this.mState + ": on phone " + getPhone().getPhoneName());
                if (this.mState.isAlive()) {
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
                        this.mAdapter.onCallEnded((this.mState == Call.State.INCOMING || this.mState == Call.State.WAITING) ? 16 : 3);
                    }
                }
            }
        }

        @Override
        public void separate() throws CallStateException {
            synchronized (SipPhone.class) {
                SipCall call = getPhone() == SipPhone.this ? (SipCall) SipPhone.this.getBackgroundCall() : (SipCall) SipPhone.this.getForegroundCall();
                if (call.getState() != Call.State.IDLE) {
                    throw new CallStateException("cannot put conn back to a call in non-idle state: " + call.getState());
                }
                log("separate: conn=" + this.mPeer.getUriString() + " from " + this.mOwner + " back to " + call);
                Phone originalPhone = getPhone();
                AudioGroup audioGroup = call.getAudioGroup();
                call.add(this);
                this.mSipAudioCall.setAudioGroup(audioGroup);
                originalPhone.switchHoldingAndActive();
                SipCall call2 = (SipCall) SipPhone.this.getForegroundCall();
                this.mSipAudioCall.startAudio();
                call2.onConnectionStateChanged(this);
            }
        }

        private void log(String s) {
            Rlog.d(SCN_TAG, s);
        }
    }

    private abstract class SipAudioCallAdapter extends SipAudioCall.Listener {
        private static final boolean SACA_DBG = true;
        private static final String SACA_TAG = "SipAudioCallAdapter";

        SipAudioCallAdapter(SipPhone this$0, SipAudioCallAdapter sipAudioCallAdapter) {
            this();
        }

        protected abstract void onCallEnded(int i);

        protected abstract void onError(int i);

        private SipAudioCallAdapter() {
        }

        @Override
        public void onCallEnded(SipAudioCall call) {
            int i;
            log("onCallEnded: call=" + call);
            if (call.isInCall()) {
                i = 2;
            } else {
                i = 1;
            }
            onCallEnded(i);
        }

        @Override
        public void onCallBusy(SipAudioCall call) {
            log("onCallBusy: call=" + call);
            onCallEnded(4);
        }

        @Override
        public void onError(SipAudioCall call, int errorCode, String errorMessage) {
            log("onError: call=" + call + " code=" + SipErrorCode.toString(errorCode) + ": " + errorMessage);
            switch (errorCode) {
                case IccProvider.ERROR_ICC_PROVIDER_EMAIL_FULL:
                    onError(9);
                    break;
                case IccProvider.ERROR_ICC_PROVIDER_ADN_LIST_NOT_EXIST:
                    onError(11);
                    break;
                case -10:
                    onError(14);
                    break;
                case -9:
                case IccProvider.ERROR_ICC_PROVIDER_NOT_READY:
                default:
                    onError(36);
                    break;
                case -8:
                    onError(10);
                    break;
                case -7:
                    onError(8);
                    break;
                case IccProvider.ERROR_ICC_PROVIDER_ANR_TOO_LONG:
                    onError(7);
                    break;
                case IccProvider.ERROR_ICC_PROVIDER_PASSWORD_ERROR:
                case -3:
                    onError(13);
                    break;
                case -2:
                    onError(12);
                    break;
            }
        }

        private void log(String s) {
            Rlog.d(SACA_TAG, s);
        }
    }

    public static String hidePii(String s) {
        return "xxxxx";
    }
}
