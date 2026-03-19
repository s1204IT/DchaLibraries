package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import com.android.ims.ImsCallForwardInfo;
import com.android.ims.ImsCallForwardInfoEx;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsSsInfo;
import com.android.ims.ImsUtInterface;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.imsphone.ImsPhoneMmiCode;
import java.util.ArrayList;

public class SSRequestDecisionMaker {
    private static final int CLEAR_DELAY_TIMEOUT = 10000;
    private static final int EVENT_SS_CLEAR_TEMP_VOLTE_USER_FLAG = 3;
    private static final int EVENT_SS_RESPONSE = 2;
    private static final int EVENT_SS_SEND = 1;
    static final String LOG_TAG = "SSDecisonMaker";
    private static final int SS_REQUEST_GET_CALL_BARRING = 3;
    private static final int SS_REQUEST_GET_CALL_FORWARD = 1;
    private static final int SS_REQUEST_GET_CALL_FORWARD_TIME_SLOT = 15;
    private static final int SS_REQUEST_GET_CALL_WAITING = 5;
    private static final int SS_REQUEST_GET_CLIP = 9;
    private static final int SS_REQUEST_GET_CLIR = 7;
    private static final int SS_REQUEST_GET_COLP = 13;
    private static final int SS_REQUEST_GET_COLR = 11;
    private static final int SS_REQUEST_SET_CALL_BARRING = 4;
    private static final int SS_REQUEST_SET_CALL_FORWARD = 2;
    private static final int SS_REQUEST_SET_CALL_FORWARD_TIME_SLOT = 16;
    private static final int SS_REQUEST_SET_CALL_WAITING = 6;
    private static final int SS_REQUEST_SET_CLIP = 10;
    private static final int SS_REQUEST_SET_CLIR = 8;
    private static final int SS_REQUEST_SET_COLP = 14;
    private static final int SS_REQUEST_SET_COLR = 12;
    private CommandsInterface mCi;
    private Context mContext;
    private ImsManager mImsManager;
    private boolean mIsTempVolteUser;
    private Phone mPhone;
    private int mPhoneId;
    private HandlerThread mSSHandlerThread = new HandlerThread("SSRequestHandler");
    private SSRequestHandler mSSRequestHandler;

    public SSRequestDecisionMaker(Context context, Phone phone) {
        this.mContext = context;
        this.mPhone = phone;
        this.mCi = this.mPhone.mCi;
        this.mPhoneId = phone.getPhoneId();
        this.mSSHandlerThread.start();
        Looper looper = this.mSSHandlerThread.getLooper();
        this.mSSRequestHandler = new SSRequestHandler(looper);
        this.mImsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
    }

    public void dispose() {
        Rlog.d(LOG_TAG, "dispose.");
        Looper looper = this.mSSHandlerThread.getLooper();
        looper.quit();
    }

    private int getPhoneId() {
        this.mPhoneId = this.mPhone.getPhoneId();
        return this.mPhoneId;
    }

    private ImsUtInterface getUtInterface() throws ImsException {
        ImsUtInterface ut = ((GsmCdmaPhone) this.mPhone).getUtInterface();
        if (ut == null) {
            throw new ImsException("ut is null", 0);
        }
        return ut;
    }

    void sendGenericErrorResponse(Message onComplete) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (onComplete == null) {
            return;
        }
        AsyncResult.forMessage(onComplete, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
        onComplete.sendToTarget();
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
            default:
                return -1;
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

    private CallForwardInfoEx[] imsCFInfoExToCFInfoEx(ImsCallForwardInfoEx[] infos) {
        CallForwardInfoEx[] cfInfos;
        if (infos != null && infos.length != 0) {
            cfInfos = new CallForwardInfoEx[infos.length];
            int s = infos.length;
            for (int i = 0; i < s; i++) {
                cfInfos[i] = getCallForwardInfoEx(infos[i]);
            }
        } else {
            Rlog.d(LOG_TAG, "No CFInfoEx exist .");
            cfInfos = new CallForwardInfoEx[0];
        }
        Rlog.d(LOG_TAG, "imsCFInfoExToCFInfoEx finish.");
        return cfInfos;
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

    private CallForwardInfo[] imsCFInfoToCFInfo(ImsCallForwardInfo[] infos) {
        CallForwardInfo[] cfInfos;
        if (infos != null && infos.length != 0) {
            cfInfos = new CallForwardInfo[infos.length];
            int s = infos.length;
            for (int i = 0; i < s; i++) {
                cfInfos[i] = getCallForwardInfo(infos[i]);
            }
        } else {
            Rlog.d(LOG_TAG, "No CFInfo exist .");
            cfInfos = new CallForwardInfo[0];
        }
        Rlog.d(LOG_TAG, "imsCFInfoToCFInfo finish.");
        return cfInfos;
    }

    private int getCFReasonFromCondition(int condition) {
        switch (condition) {
        }
        return 3;
    }

    class SSRequestHandler extends Handler implements Runnable {
        public SSRequestHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void run() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SSRequestDecisionMaker.this.processSendRequest(msg.obj);
                    break;
                case 2:
                    SSRequestDecisionMaker.this.processResponse(msg.obj);
                    break;
                case 3:
                    SSRequestDecisionMaker.this.mIsTempVolteUser = false;
                    break;
                default:
                    Rlog.d(SSRequestDecisionMaker.LOG_TAG, "SSRequestDecisionMaker:msg.what=" + msg.what);
                    break;
            }
        }
    }

