package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.HexDump;
import com.google.android.mms.pdu.PduHeaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class IccSmsInterfaceManager {
    static final boolean DBG = true;
    private static final int EVENT_LOAD_DONE = 1;
    protected static final int EVENT_SET_BROADCAST_ACTIVATION_DONE = 3;
    protected static final int EVENT_SET_BROADCAST_CONFIG_DONE = 4;
    private static final int EVENT_UPDATE_DONE = 2;
    static final String LOG_TAG = "IccSmsInterfaceManager";
    private static final int SMS_CB_CODE_SCHEME_MAX = 255;
    private static final int SMS_CB_CODE_SCHEME_MIN = 0;
    public static final int SMS_MESSAGE_PERIOD_NOT_SPECIFIED = -1;
    public static final int SMS_MESSAGE_PRIORITY_NOT_SPECIFIED = -1;
    protected final AppOpsManager mAppOps;
    private CdmaBroadcastRangeManager mCdmaBroadcastRangeManager;
    private CellBroadcastRangeManager mCellBroadcastRangeManager;
    protected final Context mContext;
    protected SmsDispatchersController mDispatchersController;
    protected Handler mHandler;
    protected final Object mLock;
    protected Phone mPhone;
    private List<SmsRawData> mSms;
    protected boolean mSuccess;
    private final UserManager mUserManager;

    protected IccSmsInterfaceManager(Phone phone) {
        this(phone, phone.getContext(), (AppOpsManager) phone.getContext().getSystemService("appops"), (UserManager) phone.getContext().getSystemService("user"), TelephonyComponentFactory.getInstance().makeSmsDispatchersController(phone, phone.mSmsStorageMonitor, phone.mSmsUsageMonitor));
    }

    @VisibleForTesting
    public IccSmsInterfaceManager(Phone phone, Context context, AppOpsManager appOpsManager, UserManager userManager, SmsDispatchersController smsDispatchersController) {
        this.mLock = new Object();
        this.mCellBroadcastRangeManager = new CellBroadcastRangeManager();
        this.mCdmaBroadcastRangeManager = new CdmaBroadcastRangeManager();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        synchronized (IccSmsInterfaceManager.this.mLock) {
                            if (asyncResult.exception == null) {
                                IccSmsInterfaceManager.this.mSms = IccSmsInterfaceManager.this.buildValidRawData((ArrayList) asyncResult.result);
                                IccSmsInterfaceManager.this.markMessagesAsRead((ArrayList) asyncResult.result);
                            } else {
                                if (Rlog.isLoggable("SMS", 3)) {
                                    IccSmsInterfaceManager.this.log("Cannot load Sms records");
                                }
                                IccSmsInterfaceManager.this.mSms = null;
                            }
                            IccSmsInterfaceManager.this.mLock.notifyAll();
                            break;
                        }
                        return;
                    case 2:
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        synchronized (IccSmsInterfaceManager.this.mLock) {
                            IccSmsInterfaceManager.this.mSuccess = asyncResult2.exception == null;
                            IccSmsInterfaceManager.this.mLock.notifyAll();
                            break;
                        }
                        return;
                    case 3:
                    case 4:
                        AsyncResult asyncResult3 = (AsyncResult) message.obj;
                        synchronized (IccSmsInterfaceManager.this.mLock) {
                            IccSmsInterfaceManager.this.mSuccess = asyncResult3.exception == null;
                            IccSmsInterfaceManager.this.mLock.notifyAll();
                            break;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mPhone = phone;
        this.mContext = context;
        this.mAppOps = appOpsManager;
        this.mUserManager = userManager;
        this.mDispatchersController = smsDispatchersController;
    }

    protected void markMessagesAsRead(ArrayList<byte[]> arrayList) {
        if (arrayList == null) {
            return;
        }
        IccFileHandler iccFileHandler = this.mPhone.getIccFileHandler();
        if (iccFileHandler == null) {
            if (Rlog.isLoggable("SMS", 3)) {
                log("markMessagesAsRead - aborting, no icc card present.");
                return;
            }
            return;
        }
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            byte[] bArr = arrayList.get(i);
            if (bArr[0] == 3) {
                int length = bArr.length - 1;
                byte[] bArr2 = new byte[length];
                System.arraycopy(bArr, 1, bArr2, 0, length);
                int i2 = i + 1;
                iccFileHandler.updateEFLinearFixed(IccConstants.EF_SMS, i2, makeSmsRecordData(1, bArr2), null, null);
                if (Rlog.isLoggable("SMS", 3)) {
                    log("SMS " + i2 + " marked as read");
                }
            }
        }
    }

    protected void updatePhoneObject(Phone phone) {
        this.mPhone = phone;
        this.mDispatchersController.updatePhoneObject(phone);
    }

    protected void enforceReceiveAndSend(String str) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", str);
        this.mContext.enforceCallingOrSelfPermission("android.permission.SEND_SMS", str);
    }

    public boolean updateMessageOnIccEf(String str, int i, int i2, byte[] bArr) {
        log("updateMessageOnIccEf: index=" + i + " status=" + i2 + " ==> (" + Arrays.toString(bArr) + ")");
        enforceReceiveAndSend("Updating message on Icc");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), str) != 0) {
            return false;
        }
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message messageObtainMessage = this.mHandler.obtainMessage(2);
            if (i2 == 0) {
                if (1 == this.mPhone.getPhoneType()) {
                    this.mPhone.mCi.deleteSmsOnSim(i, messageObtainMessage);
                } else {
                    this.mPhone.mCi.deleteSmsOnRuim(i, messageObtainMessage);
                }
            } else {
                IccFileHandler iccFileHandler = this.mPhone.getIccFileHandler();
                if (iccFileHandler == null) {
                    messageObtainMessage.recycle();
                    return this.mSuccess;
                }
                iccFileHandler.updateEFLinearFixed(IccConstants.EF_SMS, i, makeSmsRecordData(i2, bArr), null, messageObtainMessage);
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
            return this.mSuccess;
        }
    }

    public boolean copyMessageToIccEf(String str, int i, byte[] bArr, byte[] bArr2) {
        log("copyMessageToIccEf: status=" + i + " ==> pdu=(" + Arrays.toString(bArr) + "), smsc=(" + Arrays.toString(bArr2) + ")");
        enforceReceiveAndSend("Copying message to Icc");
        if (this.mAppOps.noteOp(22, Binder.getCallingUid(), str) != 0) {
            return false;
        }
        synchronized (this.mLock) {
            this.mSuccess = false;
            Message messageObtainMessage = this.mHandler.obtainMessage(2);
            if (1 == this.mPhone.getPhoneType()) {
                this.mPhone.mCi.writeSmsToSim(i, IccUtils.bytesToHexString(bArr2), IccUtils.bytesToHexString(bArr), messageObtainMessage);
            } else {
                this.mPhone.mCi.writeSmsToRuim(i, IccUtils.bytesToHexString(bArr), messageObtainMessage);
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to update by index");
            }
        }
        return this.mSuccess;
    }

    public List<SmsRawData> getAllMessagesFromIccEf(String str) {
        log("getAllMessagesFromEF");
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECEIVE_SMS", "Reading messages from Icc");
        if (this.mAppOps.noteOp(21, Binder.getCallingUid(), str) != 0) {
            return new ArrayList();
        }
        synchronized (this.mLock) {
            IccFileHandler iccFileHandler = this.mPhone.getIccFileHandler();
            if (iccFileHandler == null) {
                Rlog.e(LOG_TAG, "Cannot load Sms records. No icc card?");
                this.mSms = null;
                return this.mSms;
            }
            iccFileHandler.loadEFLinearFixedAll(IccConstants.EF_SMS, this.mHandler.obtainMessage(1));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to load from the Icc");
            }
            return this.mSms;
        }
    }

    public void sendDataWithSelfPermissions(String str, String str2, String str3, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (!checkCallingOrSelfSendSmsPermission(str, "Sending SMS message")) {
            return;
        }
        sendDataInternal(str2, str3, i, bArr, pendingIntent, pendingIntent2);
    }

    public void sendData(String str, String str2, String str3, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (!checkCallingSendSmsPermission(str, "Sending SMS message")) {
            return;
        }
        sendDataInternal(str2, str3, i, bArr, pendingIntent, pendingIntent2);
    }

    protected void sendDataInternal(String str, String str2, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (Rlog.isLoggable("SMS", 2)) {
            log("sendData: destAddr=" + str + " scAddr=" + str2 + " destPort=" + i + " data='" + HexDump.toHexString(bArr) + "' sentIntent=" + pendingIntent + " deliveryIntent=" + pendingIntent2);
        }
        this.mDispatchersController.sendData(filterDestAddress(str), str2, i, bArr, pendingIntent, pendingIntent2);
    }

    public void sendText(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        if (!checkCallingSendTextPermissions(z, str, "Sending SMS message")) {
            return;
        }
        sendTextInternal(str, str2, str3, str4, pendingIntent, pendingIntent2, z, -1, false, -1);
    }

    public void sendTextWithSelfPermissions(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        if (!checkCallingOrSelfSendSmsPermission(str, "Sending SMS message")) {
            return;
        }
        sendTextInternal(str, str2, str3, str4, pendingIntent, pendingIntent2, z, -1, false, -1);
    }

    protected void sendTextInternal(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, int i, boolean z2, int i2) {
        String str5;
        String str6;
        PendingIntent pendingIntent3;
        PendingIntent pendingIntent4;
        int i3;
        boolean z3;
        int i4;
        if (Rlog.isLoggable("SMS", 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("sendText: destAddr=");
            sb.append(str2);
            sb.append(" scAddr=");
            str5 = str3;
            sb.append(str5);
            sb.append(" text='");
            str6 = str4;
            sb.append(str6);
            sb.append("' sentIntent=");
            pendingIntent3 = pendingIntent;
            sb.append(pendingIntent3);
            sb.append(" deliveryIntent=");
            pendingIntent4 = pendingIntent2;
            sb.append(pendingIntent4);
            sb.append(" priority=");
            i3 = i;
            sb.append(i3);
            sb.append(" expectMore=");
            z3 = z2;
            sb.append(z3);
            sb.append(" validityPeriod=");
            i4 = i2;
            sb.append(i4);
            log(sb.toString());
        } else {
            str5 = str3;
            str6 = str4;
            pendingIntent3 = pendingIntent;
            pendingIntent4 = pendingIntent2;
            i3 = i;
            z3 = z2;
            i4 = i2;
        }
        this.mDispatchersController.sendText(filterDestAddress(str2), str5, str6, pendingIntent3, pendingIntent4, null, str, z, i3, z3, i4);
    }

    public void sendTextWithOptions(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, int i, boolean z2, int i2) {
        if (!checkCallingOrSelfSendSmsPermission(str, "Sending SMS message")) {
            return;
        }
        sendTextInternal(str, str2, str3, str4, pendingIntent, pendingIntent2, z, i, z2, i2);
    }

    public void injectSmsPdu(byte[] bArr, String str, final PendingIntent pendingIntent) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            enforceCallerIsImsAppOrCarrierApp("injectSmsPdu");
        }
        if (Rlog.isLoggable("SMS", 2)) {
            log("pdu: " + bArr + "\n format=" + str + "\n receivedIntent=" + pendingIntent);
        }
        this.mDispatchersController.injectSmsPdu(bArr, str, new SmsDispatchersController.SmsInjectionCallback() {
            @Override
            public final void onSmsInjectedResult(int i) {
                IccSmsInterfaceManager.lambda$injectSmsPdu$0(pendingIntent, i);
            }
        });
    }

    static void lambda$injectSmsPdu$0(PendingIntent pendingIntent, int i) {
        if (pendingIntent != null) {
            try {
                pendingIntent.send(i);
            } catch (PendingIntent.CanceledException e) {
                Rlog.d(LOG_TAG, "receivedIntent cancelled.");
            }
        }
    }

    public void sendMultipartText(String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) {
        sendMultipartTextWithOptions(str, str2, str3, list, list2, list3, z, -1, false, -1);
    }

    public void sendMultipartTextWithOptions(String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z, int i, boolean z2, int i2) {
        String strConcat;
        PendingIntent pendingIntent;
        String str4 = str;
        if (!checkCallingSendTextPermissions(z, str4, "Sending SMS message")) {
            return;
        }
        if (Rlog.isLoggable("SMS", 2)) {
            Iterator<String> it = list.iterator();
            int i3 = 0;
            while (it.hasNext()) {
                log("sendMultipartTextWithOptions: destAddr=" + str2 + ", srAddr=" + str3 + ", part[" + i3 + "]=" + it.next());
                i3++;
            }
        }
        String str5 = str3;
        String strFilterDestAddress = filterDestAddress(str2);
        if (list.size() > 1 && list.size() < 10 && !SmsMessage.hasEmsSupport()) {
            int i4 = 0;
            while (i4 < list.size()) {
                String str6 = list.get(i4);
                if (SmsMessage.shouldAppendPageNumberAsPrefix()) {
                    strConcat = String.valueOf(i4 + 1) + '/' + list.size() + ' ' + str6;
                } else {
                    strConcat = str6.concat(' ' + String.valueOf(i4 + 1) + '/' + list.size());
                }
                String str7 = strConcat;
                PendingIntent pendingIntent2 = null;
                if (list2 == null || list2.size() <= i4) {
                    pendingIntent = null;
                } else {
                    pendingIntent = list2.get(i4);
                }
                if (list3 != null && list3.size() > i4) {
                    pendingIntent2 = list3.get(i4);
                }
                this.mDispatchersController.sendText(strFilterDestAddress, str5, str7, pendingIntent, pendingIntent2, null, str4, z, i, z2, i2);
                i4++;
                str4 = str;
                str5 = str3;
            }
            return;
        }
        this.mDispatchersController.sendMultipartText(strFilterDestAddress, str3, (ArrayList) list, (ArrayList) list2, (ArrayList) list3, null, str, z, i, z2, i2);
    }

    public int getPremiumSmsPermission(String str) {
        return this.mDispatchersController.getPremiumSmsPermission(str);
    }

    public void setPremiumSmsPermission(String str, int i) {
        this.mDispatchersController.setPremiumSmsPermission(str, i);
    }

    protected ArrayList<SmsRawData> buildValidRawData(ArrayList<byte[]> arrayList) {
        int size = arrayList.size();
        ArrayList<SmsRawData> arrayList2 = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if (arrayList.get(i)[0] == 0) {
                arrayList2.add(null);
            } else {
                arrayList2.add(new SmsRawData(arrayList.get(i)));
            }
        }
        return arrayList2;
    }

    protected byte[] makeSmsRecordData(int i, byte[] bArr) {
        byte[] bArr2;
        if (1 == this.mPhone.getPhoneType()) {
            bArr2 = new byte[PduHeaders.ADDITIONAL_HEADERS];
        } else {
            bArr2 = new byte[255];
        }
        bArr2[0] = (byte) (i & 7);
        System.arraycopy(bArr, 0, bArr2, 1, bArr.length);
        for (int length = bArr.length + 1; length < bArr2.length; length++) {
            bArr2[length] = -1;
        }
        return bArr2;
    }

    public boolean enableCellBroadcast(int i, int i2) {
        return enableCellBroadcastRange(i, i, i2);
    }

    public boolean disableCellBroadcast(int i, int i2) {
        return disableCellBroadcastRange(i, i, i2);
    }

    public boolean enableCellBroadcastRange(int i, int i2, int i3) {
        if (i3 == 0) {
            return enableGsmBroadcastRange(i, i2);
        }
        if (i3 == 1) {
            return enableCdmaBroadcastRange(i, i2);
        }
        throw new IllegalArgumentException("Not a supportted RAN Type");
    }

    public boolean disableCellBroadcastRange(int i, int i2, int i3) {
        if (i3 == 0) {
            return disableGsmBroadcastRange(i, i2);
        }
        if (i3 == 1) {
            return disableCdmaBroadcastRange(i, i2);
        }
        throw new IllegalArgumentException("Not a supportted RAN Type");
    }

    public synchronized boolean enableGsmBroadcastRange(int i, int i2) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling cell broadcast SMS");
        String nameForUid = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (!this.mCellBroadcastRangeManager.enableRange(i, i2, nameForUid)) {
            log("Failed to add GSM cell broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
            return false;
        }
        log("Added GSM cell broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
        setCellBroadcastActivation(this.mCellBroadcastRangeManager.isEmpty() ^ true);
        return true;
    }

    public synchronized boolean disableGsmBroadcastRange(int i, int i2) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Disabling cell broadcast SMS");
        String nameForUid = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (!this.mCellBroadcastRangeManager.disableRange(i, i2, nameForUid)) {
            log("Failed to remove GSM cell broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
            return false;
        }
        log("Removed GSM cell broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
        setCellBroadcastActivation(this.mCellBroadcastRangeManager.isEmpty() ^ true);
        return true;
    }

    public synchronized boolean enableCdmaBroadcastRange(int i, int i2) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Enabling cdma broadcast SMS");
        String nameForUid = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (!this.mCdmaBroadcastRangeManager.enableRange(i, i2, nameForUid)) {
            log("Failed to add cdma broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
            return false;
        }
        log("Added cdma broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
        setCdmaBroadcastActivation(this.mCdmaBroadcastRangeManager.isEmpty() ^ true);
        return true;
    }

    public synchronized boolean disableCdmaBroadcastRange(int i, int i2) {
        this.mContext.enforceCallingPermission("android.permission.RECEIVE_SMS", "Disabling cell broadcast SMS");
        String nameForUid = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (!this.mCdmaBroadcastRangeManager.disableRange(i, i2, nameForUid)) {
            log("Failed to remove cdma broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
            return false;
        }
        log("Removed cdma broadcast subscription for MID range " + i + " to " + i2 + " from client " + nameForUid);
        setCdmaBroadcastActivation(this.mCdmaBroadcastRangeManager.isEmpty() ^ true);
        return true;
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
        protected void addRange(int i, int i2, boolean z) {
            this.mConfigList.add(new SmsBroadcastConfigInfo(i, i2, 0, 255, z));
        }

        @Override
        protected boolean finishUpdate() {
            if (this.mConfigList.isEmpty()) {
                return IccSmsInterfaceManager.this.setCellBroadcastConfig(new SmsBroadcastConfigInfo[0]);
            }
            return IccSmsInterfaceManager.this.setCellBroadcastConfig((SmsBroadcastConfigInfo[]) this.mConfigList.toArray(new SmsBroadcastConfigInfo[this.mConfigList.size()]));
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
        protected void addRange(int i, int i2, boolean z) {
            this.mConfigList.add(new CdmaSmsBroadcastConfigInfo(i, i2, 1, z));
        }

        @Override
        protected boolean finishUpdate() {
            if (this.mConfigList.isEmpty()) {
                return true;
            }
            return IccSmsInterfaceManager.this.setCdmaBroadcastConfig((CdmaSmsBroadcastConfigInfo[]) this.mConfigList.toArray(new CdmaSmsBroadcastConfigInfo[this.mConfigList.size()]));
        }
    }

    private boolean setCellBroadcastConfig(SmsBroadcastConfigInfo[] smsBroadcastConfigInfoArr) {
        log("Calling setGsmBroadcastConfig with " + smsBroadcastConfigInfoArr.length + " configurations");
        synchronized (this.mLock) {
            Message messageObtainMessage = this.mHandler.obtainMessage(4);
            this.mSuccess = false;
            this.mPhone.mCi.setGsmBroadcastConfig(smsBroadcastConfigInfoArr, messageObtainMessage);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast config");
            }
        }
        return this.mSuccess;
    }

    protected boolean setCellBroadcastActivation(boolean z) {
        log("Calling setCellBroadcastActivation(" + z + ')');
        synchronized (this.mLock) {
            Message messageObtainMessage = this.mHandler.obtainMessage(3);
            this.mSuccess = false;
            this.mPhone.mCi.setGsmBroadcastActivation(z, messageObtainMessage);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cell broadcast activation");
            }
        }
        return this.mSuccess;
    }

    private boolean setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] cdmaSmsBroadcastConfigInfoArr) {
        log("Calling setCdmaBroadcastConfig with " + cdmaSmsBroadcastConfigInfoArr.length + " configurations");
        synchronized (this.mLock) {
            Message messageObtainMessage = this.mHandler.obtainMessage(4);
            this.mSuccess = false;
            this.mPhone.mCi.setCdmaBroadcastConfig(cdmaSmsBroadcastConfigInfoArr, messageObtainMessage);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cdma broadcast config");
            }
        }
        return this.mSuccess;
    }

    private boolean setCdmaBroadcastActivation(boolean z) {
        log("Calling setCdmaBroadcastActivation(" + z + ")");
        synchronized (this.mLock) {
            Message messageObtainMessage = this.mHandler.obtainMessage(3);
            this.mSuccess = false;
            this.mPhone.mCi.setCdmaBroadcastActivation(z, messageObtainMessage);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                log("interrupted while trying to set cdma broadcast activation");
            }
        }
        return this.mSuccess;
    }

    protected void log(String str) {
        Log.d(LOG_TAG, "[IccSmsInterfaceManager] " + str);
    }

    public boolean isImsSmsSupported() {
        return this.mDispatchersController.isIms();
    }

    public String getImsSmsFormat() {
        return this.mDispatchersController.getImsSmsFormat();
    }

    public void sendStoredText(String str, Uri uri, String str2, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws Throwable {
        String str3;
        PendingIntent pendingIntent3;
        if (!checkCallingSendSmsPermission(str, "Sending SMS message")) {
            return;
        }
        if (Rlog.isLoggable("SMS", 2)) {
            StringBuilder sb = new StringBuilder();
            sb.append("sendStoredText: scAddr=");
            str3 = str2;
            sb.append(str3);
            sb.append(" messageUri=");
            sb.append(uri);
            sb.append(" sentIntent=");
            sb.append(pendingIntent);
            sb.append(" deliveryIntent=");
            pendingIntent3 = pendingIntent2;
            sb.append(pendingIntent3);
            log(sb.toString());
        } else {
            str3 = str2;
            pendingIntent3 = pendingIntent2;
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (!isFailedOrDraft(contentResolver, uri)) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(pendingIntent);
            return;
        }
        String[] strArrLoadTextAndAddress = loadTextAndAddress(contentResolver, uri);
        if (strArrLoadTextAndAddress == null) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredText: can not load text");
            returnUnspecifiedFailure(pendingIntent);
        } else {
            strArrLoadTextAndAddress[1] = filterDestAddress(strArrLoadTextAndAddress[1]);
            this.mDispatchersController.sendText(strArrLoadTextAndAddress[1], str3, strArrLoadTextAndAddress[0], pendingIntent, pendingIntent3, uri, str, true, -1, false, -1);
        }
    }

    public void sendStoredMultipartText(String str, Uri uri, String str2, List<PendingIntent> list, List<PendingIntent> list2) throws Throwable {
        String strConcat;
        PendingIntent pendingIntent;
        String str3 = str;
        if (!checkCallingSendSmsPermission(str3, "Sending SMS message")) {
            return;
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (!isFailedOrDraft(contentResolver, uri)) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: not FAILED or DRAFT message");
            returnUnspecifiedFailure(list);
            return;
        }
        String[] strArrLoadTextAndAddress = loadTextAndAddress(contentResolver, uri);
        if (strArrLoadTextAndAddress == null) {
            Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: can not load text");
            returnUnspecifiedFailure(list);
            return;
        }
        ArrayList<String> arrayListDivideMessage = SmsManager.getDefault().divideMessage(strArrLoadTextAndAddress[0]);
        if (arrayListDivideMessage != null) {
            char c = 1;
            if (arrayListDivideMessage.size() >= 1) {
                strArrLoadTextAndAddress[1] = filterDestAddress(strArrLoadTextAndAddress[1]);
                if (arrayListDivideMessage.size() <= 1 || arrayListDivideMessage.size() >= 10 || SmsMessage.hasEmsSupport()) {
                    this.mDispatchersController.sendMultipartText(strArrLoadTextAndAddress[1], str2, arrayListDivideMessage, (ArrayList) list, (ArrayList) list2, uri, str, true, -1, false, -1);
                    return;
                }
                int i = 0;
                while (i < arrayListDivideMessage.size()) {
                    String str4 = arrayListDivideMessage.get(i);
                    if (SmsMessage.shouldAppendPageNumberAsPrefix()) {
                        strConcat = String.valueOf(i + 1) + '/' + arrayListDivideMessage.size() + ' ' + str4;
                    } else {
                        strConcat = str4.concat(' ' + String.valueOf(i + 1) + '/' + arrayListDivideMessage.size());
                    }
                    String str5 = strConcat;
                    PendingIntent pendingIntent2 = null;
                    if (list == null || list.size() <= i) {
                        pendingIntent = null;
                    } else {
                        pendingIntent = list.get(i);
                    }
                    if (list2 != null && list2.size() > i) {
                        pendingIntent2 = list2.get(i);
                    }
                    this.mDispatchersController.sendText(strArrLoadTextAndAddress[c], str2, str5, pendingIntent, pendingIntent2, uri, str3, true, -1, false, -1);
                    i++;
                    str3 = str;
                    c = c;
                    arrayListDivideMessage = arrayListDivideMessage;
                }
                return;
            }
        }
        Log.e(LOG_TAG, "[IccSmsInterfaceManager]sendStoredMultipartText: can not divide text");
        returnUnspecifiedFailure(list);
    }

    private boolean isFailedOrDraft(ContentResolver contentResolver, Uri uri) throws Throwable {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            try {
                Cursor cursorQuery = contentResolver.query(uri, new String[]{"type"}, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            int i = cursorQuery.getInt(0);
                            boolean z = i == 3 || i == 5;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return z;
                        }
                    } catch (SQLiteException e) {
                        e = e;
                        cursor = cursorQuery;
                        Log.e(LOG_TAG, "[IccSmsInterfaceManager]isFailedOrDraft: query message type failed", e);
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (SQLiteException e2) {
            e = e2;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        return false;
    }

    private String[] loadTextAndAddress(ContentResolver contentResolver, Uri uri) throws Throwable {
        Cursor cursorQuery;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            cursorQuery = contentResolver.query(uri, new String[]{"body", "address"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            String[] strArr = {cursorQuery.getString(0), cursorQuery.getString(1)};
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return strArr;
                        }
                    } catch (SQLiteException e) {
                        e = e;
                        Log.e(LOG_TAG, "[IccSmsInterfaceManager]loadText: query message text failed", e);
                        if (cursorQuery != null) {
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        } catch (SQLiteException e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
            if (cursorQuery != null) {
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        return null;
    }

    private void returnUnspecifiedFailure(PendingIntent pendingIntent) {
        if (pendingIntent != null) {
            try {
                pendingIntent.send(1);
            } catch (PendingIntent.CanceledException e) {
            }
        }
    }

    private void returnUnspecifiedFailure(List<PendingIntent> list) {
        if (list == null) {
            return;
        }
        Iterator<PendingIntent> it = list.iterator();
        while (it.hasNext()) {
            returnUnspecifiedFailure(it.next());
        }
    }

    @VisibleForTesting
    public boolean checkCallingSendTextPermissions(boolean z, String str, String str2) {
        if (!z) {
            try {
                enforceCallerIsImsAppOrCarrierApp(str2);
                return true;
            } catch (SecurityException e) {
                this.mContext.enforceCallingPermission("android.permission.MODIFY_PHONE_STATE", str2);
            }
        }
        return checkCallingSendSmsPermission(str, str2);
    }

    private boolean checkCallingOrSelfSendSmsPermission(String str, String str2) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SEND_SMS", str2);
        return this.mAppOps.noteOp(20, Binder.getCallingUid(), str) == 0;
    }

    private boolean checkCallingSendSmsPermission(String str, String str2) {
        this.mContext.enforceCallingPermission("android.permission.SEND_SMS", str2);
        return this.mAppOps.noteOp(20, Binder.getCallingUid(), str) == 0;
    }

    @VisibleForTesting
    public void enforceCallerIsImsAppOrCarrierApp(String str) {
        int callingUid = Binder.getCallingUid();
        String carrierImsPackageForIntent = CarrierSmsUtils.getCarrierImsPackageForIntent(this.mContext, this.mPhone, new Intent("android.service.carrier.CarrierMessagingService"));
        if (carrierImsPackageForIntent != null) {
            try {
                if (callingUid == this.mContext.getPackageManager().getPackageUid(carrierImsPackageForIntent, 0)) {
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                if (Rlog.isLoggable("SMS", 3)) {
                    log("Cannot find configured carrier ims package");
                }
            }
        }
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(this.mPhone.getSubId(), str);
    }

    private String filterDestAddress(String str) {
        String strFilterDestAddr = SmsNumberUtils.filterDestAddr(this.mPhone, str);
        return strFilterDestAddr != null ? strFilterDestAddr : str;
    }
}
