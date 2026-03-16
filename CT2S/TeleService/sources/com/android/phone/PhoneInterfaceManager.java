package com.android.phone;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.NeighboringCellInfo;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.HexDump;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PhoneInterfaceManager extends ITelephony.Stub {
    private static PhoneInterfaceManager sInstance;
    private int lastError;
    private PhoneGlobals mApp;
    private AppOpsManager mAppOps;
    private Phone mPhone;
    private SharedPreferences mTelephonySharedPreferences;
    private CallManager mCM = PhoneGlobals.getInstance().mCM;
    private MainThreadHandler mMainThreadHandler = new MainThreadHandler();
    private SubscriptionController mSubscriptionController = SubscriptionController.getInstance();

    private static final class IccAPDUArgument {
        public int channel;
        public int cla;
        public int command;
        public String data;
        public int p1;
        public int p2;
        public int p3;

        public IccAPDUArgument(int channel, int cla, int command, int p1, int p2, int p3, String data) {
            this.channel = channel;
            this.cla = cla;
            this.command = command;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.data = data;
        }
    }

    private static final class MainThreadRequest {
        public Object argument;
        public Object result;
        public Integer subId;

        public MainThreadRequest(Object argument, Integer subId) {
            this.argument = argument;
            this.subId = subId;
        }
    }

    private final class MainThreadHandler extends Handler {
        private MainThreadHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            IccOpenLogicalChannelResponse openChannelResp;
            boolean hungUp;
            UiccCard uiccCard = UiccController.getInstance().getUiccCard(PhoneInterfaceManager.this.mPhone.getPhoneId());
            switch (msg.what) {
                case 1:
                    MainThreadRequest request = (MainThreadRequest) msg.obj;
                    request.result = Boolean.valueOf(PhoneInterfaceManager.this.getPhoneFromRequest(request).handlePinMmi((String) request.argument));
                    synchronized (request) {
                        request.notifyAll();
                        break;
                    }
                    return;
                case 2:
                    Message onCompleted = obtainMessage(3, (MainThreadRequest) msg.obj);
                    PhoneInterfaceManager.this.mPhone.getNeighboringCids(onCompleted);
                    return;
                case 3:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    MainThreadRequest request2 = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request2.result = ar.result;
                    } else {
                        request2.result = new ArrayList(0);
                    }
                    synchronized (request2) {
                        request2.notifyAll();
                        break;
                    }
                    return;
                case 4:
                    int answer_subId = ((MainThreadRequest) msg.obj).subId.intValue();
                    PhoneInterfaceManager.this.answerRingingCallInternal(answer_subId);
                    return;
                case 5:
                    MainThreadRequest request3 = (MainThreadRequest) msg.obj;
                    int end_subId = request3.subId.intValue();
                    int phoneType = PhoneInterfaceManager.this.getPhone(end_subId).getPhoneType();
                    if (phoneType == 2) {
                        hungUp = PhoneUtils.hangupRingingAndActive(PhoneInterfaceManager.this.getPhone(end_subId));
                    } else if (phoneType == 1) {
                        hungUp = PhoneUtils.hangup(PhoneInterfaceManager.this.mCM);
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    PhoneInterfaceManager.log("CMD_END_CALL: " + (hungUp ? "hung up!" : "no call to hang up"));
                    request3.result = Boolean.valueOf(hungUp);
                    synchronized (request3) {
                        request3.notifyAll();
                        break;
                    }
                    return;
                case 6:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                default:
                    Log.w("PhoneInterfaceManager", "MainThreadHandler: unexpected message code: " + msg.what);
                    return;
                case 7:
                    MainThreadRequest request4 = (MainThreadRequest) msg.obj;
                    IccAPDUArgument iccArgument = (IccAPDUArgument) request4.argument;
                    if (uiccCard == null) {
                        PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: No UICC");
                        request4.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (request4) {
                            request4.notifyAll();
                            break;
                        }
                        return;
                    }
                    Message onCompleted2 = obtainMessage(8, request4);
                    uiccCard.iccTransmitApduLogicalChannel(iccArgument.channel, iccArgument.cla, iccArgument.command, iccArgument.p1, iccArgument.p2, iccArgument.p3, iccArgument.data, onCompleted2);
                    return;
                case 8:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    MainThreadRequest request5 = (MainThreadRequest) ar2.userObj;
                    if (ar2.exception == null && ar2.result != null) {
                        request5.result = ar2.result;
                        PhoneInterfaceManager.this.lastError = 0;
                    } else {
                        request5.result = new IccIoResult(111, 0, (byte[]) null);
                        PhoneInterfaceManager.this.lastError = 1;
                        if (ar2.result == null) {
                            PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: Empty response");
                        } else if (!(ar2.exception instanceof CommandException)) {
                            PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: Unknown exception");
                        } else {
                            if (ar2.exception.getCommandError() == CommandException.Error.INVALID_PARAMETER) {
                                PhoneInterfaceManager.this.lastError = 5;
                            }
                            PhoneInterfaceManager.loge("iccTransmitApduLogicalChannel: CommandException: " + ar2.exception);
                        }
                    }
                    synchronized (request5) {
                        request5.notifyAll();
                        break;
                    }
                    return;
                case 9:
                    MainThreadRequest request6 = (MainThreadRequest) msg.obj;
                    if (uiccCard == null) {
                        PhoneInterfaceManager.loge("iccOpenLogicalChannel: No UICC");
                        request6.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (request6) {
                            request6.notifyAll();
                            break;
                        }
                        return;
                    }
                    Message onCompleted3 = obtainMessage(10, request6);
                    uiccCard.iccOpenLogicalChannel((String) request6.argument, onCompleted3);
                    return;
                case 10:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    MainThreadRequest request7 = (MainThreadRequest) ar3.userObj;
                    if (ar3.exception != null || ar3.result == null) {
                        PhoneInterfaceManager.this.lastError = 1;
                        if (ar3.result == null) {
                            PhoneInterfaceManager.loge("iccOpenLogicalChannel: Empty response");
                        }
                        if (ar3.exception != null) {
                            PhoneInterfaceManager.loge("iccOpenLogicalChannel: Exception: " + ar3.exception);
                        }
                        int errorCode = 4;
                        if (ar3.exception != null && (ar3.exception instanceof CommandException)) {
                            if (ar3.exception.getMessage().compareTo("MISSING_RESOURCE") == 0) {
                                errorCode = 2;
                                PhoneInterfaceManager.this.lastError = 2;
                            } else if (ar3.exception.getMessage().compareTo("NO_SUCH_ELEMENT") == 0) {
                                errorCode = 3;
                                PhoneInterfaceManager.this.lastError = 3;
                            }
                        }
                        openChannelResp = new IccOpenLogicalChannelResponse(-1, errorCode, null);
                    } else {
                        int[] result = (int[]) ar3.result;
                        int channelId = result[0];
                        byte[] selectResponse = null;
                        if (result.length > 1) {
                            selectResponse = new byte[result.length - 1];
                            for (int i = 1; i < result.length; i++) {
                                selectResponse[i - 1] = (byte) result[i];
                            }
                        }
                        openChannelResp = new IccOpenLogicalChannelResponse(channelId, 1, selectResponse);
                        PhoneInterfaceManager.this.lastError = 0;
                    }
                    request7.result = openChannelResp;
                    synchronized (request7) {
                        request7.notifyAll();
                        break;
                    }
                    return;
                case 11:
                    MainThreadRequest request8 = (MainThreadRequest) msg.obj;
                    if (uiccCard == null) {
                        PhoneInterfaceManager.loge("iccCloseLogicalChannel: No UICC");
                        request8.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (request8) {
                            request8.notifyAll();
                            break;
                        }
                        return;
                    }
                    Message onCompleted4 = obtainMessage(12, request8);
                    uiccCard.iccCloseLogicalChannel(((Integer) request8.argument).intValue(), onCompleted4);
                    return;
                case 12:
                    handleNullReturnEvent(msg, "iccCloseLogicalChannel");
                    return;
                case 13:
                    MainThreadRequest request9 = (MainThreadRequest) msg.obj;
                    Message onCompleted5 = obtainMessage(14, request9);
                    PhoneInterfaceManager.this.mPhone.nvReadItem(((Integer) request9.argument).intValue(), onCompleted5);
                    return;
                case 14:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    MainThreadRequest request10 = (MainThreadRequest) ar4.userObj;
                    if (ar4.exception == null && ar4.result != null) {
                        request10.result = ar4.result;
                    } else {
                        request10.result = "";
                        if (ar4.result == null) {
                            PhoneInterfaceManager.loge("nvReadItem: Empty response");
                        } else if (ar4.exception instanceof CommandException) {
                            PhoneInterfaceManager.loge("nvReadItem: CommandException: " + ar4.exception);
                        } else {
                            PhoneInterfaceManager.loge("nvReadItem: Unknown exception");
                        }
                    }
                    synchronized (request10) {
                        request10.notifyAll();
                        break;
                    }
                    return;
                case 15:
                    MainThreadRequest request11 = (MainThreadRequest) msg.obj;
                    Message onCompleted6 = obtainMessage(16, request11);
                    Pair<Integer, String> idValue = (Pair) request11.argument;
                    PhoneInterfaceManager.this.mPhone.nvWriteItem(((Integer) idValue.first).intValue(), (String) idValue.second, onCompleted6);
                    return;
                case 16:
                    handleNullReturnEvent(msg, "nvWriteItem");
                    return;
                case 17:
                    MainThreadRequest request12 = (MainThreadRequest) msg.obj;
                    Message onCompleted7 = obtainMessage(18, request12);
                    PhoneInterfaceManager.this.mPhone.nvWriteCdmaPrl((byte[]) request12.argument, onCompleted7);
                    return;
                case 18:
                    handleNullReturnEvent(msg, "nvWriteCdmaPrl");
                    return;
                case 19:
                    MainThreadRequest request13 = (MainThreadRequest) msg.obj;
                    Message onCompleted8 = obtainMessage(20, request13);
                    PhoneInterfaceManager.this.mPhone.nvResetConfig(((Integer) request13.argument).intValue(), onCompleted8);
                    return;
                case 20:
                    handleNullReturnEvent(msg, "nvResetConfig");
                    return;
                case 21:
                    MainThreadRequest request14 = (MainThreadRequest) msg.obj;
                    Message onCompleted9 = obtainMessage(22, request14);
                    if (request14.subId == null || !SubscriptionManager.isValidSubscriptionId(request14.subId.intValue())) {
                        PhoneInterfaceManager.this.mPhone.getPreferredNetworkType(onCompleted9);
                        return;
                    } else {
                        PhoneInterfaceManager.this.getPhone(request14.subId.intValue()).getPreferredNetworkType(onCompleted9);
                        return;
                    }
                case 22:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    MainThreadRequest request15 = (MainThreadRequest) ar5.userObj;
                    if (ar5.exception == null && ar5.result != null) {
                        request15.result = ar5.result;
                    } else {
                        request15.result = -1;
                        if (ar5.result == null) {
                            PhoneInterfaceManager.loge("getPreferredNetworkType: Empty response");
                        } else if (ar5.exception instanceof CommandException) {
                            PhoneInterfaceManager.loge("getPreferredNetworkType: CommandException: " + ar5.exception);
                        } else {
                            PhoneInterfaceManager.loge("getPreferredNetworkType: Unknown exception");
                        }
                    }
                    synchronized (request15) {
                        request15.notifyAll();
                        break;
                    }
                    return;
                case 23:
                    MainThreadRequest request16 = (MainThreadRequest) msg.obj;
                    Message onCompleted10 = obtainMessage(24, request16);
                    int networkType = ((Integer) request16.argument).intValue();
                    if (request16.subId == null || !SubscriptionManager.isValidSubscriptionId(request16.subId.intValue())) {
                        PhoneInterfaceManager.this.mPhone.setPreferredNetworkType(networkType, onCompleted10);
                        return;
                    } else {
                        PhoneInterfaceManager.this.getPhone(request16.subId.intValue()).setPreferredNetworkType(networkType, onCompleted10);
                        return;
                    }
                case 24:
                    handleNullReturnEvent(msg, "setPreferredNetworkType");
                    return;
                case 25:
                    MainThreadRequest request17 = (MainThreadRequest) msg.obj;
                    if (uiccCard == null) {
                        PhoneInterfaceManager.loge("sendEnvelopeWithStatus: No UICC");
                        request17.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (request17) {
                            request17.notifyAll();
                            break;
                        }
                        return;
                    }
                    Message onCompleted11 = obtainMessage(26, request17);
                    uiccCard.sendEnvelopeWithStatus((String) request17.argument, onCompleted11);
                    return;
                case 26:
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    MainThreadRequest request18 = (MainThreadRequest) ar6.userObj;
                    if (ar6.exception == null && ar6.result != null) {
                        request18.result = ar6.result;
                    } else {
                        request18.result = new IccIoResult(111, 0, (byte[]) null);
                        if (ar6.result == null) {
                            PhoneInterfaceManager.loge("sendEnvelopeWithStatus: Empty response");
                        } else if (ar6.exception instanceof CommandException) {
                            PhoneInterfaceManager.loge("sendEnvelopeWithStatus: CommandException: " + ar6.exception);
                        } else {
                            PhoneInterfaceManager.loge("sendEnvelopeWithStatus: exception:" + ar6.exception);
                        }
                    }
                    synchronized (request18) {
                        request18.notifyAll();
                        break;
                    }
                    return;
                case 27:
                    MainThreadRequest request19 = (MainThreadRequest) msg.obj;
                    Message onCompleted12 = obtainMessage(28, request19);
                    PhoneInterfaceManager.this.mPhone.invokeOemRilRequestRaw((byte[]) request19.argument, onCompleted12);
                    return;
                case 28:
                    AsyncResult ar7 = (AsyncResult) msg.obj;
                    MainThreadRequest request20 = (MainThreadRequest) ar7.userObj;
                    request20.result = ar7;
                    synchronized (request20) {
                        request20.notifyAll();
                        break;
                    }
                    return;
                case 29:
                    MainThreadRequest request21 = (MainThreadRequest) msg.obj;
                    IccAPDUArgument iccArgument2 = (IccAPDUArgument) request21.argument;
                    if (uiccCard == null) {
                        PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: No UICC");
                        request21.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (request21) {
                            request21.notifyAll();
                            break;
                        }
                        return;
                    }
                    Message onCompleted13 = obtainMessage(30, request21);
                    uiccCard.iccTransmitApduBasicChannel(iccArgument2.cla, iccArgument2.command, iccArgument2.p1, iccArgument2.p2, iccArgument2.p3, iccArgument2.data, onCompleted13);
                    return;
                case 30:
                    AsyncResult ar8 = (AsyncResult) msg.obj;
                    MainThreadRequest request22 = (MainThreadRequest) ar8.userObj;
                    if (ar8.exception == null && ar8.result != null) {
                        request22.result = ar8.result;
                        PhoneInterfaceManager.this.lastError = 0;
                    } else {
                        request22.result = new IccIoResult(111, 0, (byte[]) null);
                        PhoneInterfaceManager.this.lastError = 1;
                        if (ar8.result == null) {
                            PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: Empty response");
                        } else if (!(ar8.exception instanceof CommandException)) {
                            PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: Unknown exception");
                        } else {
                            if (ar8.exception.getCommandError() == CommandException.Error.INVALID_PARAMETER) {
                                PhoneInterfaceManager.this.lastError = 5;
                            }
                            PhoneInterfaceManager.loge("iccTransmitApduBasicChannel: CommandException: " + ar8.exception);
                        }
                    }
                    synchronized (request22) {
                        request22.notifyAll();
                        break;
                    }
                    return;
                case 31:
                    MainThreadRequest request23 = (MainThreadRequest) msg.obj;
                    IccAPDUArgument iccArgument3 = (IccAPDUArgument) request23.argument;
                    if (uiccCard == null) {
                        PhoneInterfaceManager.loge("iccExchangeSimIO: No UICC");
                        request23.result = new IccIoResult(111, 0, (byte[]) null);
                        synchronized (request23) {
                            request23.notifyAll();
                            break;
                        }
                        return;
                    }
                    Message onCompleted14 = obtainMessage(32, request23);
                    uiccCard.iccExchangeSimIO(iccArgument3.cla, iccArgument3.command, iccArgument3.p1, iccArgument3.p2, iccArgument3.p3, iccArgument3.data, onCompleted14);
                    return;
                case 32:
                    AsyncResult ar9 = (AsyncResult) msg.obj;
                    MainThreadRequest request24 = (MainThreadRequest) ar9.userObj;
                    if (ar9.exception == null && ar9.result != null) {
                        request24.result = ar9.result;
                        PhoneInterfaceManager.this.lastError = 0;
                    } else {
                        request24.result = new IccIoResult(111, 0, (byte[]) null);
                        PhoneInterfaceManager.this.lastError = 1;
                        if (ar9.result == null) {
                            PhoneInterfaceManager.loge("iccExchangeSimIO: Empty response");
                        } else if (!(ar9.exception instanceof CommandException)) {
                            PhoneInterfaceManager.loge("iccExchangeSimIO: Unknown exception");
                        } else {
                            if (ar9.exception.getCommandError() == CommandException.Error.INVALID_PARAMETER) {
                                PhoneInterfaceManager.this.lastError = 5;
                            }
                            PhoneInterfaceManager.loge("iccExchangeSimIO: CommandException: " + ar9.exception);
                        }
                    }
                    synchronized (request24) {
                        request24.notifyAll();
                        break;
                    }
                    return;
                case 33:
                    MainThreadRequest request25 = (MainThreadRequest) msg.obj;
                    Message onCompleted15 = obtainMessage(34, request25);
                    Pair<String, String> tagNum = (Pair) request25.argument;
                    PhoneInterfaceManager.this.getPhoneFromRequest(request25).setVoiceMailNumber((String) tagNum.first, (String) tagNum.second, onCompleted15);
                    return;
                case 34:
                    handleNullReturnEvent(msg, "setVoicemailNumber");
                    return;
                case 50:
                    MainThreadRequest request26 = (MainThreadRequest) msg.obj;
                    Message onCompleted16 = obtainMessage(51, request26);
                    PhoneInterfaceManager.this.getPhone(request26.subId.intValue()).getSIMLockInfo(onCompleted16);
                    return;
                case 51:
                    AsyncResult ar10 = (AsyncResult) msg.obj;
                    MainThreadRequest request27 = (MainThreadRequest) ar10.userObj;
                    if (ar10.exception == null && ar10.result != null) {
                        request27.result = ar10.result;
                    } else {
                        request27.result = new int[4];
                    }
                    synchronized (request27) {
                        request27.notifyAll();
                        break;
                    }
                    return;
                case 52:
                    MainThreadRequest request28 = (MainThreadRequest) msg.obj;
                    if (uiccCard == null) {
                        PhoneInterfaceManager.loge("iccGetAtr: No UICC");
                        request28.result = null;
                        synchronized (request28) {
                            request28.notifyAll();
                            break;
                        }
                        return;
                    }
                    Message onCompleted17 = obtainMessage(53, request28);
                    uiccCard.iccGetAtr(onCompleted17);
                    return;
                case 53:
                    AsyncResult ar11 = (AsyncResult) msg.obj;
                    MainThreadRequest request29 = (MainThreadRequest) ar11.userObj;
                    if (ar11.exception == null) {
                        request29.result = (byte[]) ar11.result;
                    } else {
                        request29.result = null;
                    }
                    synchronized (request29) {
                        request29.notifyAll();
                        break;
                    }
                    return;
            }
        }

        private void handleNullReturnEvent(Message msg, String command) {
            AsyncResult ar = (AsyncResult) msg.obj;
            MainThreadRequest request = (MainThreadRequest) ar.userObj;
            if (ar.exception == null) {
                request.result = true;
                PhoneInterfaceManager.this.lastError = 0;
            } else {
                request.result = false;
                PhoneInterfaceManager.this.lastError = 1;
                if (ar.exception instanceof CommandException) {
                    PhoneInterfaceManager.loge(command + ": CommandException: " + ar.exception);
                    if (ar.exception.getCommandError() == CommandException.Error.INVALID_PARAMETER) {
                        PhoneInterfaceManager.this.lastError = 5;
                    }
                } else {
                    PhoneInterfaceManager.loge(command + ": Unknown exception");
                }
            }
            synchronized (request) {
                request.notifyAll();
            }
        }
    }

    private Object sendRequest(int command, Object argument) {
        return sendRequest(command, argument, null);
    }

    private Object sendRequest(int command, Object argument, Integer subId) {
        if (Looper.myLooper() == this.mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }
        MainThreadRequest request = new MainThreadRequest(argument, subId);
        Message msg = this.mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();
        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return request.result;
    }

    static PhoneInterfaceManager init(PhoneGlobals app, Phone phone) {
        PhoneInterfaceManager phoneInterfaceManager;
        synchronized (PhoneInterfaceManager.class) {
            if (sInstance == null) {
                sInstance = new PhoneInterfaceManager(app, phone);
            } else {
                Log.wtf("PhoneInterfaceManager", "init() called multiple times!  sInstance = " + sInstance);
            }
            phoneInterfaceManager = sInstance;
        }
        return phoneInterfaceManager;
    }

    private PhoneInterfaceManager(PhoneGlobals app, Phone phone) {
        this.mApp = app;
        this.mPhone = phone;
        this.mAppOps = (AppOpsManager) app.getSystemService("appops");
        this.mTelephonySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        publish();
    }

    private void publish() {
        log("publish: " + this);
        ServiceManager.addService("phone", this);
    }

    private Phone getPhoneFromRequest(MainThreadRequest request) {
        return request.subId == null ? this.mPhone : getPhone(request.subId.intValue());
    }

    private Phone getPhone(int subId) {
        return SubscriptionManager.isValidSubscriptionId(subId) ? PhoneFactory.getPhone(this.mSubscriptionController.getPhoneId(subId)) : this.mPhone;
    }

    public void dial(String number) {
        dialForSubscriber(getPreferredVoiceSubscription(), number);
    }

    public void dialForSubscriber(int subId, String number) {
        PhoneConstants.State state;
        log("dial: " + number);
        String url = createTelUrl(number);
        if (url != null && (state = this.mCM.getState(subId)) != PhoneConstants.State.OFFHOOK && state != PhoneConstants.State.RINGING) {
            Intent intent = new Intent("android.intent.action.DIAL", Uri.parse(url));
            intent.addFlags(268435456);
            this.mApp.startActivity(intent);
        }
    }

    public void call(String callingPackage, String number) {
        callForSubscriber(getPreferredVoiceSubscription(), callingPackage, number);
    }

    public void callForSubscriber(int subId, String callingPackage, String number) {
        String url;
        log("call: " + number);
        enforceCallPermission();
        if (this.mAppOps.noteOp(13, Binder.getCallingUid(), callingPackage) == 0 && (url = createTelUrl(number)) != null) {
            boolean isValid = false;
            List<SubscriptionInfo> slist = this.mSubscriptionController.getActiveSubscriptionInfoList();
            if (slist != null) {
                Iterator<SubscriptionInfo> it = slist.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    SubscriptionInfo subInfoRecord = it.next();
                    if (subInfoRecord.getSubscriptionId() == subId) {
                        isValid = true;
                        break;
                    }
                }
            }
            if (isValid) {
                Intent intent = new Intent("android.intent.action.CALL", Uri.parse(url));
                intent.putExtra("subscription", subId);
                intent.addFlags(268435456);
                this.mApp.startActivity(intent);
            }
        }
    }

    public boolean endCall() {
        return endCallForSubscriber(getDefaultSubscription());
    }

    public boolean endCallForSubscriber(int subId) {
        enforceCallPermission();
        return ((Boolean) sendRequest(5, null, new Integer(subId))).booleanValue();
    }

    public void answerRingingCall() {
        answerRingingCallForSubscriber(getDefaultSubscription());
    }

    public void answerRingingCallForSubscriber(int subId) {
        log("answerRingingCall...");
        enforceModifyPermission();
        sendRequest(4, null, new Integer(subId));
    }

    private void answerRingingCallInternal(int subId) {
        boolean hasRingingCall = !getPhone(subId).getRingingCall().isIdle();
        if (hasRingingCall) {
            boolean hasActiveCall = !getPhone(subId).getForegroundCall().isIdle();
            boolean hasHoldingCall = !getPhone(subId).getBackgroundCall().isIdle();
            if (hasActiveCall && hasHoldingCall) {
                PhoneUtils.answerAndEndActive(this.mCM, this.mCM.getFirstActiveRingingCall());
            } else {
                PhoneUtils.answerCall(this.mCM.getFirstActiveRingingCall());
            }
        }
    }

    public void silenceRinger() {
        Log.e("PhoneInterfaceManager", "silenseRinger not supported");
    }

    public boolean isOffhook() {
        return isOffhookForSubscriber(getDefaultSubscription());
    }

    public boolean isOffhookForSubscriber(int subId) {
        return getPhone(subId).getState() == PhoneConstants.State.OFFHOOK;
    }

    public boolean isRinging() {
        return isRingingForSubscriber(getDefaultSubscription());
    }

    public boolean isRingingForSubscriber(int subId) {
        return getPhone(subId).getState() == PhoneConstants.State.RINGING;
    }

    public boolean isIdle() {
        return isIdleForSubscriber(getDefaultSubscription());
    }

    public boolean isIdleForSubscriber(int subId) {
        return getPhone(subId).getState() == PhoneConstants.State.IDLE;
    }

    public boolean isSimPinEnabled() {
        enforceReadPermission();
        return PhoneGlobals.getInstance().isSimPinEnabled();
    }

    public boolean supplyPin(String pin) {
        return supplyPinForSubscriber(getDefaultSubscription(), pin);
    }

    public boolean supplyPinForSubscriber(int subId, String pin) {
        int[] resultArray = supplyPinReportResultForSubscriber(subId, pin);
        return resultArray[0] == 0;
    }

    public boolean supplyPuk(String puk, String pin) {
        return supplyPukForSubscriber(getDefaultSubscription(), puk, pin);
    }

    public boolean supplyPukForSubscriber(int subId, String puk, String pin) {
        int[] resultArray = supplyPukReportResultForSubscriber(subId, puk, pin);
        return resultArray[0] == 0;
    }

    public int[] supplyPinReportResult(String pin) {
        return supplyPinReportResultForSubscriber(getDefaultSubscription(), pin);
    }

    public int[] supplyPinReportResultForSubscriber(int subId, String pin) {
        enforceModifyPermission();
        UnlockSim checkSimPin = new UnlockSim(getPhone(subId).getIccCard());
        checkSimPin.start();
        return checkSimPin.unlockSim(null, pin);
    }

    public int[] supplyPukReportResult(String puk, String pin) {
        return supplyPukReportResultForSubscriber(getDefaultSubscription(), puk, pin);
    }

    public int[] supplyPukReportResultForSubscriber(int subId, String puk, String pin) {
        enforceModifyPermission();
        UnlockSim checkSimPuk = new UnlockSim(getPhone(subId).getIccCard());
        checkSimPuk.start();
        return checkSimPuk.unlockSim(puk, pin);
    }

    private static class UnlockSim extends Thread {
        private Handler mHandler;
        private final IccCard mSimCard;
        private boolean mDone = false;
        private int mResult = 2;
        private int mRetryCount = -1;

        public UnlockSim(IccCard simCard) {
            this.mSimCard = simCard;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (this) {
                this.mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case 100:
                                Log.d("PhoneInterfaceManager", "SUPPLY_PIN_COMPLETE");
                                synchronized (UnlockSim.this) {
                                    UnlockSim.this.mRetryCount = msg.arg1;
                                    if (ar.exception == null) {
                                        UnlockSim.this.mResult = 0;
                                    } else if (!(ar.exception instanceof CommandException) || ar.exception.getCommandError() != CommandException.Error.PASSWORD_INCORRECT) {
                                        UnlockSim.this.mResult = 2;
                                    } else {
                                        UnlockSim.this.mResult = 1;
                                    }
                                    UnlockSim.this.mDone = true;
                                    UnlockSim.this.notifyAll();
                                    break;
                                }
                                return;
                            default:
                                return;
                        }
                    }
                };
                notifyAll();
            }
            Looper.loop();
        }

        synchronized int[] unlockSim(String puk, String pin) {
            int[] resultArray;
            while (this.mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Message callback = Message.obtain(this.mHandler, 100);
            if (puk == null) {
                this.mSimCard.supplyPin(pin, callback);
            } else {
                this.mSimCard.supplyPuk(puk, pin, callback);
            }
            while (!this.mDone) {
                try {
                    Log.d("PhoneInterfaceManager", "wait for done");
                    wait();
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                }
            }
            Log.d("PhoneInterfaceManager", "done");
            resultArray = new int[]{this.mResult, this.mRetryCount};
            return resultArray;
        }
    }

    public void updateServiceLocation() {
        updateServiceLocationForSubscriber(getDefaultSubscription());
    }

    public void updateServiceLocationForSubscriber(int subId) {
        getPhone(subId).updateServiceLocation();
    }

    public boolean isRadioOn() {
        return isRadioOnForSubscriber(getDefaultSubscription());
    }

    public boolean isRadioOnForSubscriber(int subId) {
        return getPhone(subId).getServiceState().getState() != 3;
    }

    public void toggleRadioOnOff() {
        toggleRadioOnOffForSubscriber(getDefaultSubscription());
    }

    public void toggleRadioOnOffForSubscriber(int subId) {
        enforceModifyPermission();
        getPhone(subId).setRadioPower(!isRadioOnForSubscriber(subId));
    }

    public boolean setRadio(boolean turnOn) {
        return setRadioForSubscriber(getDefaultSubscription(), turnOn);
    }

    public boolean setRadioForSubscriber(int subId, boolean turnOn) {
        enforceModifyPermission();
        if ((getPhone(subId).getServiceState().getState() != 3) != turnOn) {
            toggleRadioOnOffForSubscriber(subId);
        }
        return true;
    }

    public boolean needMobileRadioShutdown() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null && phone.isRadioAvailable()) {
                return true;
            }
        }
        logv(TelephonyManager.getDefault().getPhoneCount() + " Phones are shutdown.");
        return false;
    }

    public void shutdownMobileRadios() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            logv("Shutting down Phone " + i);
            shutdownRadioUsingPhoneId(i);
        }
    }

    private void shutdownRadioUsingPhoneId(int phoneId) {
        enforceModifyPermission();
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null && phone.isRadioAvailable()) {
            phone.shutdownRadio();
        }
    }

    public boolean setRadioPower(boolean turnOn) {
        return setRadioPowerForSubscriber(getDefaultSubscription(), turnOn);
    }

    public boolean setRadioPowerForSubscriber(int subId, boolean turnOn) {
        enforceModifyPermission();
        getPhone(subId).setRadioPower(turnOn);
        return true;
    }

    public boolean enableDataConnectivity() {
        enforceModifyPermission();
        int subId = this.mSubscriptionController.getDefaultDataSubId();
        getPhone(subId).setDataEnabled(true);
        return true;
    }

    public boolean disableDataConnectivity() {
        enforceModifyPermission();
        int subId = this.mSubscriptionController.getDefaultDataSubId();
        getPhone(subId).setDataEnabled(false);
        return true;
    }

    public boolean isDataConnectivityPossible() {
        int subId = this.mSubscriptionController.getDefaultDataSubId();
        return getPhone(subId).isDataConnectivityPossible();
    }

    public boolean handlePinMmi(String dialString) {
        return handlePinMmiForSubscriber(getDefaultSubscription(), dialString);
    }

    public boolean handlePinMmiForSubscriber(int subId, String dialString) {
        enforceModifyPermission();
        return ((Boolean) sendRequest(1, dialString, Integer.valueOf(subId))).booleanValue();
    }

    public int getCallState() {
        return getCallStateForSubscriber(getDefaultSubscription());
    }

    public int getCallStateForSubscriber(int subId) {
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            return DefaultPhoneNotifier.convertCallState(getPhone(subId).getState());
        }
        return 0;
    }

    public int getDataState() {
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        return DefaultPhoneNotifier.convertDataState(phone.getDataConnectionState());
    }

    public int getDataActivity() {
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        return DefaultPhoneNotifier.convertDataActivityState(phone.getDataActivityState());
    }

    public Bundle getCellLocation() {
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        } catch (SecurityException e) {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", null);
        }
        if (!checkIfCallerIsSelfOrForegroundUser()) {
            return null;
        }
        Bundle data = new Bundle();
        Phone phone = getPhone(this.mSubscriptionController.getDefaultDataSubId());
        phone.getCellLocation().fillInNotifierBundle(data);
        return data;
    }

    public void enableLocationUpdates() {
        enableLocationUpdatesForSubscriber(getDefaultSubscription());
    }

    public void enableLocationUpdatesForSubscriber(int subId) {
        this.mApp.enforceCallingOrSelfPermission("android.permission.CONTROL_LOCATION_UPDATES", null);
        getPhone(subId).enableLocationUpdates();
    }

    public void disableLocationUpdates() {
        disableLocationUpdatesForSubscriber(getDefaultSubscription());
    }

    public void disableLocationUpdatesForSubscriber(int subId) {
        this.mApp.enforceCallingOrSelfPermission("android.permission.CONTROL_LOCATION_UPDATES", null);
        getPhone(subId).disableLocationUpdates();
    }

    public List<NeighboringCellInfo> getNeighboringCellInfo(String callingPackage) {
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        } catch (SecurityException e) {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", null);
        }
        if (this.mAppOps.noteOp(12, Binder.getCallingUid(), callingPackage) != 0 || !checkIfCallerIsSelfOrForegroundUser()) {
            return null;
        }
        try {
            return (ArrayList) sendRequest(2, null, null);
        } catch (RuntimeException e2) {
            Log.e("PhoneInterfaceManager", "getNeighboringCellInfo " + e2);
            return null;
        }
    }

    public List<CellInfo> getAllCellInfo() {
        List<CellInfo> cellInfos = null;
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION", null);
        } catch (SecurityException e) {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", null);
        }
        if (checkIfCallerIsSelfOrForegroundUser()) {
            cellInfos = new ArrayList<>();
            Phone[] arr$ = PhoneFactory.getPhones();
            for (Phone phone : arr$) {
                cellInfos.addAll(phone.getAllCellInfo());
            }
        }
        return cellInfos;
    }

    public void setCellInfoListRate(int rateInMillis) {
        enforceModifyPermission();
        this.mPhone.setCellInfoListRate(rateInMillis);
    }

    private static boolean checkIfCallerIsSelfOrForegroundUser() {
        boolean self = Binder.getCallingUid() == Process.myUid();
        if (!self) {
            int callingUser = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();
            try {
                int foregroundUser = ActivityManager.getCurrentUser();
                boolean ok = foregroundUser == callingUser;
                return ok;
            } catch (Exception e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return true;
    }

    private void enforceReadPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
    }

    private void enforceModifyPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
    }

    private void enforceModifyPermissionOrCarrierPrivilege() {
        int permission = this.mApp.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE");
        if (permission != 0) {
            log("No modify permission, check carrier privilege next.");
            if (getCarrierPrivilegeStatus() != 1) {
                loge("No Carrier Privilege.");
                throw new SecurityException("No modify permission or carrier privilege.");
            }
        }
    }

    private void enforceCarrierPrivilege() {
        if (getCarrierPrivilegeStatus() != 1) {
            loge("No Carrier Privilege.");
            throw new SecurityException("No Carrier Privilege.");
        }
    }

    private void enforceCallPermission() {
        this.mApp.enforceCallingOrSelfPermission("android.permission.CALL_PHONE", null);
    }

    private String createTelUrl(String number) {
        if (TextUtils.isEmpty(number)) {
            return null;
        }
        return "tel:" + number;
    }

    private static void log(String msg) {
        Log.d("PhoneInterfaceManager", "[PhoneIntfMgr] " + msg);
    }

    private static void logv(String msg) {
        Log.v("PhoneInterfaceManager", "[PhoneIntfMgr] " + msg);
    }

    private static void loge(String msg) {
        Log.e("PhoneInterfaceManager", "[PhoneIntfMgr] " + msg);
    }

    public int getActivePhoneType() {
        return getActivePhoneTypeForSubscriber(getDefaultSubscription());
    }

    public int getActivePhoneTypeForSubscriber(int subId) {
        return getPhone(subId).getPhoneType();
    }

    public int getCdmaEriIconIndex() {
        return getCdmaEriIconIndexForSubscriber(getDefaultSubscription());
    }

    public int getCdmaEriIconIndexForSubscriber(int subId) {
        return getPhone(subId).getCdmaEriIconIndex();
    }

    public int getCdmaEriIconMode() {
        return getCdmaEriIconModeForSubscriber(getDefaultSubscription());
    }

    public int getCdmaEriIconModeForSubscriber(int subId) {
        return getPhone(subId).getCdmaEriIconMode();
    }

    public String getCdmaEriText() {
        return getCdmaEriTextForSubscriber(getDefaultSubscription());
    }

    public String getCdmaEriTextForSubscriber(int subId) {
        return getPhone(subId).getCdmaEriText();
    }

    public String getCdmaMdn(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (this.mPhone.getPhoneType() == 2) {
            return getPhone(subId).getLine1Number();
        }
        return null;
    }

    public String getCdmaMin(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (this.mPhone.getPhoneType() == 2) {
            return getPhone(subId).getCdmaMin();
        }
        return null;
    }

    public boolean needsOtaServiceProvisioning() {
        return this.mPhone.needsOtaServiceProvisioning();
    }

    public boolean setVoiceMailNumber(int subId, String alphaTag, String number) {
        enforceCarrierPrivilege();
        Boolean success = (Boolean) sendRequest(33, new Pair(alphaTag, number), new Integer(subId));
        return success.booleanValue();
    }

    public int getVoiceMessageCount() {
        return getVoiceMessageCountForSubscriber(getDefaultSubscription());
    }

    public int getVoiceMessageCountForSubscriber(int subId) {
        return getPhone(subId).getVoiceMessageCount();
    }

    public int getNetworkType() {
        return getNetworkTypeForSubscriber(getDefaultSubscription());
    }

    public int getNetworkTypeForSubscriber(int subId) {
        return getPhone(subId).getServiceState().getDataNetworkType();
    }

    public int getDataNetworkType() {
        return getDataNetworkTypeForSubscriber(getDefaultSubscription());
    }

    public int getDataNetworkTypeForSubscriber(int subId) {
        return getPhone(subId).getServiceState().getDataNetworkType();
    }

    public int getVoiceNetworkType() {
        return getVoiceNetworkTypeForSubscriber(getDefaultSubscription());
    }

    public int getVoiceNetworkTypeForSubscriber(int subId) {
        return getPhone(subId).getServiceState().getVoiceNetworkType();
    }

    public boolean hasIccCard() {
        return hasIccCardUsingSlotId(this.mSubscriptionController.getSlotId(getDefaultSubscription()));
    }

    public boolean hasIccCardUsingSlotId(int slotId) {
        int[] subId = this.mSubscriptionController.getSubIdUsingSlotId(slotId);
        if (subId == null || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            return false;
        }
        return getPhone(subId[0]).getIccCard().hasIccCard();
    }

    public int getLteOnCdmaMode() {
        return getLteOnCdmaModeForSubscriber(getDefaultSubscription());
    }

    public int getLteOnCdmaModeForSubscriber(int subId) {
        return getPhone(subId).getLteOnCdmaMode();
    }

    private int getDefaultSubscription() {
        return this.mSubscriptionController.getDefaultSubId();
    }

    private int getPreferredVoiceSubscription() {
        return this.mSubscriptionController.getDefaultVoiceSubId();
    }

    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("iccOpenLogicalChannel: " + AID);
        IccOpenLogicalChannelResponse response = (IccOpenLogicalChannelResponse) sendRequest(9, AID);
        log("iccOpenLogicalChannel: " + response);
        return response;
    }

    public boolean iccCloseLogicalChannel(int channel) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("iccCloseLogicalChannel: " + channel);
        if (channel < 0) {
            return false;
        }
        Boolean success = (Boolean) sendRequest(11, Integer.valueOf(channel));
        log("iccCloseLogicalChannel: " + success);
        return success.booleanValue();
    }

    public String iccTransmitApduLogicalChannel(int channel, int cla, int command, int p1, int p2, int p3, String data) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("iccTransmitApduLogicalChannel: chnl=" + channel + " cla=" + cla + " cmd=" + command + " p1=" + p1 + " p2=" + p2 + " p3=" + p3 + " data=" + data);
        if (channel < 0) {
            return "";
        }
        IccIoResult response = (IccIoResult) sendRequest(7, new IccAPDUArgument(channel, cla, command, p1, p2, p3, data));
        log("iccTransmitApduLogicalChannel: " + response);
        String s = Integer.toHexString((response.sw1 << 8) + response.sw2 + 65536).substring(1);
        if (response.payload != null) {
            return IccUtils.bytesToHexString(response.payload) + s;
        }
        return s;
    }

    public String iccTransmitApduBasicChannel(int cla, int command, int p1, int p2, int p3, String data) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("iccTransmitApduBasicChannel: cla=" + cla + " cmd=" + command + " p1=" + p1 + " p2=" + p2 + " p3=" + p3 + " data=" + data);
        IccIoResult response = (IccIoResult) sendRequest(29, new IccAPDUArgument(0, cla, command, p1, p2, p3, data));
        log("iccTransmitApduBasicChannel: " + response);
        String s = Integer.toHexString((response.sw1 << 8) + response.sw2 + 65536).substring(1);
        if (response.payload != null) {
            return IccUtils.bytesToHexString(response.payload) + s;
        }
        return s;
    }

    public byte[] iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String filePath) {
        byte[] result;
        enforceModifyPermissionOrCarrierPrivilege();
        log("Exchange SIM_IO " + fileID + ":" + command + " " + p1 + " " + p2 + " " + p3 + ":" + filePath);
        IccIoResult response = (IccIoResult) sendRequest(31, new IccAPDUArgument(-1, fileID, command, p1, p2, p3, filePath));
        log("Exchange SIM_IO [R]" + response);
        int length = 2;
        if (response.payload != null) {
            length = response.payload.length + 2;
            result = new byte[length];
            System.arraycopy(response.payload, 0, result, 0, response.payload.length);
        } else {
            result = new byte[2];
        }
        result[length - 1] = (byte) response.sw2;
        result[length - 2] = (byte) response.sw1;
        return result;
    }

    public String sendEnvelopeWithStatus(String content) {
        enforceModifyPermissionOrCarrierPrivilege();
        IccIoResult response = (IccIoResult) sendRequest(25, content);
        if (response.payload == null) {
            return "";
        }
        String s = Integer.toHexString((response.sw1 << 8) + response.sw2 + 65536).substring(1);
        return IccUtils.bytesToHexString(response.payload) + s;
    }

    public String nvReadItem(int itemID) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("nvReadItem: item " + itemID);
        String value = (String) sendRequest(13, Integer.valueOf(itemID));
        log("nvReadItem: item " + itemID + " is \"" + value + '\"');
        return value;
    }

    public boolean nvWriteItem(int itemID, String itemValue) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("nvWriteItem: item " + itemID + " value \"" + itemValue + '\"');
        Boolean success = (Boolean) sendRequest(15, new Pair(Integer.valueOf(itemID), itemValue));
        log("nvWriteItem: item " + itemID + ' ' + (success.booleanValue() ? "ok" : "fail"));
        return success.booleanValue();
    }

    public boolean nvWriteCdmaPrl(byte[] preferredRoamingList) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("nvWriteCdmaPrl: value: " + HexDump.toHexString(preferredRoamingList));
        Boolean success = (Boolean) sendRequest(17, preferredRoamingList);
        log("nvWriteCdmaPrl: " + (success.booleanValue() ? "ok" : "fail"));
        return success.booleanValue();
    }

    public boolean nvResetConfig(int resetType) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("nvResetConfig: type " + resetType);
        Boolean success = (Boolean) sendRequest(19, Integer.valueOf(resetType));
        log("nvResetConfig: type " + resetType + ' ' + (success.booleanValue() ? "ok" : "fail"));
        return success.booleanValue();
    }

    public int getDefaultSim() {
        return 0;
    }

    public String[] getPcscfAddress(String apnType) {
        enforceReadPermission();
        return this.mPhone.getPcscfAddress(apnType);
    }

    public void setImsRegistrationState(boolean registered) {
        enforceModifyPermission();
        this.mPhone.setImsRegistrationState(registered);
    }

    public int getCalculatedPreferredNetworkType() {
        enforceReadPermission();
        return PhoneFactory.calculatePreferredNetworkTypeByPhoneId(this.mPhone.getContext(), Dsds.defaultSimId().ordinal());
    }

    public int getCalculatedPreferredNetworkTypeForSubscription(int subId) {
        enforceReadPermission();
        return PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), subId);
    }

    public int getPreferredNetworkType() {
        enforceModifyPermissionOrCarrierPrivilege();
        log("getPreferredNetworkType");
        int[] result = (int[]) sendRequest(21, null);
        int networkType = result != null ? result[0] : -1;
        log("getPreferredNetworkType: " + networkType);
        return networkType;
    }

    public int getPreferredNetworkTypeForSubscription(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("getPreferredNetworkType");
        int[] result = (int[]) sendRequest(21, null, Integer.valueOf(subId));
        int networkType = result != null ? result[0] : -1;
        log("getPreferredNetworkType: " + networkType);
        return networkType;
    }

    public boolean setPreferredNetworkType(int networkType) {
        enforceModifyPermissionOrCarrierPrivilege();
        this.mPhone.getSubId();
        log("setPreferredNetworkType: type " + networkType);
        Boolean success = (Boolean) sendRequest(23, Integer.valueOf(networkType));
        log("setPreferredNetworkType: " + (success.booleanValue() ? "ok" : "fail"));
        if (success.booleanValue()) {
            PhoneFactory.saveNetworkMode(this.mApp, this.mPhone.getPhoneId(), networkType);
        }
        return success.booleanValue();
    }

    public boolean setPreferredNetworkTypeForSubscription(int networkType, int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        log("setPreferredNetworkType: type " + networkType);
        Boolean success = (Boolean) sendRequest(23, Integer.valueOf(networkType), Integer.valueOf(subId));
        log("setPreferredNetworkType: " + (success.booleanValue() ? "ok" : "fail"));
        if (success.booleanValue()) {
            PhoneFactory.saveNetworkMode(this.mApp, getPhone(subId).getPhoneId(), networkType);
        }
        return success.booleanValue();
    }

    public int getTetherApnRequired() {
        enforceModifyPermissionOrCarrierPrivilege();
        int dunRequired = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "tether_dun_required", 2);
        if (dunRequired == 2 && this.mPhone.hasMatchedTetherApnSetting()) {
            return 1;
        }
        return dunRequired;
    }

    public void setDataEnabled(int subId, boolean enable) {
        enforceModifyPermission();
        int phoneId = this.mSubscriptionController.getPhoneId(subId);
        log("getDataEnabled: subId=" + subId + " phoneId=" + phoneId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            log("setDataEnabled: subId=" + subId + " enable=" + enable);
            phone.setDataEnabled(enable);
        } else {
            loge("setDataEnabled: no phone for subId=" + subId);
        }
    }

    public boolean getDataEnabled(int subId) {
        try {
            this.mApp.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", null);
        } catch (Exception e) {
            this.mApp.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        }
        int phoneId = this.mSubscriptionController.getPhoneId(subId);
        log("getDataEnabled: subId=" + subId + " phoneId=" + phoneId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            boolean retVal = phone.getDataEnabled();
            log("getDataEnabled: subId=" + subId + " retVal=" + retVal);
            return retVal;
        }
        loge("getDataEnabled: no phone subId=" + subId + " retVal=false");
        return false;
    }

    public int getCarrierPrivilegeStatus() {
        UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (card != null) {
            return card.getCarrierPrivilegeStatusForCurrentTransaction(this.mPhone.getContext().getPackageManager());
        }
        loge("getCarrierPrivilegeStatus: No UICC");
        return -1;
    }

    public int checkCarrierPrivilegesForPackage(String pkgname) {
        UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (card != null) {
            return card.getCarrierPrivilegeStatus(this.mPhone.getContext().getPackageManager(), pkgname);
        }
        loge("checkCarrierPrivilegesForPackage: No UICC");
        return -1;
    }

    public List<String> getCarrierPackageNamesForIntent(Intent intent) {
        UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (card != null) {
            return card.getCarrierPackageNamesForIntent(this.mPhone.getContext().getPackageManager(), intent);
        }
        loge("getCarrierPackageNamesForIntent: No UICC");
        return null;
    }

    private String getIccId(int subId) {
        UiccCard card = getPhone(subId).getUiccCard();
        if (card == null) {
            loge("getIccId: No UICC");
            return null;
        }
        String iccId = card.getIccId();
        if (TextUtils.isEmpty(iccId)) {
            loge("getIccId: ICC ID is null or empty.");
            return null;
        }
        return iccId;
    }

    public boolean setLine1NumberForDisplayForSubscriber(int subId, String alphaTag, String number) {
        enforceCarrierPrivilege();
        String iccId = getIccId(subId);
        String subscriberId = getPhone(subId).getSubscriberId();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        SharedPreferences.Editor editor = this.mTelephonySharedPreferences.edit();
        String alphaTagPrefKey = "carrier_alphtag_" + iccId;
        if (alphaTag == null) {
            editor.remove(alphaTagPrefKey);
        } else {
            editor.putString(alphaTagPrefKey, alphaTag);
        }
        String numberPrefKey = "carrier_number_" + iccId;
        String subscriberPrefKey = "carrier_subscriber_" + iccId;
        if (number == null) {
            editor.remove(numberPrefKey);
            editor.remove(subscriberPrefKey);
        } else {
            editor.putString(numberPrefKey, number);
            editor.putString(subscriberPrefKey, subscriberId);
        }
        editor.commit();
        return true;
    }

    public String getLine1NumberForDisplay(int subId) {
        enforceReadPermission();
        String iccId = getIccId(subId);
        if (iccId == null) {
            return null;
        }
        String numberPrefKey = "carrier_number_" + iccId;
        return this.mTelephonySharedPreferences.getString(numberPrefKey, null);
    }

    public String getLine1AlphaTagForDisplay(int subId) {
        enforceReadPermission();
        String iccId = getIccId(subId);
        if (iccId == null) {
            return null;
        }
        String alphaTagPrefKey = "carrier_alphtag_" + iccId;
        return this.mTelephonySharedPreferences.getString(alphaTagPrefKey, null);
    }

    public String[] getMergedSubscriberIds() {
        Context context = this.mPhone.getContext();
        TelephonyManager tele = TelephonyManager.from(context);
        SubscriptionManager sub = SubscriptionManager.from(context);
        ArraySet<String> activeSubscriberIds = new ArraySet<>();
        int[] subIds = sub.getActiveSubscriptionIdList();
        for (int subId : subIds) {
            activeSubscriberIds.add(tele.getSubscriberId(subId));
        }
        String mergeNumber = null;
        Map<String, ?> prefs = this.mTelephonySharedPreferences.getAll();
        for (String key : prefs.keySet()) {
            if (key.startsWith("carrier_subscriber_") && activeSubscriberIds.contains((String) prefs.get(key))) {
                String iccId = key.substring("carrier_subscriber_".length());
                String numberKey = "carrier_number_" + iccId;
                mergeNumber = (String) prefs.get(numberKey);
                if (!TextUtils.isEmpty(mergeNumber)) {
                    break;
                }
            }
        }
        if (TextUtils.isEmpty(mergeNumber)) {
            return null;
        }
        ArraySet<String> result = new ArraySet<>();
        for (String key2 : prefs.keySet()) {
            if (key2.startsWith("carrier_number_")) {
                String number = (String) prefs.get(key2);
                if (mergeNumber.equals(number)) {
                    String iccId2 = key2.substring("carrier_number_".length());
                    String subscriberKey = "carrier_subscriber_" + iccId2;
                    String subscriberId = (String) prefs.get(subscriberKey);
                    if (!TextUtils.isEmpty(subscriberId)) {
                        result.add(subscriberId);
                    }
                }
            }
        }
        String[] resultArray = (String[]) result.toArray(new String[result.size()]);
        Arrays.sort(resultArray);
        return resultArray;
    }

    public boolean setOperatorBrandOverride(String brand) {
        enforceCarrierPrivilege();
        return this.mPhone.setOperatorBrandOverride(brand);
    }

    public boolean setRoamingOverride(List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) {
        enforceCarrierPrivilege();
        return this.mPhone.setRoamingOverride(gsmRoamingList, gsmNonRoamingList, cdmaRoamingList, cdmaNonRoamingList);
    }

    public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) {
        enforceModifyPermission();
        try {
            AsyncResult result = (AsyncResult) sendRequest(27, oemReq);
            if (result.exception == null) {
                if (result.result == null) {
                    return 0;
                }
                byte[] responseData = (byte[]) result.result;
                if (responseData.length > oemResp.length) {
                    Log.w("PhoneInterfaceManager", "Buffer to copy response too small: Response length is " + responseData.length + "bytes. Buffer Size is " + oemResp.length + "bytes.");
                }
                System.arraycopy(responseData, 0, oemResp, 0, responseData.length);
                return responseData.length;
            }
            CommandException ex = result.exception;
            int returnValue = ex.getCommandError().ordinal();
            return returnValue > 0 ? returnValue * (-1) : returnValue;
        } catch (RuntimeException e) {
            Log.w("PhoneInterfaceManager", "sendOemRilRequestRaw: Runtime Exception");
            int returnValue2 = CommandException.Error.GENERIC_FAILURE.ordinal();
            return returnValue2 > 0 ? returnValue2 * (-1) : returnValue2;
        }
    }

    public void setRadioCapability(RadioAccessFamily[] rafs) {
        try {
            ProxyController.getInstance().setRadioCapability(rafs);
        } catch (RuntimeException e) {
            Log.w("PhoneInterfaceManager", "setRadioCapability: Runtime Exception");
        }
    }

    public int getRadioAccessFamily(int phoneId) {
        return ProxyController.getInstance().getRadioAccessFamily(phoneId);
    }

    public void enableVideoCalling(boolean enable) {
        enforceModifyPermission();
        SharedPreferences.Editor editor = this.mTelephonySharedPreferences.edit();
        editor.putBoolean("enable_video_calling", enable);
        editor.commit();
    }

    public boolean isVideoCallingEnabled() {
        enforceReadPermission();
        return ImsManager.isVtEnabledByPlatform(this.mPhone.getContext()) && ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mPhone.getContext()) && this.mTelephonySharedPreferences.getBoolean("enable_video_calling", true);
    }

    public String getDeviceId() {
        enforceReadPermission();
        Phone phone = PhoneFactory.getPhone(0);
        if (phone != null) {
            return phone.getDeviceId();
        }
        return null;
    }

    public boolean isImsRegistered() {
        return this.mPhone.isImsRegistered();
    }

    public int[] getSimLockInfo(int subId) {
        int[] SimRetryStat = {3, 3, 10, 10};
        try {
            int[] SimRetryStat2 = (int[]) sendRequest(50, null, Integer.valueOf(subId));
            return SimRetryStat2;
        } catch (RuntimeException e) {
            Log.e("PhoneInterfaceManager", "getSimLockInfo " + e);
            return SimRetryStat;
        }
    }

    public byte[] transmitIccSimIO(int fileID, int command, int p1, int p2, int p3, String filePath) {
        if (Binder.getCallingUid() != 1036) {
            throw new SecurityException("Only Smartcard API may access UICC");
        }
        return iccExchangeSimIO(fileID, command, p1, p2, p3, filePath);
    }

    public int getLastError() {
        return this.lastError;
    }

    public byte[] getIccATR() {
        if (Binder.getCallingUid() != 1036) {
            throw new SecurityException("Only Smartcard API may access UICC");
        }
        Log.d("PhoneInterfaceManager", "> getIccATR ");
        byte[] result = (byte[]) sendRequest(52, null);
        Log.d("PhoneInterfaceManager", "< getIccATR " + IccUtils.bytesToHexString(result));
        return result;
    }

    public int getUiccAppType(int subId) {
        return getPhone(subId).getCurrentUiccAppType().ordinal();
    }

    public boolean isDataConnectivityPossibleForSubscriber(int subId) {
        return getPhone(subId).isDataConnectivityPossible();
    }

    public boolean isVideoEnabled() {
        ImsPhone imsPhone = getPhone(Dsds.getMasterSubId()).getImsPhone();
        return imsPhone != null && imsPhone.getServiceState().getState() == 0;
    }
}