    private void processSendRequest(Object obj) {
        String getNumber;
        ArrayList<Object> ssParmList = (ArrayList) obj;
        Integer request = (Integer) ssParmList.get(0);
        Message utResp = this.mSSRequestHandler.obtainMessage(2, ssParmList);
        Rlog.d(LOG_TAG, "processSendRequest, request = " + request);
        switch (request.intValue()) {
            case 1:
                int cfReason = ((Integer) ssParmList.get(1)).intValue();
                ((Integer) ssParmList.get(2)).intValue();
                Message resp = (Message) ssParmList.get(4);
                try {
                    ImsUtInterface ut = getUtInterface();
                    ut.queryCallForward(getConditionFromCFReason(cfReason), (String) null, utResp);
                } catch (ImsException e) {
                    sendGenericErrorResponse(resp);
                    return;
                }
                break;
            case 2:
                int action = ((Integer) ssParmList.get(1)).intValue();
                int cfReason2 = ((Integer) ssParmList.get(2)).intValue();
                int serviceClass = ((Integer) ssParmList.get(3)).intValue();
                String number = (String) ssParmList.get(4);
                int timeSeconds = ((Integer) ssParmList.get(5)).intValue();
                Message resp2 = (Message) ssParmList.get(6);
                if ((number == null || number.isEmpty()) && this.mPhone.getPhoneType() == 1 && (this.mPhone instanceof GsmCdmaPhone) && ((GsmCdmaPhone) this.mPhone).isSupportSaveCFNumber() && ((action == 1 || action == 3) && (getNumber = ((GsmCdmaPhone) this.mPhone).getCFPreviousDialNumber(cfReason2)) != null && !getNumber.isEmpty())) {
                    number = getNumber;
                }
                try {
                    ImsUtInterface ut2 = getUtInterface();
                    ut2.updateCallForward(getActionFromCFAction(action), getConditionFromCFReason(cfReason2), number, serviceClass, timeSeconds, utResp);
                } catch (ImsException e2) {
                    sendGenericErrorResponse(resp2);
                    return;
                }
                break;
            case 3:
                String facility = (String) ssParmList.get(1);
                ((Integer) ssParmList.get(3)).intValue();
                Message resp3 = (Message) ssParmList.get(4);
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportOCB(facility)) {
                    if (this.mIsTempVolteUser) {
                        if (resp3 != null) {
                            AsyncResult.forMessage(resp3, (Object) null, new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED));
                            resp3.sendToTarget();
                        }
                    } else {
                        facility = CommandsInterface.CB_FACILITY_BAIC;
                    }
                }
                try {
                    ImsUtInterface ut3 = getUtInterface();
                    ut3.queryCallBarring(getCBTypeFromFacility(facility), utResp);
                } catch (ImsException e3) {
                    sendGenericErrorResponse(resp3);
                    return;
                }
                break;
            case 4:
                String facility2 = (String) ssParmList.get(1);
                boolean lockState = ((Boolean) ssParmList.get(2)).booleanValue();
                ((Integer) ssParmList.get(4)).intValue();
                Message resp4 = (Message) ssParmList.get(5);
                int iLockState = lockState ? 1 : 0;
                try {
                    ImsUtInterface ut4 = getUtInterface();
                    ut4.updateCallBarring(getCBTypeFromFacility(facility2), iLockState, utResp, (String[]) null);
                } catch (ImsException e4) {
                    sendGenericErrorResponse(resp4);
                    return;
                }
                break;
            case 5:
                ((Integer) ssParmList.get(1)).intValue();
                Message resp5 = (Message) ssParmList.get(2);
                try {
                    ImsUtInterface ut5 = getUtInterface();
                    ut5.queryCallWaiting(utResp);
                } catch (ImsException e5) {
                    sendGenericErrorResponse(resp5);
                    return;
                }
                break;
            case 6:
                boolean enable = ((Boolean) ssParmList.get(1)).booleanValue();
                int serviceClass2 = ((Integer) ssParmList.get(2)).intValue();
                Message resp6 = (Message) ssParmList.get(3);
                try {
                    ImsUtInterface ut6 = getUtInterface();
                    if (((GsmCdmaPhone) this.mPhone).isOpNwCW()) {
                        ut6.updateCallWaiting(enable, serviceClass2, utResp);
                    } else {
                        ut6.queryCallWaiting(utResp);
                    }
                } catch (ImsException e6) {
                    sendGenericErrorResponse(resp6);
                    return;
                }
                break;
            case 7:
                Message resp7 = (Message) ssParmList.get(1);
                try {
                    ImsUtInterface ut7 = getUtInterface();
                    ut7.queryCLIR(utResp);
                } catch (ImsException e7) {
                    sendGenericErrorResponse(resp7);
                    return;
                }
                break;
            case 8:
                int mode = ((Integer) ssParmList.get(1)).intValue();
                Message resp8 = (Message) ssParmList.get(2);
                try {
                    ImsUtInterface ut8 = getUtInterface();
                    ut8.updateCLIR(mode, utResp);
                } catch (ImsException e8) {
                    sendGenericErrorResponse(resp8);
                    return;
                }
                break;
            case 9:
                Message resp9 = (Message) ssParmList.get(1);
                try {
                    ImsUtInterface ut9 = getUtInterface();
                    ut9.queryCLIP(utResp);
                } catch (ImsException e9) {
                    sendGenericErrorResponse(resp9);
                    return;
                }
                break;
            case 10:
                int mode2 = ((Integer) ssParmList.get(1)).intValue();
                Message resp10 = (Message) ssParmList.get(2);
                try {
                    ImsUtInterface ut10 = getUtInterface();
                    ut10.updateCLIP(mode2 != 0, utResp);
                } catch (ImsException e10) {
                    sendGenericErrorResponse(resp10);
                    return;
                }
                break;
            case 11:
                Message resp11 = (Message) ssParmList.get(1);
                try {
                    ImsUtInterface ut11 = getUtInterface();
                    ut11.queryCOLR(utResp);
                } catch (ImsException e11) {
                    sendGenericErrorResponse(resp11);
                    return;
                }
                break;
            case 12:
                int mode3 = ((Integer) ssParmList.get(1)).intValue();
                Message resp12 = (Message) ssParmList.get(2);
                try {
                    ImsUtInterface ut12 = getUtInterface();
                    ut12.updateCOLR(mode3, utResp);
                } catch (ImsException e12) {
                    sendGenericErrorResponse(resp12);
                    return;
                }
                break;
            case 13:
                Message resp13 = (Message) ssParmList.get(1);
                try {
                    ImsUtInterface ut13 = getUtInterface();
                    ut13.queryCOLP(utResp);
                } catch (ImsException e13) {
                    sendGenericErrorResponse(resp13);
                    return;
                }
                break;
            case 14:
                int mode4 = ((Integer) ssParmList.get(1)).intValue();
                Message resp14 = (Message) ssParmList.get(2);
                try {
                    ImsUtInterface ut14 = getUtInterface();
                    ut14.updateCOLP(mode4 != 0, utResp);
                } catch (ImsException e14) {
                    sendGenericErrorResponse(resp14);
                    return;
                }
                break;
            case 15:
                int cfReason3 = ((Integer) ssParmList.get(1)).intValue();
                ((Integer) ssParmList.get(2)).intValue();
                Message resp15 = (Message) ssParmList.get(3);
                try {
                    ImsUtInterface ut15 = getUtInterface();
                    ut15.queryCallForwardInTimeSlot(getConditionFromCFReason(cfReason3), utResp);
                } catch (ImsException e15) {
                    sendGenericErrorResponse(resp15);
                    return;
                }
                break;
            case 16:
                int action2 = ((Integer) ssParmList.get(1)).intValue();
                int cfReason4 = ((Integer) ssParmList.get(2)).intValue();
                ((Integer) ssParmList.get(3)).intValue();
                String number2 = (String) ssParmList.get(4);
                int timeSeconds2 = ((Integer) ssParmList.get(5)).intValue();
                long[] timeSlot = (long[]) ssParmList.get(6);
                Message resp16 = (Message) ssParmList.get(7);
                try {
                    ImsUtInterface ut16 = getUtInterface();
                    ut16.updateCallForwardInTimeSlot(getActionFromCFAction(action2), getConditionFromCFReason(cfReason4), number2, timeSeconds2, timeSlot, utResp);
                } catch (ImsException e16) {
                    sendGenericErrorResponse(resp16);
                    return;
                }
                break;
        }
    }

    private void processResponse(Object obj) {
        int iIntValue;
        Message message;
        Message message2 = null;
        AsyncResult asyncResult = (AsyncResult) obj;
        ?? HandleCwQueryResult = asyncResult.result;
        ImsException commandException = asyncResult.exception;
        ArrayList arrayList = (ArrayList) asyncResult.userObj;
        Integer num = (Integer) arrayList.get(0);
        Rlog.d(LOG_TAG, "processResponse, request = " + num);
        switch (num.intValue()) {
            case 1:
                int iIntValue2 = ((Integer) arrayList.get(1)).intValue();
                int iIntValue3 = ((Integer) arrayList.get(2)).intValue();
                String str = (String) arrayList.get(3);
                message2 = (Message) arrayList.get(4);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException = asyncResult.exception;
                    if (imsException.getCode() == 830) {
                        this.mCi.queryCallForwardStatus(iIntValue2, iIntValue3, str, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException.getCode() == 831) {
                        this.mCi.queryCallForwardStatus(iIntValue2, iIntValue3, str, message2);
                        return;
                    }
                }
                if (HandleCwQueryResult != 0) {
                    Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_FORWARD cfinfo check.");
                    if (HandleCwQueryResult instanceof ImsCallForwardInfo[]) {
                        HandleCwQueryResult = imsCFInfoToCFInfo(HandleCwQueryResult);
                    }
                }
                break;
            case 2:
                int iIntValue4 = ((Integer) arrayList.get(1)).intValue();
                int iIntValue5 = ((Integer) arrayList.get(2)).intValue();
                int iIntValue6 = ((Integer) arrayList.get(3)).intValue();
                String str2 = (String) arrayList.get(4);
                int iIntValue7 = ((Integer) arrayList.get(5)).intValue();
                message2 = (Message) arrayList.get(6);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException2 = asyncResult.exception;
                    if (imsException2.getCode() == 830) {
                        this.mCi.setCallForward(iIntValue4, iIntValue5, iIntValue6, str2, iIntValue7, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException2.getCode() == 831) {
                        this.mCi.setCallForward(iIntValue4, iIntValue5, iIntValue6, str2, iIntValue7, message2);
                        return;
                    }
                }
                if (asyncResult.exception == null) {
                    if (this.mPhone.getPhoneType() == 1 && (this.mPhone instanceof GsmCdmaPhone) && ((GsmCdmaPhone) this.mPhone).isSupportSaveCFNumber()) {
                        if (iIntValue4 == 1 || iIntValue4 == 3) {
                            if (!((GsmCdmaPhone) this.mPhone).applyCFSharePreference(iIntValue5, str2)) {
                                Rlog.d(LOG_TAG, "applySharePreference false.");
                            }
                        } else if (iIntValue4 == 4) {
                            ((GsmCdmaPhone) this.mPhone).clearCFSharePreference(iIntValue5);
                        }
                    }
                    if (((GsmCdmaPhone) this.mPhone).queryCFUAgainAfterSet() && iIntValue5 == 0) {
                        ?? r21 = 0;
                        r21 = 0;
                        if (HandleCwQueryResult == 0) {
                            Rlog.d(LOG_TAG, "arResult is null.");
                            HandleCwQueryResult = HandleCwQueryResult;
                        } else if (HandleCwQueryResult instanceof ImsCallForwardInfo[]) {
                            CallForwardInfo[] callForwardInfoArrImsCFInfoToCFInfo = imsCFInfoToCFInfo(HandleCwQueryResult);
                            HandleCwQueryResult = callForwardInfoArrImsCFInfoToCFInfo;
                            r21 = callForwardInfoArrImsCFInfoToCFInfo;
                        } else {
                            boolean z = HandleCwQueryResult instanceof CallForwardInfo[];
                            HandleCwQueryResult = HandleCwQueryResult;
                            if (z) {
                                r21 = HandleCwQueryResult;
                                HandleCwQueryResult = HandleCwQueryResult;
                            }
                        }
                        if (r21 == 0 || r21.length == 0) {
                            Rlog.d(LOG_TAG, "cfInfo is null or length is 0.");
                        } else {
                            int i = 0;
                            while (true) {
                                if (i < r21.length) {
                                    if ((r21[i].serviceClass & 1) == 0) {
                                        i++;
                                    } else if (r21[i].status == 0) {
                                        Rlog.d(LOG_TAG, "Set CF_DISABLE, serviceClass: " + r21[i].serviceClass);
                                        iIntValue4 = 0;
                                    } else {
                                        Rlog.d(LOG_TAG, "Set CF_ENABLE, serviceClass: " + r21[i].serviceClass);
                                        iIntValue4 = 1;
                                    }
                                }
                            }
                        }
                    }
                    if (iIntValue5 == 0) {
                        if (iIntValue4 == 1 || iIntValue4 == 3) {
                            this.mPhone.setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                        } else {
                            this.mPhone.setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                        }
                    }
                }
                break;
            case 3:
                String str3 = (String) arrayList.get(1);
                String str4 = (String) arrayList.get(2);
                int iIntValue8 = ((Integer) arrayList.get(3)).intValue();
                message2 = (Message) arrayList.get(4);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException3 = asyncResult.exception;
                    if (imsException3.getCode() == 830) {
                        this.mCi.queryFacilityLock(str3, str4, iIntValue8, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException3.getCode() == 831) {
                        this.mCi.queryFacilityLock(str3, str4, iIntValue8, message2);
                        return;
                    } else if (imsException3.getCode() == 832 && ((GsmCdmaPhone) this.mPhone).isOpTransferXcap404()) {
                        Rlog.d(LOG_TAG, "processResponse CODE_UT_XCAP_404_NOT_FOUND");
                        commandException = new CommandException(CommandException.Error.UT_XCAP_404_NOT_FOUND);
                    }
                }
                if (HandleCwQueryResult != 0) {
                    Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_BARRING ssinfo check.");
                    if (HandleCwQueryResult instanceof ImsSsInfo[]) {
                        HandleCwQueryResult = handleCbQueryResult((ImsSsInfo[]) asyncResult.result);
                    }
                }
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportOCB(str3)) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                    this.mIsTempVolteUser = true;
                    this.mSSRequestHandler.sendMessageDelayed(this.mSSRequestHandler.obtainMessage(3), 10000L);
                }
                break;
            case 4:
                String str5 = (String) arrayList.get(1);
                boolean zBooleanValue = ((Boolean) arrayList.get(2)).booleanValue();
                String str6 = (String) arrayList.get(3);
                int iIntValue9 = ((Integer) arrayList.get(4)).intValue();
                message2 = (Message) arrayList.get(5);
                HandleCwQueryResult = HandleCwQueryResult;
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportOCB(str5)) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                }
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException4 = asyncResult.exception;
                    if (imsException4.getCode() == 830) {
                        this.mCi.setFacilityLock(str5, zBooleanValue, str6, iIntValue9, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException4.getCode() == 831) {
                        this.mCi.setFacilityLock(str5, zBooleanValue, str6, iIntValue9, message2);
                        return;
                    } else if (imsException4.getCode() == 832 && ((GsmCdmaPhone) this.mPhone).isOpTransferXcap404()) {
                        Rlog.d(LOG_TAG, "processResponse CODE_UT_XCAP_404_NOT_FOUND");
                        commandException = new CommandException(CommandException.Error.UT_XCAP_404_NOT_FOUND);
                    }
                }
                break;
            case 5:
                boolean z2 = false;
                if ((this.mPhone instanceof GsmCdmaPhone) && ((GsmCdmaPhone) this.mPhone).getTbcwMode() == 0) {
                    z2 = true;
                }
                if (z2 && !((GsmCdmaPhone) this.mPhone).isOpNwCW()) {
                    GsmCdmaPhone gsmCdmaPhone = (GsmCdmaPhone) this.mPhone;
                    Integer num2 = (Integer) arrayList.get(0);
                    boolean zBooleanValue2 = false;
                    if (num2.intValue() == 5) {
                        iIntValue = ((Integer) arrayList.get(1)).intValue();
                        message = (Message) arrayList.get(2);
                    } else {
                        zBooleanValue2 = ((Boolean) arrayList.get(1)).booleanValue();
                        iIntValue = ((Integer) arrayList.get(2)).intValue();
                        message = (Message) arrayList.get(3);
                    }
                    ImsException imsException5 = null;
                    if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                        imsException5 = (ImsException) asyncResult.exception;
                    }
                    if (asyncResult.exception == null) {
                        gsmCdmaPhone.setTbcwMode(2);
                        gsmCdmaPhone.setTbcwToEnabledOnIfDisabled();
                        if (num2.intValue() == 5) {
                            gsmCdmaPhone.getTerminalBasedCallWaiting(message);
                            return;
                        } else {
                            gsmCdmaPhone.setTerminalBasedCallWaiting(zBooleanValue2, message);
                            return;
                        }
                    }
                    if (imsException5 != null && imsException5.getCode() == 830) {
                        gsmCdmaPhone.setTbcwMode(3);
                        gsmCdmaPhone.setSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
                        if (num2.intValue() == 5) {
                            this.mCi.queryCallWaiting(iIntValue, message);
                        } else {
                            this.mCi.setCallWaiting(zBooleanValue2, iIntValue, message);
                        }
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    }
                    if (imsException5 != null && imsException5.getCode() == 831) {
                        if (num2.intValue() == 5) {
                            this.mCi.queryCallWaiting(iIntValue, message);
                            return;
                        } else {
                            this.mCi.setCallWaiting(zBooleanValue2, iIntValue, message);
                            return;
                        }
                    }
                    gsmCdmaPhone.setTbcwToEnabledOnIfDisabled();
                    if (num2.intValue() == 5) {
                        gsmCdmaPhone.getTerminalBasedCallWaiting(message);
                        return;
                    } else {
                        gsmCdmaPhone.setTerminalBasedCallWaiting(zBooleanValue2, message);
                        return;
                    }
                }
                Rlog.d(LOG_TAG, "processResponse: SS_REQUEST_GET_CALL_WAITING");
                int iIntValue10 = ((Integer) arrayList.get(1)).intValue();
                message2 = (Message) arrayList.get(2);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException6 = asyncResult.exception;
                    if (imsException6.getCode() == 830) {
                        this.mCi.queryCallWaiting(iIntValue10, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException6.getCode() == 831) {
                        this.mCi.queryCallWaiting(iIntValue10, message2);
                        return;
                    }
                }
                if (HandleCwQueryResult != 0) {
                    Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_WAITING ssinfo check.");
                    if (HandleCwQueryResult instanceof ImsSsInfo[]) {
                        HandleCwQueryResult = handleCwQueryResult((ImsSsInfo[]) asyncResult.result);
                    }
                }
                break;
                break;
            case 6:
                boolean zBooleanValue3 = ((Boolean) arrayList.get(1)).booleanValue();
                int iIntValue11 = ((Integer) arrayList.get(2)).intValue();
                message2 = (Message) arrayList.get(3);
                if (asyncResult.exception == null) {
                    GsmCdmaPhone gsmCdmaPhone2 = (GsmCdmaPhone) this.mPhone;
                    gsmCdmaPhone2.setTbcwMode(2);
                    gsmCdmaPhone2.setTbcwToEnabledOnIfDisabled();
                    gsmCdmaPhone2.setTerminalBasedCallWaiting(zBooleanValue3, message2);
                    return;
                }
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException7 = asyncResult.exception;
                    if (imsException7.getCode() == 830) {
                        this.mCi.setCallWaiting(zBooleanValue3, iIntValue11, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException7.getCode() == 831) {
                        this.mCi.setCallWaiting(zBooleanValue3, iIntValue11, message2);
                        return;
                    }
                }
                break;
            case 7:
                message2 = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException8 = asyncResult.exception;
                    if (imsException8.getCode() == 830) {
                        this.mCi.getCLIR(message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException8.getCode() == 831) {
                        this.mCi.getCLIR(message2);
                        return;
                    }
                }
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                } else {
                    int[] intArray = null;
                    if (asyncResult.exception == null) {
                        intArray = ((Bundle) HandleCwQueryResult).getIntArray("queryClir");
                        Rlog.d(LOG_TAG, "SS_REQUEST_GET_CLIR: CLIR param n=" + intArray[0] + " m=" + intArray[1]);
                    }
                    HandleCwQueryResult = intArray;
                }
                break;
            case 8:
                int iIntValue12 = ((Integer) arrayList.get(1)).intValue();
                message2 = (Message) arrayList.get(2);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException9 = asyncResult.exception;
                    if (imsException9.getCode() == 830) {
                        this.mCi.setCLIR(iIntValue12, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException9.getCode() == 831) {
                        this.mCi.setCLIR(iIntValue12, message2);
                        return;
                    }
                }
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                }
                break;
            case 9:
                message2 = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException10 = asyncResult.exception;
                    if (imsException10.getCode() == 830) {
                        this.mCi.queryCLIP(message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException10.getCode() == 831) {
                        this.mCi.queryCLIP(message2);
                        return;
                    }
                }
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                } else {
                    int[] iArr = {0};
                    if (asyncResult.exception == null) {
                        Bundle bundle = (Bundle) HandleCwQueryResult;
                        if (bundle != null) {
                            ImsSsInfo parcelable = bundle.getParcelable(ImsPhoneMmiCode.UT_BUNDLE_KEY_SSINFO);
                            if (parcelable != null) {
                                Rlog.d(LOG_TAG, "ImsSsInfo mStatus = " + parcelable.mStatus);
                                iArr[0] = parcelable.mStatus;
                            } else {
                                Rlog.e(LOG_TAG, "SS_REQUEST_GET_CLIP: ssInfo null!");
                            }
                        } else {
                            Rlog.e(LOG_TAG, "SS_REQUEST_GET_CLIP: bundle null!");
                        }
                    }
                    HandleCwQueryResult = iArr;
                }
                break;
            case 10:
                boolean z3 = ((Integer) arrayList.get(1)).intValue() != 0;
                message2 = (Message) arrayList.get(2);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException11 = asyncResult.exception;
                    if (imsException11.getCode() == 830) {
                        this.mCi.setCLIP(z3, message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException11.getCode() == 831) {
                        this.mCi.setCLIP(z3, message2);
                        return;
                    }
                }
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                }
                break;
            case 11:
                message2 = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException12 = asyncResult.exception;
                    if (imsException12.getCode() == 830) {
                        this.mCi.getCOLR(message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException12.getCode() == 831) {
                        this.mCi.getCOLR(message2);
                        return;
                    }
                }
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                } else {
                    int[] iArr2 = {0};
                    if (asyncResult.exception == null) {
                        Bundle bundle2 = (Bundle) HandleCwQueryResult;
                        if (bundle2 != null) {
                            ImsSsInfo parcelable2 = bundle2.getParcelable(ImsPhoneMmiCode.UT_BUNDLE_KEY_SSINFO);
                            if (parcelable2 != null) {
                                Rlog.d(LOG_TAG, "ImsSsInfo mStatus = " + parcelable2.mStatus);
                                iArr2[0] = parcelable2.mStatus;
                            } else {
                                Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLR: ssInfo null!");
                            }
                        } else {
                            Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLR: bundle null!");
                        }
                    }
                    HandleCwQueryResult = iArr2;
                }
                break;
            case 12:
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                }
                break;
            case 13:
                message2 = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException13 = asyncResult.exception;
                    if (imsException13.getCode() == 830) {
                        this.mCi.getCOLP(message2);
                        this.mPhone.setCsFallbackStatus(2);
                        return;
                    } else if (imsException13.getCode() == 831) {
                        this.mCi.getCOLP(message2);
                        return;
                    }
                }
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                } else {
                    int[] iArr3 = {0};
                    if (asyncResult.exception == null) {
                        Bundle bundle3 = (Bundle) HandleCwQueryResult;
                        if (bundle3 != null) {
                            ImsSsInfo parcelable3 = bundle3.getParcelable(ImsPhoneMmiCode.UT_BUNDLE_KEY_SSINFO);
                            if (parcelable3 != null) {
                                Rlog.d(LOG_TAG, "ImsSsInfo mStatus = " + parcelable3.mStatus);
                                iArr3[0] = parcelable3.mStatus;
                            } else {
                                Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLP: ssInfo null!");
                            }
                        } else {
                            Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLP: bundle null!");
                        }
                    }
                    HandleCwQueryResult = iArr3;
                }
                break;
            case 14:
                if (((GsmCdmaPhone) this.mPhone).isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    HandleCwQueryResult = 0;
                }
                break;
            case 15:
                message2 = (Message) arrayList.get(3);
                HandleCwQueryResult = HandleCwQueryResult;
                if (asyncResult.exception != null) {
                    HandleCwQueryResult = HandleCwQueryResult;
                    if (asyncResult.exception instanceof ImsException) {
                        ImsException imsException14 = asyncResult.exception;
                        if (imsException14.getCode() == 830) {
                            commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                            HandleCwQueryResult = 0;
                            this.mPhone.setCsFallbackStatus(2);
                        } else {
                            HandleCwQueryResult = HandleCwQueryResult;
                            if (imsException14.getCode() == 831) {
                                if (message2 != null) {
                                    AsyncResult.forMessage(message2, (Object) null, new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED));
                                    message2.sendToTarget();
                                    return;
                                }
                                return;
                            }
                        }
                    }
                }
                if (HandleCwQueryResult != 0) {
                    Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_FORWARD_TIME_SLOT cfinfoEx check.");
                    if (HandleCwQueryResult instanceof ImsCallForwardInfoEx[]) {
                        HandleCwQueryResult = imsCFInfoExToCFInfoEx(HandleCwQueryResult);
                    }
                }
                break;
            case 16:
                message2 = (Message) arrayList.get(7);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException15 = asyncResult.exception;
                    if (imsException15.getCode() == 830) {
                        commandException = new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                        HandleCwQueryResult = 0;
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException15.getCode() == 831) {
                        if (message2 != null) {
                            AsyncResult.forMessage(message2, (Object) null, new CommandException(CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED));
                            message2.sendToTarget();
                            return;
                        }
                        return;
                    }
                }
                break;
        }
        if (commandException != null && (commandException instanceof ImsException)) {
            Rlog.d(LOG_TAG, "processResponse, imsException.getCode = " + commandException.getCode());
            commandException = getCommandException(commandException);
        }
        if (message2 == null) {
            return;
        }
        AsyncResult.forMessage(message2, (Object) HandleCwQueryResult, commandException);
        message2.sendToTarget();
    }

    private CommandException getCommandException(ImsException imsException) {
        switch (imsException.getCode()) {
            case 833:
                if (((GsmCdmaPhone) this.mPhone).isEnableXcapHttpResponse409()) {
                    Rlog.d(LOG_TAG, "getCommandException UT_XCAP_409_CONFLICT");
                    return new CommandException(CommandException.Error.UT_XCAP_409_CONFLICT);
                }
                Rlog.d(LOG_TAG, "getCommandException GENERIC_FAILURE");
                return new CommandException(CommandException.Error.GENERIC_FAILURE);
            default:
                Rlog.d(LOG_TAG, "getCommandException GENERIC_FAILURE");
                return new CommandException(CommandException.Error.GENERIC_FAILURE);
        }
    }

    public void queryCallForwardStatus(int cfReason, int serviceClass, String number, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(1));
        ssParmList.add(new Integer(cfReason));
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(number);
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setCallForward(int action, int cfReason, int serviceClass, String number, int timeSeconds, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(2));
        ssParmList.add(new Integer(action));
        ssParmList.add(new Integer(cfReason));
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(number);
        ssParmList.add(new Integer(timeSeconds));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void queryCallForwardInTimeSlotStatus(int cfReason, int serviceClass, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(15));
        ssParmList.add(new Integer(cfReason));
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setCallForwardInTimeSlot(int action, int cfReason, int serviceClass, String number, int timeSeconds, long[] timeSlot, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(16));
        ssParmList.add(new Integer(action));
        ssParmList.add(new Integer(cfReason));
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(number);
        ssParmList.add(new Integer(timeSeconds));
        ssParmList.add(timeSlot);
        ssParmList.add(response);
        send(ssParmList);
    }

    public void queryFacilityLock(String facility, String password, int serviceClass, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(3));
        ssParmList.add(facility);
        ssParmList.add(password);
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setFacilityLock(String facility, boolean lockState, String password, int serviceClass, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(4));
        ssParmList.add(facility);
        ssParmList.add(new Boolean(lockState));
        ssParmList.add(password);
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void queryCallWaiting(int serviceClass, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(5));
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setCallWaiting(boolean enable, int serviceClass, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(6));
        ssParmList.add(new Boolean(enable));
        ssParmList.add(new Integer(serviceClass));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void getCLIR(Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(7));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setCLIR(int clirMode, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(8));
        ssParmList.add(new Integer(clirMode));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void getCLIP(Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(9));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setCLIP(int clipMode, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(10));
        ssParmList.add(new Integer(clipMode));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void getCOLR(Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(11));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setCOLR(int colrMode, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(12));
        ssParmList.add(new Integer(colrMode));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void getCOLP(Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(13));
        ssParmList.add(response);
        send(ssParmList);
    }

    public void setCOLP(int colpMode, Message response) {
        ArrayList<Object> ssParmList = new ArrayList<>();
        ssParmList.add(new Integer(14));
        ssParmList.add(new Integer(colpMode));
        ssParmList.add(response);
        send(ssParmList);
    }

    void send(ArrayList<Object> ssParmList) {
        Message msg = this.mSSRequestHandler.obtainMessage(1, ssParmList);
        msg.sendToTarget();
    }
}
