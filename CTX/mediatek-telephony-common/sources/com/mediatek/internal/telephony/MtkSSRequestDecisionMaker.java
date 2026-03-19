package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsSsInfo;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsUtInterface;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.mediatek.ims.MtkImsCallForwardInfo;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import java.util.ArrayList;

public class MtkSSRequestDecisionMaker {
    private static final int CLEAR_DELAY_TIMEOUT = 10000;
    private static final int EVENT_SS_CLEAR_TEMP_VOLTE_USER_FLAG = 3;
    private static final int EVENT_SS_RESPONSE = 2;
    private static final int EVENT_SS_SEND = 1;
    static final String LOG_TAG = "MtkSSDecisonMaker";
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
    private ImsManager mImsManager;
    private boolean mIsTempVolteUser;
    private Phone mPhone;
    private int mPhoneId;
    private HandlerThread mSSHandlerThread;
    private SSRequestHandler mSSRequestHandler;

    public MtkSSRequestDecisionMaker(Context context, Phone phone) {
        this.mPhone = phone;
        this.mCi = this.mPhone.mCi;
        this.mPhoneId = phone.getPhoneId();
        this.mImsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
    }

    public void starThread() {
        this.mSSHandlerThread = new HandlerThread("SSRequestHandler");
        this.mSSHandlerThread.start();
        this.mSSRequestHandler = new SSRequestHandler(this.mSSHandlerThread.getLooper());
    }

    public void dispose() {
        Rlog.d(LOG_TAG, "dispose.");
        this.mSSHandlerThread.getLooper().quit();
    }

    private int getPhoneId() {
        this.mPhoneId = this.mPhone.getPhoneId();
        return this.mPhoneId;
    }

    private ImsUtInterface getUtInterface() throws ImsException {
        if (this.mImsManager == null) {
            throw new ImsException("no ims manager", 0);
        }
        this.mPhone.getPhoneId();
        return this.mImsManager.getSupplementaryServiceConfiguration();
    }

