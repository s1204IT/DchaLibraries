package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.telephony.Rlog;
import android.telephony.SimSmsInsertStatus;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsParameters;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.cdma.SmsMessage;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.HexDump;
import com.mediatek.internal.telephony.IccSmsStorageStatus;
import com.mediatek.internal.telephony.SmsCbConfigInfo;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IccSmsInterfaceManager {
    static final boolean DBG = true;
    private static final int EVENT_GET_BROADCAST_ACTIVATION_DONE = 108;
    private static final int EVENT_GET_BROADCAST_CONFIG_DONE = 107;
    private static final int EVENT_GET_SMS_PARAMS = 104;
    private static final int EVENT_GET_SMS_SIM_MEM_STATUS_DONE = 102;
    private static final int EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE = 103;
    private static final int EVENT_LOAD_DONE = 1;
    private static final int EVENT_LOAD_ONE_RECORD_DONE = 106;
    private static final int EVENT_REMOVE_BROADCAST_MSG_DONE = 109;
    protected static final int EVENT_SET_BROADCAST_ACTIVATION_DONE = 3;
    protected static final int EVENT_SET_BROADCAST_CONFIG_DONE = 4;
    private static final int EVENT_SET_ETWS_CONFIG_DONE = 101;
    private static final int EVENT_SET_SMS_PARAMS = 105;
    private static final int EVENT_SIM_SMS_DELETE_DONE = 100;
    private static final int EVENT_UPDATE_DONE = 2;
    private static final String INDEXT_SPLITOR = ",";
    static final String LOG_TAG = "IccSmsInterfaceManager";
    private static final int SMS_CB_CODE_SCHEME_MAX = 255;
    private static final int SMS_CB_CODE_SCHEME_MIN = 0;
    private static int sConcatenatedRef = 456;
    protected final AppOpsManager mAppOps;
    protected final Context mContext;
    protected SMSDispatcher mDispatcher;
    private boolean mInsertMessageSuccess;
    protected Phone mPhone;
    private IccSmsStorageStatus mSimMemStatus;
    private List<SmsRawData> mSms;
    protected boolean mSuccess;
    private final UserManager mUserManager;
    protected final Object mLock = new Object();
    protected final Object mLoadLock = new Object();
    private CellBroadcastRangeManager mCellBroadcastRangeManager = new CellBroadcastRangeManager();
    private CdmaBroadcastRangeManager mCdmaBroadcastRangeManager = new CdmaBroadcastRangeManager();
    private final Object mSimInsertLock = new Object();
    private SimSmsInsertStatus smsInsertRet = new SimSmsInsertStatus(0, UsimPBMemInfo.STRING_NOT_SET);
    private SimSmsInsertStatus smsInsertRet2 = new SimSmsInsertStatus(0, UsimPBMemInfo.STRING_NOT_SET);
    private SmsParameters mSmsParams = null;
    private boolean mSmsParamsSuccess = false;
    private SmsRawData mSmsRawData = null;
    private SmsBroadcastConfigInfo[] mSmsCBConfig = null;
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Object obj;
            switch (msg.what) {
                case 1:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLoadLock;
                    synchronized (obj) {
                        if (ar.exception == null) {
                            IccSmsInterfaceManager.this.mSms = IccSmsInterfaceManager.this.buildValidRawData((ArrayList) ar.result);
                            IccSmsInterfaceManager.this.markMessagesAsRead((ArrayList) ar.result);
                        } else {
                            if (Rlog.isLoggable("SMS", 3)) {
                                IccSmsInterfaceManager.this.log("Cannot load Sms records");
                            }
                            IccSmsInterfaceManager.this.mSms = null;
                        }
                        IccSmsInterfaceManager.this.mLoadLock.notifyAll();
                        break;
                    }
                    break;
                case 2:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    synchronized (IccSmsInterfaceManager.this.mLock) {
                        IccSmsInterfaceManager.this.mSuccess = ar2.exception == null;
                        if (IccSmsInterfaceManager.this.mSuccess) {
                            try {
                                try {
                                    int index = ((int[]) ar2.result)[0];
                                    IccSmsInterfaceManager.this.smsInsertRet2.indexInIcc += index + IccSmsInterfaceManager.INDEXT_SPLITOR;
                                    IccSmsInterfaceManager.this.log("[insertRaw save one pdu in index " + index);
                                } catch (ClassCastException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            IccSmsInterfaceManager.this.log("[insertRaw fail to insert raw into ICC");
                            IccSmsInterfaceManager.this.smsInsertRet2.indexInIcc += "-1,";
                        }
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    if (ar2.exception != null) {
                        CommandException e2 = (CommandException) ar2.exception;
                        IccSmsInterfaceManager.this.log("Cannot update SMS " + e2.getCommandError());
                        if (e2.getCommandError() == CommandException.Error.SIM_FULL) {
                            IccSmsInterfaceManager.this.mDispatcher.handleIccFull();
                            return;
                        }
                        return;
                    }
                    return;
                case 3:
                case 4:
                case 101:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        IccSmsInterfaceManager.this.mSuccess = ar3.exception == null;
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                case 102:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar4.exception == null) {
                            IccSmsInterfaceManager.this.mSuccess = true;
                            if (IccSmsInterfaceManager.this.mSimMemStatus == null) {
                                IccSmsInterfaceManager.this.mSimMemStatus = new IccSmsStorageStatus();
                            }
                            IccSmsStorageStatus tmpStatus = (IccSmsStorageStatus) ar4.result;
                            IccSmsInterfaceManager.this.mSimMemStatus.mUsed = tmpStatus.mUsed;
                            IccSmsInterfaceManager.this.mSimMemStatus.mTotal = tmpStatus.mTotal;
                        } else {
                            IccSmsInterfaceManager.this.log("Cannot Get Sms SIM Memory Status from SIM");
                        }
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                case IccSmsInterfaceManager.EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mSimInsertLock;
                    synchronized (obj) {
                        IccSmsInterfaceManager.this.mInsertMessageSuccess = ar5.exception == null;
                        if (IccSmsInterfaceManager.this.mInsertMessageSuccess) {
                            try {
                                int index2 = ((int[]) ar5.result)[0];
                                IccSmsInterfaceManager.this.smsInsertRet.indexInIcc += index2 + IccSmsInterfaceManager.INDEXT_SPLITOR;
                                IccSmsInterfaceManager.this.log("insertText save one pdu in index " + index2);
                            } catch (ClassCastException e3) {
                                e3.printStackTrace();
                            } catch (Exception ex2) {
                                ex2.printStackTrace();
                            }
                        } else {
                            IccSmsInterfaceManager.this.log("insertText fail to insert sms into ICC");
                            IccSmsInterfaceManager.this.smsInsertRet.indexInIcc += "-1,";
                        }
                        IccSmsInterfaceManager.this.mSimInsertLock.notifyAll();
                        break;
                    }
                    break;
                case 104:
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar6.exception == null) {
                            try {
                                IccSmsInterfaceManager.this.mSmsParams = (SmsParameters) ar6.result;
                            } catch (ClassCastException e4) {
                                IccSmsInterfaceManager.this.log("[EFsmsp fail to get sms params ClassCastException");
                                e4.printStackTrace();
                            } catch (Exception ex3) {
                                IccSmsInterfaceManager.this.log("[EFsmsp fail to get sms params Exception");
                                ex3.printStackTrace();
                            }
                        } else {
                            IccSmsInterfaceManager.this.log("[EFsmsp fail to get sms params");
                            IccSmsInterfaceManager.this.mSmsParams = null;
                        }
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                case 105:
                    AsyncResult ar7 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar7.exception == null) {
                            IccSmsInterfaceManager.this.mSmsParamsSuccess = true;
                        } else {
                            IccSmsInterfaceManager.this.log("[EFsmsp fail to set sms params");
                            IccSmsInterfaceManager.this.mSmsParamsSuccess = false;
                        }
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                case 106:
                    AsyncResult ar8 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar8.exception == null) {
                            try {
                                byte[] rawData = (byte[]) ar8.result;
                                if (rawData[0] == 0) {
                                    IccSmsInterfaceManager.this.log("sms raw data status is FREE");
                                    IccSmsInterfaceManager.this.mSmsRawData = null;
                                } else {
                                    IccSmsInterfaceManager.this.mSmsRawData = new SmsRawData(rawData);
                                }
                            } catch (ClassCastException e5) {
                                IccSmsInterfaceManager.this.log("fail to get sms raw data ClassCastException");
                                e5.printStackTrace();
                                IccSmsInterfaceManager.this.mSmsRawData = null;
                            }
                        } else {
                            IccSmsInterfaceManager.this.log("fail to get sms raw data rild");
                            IccSmsInterfaceManager.this.mSmsRawData = null;
                        }
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                case IccSmsInterfaceManager.EVENT_GET_BROADCAST_CONFIG_DONE:
                    AsyncResult ar9 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar9.exception == null) {
                            ArrayList<SmsBroadcastConfigInfo> mList = (ArrayList) ar9.result;
                            if (mList.size() != 0) {
                                IccSmsInterfaceManager.this.mSmsCBConfig = new SmsBroadcastConfigInfo[mList.size()];
                                mList.toArray(IccSmsInterfaceManager.this.mSmsCBConfig);
                                if (IccSmsInterfaceManager.this.mSmsCBConfig != null) {
                                    IccSmsInterfaceManager.this.log("config size=" + IccSmsInterfaceManager.this.mSmsCBConfig.length);
                                    for (int index3 = 0; index3 < IccSmsInterfaceManager.this.mSmsCBConfig.length; index3++) {
                                        IccSmsInterfaceManager.this.log("mSmsCBConfig[" + index3 + "] = Channel id: " + IccSmsInterfaceManager.this.mSmsCBConfig[index3].getFromServiceId() + "-" + IccSmsInterfaceManager.this.mSmsCBConfig[index3].getToServiceId() + ", Language: " + IccSmsInterfaceManager.this.mSmsCBConfig[index3].getFromCodeScheme() + "-" + IccSmsInterfaceManager.this.mSmsCBConfig[index3].getToCodeScheme() + ", Selected: " + IccSmsInterfaceManager.this.mSmsCBConfig[index3].isSelected());
                                    }
                                }
                            }
                        } else {
                            IccSmsInterfaceManager.this.log("Cannot Get CB configs");
                        }
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                case IccSmsInterfaceManager.EVENT_GET_BROADCAST_ACTIVATION_DONE:
                    AsyncResult ar10 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar10.exception == null) {
                            ArrayList<SmsBroadcastConfigInfo> list = (ArrayList) ar10.result;
                            if (list.size() == 0) {
                                IccSmsInterfaceManager.this.mSuccess = false;
                            } else {
                                SmsBroadcastConfigInfo cbConfig = list.get(0);
                                IccSmsInterfaceManager.this.log("cbConfig: " + cbConfig.toString());
                                if (cbConfig.getFromCodeScheme() == -1 && cbConfig.getToCodeScheme() == -1 && cbConfig.getFromServiceId() == -1 && cbConfig.getToServiceId() == -1 && !cbConfig.isSelected()) {
                                    IccSmsInterfaceManager.this.mSuccess = false;
                                } else {
                                    IccSmsInterfaceManager.this.mSuccess = true;
                                }
                            }
                        }
                        IccSmsInterfaceManager.this.log("queryCbActivation: " + IccSmsInterfaceManager.this.mSuccess);
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                case 109:
                    AsyncResult ar11 = (AsyncResult) msg.obj;
                    obj = IccSmsInterfaceManager.this.mLock;
                    synchronized (obj) {
                        IccSmsInterfaceManager.this.mSuccess = ar11.exception == null;
                        IccSmsInterfaceManager.this.mLock.notifyAll();
                        break;
                    }
                    break;
                default:
                    return;
            }
        }
    };
    private BroadcastReceiver mSmsWipeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            IccSmsInterfaceManager.this.log("Receive intent");
            if (!intent.getAction().equals("com.mediatek.dm.LAWMO_WIPE")) {
                return;
            }
            IccSmsInterfaceManager.this.log("Receive wipe intent");
            Thread t = new Thread() {
                @Override
                public void run() {
                    IccSmsInterfaceManager.this.log("Delete message on sub " + IccSmsInterfaceManager.this.mPhone.getSubId());
                    Message response = IccSmsInterfaceManager.this.mHandler.obtainMessage(2);
                    IccSmsInterfaceManager.this.mPhone.mCi.deleteSmsOnSim(-1, response);
                }
            };
            t.start();
        }
    };

    protected IccSmsInterfaceManager(Phone phone) {
        this.mPhone = phone;
        this.mContext = phone.getContext();
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mDispatcher = new ImsSMSDispatcher(phone, phone.mSmsStorageMonitor, phone.mSmsUsageMonitor);
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mediatek.dm.LAWMO_WIPE");
        this.mContext.registerReceiver(this.mSmsWipeReceiver, filter);
    }

    protected void markMessagesAsRead(ArrayList<byte[]> messages) {
        if (messages == null) {
            return;
        }
        IccFileHandler fh = this.mPhone.getIccFileHandler();
        if (fh == null) {
            if (Rlog.isLoggable("SMS", 3)) {
                log("markMessagesAsRead - aborting, no icc card present.");
                return;
            }
            return;
        }
        int count = messages.size();
        for (int i = 0; i < count; i++) {
            byte[] ba = messages.get(i);
            if (ba[0] == 3) {
                int n = ba.length;
                byte[] nba = new byte[n - 1];
                System.arraycopy(ba, 1, nba, 0, n - 1);
                byte[] record = makeSmsRecordData(1, nba);
                fh.updateEFLinearFixed(IccConstants.EF_SMS, i + 1, record, null, null);
                if (Rlog.isLoggable("SMS", 3)) {
                    log("SMS " + (i + 1) + " marked as read");
                }
            }
        }
    }

    protected void updatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mDispatcher.updatePhoneObject(phone);
    }

    protected void enforceReceiveAndSend(String message) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", message);
        this.mContext.enforceCallingOrSelfPermission("android.permission.SEND_SMS", message);
    }

    public boolean updateMessageOnIccEf(String callingPackage, int index, int status, byte[] pdu) {
        log("updateMessageOnIccEf: index=" + index + " status=" + status + " ==> (" + Arrays.toString(pdu) + ")");
        enforceReceiveAndSend("Updating message on Icc");
        log("updateMessageOnIccEf: callingPackage = " + callingPackage + ", Binder.getCallingUid() = " + Binder.getCallingUid());
        if (Binder.getCallingUid() == 1001) {
            callingPackage = "com.android.phone";
        }
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), callingPackage) != 0) {
            log("updateMessageOnIccEf: noteOp NOT ALLOWED");
            return false;
        }
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(2);
            if (status != 0) {
                IccFileHandler fh = this.mPhone.getIccFileHandler();
                if (fh == null) {
                    response.recycle();
                    return this.mSuccess;
                }
                byte[] record = makeSmsRecordData(status, pdu);
                fh.updateEFLinearFixed(IccConstants.EF_SMS, index, record, null, response);
            } else if (1 == this.mPhone.getPhoneType()) {
                this.mPhone.mCi.deleteSmsOnSim(index, response);
            } else {
                this.mPhone.mCi.deleteSmsOnRuim(index, response);
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
            return this.mSuccess;
        }
    }

    public boolean copyMessageToIccEf(String callingPackage, int status, byte[] pdu, byte[] smsc) {
        log("copyMessageToIccEf: status=" + status + " ==> pdu=(" + Arrays.toString(pdu) + "), smsc=(" + Arrays.toString(smsc) + ")");
        enforceReceiveAndSend("Copying message to Icc");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), callingPackage) != 0) {
            return false;
        }
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(2);
            if (1 == this.mPhone.getPhoneType()) {
                this.mPhone.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), response);
            } else {
                if (status == 1 || status == 3) {
                    SmsMessage msg = SmsMessage.createFromPdu(pdu, SmsMessage.FORMAT_3GPP2);
                    if (msg == null) {
                        return false;
                    }
                    SmsMessage.SubmitPdu mpdu = com.android.internal.telephony.cdma.SmsMessage.createEfPdu(msg.getDisplayOriginatingAddress(), msg.getMessageBody(), msg.getTimestampMillis());
                    if (mpdu == null) {
                        return false;
                    }
                    pdu = mpdu.encodedMessage;
                }
                this.mPhone.mCi.writeSmsToRuim(status, IccUtils.bytesToHexString(pdu), response);
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
            return this.mSuccess;
        }
    }

    public List<SmsRawData> getAllMessagesFromIccEf(String callingPackage) {
        log("getAllMessagesFromEF " + callingPackage);
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", "Reading messages from Icc");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), callingPackage) != 0) {
            return new ArrayList();
        }
        synchronized (this.mLoadLock) {
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh == null) {
                Rlog.e(LOG_TAG, "Cannot load Sms records. No icc card?");
                this.mSms = null;
                return this.mSms;
            }
            Message response = this.mHandler.obtainMessage(1);
            fh.loadEFLinearFixedAll(IccConstants.EF_SMS, response);
            try {
                this.mLoadLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to load from the Icc");
            }
            return this.mSms;
        }
    }

    public void sendDataWithSelfPermissions(String callingPackage, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        this.mPhone.getContext().enforceCallingOrSelfPermission("android.permission.SEND_SMS", "Sending SMS message");
        sendDataInternal(callingPackage, destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
    }

    public void sendData(String callingPackage, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        sendDataInternal(callingPackage, destAddr, scAddr, destPort, data, sentIntent, deliveryIntent);
    }

    private void sendDataInternal(String callingPackage, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (Rlog.isLoggable("SMS", 2)) {
            log("sendData: destAddr=" + destAddr + " scAddr=" + scAddr + " destPort=" + destPort + " data='" + HexDump.toHexString(data) + "' sentIntent=" + sentIntent + " deliveryIntent=" + deliveryIntent);
        }
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        this.mDispatcher.sendData(filterDestAddress(destAddr), scAddr, destPort, data, sentIntent, deliveryIntent);
    }

    public void sendText(String callingPackage, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        sendTextInternal(callingPackage, destAddr, scAddr, text, sentIntent, deliveryIntent, persistMessageForNonDefaultSmsApp);
    }

    public void sendTextWithSelfPermissions(String callingPackage, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        this.mPhone.getContext().enforceCallingOrSelfPermission("android.permission.SEND_SMS", "Sending SMS message");
        sendTextInternal(callingPackage, destAddr, scAddr, text, sentIntent, deliveryIntent, true);
    }

    private void sendTextInternal(String callingPackage, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp) {
        if (Rlog.isLoggable("SMS", 2)) {
            log("sendText: destAddr=" + destAddr + " scAddr=" + scAddr + " text='" + text + "' sentIntent=" + sentIntent + " deliveryIntent=" + deliveryIntent);
        }
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        if (!persistMessageForNonDefaultSmsApp) {
            enforceCarrierPrivilege();
        }
        this.mDispatcher.sendText(filterDestAddress(destAddr), scAddr, text, sentIntent, deliveryIntent, null, callingPackage, persistMessageForNonDefaultSmsApp);
    }

    public void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        enforceCarrierPrivilege();
        if (Rlog.isLoggable("SMS", 2)) {
            log("pdu: " + pdu + "\n format=" + format + "\n receivedIntent=" + receivedIntent);
        }
        this.mDispatcher.injectSmsPdu(pdu, format, receivedIntent);
    }

    public void sendMultipartText(String callingPackage, String destAddr, String scAddr, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (!persistMessageForNonDefaultSmsApp) {
            enforceCarrierPrivilege();
        }
        if (Rlog.isLoggable("SMS", 2)) {
            int i = 0;
            for (String part : parts) {
                log("sendMultipartText: destAddr=" + destAddr + ", srAddr=" + scAddr + ", part[" + i + "]=" + part);
                i++;
            }
        }
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        String destAddr2 = filterDestAddress(destAddr);
        if (parts.size() <= 1 || parts.size() >= 10 || android.telephony.SmsMessage.hasEmsSupport()) {
            this.mDispatcher.sendMultipartText(destAddr2, scAddr, (ArrayList) parts, (ArrayList) sentIntents, (ArrayList) deliveryIntents, null, callingPackage, persistMessageForNonDefaultSmsApp);
            return;
        }
        for (int i2 = 0; i2 < parts.size(); i2++) {
            String singlePart = parts.get(i2);
            String singlePart2 = android.telephony.SmsMessage.shouldAppendPageNumberAsPrefix() ? String.valueOf(i2 + 1) + '/' + parts.size() + ' ' + singlePart : singlePart.concat(' ' + String.valueOf(i2 + 1) + '/' + parts.size());
            PendingIntent singleSentIntent = null;
            if (sentIntents != null && sentIntents.size() > i2) {
                singleSentIntent = sentIntents.get(i2);
            }
            PendingIntent singleDeliveryIntent = null;
            if (deliveryIntents != null && deliveryIntents.size() > i2) {
                singleDeliveryIntent = deliveryIntents.get(i2);
            }
            this.mDispatcher.sendText(destAddr2, scAddr, singlePart2, singleSentIntent, singleDeliveryIntent, null, callingPackage, persistMessageForNonDefaultSmsApp);
        }
    }

    public int getPremiumSmsPermission(String packageName) {
        return this.mDispatcher.getPremiumSmsPermission(packageName);
    }

    public void setPremiumSmsPermission(String packageName, int permission) {
        this.mDispatcher.setPremiumSmsPermission(packageName, permission);
    }

    protected ArrayList<SmsRawData> buildValidRawData(ArrayList<byte[]> messages) {
        int count = messages.size();
        ArrayList<SmsRawData> ret = new ArrayList<>(count);
        int validSmsCount = 0;
        for (int i = 0; i < count; i++) {
            byte[] ba = messages.get(i);
            if (ba[0] == 0) {
                ret.add(null);
            } else {
                validSmsCount++;
                ret.add(new SmsRawData(messages.get(i)));
            }
        }
        log("validSmsCount = " + validSmsCount);
        return ret;
    }

    protected byte[] makeSmsRecordData(int status, byte[] pdu) {
        byte[] data;
        if (1 == this.mPhone.getPhoneType()) {
            data = new byte[176];
        } else {
            data = new byte[255];
        }
        data[0] = (byte) (status & 7);
        log("ISIM-makeSmsRecordData: pdu size = " + pdu.length);
        if (pdu.length == 176) {
            log("ISIM-makeSmsRecordData: sim pdu");
            try {
                System.arraycopy(pdu, 1, data, 1, pdu.length - 1);
            } catch (ArrayIndexOutOfBoundsException e) {
                log("ISIM-makeSmsRecordData: out of bounds, sim pdu");
            }
        } else {
            log("ISIM-makeSmsRecordData: normal pdu");
            try {
                System.arraycopy(pdu, 0, data, 1, pdu.length);
            } catch (ArrayIndexOutOfBoundsException e2) {
                log("ISIM-makeSmsRecordData: out of bounds, normal pdu");
            }
        }
        for (int j = pdu.length + 1; j < data.length; j++) {
            data[j] = -1;
        }
        return data;
    }

    public boolean enableCellBroadcast(int messageIdentifier, int ranType) {
        return enableCellBroadcastRange(messageIdentifier, messageIdentifier, ranType);
    }

    public boolean disableCellBroadcast(int messageIdentifier, int ranType) {
        return disableCellBroadcastRange(messageIdentifier, messageIdentifier, ranType);
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        if (ranType == 0) {
            return enableGsmBroadcastRange(startMessageId, endMessageId);
        }
        if (ranType == 1) {
            return enableCdmaBroadcastRange(startMessageId, endMessageId);
        }
        throw new IllegalArgumentException("Not a supportted RAN Type");
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        if (ranType == 0) {
            return disableGsmBroadcastRange(startMessageId, endMessageId);
        }
        if (ranType == 1) {
            return disableCdmaBroadcastRange(startMessageId, endMessageId);
        }
        throw new IllegalArgumentException("Not a supportted RAN Type");
    }

    public synchronized boolean enableGsmBroadcastRange(int startMessageId, int endMessageId) {
        synchronized (this) {
            Context context = this.mPhone.getContext();
            context.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling cell broadcast SMS");
            String client = context.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!this.mCellBroadcastRangeManager.enableRange(startMessageId, endMessageId, client)) {
                log("Failed to add GSM cell broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
                return false;
            }
            log("Added GSM cell broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
            setCellBroadcastActivation(this.mCellBroadcastRangeManager.isEmpty() ? false : true);
            return true;
        }
    }

    public synchronized boolean disableGsmBroadcastRange(int startMessageId, int endMessageId) {
        synchronized (this) {
            Context context = this.mPhone.getContext();
            context.enforceCallingPermission("android.permission.RECEIVE_SMS", "Disabling cell broadcast SMS");
            String client = context.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!this.mCellBroadcastRangeManager.disableRange(startMessageId, endMessageId, client)) {
                log("Failed to remove GSM cell broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
                return false;
            }
            log("Removed GSM cell broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
            setCellBroadcastActivation(this.mCellBroadcastRangeManager.isEmpty() ? false : true);
            return true;
        }
    }

    public synchronized boolean enableCdmaBroadcastRange(int startMessageId, int endMessageId) {
        synchronized (this) {
            Context context = this.mPhone.getContext();
            context.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling cdma broadcast SMS");
            String client = context.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!this.mCdmaBroadcastRangeManager.enableRange(startMessageId, endMessageId, client)) {
                log("Failed to add cdma broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
                return false;
            }
            log("Added cdma broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
            setCdmaBroadcastActivation(this.mCdmaBroadcastRangeManager.isEmpty() ? false : true);
            return true;
        }
    }

    public synchronized boolean disableCdmaBroadcastRange(int startMessageId, int endMessageId) {
        synchronized (this) {
            Context context = this.mPhone.getContext();
            context.enforceCallingPermission("android.permission.RECEIVE_SMS", "Disabling cell broadcast SMS");
            String client = context.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!this.mCdmaBroadcastRangeManager.disableRange(startMessageId, endMessageId, client)) {
                log("Failed to remove cdma broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
                return false;
            }
            log("Removed cdma broadcast subscription for MID range " + startMessageId + " to " + endMessageId + " from client " + client);
            setCdmaBroadcastActivation(this.mCdmaBroadcastRangeManager.isEmpty() ? false : true);
            return true;
        }
    }

    class CellBroadcastRangeManager extends IntRangeManager {
        private ArrayList<SmsBroadcastConfigInfo> mConfigList = new ArrayList<>();

        CellBroadcastRangeManager() {
        }

        @Override
        protected void startUpdate() {
            this.mConfigList.clear();
        }

        @Override
        protected void addRange(int startId, int endId, boolean selected) {
            this.mConfigList.add(new SmsBroadcastConfigInfo(startId, endId, 0, 255, selected));
        }

        @Override
        protected boolean finishUpdate() {
            if (this.mConfigList.isEmpty()) {
                return true;
            }
            SmsBroadcastConfigInfo[] configs = (SmsBroadcastConfigInfo[]) this.mConfigList.toArray(new SmsBroadcastConfigInfo[this.mConfigList.size()]);
            return IccSmsInterfaceManager.this.setCellBroadcastConfig(configs);
        }
    }

    class CdmaBroadcastRangeManager extends IntRangeManager {
        private ArrayList<CdmaSmsBroadcastConfigInfo> mConfigList = new ArrayList<>();

        CdmaBroadcastRangeManager() {
        }

        @Override
        protected void startUpdate() {
            this.mConfigList.clear();
        }

        @Override
        protected void addRange(int startId, int endId, boolean selected) {
            this.mConfigList.add(new CdmaSmsBroadcastConfigInfo(startId, endId, 1, selected));
        }

        @Override
        protected boolean finishUpdate() {
            if (this.mConfigList.isEmpty()) {
                return true;
            }
            CdmaSmsBroadcastConfigInfo[] configs = (CdmaSmsBroadcastConfigInfo[]) this.mConfigList.toArray(new CdmaSmsBroadcastConfigInfo[this.mConfigList.size()]);
            return IccSmsInterfaceManager.this.setCdmaBroadcastConfig(configs);
        }
    }

    private boolean setCellBroadcastConfig(SmsBroadcastConfigInfo[] configs) {
        log("Calling setGsmBroadcastConfig with " + configs.length + " configurations");
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(4);
            this.mSuccess = false;
            this.mPhone.mCi.setGsmBroadcastConfig(configs, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast config");
            }
        }
        return this.mSuccess;
    }

    private boolean setCellBroadcastActivation(boolean activate) {
        log("Calling setCellBroadcastActivation(" + activate + ')');
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(3);
            this.mSuccess = false;
            this.mPhone.mCi.setGsmBroadcastActivation(activate, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast activation");
            }
        }
        if (!activate && this.mSuccess) {
            this.mCellBroadcastRangeManager.clearAllRanges();
        }
        return this.mSuccess;
    }

    private boolean setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs) {
        log("Calling setCdmaBroadcastConfig with " + configs.length + " configurations");
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(4);
            this.mSuccess = false;
            this.mPhone.mCi.setCdmaBroadcastConfig(configs, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cdma broadcast config");
            }
        }
        return this.mSuccess;
    }

    private boolean setCdmaBroadcastActivation(boolean activate) {
        log("Calling setCdmaBroadcastActivation(" + activate + ")");
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(3);
            this.mSuccess = false;
            this.mPhone.mCi.setCdmaBroadcastActivation(activate, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cdma broadcast activation");
            }
        }
        return this.mSuccess;
    }

    protected void log(String msg) {
        Rlog.d(LOG_TAG, "[IccSmsInterfaceManager] " + msg);
    }

    public boolean isImsSmsSupported() {
        return this.mDispatcher.isIms();
    }

    public String getImsSmsFormat() {
        return this.mDispatcher.getImsSmsFormat();
    }

    public void sendStoredText(String callingPkg, Uri messageUri, String scAddress, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (Rlog.isLoggable("SMS", 2)) {
            log("sendStoredText: scAddr=" + scAddress + " messageUri=" + messageUri + " sentIntent=" + sentIntent + " deliveryIntent=" + deliveryIntent);
        }
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPkg) != 0) {
            return;
        }
        ContentResolver resolver = this.mPhone.getContext().getContentResolver();
        if (!isFailedOrDraft(resolver, messageUri)) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(sentIntent);
            return;
        }
        String[] textAndAddress = loadTextAndAddress(resolver, messageUri);
        if (textAndAddress == null) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredText: can not load text");
            returnUnspecifiedFailure(sentIntent);
        } else {
            textAndAddress[1] = filterDestAddress(textAndAddress[1]);
            this.mDispatcher.sendText(textAndAddress[1], scAddress, textAndAddress[0], sentIntent, deliveryIntent, messageUri, callingPkg, true);
        }
    }

    public void sendStoredMultipartText(String callingPkg, Uri messageUri, String scAddress, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) {
        String singlePart;
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPkg) != 0) {
            return;
        }
        ContentResolver resolver = this.mPhone.getContext().getContentResolver();
        if (!isFailedOrDraft(resolver, messageUri)) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        String[] textAndAddress = loadTextAndAddress(resolver, messageUri);
        if (textAndAddress == null) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: can not load text");
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        ArrayList<String> parts = SmsManager.getDefault().divideMessage(textAndAddress[0]);
        if (parts == null || parts.size() < 1) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: can not divide text");
            returnUnspecifiedFailure(sentIntents);
            return;
        }
        textAndAddress[1] = filterDestAddress(textAndAddress[1]);
        if (parts.size() > 1 && parts.size() < 10 && !android.telephony.SmsMessage.hasEmsSupport()) {
            for (int i = 0; i < parts.size(); i++) {
                String singlePart2 = parts.get(i);
                if (android.telephony.SmsMessage.shouldAppendPageNumberAsPrefix()) {
                    singlePart = String.valueOf(i + 1) + '/' + parts.size() + ' ' + singlePart2;
                } else {
                    singlePart = singlePart2.concat(' ' + String.valueOf(i + 1) + '/' + parts.size());
                }
                PendingIntent singleSentIntent = null;
                if (sentIntents != null && sentIntents.size() > i) {
                    singleSentIntent = sentIntents.get(i);
                }
                PendingIntent singleDeliveryIntent = null;
                if (deliveryIntents != null && deliveryIntents.size() > i) {
                    singleDeliveryIntent = deliveryIntents.get(i);
                }
                this.mDispatcher.sendText(textAndAddress[1], scAddress, singlePart, singleSentIntent, singleDeliveryIntent, messageUri, callingPkg, true);
            }
            return;
        }
        this.mDispatcher.sendMultipartText(textAndAddress[1], scAddress, parts, (ArrayList) sentIntents, (ArrayList) deliveryIntents, messageUri, callingPkg, true);
    }

    private boolean isFailedOrDraft(ContentResolver resolver, Uri messageUri) {
        long identity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(messageUri, new String[]{"type"}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int type = cursor.getInt(0);
                    boolean z = type == 3 || type == 5;
                    if (cursor != null) {
                        cursor.close();
                    }
                    Binder.restoreCallingIdentity(identity);
                    return z;
                }
                if (cursor != null) {
                    cursor.close();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (SQLiteException e) {
                Log.e(LOG_TAG, "[IccSmsInterfaceManager]isFailedOrDraft: query message type failed", e);
                if (cursor != null) {
                    cursor.close();
                }
                Binder.restoreCallingIdentity(identity);
            }
            return false;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private String[] loadTextAndAddress(ContentResolver resolver, Uri messageUri) {
        long identity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(messageUri, new String[]{"body", "address"}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String[] strArr = {cursor.getString(0), cursor.getString(1)};
                    if (cursor != null) {
                        cursor.close();
                    }
                    Binder.restoreCallingIdentity(identity);
                    return strArr;
                }
                if (cursor != null) {
                    cursor.close();
                }
                Binder.restoreCallingIdentity(identity);
            } catch (SQLiteException e) {
                Log.e(LOG_TAG, "[IccSmsInterfaceManager]loadText: query message text failed", e);
                if (cursor != null) {
                    cursor.close();
                }
                Binder.restoreCallingIdentity(identity);
            }
            return null;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private void returnUnspecifiedFailure(PendingIntent pi) {
        if (pi == null) {
            return;
        }
        try {
            pi.send(1);
        } catch (PendingIntent.CanceledException e) {
        }
    }

    private void returnUnspecifiedFailure(List<PendingIntent> pis) {
        if (pis == null) {
            return;
        }
        for (PendingIntent pi : pis) {
            returnUnspecifiedFailure(pi);
        }
    }

    private void enforceCarrierPrivilege() {
        UiccController controller = UiccController.getInstance();
        if (controller == null || controller.getUiccCard(this.mPhone.getPhoneId()) == null) {
            throw new SecurityException("No Carrier Privilege: No UICC");
        }
        if (controller.getUiccCard(this.mPhone.getPhoneId()).getCarrierPrivilegeStatusForCurrentTransaction(this.mContext.getPackageManager()) == 1) {
        } else {
            throw new SecurityException("No Carrier Privilege.");
        }
    }

    private String filterDestAddress(String destAddr) {
        String result = SmsNumberUtils.filterDestAddr(this.mPhone, destAddr);
        return result != null ? result : destAddr;
    }

    public void sendDataWithOriginalPort(String callingPackage, String destAddr, String scAddr, int destPort, int originalPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(LOG_TAG, "Enter IccSmsInterfaceManager.sendDataWithOriginalPort");
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (Rlog.isLoggable("SMS", 2)) {
            log("sendData: destAddr=" + destAddr + " scAddr=" + scAddr + " destPort=" + destPort + " originalPort=" + originalPort + " data='" + HexDump.toHexString(data) + "' sentIntent=" + sentIntent + " deliveryIntent=" + deliveryIntent);
        }
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        this.mDispatcher.sendData(destAddr, scAddr, destPort, originalPort, data, sentIntent, deliveryIntent);
    }

    public void sendMultipartData(String callingPackage, String destAddr, String scAddr, int destPort, List<SmsRawData> data, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (Rlog.isLoggable("SMS", 2)) {
            for (SmsRawData rData : data) {
                log("sendMultipartData: destAddr=" + destAddr + " scAddr=" + scAddr + " destPort=" + destPort + " data='" + HexDump.toHexString(rData.getBytes()));
            }
        }
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        this.mDispatcher.sendMultipartData(destAddr, scAddr, destPort, (ArrayList) data, (ArrayList) sentIntents, (ArrayList) deliveryIntents);
    }

    public void setSmsMemoryStatus(boolean status) {
        log("setSmsMemoryStatus: set storage status -> " + status);
        this.mDispatcher.setSmsMemoryStatus(status);
    }

    public boolean isSmsReady() {
        boolean isReady = this.mDispatcher.isSmsReady();
        log("isSmsReady: " + isReady);
        return isReady;
    }

    public void sendTextWithEncodingType(String callingPackage, String destAddr, String scAddr, String text, int encodingType, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessage) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        this.mDispatcher.sendTextWithEncodingType(destAddr, scAddr, text, encodingType, sentIntent, deliveryIntent, null, callingPackage, persistMessage);
    }

    public void sendMultipartTextWithEncodingType(String callingPackage, String destAddr, String scAddr, List<String> parts, int encodingType, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp) {
        String singlePart;
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        String destAddr2 = filterDestAddress(destAddr);
        if (parts.size() > 1 && parts.size() < 10 && !android.telephony.SmsMessage.hasEmsSupport()) {
            for (int i = 0; i < parts.size(); i++) {
                String singlePart2 = parts.get(i);
                if (android.telephony.SmsMessage.shouldAppendPageNumberAsPrefix()) {
                    singlePart = String.valueOf(i + 1) + '/' + parts.size() + ' ' + singlePart2;
                } else {
                    singlePart = singlePart2.concat(' ' + String.valueOf(i + 1) + '/' + parts.size());
                }
                PendingIntent singleSentIntent = null;
                if (sentIntents != null && sentIntents.size() > i) {
                    singleSentIntent = sentIntents.get(i);
                }
                PendingIntent singleDeliveryIntent = null;
                if (deliveryIntents != null && deliveryIntents.size() > i) {
                    singleDeliveryIntent = deliveryIntents.get(i);
                }
                this.mDispatcher.sendTextWithEncodingType(destAddr2, scAddr, singlePart, encodingType, singleSentIntent, singleDeliveryIntent, null, callingPackage, persistMessageForNonDefaultSmsApp);
            }
            return;
        }
        this.mDispatcher.sendMultipartTextWithEncodingType(destAddr2, scAddr, (ArrayList) parts, encodingType, (ArrayList) sentIntents, (ArrayList) deliveryIntents, null, callingPackage, persistMessageForNonDefaultSmsApp);
    }

    public void sendTextWithExtraParams(String callingPackage, String destAddr, String scAddr, String text, Bundle extraParams, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessage) {
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        this.mDispatcher.sendTextWithExtraParams(destAddr, scAddr, text, extraParams, sentIntent, deliveryIntent, null, callingPackage, persistMessage);
    }

    public void sendMultipartTextWithExtraParams(String callingPackage, String destAddr, String scAddr, List<String> parts, Bundle extraParams, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents, boolean persistMessageForNonDefaultSmsApp) {
        String singlePart;
        this.mPhone.getContext().enforceCallingPermission("android.permission.SEND_SMS", "Sending SMS message");
        if (this.mAppOps.noteOp(20, Binder.getCallingUid(), callingPackage) != 0) {
            return;
        }
        String destAddr2 = filterDestAddress(destAddr);
        if (parts.size() > 1 && parts.size() < 10 && !android.telephony.SmsMessage.hasEmsSupport()) {
            for (int i = 0; i < parts.size(); i++) {
                String singlePart2 = parts.get(i);
                if (android.telephony.SmsMessage.shouldAppendPageNumberAsPrefix()) {
                    singlePart = String.valueOf(i + 1) + '/' + parts.size() + ' ' + singlePart2;
                } else {
                    singlePart = singlePart2.concat(' ' + String.valueOf(i + 1) + '/' + parts.size());
                }
                PendingIntent singleSentIntent = null;
                if (sentIntents != null && sentIntents.size() > i) {
                    singleSentIntent = sentIntents.get(i);
                }
                PendingIntent singleDeliveryIntent = null;
                if (deliveryIntents != null && deliveryIntents.size() > i) {
                    singleDeliveryIntent = deliveryIntents.get(i);
                }
                this.mDispatcher.sendTextWithExtraParams(destAddr2, scAddr, singlePart, extraParams, singleSentIntent, singleDeliveryIntent, null, callingPackage, persistMessageForNonDefaultSmsApp);
            }
            return;
        }
        this.mDispatcher.sendMultipartTextWithExtraParams(destAddr2, scAddr, (ArrayList) parts, extraParams, (ArrayList) sentIntents, (ArrayList) deliveryIntents, null, callingPackage, persistMessageForNonDefaultSmsApp);
    }

    public String getFormat() {
        return this.mDispatcher.getFormat();
    }

    public SmsRawData getMessageFromIccEf(String callingPackage, int index) {
        log("getMessageFromIccEf");
        this.mPhone.getContext().enforceCallingPermission("android.permission.RECEIVE_SMS", "Reading messages from SIM");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), callingPackage) != 0) {
            return null;
        }
        this.mSmsRawData = null;
        synchronized (this.mLock) {
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                Message response = this.mHandler.obtainMessage(106);
                this.mPhone.getIccFileHandler().loadEFLinearFixed(IccConstants.EF_SMS, index, response);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to load from the SIM");
                }
            }
        }
        return this.mSmsRawData;
    }

    public List<SmsRawData> getAllMessagesFromIccEfByMode(String callingPackage, int mode) {
        log("getAllMessagesFromIccEfByMode, mode=" + mode);
        if (mode < 1 || mode > 2) {
            log("getAllMessagesFromIccEfByMode wrong mode=" + mode);
            return this.mSms;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", "Reading messages from Icc");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), callingPackage) != 0) {
            return new ArrayList();
        }
        synchronized (this.mLoadLock) {
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh == null) {
                Rlog.e(LOG_TAG, "Cannot load Sms records. No icc card?");
                if (this.mSms != null) {
                    this.mSms.clear();
                    return this.mSms;
                }
            }
            Message response = this.mHandler.obtainMessage(1);
            this.mPhone.getIccFileHandler().loadEFLinearFixedAll(IccConstants.EF_SMS, mode, response);
            try {
                this.mLoadLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to load from the SIM");
            }
            return this.mSms;
        }
    }

    public SmsParameters getSmsParameters(String callingPackage) {
        log("getSmsParameters");
        enforceReceiveAndSend("Get SMS parametner on SIM");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), callingPackage) != 0) {
            return null;
        }
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(104);
            this.mPhone.mCi.getSmsParameters(response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get sms params");
            }
        }
        return this.mSmsParams;
    }

    public boolean setSmsParameters(String callingPackage, SmsParameters params) {
        log("setSmsParameters");
        enforceReceiveAndSend("Set SMS parametner on SIM");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), callingPackage) != 0) {
            return false;
        }
        this.mSmsParamsSuccess = false;
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(105);
            this.mPhone.mCi.setSmsParameters(params, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get sms params");
            }
        }
        return this.mSmsParamsSuccess;
    }

    public int copyTextMessageToIccCard(String callingPkg, String scAddress, String address, List<String> text, int status, long timestamp) {
        log("copyTextMessageToIccCard, sc address: " + scAddress + " address: " + address + " message count: " + text.size() + " status: " + status + " timestamp: " + timestamp);
        enforceReceiveAndSend("Copying message to USIM/SIM");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), callingPkg) != 0) {
            return 1;
        }
        IccSmsStorageStatus memStatus = getSmsSimMemoryStatus(callingPkg);
        if (memStatus == null) {
            log("Fail to get SIM memory status");
            return 1;
        }
        if (memStatus.getUnused() >= text.size()) {
            return this.mDispatcher.copyTextMessageToIccCard(scAddress, address, text, status, timestamp);
        }
        log("SIM memory is not enough");
        return 7;
    }

    public SimSmsInsertStatus insertTextMessageToIccCard(String callingPackage, String scAddress, String address, List<String> text, int status, long timestamp) {
        boolean isDeliverPdu;
        log("insertTextMessageToIccCard");
        enforceReceiveAndSend("insertText insert message into SIM");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), callingPackage) != 0) {
            this.smsInsertRet.insertStatus = 1;
            return this.smsInsertRet;
        }
        int msgCount = text.size();
        log("insertText scAddr=" + scAddress + ", addr=" + address + ", msgCount=" + msgCount + ", status=" + status + ", timestamp=" + timestamp);
        this.smsInsertRet.indexInIcc = UsimPBMemInfo.STRING_NOT_SET;
        IccSmsStorageStatus memStatus = getSmsSimMemoryStatus(callingPackage);
        if (memStatus != null) {
            int unused = memStatus.getUnused();
            if (unused < msgCount) {
                log("insertText SIM mem is not enough [" + unused + "/" + msgCount + "]");
                this.smsInsertRet.insertStatus = 7;
                return this.smsInsertRet;
            }
            if (!checkPhoneNumberInternal(scAddress)) {
                log("insertText invalid sc address");
                scAddress = null;
            }
            if (!checkPhoneNumberInternal(address)) {
                log("insertText invalid address");
                this.smsInsertRet.insertStatus = 8;
                return this.smsInsertRet;
            }
            if (status == 1 || status == 3) {
                log("insertText to encode delivery pdu");
                isDeliverPdu = true;
            } else if (status == 5 || status == 7) {
                log("insertText to encode submit pdu");
                isDeliverPdu = false;
            } else {
                log("insertText invalid status " + status);
                this.smsInsertRet.insertStatus = 1;
                return this.smsInsertRet;
            }
            log("insertText params check pass");
            if (2 == this.mPhone.getPhoneType()) {
                return writeTextMessageToRuim(address, text, status, timestamp);
            }
            int encoding = 0;
            GsmAlphabet.TextEncodingDetails[] details = new GsmAlphabet.TextEncodingDetails[msgCount];
            for (int i = 0; i < msgCount; i++) {
                details[i] = com.android.internal.telephony.gsm.SmsMessage.calculateLength(text.get(i), false);
                if (encoding != details[i].codeUnitSize && (encoding == 0 || encoding == 1)) {
                    encoding = details[i].codeUnitSize;
                }
            }
            log("insertText create & insert pdu start...");
            for (int i2 = 0; i2 < msgCount; i2++) {
                if (!this.mInsertMessageSuccess && i2 > 0) {
                    log("insertText last message insert fail");
                    this.smsInsertRet.insertStatus = 1;
                    return this.smsInsertRet;
                }
                int singleShiftId = -1;
                int lockingShiftId = -1;
                int language = details[i2].shiftLangId;
                int encoding_detail = encoding;
                if (encoding == 1) {
                    if (details[i2].languageTable > 0 && details[i2].languageShiftTable > 0) {
                        singleShiftId = details[i2].languageTable;
                        lockingShiftId = details[i2].languageShiftTable;
                        encoding_detail = 13;
                    } else if (details[i2].languageShiftTable > 0) {
                        lockingShiftId = details[i2].languageShiftTable;
                        encoding_detail = 12;
                    } else if (details[i2].languageTable > 0) {
                        singleShiftId = details[i2].languageTable;
                        encoding_detail = 11;
                    }
                }
                byte[] smsHeader = null;
                if (msgCount > 1) {
                    log("insertText create pdu header for concat-message");
                    smsHeader = SmsHeader.getSubmitPduHeaderWithLang(-1, getNextConcatRef() & 255, i2 + 1, msgCount, singleShiftId, lockingShiftId);
                }
                if (isDeliverPdu) {
                    SmsMessage.DeliverPdu pdu = com.android.internal.telephony.gsm.SmsMessage.getDeliverPduWithLang(scAddress, address, text.get(i2), smsHeader, timestamp, encoding_detail, language);
                    if (pdu != null) {
                        this.mPhone.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(pdu.encodedScAddress), IccUtils.bytesToHexString(pdu.encodedMessage), this.mHandler.obtainMessage(EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE));
                    } else {
                        log("insertText fail to create deliver pdu");
                        this.smsInsertRet.insertStatus = 1;
                        return this.smsInsertRet;
                    }
                } else {
                    SmsMessage.SubmitPdu pdu2 = com.android.internal.telephony.gsm.SmsMessage.getSubmitPduWithLang(scAddress, address, text.get(i2), false, smsHeader, encoding_detail, language);
                    if (pdu2 != null) {
                        this.mPhone.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(pdu2.encodedScAddress), IccUtils.bytesToHexString(pdu2.encodedMessage), this.mHandler.obtainMessage(EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE));
                    } else {
                        log("insertText fail to create submit pdu");
                        this.smsInsertRet.insertStatus = 1;
                        return this.smsInsertRet;
                    }
                }
                synchronized (this.mSimInsertLock) {
                    try {
                        log("insertText wait until the pdu be wrote into the SIM");
                        this.mSimInsertLock.wait();
                    } catch (InterruptedException e) {
                        log("insertText fail to insert pdu");
                        this.smsInsertRet.insertStatus = 1;
                        return this.smsInsertRet;
                    }
                }
            }
            log("insertText create & insert pdu end");
            if (this.mInsertMessageSuccess) {
                log("insertText all messages inserted");
                this.smsInsertRet.insertStatus = 1;
                return this.smsInsertRet;
            }
            log("insertText pdu insert fail");
            this.smsInsertRet.insertStatus = 1;
            return this.smsInsertRet;
        }
        log("insertText fail to get SIM mem status");
        this.smsInsertRet.insertStatus = 1;
        return this.smsInsertRet;
    }

    public SimSmsInsertStatus insertRawMessageToIccCard(String callingPackage, int status, byte[] pdu, byte[] smsc) {
        log("insertRawMessageToIccCard");
        enforceReceiveAndSend("insertRaw insert message into SIM");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), callingPackage) != 0) {
            this.smsInsertRet2.insertStatus = 1;
            return this.smsInsertRet2;
        }
        synchronized (this.mLock) {
            this.mSuccess = false;
            this.smsInsertRet2.insertStatus = 1;
            this.smsInsertRet2.indexInIcc = UsimPBMemInfo.STRING_NOT_SET;
            Message response = this.mHandler.obtainMessage(2);
            if (2 != this.mPhone.getPhoneType()) {
                this.mPhone.mCi.writeSmsToSim(status, IccUtils.bytesToHexString(smsc), IccUtils.bytesToHexString(pdu), response);
            } else {
                this.mPhone.mCi.writeSmsToRuim(status, IccUtils.bytesToHexString(pdu), response);
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("insertRaw interrupted while trying to update by index");
            }
        }
        if (this.mSuccess) {
            log("insertRaw message inserted");
            this.smsInsertRet2.insertStatus = 0;
            return this.smsInsertRet2;
        }
        log("insertRaw pdu insert fail");
        this.smsInsertRet2.insertStatus = 1;
        return this.smsInsertRet2;
    }

    public IccSmsStorageStatus getSmsSimMemoryStatus(String callingPackage) {
        log("getSmsSimMemoryStatus");
        enforceReceiveAndSend("Get SMS SIM Card Memory Status from RUIM");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), callingPackage) != 0) {
            return null;
        }
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message response = this.mHandler.obtainMessage(102);
            this.mPhone.mCi.getSmsSimMemoryStatus(response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get SMS SIM Card Memory Status from SIM");
            }
        }
        if (this.mSuccess) {
            return this.mSimMemStatus;
        }
        return null;
    }

    public boolean setEtwsConfig(int mode) {
        log("Calling setEtwsConfig(" + mode + ')');
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(101);
            this.mSuccess = false;
            this.mPhone.mCi.setEtws(mode, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set ETWS config");
            }
        }
        return this.mSuccess;
    }

    public boolean activateCellBroadcastSms(boolean activate) {
        log("activateCellBroadcastSms activate : " + activate);
        return setCellBroadcastActivation(activate);
    }

    private static int getNextConcatRef() {
        int i = sConcatenatedRef;
        sConcatenatedRef = i + 1;
        return i;
    }

    private static boolean checkPhoneNumberCharacter(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '+' || c == '#' || c == 'N' || c == ' ' || c == '-';
    }

    private static boolean checkPhoneNumberInternal(String number) {
        if (number == null) {
            return true;
        }
        int n = number.length();
        for (int i = 0; i < n; i++) {
            if (!checkPhoneNumberCharacter(number.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private SmsCbConfigInfo Convert2SmsCbConfigInfo(SmsBroadcastConfigInfo info) {
        return new SmsCbConfigInfo(info.getFromServiceId(), info.getToServiceId(), info.getFromCodeScheme(), info.getToCodeScheme(), info.isSelected());
    }

    private SmsBroadcastConfigInfo Convert2SmsBroadcastConfigInfo(SmsCbConfigInfo info) {
        return new SmsBroadcastConfigInfo(info.mFromServiceId, info.mToServiceId, info.mFromCodeScheme, info.mToCodeScheme, info.mSelected);
    }

    public SmsCbConfigInfo[] getCellBroadcastSmsConfig() {
        log("getCellBroadcastSmsConfig");
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(EVENT_GET_BROADCAST_CONFIG_DONE);
            this.mSmsCBConfig = null;
            this.mPhone.mCi.getGsmBroadcastConfig(response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get CB config");
            }
        }
        if (this.mSmsCBConfig != null) {
            log("config length = " + this.mSmsCBConfig.length);
            if (this.mSmsCBConfig.length != 0) {
                SmsCbConfigInfo[] result = new SmsCbConfigInfo[this.mSmsCBConfig.length];
                for (int i = 0; i < this.mSmsCBConfig.length; i++) {
                    result[i] = Convert2SmsCbConfigInfo(this.mSmsCBConfig[i]);
                }
                return result;
            }
        }
        return null;
    }

    public boolean setCellBroadcastSmsConfig(SmsCbConfigInfo[] channels, SmsCbConfigInfo[] languages) {
        log("setCellBroadcastSmsConfig");
        if (channels == null && languages == null) {
            return true;
        }
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(4);
            this.mSuccess = false;
            ArrayList<SmsBroadcastConfigInfo> chid_list = new ArrayList<>();
            if (channels != null) {
                for (SmsCbConfigInfo smsCbConfigInfo : channels) {
                    chid_list.add(Convert2SmsBroadcastConfigInfo(smsCbConfigInfo));
                }
            }
            ArrayList<SmsBroadcastConfigInfo> lang_list = new ArrayList<>();
            if (languages != null) {
                for (SmsCbConfigInfo smsCbConfigInfo2 : languages) {
                    lang_list.add(Convert2SmsBroadcastConfigInfo(smsCbConfigInfo2));
                }
            }
            chid_list.addAll(lang_list);
            this.mPhone.mCi.setGsmBroadcastConfig((SmsBroadcastConfigInfo[]) chid_list.toArray(new SmsBroadcastConfigInfo[1]), response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set CB config");
            }
        }
        return this.mSuccess;
    }

    public boolean queryCellBroadcastSmsActivation() {
        log("queryCellBroadcastSmsActivation");
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(EVENT_GET_BROADCAST_ACTIVATION_DONE);
            this.mSuccess = false;
            this.mPhone.mCi.getGsmBroadcastConfig(response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to get CB activation");
            }
        }
        return this.mSuccess;
    }

    public boolean removeCellBroadcastMsg(int channelId, int serialId) {
        log("removeCellBroadcastMsg(" + channelId + " , " + serialId + ")");
        synchronized (this.mLock) {
            Message response = this.mHandler.obtainMessage(109);
            this.mSuccess = false;
            this.mPhone.mCi.removeCellBroadcastMsg(channelId, serialId, response);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to remove CB msg");
            }
        }
        return this.mSuccess;
    }

    protected SimSmsInsertStatus writeTextMessageToRuim(String address, List<String> text, int status, long timestamp) {
        SimSmsInsertStatus insertRet = new SimSmsInsertStatus(0, UsimPBMemInfo.STRING_NOT_SET);
        this.mSuccess = true;
        for (int i = 0; i < text.size(); i++) {
            if (!this.mSuccess) {
                log("[copyText Exception happened when copy message");
                insertRet.insertStatus = 1;
                return insertRet;
            }
            SmsMessage.SubmitPdu pdu = com.android.internal.telephony.cdma.SmsMessage.createEfPdu(address, text.get(i), timestamp);
            if (pdu != null) {
                Message response = this.mHandler.obtainMessage(EVENT_INSERT_TEXT_MESSAGE_TO_ICC_DONE);
                this.mPhone.mCi.writeSmsToRuim(status, IccUtils.bytesToHexString(pdu.encodedMessage), response);
                synchronized (this.mLock) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        log("InterruptedException " + e);
                        insertRet.insertStatus = 1;
                        return insertRet;
                    }
                }
            } else {
                log("writeTextMessageToRuim: pdu == null");
                insertRet.insertStatus = 1;
                return insertRet;
            }
        }
        log("writeTextMessageToRuim: done");
        insertRet.insertStatus = 0;
        return insertRet;
    }
}
