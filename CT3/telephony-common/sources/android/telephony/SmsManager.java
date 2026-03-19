package android.telephony;

import android.app.ActivityThread;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.telephony.IMms;
import com.android.internal.telephony.ISms;
import com.android.internal.telephony.SmsRawData;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.common.MPlugin;
import com.mediatek.common.sms.IDataOnlySmsFwkExt;
import com.mediatek.common.telephony.IOnlyOwnerSimSupport;
import com.mediatek.internal.telephony.IccSmsStorageStatus;
import com.mediatek.internal.telephony.SmsCbConfigInfo;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class SmsManager {
    public static final int CELL_BROADCAST_RAN_TYPE_CDMA = 1;
    public static final int CELL_BROADCAST_RAN_TYPE_GSM = 0;
    public static final String EXTRA_MMS_DATA = "android.telephony.extra.MMS_DATA";
    public static final String EXTRA_MMS_HTTP_STATUS = "android.telephony.extra.MMS_HTTP_STATUS";
    public static final String EXTRA_PARAMS_ENCODING_TYPE = "encoding_type";
    public static final String EXTRA_PARAMS_VALIDITY_PERIOD = "validity_period";
    public static final String MESSAGE_STATUS_READ = "read";
    public static final String MESSAGE_STATUS_SEEN = "seen";
    public static final String MMS_CONFIG_ALIAS_ENABLED = "aliasEnabled";
    public static final String MMS_CONFIG_ALIAS_MAX_CHARS = "aliasMaxChars";
    public static final String MMS_CONFIG_ALIAS_MIN_CHARS = "aliasMinChars";
    public static final String MMS_CONFIG_ALLOW_ATTACH_AUDIO = "allowAttachAudio";
    public static final String MMS_CONFIG_APPEND_TRANSACTION_ID = "enabledTransID";
    public static final String MMS_CONFIG_EMAIL_GATEWAY_NUMBER = "emailGatewayNumber";
    public static final String MMS_CONFIG_GROUP_MMS_ENABLED = "enableGroupMms";
    public static final String MMS_CONFIG_HTTP_PARAMS = "httpParams";
    public static final String MMS_CONFIG_HTTP_SOCKET_TIMEOUT = "httpSocketTimeout";
    public static final String MMS_CONFIG_MAX_IMAGE_HEIGHT = "maxImageHeight";
    public static final String MMS_CONFIG_MAX_IMAGE_WIDTH = "maxImageWidth";
    public static final String MMS_CONFIG_MAX_MESSAGE_SIZE = "maxMessageSize";
    public static final String MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE = "maxMessageTextSize";
    public static final String MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED = "enableMMSDeliveryReports";
    public static final String MMS_CONFIG_MMS_ENABLED = "enabledMMS";
    public static final String MMS_CONFIG_MMS_READ_REPORT_ENABLED = "enableMMSReadReports";
    public static final String MMS_CONFIG_MULTIPART_SMS_ENABLED = "enableMultipartSMS";
    public static final String MMS_CONFIG_NAI_SUFFIX = "naiSuffix";
    public static final String MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED = "enabledNotifyWapMMSC";
    public static final String MMS_CONFIG_RECIPIENT_LIMIT = "recipientLimit";
    public static final String MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES = "sendMultipartSmsAsSeparateMessages";
    public static final String MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS = "config_cellBroadcastAppLinks";
    public static final String MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED = "enableSMSDeliveryReports";
    public static final String MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD = "smsToMmsTextLengthThreshold";
    public static final String MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD = "smsToMmsTextThreshold";
    public static final String MMS_CONFIG_SUBJECT_MAX_LENGTH = "maxSubjectLength";
    public static final String MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER = "supportHttpCharsetHeader";
    public static final String MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION = "supportMmsContentDisposition";
    public static final String MMS_CONFIG_UA_PROF_TAG_NAME = "uaProfTagName";
    public static final String MMS_CONFIG_UA_PROF_URL = "uaProfUrl";
    public static final String MMS_CONFIG_USER_AGENT = "userAgent";
    public static final int MMS_ERROR_CONFIGURATION_ERROR = 7;
    public static final int MMS_ERROR_HTTP_FAILURE = 4;
    public static final int MMS_ERROR_INVALID_APN = 2;
    public static final int MMS_ERROR_IO_ERROR = 5;
    public static final int MMS_ERROR_NO_DATA_NETWORK = 8;
    public static final int MMS_ERROR_RETRY = 6;
    public static final int MMS_ERROR_UNABLE_CONNECT_MMS = 3;
    public static final int MMS_ERROR_UNSPECIFIED = 1;
    private static final String PHONE_PACKAGE_NAME = "com.android.phone";
    public static final int RESULT_ERROR_FDN_CHECK_FAILURE = 6;
    public static final int RESULT_ERROR_GENERIC_FAILURE = 1;
    public static final int RESULT_ERROR_INVALID_ADDRESS = 8;
    public static final int RESULT_ERROR_LIMIT_EXCEEDED = 5;
    public static final int RESULT_ERROR_NO_SERVICE = 4;
    public static final int RESULT_ERROR_NULL_PDU = 3;
    public static final int RESULT_ERROR_RADIO_OFF = 2;
    public static final int RESULT_ERROR_SIM_MEM_FULL = 7;
    public static final int RESULT_ERROR_SUCCESS = 0;
    private static final int SMS_PICK = 2;
    public static final int SMS_TYPE_INCOMING = 0;
    public static final int SMS_TYPE_OUTGOING = 1;
    public static final int STATUS_ON_ICC_FREE = 0;
    public static final int STATUS_ON_ICC_READ = 1;
    public static final int STATUS_ON_ICC_SENT = 5;
    public static final int STATUS_ON_ICC_UNREAD = 3;
    public static final int STATUS_ON_ICC_UNSENT = 7;
    private static final String TAG = "SmsManager";
    public static final int VALIDITY_PERIOD_MAX_DURATION = 255;
    public static final int VALIDITY_PERIOD_NO_DURATION = -1;
    public static final int VALIDITY_PERIOD_ONE_DAY = 167;
    public static final int VALIDITY_PERIOD_ONE_HOUR = 11;
    public static final int VALIDITY_PERIOD_SIX_HOURS = 71;
    public static final int VALIDITY_PERIOD_TWELVE_HOURS = 143;
    private IDataOnlySmsFwkExt mDataOnlySmsFwkExt;
    private IOnlyOwnerSimSupport mOnlyOwnerSimSupport;
    private int mSubId;
    private static final int DEFAULT_SUBSCRIPTION_ID = -1002;
    private static final SmsManager sInstance = new SmsManager(DEFAULT_SUBSCRIPTION_ID);
    private static final Object sLockObject = new Object();
    private static final Map<Integer, SmsManager> sSubInstances = new ArrayMap();
    private static String DIALOG_TYPE_KEY = "dialog_type";

    public void sendTextMessage(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        sendTextMessageInternal(destinationAddress, scAddress, text, sentIntent, deliveryIntent, true);
    }

    private void sendTextMessageInternal(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForCarrierApp) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        Rlog.d(TAG, "sendTextMessage, text=" + text + ", destinationAddress=" + destinationAddress);
        if (!isValidParameters(destinationAddress, text, sentIntent)) {
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendTextForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destinationAddress, scAddress, text, sentIntent, deliveryIntent, persistMessageForCarrierApp);
        } catch (RemoteException e) {
            Rlog.d(TAG, "sendTextMessage, RemoteException!");
        }
    }

    public void sendTextMessageWithoutPersisting(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        sendTextMessageInternal(destinationAddress, scAddress, text, sentIntent, deliveryIntent, false);
    }

    public void sendTextMessageWithSelfPermissions(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        Rlog.d(TAG, "sendTextMessage, text=" + text + ", destinationAddress=" + destinationAddress);
        if (!isValidParameters(destinationAddress, text, sentIntent)) {
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendTextForSubscriberWithSelfPermissions(getSubscriptionId(), ActivityThread.currentPackageName(), destinationAddress, scAddress, text, sentIntent, deliveryIntent);
        } catch (RemoteException e) {
        }
    }

    public void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) {
        if (!format.equals(SmsMessage.FORMAT_3GPP) && !format.equals(SmsMessage.FORMAT_3GPP2)) {
            throw new IllegalArgumentException("Invalid pdu format. format must be either 3gpp or 3gpp2");
        }
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iccISms == null) {
                return;
            }
            iccISms.injectSmsPduForSubscriber(getSubscriptionId(), pdu, format, receivedIntent);
        } catch (RemoteException e) {
        }
    }

    public ArrayList<String> divideMessage(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text is null");
        }
        return SmsMessage.fragmentText(text);
    }

    public void sendMultipartTextMessage(String destinationAddress, String scAddress, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        sendMultipartTextMessageInternal(destinationAddress, scAddress, parts, sentIntents, deliveryIntents, true);
    }

    private void sendMultipartTextMessageInternal(String destinationAddress, String scAddress, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, boolean persistMessageForCarrierApp) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        Rlog.d(TAG, "sendMultipartTextMessage, destinationAddress=" + destinationAddress);
        if (!isValidParameters(destinationAddress, parts, sentIntents)) {
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        if (parts.size() > 1) {
            try {
                ISms iccISms = getISmsServiceOrThrow();
                iccISms.sendMultipartTextForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destinationAddress, scAddress, parts, sentIntents, deliveryIntents, persistMessageForCarrierApp);
                return;
            } catch (RemoteException e) {
                Rlog.d(TAG, "sendMultipartTextMessage, RemoteException!");
                return;
            }
        }
        PendingIntent sentIntent = null;
        PendingIntent deliveryIntent = null;
        if (sentIntents != null && sentIntents.size() > 0) {
            sentIntent = sentIntents.get(0);
        }
        if (deliveryIntents != null && deliveryIntents.size() > 0) {
            deliveryIntent = deliveryIntents.get(0);
        }
        String text = (parts == null || parts.size() == 0) ? UsimPBMemInfo.STRING_NOT_SET : parts.get(0);
        sendTextMessage(destinationAddress, scAddress, text, sentIntent, deliveryIntent);
    }

    public void sendMultipartTextMessageWithoutPersisting(String destinationAddress, String scAddress, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        sendMultipartTextMessageInternal(destinationAddress, scAddress, parts, sentIntents, deliveryIntents, false);
    }

    public void sendDataMessage(String destinationAddress, String scAddress, short destinationPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendDataForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destinationAddress, scAddress, destinationPort & 65535, data, sentIntent, deliveryIntent);
        } catch (RemoteException e) {
            Rlog.d(TAG, "sendDataMessage, RemoteException!");
        }
    }

    public void sendDataMessageWithSelfPermissions(String destinationAddress, String scAddress, short destinationPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {
            return;
        }
        ArrayList<PendingIntent> sentIntents = new ArrayList<>(1);
        sentIntents.add(sentIntent);
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendDataForSubscriberWithSelfPermissions(getSubscriptionId(), ActivityThread.currentPackageName(), destinationAddress, scAddress, destinationPort & 65535, data, sentIntent, deliveryIntent);
        } catch (RemoteException e) {
        }
    }

    public static SmsManager getDefault() {
        return sInstance;
    }

    public static SmsManager getSmsManagerForSubscriptionId(int subId) {
        SmsManager smsManager;
        synchronized (sLockObject) {
            smsManager = sSubInstances.get(Integer.valueOf(subId));
            if (smsManager == null) {
                smsManager = new SmsManager(subId);
                sSubInstances.put(Integer.valueOf(subId), smsManager);
            }
        }
        return smsManager;
    }

    private SmsManager(int subId) {
        this.mOnlyOwnerSimSupport = null;
        this.mDataOnlySmsFwkExt = null;
        this.mSubId = subId;
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return;
        }
        try {
            this.mOnlyOwnerSimSupport = (IOnlyOwnerSimSupport) MPlugin.createInstance(IOnlyOwnerSimSupport.class.getName());
            if (this.mOnlyOwnerSimSupport != null) {
                String actualClassName = this.mOnlyOwnerSimSupport.getClass().getName();
                Rlog.d(TAG, "initial mOnlyOwnerSimSupport done, actual class name is " + actualClassName);
            } else {
                Rlog.e(TAG, "FAIL! intial mOnlyOwnerSimSupport");
            }
        } catch (RuntimeException e) {
            Rlog.e(TAG, "FAIL! No IOnlyOwnerSimSupport");
        }
        try {
            this.mDataOnlySmsFwkExt = (IDataOnlySmsFwkExt) MPlugin.createInstance(IDataOnlySmsFwkExt.class.getName());
            if (this.mDataOnlySmsFwkExt != null) {
                String className = this.mDataOnlySmsFwkExt.getClass().getName();
                Rlog.d(TAG, "initial mDataOnlySmsFwkExt done, class name is " + className);
            } else {
                Rlog.e(TAG, "FAIL! intial mDataOnlySmsFwkExt");
            }
        } catch (RuntimeException e2) {
            Rlog.e(TAG, "FAIL! No mDataOnlySmsFwkExt");
        }
    }

    public int getSubscriptionId() {
        int subId = this.mSubId == DEFAULT_SUBSCRIPTION_ID ? getDefaultSmsSubscriptionId() : this.mSubId;
        Context context = ActivityThread.currentApplication().getApplicationContext();
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                iccISms.isSmsSimPickActivityNeeded(subId);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in getSubscriptionId");
        }
        if (0 != 0 && BenesseExtension.getDchaState() == 0) {
            Log.d(TAG, "getSubscriptionId isSmsSimPickActivityNeeded is true");
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.sim.SimDialogActivity");
            intent.addFlags(268435456);
            intent.putExtra(DIALOG_TYPE_KEY, 2);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e2) {
                Log.e(TAG, "Unable to launch Settings application.");
            }
        }
        return subId;
    }

    private static ISms getISmsServiceOrThrow() {
        ISms iccISms = getISmsService();
        if (iccISms == null) {
            throw new UnsupportedOperationException("Sms is not supported");
        }
        return iccISms;
    }

    private static ISms getISmsService() {
        return ISms.Stub.asInterface(ServiceManager.getService("isms"));
    }

    public boolean copyMessageToIcc(byte[] smsc, byte[] pdu, int status) {
        Rlog.d(TAG, "copyMessageToIcc");
        if (pdu == null) {
            throw new IllegalArgumentException("pdu is NULL");
        }
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false;
        }
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean success = iccISms.copyMessageToIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), status, pdu, smsc);
            return success;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean deleteMessageFromIcc(int messageIndex) {
        Rlog.d(TAG, "deleteMessageFromIcc, messageIndex=" + messageIndex);
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false;
        }
        byte[] pdu = new byte[PduHeaders.START];
        Arrays.fill(pdu, (byte) -1);
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean success = iccISms.updateMessageOnIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), messageIndex, 0, pdu);
            return success;
        } catch (RemoteException e) {
            Rlog.d(TAG, "deleteMessageFromIcc, RemoteException!");
            return false;
        }
    }

    public boolean updateMessageOnIcc(int messageIndex, int newStatus, byte[] pdu) {
        Rlog.d(TAG, "updateMessageOnIcc, messageIndex=" + messageIndex);
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false;
        }
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean success = iccISms.updateMessageOnIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), messageIndex, newStatus, pdu);
            return success;
        } catch (RemoteException e) {
            Rlog.d(TAG, "updateMessageOnIcc, RemoteException!");
            return false;
        }
    }

    public ArrayList<SmsMessage> getAllMessagesFromIcc() {
        Rlog.d(TAG, "getAllMessagesFromIcc");
        List<SmsRawData> records = null;
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                records = iccISms.getAllMessagesFromIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName());
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "getAllMessagesFromIcc, RemoteException!");
        }
        return createMessageListFromRawRecords(records);
    }

    public boolean enableCellBroadcast(int messageIdentifier, int ranType) {
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean success = iccISms.enableCellBroadcastForSubscriber(getSubscriptionId(), messageIdentifier, ranType);
            return success;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disableCellBroadcast(int messageIdentifier, int ranType) {
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean success = iccISms.disableCellBroadcastForSubscriber(getSubscriptionId(), messageIdentifier, ranType);
            return success;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        if (endMessageId < startMessageId) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean success = iccISms.enableCellBroadcastRangeForSubscriber(getSubscriptionId(), startMessageId, endMessageId, ranType);
            return success;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) {
        if (endMessageId < startMessageId) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean success = iccISms.disableCellBroadcastRangeForSubscriber(getSubscriptionId(), startMessageId, endMessageId, ranType);
            return success;
        } catch (RemoteException e) {
            return false;
        }
    }

    private ArrayList<SmsMessage> createMessageListFromRawRecords(List<SmsRawData> records) {
        ArrayList<SmsMessage> messages = new ArrayList<>();
        Rlog.d(TAG, "createMessageListFromRawRecords");
        if (records != null) {
            int count = records.size();
            for (int i = 0; i < count; i++) {
                SmsRawData data = records.get(i);
                if (data != null) {
                    int activePhone = TelephonyManager.getDefault().getCurrentPhoneType(this.mSubId);
                    String phoneType = 2 == activePhone ? SmsMessage.FORMAT_3GPP2 : SmsMessage.FORMAT_3GPP;
                    Rlog.d(TAG, "phoneType: " + phoneType);
                    SmsMessage sms = SmsMessage.createFromEfRecord(i + 1, data.getBytes(), phoneType);
                    if (sms != null) {
                        messages.add(sms);
                    }
                }
            }
            Rlog.d(TAG, "actual sms count is " + count);
        } else {
            Rlog.d(TAG, "fail to parse SIM sms, records is null");
        }
        return messages;
    }

    public boolean isImsSmsSupported() {
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return false;
            }
            boolean boSupported = iccISms.isImsSmsSupportedForSubscriber(getSubscriptionId());
            return boSupported;
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getImsSmsFormat() {
        try {
            ISms iccISms = getISmsService();
            if (iccISms == null) {
                return "unknown";
            }
            String format = iccISms.getImsSmsFormatForSubscriber(getSubscriptionId());
            return format;
        } catch (RemoteException e) {
            return "unknown";
        }
    }

    public static int getDefaultSmsSubscriptionId() {
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            return iccISms.getPreferredSmsSubscription();
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public boolean isSMSPromptEnabled() {
        try {
            ISms iccISms = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            return iccISms.isSMSPromptEnabled();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public void sendMultimediaMessage(Context context, Uri contentUri, String locationUrl, Bundle configOverrides, PendingIntent sentIntent) {
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms == null) {
                return;
            }
            iMms.sendMessage(getSubscriptionId(), ActivityThread.currentPackageName(), contentUri, locationUrl, configOverrides, sentIntent);
        } catch (RemoteException e) {
        }
    }

    public void downloadMultimediaMessage(Context context, String locationUrl, Uri contentUri, Bundle configOverrides, PendingIntent downloadedIntent) {
        if (TextUtils.isEmpty(locationUrl)) {
            throw new IllegalArgumentException("Empty MMS location URL");
        }
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms == null) {
                return;
            }
            iMms.downloadMessage(getSubscriptionId(), ActivityThread.currentPackageName(), locationUrl, contentUri, configOverrides, downloadedIntent);
        } catch (RemoteException e) {
        }
    }

    public Uri importTextMessage(String address, int type, String text, long timestampMillis, boolean seen, boolean read) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.importTextMessage(ActivityThread.currentPackageName(), address, type, text, timestampMillis, seen, read);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public Uri importMultimediaMessage(Uri contentUri, String messageId, long timestampSecs, boolean seen, boolean read) {
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.importMultimediaMessage(ActivityThread.currentPackageName(), contentUri, messageId, timestampSecs, seen, read);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean deleteStoredMessage(Uri messageUri) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.deleteStoredMessage(ActivityThread.currentPackageName(), messageUri);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean deleteStoredConversation(long conversationId) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.deleteStoredConversation(ActivityThread.currentPackageName(), conversationId);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean updateStoredMessageStatus(Uri messageUri, ContentValues statusValues) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.updateStoredMessageStatus(ActivityThread.currentPackageName(), messageUri, statusValues);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean archiveStoredConversation(long conversationId, boolean archived) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.archiveStoredConversation(ActivityThread.currentPackageName(), conversationId, archived);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public Uri addTextMessageDraft(String address, String text) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.addTextMessageDraft(ActivityThread.currentPackageName(), address, text);
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    public Uri addMultimediaMessageDraft(Uri contentUri) {
        if (contentUri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.addMultimediaMessageDraft(ActivityThread.currentPackageName(), contentUri);
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    public void sendStoredTextMessage(Uri messageUri, String scAddress, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendStoredText(getSubscriptionId(), ActivityThread.currentPackageName(), messageUri, scAddress, sentIntent, deliveryIntent);
        } catch (RemoteException e) {
        }
    }

    public void sendStoredMultipartTextMessage(Uri messageUri, String scAddress, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            iccISms.sendStoredMultipartText(getSubscriptionId(), ActivityThread.currentPackageName(), messageUri, scAddress, sentIntents, deliveryIntents);
        } catch (RemoteException e) {
        }
    }

    public void sendStoredMultimediaMessage(Uri messageUri, Bundle configOverrides, PendingIntent sentIntent) {
        if (messageUri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms == null) {
                return;
            }
            iMms.sendStoredMessage(getSubscriptionId(), ActivityThread.currentPackageName(), messageUri, configOverrides, sentIntent);
        } catch (RemoteException e) {
        }
    }

    public void setAutoPersisting(boolean enabled) {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms == null) {
                return;
            }
            iMms.setAutoPersisting(ActivityThread.currentPackageName(), enabled);
        } catch (RemoteException e) {
        }
    }

    public boolean getAutoPersisting() {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.getAutoPersisting();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public Bundle getCarrierConfigValues() {
        try {
            IMms iMms = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMms != null) {
                return iMms.getCarrierConfigValues(getSubscriptionId());
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    public static Bundle getMmsConfig(BaseBundle config) {
        Bundle filtered = new Bundle();
        filtered.putBoolean(MMS_CONFIG_APPEND_TRANSACTION_ID, config.getBoolean(MMS_CONFIG_APPEND_TRANSACTION_ID));
        filtered.putBoolean(MMS_CONFIG_MMS_ENABLED, config.getBoolean(MMS_CONFIG_MMS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_GROUP_MMS_ENABLED, config.getBoolean(MMS_CONFIG_GROUP_MMS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED, config.getBoolean(MMS_CONFIG_NOTIFY_WAP_MMSC_ENABLED));
        filtered.putBoolean(MMS_CONFIG_ALIAS_ENABLED, config.getBoolean(MMS_CONFIG_ALIAS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_ALLOW_ATTACH_AUDIO, config.getBoolean(MMS_CONFIG_ALLOW_ATTACH_AUDIO));
        filtered.putBoolean(MMS_CONFIG_MULTIPART_SMS_ENABLED, config.getBoolean(MMS_CONFIG_MULTIPART_SMS_ENABLED));
        filtered.putBoolean(MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED, config.getBoolean(MMS_CONFIG_SMS_DELIVERY_REPORT_ENABLED));
        filtered.putBoolean(MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION, config.getBoolean(MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION));
        filtered.putBoolean(MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES, config.getBoolean(MMS_CONFIG_SEND_MULTIPART_SMS_AS_SEPARATE_MESSAGES));
        filtered.putBoolean(MMS_CONFIG_MMS_READ_REPORT_ENABLED, config.getBoolean(MMS_CONFIG_MMS_READ_REPORT_ENABLED));
        filtered.putBoolean(MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED, config.getBoolean(MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED));
        filtered.putInt(MMS_CONFIG_MAX_MESSAGE_SIZE, config.getInt(MMS_CONFIG_MAX_MESSAGE_SIZE));
        filtered.putInt(MMS_CONFIG_MAX_IMAGE_WIDTH, config.getInt(MMS_CONFIG_MAX_IMAGE_WIDTH));
        filtered.putInt(MMS_CONFIG_MAX_IMAGE_HEIGHT, config.getInt(MMS_CONFIG_MAX_IMAGE_HEIGHT));
        filtered.putInt(MMS_CONFIG_RECIPIENT_LIMIT, config.getInt(MMS_CONFIG_RECIPIENT_LIMIT));
        filtered.putInt(MMS_CONFIG_ALIAS_MIN_CHARS, config.getInt(MMS_CONFIG_ALIAS_MIN_CHARS));
        filtered.putInt(MMS_CONFIG_ALIAS_MAX_CHARS, config.getInt(MMS_CONFIG_ALIAS_MAX_CHARS));
        filtered.putInt(MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD, config.getInt(MMS_CONFIG_SMS_TO_MMS_TEXT_THRESHOLD));
        filtered.putInt(MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD, config.getInt(MMS_CONFIG_SMS_TO_MMS_TEXT_LENGTH_THRESHOLD));
        filtered.putInt(MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE, config.getInt(MMS_CONFIG_MESSAGE_TEXT_MAX_SIZE));
        filtered.putInt(MMS_CONFIG_SUBJECT_MAX_LENGTH, config.getInt(MMS_CONFIG_SUBJECT_MAX_LENGTH));
        filtered.putInt(MMS_CONFIG_HTTP_SOCKET_TIMEOUT, config.getInt(MMS_CONFIG_HTTP_SOCKET_TIMEOUT));
        filtered.putString(MMS_CONFIG_UA_PROF_TAG_NAME, config.getString(MMS_CONFIG_UA_PROF_TAG_NAME));
        filtered.putString(MMS_CONFIG_USER_AGENT, config.getString(MMS_CONFIG_USER_AGENT));
        filtered.putString(MMS_CONFIG_UA_PROF_URL, config.getString(MMS_CONFIG_UA_PROF_URL));
        filtered.putString(MMS_CONFIG_HTTP_PARAMS, config.getString(MMS_CONFIG_HTTP_PARAMS));
        filtered.putString(MMS_CONFIG_EMAIL_GATEWAY_NUMBER, config.getString(MMS_CONFIG_EMAIL_GATEWAY_NUMBER));
        filtered.putString(MMS_CONFIG_NAI_SUFFIX, config.getString(MMS_CONFIG_NAI_SUFFIX));
        filtered.putBoolean(MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS, config.getBoolean(MMS_CONFIG_SHOW_CELL_BROADCAST_APP_LINKS));
        filtered.putBoolean(MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER, config.getBoolean(MMS_CONFIG_SUPPORT_HTTP_CHARSET_HEADER));
        return filtered;
    }

    private static boolean isValidParameters(String destinationAddress, String text, PendingIntent sentIntent) {
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        ArrayList<String> parts = new ArrayList<>();
        sentIntents.add(sentIntent);
        parts.add(text);
        return isValidParameters(destinationAddress, parts, sentIntents);
    }

    private static boolean isValidParameters(String destinationAddress, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents) {
        if (parts == null || parts.size() == 0) {
            return true;
        }
        if (!isValidSmsDestinationAddress(destinationAddress)) {
            for (int i = 0; i < sentIntents.size(); i++) {
                PendingIntent sentIntent = sentIntents.get(i);
                if (sentIntent != null) {
                    try {
                        sentIntent.send(1);
                    } catch (PendingIntent.CanceledException e) {
                    }
                }
            }
            Rlog.d(TAG, "Invalid destinationAddress: " + destinationAddress);
            return false;
        }
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (parts == null || parts.size() < 1) {
            throw new IllegalArgumentException("Invalid message body");
        }
        return true;
    }

    private static boolean isValidSmsDestinationAddress(String da) {
        String encodeAddress = PhoneNumberUtils.extractNetworkPortion(da);
        return encodeAddress == null || !encodeAddress.isEmpty();
    }

    public ArrayList<SmsMessage> getAllMessagesFromIccEfByMode(int mode) {
        Rlog.d(TAG, "getAllMessagesFromIcc, mode=" + mode);
        List<SmsRawData> records = null;
        try {
            ISms iccISms = getISmsService();
            if (iccISms != null) {
                records = iccISms.getAllMessagesFromIccEfByModeForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), mode);
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException!");
        }
        int sz = 0;
        if (records != null) {
            sz = records.size();
        }
        for (int i = 0; i < sz; i++) {
            SmsRawData record = records.get(i);
            if (record != null) {
                byte[] data = record.getBytes();
                int index = i + 1;
                if ((data[0] & PplMessageManager.Type.INVALID) == 3) {
                    Rlog.d(TAG, "index[" + index + "] is STATUS_ON_ICC_READ");
                    boolean ret = updateMessageOnIcc(index, 1, data);
                    if (ret) {
                        Rlog.d(TAG, "update index[" + index + "] to STATUS_ON_ICC_READ");
                    } else {
                        Rlog.d(TAG, "fail to update message status");
                    }
                }
            }
        }
        return createMessageListFromRawRecordsByMode(getSubscriptionId(), records, mode);
    }

    private static ArrayList<SmsMessage> createMessageListFromRawRecordsByMode(int subId, List<SmsRawData> records, int mode) {
        SmsMessage singleSms;
        Rlog.d(TAG, "createMessageListFromRawRecordsByMode");
        ArrayList<SmsMessage> msg = null;
        if (records != null) {
            int count = records.size();
            msg = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                SmsRawData data = records.get(i);
                if (data != null && (singleSms = createFromEfRecordByMode(subId, i + 1, data.getBytes(), mode)) != null) {
                    msg.add(singleSms);
                }
            }
            Rlog.d(TAG, "actual sms count is " + msg.size());
        } else {
            Rlog.d(TAG, "fail to parse SIM sms, records is null");
        }
        return msg;
    }

    private static SmsMessage createFromEfRecordByMode(int subId, int index, byte[] data, int mode) {
        SmsMessage sms;
        if (mode == 2) {
            sms = SmsMessage.createFromEfRecord(index, data, SmsMessage.FORMAT_3GPP2);
        } else {
            sms = SmsMessage.createFromEfRecord(index, data, SmsMessage.FORMAT_3GPP);
        }
        if (sms != null) {
            sms.setSubId(subId);
        }
        return sms;
    }

    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text, int status, long timestamp) {
        Rlog.d(TAG, "copyTextMessageToIccCard");
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return 1;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms == null) {
                return 1;
            }
            int result = iccISms.copyTextMessageToIccCardForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), scAddress, address, text, status, timestamp);
            return result;
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException!");
            return 1;
        }
    }

    public void sendDataMessage(String destinationAddress, String scAddress, short destinationPort, short originalPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        if (TextUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (!isValidParameters(destinationAddress, "send_data", sentIntent)) {
            return;
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms == null) {
                return;
            }
            iccISms.sendDataWithOriginalPortForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destinationAddress, scAddress, destinationPort & 65535, originalPort & 65535, data, sentIntent, deliveryIntent);
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException!");
        }
    }

    public void sendTextMessageWithEncodingType(String destAddr, String scAddr, String text, int encodingType, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(TAG, "sendTextMessageWithEncodingType, text=" + text + ", encoding=" + encodingType);
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (!isValidParameters(destAddr, text, sentIntent)) {
            Rlog.d(TAG, "the parameters are invalid");
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms == null) {
                return;
            }
            iccISms.sendTextWithEncodingTypeForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destAddr, scAddr, text, encodingType, sentIntent, deliveryIntent, true);
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
    }

    public void sendMultipartTextMessageWithEncodingType(String destAddr, String scAddr, ArrayList<String> parts, int encodingType, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        Rlog.d(TAG, "sendMultipartTextMessageWithEncodingType, encoding=" + encodingType);
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (!isValidParameters(destAddr, parts, sentIntents)) {
            Rlog.d(TAG, "invalid parameters for multipart message");
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        if (parts.size() > 1) {
            try {
                ISms iccISms = getISmsServiceOrThrow();
                if (iccISms == null) {
                    return;
                }
                iccISms.sendMultipartTextWithEncodingTypeForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destAddr, scAddr, parts, encodingType, sentIntents, deliveryIntents, true);
                return;
            } catch (RemoteException e) {
                Rlog.d(TAG, "RemoteException");
                return;
            }
        }
        PendingIntent sentIntent = null;
        PendingIntent deliveryIntent = null;
        if (sentIntents != null && sentIntents.size() > 0) {
            sentIntent = sentIntents.get(0);
        }
        Rlog.d(TAG, "get sentIntent: " + sentIntent);
        if (deliveryIntents != null && deliveryIntents.size() > 0) {
            deliveryIntent = deliveryIntents.get(0);
        }
        Rlog.d(TAG, "send single message");
        if (parts != null) {
            Rlog.d(TAG, "parts.size = " + parts.size());
        }
        String text = (parts == null || parts.size() == 0) ? UsimPBMemInfo.STRING_NOT_SET : parts.get(0);
        Rlog.d(TAG, "pass encoding type " + encodingType);
        sendTextMessageWithEncodingType(destAddr, scAddr, text, encodingType, sentIntent, deliveryIntent);
    }

    public ArrayList<String> divideMessage(String text, int encodingType) {
        Rlog.d(TAG, "divideMessage, encoding = " + encodingType);
        ArrayList<String> ret = SmsMessage.fragmentText(text, encodingType);
        Rlog.d(TAG, "divideMessage: size = " + ret.size());
        return ret;
    }

    public SimSmsInsertStatus insertTextMessageToIccCard(String scAddress, String address, List<String> text, int status, long timestamp) {
        Rlog.d(TAG, "insertTextMessageToIccCard");
        SimSmsInsertStatus ret = null;
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return null;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                ret = iccISms.insertTextMessageToIccCardForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), scAddress, address, text, status, timestamp);
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
        Rlog.d(TAG, ret != null ? "insert Text " + ret.indexInIcc : "insert Text null");
        return ret;
    }

    public SimSmsInsertStatus insertRawMessageToIccCard(int status, byte[] pdu, byte[] smsc) {
        Rlog.d(TAG, "insertRawMessageToIccCard");
        SimSmsInsertStatus ret = null;
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, UsimPBMemInfo.STRING_NOT_SET);
            return null;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                ret = iccISms.insertRawMessageToIccCardForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), status, pdu, smsc);
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
        Rlog.d(TAG, ret != null ? "insert Raw " + ret.indexInIcc : "insert Raw null");
        return ret;
    }

    public void sendTextMessageWithExtraParams(String destAddr, String scAddr, String text, Bundle extraParams, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        Rlog.d(TAG, "sendTextMessageWithExtraParams, text=" + text);
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (!isValidParameters(destAddr, text, sentIntent)) {
            return;
        }
        if (extraParams == null) {
            Rlog.d(TAG, "bundle is null");
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntent, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms == null) {
                return;
            }
            iccISms.sendTextWithExtraParamsForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destAddr, scAddr, text, extraParams, sentIntent, deliveryIntent, true);
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
    }

    public void sendMultipartTextMessageWithExtraParams(String destAddr, String scAddr, ArrayList<String> parts, Bundle extraParams, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        Rlog.d(TAG, "sendMultipartTextMessageWithExtraParams");
        if (TextUtils.isEmpty(destAddr)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (!isValidParameters(destAddr, parts, sentIntents)) {
            return;
        }
        if (extraParams == null) {
            Rlog.d(TAG, "bundle is null");
            return;
        }
        Context context = ActivityThread.currentApplication().getApplicationContext();
        if (this.mDataOnlySmsFwkExt != null && this.mDataOnlySmsFwkExt.is4GDataOnlyMode(sentIntents, getSubscriptionId(), context)) {
            Rlog.d(TAG, "is4GDataOnlyMode");
            return;
        }
        if (parts.size() > 1) {
            try {
                ISms iccISms = getISmsServiceOrThrow();
                if (iccISms == null) {
                    return;
                }
                iccISms.sendMultipartTextWithExtraParamsForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), destAddr, scAddr, parts, extraParams, sentIntents, deliveryIntents, true);
                return;
            } catch (RemoteException e) {
                Rlog.d(TAG, "RemoteException");
                return;
            }
        }
        PendingIntent sentIntent = null;
        PendingIntent deliveryIntent = null;
        if (sentIntents != null && sentIntents.size() > 0) {
            sentIntent = sentIntents.get(0);
        }
        if (deliveryIntents != null && deliveryIntents.size() > 0) {
            deliveryIntent = deliveryIntents.get(0);
        }
        String text = (parts == null || parts.size() == 0) ? UsimPBMemInfo.STRING_NOT_SET : parts.get(0);
        sendTextMessageWithExtraParams(destAddr, scAddr, text, extraParams, sentIntent, deliveryIntent);
    }

    public SmsParameters getSmsParameters() {
        Rlog.d(TAG, "getSmsParameters");
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return null;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                return iccISms.getSmsParametersForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName());
            }
            return null;
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
            Rlog.d(TAG, "fail to get SmsParameters");
            return null;
        }
    }

    public boolean setSmsParameters(SmsParameters params) {
        Rlog.d(TAG, "setSmsParameters");
        if (this.mOnlyOwnerSimSupport != null && !this.mOnlyOwnerSimSupport.isCurrentUserOwner()) {
            Rlog.d(TAG, "Not the current owner and reject this operation");
            return false;
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                return iccISms.setSmsParametersForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), params);
            }
            return false;
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
            return false;
        }
    }

    public int copySmsToIcc(byte[] smsc, byte[] pdu, int status) {
        int[] index;
        Rlog.d(TAG, "copySmsToIcc");
        SimSmsInsertStatus smsStatus = insertRawMessageToIccCard(status, pdu, smsc);
        if (smsStatus == null || (index = smsStatus.getIndex()) == null || index.length <= 0) {
            return -1;
        }
        return index[0];
    }

    public boolean updateSmsOnSimReadStatus(int index, boolean read) {
        Rlog.d(TAG, "updateSmsOnSimReadStatus");
        SmsRawData record = null;
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                record = iccISms.getMessageFromIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), index);
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
        if (record != null) {
            byte[] rawData = record.getBytes();
            int status = rawData[0] & PplMessageManager.Type.INVALID;
            Rlog.d(TAG, "sms status is " + status);
            if (status != 3 && status != 1) {
                Rlog.d(TAG, "non-delivery sms " + status);
                return false;
            }
            if ((status == 3 && !read) || (status == 1 && read)) {
                Rlog.d(TAG, "no need to update status");
                return true;
            }
            Rlog.d(TAG, "update sms status as " + read);
            int newStatus = read ? 1 : 3;
            return updateMessageOnIcc(index, newStatus, rawData);
        }
        Rlog.d(TAG, "record is null");
        return false;
    }

    public boolean setEtwsConfig(int mode) {
        Rlog.d(TAG, "setEtwsConfig, mode=" + mode);
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms == null) {
                return false;
            }
            boolean ret = iccISms.setEtwsConfigForSubscriber(getSubscriptionId(), mode);
            return ret;
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
            return false;
        }
    }

    public void setSmsMemoryStatus(boolean status) {
        Rlog.d(TAG, "setSmsMemoryStatus");
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms == null) {
                return;
            }
            iccISms.setSmsMemoryStatusForSubscriber(getSubscriptionId(), status);
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
    }

    public IccSmsStorageStatus getSmsSimMemoryStatus() {
        Rlog.d(TAG, "getSmsSimMemoryStatus");
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                return iccISms.getSmsSimMemoryStatusForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName());
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
        return null;
    }

    private SmsBroadcastConfigInfo Convert2SmsBroadcastConfigInfo(SmsCbConfigInfo info) {
        return new SmsBroadcastConfigInfo(info.mFromServiceId, info.mToServiceId, info.mFromCodeScheme, info.mToCodeScheme, info.mSelected);
    }

    private SmsCbConfigInfo Convert2SmsCbConfigInfo(SmsBroadcastConfigInfo info) {
        return new SmsCbConfigInfo(info.getFromServiceId(), info.getToServiceId(), info.getFromCodeScheme(), info.getToCodeScheme(), info.isSelected());
    }

    public SmsBroadcastConfigInfo[] getCellBroadcastSmsConfig() {
        Rlog.d(TAG, "getCellBroadcastSmsConfig");
        Rlog.d(TAG, "subId=" + getSubscriptionId());
        SmsCbConfigInfo[] configs = null;
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                configs = iccISms.getCellBroadcastSmsConfigForSubscriber(getSubscriptionId());
            } else {
                Rlog.d(TAG, "fail to get sms service");
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException");
        }
        if (configs != null) {
            Rlog.d(TAG, "config length = " + configs.length);
            if (configs.length != 0) {
                SmsBroadcastConfigInfo[] result = new SmsBroadcastConfigInfo[configs.length];
                for (int i = 0; i < configs.length; i++) {
                    result[i] = Convert2SmsBroadcastConfigInfo(configs[i]);
                }
                return result;
            }
        }
        return null;
    }

    public boolean setCellBroadcastSmsConfig(SmsBroadcastConfigInfo[] channels, SmsBroadcastConfigInfo[] languages) {
        Rlog.d(TAG, "setCellBroadcastSmsConfig");
        Rlog.d(TAG, "subId=" + getSubscriptionId());
        if (channels != null) {
            Rlog.d(TAG, "channel size=" + channels.length);
        } else {
            Rlog.d(TAG, "channel size=0");
        }
        if (languages != null) {
            Rlog.d(TAG, "language size=" + languages.length);
        } else {
            Rlog.d(TAG, "language size=0");
        }
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                SmsCbConfigInfo[] channelInfos = null;
                SmsCbConfigInfo[] languageInfos = null;
                if (channels != null && channels.length != 0) {
                    channelInfos = new SmsCbConfigInfo[channels.length];
                    for (int i = 0; i < channels.length; i++) {
                        channelInfos[i] = Convert2SmsCbConfigInfo(channels[i]);
                    }
                }
                if (languages != null && languages.length != 0) {
                    languageInfos = new SmsCbConfigInfo[languages.length];
                    for (int i2 = 0; i2 < languages.length; i2++) {
                        languageInfos[i2] = Convert2SmsCbConfigInfo(languages[i2]);
                    }
                }
                boolean result = iccISms.setCellBroadcastSmsConfigForSubscriber(getSubscriptionId(), channelInfos, languageInfos);
                return result;
            }
            Rlog.d(TAG, "fail to get sms service");
            return false;
        } catch (RemoteException e) {
            Rlog.d(TAG, "setCellBroadcastSmsConfig, RemoteException!");
            return false;
        }
    }

    public boolean queryCellBroadcastSmsActivation() {
        Rlog.d(TAG, "queryCellBroadcastSmsActivation");
        Rlog.d(TAG, "subId=" + getSubscriptionId());
        boolean result = false;
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                result = iccISms.queryCellBroadcastSmsActivationForSubscriber(getSubscriptionId());
            } else {
                Rlog.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoteException!");
        }
        return result;
    }

    public boolean activateCellBroadcastSms(boolean activate) {
        boolean result;
        Rlog.d(TAG, "activateCellBroadcastSms activate : " + activate + ", sub = " + getSubscriptionId());
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                result = iccISms.activateCellBroadcastSmsForSubscriber(getSubscriptionId(), activate);
            } else {
                Rlog.d(TAG, "fail to get sms service");
                result = false;
            }
            return result;
        } catch (RemoteException e) {
            Rlog.d(TAG, "fail to activate CB");
            return false;
        }
    }

    public boolean removeCellBroadcastMsg(int channelId, int serialId) {
        Rlog.d(TAG, "RemoveCellBroadcastMsg, subId=" + getSubscriptionId());
        boolean result = false;
        try {
            ISms iccISms = getISmsServiceOrThrow();
            if (iccISms != null) {
                result = iccISms.removeCellBroadcastMsgForSubscriber(getSubscriptionId(), channelId, serialId);
            } else {
                Rlog.d(TAG, "fail to get sms service");
                result = false;
            }
        } catch (RemoteException e) {
            Rlog.d(TAG, "RemoveCellBroadcastMsg, RemoteException!");
        }
        return result;
    }
}
