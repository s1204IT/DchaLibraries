package com.mediatek.internal.telephony;

import android.app.ActivityThread;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.IccSmsInterfaceManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SmsNumberUtils;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.HexDump;
import com.mediatek.internal.telephony.gsm.MtkSmsMessage;
import com.mediatek.internal.telephony.uicc.MtkSIMFileHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mediatek.telephony.MtkSimSmsInsertStatus;
import mediatek.telephony.MtkSmsParameters;

public class MtkIccSmsInterfaceManager extends IccSmsInterfaceManager {
    private static final int CB_ACTIVATION_OFF = 0;
    private static final int CB_ACTIVATION_ON = 1;
    private static final int CB_ACTIVATION_UNKNOWN = -1;
    static final boolean DBG = true;
    private static final int EVENT_GET_BROADCAST_ACTIVATION_DONE = 106;
    private static final int EVENT_GET_BROADCAST_CONFIG_CHANNEL_DONE = 108;
    private static final int EVENT_GET_BROADCAST_CONFIG_LANGUAGE_DONE = 110;
    private static final int EVENT_GET_SMSC_ADDRESS_BUNDLE_DONE = 113;
    private static final int EVENT_GET_SMSC_ADDRESS_DONE = 112;
    private static final int EVENT_GET_SMS_PARAMS = 103;
    private static final int EVENT_GET_SMS_SIM_MEM_STATUS_DONE = 101;
    private static final int EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE = 102;
    private static final int EVENT_LOAD_ONE_RECORD_DONE = 105;
    private static final int EVENT_MTK_LOAD_DONE = 115;
    private static final int EVENT_MTK_UPDATE_DONE = 116;
    private static final int EVENT_REMOVE_BROADCAST_MSG_DONE = 107;
    private static final int EVENT_SET_BROADCAST_CONFIG_LANGUAGE_DONE = 109;
    private static final int EVENT_SET_ETWS_CONFIG_DONE = 111;
    private static final int EVENT_SET_SMSC_ADDRESS_DONE = 114;
    private static final int EVENT_SET_SMS_PARAMS = 104;
    private static final int EVENT_SIM_SMS_DELETE_DONE = 100;
    private static final int EVENT_SMS_WIPE_DONE = 117;
    private static final String INDEXT_SPLITOR = ",";
    static final String LOG_TAG = "MtkIccSmsInterfaceManager";
    private static int sConcatenatedRef = 456;
    private int mCurrentCellBroadcastActivation;
    private boolean mInsertMessageSuccess;
    private boolean mInserted;
    protected Handler mMtkHandler;
    protected final Object mMtkLoadLock;
    protected final Object mMtkLock;
    protected boolean mMtkSuccess;
    private final Object mSimInsertLock;
    private MtkIccSmsStorageStatus mSimMemStatus;
    private List<SmsRawData> mSms;
    private SmsBroadcastConfigInfo[] mSmsCBConfig;
    private String mSmsCbChannelConfig;
    private String mSmsCbLanguageConfig;
    private MtkSmsParameters mSmsParams;
    private boolean mSmsParamsSuccess;
    private SmsRawData mSmsRawData;
    private BroadcastReceiver mSmsWipeReceiver;
    protected boolean mSmsWipedRsp;
    private String mSmscAddress;
    private Bundle mSmscAddressBundle;
    private MtkSimSmsInsertStatus smsInsertRet;
    private MtkSimSmsInsertStatus smsInsertRet2;

