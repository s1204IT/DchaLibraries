package com.mediatek.internal.telephony.imsphone;

import android.net.Uri;
import android.os.Looper;
import android.os.SystemProperties;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallProfile;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.imsphone.ImsRttTextHandler;
import com.mediatek.ims.MtkImsCall;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.imsphone.op.OpImsPhoneConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MtkImsPhoneConnection extends ImsPhoneConnection {
    private static final String LOG_TAG = "MtkImsPhoneConnection";
    private static final String PROPERTY_HD_VOICE_STATUS = "vendor.audiohal.ril.hd.voice.status";
    public static final int SUPPORTS_VT_RINGTONE = 64;
    private int mCallIdBeforeDisconnected;
    private ArrayList<String> mConfDialStrings;
    private List<ConferenceParticipant> mConferenceParticipants;
    private OpImsPhoneConnection mOpImsPhoneConnection;
    private String mVendorCause;
    public boolean mWasMultiparty;
    public boolean mWasPreMultipartyHost;

    public enum AUDIO_CODEC {
        UNDEFINED,
        HD,
        NOT_HD
    }

    public MtkImsPhoneConnection(Phone phone, ImsCall imsCall, ImsPhoneCallTracker imsPhoneCallTracker, ImsPhoneCall imsPhoneCall, boolean z) {
        super(phone, imsCall, imsPhoneCallTracker, imsPhoneCall, z);
        this.mConfDialStrings = null;
        this.mConferenceParticipants = null;
        this.mCallIdBeforeDisconnected = -1;
        this.mVendorCause = null;
        this.mWasMultiparty = false;
        this.mWasPreMultipartyHost = false;
        this.mOpImsPhoneConnection = OpTelephonyCustomizationUtils.getOpCommonInstance().makeOpImsPhoneConnection();
    }

    public MtkImsPhoneConnection(Phone phone, String str, ImsPhoneCallTracker imsPhoneCallTracker, ImsPhoneCall imsPhoneCall, boolean z) {
        super(phone, str, imsPhoneCallTracker, imsPhoneCall, z);
        this.mConfDialStrings = null;
        this.mConferenceParticipants = null;
        this.mCallIdBeforeDisconnected = -1;
        this.mVendorCause = null;
        this.mWasMultiparty = false;
        this.mWasPreMultipartyHost = false;
        if (PhoneNumberUtils.isUriNumber(str)) {
            this.mAddress = str;
            this.mPostDialString = "";
        }
        this.mOpImsPhoneConnection = OpTelephonyCustomizationUtils.getOpCommonInstance().makeOpImsPhoneConnection();
    }

    public String getVendorDisconnectCause() {
        return this.mVendorCause;
    }

    public void hangup() throws CallStateException {
        if (this.mOwner != null && (this.mOwner instanceof MtkImsPhoneCallTracker)) {
            ((MtkImsPhoneCallTracker) this.mOwner).logDebugMessagesWithOpFormat("CC", "Hangup", this, "MtkImsphoneConnection.hangup");
        }
        super.hangup();
    }

    public boolean onDisconnect() {
        if (!this.mDisconnected) {
            this.mCallIdBeforeDisconnected = getCallId();
        }
        return super.onDisconnect();
    }

    public void onDisconnectConferenceParticipant(Uri uri) {
        if (this.mOwner != null && (this.mOwner instanceof MtkImsPhoneCallTracker)) {
            ((MtkImsPhoneCallTracker) this.mOwner).logDebugMessagesWithOpFormat("CC", "RemoveMember", this, " remove: " + uri);
        }
        super.onDisconnectConferenceParticipant(uri);
    }

    public boolean updateAddressDisplay(ImsCall imsCall) {
        boolean zUpdateAddressDisplay = super.updateAddressDisplay(imsCall);
        if (zUpdateAddressDisplay) {
            setConnectionAddressDisplay();
        }
        return zUpdateAddressDisplay;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" state:" + getState());
        sb.append(" mParent:");
        sb.append(getParentCallName());
        return sb.toString();
    }

    int getCallId() {
        ImsCall imsCall = getImsCall();
        if (imsCall == null || imsCall.getCallSession() == null) {
            return -1;
        }
        String callId = imsCall.getCallSession().getCallId();
        if (callId == null) {
            Rlog.d(LOG_TAG, "Abnormal! Call Id = null");
            return -1;
        }
        return Integer.parseInt(callId);
    }

    int getCallIdBeforeDisconnected() {
        return this.mCallIdBeforeDisconnected;
    }

    ArrayList<String> getConfDialStrings() {
        return this.mConfDialStrings;
    }

    public String getConferenceParticipantAddress(int i) {
        int i2;
        if (this.mConferenceParticipants == null) {
            Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): no XML information");
            return "";
        }
        if (i < 0 || (i2 = i + 1) >= this.mConferenceParticipants.size()) {
            Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): invalid index");
            return "";
        }
        ConferenceParticipant conferenceParticipant = this.mConferenceParticipants.get(i2);
        if (conferenceParticipant == null) {
            Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): empty participant info");
            return "";
        }
        Uri handle = conferenceParticipant.getHandle();
        Rlog.d(LOG_TAG, "getConferenceParticipantAddress(): ret=" + handle);
        return handle.toString();
    }

    String getParentCallName() {
        if (this.mOwner == null) {
            return "Unknown";
        }
        if (this.mParent == this.mOwner.mForegroundCall) {
            return "Foreground Call";
        }
        if (this.mParent == this.mOwner.mBackgroundCall) {
            return "Background Call";
        }
        if (this.mParent == this.mOwner.mRingingCall) {
            return "Ringing Call";
        }
        if (this.mParent == this.mOwner.mHandoverCall) {
            return "Handover Call";
        }
        return "Abnormal";
    }

    public boolean isConfHostBeforeHandover() {
        return this.mWasPreMultipartyHost;
    }

    public boolean isMultipartyBeforeHandover() {
        return this.mWasMultiparty;
    }

    public synchronized boolean isIncomingCallMultiparty() {
        boolean z;
        if (this.mImsCall == null || !(this.mImsCall instanceof MtkImsCall)) {
            z = false;
        } else if (this.mImsCall.isIncomingCallMultiparty()) {
            z = true;
        }
        return z;
    }

    public void inviteConferenceParticipants(List<String> list) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(", ");
        }
        if (this.mOwner != null && (this.mOwner instanceof MtkImsPhoneCallTracker)) {
            ((MtkImsPhoneCallTracker) this.mOwner).logDebugMessagesWithOpFormat("CC", "AddMember", this, " invite with " + sb.toString());
        }
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<String> it2 = list.iterator();
        while (it2.hasNext()) {
            arrayList.add(PhoneNumberUtils.extractNetworkPortionAlt(it2.next()));
        }
        String[] strArr = (String[]) arrayList.toArray(new String[arrayList.size()]);
        try {
            imsCall.inviteParticipants(strArr);
        } catch (ImsException e) {
            Rlog.e(LOG_TAG, "inviteConferenceParticipants: no call session and fail to invite participants " + Arrays.toString(strArr));
        }
    }

    void setConfDialStrings(ArrayList<String> arrayList) {
        this.mConfDialStrings = arrayList;
    }

    void setConferenceAsHost() {
        Rlog.d(LOG_TAG, "set is conference host connection: " + this);
        this.mIsIncoming = false;
    }

    void setVendorDisconnectCause(String str) {
        this.mVendorCause = str;
    }

    public void updateConferenceParticipants(List<ConferenceParticipant> list) {
        this.mConferenceParticipants = list;
        super.updateConferenceParticipants(list);
    }

    public void unhold() throws CallStateException {
        if (this.mOwner != null && (this.mOwner instanceof MtkImsPhoneCallTracker)) {
            ((MtkImsPhoneCallTracker) this.mOwner).unhold(this);
        }
    }

    public static abstract class MtkListenerBase extends Connection.ListenerBase {
        public void onConferenceParticipantsInvited(boolean z) {
        }

        public void onConferenceConnectionsConfigured(ArrayList<Connection> arrayList) {
        }

        public void onDeviceSwitched(boolean z) {
        }

        public void onRemoteHeld(boolean z) {
        }

        public void onAddressDisplayChanged() {
        }

        public void onTextCapabilityChanged(int i, int i2, int i3, int i4) {
        }
    }

    void notifyConferenceParticipantsInvited(boolean z) {
        for (MtkListenerBase mtkListenerBase : this.mListeners) {
            if (mtkListenerBase instanceof MtkListenerBase) {
                mtkListenerBase.onConferenceParticipantsInvited(z);
            }
        }
    }

    public void notifyConferenceConnectionsConfigured(ArrayList<Connection> arrayList) {
        for (MtkListenerBase mtkListenerBase : this.mListeners) {
            if (mtkListenerBase instanceof MtkListenerBase) {
                mtkListenerBase.onConferenceConnectionsConfigured(arrayList);
            }
        }
    }

    public void notifyDeviceSwitched(boolean z) {
        for (MtkListenerBase mtkListenerBase : this.mListeners) {
            if (mtkListenerBase instanceof MtkListenerBase) {
                mtkListenerBase.onDeviceSwitched(z);
            }
        }
    }

    void notifyRemoteHeld(boolean z) {
        Rlog.d(LOG_TAG, "Connection: notify remote hold");
        for (MtkListenerBase mtkListenerBase : this.mListeners) {
            if (mtkListenerBase instanceof MtkListenerBase) {
                mtkListenerBase.onRemoteHeld(z);
            }
        }
    }

    private void setConnectionAddressDisplay() {
        for (MtkListenerBase mtkListenerBase : this.mListeners) {
            if (mtkListenerBase instanceof MtkListenerBase) {
                mtkListenerBase.onAddressDisplayChanged();
            }
        }
    }

    class RttNetworkWriter implements ImsRttTextHandler.NetworkWriter {
        RttNetworkWriter() {
        }

        public void write(String str) {
            ImsCall imsCall = MtkImsPhoneConnection.this.getImsCall();
            if (imsCall != null) {
                imsCall.sendRttMessage(str);
                return;
            }
            Rlog.d(MtkImsPhoneConnection.LOG_TAG, "getImsCall() is null, cannot send msg: " + str);
        }
    }

    protected void createRttTextHandler() {
        this.mRttTextHandler = new ImsRttTextHandler(Looper.getMainLooper(), new RttNetworkWriter());
        this.mRttTextHandler.initialize(this.mRttTextStream);
    }

    public void updateTextCapability(int i, int i2, int i3, int i4) {
        this.mOpImsPhoneConnection.updateTextCapability(this.mListeners, i, i2, i3, i4);
    }

    protected void checkIncomingRejected(int i) {
        if (isIncoming() && getConnectTime() == 0 && this.mCause == 3) {
            this.mCause = 16;
        }
    }

    protected int applyVideoRingtoneCapabilities(ImsCallProfile imsCallProfile, int i) {
        if (imsCallProfile.mMediaProfile.mVideoDirection == 1) {
            Rlog.d(LOG_TAG, "Set video ringtone capability");
            return addCapability(i, 64);
        }
        return removeCapability(i, 64);
    }

    protected boolean skipSwitchingCallToForeground() {
        if (this.mParent != this.mOwner.mHandoverCall) {
            Rlog.d(LOG_TAG, "update() - Switch Connection to foreground call:" + this);
            return false;
        }
        return true;
    }

    protected void switchCallToBackgroundIfNecessary() {
        if (this.mParent == this.mOwner.mForegroundCall) {
            Rlog.d(LOG_TAG, "update() - Switch Connection to background call:" + this);
            this.mParent.detach(this);
            this.mParent = this.mOwner.mBackgroundCall;
            this.mParent.attach(this);
        }
    }

    protected int calNumberPresentation(ImsCallProfile imsCallProfile) {
        int iOIRToPresentation = ImsCallProfile.OIRToPresentation(imsCallProfile.getCallExtraInt("oir"));
        if (!this.mIsIncoming) {
            return 1;
        }
        return iOIRToPresentation;
    }

    protected boolean needUpdateAddress(String str) {
        if (!equalsBaseDialString(this.mAddress, str)) {
            Rlog.d(LOG_TAG, "update address = " + str + " isMpty = " + isMultiparty());
            if (!TextUtils.isEmpty(str)) {
                return true;
            }
            return false;
        }
        return false;
    }

    protected boolean allowedUpdateMOAddress() {
        return true;
    }

    public void stopRttTextProcessing() {
        this.mOpImsPhoneConnection.stopRttTextProcessing(this.mRttTextHandler, this);
    }

    public void sendRttDowngradeRequest() {
        this.mOpImsPhoneConnection.sendRttDowngradeRequest((MtkImsCall) this.mImsCall, this.mRttTextHandler, this);
    }

    public void setRttIncomingCall(boolean z) {
        this.mOpImsPhoneConnection.setRttIncomingCall(z);
    }

    public boolean isIncomingRtt() {
        return this.mOpImsPhoneConnection.isIncomingRtt();
    }

    public void setIncomingRttDuringEmcGuard(boolean z) {
        this.mOpImsPhoneConnection.setIncomingRttDuringEmcGuard(z);
    }

    public boolean isIncomingRttDuringEmcGuard() {
        return this.mOpImsPhoneConnection.isIncomingRttDuringEmcGuard();
    }

    public synchronized void setImsCall(ImsCall imsCall) {
        super.setImsCall(imsCall);
        this.mOpImsPhoneConnection.setImsCall(imsCall);
    }

    public void sendRttModifyRequest(Connection.RttTextStream rttTextStream) {
        if (!this.mOpImsPhoneConnection.onSendRttModifyRequest(this)) {
            getImsCall().sendRttModifyRequest();
        }
        setCurrentRttTextStream(rttTextStream);
    }

    public void setCurrentRttTextStream(Connection.RttTextStream rttTextStream) {
        Rlog.d(LOG_TAG, "setCurrentRttTextStream = " + rttTextStream);
        super.setCurrentRttTextStream(rttTextStream);
    }

    protected int getAudioQualityFromCallProfile(ImsCallProfile imsCallProfile, ImsCallProfile imsCallProfile2) {
        if (imsCallProfile == null || imsCallProfile2 == null || imsCallProfile.mMediaProfile == null) {
            return 1;
        }
        AUDIO_CODEC highDefAudioInfoFromSysProp = getHighDefAudioInfoFromSysProp();
        boolean z = false;
        boolean z2 = imsCallProfile.mMediaProfile.mAudioQuality == 2 || imsCallProfile.mMediaProfile.mAudioQuality == 6 || (imsCallProfile.mMediaProfile.mAudioQuality == 18 || imsCallProfile.mMediaProfile.mAudioQuality == 19 || imsCallProfile.mMediaProfile.mAudioQuality == 20);
        if (highDefAudioInfoFromSysProp != AUDIO_CODEC.UNDEFINED) {
            z2 = highDefAudioInfoFromSysProp == AUDIO_CODEC.HD;
        }
        if (z2 && imsCallProfile2.mRestrictCause == 0) {
            z = true;
        }
        return z ? 2 : 1;
    }

    private AUDIO_CODEC getHighDefAudioInfoFromSysProp() {
        String str = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "OM");
        Rlog.d(LOG_TAG, "isHighDefAudio, optr:" + str);
        String strConcat = str.concat("=");
        String str2 = SystemProperties.get(PROPERTY_HD_VOICE_STATUS, "");
        if (str2 != null && !str2.equals("")) {
            Rlog.d(LOG_TAG, "HD voice status: " + str2);
            int iIndexOf = 0;
            boolean z = str2.indexOf(strConcat) != -1;
            boolean z2 = str2.indexOf("OM=") != -1;
            if (z && !strConcat.equals("OM=")) {
                iIndexOf = str2.indexOf(strConcat) + strConcat.length();
            } else if (z2) {
                iIndexOf = str2.indexOf("OM=") + 3;
            }
            int i = iIndexOf + 1;
            if ((str2.length() > i ? str2.substring(iIndexOf, i) : "").equals("Y")) {
                return AUDIO_CODEC.HD;
            }
            return AUDIO_CODEC.NOT_HD;
        }
        return AUDIO_CODEC.UNDEFINED;
    }
}
