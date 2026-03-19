package com.android.internal.telephony;

import android.R;
import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.IWapPushManager;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import java.util.HashMap;

public class WapPushOverSms implements ServiceConnection {
    private static final boolean DBG = false;
    private static final String LOCATION_SELECTION = "m_type=? AND ct_l =?";
    private static final String TAG = "WAP PUSH";
    private static final String THREAD_ID_SELECTION = "m_id=? AND m_type=?";
    protected final Context mContext;
    protected volatile IWapPushManager mWapPushManager;
    protected String mWapPushManagerPackage;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Rlog.d(WapPushOverSms.TAG, "Received broadcast " + intent.getAction());
            if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                new BindServiceThread(WapPushOverSms.this.mContext).start();
            }
        }
    };
    protected IDeviceIdleController mDeviceIdleController = TelephonyComponentFactory.getInstance().getIDeviceIdleController();

    private class BindServiceThread extends Thread {
        private final Context context;

        private BindServiceThread(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            WapPushOverSms.this.bindWapPushManagerService(this.context);
        }
    }

    private void bindWapPushManagerService(Context context) {
        Intent intent = new Intent(IWapPushManager.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(context.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !context.bindService(intent, this, 1)) {
            Rlog.e(TAG, "bindService() for wappush manager failed");
        } else {
            synchronized (this) {
                this.mWapPushManagerPackage = componentNameResolveSystemService.getPackageName();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.mWapPushManager = IWapPushManager.Stub.asInterface(iBinder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        this.mWapPushManager = null;
    }

    public WapPushOverSms(Context context) {
        this.mContext = context;
        if (((UserManager) this.mContext.getSystemService("user")).isUserUnlocked()) {
            bindWapPushManagerService(this.mContext);
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    public void dispose() {
        if (this.mWapPushManager != null) {
            this.mContext.unbindService(this);
        } else {
            Rlog.e(TAG, "dispose: not bound to a wappush manager");
        }
    }

    private DecodedResult decodeWapPdu(byte[] bArr, InboundSmsHandler inboundSmsHandler) {
        int i;
        byte[] bArr2;
        byte[] bArr3;
        GenericPdu genericPdu;
        String string;
        DecodedResult decodedResult = new DecodedResult();
        try {
            int i2 = bArr[0] & 255;
            int i3 = bArr[1] & 255;
            int phoneId = inboundSmsHandler.getPhone().getPhoneId();
            if (i3 != 6 && i3 != 7) {
                int integer = this.mContext.getResources().getInteger(R.integer.config_extraFreeKbytesAdjust);
                if (integer != -1) {
                    int i4 = integer + 1;
                    i2 = bArr[integer] & 255;
                    i = i4 + 1;
                    i3 = bArr[i4] & 255;
                    if (i3 != 6 && i3 != 7) {
                        decodedResult.statusCode = 1;
                        return decodedResult;
                    }
                } else {
                    decodedResult.statusCode = 1;
                    return decodedResult;
                }
            } else {
                i = 2;
            }
            WspTypeDecoder wspTypeDecoderMakeWspTypeDecoder = TelephonyComponentFactory.getInstance().makeWspTypeDecoder(bArr);
            if (!wspTypeDecoderMakeWspTypeDecoder.decodeUintvarInteger(i)) {
                decodedResult.statusCode = 2;
                return decodedResult;
            }
            int value32 = (int) wspTypeDecoderMakeWspTypeDecoder.getValue32();
            int decodedDataLength = i + wspTypeDecoderMakeWspTypeDecoder.getDecodedDataLength();
            if (!wspTypeDecoderMakeWspTypeDecoder.decodeContentType(decodedDataLength)) {
                decodedResult.statusCode = 2;
                return decodedResult;
            }
            String valueString = wspTypeDecoderMakeWspTypeDecoder.getValueString();
            int i5 = i3;
            long value322 = wspTypeDecoderMakeWspTypeDecoder.getValue32();
            int decodedDataLength2 = wspTypeDecoderMakeWspTypeDecoder.getDecodedDataLength() + decodedDataLength;
            byte[] bArr4 = new byte[value32];
            System.arraycopy(bArr, decodedDataLength, bArr4, 0, bArr4.length);
            if (valueString == null || !valueString.equals(WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO)) {
                int i6 = decodedDataLength + value32;
                bArr2 = new byte[bArr.length - i6];
                System.arraycopy(bArr, i6, bArr2, 0, bArr2.length);
            } else {
                bArr2 = bArr;
            }
            int[] subId = SubscriptionManager.getSubId(phoneId);
            int defaultSmsSubscriptionId = (subId == null || subId.length <= 0) ? SmsManager.getDefaultSmsSubscriptionId() : subId[0];
            try {
                genericPdu = new PduParser(bArr2, shouldParseContentDisposition(defaultSmsSubscriptionId)).parse();
                bArr3 = bArr2;
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                bArr3 = bArr2;
                sb.append("Unable to parse PDU: ");
                sb.append(e.toString());
                Rlog.e(TAG, sb.toString());
                genericPdu = null;
            }
            if (genericPdu != null && genericPdu.getMessageType() == 130) {
                NotificationInd notificationInd = (NotificationInd) genericPdu;
                if (notificationInd.getFrom() != null && BlockChecker.isBlocked(this.mContext, notificationInd.getFrom().getString(), null)) {
                    decodedResult.statusCode = 1;
                    return decodedResult;
                }
            }
            if (wspTypeDecoderMakeWspTypeDecoder.seekXWapApplicationId(decodedDataLength2, (value32 + decodedDataLength2) - 1)) {
                wspTypeDecoderMakeWspTypeDecoder.decodeXWapApplicationId((int) wspTypeDecoderMakeWspTypeDecoder.getValue32());
                String valueString2 = wspTypeDecoderMakeWspTypeDecoder.getValueString();
                if (valueString2 == null) {
                    valueString2 = Integer.toString((int) wspTypeDecoderMakeWspTypeDecoder.getValue32());
                }
                decodedResult.wapAppId = valueString2;
                if (valueString == null) {
                    string = Long.toString(value322);
                } else {
                    string = valueString;
                }
                decodedResult.contentType = string;
            }
            decodedResult.subId = defaultSmsSubscriptionId;
            decodedResult.phoneId = phoneId;
            decodedResult.parsedPdu = genericPdu;
            decodedResult.mimeType = valueString;
            decodedResult.transactionId = i2;
            decodedResult.pduType = i5;
            decodedResult.header = bArr4;
            decodedResult.intentData = bArr3;
            decodedResult.contentTypeParameters = wspTypeDecoderMakeWspTypeDecoder.getContentParameters();
            decodedResult.statusCode = -1;
        } catch (ArrayIndexOutOfBoundsException e2) {
            Rlog.e(TAG, "ignoring dispatchWapPdu() array index exception: " + e2);
            decodedResult.statusCode = 2;
        }
        return decodedResult;
    }

    public int dispatchWapPdu(byte[] bArr, BroadcastReceiver broadcastReceiver, InboundSmsHandler inboundSmsHandler) throws Throwable {
        boolean z;
        Bundle bundle;
        DecodedResult decodedResultDecodeWapPdu = decodeWapPdu(bArr, inboundSmsHandler);
        if (decodedResultDecodeWapPdu.statusCode != -1) {
            return decodedResultDecodeWapPdu.statusCode;
        }
        if (SmsManager.getDefault().getAutoPersisting()) {
            writeInboxMessage(decodedResultDecodeWapPdu.subId, decodedResultDecodeWapPdu.parsedPdu);
        }
        if (decodedResultDecodeWapPdu.wapAppId != null) {
            try {
                IWapPushManager iWapPushManager = this.mWapPushManager;
                if (iWapPushManager != null) {
                    synchronized (this) {
                        this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(this.mWapPushManagerPackage, 0, "mms-mgr");
                    }
                    Intent intent = new Intent();
                    intent.putExtra("transactionId", decodedResultDecodeWapPdu.transactionId);
                    intent.putExtra("pduType", decodedResultDecodeWapPdu.pduType);
                    intent.putExtra("header", decodedResultDecodeWapPdu.header);
                    intent.putExtra("data", decodedResultDecodeWapPdu.intentData);
                    intent.putExtra("contentTypeParameters", decodedResultDecodeWapPdu.contentTypeParameters);
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent, decodedResultDecodeWapPdu.phoneId);
                    int iProcessMessage = iWapPushManager.processMessage(decodedResultDecodeWapPdu.wapAppId, decodedResultDecodeWapPdu.contentType, intent);
                    if ((iProcessMessage & 1) > 0 && (iProcessMessage & 32768) == 0) {
                        z = false;
                    }
                    if (!z) {
                        return 1;
                    }
                }
                z = true;
                if (!z) {
                }
            } catch (RemoteException e) {
            }
        }
        if (decodedResultDecodeWapPdu.mimeType == null) {
            return 2;
        }
        Intent intent2 = new Intent("android.provider.Telephony.WAP_PUSH_DELIVER");
        intent2.setType(decodedResultDecodeWapPdu.mimeType);
        intent2.putExtra("transactionId", decodedResultDecodeWapPdu.transactionId);
        intent2.putExtra("pduType", decodedResultDecodeWapPdu.pduType);
        intent2.putExtra("header", decodedResultDecodeWapPdu.header);
        intent2.putExtra("data", decodedResultDecodeWapPdu.intentData);
        intent2.putExtra("contentTypeParameters", decodedResultDecodeWapPdu.contentTypeParameters);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, decodedResultDecodeWapPdu.phoneId);
        ComponentName defaultMmsApplication = SmsApplication.getDefaultMmsApplication(this.mContext, true);
        if (defaultMmsApplication != null) {
            intent2.setComponent(defaultMmsApplication);
            try {
                long jAddPowerSaveTempWhitelistAppForMms = this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(defaultMmsApplication.getPackageName(), 0, "mms-app");
                BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
                broadcastOptionsMakeBasic.setTemporaryAppWhitelistDuration(jAddPowerSaveTempWhitelistAppForMms);
                bundle = broadcastOptionsMakeBasic.toBundle();
            } catch (RemoteException e2) {
                bundle = null;
            }
        } else {
            bundle = null;
        }
        inboundSmsHandler.dispatchIntent(intent2, getPermissionForType(decodedResultDecodeWapPdu.mimeType), getAppOpsPermissionForIntent(decodedResultDecodeWapPdu.mimeType), bundle, broadcastReceiver, UserHandle.SYSTEM);
        return -1;
    }

    public boolean isWapPushForMms(byte[] bArr, InboundSmsHandler inboundSmsHandler) {
        DecodedResult decodedResultDecodeWapPdu = decodeWapPdu(bArr, inboundSmsHandler);
        return decodedResultDecodeWapPdu.statusCode == -1 && "application/vnd.wap.mms-message".equals(decodedResultDecodeWapPdu.mimeType);
    }

    protected static boolean shouldParseContentDisposition(int i) {
        return SmsManager.getSmsManagerForSubscriptionId(i).getCarrierConfigValues().getBoolean("supportMmsContentDisposition", true);
    }

    protected void writeInboxMessage(int i, GenericPdu genericPdu) throws Throwable {
        if (genericPdu == null) {
            Rlog.e(TAG, "Invalid PUSH PDU");
        }
        PduPersister pduPersister = PduPersister.getPduPersister(this.mContext);
        int messageType = genericPdu.getMessageType();
        try {
            if (messageType != 130) {
                if (messageType == 134 || messageType == 136) {
                    long deliveryOrReadReportThreadId = getDeliveryOrReadReportThreadId(this.mContext, genericPdu);
                    if (deliveryOrReadReportThreadId != -1) {
                        Uri uriPersist = pduPersister.persist(genericPdu, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
                        if (uriPersist == null) {
                            Rlog.e(TAG, "Failed to persist delivery or read report");
                            return;
                        }
                        ContentValues contentValues = new ContentValues(1);
                        contentValues.put("thread_id", Long.valueOf(deliveryOrReadReportThreadId));
                        if (SqliteWrapper.update(this.mContext, this.mContext.getContentResolver(), uriPersist, contentValues, (String) null, (String[]) null) != 1) {
                            Rlog.e(TAG, "Failed to update delivery or read report thread id");
                            return;
                        }
                        return;
                    }
                    Rlog.e(TAG, "Failed to find delivery or read report's thread id");
                    return;
                }
                Log.e(TAG, "Received unrecognized WAP Push PDU.");
                return;
            }
            NotificationInd notificationInd = (NotificationInd) genericPdu;
            Bundle carrierConfigValues = SmsManager.getSmsManagerForSubscriptionId(i).getCarrierConfigValues();
            if (carrierConfigValues != null && carrierConfigValues.getBoolean("enabledTransID", false)) {
                byte[] contentLocation = notificationInd.getContentLocation();
                if (61 == contentLocation[contentLocation.length - 1]) {
                    byte[] transactionId = notificationInd.getTransactionId();
                    byte[] bArr = new byte[contentLocation.length + transactionId.length];
                    System.arraycopy(contentLocation, 0, bArr, 0, contentLocation.length);
                    System.arraycopy(transactionId, 0, bArr, contentLocation.length, transactionId.length);
                    notificationInd.setContentLocation(bArr);
                }
            }
            if (!isDuplicateNotification(this.mContext, notificationInd)) {
                if (pduPersister.persist(genericPdu, Telephony.Mms.Inbox.CONTENT_URI, true, true, null) == null) {
                    Rlog.e(TAG, "Failed to save MMS WAP push notification ind");
                }
            } else {
                Rlog.d(TAG, "Skip storing duplicate MMS WAP push notification ind: " + new String(notificationInd.getContentLocation()));
            }
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save MMS WAP push data: type=" + messageType, e);
        } catch (RuntimeException e2) {
            Log.e(TAG, "Unexpected RuntimeException in persisting MMS WAP push data", e2);
        }
    }

    private static long getDeliveryOrReadReportThreadId(Context context, GenericPdu genericPdu) throws Throwable {
        String str;
        if (genericPdu instanceof DeliveryInd) {
            str = new String(((DeliveryInd) genericPdu).getMessageId());
        } else {
            if (!(genericPdu instanceof ReadOrigInd)) {
                Rlog.e(TAG, "WAP Push data is neither delivery or read report type: " + genericPdu.getClass().getCanonicalName());
                return -1L;
            }
            str = new String(((ReadOrigInd) genericPdu).getMessageId());
        }
        Cursor cursor = null;
        cursor = null;
        cursor = null;
        try {
            try {
                Cursor cursorQuery = SqliteWrapper.query(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, new String[]{"thread_id"}, THREAD_ID_SELECTION, new String[]{DatabaseUtils.sqlEscapeString(str), Integer.toString(128)}, (String) null);
                if (cursorQuery != null) {
                    try {
                        boolean zMoveToFirst = cursorQuery.moveToFirst();
                        cursor = zMoveToFirst;
                        if (zMoveToFirst) {
                            long j = cursorQuery.getLong(0);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return j;
                        }
                    } catch (SQLiteException e) {
                        cursor = cursorQuery;
                        e = e;
                        Rlog.e(TAG, "Failed to query delivery or read report thread id", e);
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Throwable th) {
                        cursor = cursorQuery;
                        th = th;
                        if (cursor != null) {
                            cursor.close();
                        }
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
        return -1L;
    }

    private static boolean isDuplicateNotification(Context context, NotificationInd notificationInd) throws Throwable {
        byte[] contentLocation = notificationInd.getContentLocation();
        if (contentLocation != null) {
            String[] strArr = {new String(contentLocation)};
            Cursor cursor = null;
            try {
                try {
                    Cursor cursorQuery = SqliteWrapper.query(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, new String[]{HbpcdLookup.ID}, LOCATION_SELECTION, new String[]{Integer.toString(130), new String(contentLocation)}, (String) null);
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.getCount() > 0) {
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                return true;
                            }
                        } catch (SQLiteException e) {
                            e = e;
                            cursor = cursorQuery;
                            Rlog.e(TAG, "failed to query existing notification ind", e);
                            if (cursor != null) {
                                cursor.close();
                            }
                        } catch (Throwable th) {
                            th = th;
                            cursor = cursorQuery;
                            if (cursor != null) {
                                cursor.close();
                            }
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
        }
        return false;
    }

    public static String getPermissionForType(String str) {
        if ("application/vnd.wap.mms-message".equals(str)) {
            return "android.permission.RECEIVE_MMS";
        }
        return "android.permission.RECEIVE_WAP_PUSH";
    }

    public static int getAppOpsPermissionForIntent(String str) {
        if ("application/vnd.wap.mms-message".equals(str)) {
            return 18;
        }
        return 19;
    }

    private final class DecodedResult {
        String contentType;
        HashMap<String, String> contentTypeParameters;
        byte[] header;
        byte[] intentData;
        String mimeType;
        GenericPdu parsedPdu;
        int pduType;
        int phoneId;
        int statusCode;
        int subId;
        int transactionId;
        String wapAppId;

        private DecodedResult() {
        }
    }
}