    void sendGenericErrorResponse(Message message) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (message != null) {
            AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
            message.sendToTarget();
        }
    }

    private void sendRadioNotAvailable(Message message) {
        Rlog.d(LOG_TAG, "sendRadioNotAvailable");
        if (message != null) {
            AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE));
            message.sendToTarget();
        }
    }

    private int getActionFromCFAction(int i) {
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

    private int getConditionFromCFReason(int i) {
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

    private int getCBTypeFromFacility(String str) {
        if ("AO".equals(str)) {
            return 2;
        }
        if ("OI".equals(str)) {
            return 3;
        }
        if ("OX".equals(str)) {
            return 4;
        }
        if ("AI".equals(str)) {
            return 1;
        }
        if ("IR".equals(str)) {
            return 5;
        }
        if ("AB".equals(str)) {
            return 7;
        }
        if ("AG".equals(str)) {
            return 8;
        }
        if ("AC".equals(str)) {
            return 9;
        }
        return 0;
    }

    private int[] handleCbQueryResult(ImsSsInfo[] imsSsInfoArr) {
        return new int[]{imsSsInfoArr[0].mStatus};
    }

    private int[] handleCwQueryResult(ImsSsInfo[] imsSsInfoArr) {
        int[] iArr = {0, 0};
        if (imsSsInfoArr[0].mStatus == 1) {
            iArr[0] = 1;
            iArr[1] = 1;
        }
        return iArr;
    }

    private MtkCallForwardInfo getMtkCallForwardInfo(MtkImsCallForwardInfo mtkImsCallForwardInfo) {
        MtkCallForwardInfo mtkCallForwardInfo = new MtkCallForwardInfo();
        mtkCallForwardInfo.status = mtkImsCallForwardInfo.mStatus;
        mtkCallForwardInfo.reason = getCFReasonFromCondition(mtkImsCallForwardInfo.mCondition);
        mtkCallForwardInfo.serviceClass = mtkImsCallForwardInfo.mServiceClass;
        mtkCallForwardInfo.toa = mtkImsCallForwardInfo.mToA;
        mtkCallForwardInfo.number = mtkImsCallForwardInfo.mNumber;
        mtkCallForwardInfo.timeSeconds = mtkImsCallForwardInfo.mTimeSeconds;
        mtkCallForwardInfo.timeSlot = mtkImsCallForwardInfo.mTimeSlot;
        return mtkCallForwardInfo;
    }

    private MtkCallForwardInfo[] imsCFInfoExToCFInfoEx(MtkImsCallForwardInfo[] mtkImsCallForwardInfoArr) {
        MtkCallForwardInfo[] mtkCallForwardInfoArr;
        if (mtkImsCallForwardInfoArr != null && mtkImsCallForwardInfoArr.length != 0) {
            mtkCallForwardInfoArr = new MtkCallForwardInfo[mtkImsCallForwardInfoArr.length];
            int length = mtkImsCallForwardInfoArr.length;
            for (int i = 0; i < length; i++) {
                mtkCallForwardInfoArr[i] = getMtkCallForwardInfo(mtkImsCallForwardInfoArr[i]);
            }
        } else {
            Rlog.d(LOG_TAG, "No CFInfoEx exist .");
            mtkCallForwardInfoArr = new MtkCallForwardInfo[0];
        }
        Rlog.d(LOG_TAG, "imsCFInfoExToCFInfoEx finish.");
        return mtkCallForwardInfoArr;
    }

    private CallForwardInfo getCallForwardInfo(ImsCallForwardInfo imsCallForwardInfo) {
        CallForwardInfo callForwardInfo = new CallForwardInfo();
        callForwardInfo.status = imsCallForwardInfo.mStatus;
        callForwardInfo.reason = getCFReasonFromCondition(imsCallForwardInfo.mCondition);
        callForwardInfo.serviceClass = imsCallForwardInfo.mServiceClass;
        callForwardInfo.toa = imsCallForwardInfo.mToA;
        callForwardInfo.number = imsCallForwardInfo.mNumber;
        callForwardInfo.timeSeconds = imsCallForwardInfo.mTimeSeconds;
        return callForwardInfo;
    }

    private CallForwardInfo[] imsCFInfoToCFInfo(ImsCallForwardInfo[] imsCallForwardInfoArr) {
        CallForwardInfo[] callForwardInfoArr;
        if (imsCallForwardInfoArr != null && imsCallForwardInfoArr.length != 0) {
            callForwardInfoArr = new CallForwardInfo[imsCallForwardInfoArr.length];
            int length = imsCallForwardInfoArr.length;
            for (int i = 0; i < length; i++) {
                callForwardInfoArr[i] = getCallForwardInfo(imsCallForwardInfoArr[i]);
            }
        } else {
            Rlog.d(LOG_TAG, "No CFInfo exist .");
            callForwardInfoArr = new CallForwardInfo[0];
        }
        Rlog.d(LOG_TAG, "imsCFInfoToCFInfo finish.");
        return callForwardInfoArr;
    }

    private int getCFReasonFromCondition(int i) {
        switch (i) {
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
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    MtkSSRequestDecisionMaker.this.processSendRequest(message.obj);
                    break;
                case 2:
                    MtkSSRequestDecisionMaker.this.processResponse(message.obj);
                    break;
                case 3:
                    MtkSSRequestDecisionMaker.this.mIsTempVolteUser = false;
                    break;
                default:
                    Rlog.d(MtkSSRequestDecisionMaker.LOG_TAG, "MtkSSRequestDecisionMaker:msg.what=" + message.what);
                    break;
            }
        }
    }

    private void processSendRequest(Object obj) {
        String cFPreviousDialNumber;
        ArrayList arrayList = (ArrayList) obj;
        Integer num = (Integer) arrayList.get(0);
        Message messageObtainMessage = this.mSSRequestHandler.obtainMessage(2, arrayList);
        Rlog.d(LOG_TAG, "processSendRequest, request = " + num);
        switch (num.intValue()) {
            case 1:
                int iIntValue = ((Integer) arrayList.get(1)).intValue();
                int iIntValue2 = ((Integer) arrayList.get(2)).intValue();
                Message message = (Message) arrayList.get(4);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message);
                } else {
                    try {
                        ImsUtInterface utInterface = getUtInterface();
                        if (iIntValue2 != 0 && (this.mPhone instanceof MtkGsmCdmaPhone)) {
                            this.mPhone.setServiceClass(iIntValue2);
                        }
                        utInterface.queryCallForward(getConditionFromCFReason(iIntValue), (String) null, messageObtainMessage);
                    } catch (ImsException e) {
                        sendGenericErrorResponse(message);
                        return;
                    }
                }
                break;
            case 2:
                int iIntValue3 = ((Integer) arrayList.get(1)).intValue();
                int iIntValue4 = ((Integer) arrayList.get(2)).intValue();
                int iIntValue5 = ((Integer) arrayList.get(3)).intValue();
                String str = (String) arrayList.get(4);
                int iIntValue6 = ((Integer) arrayList.get(5)).intValue();
                Message message2 = (Message) arrayList.get(6);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message2);
                } else {
                    try {
                        getUtInterface().updateCallForward(getActionFromCFAction(iIntValue3), getConditionFromCFReason(iIntValue4), ((str == null || str.isEmpty()) && this.mPhone.getPhoneType() == 1 && (this.mPhone instanceof MtkGsmCdmaPhone) && this.mPhone.isSupportSaveCFNumber() && !((iIntValue3 != 1 && iIntValue3 != 3) || (cFPreviousDialNumber = this.mPhone.getCFPreviousDialNumber(iIntValue4)) == null || cFPreviousDialNumber.isEmpty())) ? cFPreviousDialNumber : str, iIntValue5, iIntValue6, messageObtainMessage);
                    } catch (ImsException e2) {
                        sendGenericErrorResponse(message2);
                        return;
                    }
                }
                break;
            case 3:
                String str2 = (String) arrayList.get(1);
                int iIntValue7 = ((Integer) arrayList.get(3)).intValue();
                Message message3 = (Message) arrayList.get(4);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message3);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message3);
                } else {
                    if (this.mPhone.isOpNotSupportOCB(str2)) {
                        if (this.mIsTempVolteUser) {
                            if (message3 != null) {
                                AsyncResult.forMessage(message3, (Object) null, new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
                                message3.sendToTarget();
                            }
                        } else {
                            str2 = "AI";
                        }
                    }
                    try {
                        getUtInterface().queryCallBarring(getCBTypeFromFacility(str2), messageObtainMessage, iIntValue7);
                    } catch (ImsException e3) {
                        sendGenericErrorResponse(message3);
                        return;
                    }
                }
                break;
            case 4:
                String str3 = (String) arrayList.get(1);
                boolean zBooleanValue = ((Boolean) arrayList.get(2)).booleanValue();
                int iIntValue8 = ((Integer) arrayList.get(4)).intValue();
                Message message4 = (Message) arrayList.get(5);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message4);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message4);
                } else {
                    try {
                        getUtInterface().updateCallBarring(getCBTypeFromFacility(str3), zBooleanValue ? 1 : 0, messageObtainMessage, (String[]) null, iIntValue8);
                    } catch (ImsException e4) {
                        sendGenericErrorResponse(message4);
                        return;
                    }
                }
                break;
            case 5:
                ((Integer) arrayList.get(1)).intValue();
                Message message5 = (Message) arrayList.get(2);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message5);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message5);
                } else {
                    try {
                        getUtInterface().queryCallWaiting(messageObtainMessage);
                    } catch (ImsException e5) {
                        sendGenericErrorResponse(message5);
                        return;
                    }
                }
                break;
            case 6:
                boolean zBooleanValue2 = ((Boolean) arrayList.get(1)).booleanValue();
                int iIntValue9 = ((Integer) arrayList.get(2)).intValue();
                Message message6 = (Message) arrayList.get(3);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message6);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message6);
                } else {
                    try {
                        ImsUtInterface utInterface2 = getUtInterface();
                        if (this.mPhone.isOpNwCW()) {
                            utInterface2.updateCallWaiting(zBooleanValue2, iIntValue9, messageObtainMessage);
                        } else {
                            utInterface2.queryCallWaiting(messageObtainMessage);
                        }
                    } catch (ImsException e6) {
                        sendGenericErrorResponse(message6);
                        return;
                    }
                }
                break;
            case 7:
                Message message7 = (Message) arrayList.get(1);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message7);
                } else {
                    try {
                        getUtInterface().queryCLIR(messageObtainMessage);
                    } catch (ImsException e7) {
                        sendGenericErrorResponse(message7);
                        return;
                    }
                }
                break;
            case 8:
                int iIntValue10 = ((Integer) arrayList.get(1)).intValue();
                Message message8 = (Message) arrayList.get(2);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message8);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message8);
                } else {
                    try {
                        getUtInterface().updateCLIR(iIntValue10, messageObtainMessage);
                    } catch (ImsException e8) {
                        sendGenericErrorResponse(message8);
                        return;
                    }
                }
                break;
            case 9:
                Message message9 = (Message) arrayList.get(1);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message9);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message9);
                } else {
                    try {
                        getUtInterface().queryCLIP(messageObtainMessage);
                    } catch (ImsException e9) {
                        sendGenericErrorResponse(message9);
                        return;
                    }
                }
                break;
            case 10:
                int iIntValue11 = ((Integer) arrayList.get(1)).intValue();
                Message message10 = (Message) arrayList.get(2);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message10);
                } else if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message10);
                } else {
                    try {
                        getUtInterface().updateCLIP(iIntValue11 != 0, messageObtainMessage);
                    } catch (ImsException e10) {
                        sendGenericErrorResponse(message10);
                        return;
                    }
                }
                break;
            case 11:
                Message message11 = (Message) arrayList.get(1);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message11);
                } else {
                    try {
                        getUtInterface().queryCOLR(messageObtainMessage);
                    } catch (ImsException e11) {
                        sendGenericErrorResponse(message11);
                        return;
                    }
                }
                break;
            case 12:
                int iIntValue12 = ((Integer) arrayList.get(1)).intValue();
                Message message12 = (Message) arrayList.get(2);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message12);
                } else {
                    try {
                        getUtInterface().updateCOLR(iIntValue12, messageObtainMessage);
                    } catch (ImsException e12) {
                        sendGenericErrorResponse(message12);
                        return;
                    }
                }
                break;
            case 13:
                Message message13 = (Message) arrayList.get(1);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message13);
                } else {
                    try {
                        getUtInterface().queryCOLP(messageObtainMessage);
                    } catch (ImsException e13) {
                        sendGenericErrorResponse(message13);
                        return;
                    }
                }
                break;
            case 14:
                int iIntValue13 = ((Integer) arrayList.get(1)).intValue();
                Message message14 = (Message) arrayList.get(2);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message14);
                } else {
                    try {
                        getUtInterface().updateCOLP(iIntValue13 != 0, messageObtainMessage);
                    } catch (ImsException e14) {
                        sendGenericErrorResponse(message14);
                        return;
                    }
                }
                break;
            case 15:
                int iIntValue14 = ((Integer) arrayList.get(1)).intValue();
                ((Integer) arrayList.get(2)).intValue();
                Message message15 = (Message) arrayList.get(3);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message15);
                } else {
                    try {
                        getUtInterface().queryCallForwardInTimeSlot(getConditionFromCFReason(iIntValue14), messageObtainMessage);
                    } catch (ImsException e15) {
                        sendGenericErrorResponse(message15);
                        return;
                    }
                }
                break;
            case 16:
                int iIntValue15 = ((Integer) arrayList.get(1)).intValue();
                int iIntValue16 = ((Integer) arrayList.get(2)).intValue();
                ((Integer) arrayList.get(3)).intValue();
                String str4 = (String) arrayList.get(4);
                int iIntValue17 = ((Integer) arrayList.get(5)).intValue();
                long[] jArr = (long[]) arrayList.get(6);
                Message message16 = (Message) arrayList.get(7);
                if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                    sendRadioNotAvailable(message16);
                } else {
                    try {
                        getUtInterface().updateCallForwardInTimeSlot(getActionFromCFAction(iIntValue15), getConditionFromCFReason(iIntValue16), str4, iIntValue17, jArr, messageObtainMessage);
                    } catch (ImsException e16) {
                        sendGenericErrorResponse(message16);
                        return;
                    }
                }
                break;
        }
    }

    private void processResponse(Object obj) {
        Message message;
        Message message2;
        int iIntValue;
        Message message3;
        int[] iArr;
        AsyncResult asyncResult = (AsyncResult) obj;
        Object objImsCFInfoToCFInfo = asyncResult.result;
        Throwable commandException = asyncResult.exception;
        ArrayList arrayList = (ArrayList) asyncResult.userObj;
        boolean zBooleanValue = false;
        Integer num = (Integer) arrayList.get(0);
        Rlog.d(LOG_TAG, "processResponse, request = " + num);
        Message message4 = null;
        switch (num.intValue()) {
            case 1:
                int iIntValue2 = ((Integer) arrayList.get(1)).intValue();
                int iIntValue3 = ((Integer) arrayList.get(2)).intValue();
                String str = (String) arrayList.get(3);
                message = (Message) arrayList.get(4);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException = asyncResult.exception;
                    if (imsException.getCode() == 61446) {
                        this.mPhone.setCsFallbackStatus(2);
                        if (!this.mPhone.isNotSupportUtToCS()) {
                            Rlog.d(LOG_TAG, "mCi.queryCallForwardStatus.");
                            this.mCi.queryCallForwardStatus(iIntValue2, iIntValue3, str, message);
                        } else {
                            Rlog.d(LOG_TAG, "isNotSupportUtToCS.");
                            commandException = new CommandException(CommandException.Error.OPERATION_NOT_ALLOWED);
                        }
                    } else if (imsException.getCode() == 61447) {
                        if (!this.mPhone.isNotSupportUtToCS()) {
                            Rlog.d(LOG_TAG, "mCi.queryCallForwardStatus.");
                            this.mCi.queryCallForwardStatus(iIntValue2, iIntValue3, str, message);
                        } else {
                            Rlog.d(LOG_TAG, "isNotSupportUtToCS.");
                            commandException = new CommandException(CommandException.Error.OPERATION_NOT_ALLOWED);
                        }
                    }
                    objImsCFInfoToCFInfo = null;
                }
                if (objImsCFInfoToCFInfo != null) {
                    Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_FORWARD cfinfo check.");
                    if (objImsCFInfoToCFInfo instanceof ImsCallForwardInfo[]) {
                        objImsCFInfoToCFInfo = imsCFInfoToCFInfo((ImsCallForwardInfo[]) objImsCFInfoToCFInfo);
                    }
                }
                if (commandException != null && (commandException instanceof ImsException)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("processResponse, imsException.getCode = ");
                    ImsException imsException2 = (ImsException) commandException;
                    sb.append(imsException2.getCode());
                    Rlog.d(LOG_TAG, sb.toString());
                    commandException = getCommandException(imsException2);
                }
                if (message != null) {
                    AsyncResult.forMessage(message, objImsCFInfoToCFInfo, commandException);
                    message.sendToTarget();
                }
                break;
            case 2:
                int iIntValue4 = ((Integer) arrayList.get(1)).intValue();
                int iIntValue5 = ((Integer) arrayList.get(2)).intValue();
                int iIntValue6 = ((Integer) arrayList.get(3)).intValue();
                String str2 = (String) arrayList.get(4);
                int iIntValue7 = ((Integer) arrayList.get(5)).intValue();
                Message message5 = (Message) arrayList.get(6);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException3 = asyncResult.exception;
                    if (imsException3.getCode() == 61446) {
                        this.mCi.setCallForward(iIntValue4, iIntValue5, iIntValue6, str2, iIntValue7, message5);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException3.getCode() == 61447) {
                        this.mCi.setCallForward(iIntValue4, iIntValue5, iIntValue6, str2, iIntValue7, message5);
                    }
                }
                if (asyncResult.exception == null) {
                    if (this.mPhone.getPhoneType() == 1 && (this.mPhone instanceof MtkGsmCdmaPhone) && this.mPhone.isSupportSaveCFNumber()) {
                        if (iIntValue4 == 1 || iIntValue4 == 3) {
                            if (!this.mPhone.applyCFSharePreference(iIntValue5, str2)) {
                                Rlog.d(LOG_TAG, "applySharePreference false.");
                            }
                        } else if (iIntValue4 == 4) {
                            this.mPhone.clearCFSharePreference(iIntValue5);
                        }
                    }
                    if (this.mPhone.queryCFUAgainAfterSet() && iIntValue5 == 0) {
                        if (objImsCFInfoToCFInfo == null) {
                            Rlog.d(LOG_TAG, "arResult is null.");
                        } else if (objImsCFInfoToCFInfo instanceof ImsCallForwardInfo[]) {
                            objImsCFInfoToCFInfo = imsCFInfoToCFInfo((ImsCallForwardInfo[]) objImsCFInfoToCFInfo);
                        } else if (objImsCFInfoToCFInfo instanceof CallForwardInfo[]) {
                        }
                    }
                }
                message = message5;
                if (commandException != null) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("processResponse, imsException.getCode = ");
                    ImsException imsException22 = (ImsException) commandException;
                    sb2.append(imsException22.getCode());
                    Rlog.d(LOG_TAG, sb2.toString());
                    commandException = getCommandException(imsException22);
                }
                if (message != null) {
                }
                break;
            case 3:
                String str3 = (String) arrayList.get(1);
                String str4 = (String) arrayList.get(2);
                int iIntValue8 = ((Integer) arrayList.get(3)).intValue();
                message2 = (Message) arrayList.get(4);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException4 = asyncResult.exception;
                    if (imsException4.getCode() == 61446 || imsException4.getCode() == 61447) {
                        commandException = this.mPhone.checkUiccApplicationForCB();
                        if (commandException == null) {
                            this.mCi.queryFacilityLockForApp(str3, str4, iIntValue8, this.mPhone.getUiccCardApplication().getAid(), message2);
                            if (imsException4.getCode() == 61446) {
                                this.mPhone.setCsFallbackStatus(2);
                            }
                        }
                    } else if (imsException4.getCode() == 61448 && this.mPhone.isOpTransferXcap404()) {
                        Rlog.d(LOG_TAG, "processResponse CODE_UT_XCAP_404_NOT_FOUND");
                        commandException = new CommandException(CommandException.Error.NO_SUCH_ELEMENT);
                    }
                }
                if (objImsCFInfoToCFInfo != null) {
                    Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_BARRING ssinfo check.");
                    if (objImsCFInfoToCFInfo instanceof ImsSsInfo[]) {
                        objImsCFInfoToCFInfo = handleCbQueryResult((ImsSsInfo[]) asyncResult.result);
                    }
                }
                if (this.mPhone.isOpNotSupportOCB(str3)) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    this.mIsTempVolteUser = true;
                    this.mSSRequestHandler.sendMessageDelayed(this.mSSRequestHandler.obtainMessage(3), 10000L);
                    objImsCFInfoToCFInfo = null;
                }
                message = message2;
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            case 4:
                String str5 = (String) arrayList.get(1);
                boolean zBooleanValue2 = ((Boolean) arrayList.get(2)).booleanValue();
                String str6 = (String) arrayList.get(3);
                int iIntValue9 = ((Integer) arrayList.get(4)).intValue();
                message2 = (Message) arrayList.get(5);
                if (this.mPhone.isOpNotSupportOCB(str5)) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                }
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException5 = asyncResult.exception;
                    if (imsException5.getCode() == 61446) {
                        this.mCi.setFacilityLock(str5, zBooleanValue2, str6, iIntValue9, message2);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException5.getCode() == 61447) {
                        this.mCi.setFacilityLock(str5, zBooleanValue2, str6, iIntValue9, message2);
                    } else if (imsException5.getCode() == 61448 && this.mPhone.isOpTransferXcap404()) {
                        Rlog.d(LOG_TAG, "processResponse CODE_UT_XCAP_404_NOT_FOUND");
                        commandException = new CommandException(CommandException.Error.NO_SUCH_ELEMENT);
                    }
                }
                message = message2;
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            case 5:
                if (((this.mPhone instanceof MtkGsmCdmaPhone) && this.mPhone.getTbcwMode() == 0) && !this.mPhone.isOpNwCW()) {
                    MtkGsmCdmaPhone mtkGsmCdmaPhone = this.mPhone;
                    Integer num2 = (Integer) arrayList.get(0);
                    if (num2.intValue() == 5) {
                        iIntValue = ((Integer) arrayList.get(1)).intValue();
                        message3 = (Message) arrayList.get(2);
                    } else {
                        zBooleanValue = ((Boolean) arrayList.get(1)).booleanValue();
                        iIntValue = ((Integer) arrayList.get(2)).intValue();
                        message3 = (Message) arrayList.get(3);
                    }
                    ImsException imsException6 = (asyncResult.exception == null || !(asyncResult.exception instanceof ImsException)) ? null : (ImsException) asyncResult.exception;
                    if (asyncResult.exception == null) {
                        mtkGsmCdmaPhone.setTbcwMode(1);
                        mtkGsmCdmaPhone.setTbcwToEnabledOnIfDisabled();
                        if (num2.intValue() != 5) {
                            mtkGsmCdmaPhone.setTerminalBasedCallWaiting(zBooleanValue, message3);
                        } else {
                            mtkGsmCdmaPhone.getTerminalBasedCallWaiting(message3);
                        }
                    } else if (imsException6 != null && imsException6.getCode() == 61446) {
                        mtkGsmCdmaPhone.setTbcwMode(2);
                        mtkGsmCdmaPhone.setSSPropertyThroughHidl(mtkGsmCdmaPhone.getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw");
                        this.mPhone.setCsFallbackStatus(2);
                        if (this.mPhone.isNotSupportUtToCS()) {
                            Rlog.d(LOG_TAG, "isNotSupportUtToCS.");
                            CommandException commandException2 = new CommandException(CommandException.Error.OPERATION_NOT_ALLOWED);
                            if (message3 != null) {
                                AsyncResult.forMessage(message3, (Object) null, commandException2);
                                message3.sendToTarget();
                            }
                        } else if (num2.intValue() != 5) {
                            this.mCi.setCallWaiting(zBooleanValue, iIntValue, message3);
                        } else {
                            this.mCi.queryCallWaiting(iIntValue, message3);
                        }
                    } else if (imsException6 == null || imsException6.getCode() != 61447) {
                        mtkGsmCdmaPhone.setTbcwToEnabledOnIfDisabled();
                        if (num2.intValue() != 5) {
                            mtkGsmCdmaPhone.setTerminalBasedCallWaiting(zBooleanValue, message3);
                        } else {
                            mtkGsmCdmaPhone.getTerminalBasedCallWaiting(message3);
                        }
                    } else if (this.mPhone.isNotSupportUtToCS()) {
                        Rlog.d(LOG_TAG, "isNotSupportUtToCS.");
                        CommandException commandException3 = new CommandException(CommandException.Error.OPERATION_NOT_ALLOWED);
                        if (message3 != null) {
                            AsyncResult.forMessage(message3, (Object) null, commandException3);
                            message3.sendToTarget();
                        }
                    } else if (num2.intValue() != 5) {
                        this.mCi.setCallWaiting(zBooleanValue, iIntValue, message3);
                    } else {
                        this.mCi.queryCallWaiting(iIntValue, message3);
                    }
                } else {
                    Rlog.d(LOG_TAG, "processResponse: SS_REQUEST_GET_CALL_WAITING");
                    int iIntValue10 = ((Integer) arrayList.get(1)).intValue();
                    message4 = (Message) arrayList.get(2);
                    if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                        ImsException imsException7 = asyncResult.exception;
                        if (imsException7.getCode() == 61446) {
                            this.mCi.queryCallWaiting(iIntValue10, message4);
                            this.mPhone.setCsFallbackStatus(2);
                        } else if (imsException7.getCode() == 61447) {
                            this.mCi.queryCallWaiting(iIntValue10, message4);
                        }
                    }
                    if (objImsCFInfoToCFInfo != null) {
                        Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_WAITING ssinfo check.");
                        if (objImsCFInfoToCFInfo instanceof ImsSsInfo[]) {
                            objImsCFInfoToCFInfo = handleCwQueryResult((ImsSsInfo[]) asyncResult.result);
                        }
                    }
                    message = message4;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                break;
            case 6:
                boolean zBooleanValue3 = ((Boolean) arrayList.get(1)).booleanValue();
                int iIntValue11 = ((Integer) arrayList.get(2)).intValue();
                message4 = (Message) arrayList.get(3);
                if (asyncResult.exception == null) {
                    MtkGsmCdmaPhone mtkGsmCdmaPhone2 = this.mPhone;
                    mtkGsmCdmaPhone2.setTbcwMode(1);
                    mtkGsmCdmaPhone2.setTbcwToEnabledOnIfDisabled();
                    mtkGsmCdmaPhone2.setTerminalBasedCallWaiting(zBooleanValue3, message4);
                } else {
                    if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                        ImsException imsException8 = asyncResult.exception;
                        if (imsException8.getCode() == 61446) {
                            this.mCi.setCallWaiting(zBooleanValue3, iIntValue11, message4);
                            this.mPhone.setCsFallbackStatus(2);
                        } else if (imsException8.getCode() == 61447) {
                            this.mCi.setCallWaiting(zBooleanValue3, iIntValue11, message4);
                        }
                    }
                    message = message4;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                break;
            case 7:
                message = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException9 = asyncResult.exception;
                    if (imsException9.getCode() == 61446) {
                        this.mCi.getCLIR(message);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException9.getCode() == 61447) {
                        this.mCi.getCLIR(message);
                    }
                }
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                } else {
                    if (asyncResult.exception == null) {
                        int[] intArray = ((Bundle) objImsCFInfoToCFInfo).getIntArray(MtkImsPhone.UT_BUNDLE_KEY_CLIR);
                        int[] savedClirSetting = this.mPhone.getSavedClirSetting();
                        if (savedClirSetting[0] == 0) {
                            Rlog.d(LOG_TAG, "Set clirInfo[0] to default");
                            intArray[0] = savedClirSetting[0];
                        }
                        Rlog.d(LOG_TAG, "SS_REQUEST_GET_CLIR: CLIR param n=" + intArray[0] + " m=" + intArray[1]);
                        objImsCFInfoToCFInfo = intArray;
                    } else {
                        objImsCFInfoToCFInfo = null;
                    }
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                break;
            case 8:
                int iIntValue12 = ((Integer) arrayList.get(1)).intValue();
                message = (Message) arrayList.get(2);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException10 = asyncResult.exception;
                    if (imsException10.getCode() == 61446) {
                        this.mCi.setCLIR(iIntValue12, message);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException10.getCode() == 61447) {
                        this.mCi.setCLIR(iIntValue12, message);
                    }
                }
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                }
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            case 9:
                message = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException11 = asyncResult.exception;
                    if (imsException11.getCode() == 61446) {
                        this.mCi.queryCLIP(message);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException11.getCode() == 61447) {
                        this.mCi.queryCLIP(message);
                    }
                }
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                } else {
                    iArr = new int[]{0};
                    if (asyncResult.exception == null) {
                        Bundle bundle = (Bundle) objImsCFInfoToCFInfo;
                        if (bundle != null) {
                            ImsSsInfo parcelable = bundle.getParcelable("imsSsInfo");
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
                    objImsCFInfoToCFInfo = iArr;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                break;
            case 10:
                int iIntValue13 = ((Integer) arrayList.get(1)).intValue();
                message = (Message) arrayList.get(2);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException12 = asyncResult.exception;
                    if (imsException12.getCode() == 61446) {
                        this.mPhone.mMtkCi.setCLIP(iIntValue13, message);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException12.getCode() == 61447) {
                        this.mPhone.mMtkCi.setCLIP(iIntValue13, message);
                    }
                }
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                }
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            case 11:
                message = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException13 = asyncResult.exception;
                    if (imsException13.getCode() == 61446) {
                        this.mPhone.mMtkCi.getCOLR(message);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException13.getCode() == 61447) {
                        this.mPhone.mMtkCi.getCOLR(message);
                    }
                }
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                } else {
                    iArr = new int[]{0};
                    if (asyncResult.exception == null) {
                        Bundle bundle2 = (Bundle) objImsCFInfoToCFInfo;
                        if (bundle2 != null) {
                            ImsSsInfo parcelable2 = bundle2.getParcelable("imsSsInfo");
                            if (parcelable2 != null) {
                                Rlog.d(LOG_TAG, "ImsSsInfo mStatus = " + parcelable2.mStatus);
                                iArr[0] = parcelable2.mStatus;
                            } else {
                                Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLR: ssInfo null!");
                            }
                        } else {
                            Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLR: bundle null!");
                        }
                    }
                    objImsCFInfoToCFInfo = iArr;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                break;
            case 12:
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                    message = null;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                message = message4;
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            case 13:
                message = (Message) arrayList.get(1);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException14 = asyncResult.exception;
                    if (imsException14.getCode() == 61446) {
                        this.mPhone.mMtkCi.getCOLP(message);
                        this.mPhone.setCsFallbackStatus(2);
                    } else if (imsException14.getCode() == 61447) {
                        this.mPhone.mMtkCi.getCOLP(message);
                    }
                }
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                } else {
                    iArr = new int[]{0};
                    if (asyncResult.exception == null) {
                        Bundle bundle3 = (Bundle) objImsCFInfoToCFInfo;
                        if (bundle3 != null) {
                            ImsSsInfo parcelable3 = bundle3.getParcelable("imsSsInfo");
                            if (parcelable3 != null) {
                                Rlog.d(LOG_TAG, "ImsSsInfo mStatus = " + parcelable3.mStatus);
                                iArr[0] = parcelable3.mStatus;
                            } else {
                                Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLP: ssInfo null!");
                            }
                        } else {
                            Rlog.e(LOG_TAG, "SS_REQUEST_GET_COLP: bundle null!");
                        }
                    }
                    objImsCFInfoToCFInfo = iArr;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                break;
            case 14:
                if (this.mPhone.isOpNotSupportCallIdentity()) {
                    commandException = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    objImsCFInfoToCFInfo = null;
                    message = null;
                    if (commandException != null) {
                    }
                    if (message != null) {
                    }
                }
                message = message4;
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            case 15:
                message = (Message) arrayList.get(3);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException15 = asyncResult.exception;
                    if (imsException15.getCode() == 61446) {
                        Throwable commandException4 = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                        this.mPhone.setCsFallbackStatus(2);
                        commandException = commandException4;
                        objImsCFInfoToCFInfo = null;
                    } else if (imsException15.getCode() == 61447) {
                        if (message != null) {
                            AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
                            message.sendToTarget();
                        }
                    }
                }
                if (objImsCFInfoToCFInfo != null) {
                    Rlog.d(LOG_TAG, "SS_REQUEST_GET_CALL_FORWARD_TIME_SLOT cfinfoEx check.");
                    if (objImsCFInfoToCFInfo instanceof MtkImsCallForwardInfo[]) {
                        objImsCFInfoToCFInfo = imsCFInfoExToCFInfoEx((MtkImsCallForwardInfo[]) objImsCFInfoToCFInfo);
                    }
                }
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            case 16:
                message = (Message) arrayList.get(7);
                if (asyncResult.exception != null && (asyncResult.exception instanceof ImsException)) {
                    ImsException imsException16 = asyncResult.exception;
                    if (imsException16.getCode() == 61446) {
                        Throwable commandException5 = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                        this.mPhone.setCsFallbackStatus(2);
                        commandException = commandException5;
                        objImsCFInfoToCFInfo = null;
                    } else if (imsException16.getCode() == 61447) {
                        if (message != null) {
                            AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED));
                            message.sendToTarget();
                        }
                    }
                }
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
            default:
                message = message4;
                if (commandException != null) {
                }
                if (message != null) {
                }
                break;
        }
    }

    private CommandException getCommandException(ImsException imsException) {
        if (imsException.getCode() == 61449) {
            if (this.mPhone.isEnableXcapHttpResponse409()) {
                Rlog.d(LOG_TAG, "getCommandException UT_XCAP_409_CONFLICT");
                return new CommandException(CommandException.Error.OEM_ERROR_1);
            }
            Rlog.d(LOG_TAG, "getCommandException GENERIC_FAILURE");
            return new CommandException(CommandException.Error.GENERIC_FAILURE);
        }
        Rlog.d(LOG_TAG, "getCommandException GENERIC_FAILURE");
        return new CommandException(CommandException.Error.GENERIC_FAILURE);
    }

    public void queryCallForwardStatus(int i, int i2, String str, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(1));
        arrayList.add(new Integer(i));
        arrayList.add(new Integer(i2));
        arrayList.add(str);
        arrayList.add(message);
        send(arrayList);
    }

    public void setCallForward(int i, int i2, int i3, String str, int i4, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(2));
        arrayList.add(new Integer(i));
        arrayList.add(new Integer(i2));
        arrayList.add(new Integer(i3));
        arrayList.add(str);
        arrayList.add(new Integer(i4));
        arrayList.add(message);
        send(arrayList);
    }

    public void queryCallForwardInTimeSlotStatus(int i, int i2, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(15));
        arrayList.add(new Integer(i));
        arrayList.add(new Integer(i2));
        arrayList.add(message);
        send(arrayList);
    }

    public void setCallForwardInTimeSlot(int i, int i2, int i3, String str, int i4, long[] jArr, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(16));
        arrayList.add(new Integer(i));
        arrayList.add(new Integer(i2));
        arrayList.add(new Integer(i3));
        arrayList.add(str);
        arrayList.add(new Integer(i4));
        arrayList.add(jArr);
        arrayList.add(message);
        send(arrayList);
    }

    public void queryFacilityLock(String str, String str2, int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(3));
        arrayList.add(str);
        arrayList.add(str2);
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    public void setFacilityLock(String str, boolean z, String str2, int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(4));
        arrayList.add(str);
        arrayList.add(new Boolean(z));
        arrayList.add(str2);
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    public void queryCallWaiting(int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(5));
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    public void setCallWaiting(boolean z, int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(6));
        arrayList.add(new Boolean(z));
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    public void getCLIR(Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(7));
        arrayList.add(message);
        send(arrayList);
    }

    public void setCLIR(int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(8));
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    public void getCLIP(Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(9));
        arrayList.add(message);
        send(arrayList);
    }

    public void setCLIP(int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(10));
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    public void getCOLR(Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(11));
        arrayList.add(message);
        send(arrayList);
    }

    public void setCOLR(int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(12));
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    public void getCOLP(Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(13));
        arrayList.add(message);
        send(arrayList);
    }

    public void setCOLP(int i, Message message) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(new Integer(14));
        arrayList.add(new Integer(i));
        arrayList.add(message);
        send(arrayList);
    }

    void send(ArrayList<Object> arrayList) {
        this.mSSRequestHandler.obtainMessage(1, arrayList).sendToTarget();
    }
}