    protected MtkIccSmsInterfaceManager(Phone phone) {
        super(phone);
        this.mMtkLock = new Object();
        this.mMtkLoadLock = new Object();
        this.mSimInsertLock = new Object();
        this.smsInsertRet = new MtkSimSmsInsertStatus(0, "");
        this.smsInsertRet2 = new MtkSimSmsInsertStatus(0, "");
        this.mSmsParams = null;
        this.mSmsParamsSuccess = false;
        this.mSmsRawData = null;
        this.mSmsCBConfig = null;
        this.mSmsCbChannelConfig = "";
        this.mSmsCbLanguageConfig = "";
        this.mSmscAddress = "";
        this.mSmscAddressBundle = new Bundle();
        this.mMtkHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                int i = 0;
                byte b = 1;
                switch (message.what) {
                    case 101:
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult.exception == null) {
                                MtkIccSmsInterfaceManager.this.mMtkSuccess = true;
                                if (MtkIccSmsInterfaceManager.this.mSimMemStatus == null) {
                                    MtkIccSmsInterfaceManager.this.mSimMemStatus = new MtkIccSmsStorageStatus();
                                }
                                MtkIccSmsStorageStatus mtkIccSmsStorageStatus = (MtkIccSmsStorageStatus) asyncResult.result;
                                MtkIccSmsInterfaceManager.this.mSimMemStatus.mUsed = mtkIccSmsStorageStatus.mUsed;
                                MtkIccSmsInterfaceManager.this.mSimMemStatus.mTotal = mtkIccSmsStorageStatus.mTotal;
                            } else {
                                MtkIccSmsInterfaceManager.this.log("Cannot Get Sms SIM Memory Status from SIM");
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 102:
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mSimInsertLock) {
                            MtkIccSmsInterfaceManager.this.mInsertMessageSuccess = asyncResult2.exception == null;
                            if (MtkIccSmsInterfaceManager.this.mInsertMessageSuccess) {
                                try {
                                    int i2 = ((int[]) asyncResult2.result)[0];
                                    StringBuilder sb = new StringBuilder();
                                    MtkSimSmsInsertStatus mtkSimSmsInsertStatus = MtkIccSmsInterfaceManager.this.smsInsertRet;
                                    sb.append(mtkSimSmsInsertStatus.indexInIcc);
                                    sb.append(i2);
                                    sb.append(MtkIccSmsInterfaceManager.INDEXT_SPLITOR);
                                    mtkSimSmsInsertStatus.indexInIcc = sb.toString();
                                    MtkIccSmsInterfaceManager.this.log("insertText save one pdu in index " + i2);
                                } catch (ClassCastException e) {
                                    e.printStackTrace();
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                }
                            } else {
                                MtkIccSmsInterfaceManager.this.log("insertText fail to insert sms into ICC");
                                StringBuilder sb2 = new StringBuilder();
                                MtkSimSmsInsertStatus mtkSimSmsInsertStatus2 = MtkIccSmsInterfaceManager.this.smsInsertRet;
                                sb2.append(mtkSimSmsInsertStatus2.indexInIcc);
                                sb2.append("-1,");
                                mtkSimSmsInsertStatus2.indexInIcc = sb2.toString();
                            }
                            MtkIccSmsInterfaceManager.this.mInserted = true;
                            MtkIccSmsInterfaceManager.this.mSimInsertLock.notifyAll();
                            break;
                        }
                        return;
                    case MtkIccSmsInterfaceManager.EVENT_GET_SMS_PARAMS:
                        AsyncResult asyncResult3 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult3.exception == null) {
                                try {
                                    try {
                                        MtkIccSmsInterfaceManager.this.mSmsParams = (MtkSmsParameters) asyncResult3.result;
                                    } catch (Exception e3) {
                                        MtkIccSmsInterfaceManager.this.log("[EFsmsp fail to get sms params Exception");
                                        e3.printStackTrace();
                                    }
                                } catch (ClassCastException e4) {
                                    MtkIccSmsInterfaceManager.this.log("[EFsmsp fail to get sms params ClassCastException");
                                    e4.printStackTrace();
                                }
                            } else {
                                MtkIccSmsInterfaceManager.this.log("[EFsmsp fail to get sms params");
                                MtkIccSmsInterfaceManager.this.mSmsParams = null;
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 104:
                        AsyncResult asyncResult4 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult4.exception == null) {
                                MtkIccSmsInterfaceManager.this.mSmsParamsSuccess = true;
                            } else {
                                MtkIccSmsInterfaceManager.this.log("[EFsmsp fail to set sms params");
                                MtkIccSmsInterfaceManager.this.mSmsParamsSuccess = false;
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 105:
                        AsyncResult asyncResult5 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult5.exception == null) {
                                try {
                                    byte[] bArr = (byte[]) asyncResult5.result;
                                    if (bArr[0] != 0) {
                                        MtkIccSmsInterfaceManager.this.mSmsRawData = new SmsRawData(bArr);
                                    } else {
                                        MtkIccSmsInterfaceManager.this.log("sms raw data status is FREE");
                                        MtkIccSmsInterfaceManager.this.mSmsRawData = null;
                                    }
                                } catch (ClassCastException e5) {
                                    MtkIccSmsInterfaceManager.this.log("fail to get sms raw data ClassCastException");
                                    e5.printStackTrace();
                                    MtkIccSmsInterfaceManager.this.mSmsRawData = null;
                                }
                            } else {
                                MtkIccSmsInterfaceManager.this.log("fail to get sms raw data rild");
                                MtkIccSmsInterfaceManager.this.mSmsRawData = null;
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case MtkIccSmsInterfaceManager.EVENT_GET_BROADCAST_ACTIVATION_DONE:
                        AsyncResult asyncResult6 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult6.exception == null) {
                                MtkIccSmsInterfaceManager.this.mMtkSuccess = ((int[]) asyncResult6.result)[0] == 1;
                            }
                            MtkIccSmsInterfaceManager.this.log("queryCbActivation: " + MtkIccSmsInterfaceManager.this.mMtkSuccess);
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case MtkIccSmsInterfaceManager.EVENT_REMOVE_BROADCAST_MSG_DONE:
                        AsyncResult asyncResult7 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            MtkIccSmsInterfaceManager.this.mMtkSuccess = asyncResult7.exception == null;
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case MtkIccSmsInterfaceManager.EVENT_GET_BROADCAST_CONFIG_CHANNEL_DONE:
                        AsyncResult asyncResult8 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult8.exception == null) {
                                ArrayList arrayList = (ArrayList) asyncResult8.result;
                                while (i < arrayList.size()) {
                                    SmsBroadcastConfigInfo smsBroadcastConfigInfo = (SmsBroadcastConfigInfo) arrayList.get(i);
                                    if (smsBroadcastConfigInfo.getFromServiceId() == smsBroadcastConfigInfo.getToServiceId()) {
                                        MtkIccSmsInterfaceManager.this.mSmsCbChannelConfig = MtkIccSmsInterfaceManager.this.mSmsCbChannelConfig + smsBroadcastConfigInfo.getFromServiceId();
                                    } else {
                                        MtkIccSmsInterfaceManager.this.mSmsCbChannelConfig = MtkIccSmsInterfaceManager.this.mSmsCbChannelConfig + smsBroadcastConfigInfo.getFromServiceId() + "-" + smsBroadcastConfigInfo.getToServiceId();
                                    }
                                    i++;
                                    if (i != arrayList.size()) {
                                        MtkIccSmsInterfaceManager.this.mSmsCbChannelConfig = MtkIccSmsInterfaceManager.this.mSmsCbChannelConfig + MtkIccSmsInterfaceManager.INDEXT_SPLITOR;
                                    }
                                }
                                MtkIccSmsInterfaceManager.this.log("Channel configuration " + MtkIccSmsInterfaceManager.this.mSmsCbChannelConfig);
                            } else {
                                MtkIccSmsInterfaceManager.this.log("Cannot Get CB configs");
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 109:
                    case 111:
                    case 114:
                        AsyncResult asyncResult9 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            MtkIccSmsInterfaceManager.this.mMtkSuccess = asyncResult9.exception == null;
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 110:
                        AsyncResult asyncResult10 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult10.exception == null) {
                                MtkIccSmsInterfaceManager.this.mSmsCbLanguageConfig = (String) asyncResult10.result;
                                MtkIccSmsInterfaceManager.this.mSmsCbLanguageConfig = MtkIccSmsInterfaceManager.this.mSmsCbLanguageConfig != null ? MtkIccSmsInterfaceManager.this.mSmsCbLanguageConfig : "";
                                MtkIccSmsInterfaceManager.this.log("Language configuration " + MtkIccSmsInterfaceManager.this.mSmsCbLanguageConfig);
                            } else {
                                MtkIccSmsInterfaceManager.this.log("Cannot Get CB configs");
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 112:
                        AsyncResult asyncResult11 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult11.exception == null) {
                                MtkIccSmsInterfaceManager.this.mSmscAddress = (String) asyncResult11.result;
                                MtkIccSmsInterfaceManager.this.log("SMSC address " + MtkIccSmsInterfaceManager.this.mSmscAddress);
                            } else {
                                MtkIccSmsInterfaceManager.this.log("Cannot Get SMSC address");
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 113:
                        AsyncResult asyncResult12 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            MtkIccSmsInterfaceManager.this.mSmscAddressBundle.clear();
                            if (asyncResult12.exception == null) {
                                MtkIccSmsInterfaceManager.this.log("SMSC address " + ((String) asyncResult12.result));
                                MtkIccSmsInterfaceManager.this.mSmscAddressBundle.putByte("errorCode", (byte) 0);
                                MtkIccSmsInterfaceManager.this.mSmscAddressBundle.putCharSequence("scAddress", (String) asyncResult12.result);
                            } else {
                                MtkIccSmsInterfaceManager.this.log("Cannot Get SMSC address");
                                if ((asyncResult12.exception instanceof CommandException) && asyncResult12.exception.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                                    b = 2;
                                }
                                MtkIccSmsInterfaceManager.this.log("Fail to get sc address, error = " + ((int) b));
                                MtkIccSmsInterfaceManager.this.mSmscAddressBundle.putByte("errorCode", b);
                                MtkIccSmsInterfaceManager.this.mSmscAddressBundle.putCharSequence("scAddress", "");
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        return;
                    case 115:
                        AsyncResult asyncResult13 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLoadLock) {
                            if (asyncResult13.exception == null) {
                                MtkIccSmsInterfaceManager.this.mSms = MtkIccSmsInterfaceManager.this.buildValidRawData((ArrayList) asyncResult13.result);
                                MtkIccSmsInterfaceManager.this.markMessagesAsRead((ArrayList) asyncResult13.result);
                            } else {
                                if (Rlog.isLoggable("SMS", 3)) {
                                    MtkIccSmsInterfaceManager.this.log("Cannot load Sms records");
                                }
                                MtkIccSmsInterfaceManager.this.mSms = null;
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLoadLock.notifyAll();
                            break;
                        }
                        return;
                    case 116:
                        AsyncResult asyncResult14 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            MtkIccSmsInterfaceManager.this.mMtkSuccess = asyncResult14.exception == null;
                            if (MtkIccSmsInterfaceManager.this.mMtkSuccess) {
                                try {
                                    try {
                                        int i3 = ((int[]) asyncResult14.result)[0];
                                        StringBuilder sb3 = new StringBuilder();
                                        MtkSimSmsInsertStatus mtkSimSmsInsertStatus3 = MtkIccSmsInterfaceManager.this.smsInsertRet2;
                                        sb3.append(mtkSimSmsInsertStatus3.indexInIcc);
                                        sb3.append(i3);
                                        sb3.append(MtkIccSmsInterfaceManager.INDEXT_SPLITOR);
                                        mtkSimSmsInsertStatus3.indexInIcc = sb3.toString();
                                        MtkIccSmsInterfaceManager.this.log("[insertRaw save one pdu in index " + i3);
                                    } catch (Exception e6) {
                                        e6.printStackTrace();
                                    }
                                } catch (ClassCastException e7) {
                                    e7.printStackTrace();
                                }
                            } else {
                                MtkIccSmsInterfaceManager.this.log("[insertRaw fail to insert raw into ICC");
                                StringBuilder sb4 = new StringBuilder();
                                MtkSimSmsInsertStatus mtkSimSmsInsertStatus4 = MtkIccSmsInterfaceManager.this.smsInsertRet2;
                                sb4.append(mtkSimSmsInsertStatus4.indexInIcc);
                                sb4.append("-1,");
                                mtkSimSmsInsertStatus4.indexInIcc = sb4.toString();
                            }
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        if (asyncResult14.exception != null) {
                            CommandException commandException = asyncResult14.exception;
                            MtkIccSmsInterfaceManager.this.log("Cannot update SMS " + commandException.getCommandError());
                            if (commandException.getCommandError() == CommandException.Error.SIM_FULL) {
                                ((MtkSmsDispatchersController) MtkIccSmsInterfaceManager.this.mDispatchersController).handleIccFull();
                                return;
                            }
                            return;
                        }
                        return;
                    case 117:
                        AsyncResult asyncResult15 = (AsyncResult) message.obj;
                        synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                            if (asyncResult15.exception == null) {
                                try {
                                    try {
                                        int i4 = ((int[]) asyncResult15.result)[0];
                                        StringBuilder sb5 = new StringBuilder();
                                        MtkSimSmsInsertStatus mtkSimSmsInsertStatus5 = MtkIccSmsInterfaceManager.this.smsInsertRet2;
                                        sb5.append(mtkSimSmsInsertStatus5.indexInIcc);
                                        sb5.append(i4);
                                        sb5.append(MtkIccSmsInterfaceManager.INDEXT_SPLITOR);
                                        mtkSimSmsInsertStatus5.indexInIcc = sb5.toString();
                                        MtkIccSmsInterfaceManager.this.log("[insertRaw save one pdu in index " + i4);
                                    } catch (ClassCastException e8) {
                                        e8.printStackTrace();
                                    }
                                } catch (Exception e9) {
                                    e9.printStackTrace();
                                }
                            } else {
                                MtkIccSmsInterfaceManager.this.log("[insertRaw fail to insert raw into ICC");
                                StringBuilder sb6 = new StringBuilder();
                                MtkSimSmsInsertStatus mtkSimSmsInsertStatus6 = MtkIccSmsInterfaceManager.this.smsInsertRet2;
                                sb6.append(mtkSimSmsInsertStatus6.indexInIcc);
                                sb6.append("-1,");
                                mtkSimSmsInsertStatus6.indexInIcc = sb6.toString();
                            }
                            MtkIccSmsInterfaceManager.this.mSmsWipedRsp = true;
                            MtkIccSmsInterfaceManager.this.mMtkLock.notifyAll();
                            break;
                        }
                        if (asyncResult15.exception != null) {
                            CommandException commandException2 = asyncResult15.exception;
                            MtkIccSmsInterfaceManager.this.log("Cannot update SMS " + commandException2.getCommandError());
                            if (commandException2.getCommandError() == CommandException.Error.SIM_FULL) {
                                ((MtkSmsDispatchersController) MtkIccSmsInterfaceManager.this.mDispatchersController).handleIccFull();
                                return;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mSmsWipeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MtkIccSmsInterfaceManager.this.log("Receive intent");
                if (intent.getAction().equals("com.mediatek.dm.LAWMO_WIPE")) {
                    MtkIccSmsInterfaceManager.this.log("Receive wipe intent");
                    new Thread() {
                        @Override
                        public void run() {
                            synchronized (MtkIccSmsInterfaceManager.this.mMtkLock) {
                                MtkIccSmsInterfaceManager.this.log("Delete message on sub " + MtkIccSmsInterfaceManager.this.mPhone.getSubId());
                                MtkIccSmsInterfaceManager.this.mSmsWipedRsp = false;
                                MtkIccSmsInterfaceManager.this.mPhone.mCi.deleteSmsOnSim(-1, MtkIccSmsInterfaceManager.this.mMtkHandler.obtainMessage(117));
                                while (!MtkIccSmsInterfaceManager.this.mSmsWipedRsp) {
                                    try {
                                        MtkIccSmsInterfaceManager.this.mMtkLock.wait();
                                    } catch (InterruptedException e) {
                                        MtkIccSmsInterfaceManager.this.log("insertRaw interrupted while trying to update by index");
                                    }
                                }
                            }
                        }
                    }.start();
                }
            }
        };
        this.mCurrentCellBroadcastActivation = -1;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mediatek.dm.LAWMO_WIPE");
        this.mContext.registerReceiver(this.mSmsWipeReceiver, intentFilter);
    }

    protected void sendTextInternal(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, int i, boolean z2, int i2) {
        log("sendTextMessage, text=" + str4 + ", destinationAddress=" + str2);
        if (!isValidParameters(str2, str4, pendingIntent)) {
            return;
        }
        if (!checkTddDataOnlyPermission(pendingIntent)) {
            log("TDD data only and w/o permission!");
        } else {
            ActivityThread.currentApplication().getApplicationContext();
            super.sendTextInternal(str, str2, str3, str4, pendingIntent, pendingIntent2, z, i, z2, i2);
        }
    }

    protected void sendDataInternal(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        log("sendDataMessage, destinationAddress=" + str);
        if (!isValidParameters(str, "send_data", pendingIntent)) {
            return;
        }
        if (!checkTddDataOnlyPermission(pendingIntent)) {
            log("TDD data only and w/o permission!");
        } else {
            ActivityThread.currentApplication().getApplicationContext();
            super.sendDataInternal(str, str2, i, bArr, pendingIntent, pendingIntent2);
        }
    }

    public void sendStoredText(String str, Uri uri, String str2, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        ActivityThread.currentApplication().getApplicationContext();
        if (!checkTddDataOnlyPermission(pendingIntent)) {
            log("TDD data only and w/o permission!");
        } else {
            super.sendStoredText(str, uri, str2, pendingIntent, pendingIntent2);
        }
    }

    public void sendStoredMultipartText(String str, Uri uri, String str2, List<PendingIntent> list, List<PendingIntent> list2) {
        ActivityThread.currentApplication().getApplicationContext();
        if (!checkTddDataOnlyPermission(list)) {
            log("TDD data only and w/o permission!");
        } else {
            super.sendStoredMultipartText(str, uri, str2, list, list2);
        }
    }

    public List<SmsRawData> getAllMessagesFromIccEf(String str) {
        log("getAllMessagesFromEF " + str);
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", "Reading messages from Icc");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), str) != 0) {
            return new ArrayList();
        }
        synchronized (this.mMtkLoadLock) {
            IccFileHandler iccFileHandler = this.mPhone.getIccFileHandler();
            if (iccFileHandler == null) {
                Rlog.e(LOG_TAG, "Cannot load Sms records. No icc card?");
                this.mSms = null;
                return this.mSms;
            }
            iccFileHandler.loadEFLinearFixedAll(28476, this.mMtkHandler.obtainMessage(115));
            try {
                this.mMtkLoadLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to load from the Icc");
            }
            return this.mSms;
        }
    }

    protected ArrayList<SmsRawData> buildValidRawData(ArrayList<byte[]> arrayList) {
        int size = arrayList.size();
        ArrayList<SmsRawData> arrayList2 = new ArrayList<>(size);
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            if (arrayList.get(i2)[0] == 0) {
                arrayList2.add(null);
            } else {
                i++;
                arrayList2.add(new SmsRawData(arrayList.get(i2)));
            }
        }
        log("validSmsCount = " + i);
        return arrayList2;
    }

    protected byte[] makeSmsRecordData(int i, byte[] bArr) {
        byte[] bArr2;
        if (1 == this.mPhone.getPhoneType()) {
            bArr2 = new byte[176];
        } else {
            bArr2 = new byte[255];
        }
        bArr2[0] = (byte) (i & 7);
        log("ISIM-makeSmsRecordData: pdu size = " + bArr.length);
        if (bArr.length == 176) {
            log("ISIM-makeSmsRecordData: sim pdu");
            try {
                System.arraycopy(bArr, 1, bArr2, 1, bArr.length - 1);
            } catch (ArrayIndexOutOfBoundsException e) {
                log("ISIM-makeSmsRecordData: out of bounds, sim pdu");
            }
        } else {
            log("ISIM-makeSmsRecordData: normal pdu");
            try {
                System.arraycopy(bArr, 0, bArr2, 1, bArr.length);
            } catch (ArrayIndexOutOfBoundsException e2) {
                log("ISIM-makeSmsRecordData: out of bounds, normal pdu");
            }
        }
        for (int length = bArr.length + 1; length < bArr2.length; length++) {
            bArr2[length] = -1;
        }
        return bArr2;
    }

    protected void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    public void sendDataWithOriginalPort(String str, String str2, String str3, int i, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        String str4;
        String str5;
        int i3;
        int i4;
        PendingIntent pendingIntent3;
        PendingIntent pendingIntent4;
        Rlog.d(LOG_TAG, "Enter IccSmsInterfaceManager.sendDataWithOriginalPort");
        if (z) {
            this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
            if (Rlog.isLoggable("SMS", 2)) {
                StringBuilder sb = new StringBuilder();
                sb.append("sendData: destAddr=");
                str4 = str2;
                sb.append(str4);
                sb.append(" scAddr=");
                str5 = str3;
                sb.append(str5);
                sb.append(" destPort=");
                i3 = i;
                sb.append(i3);
                sb.append(" originalPort=");
                i4 = i2;
                sb.append(i4);
                sb.append(" data='");
                sb.append(HexDump.toHexString(bArr));
                sb.append("' sentIntent=");
                pendingIntent3 = pendingIntent;
                sb.append(pendingIntent3);
                sb.append(" deliveryIntent=");
                pendingIntent4 = pendingIntent2;
                sb.append(pendingIntent4);
                log(sb.toString());
            } else {
                str4 = str2;
                str5 = str3;
                i3 = i;
                i4 = i2;
                pendingIntent3 = pendingIntent;
                pendingIntent4 = pendingIntent2;
            }
            if (this.mAppOps.noteOp(20, Binder.getCallingUid(), str) != 0) {
                return;
            }
        } else {
            str4 = str2;
            str5 = str3;
            i3 = i;
            i4 = i2;
            pendingIntent3 = pendingIntent;
            pendingIntent4 = pendingIntent2;
        }
        ((MtkSmsDispatchersController) this.mDispatchersController).sendData(str4, str5, i3, i4, bArr, pendingIntent3, pendingIntent4);
    }

    public void sendMultipartText(String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        log("sendMultipartTextMessage, destinationAddress=" + str2);
        if (!isValidParameters(str2, list, list2)) {
            return;
        }
        if (!checkTddDataOnlyPermission(list2)) {
            log("TDD data only and w/o permission!");
        } else {
            ActivityThread.currentApplication().getApplicationContext();
            super.sendMultipartText(str, str2, str3, list, list2, list3, z);
        }
    }

    public void sendMultipartData(String str, String str2, String str3, int i, List<SmsRawData> list, List<PendingIntent> list2, List<PendingIntent> list3) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (Rlog.isLoggable("SMS", 2)) {
            Iterator<SmsRawData> it = list.iterator();
            while (it.hasNext()) {
                log("sendMultipartData: destAddr=" + str2 + " scAddr=" + str3 + " destPort=" + i + " data='" + HexDump.toHexString(it.next().getBytes()));
            }
        }
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), str) != 0) {
            return;
        }
        ((MtkSmsDispatchersController) this.mDispatchersController).sendMultipartData(str2, str3, i, (ArrayList) list, (ArrayList) list2, (ArrayList) list3);
    }

    public void setSmsMemoryStatus(boolean z) {
        log("setSmsMemoryStatus: set storage status -> " + z);
        ((MtkSmsDispatchersController) this.mDispatchersController).setSmsMemoryStatus(z);
    }

    public boolean isSmsReady() {
        boolean zIsSmsReady = ((MtkSmsDispatchersController) this.mDispatchersController).isSmsReady();
        log("isSmsReady: " + zIsSmsReady);
        return zIsSmsReady;
    }

    public void sendTextWithEncodingType(String str, String str2, String str3, String str4, int i, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        sendTextWithOptions(str, str2, str3, str4, pendingIntent, pendingIntent2, z, -1, false, -1, i);
    }

    public void sendMultipartTextWithEncodingType(String str, String str2, String str3, List<String> list, int i, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) {
        sendMultipartTextWithOptions(str, str2, str3, list, list2, list3, z, -1, false, -1, i);
    }

    public void sendTextWithExtraParams(String str, String str2, String str3, String str4, Bundle bundle, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), str) != 0) {
            return;
        }
        sendTextInternal(str, str2, str3, str4, pendingIntent, pendingIntent2, z, -1, false, bundle.getInt("validity_period", -1));
    }

    public void sendMultipartTextWithExtraParams(String str, String str2, String str3, List<String> list, Bundle bundle, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) {
        PendingIntent pendingIntent;
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), str) != 0) {
            return;
        }
        if (!checkTddDataOnlyPermission(list2)) {
            log("TDD data only and w/o permission!");
            return;
        }
        String strFilterDestAddress = filterDestAddress(str2);
        int i = bundle.getInt("validity_period", -1);
        if (list.size() > 1 && list.size() < 10 && !SmsMessage.hasEmsSupport()) {
            for (int i2 = 0; i2 < list.size(); i2++) {
                String str4 = list.get(i2);
                String strConcat = SmsMessage.shouldAppendPageNumberAsPrefix() ? String.valueOf(i2 + 1) + '/' + list.size() + ' ' + str4 : str4.concat(' ' + String.valueOf(i2 + 1) + '/' + list.size());
                PendingIntent pendingIntent2 = null;
                if (list2 == null || list2.size() <= i2) {
                    pendingIntent = null;
                } else {
                    pendingIntent = list2.get(i2);
                }
                if (list3 != null && list3.size() > i2) {
                    pendingIntent2 = list3.get(i2);
                }
                sendTextWithOptions(str, strFilterDestAddress, str3, strConcat, pendingIntent, pendingIntent2, z, -1, false, i);
            }
            return;
        }
        sendMultipartTextWithOptions(str, strFilterDestAddress, str3, list, list2, list3, z, -1, false, i);
    }

    public SmsRawData getMessageFromIccEf(String str, int i) {
        log("getMessageFromIccEf");
        this.mPhone.getContext().enforceCallingPermission("android.permission.RECEIVE_SMS", "Reading messages from SIM");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), str) != 0) {
            return null;
        }
        this.mSmsRawData = null;
        synchronized (this.mMtkLock) {
            if (this.mPhone.getIccFileHandler() != null) {
                this.mPhone.getIccFileHandler().loadEFLinearFixed(28476, i, this.mMtkHandler.obtainMessage(105));
                try {
                    this.mMtkLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to load from the SIM");
                }
            }
        }
        return this.mSmsRawData;
    }

    public List<SmsRawData> getAllMessagesFromIccEfByMode(String str, int i) {
        log("getAllMessagesFromIccEfByMode, mode=" + i);
        if (i < 1 || i > 2) {
            log("getAllMessagesFromIccEfByMode wrong mode=" + i);
            return null;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", "Reading messages from Icc");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), str) != 0) {
            return new ArrayList();
        }
        synchronized (this.mMtkLoadLock) {
            MtkSIMFileHandler iccFileHandler = this.mPhone.getIccFileHandler();
            if (iccFileHandler == null) {
                Rlog.e(LOG_TAG, "Cannot load Sms records. No icc card?");
                if (this.mSms == null) {
                    return null;
                }
                this.mSms.clear();
                return this.mSms;
            }
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(115);
            if (1 == this.mPhone.getPhoneType()) {
                Rlog.e(LOG_TAG, "getAllMessagesFromIccEfByMode. In the case of GSM phone");
                iccFileHandler.loadEFLinearFixedAll(28476, i, messageObtainMessage);
                try {
                    this.mMtkLoadLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to load from the SIM");
                }
            }
            return this.mSms;
        }
    }

    public MtkSmsParameters getSmsParameters(String str) {
        MtkSmsParameters mtkSmsParameters;
        log("getSmsParameters");
        enforceReceiveAndSend("Get SMS parametner on SIM");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), str) != 0) {
            return null;
        }
        synchronized (this.mMtkLock) {
            this.mPhone.mCi.getSmsParameters(this.mMtkHandler.obtainMessage(EVENT_GET_SMS_PARAMS));
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get sms params");
            }
            mtkSmsParameters = this.mSmsParams;
        }
        return mtkSmsParameters;
    }

    public boolean setSmsParameters(String str, MtkSmsParameters mtkSmsParameters) {
        log("setSmsParameters");
        enforceReceiveAndSend("Set SMS parametner on SIM");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), str) != 0) {
            return false;
        }
        this.mSmsParamsSuccess = false;
        synchronized (this.mMtkLock) {
            this.mPhone.mCi.setSmsParameters(mtkSmsParameters, this.mMtkHandler.obtainMessage(104));
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get sms params");
            }
        }
        return this.mSmsParamsSuccess;
    }

    public int copyTextMessageToIccCard(String str, String str2, String str3, List<String> list, int i, long j) {
        log("copyTextMessageToIccCard, sc address: " + str2 + " address: " + str3 + " message count: " + list.size() + " status: " + i + " timestamp: " + j);
        enforceReceiveAndSend("Copying message to USIM/SIM");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), str) != 0) {
            return 1;
        }
        MtkIccSmsStorageStatus smsSimMemoryStatus = getSmsSimMemoryStatus(str);
        if (smsSimMemoryStatus == null) {
            log("Fail to get SIM memory status");
            return 1;
        }
        if (smsSimMemoryStatus.getUnused() < list.size()) {
            log("SIM memory is not enough");
            return 7;
        }
        return ((MtkSmsDispatchersController) this.mDispatchersController).copyTextMessageToIccCard(str2, str3, list, i, j);
    }

    public MtkSimSmsInsertStatus insertTextMessageToIccCard(String str, String str2, String str3, List<String> list, int i, long j) {
        String str4;
        boolean z;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        byte[] submitPduHeaderWithLang;
        int i7;
        GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr;
        int i8;
        int i9;
        log("insertTextMessageToIccCard");
        enforceReceiveAndSend("insertText insert message into SIM");
        int i10 = 1;
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), str) != 0) {
            this.smsInsertRet.insertStatus = 1;
            return this.smsInsertRet;
        }
        int size = list.size();
        StringBuilder sb = new StringBuilder();
        sb.append("insertText scAddr=");
        sb.append(str2);
        sb.append(", addr=");
        String str5 = str3;
        sb.append(str5);
        sb.append(", msgCount=");
        sb.append(size);
        sb.append(", status=");
        sb.append(i);
        sb.append(", timestamp=");
        sb.append(j);
        log(sb.toString());
        this.smsInsertRet.indexInIcc = "";
        MtkIccSmsStorageStatus smsSimMemoryStatus = getSmsSimMemoryStatus(str);
        if (smsSimMemoryStatus != null) {
            int unused = smsSimMemoryStatus.getUnused();
            if (unused < size) {
                log("insertText SIM mem is not enough [" + unused + "/" + size + "]");
                this.smsInsertRet.insertStatus = 7;
                return this.smsInsertRet;
            }
            if (checkPhoneNumberInternal(str2)) {
                str4 = str2;
            } else {
                log("insertText invalid sc address");
                str4 = null;
            }
            if (!checkPhoneNumberInternal(str3)) {
                log("insertText invalid address");
                this.smsInsertRet.insertStatus = 8;
                return this.smsInsertRet;
            }
            if (i == 1 || i == 3) {
                log("insertText to encode delivery pdu");
                z = true;
            } else if (i == 5 || i == 7) {
                log("insertText to encode submit pdu");
                z = false;
            } else {
                log("insertText invalid status " + i);
                this.smsInsertRet.insertStatus = 1;
                return this.smsInsertRet;
            }
            log("insertText params check pass");
            if (2 == this.mPhone.getPhoneType()) {
                return writeTextMessageToRuim(str5, list, i, j);
            }
            GsmAlphabet.TextEncodingDetails[] textEncodingDetailsArr2 = new GsmAlphabet.TextEncodingDetails[size];
            int i11 = 0;
            for (int i12 = 0; i12 < size; i12++) {
                textEncodingDetailsArr2[i12] = com.android.internal.telephony.gsm.SmsMessage.calculateLength(list.get(i12), false);
                if (i11 != textEncodingDetailsArr2[i12].codeUnitSize && (i11 == 0 || i11 == 1)) {
                    i11 = textEncodingDetailsArr2[i12].codeUnitSize;
                }
            }
            log("insertText create & insert pdu start...");
            int i13 = 0;
            while (i13 < size) {
                if (!this.mInsertMessageSuccess && i13 > 0) {
                    log("insertText last message insert fail");
                    this.smsInsertRet.insertStatus = i10;
                    return this.smsInsertRet;
                }
                int i14 = textEncodingDetailsArr2[i13].shiftLangId;
                int i15 = -1;
                if (i11 != i10) {
                    i2 = -1;
                    i3 = -1;
                    i4 = i11;
                } else {
                    if (textEncodingDetailsArr2[i13].languageTable > 0 && textEncodingDetailsArr2[i13].languageShiftTable > 0) {
                        i15 = textEncodingDetailsArr2[i13].languageTable;
                        i9 = textEncodingDetailsArr2[i13].languageShiftTable;
                        i8 = 13;
                    } else if (textEncodingDetailsArr2[i13].languageShiftTable > 0) {
                        i9 = textEncodingDetailsArr2[i13].languageShiftTable;
                        i8 = 12;
                    } else {
                        if (textEncodingDetailsArr2[i13].languageTable > 0) {
                            i8 = 11;
                            i3 = -1;
                            i2 = textEncodingDetailsArr2[i13].languageTable;
                            i4 = i8;
                        }
                        i2 = -1;
                        i3 = -1;
                        i4 = i11;
                    }
                    i2 = i15;
                    i3 = i9;
                    i4 = i8;
                }
                if (size > i10) {
                    log("insertText create pdu header for concat-message");
                    i5 = i14;
                    i6 = i13;
                    submitPduHeaderWithLang = MtkSmsHeader.getSubmitPduHeaderWithLang(-1, getNextConcatRef() & 255, i13 + 1, size, i2, i3);
                } else {
                    i5 = i14;
                    i6 = i13;
                    submitPduHeaderWithLang = null;
                }
                if (z) {
                    i7 = i11;
                    textEncodingDetailsArr = textEncodingDetailsArr2;
                    MtkSmsMessage.DeliverPdu deliverPduWithLang = MtkSmsMessage.getDeliverPduWithLang(str4, str5, list.get(i6), submitPduHeaderWithLang, j, i4, i5);
                    if (deliverPduWithLang != null) {
                        synchronized (this.mSimInsertLock) {
                            this.mPhone.mCi.writeSmsToSim(i, IccUtils.bytesToHexString(deliverPduWithLang.encodedScAddress), IccUtils.bytesToHexString(deliverPduWithLang.encodedMessage), this.mMtkHandler.obtainMessage(102));
                            try {
                                log("insertText wait until the pdu be wrote into the SIM");
                                this.mSimInsertLock.wait();
                            } catch (InterruptedException e) {
                                log("insertText fail to insert pdu");
                                this.smsInsertRet.insertStatus = 1;
                                return this.smsInsertRet;
                            }
                        }
                    } else {
                        log("insertText fail to create deliver pdu");
                        this.smsInsertRet.insertStatus = 1;
                        return this.smsInsertRet;
                    }
                } else {
                    i7 = i11;
                    textEncodingDetailsArr = textEncodingDetailsArr2;
                    SmsMessage.SubmitPdu submitPduWithLang = MtkSmsMessage.getSubmitPduWithLang(str4, str3, list.get(i6), false, submitPduHeaderWithLang, i4, i5, -1);
                    if (submitPduWithLang != null) {
                        synchronized (this.mSimInsertLock) {
                            this.mPhone.mCi.writeSmsToSim(i, IccUtils.bytesToHexString(submitPduWithLang.encodedScAddress), IccUtils.bytesToHexString(submitPduWithLang.encodedMessage), this.mMtkHandler.obtainMessage(102));
                            try {
                                log("insertText wait until the pdu be wrote into the SIM");
                                this.mSimInsertLock.wait();
                            } catch (InterruptedException e2) {
                                log("insertText fail to insert pdu");
                                this.smsInsertRet.insertStatus = 1;
                                return this.smsInsertRet;
                            }
                        }
                    } else {
                        log("insertText fail to create submit pdu");
                        this.smsInsertRet.insertStatus = 1;
                        return this.smsInsertRet;
                    }
                }
                i13 = i6 + 1;
                str5 = str3;
                textEncodingDetailsArr2 = textEncodingDetailsArr;
                i11 = i7;
                i10 = 1;
            }
            int i16 = i10;
            log("insertText create & insert pdu end");
            if (this.mInsertMessageSuccess == i16) {
                log("insertText all messages inserted");
                this.smsInsertRet.insertStatus = i16;
                return this.smsInsertRet;
            }
            log("insertText pdu insert fail");
            this.smsInsertRet.insertStatus = i16;
            return this.smsInsertRet;
        }
        log("insertText fail to get SIM mem status");
        this.smsInsertRet.insertStatus = 1;
        return this.smsInsertRet;
    }

    public MtkSimSmsInsertStatus insertRawMessageToIccCard(String str, int i, byte[] bArr, byte[] bArr2) {
        log("insertRawMessageToIccCard");
        enforceReceiveAndSend("insertRaw insert message into SIM");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), str) != 0) {
            this.smsInsertRet2.insertStatus = 1;
            return this.smsInsertRet2;
        }
        synchronized (this.mMtkLock) {
            this.mMtkSuccess = false;
            this.smsInsertRet2.insertStatus = 1;
            this.smsInsertRet2.indexInIcc = "";
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(116);
            if (2 != this.mPhone.getPhoneType()) {
                this.mPhone.mCi.writeSmsToSim(i, IccUtils.bytesToHexString(bArr2), IccUtils.bytesToHexString(bArr), messageObtainMessage);
            } else {
                this.mPhone.mCi.writeSmsToRuim(i, IccUtils.bytesToHexString(bArr), messageObtainMessage);
            }
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("insertRaw interrupted while trying to update by index");
            }
        }
        if (this.mMtkSuccess) {
            log("insertRaw message inserted");
            this.smsInsertRet2.insertStatus = 0;
            return this.smsInsertRet2;
        }
        log("insertRaw pdu insert fail");
        this.smsInsertRet2.insertStatus = 1;
        return this.smsInsertRet2;
    }

    public MtkIccSmsStorageStatus getSmsSimMemoryStatus(String str) {
        log("getSmsSimMemoryStatus");
        enforceReceiveAndSend("Get SMS SIM Card Memory Status from RUIM");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), str) != 0) {
            return null;
        }
        synchronized (this.mMtkLock) {
            this.mMtkSuccess = false;
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(101);
            MtkRIL mtkRIL = this.mPhone.mCi;
            if (this.mPhone.getPhoneType() == 2) {
                mtkRIL.getSmsRuimMemoryStatus(messageObtainMessage);
            } else {
                mtkRIL.getSmsSimMemoryStatus(messageObtainMessage);
            }
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get SMS SIM Card Memory Status from SIM");
            }
        }
        if (this.mMtkSuccess) {
            return this.mSimMemStatus;
        }
        return null;
    }

    private static int getNextConcatRef() {
        int i = sConcatenatedRef;
        sConcatenatedRef = i + 1;
        return i;
    }

    private static boolean checkPhoneNumberCharacter(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '+' || c == '#' || c == 'N' || c == ' ' || c == '-';
    }

    private static boolean checkPhoneNumberInternal(String str) {
        if (str == null) {
            return true;
        }
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!checkPhoneNumberCharacter(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected MtkSimSmsInsertStatus writeTextMessageToRuim(String str, List<String> list, int i, long j) {
        MtkSimSmsInsertStatus mtkSimSmsInsertStatus = new MtkSimSmsInsertStatus(0, "");
        this.mMtkSuccess = true;
        for (int i2 = 0; i2 < list.size(); i2++) {
            if (!this.mMtkSuccess) {
                log("[copyText Exception happened when copy message");
                mtkSimSmsInsertStatus.insertStatus = 1;
                return mtkSimSmsInsertStatus;
            }
            SmsMessage.SubmitPdu submitPduCreateEfPdu = com.mediatek.internal.telephony.cdma.MtkSmsMessage.createEfPdu(str, list.get(i2), j);
            if (submitPduCreateEfPdu != null) {
                synchronized (this.mSimInsertLock) {
                    this.mPhone.mCi.writeSmsToRuim(i, IccUtils.bytesToHexString(submitPduCreateEfPdu.encodedMessage), this.mMtkHandler.obtainMessage(102));
                    this.mInserted = false;
                    try {
                        this.mSimInsertLock.wait();
                    } catch (InterruptedException e) {
                        log("InterruptedException " + e);
                        mtkSimSmsInsertStatus.insertStatus = 1;
                        return mtkSimSmsInsertStatus;
                    }
                }
            } else {
                log("writeTextMessageToRuim: pdu == null");
                mtkSimSmsInsertStatus.insertStatus = 1;
                return mtkSimSmsInsertStatus;
            }
        }
        log("writeTextMessageToRuim: done");
        mtkSimSmsInsertStatus.insertStatus = 0;
        return mtkSimSmsInsertStatus;
    }

    private String filterDestAddress(String str) {
        String strFilterDestAddr = SmsNumberUtils.filterDestAddr(this.mPhone, str);
        return strFilterDestAddr != null ? strFilterDestAddr : str;
    }

    private static boolean isValidParameters(String str, String str2, PendingIntent pendingIntent) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList.add(pendingIntent);
        arrayList2.add(str2);
        return isValidParameters(str, arrayList2, arrayList);
    }

    private static boolean isValidParameters(String str, List<String> list, List<PendingIntent> list2) {
        if (list == null || list.size() == 0) {
            return true;
        }
        if (!isValidSmsDestinationAddress(str)) {
            for (int i = 0; i < list2.size(); i++) {
                PendingIntent pendingIntent = list2.get(i);
                if (pendingIntent != null) {
                    try {
                        pendingIntent.send(1);
                    } catch (PendingIntent.CanceledException e) {
                    }
                }
            }
            Rlog.d("IccSmsInterfaceManagerEx", "Invalid destinationAddress: " + str);
            return false;
        }
        if (TextUtils.isEmpty(str)) {
            Rlog.e("IccSmsInterfaceManagerEx", "Invalid destinationAddress");
            return false;
        }
        if (list != null && list.size() >= 1) {
            return true;
        }
        Rlog.e("IccSmsInterfaceManagerEx", "Invalid message body");
        return false;
    }

    private static boolean isValidSmsDestinationAddress(String str) {
        if (PhoneNumberUtils.extractNetworkPortion(str) == null) {
            return true;
        }
        return !r1.isEmpty();
    }

    public boolean activateCellBroadcastSms(boolean z) {
        log("activateCellBroadcastSms activate : " + z);
        return setCellBroadcastActivation(z);
    }

    public boolean queryCellBroadcastSmsActivation() {
        log("queryCellBroadcastSmsActivation");
        synchronized (this.mMtkLock) {
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(EVENT_GET_BROADCAST_ACTIVATION_DONE);
            this.mMtkSuccess = false;
            this.mPhone.mCi.getGsmBroadcastActivation(messageObtainMessage);
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get CB activation");
            }
        }
        return this.mMtkSuccess;
    }

    public String getCellBroadcastRanges() {
        log("getCellBroadcastChannels");
        synchronized (this.mMtkLock) {
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(EVENT_GET_BROADCAST_CONFIG_CHANNEL_DONE);
            this.mSmsCbChannelConfig = "";
            this.mPhone.mCi.getGsmBroadcastConfig(messageObtainMessage);
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get CB config");
            }
        }
        return this.mSmsCbChannelConfig;
    }

    public boolean setCellBroadcastLangs(String str) {
        log("setCellBroadcastLangs");
        synchronized (this.mMtkLock) {
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(109);
            this.mMtkSuccess = false;
            this.mPhone.mCi.setGsmBroadcastLangs(str, messageObtainMessage);
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get CB config");
            }
        }
        return this.mMtkSuccess;
    }

    public String getCellBroadcastLangs() {
        String str;
        log("getCellBroadcastLangs");
        synchronized (this.mMtkLock) {
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(110);
            this.mSmsCbLanguageConfig = "";
            this.mPhone.mCi.getGsmBroadcastLangs(messageObtainMessage);
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get CB config");
            }
            str = this.mSmsCbLanguageConfig;
        }
        return str;
    }

    public boolean removeCellBroadcastMsg(int i, int i2) {
        log("removeCellBroadcastMsg(" + i + " , " + i2 + ")");
        synchronized (this.mMtkLock) {
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(EVENT_REMOVE_BROADCAST_MSG_DONE);
            this.mMtkSuccess = false;
            this.mPhone.mCi.removeCellBroadcastMsg(i, i2, messageObtainMessage);
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to remove CB msg");
            }
        }
        return this.mMtkSuccess;
    }

    public boolean setEtwsConfig(int i) {
        log("Calling setEtwsConfig(" + i + ')');
        synchronized (this.mMtkLock) {
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(111);
            this.mMtkSuccess = false;
            this.mPhone.mCi.setEtws(i, messageObtainMessage);
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set ETWS config");
            }
        }
        return this.mMtkSuccess;
    }

    public String getScAddress() {
        log("getScAddress");
        synchronized (this.mMtkLock) {
            this.mPhone.getSmscAddress(this.mMtkHandler.obtainMessage(112));
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get SMSC address");
            }
        }
        log("getScAddress: exit with " + this.mSmscAddress);
        return this.mSmscAddress;
    }

    public Bundle getScAddressWithErrorCode() {
        log("getScAddressWithErrorCode");
        synchronized (this.mMtkLock) {
            this.mPhone.getSmscAddress(this.mMtkHandler.obtainMessage(113));
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get SMSC address and error code");
            }
        }
        log("getScAddressWithErrorCode error code done");
        return this.mSmscAddressBundle;
    }

    public boolean setScAddress(String str) {
        log("setScAddressUsingSubId(" + str + ')');
        synchronized (this.mMtkLock) {
            Message messageObtainMessage = this.mMtkHandler.obtainMessage(114);
            this.mMtkSuccess = false;
            this.mPhone.setSmscAddress(str, messageObtainMessage);
            try {
                this.mMtkLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set SMSC address");
            }
        }
        log("setScAddressUsingSubId result " + this.mMtkSuccess);
        return this.mMtkSuccess;
    }

    private boolean checkTddDataOnlyPermission(PendingIntent pendingIntent) {
        if (new MtkLteDataOnlyController(this.mContext).checkPermission(this.mPhone.getSubId())) {
            return true;
        }
        log("checkTddDataOnlyPermission, w/o permission, sentIntent = " + pendingIntent);
        if (pendingIntent == null) {
            log("checkTddDataOnlyPermission, can not notify APP");
            return false;
        }
        try {
            pendingIntent.send(1);
            return false;
        } catch (PendingIntent.CanceledException e) {
            loge("checkTddDataOnlyPermission, CanceledException happened when send sms fail with sentIntent");
            return false;
        }
    }

    private boolean checkTddDataOnlyPermission(List<PendingIntent> list) {
        if (new MtkLteDataOnlyController(this.mContext).checkPermission(this.mPhone.getSubId())) {
            return true;
        }
        log("checkTddDataOnlyPermission, w/o permission, sentIntents = " + list);
        if (list == null) {
            log("checkTddDataOnlyPermission, can not notify APP");
        } else {
            try {
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    PendingIntent pendingIntent = list.get(i);
                    if (pendingIntent == null) {
                        log("checkTddDataOnlyPermission, can not notify APP for i = " + i);
                    } else {
                        pendingIntent.send(1);
                    }
                }
            } catch (PendingIntent.CanceledException e) {
                loge("checkTddDataOnlyPermission, CanceledException happened when send sms fail with sentIntent");
            }
        }
        return false;
    }

    protected boolean setCellBroadcastActivation(boolean z) {
        log("Calling proprietary setCellBroadcastActivation(" + z + ')');
        if (this.mCurrentCellBroadcastActivation != z) {
            super.setCellBroadcastActivation(z);
        } else {
            this.mSuccess = true;
        }
        if (this.mSuccess && this.mCurrentCellBroadcastActivation != z) {
            this.mCurrentCellBroadcastActivation = z ? 1 : 0;
            log("mCurrentCellBroadcastActivation change to " + this.mCurrentCellBroadcastActivation);
        }
        return this.mSuccess;
    }

    public void sendTextWithOptions(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, int i, boolean z2, int i2, int i3) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), str) != 0) {
            return;
        }
        if (checkTddDataOnlyPermission(pendingIntent)) {
            ((MtkSmsDispatchersController) this.mDispatchersController).sendTextWithEncodingType(str2, str3, str4, i3, pendingIntent, pendingIntent2, null, str, z, i, z2, i2);
        } else {
            log("TDD data only and w/o permission!");
        }
    }

    public void sendMultipartTextWithOptions(String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z, int i, boolean z2, int i2, int i3) {
        PendingIntent pendingIntent;
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), str) != 0) {
            return;
        }
        if (!checkTddDataOnlyPermission(list2)) {
            log("TDD data only and w/o permission!");
            return;
        }
        String strFilterDestAddress = filterDestAddress(str2);
        if (list.size() > 1 && list.size() < 10 && !android.telephony.SmsMessage.hasEmsSupport()) {
            for (int i4 = 0; i4 < list.size(); i4++) {
                String str4 = list.get(i4);
                String strConcat = android.telephony.SmsMessage.shouldAppendPageNumberAsPrefix() ? String.valueOf(i4 + 1) + '/' + list.size() + ' ' + str4 : str4.concat(' ' + String.valueOf(i4 + 1) + '/' + list.size());
                PendingIntent pendingIntent2 = null;
                if (list2 == null || list2.size() <= i4) {
                    pendingIntent = null;
                } else {
                    pendingIntent = list2.get(i4);
                }
                if (list3 != null && list3.size() > i4) {
                    pendingIntent2 = list3.get(i4);
                }
                ((MtkSmsDispatchersController) this.mDispatchersController).sendTextWithEncodingType(strFilterDestAddress, str3, strConcat, i3, pendingIntent, pendingIntent2, null, str, z, i, z2, i2);
            }
            return;
        }
        ((MtkSmsDispatchersController) this.mDispatchersController).sendMultipartTextWithEncodingType(strFilterDestAddress, str3, (ArrayList) list, i3, (ArrayList) list2, (ArrayList) list3, null, str, z, i, z2, i2);
    }
}
