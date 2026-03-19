package com.android.internal.telephony;

import android.R;
import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.IWapPushManager;
import com.android.internal.telephony.uicc.IccUtils;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.DeliveryInd;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.ReadOrigInd;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.HashMap;

public class WapPushOverSms implements ServiceConnection {
    private static final boolean DBG = true;
    private static final String LOCATION_SELECTION = "m_type=? AND ct_l =?";
    private static final String TAG = "WAP PUSH";
    private static final String THREAD_ID_SELECTION = "m_id=? AND m_type=?";
    private Bundle bundle;
    private final Context mContext;
    private IDeviceIdleController mDeviceIdleController = TelephonyComponentFactory.getInstance().getIDeviceIdleController();
    private volatile IWapPushManager mWapPushManager;
    private String mWapPushManagerPackage;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        this.mWapPushManager = IWapPushManager.Stub.asInterface(service);
        Rlog.v(TAG, "wappush manager connected to " + hashCode());
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        this.mWapPushManager = null;
        Rlog.v(TAG, "wappush manager disconnected.");
    }

    public WapPushOverSms(Context context) {
        this.mContext = context;
        Intent intent = new Intent(IWapPushManager.class.getName());
        ComponentName comp = intent.resolveSystemService(context.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !context.bindService(intent, this, 1)) {
            Rlog.e(TAG, "bindService() for wappush manager failed");
        } else {
            this.mWapPushManagerPackage = comp.getPackageName();
            Rlog.v(TAG, "bindService() for wappush manager succeeded");
        }
    }

    public void dispose() {
        if (this.mWapPushManager != null) {
            Rlog.v(TAG, "dispose: unbind wappush manager");
            this.mContext.unbindService(this);
        } else {
            Rlog.e(TAG, "dispose: not bound to a wappush manager");
        }
    }

    private DecodedResult decodeWapPdu(byte[] pdu, InboundSmsHandler handler) {
        int transactionId;
        int index;
        int pduType;
        int phoneId;
        int index2;
        byte[] intentData;
        DecodedResult result = new DecodedResult(this, null);
        Rlog.d(TAG, "Rx: " + IccUtils.bytesToHexString(pdu));
        try {
            transactionId = pdu[0] & PplMessageManager.Type.INVALID;
            index = 1 + 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            aie = e;
            Rlog.e(TAG, "ignoring dispatchWapPdu() array index exception: " + aie);
            result.statusCode = 2;
        }
        try {
            pduType = pdu[1] & PplMessageManager.Type.INVALID;
            phoneId = handler.getPhone().getPhoneId();
        } catch (ArrayIndexOutOfBoundsException e2) {
            aie = e2;
            Rlog.e(TAG, "ignoring dispatchWapPdu() array index exception: " + aie);
            result.statusCode = 2;
        }
        if (pduType != 6 && pduType != 7) {
            int index3 = this.mContext.getResources().getInteger(R.integer.config_downloadDataDirLowSpaceThreshold);
            if (index3 != -1) {
                index = index3 + 1;
                transactionId = pdu[index3] & PplMessageManager.Type.INVALID;
                index2 = index + 1;
                pduType = pdu[index] & PplMessageManager.Type.INVALID;
                Rlog.d(TAG, "index = " + index2 + " PDU Type = " + pduType + " transactionID = " + transactionId);
                if (pduType != 6 && pduType != 7) {
                    Rlog.w(TAG, "Received non-PUSH WAP PDU. Type = " + pduType);
                    result.statusCode = 1;
                    return result;
                }
            } else {
                Rlog.w(TAG, "Received non-PUSH WAP PDU. Type = " + pduType);
                result.statusCode = 1;
                return result;
            }
            return result;
        }
        index2 = index;
        WspTypeDecoder pduDecoder = TelephonyComponentFactory.getInstance().makeWspTypeDecoder(pdu);
        if (!pduDecoder.decodeUintvarInteger(index2)) {
            Rlog.w(TAG, "Received PDU. Header Length error.");
            result.statusCode = 2;
            return result;
        }
        int headerLength = (int) pduDecoder.getValue32();
        int index4 = index2 + pduDecoder.getDecodedDataLength();
        if (!pduDecoder.decodeContentType(index4)) {
            Rlog.w(TAG, "Received PDU. Header Content-Type error.");
            result.statusCode = 2;
            return result;
        }
        String mimeType = pduDecoder.getValueString();
        long binaryContentType = pduDecoder.getValue32();
        int index5 = index4 + pduDecoder.getDecodedDataLength();
        byte[] header = new byte[headerLength];
        System.arraycopy(pdu, index4, header, 0, header.length);
        pduDecoder.decodeHeaders(index5, (headerLength - index5) + index4);
        if (mimeType != null && mimeType.equals(WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO)) {
            intentData = pdu;
        } else {
            int dataIndex = index4 + headerLength;
            intentData = new byte[pdu.length - dataIndex];
            System.arraycopy(pdu, dataIndex, intentData, 0, intentData.length);
        }
        int[] subIds = SubscriptionManager.getSubId(phoneId);
        int subId = (subIds == null || subIds.length <= 0) ? SmsManager.getDefaultSmsSubscriptionId() : subIds[0];
        GenericPdu parsedPdu = null;
        try {
            parsedPdu = new PduParser(intentData, shouldParseContentDisposition(subId)).parse();
        } catch (Exception e3) {
            Rlog.e(TAG, "Unable to parse PDU: " + e3.toString());
        }
        if (parsedPdu != null && parsedPdu.getMessageType() == 130) {
            NotificationInd nInd = (NotificationInd) parsedPdu;
            if (nInd.getFrom() != null && BlockChecker.isBlocked(this.mContext, nInd.getFrom().getString())) {
                result.statusCode = 1;
                return result;
            }
        }
        if (pduDecoder.seekXWapApplicationId(index5, (index5 + headerLength) - 1)) {
            pduDecoder.decodeXWapApplicationId((int) pduDecoder.getValue32());
            String wapAppId = pduDecoder.getValueString();
            if (wapAppId == null) {
                wapAppId = Integer.toString((int) pduDecoder.getValue32());
            }
            result.wapAppId = wapAppId;
            String contentType = mimeType == null ? Long.toString(binaryContentType) : mimeType;
            result.contentType = contentType;
            Rlog.v(TAG, "appid found: " + wapAppId + ":" + contentType);
        }
        result.subId = subId;
        result.phoneId = phoneId;
        result.parsedPdu = parsedPdu;
        result.mimeType = mimeType;
        result.transactionId = transactionId;
        result.pduType = pduType;
        result.header = header;
        result.intentData = intentData;
        result.contentTypeParameters = pduDecoder.getContentParameters();
        result.statusCode = -1;
        result.headerList = pduDecoder.getHeaders();
        return result;
    }

    public int dispatchWapPdu(byte[] pdu, BroadcastReceiver receiver, InboundSmsHandler handler) {
        DecodedResult result = decodeWapPdu(pdu, handler);
        if (result.statusCode != -1) {
            return result.statusCode;
        }
        if (SmsManager.getDefault().getAutoPersisting()) {
            writeInboxMessage(result.subId, result.parsedPdu);
        }
        if (result.wapAppId != null) {
            boolean processFurther = true;
            try {
                IWapPushManager wapPushMan = this.mWapPushManager;
                if (wapPushMan == null) {
                    Rlog.w(TAG, "wap push manager not found!");
                } else {
                    this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(this.mWapPushManagerPackage, 0, "mms-mgr");
                    Intent intent = new Intent();
                    intent.putExtra("transactionId", result.transactionId);
                    intent.putExtra("pduType", result.pduType);
                    intent.putExtra("header", result.header);
                    intent.putExtra("data", result.intentData);
                    intent.putExtra("contentTypeParameters", result.contentTypeParameters);
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent, result.phoneId);
                    intent.putExtra("wspHeaders", result.headerList);
                    if (this.bundle != null) {
                        Rlog.d(TAG, "put addr info into intent 1");
                        intent.putExtra("address", this.bundle.getString("address"));
                        intent.putExtra("service_center", this.bundle.getString("service_center"));
                    }
                    int procRet = wapPushMan.processMessage(result.wapAppId, result.contentType, intent);
                    Rlog.v(TAG, "procRet:" + procRet);
                    if ((procRet & 1) > 0 && (32768 & procRet) == 0) {
                        processFurther = false;
                    }
                }
                if (!processFurther) {
                    return 1;
                }
            } catch (RemoteException e) {
                Rlog.w(TAG, "remote func failed...");
            }
        }
        Rlog.v(TAG, "fall back to existing handler");
        if (result.mimeType == null) {
            Rlog.w(TAG, "Header Content-Type error.");
            return 2;
        }
        Intent intent2 = new Intent(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION);
        intent2.setType(result.mimeType);
        intent2.putExtra("transactionId", result.transactionId);
        intent2.putExtra("pduType", result.pduType);
        intent2.putExtra("header", result.header);
        intent2.putExtra("data", result.intentData);
        intent2.putExtra("contentTypeParameters", result.contentTypeParameters);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, result.phoneId);
        intent2.putExtra("wspHeaders", result.headerList);
        if (this.bundle != null) {
            Rlog.d(TAG, "put addr info into intent 2");
            intent2.putExtra("address", this.bundle.getString("address"));
            intent2.putExtra("service_center", this.bundle.getString("service_center"));
        }
        ComponentName componentName = SmsApplication.getDefaultMmsApplication(this.mContext, true);
        Bundle options = null;
        if (componentName != null) {
            intent2.setComponent(componentName);
            Rlog.v(TAG, "Delivering MMS to: " + componentName.getPackageName() + " " + componentName.getClassName());
            try {
                long duration = this.mDeviceIdleController.addPowerSaveTempWhitelistAppForMms(componentName.getPackageName(), 0, "mms-app");
                BroadcastOptions bopts = BroadcastOptions.makeBasic();
                bopts.setTemporaryAppWhitelistDuration(duration);
                options = bopts.toBundle();
            } catch (RemoteException e2) {
            }
        }
        handler.dispatchIntent(intent2, getPermissionForType(result.mimeType), getAppOpsPermissionForIntent(result.mimeType), options, receiver, UserHandle.SYSTEM);
        return -1;
    }

    public boolean isWapPushForMms(byte[] pdu, InboundSmsHandler handler) {
        DecodedResult result = decodeWapPdu(pdu, handler);
        if (result.statusCode == -1) {
            return "application/vnd.wap.mms-message".equals(result.mimeType);
        }
        return false;
    }

    private static boolean shouldParseContentDisposition(int subId) {
        return SmsManager.getSmsManagerForSubscriptionId(subId).getCarrierConfigValues().getBoolean(SmsManager.MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION, true);
    }

    private void writeInboxMessage(int subId, GenericPdu pdu) throws Throwable {
        if (pdu == null) {
            Rlog.e(TAG, "Invalid PUSH PDU");
        }
        PduPersister persister = PduPersister.getPduPersister(this.mContext);
        int type = pdu.getMessageType();
        try {
            switch (type) {
                case 130:
                    NotificationInd nInd = (NotificationInd) pdu;
                    Bundle configs = SmsManager.getSmsManagerForSubscriptionId(subId).getCarrierConfigValues();
                    if (configs != null && configs.getBoolean(SmsManager.MMS_CONFIG_APPEND_TRANSACTION_ID, false)) {
                        byte[] contentLocation = nInd.getContentLocation();
                        if (61 == contentLocation[contentLocation.length - 1]) {
                            byte[] transactionId = nInd.getTransactionId();
                            byte[] contentLocationWithId = new byte[contentLocation.length + transactionId.length];
                            System.arraycopy(contentLocation, 0, contentLocationWithId, 0, contentLocation.length);
                            System.arraycopy(transactionId, 0, contentLocationWithId, contentLocation.length, transactionId.length);
                            nInd.setContentLocation(contentLocationWithId);
                        }
                    }
                    if (!isDuplicateNotification(this.mContext, nInd)) {
                        if (persister.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, true, true, null) == null) {
                            Rlog.e(TAG, "Failed to save MMS WAP push notification ind");
                        }
                    } else {
                        Rlog.d(TAG, "Skip storing duplicate MMS WAP push notification ind: " + new String(nInd.getContentLocation()));
                    }
                    break;
                case 134:
                case 136:
                    long threadId = getDeliveryOrReadReportThreadId(this.mContext, pdu);
                    if (threadId == -1) {
                        Rlog.e(TAG, "Failed to find delivery or read report's thread id");
                    } else {
                        Uri uri = persister.persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, true, true, null);
                        if (uri == null) {
                            Rlog.e(TAG, "Failed to persist delivery or read report");
                        } else {
                            ContentValues values = new ContentValues(1);
                            values.put("thread_id", Long.valueOf(threadId));
                            if (SqliteWrapper.update(this.mContext, this.mContext.getContentResolver(), uri, values, (String) null, (String[]) null) != 1) {
                                Rlog.e(TAG, "Failed to update delivery or read report thread id");
                            }
                        }
                    }
                    break;
                default:
                    Log.e(TAG, "Received unrecognized WAP Push PDU.");
                    break;
            }
        } catch (MmsException e) {
            Log.e(TAG, "Failed to save MMS WAP push data: type=" + type, e);
        } catch (RuntimeException e2) {
            Log.e(TAG, "Unexpected RuntimeException in persisting MMS WAP push data", e2);
        }
    }

    private static long getDeliveryOrReadReportThreadId(Context context, GenericPdu pdu) {
        String messageId;
        if (pdu instanceof DeliveryInd) {
            messageId = new String(((DeliveryInd) pdu).getMessageId());
        } else {
            if (!(pdu instanceof ReadOrigInd)) {
                Rlog.e(TAG, "WAP Push data is neither delivery or read report type: " + pdu.getClass().getCanonicalName());
                return -1L;
            }
            messageId = new String(((ReadOrigInd) pdu).getMessageId());
        }
        Cursor cursor = null;
        try {
            try {
                cursor = SqliteWrapper.query(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, new String[]{"thread_id"}, THREAD_ID_SELECTION, new String[]{DatabaseUtils.sqlEscapeString(messageId), Integer.toString(128)}, (String) null);
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (SQLiteException e) {
                Rlog.e(TAG, "Failed to query delivery or read report thread id", e);
                if (cursor != null) {
                    cursor.close();
                }
            }
            return -1L;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static boolean isDuplicateNotification(Context context, NotificationInd nInd) {
        byte[] rawLocation = nInd.getContentLocation();
        if (rawLocation != null) {
            String location = new String(rawLocation);
            new String[1][0] = location;
            Cursor cursor = null;
            try {
                try {
                    cursor = SqliteWrapper.query(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, new String[]{"_id"}, LOCATION_SELECTION, new String[]{Integer.toString(130), new String(rawLocation)}, (String) null);
                    if (cursor != null) {
                        if (cursor.getCount() > 0) {
                            return true;
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (SQLiteException e) {
                    Rlog.e(TAG, "failed to query existing notification ind", e);
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return false;
    }

    public static String getPermissionForType(String mimeType) {
        if ("application/vnd.wap.mms-message".equals(mimeType)) {
            return "android.permission.RECEIVE_MMS";
        }
        return "android.permission.RECEIVE_WAP_PUSH";
    }

    public static int getAppOpsPermissionForIntent(String mimeType) {
        if ("application/vnd.wap.mms-message".equals(mimeType)) {
            return 18;
        }
        return 19;
    }

    private final class DecodedResult {
        String contentType;
        HashMap<String, String> contentTypeParameters;
        byte[] header;
        HashMap<String, String> headerList;
        byte[] intentData;
        String mimeType;
        GenericPdu parsedPdu;
        int pduType;
        int phoneId;
        int statusCode;
        int subId;
        int transactionId;
        String wapAppId;

        DecodedResult(WapPushOverSms this$0, DecodedResult decodedResult) {
            this();
        }

        private DecodedResult() {
        }
    }

    public int dispatchWapPdu(byte[] pdu, BroadcastReceiver receiver, InboundSmsHandler handler, Bundle extra) {
        Rlog.i(TAG, "dispathchWapPdu!" + extra.getString("address") + " " + extra.getString("service_center"));
        this.bundle = extra;
        return dispatchWapPdu(pdu, receiver, handler);
    }
}
