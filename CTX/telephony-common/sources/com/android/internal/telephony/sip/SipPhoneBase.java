package com.android.internal.telephony.sip;

import android.content.Context;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RegistrantList;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.telephony.CellLocation;
import android.telephony.NetworkScanRequest;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.util.ArrayList;
import java.util.List;

abstract class SipPhoneBase extends Phone {
    private static final String LOG_TAG = "SipPhoneBase";
    private RegistrantList mRingbackRegistrants;
    private PhoneConstants.State mState;

    public abstract Call getBackgroundCall();

    public abstract Call getForegroundCall();

    public abstract Call getRingingCall();

    public SipPhoneBase(String str, Context context, PhoneNotifier phoneNotifier) {
        super(str, phoneNotifier, context, new SipCommandInterface(context), false);
        this.mRingbackRegistrants = new RegistrantList();
        this.mState = PhoneConstants.State.IDLE;
    }

    void migrateFrom(SipPhoneBase sipPhoneBase) {
        super.migrateFrom((Phone) sipPhoneBase);
        migrate(this.mRingbackRegistrants, sipPhoneBase.mRingbackRegistrants);
    }

    @Override
    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        this.mRingbackRegistrants.addUnique(handler, i, obj);
    }

    @Override
    public void unregisterForRingbackTone(Handler handler) {
        this.mRingbackRegistrants.remove(handler);
    }

    @Override
    public void startRingbackTone() {
        this.mRingbackRegistrants.notifyRegistrants(new AsyncResult((Object) null, Boolean.TRUE, (Throwable) null));
    }

    @Override
    public void stopRingbackTone() {
        this.mRingbackRegistrants.notifyRegistrants(new AsyncResult((Object) null, Boolean.FALSE, (Throwable) null));
    }

    public ServiceState getServiceState() {
        ServiceState serviceState = new ServiceState();
        serviceState.setVoiceRegState(0);
        return serviceState;
    }

    public CellLocation getCellLocation(WorkSource workSource) {
        return null;
    }

    @Override
    public PhoneConstants.State getState() {
        return this.mState;
    }

    @Override
    public int getPhoneType() {
        return 3;
    }

    @Override
    public SignalStrength getSignalStrength() {
        return new SignalStrength();
    }

    @Override
    public boolean getMessageWaitingIndicator() {
        return false;
    }

    @Override
    public boolean getCallForwardingIndicator() {
        return false;
    }

    public List<? extends MmiCode> getPendingMmiCodes() {
        return new ArrayList(0);
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState() {
        return PhoneConstants.DataState.DISCONNECTED;
    }

    public PhoneConstants.DataState getDataConnectionState(String str) {
        return PhoneConstants.DataState.DISCONNECTED;
    }

    public PhoneInternalInterface.DataActivityState getDataActivityState() {
        return PhoneInternalInterface.DataActivityState.NONE;
    }

    void notifyPhoneStateChanged() {
    }

    void notifyPreciseCallStateChanged() {
        super.notifyPreciseCallStateChangedP();
    }

    void notifyNewRingingConnection(Connection connection) {
        super.notifyNewRingingConnectionP(connection);
    }

    void notifyDisconnect(Connection connection) {
        this.mDisconnectRegistrants.notifyResult(connection);
    }

    void notifyUnknownConnection() {
        this.mUnknownConnectionRegistrants.notifyResult(this);
    }

    void notifySuppServiceFailed(PhoneInternalInterface.SuppService suppService) {
        this.mSuppServiceFailedRegistrants.notifyResult(suppService);
    }

    void notifyServiceStateChanged(ServiceState serviceState) {
        super.notifyServiceStateChangedP(serviceState);
    }

    @Override
    public void notifyCallForwardingIndicator() {
        this.mNotifier.notifyCallForwardingChanged(this);
    }

    public boolean canDial() {
        int state = getServiceState().getState();
        Rlog.v(LOG_TAG, "canDial(): serviceState = " + state);
        if (state == 3) {
            return false;
        }
        String str = SystemProperties.get("ro.telephony.disable-call", "false");
        Rlog.v(LOG_TAG, "canDial(): disableCall = " + str);
        if (str.equals("true")) {
            return false;
        }
        Rlog.v(LOG_TAG, "canDial(): ringingCall: " + getRingingCall().getState());
        Rlog.v(LOG_TAG, "canDial(): foregndCall: " + getForegroundCall().getState());
        Rlog.v(LOG_TAG, "canDial(): backgndCall: " + getBackgroundCall().getState());
        if (getRingingCall().isRinging()) {
            return false;
        }
        return (getForegroundCall().getState().isAlive() && getBackgroundCall().getState().isAlive()) ? false : true;
    }

    public boolean handleInCallMmiCommands(String str) {
        return false;
    }

    boolean isInCall() {
        return getForegroundCall().getState().isAlive() || getBackgroundCall().getState().isAlive() || getRingingCall().getState().isAlive();
    }

    public boolean handlePinMmi(String str) {
        return false;
    }

    public boolean handleUssdRequest(String str, ResultReceiver resultReceiver) {
        return false;
    }

    public void sendUssdResponse(String str) {
    }

    public void registerForSuppServiceNotification(Handler handler, int i, Object obj) {
    }

    public void unregisterForSuppServiceNotification(Handler handler) {
    }

    public void setRadioPower(boolean z) {
    }

    public String getVoiceMailNumber() {
        return null;
    }

    public String getVoiceMailAlphaTag() {
        return null;
    }

    public String getDeviceId() {
        return null;
    }

    public String getDeviceSvn() {
        return null;
    }

    public String getImei() {
        return null;
    }

    public String getEsn() {
        Rlog.e(LOG_TAG, "[SipPhone] getEsn() is a CDMA method");
        return "0";
    }

    public String getMeid() {
        Rlog.e(LOG_TAG, "[SipPhone] getMeid() is a CDMA method");
        return "0";
    }

    public String getSubscriberId() {
        return null;
    }

    public String getGroupIdLevel1() {
        return null;
    }

    public String getGroupIdLevel2() {
        return null;
    }

    @Override
    public String getIccSerialNumber() {
        return null;
    }

    public String getLine1Number() {
        return null;
    }

    public String getLine1AlphaTag() {
        return null;
    }

    public boolean setLine1Number(String str, String str2, Message message) {
        return false;
    }

    public void setVoiceMailNumber(String str, String str2, Message message) {
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    public void getCallForwardingOption(int i, Message message) {
    }

    public void setCallForwardingOption(int i, int i2, String str, int i3, Message message) {
    }

    public void getOutgoingCallerIdDisplay(Message message) {
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    public void setOutgoingCallerIdDisplay(int i, Message message) {
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    public void getCallWaiting(Message message) {
        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
        message.sendToTarget();
    }

    public void setCallWaiting(boolean z, Message message) {
        Rlog.e(LOG_TAG, "call waiting not supported");
    }

    @Override
    public boolean getIccRecordsLoaded() {
        return false;
    }

    @Override
    public IccCard getIccCard() {
        return null;
    }

    public void getAvailableNetworks(Message message) {
    }

    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
    }

    public void stopNetworkScan(Message message) {
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message message) {
    }

    @Override
    public void selectNetworkManually(OperatorInfo operatorInfo, boolean z, Message message) {
    }

    @Override
    public void setOnPostDialCharacter(Handler handler, int i, Object obj) {
    }

    public void updateServiceLocation() {
    }

    public void enableLocationUpdates() {
    }

    public void disableLocationUpdates() {
    }

    public boolean getDataRoamingEnabled() {
        return false;
    }

    public void setDataRoamingEnabled(boolean z) {
    }

    public boolean isUserDataEnabled() {
        return false;
    }

    public boolean isDataEnabled() {
        return false;
    }

    public void setUserDataEnabled(boolean z) {
    }

    public boolean enableDataConnectivity() {
        return false;
    }

    public boolean disableDataConnectivity() {
        return false;
    }

    @Override
    public boolean isDataAllowed() {
        return false;
    }

    @Override
    public void saveClirSetting(int i) {
    }

    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return null;
    }

    @Override
    public IccFileHandler getIccFileHandler() {
        return null;
    }

    public void activateCellBroadcastSms(int i, Message message) {
        Rlog.e(LOG_TAG, "Error! This functionality is not implemented for SIP.");
    }

    public void getCellBroadcastSmsConfig(Message message) {
        Rlog.e(LOG_TAG, "Error! This functionality is not implemented for SIP.");
    }

    public void setCellBroadcastSmsConfig(int[] iArr, Message message) {
        Rlog.e(LOG_TAG, "Error! This functionality is not implemented for SIP.");
    }

    @Override
    public boolean needsOtaServiceProvisioning() {
        return false;
    }

    @Override
    public LinkProperties getLinkProperties(String str) {
        return null;
    }

    @Override
    public boolean isVideoEnabled() {
        return false;
    }

    void updatePhoneState() {
        PhoneConstants.State state = this.mState;
        if (getRingingCall().isRinging()) {
            this.mState = PhoneConstants.State.RINGING;
        } else if (getForegroundCall().isIdle() && getBackgroundCall().isIdle()) {
            this.mState = PhoneConstants.State.IDLE;
        } else {
            this.mState = PhoneConstants.State.OFFHOOK;
        }
        if (this.mState != state) {
            Rlog.d(LOG_TAG, " ^^^ new phone state: " + this.mState);
            notifyPhoneStateChanged();
        }
    }

    @Override
    protected void onUpdateIccAvailability() {
    }

    @Override
    public void sendEmergencyCallStateChange(boolean z) {
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean z) {
    }

    public void getCallBarring(String str, String str2, Message message, int i) {
    }

    public void setCallBarring(String str, boolean z, String str2, Message message, int i) {
    }
}
