package com.mediatek.internal.telephony;

import android.hardware.radio.V1_0.DataCallFailCause;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.mediatek.android.mms.pdu.MtkCharacterSets;

public class MtkCallManager extends CallManager {

    public class MtkCallManagerHandler extends CallManager.CallManagerHandler {
        public MtkCallManagerHandler() {
            super(MtkCallManager.this);
        }

        public void handleMessage(Message message) {
            if (message.what == 102) {
                if (!MtkCallManager.this.getActiveFgCallState(((Connection) ((AsyncResult) message.obj).result).getCall().getPhone().getSubId()).isDialing() && !MtkCallManager.this.hasMoreThanOneRingingCall()) {
                    MtkCallManager.this.mNewRingingConnectionRegistrants.notifyRegistrants((AsyncResult) message.obj);
                    return;
                }
                return;
            }
            super.handleMessage(message);
        }
    }

    protected void registerForPhoneStates(Phone phone) {
        if (((CallManager.CallManagerHandler) this.mHandlerMap.get(phone)) != null) {
            Rlog.d("CallManager", "This phone has already been registered.");
            return;
        }
        CallManager.CallManagerHandler mtkCallManagerHandler = new MtkCallManagerHandler();
        this.mHandlerMap.put(phone, mtkCallManagerHandler);
        phone.registerForPreciseCallStateChanged(mtkCallManagerHandler, 101, this.mRegistrantidentifier);
        phone.registerForDisconnect(mtkCallManagerHandler, 100, this.mRegistrantidentifier);
        phone.registerForNewRingingConnection(mtkCallManagerHandler, 102, this.mRegistrantidentifier);
        phone.registerForUnknownConnection(mtkCallManagerHandler, 103, this.mRegistrantidentifier);
        phone.registerForIncomingRing(mtkCallManagerHandler, MtkCharacterSets.ISO_2022_CN, this.mRegistrantidentifier);
        phone.registerForRingbackTone(mtkCallManagerHandler, MtkCharacterSets.ISO_2022_CN_EXT, this.mRegistrantidentifier);
        phone.registerForInCallVoicePrivacyOn(mtkCallManagerHandler, 106, this.mRegistrantidentifier);
        phone.registerForInCallVoicePrivacyOff(mtkCallManagerHandler, 107, this.mRegistrantidentifier);
        phone.registerForDisplayInfo(mtkCallManagerHandler, 109, this.mRegistrantidentifier);
        phone.registerForSignalInfo(mtkCallManagerHandler, 110, this.mRegistrantidentifier);
        phone.registerForResendIncallMute(mtkCallManagerHandler, 112, this.mRegistrantidentifier);
        phone.registerForMmiInitiate(mtkCallManagerHandler, 113, this.mRegistrantidentifier);
        phone.registerForMmiComplete(mtkCallManagerHandler, 114, this.mRegistrantidentifier);
        phone.registerForSuppServiceFailed(mtkCallManagerHandler, DataCallFailCause.IFACE_MISMATCH, this.mRegistrantidentifier);
        phone.registerForServiceStateChanged(mtkCallManagerHandler, DataCallFailCause.COMPANION_IFACE_IN_USE, this.mRegistrantidentifier);
        phone.setOnPostDialCharacter(mtkCallManagerHandler, DataCallFailCause.IP_ADDRESS_MISMATCH, (Object) null);
        phone.registerForCdmaOtaStatusChange(mtkCallManagerHandler, 111, (Object) null);
        phone.registerForSubscriptionInfoReady(mtkCallManagerHandler, DataCallFailCause.EMERGENCY_IFACE_ONLY, (Object) null);
        phone.registerForCallWaiting(mtkCallManagerHandler, 108, (Object) null);
        phone.registerForEcmTimerReset(mtkCallManagerHandler, DataCallFailCause.EMM_ACCESS_BARRED, (Object) null);
        phone.registerForOnHoldTone(mtkCallManagerHandler, DataCallFailCause.IFACE_AND_POL_FAMILY_MISMATCH, (Object) null);
        phone.registerForSuppServiceFailed(mtkCallManagerHandler, DataCallFailCause.IFACE_MISMATCH, (Object) null);
        phone.registerForTtyModeReceived(mtkCallManagerHandler, DataCallFailCause.AUTH_FAILURE_ON_EMERGENCY_CALL, (Object) null);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(i);
            sb.append("CallManager {");
            sb.append("\nstate = " + getState(subIdUsingPhoneId));
            Call activeFgCall = getActiveFgCall(subIdUsingPhoneId);
            if (activeFgCall != null) {
                sb.append("\n- Foreground: " + getActiveFgCallState(subIdUsingPhoneId));
                sb.append(" from " + activeFgCall.getPhone());
                sb.append("\n  Conn: ");
                sb.append(getFgCallConnections(subIdUsingPhoneId));
            }
            Call firstActiveBgCall = getFirstActiveBgCall(subIdUsingPhoneId);
            if (firstActiveBgCall != null) {
                sb.append("\n- Background: " + firstActiveBgCall.getState());
                sb.append(" from " + firstActiveBgCall.getPhone());
                sb.append("\n  Conn: ");
                sb.append(getBgCallConnections(subIdUsingPhoneId));
            }
            Call firstActiveRingingCall = getFirstActiveRingingCall(subIdUsingPhoneId);
            if (firstActiveRingingCall != null) {
                sb.append("\n- Ringing: " + firstActiveRingingCall.getState());
                sb.append(" from " + firstActiveRingingCall.getPhone());
            }
        }
        for (Phone phone : getAllPhones()) {
            if (phone != null) {
                sb.append("\nPhone: " + phone + ", name = " + phone.getPhoneName() + ", state = " + phone.getState());
                Call foregroundCall = phone.getForegroundCall();
                if (foregroundCall != null) {
                    sb.append("\n- Foreground: ");
                    sb.append(foregroundCall);
                }
                Call backgroundCall = phone.getBackgroundCall();
                if (backgroundCall != null) {
                    sb.append(" Background: ");
                    sb.append(backgroundCall);
                }
                Call ringingCall = phone.getRingingCall();
                if (ringingCall != null) {
                    sb.append(" Ringing: ");
                    sb.append(ringingCall);
                }
            }
        }
        sb.append("\n}");
        return sb.toString();
    }
}
