package android.telephony;

import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.contexthub.V1_0.HostEndPoint;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.telephony.IMms;
import com.android.internal.telephony.ISms;
import com.android.internal.telephony.SmsRawData;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class SmsManager {
    public static final int CDMA_SMS_RECORD_LENGTH = 255;
    public static final int CELL_BROADCAST_RAN_TYPE_CDMA = 1;
    public static final int CELL_BROADCAST_RAN_TYPE_GSM = 0;
    private static final int DEFAULT_SUBSCRIPTION_ID = -1002;
    public static final String EXTRA_MMS_DATA = "android.telephony.extra.MMS_DATA";
    public static final String EXTRA_MMS_HTTP_STATUS = "android.telephony.extra.MMS_HTTP_STATUS";
    public static final String MESSAGE_STATUS_READ = "read";
    public static final String MESSAGE_STATUS_SEEN = "seen";
    public static final String MMS_CONFIG_ALIAS_ENABLED = "aliasEnabled";
    public static final String MMS_CONFIG_ALIAS_MAX_CHARS = "aliasMaxChars";
    public static final String MMS_CONFIG_ALIAS_MIN_CHARS = "aliasMinChars";
    public static final String MMS_CONFIG_ALLOW_ATTACH_AUDIO = "allowAttachAudio";
    public static final String MMS_CONFIG_APPEND_TRANSACTION_ID = "enabledTransID";
    public static final String MMS_CONFIG_CLOSE_CONNECTION = "mmsCloseConnection";
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

    @SystemApi
    public static final int RESULT_CANCELLED = 23;

    @SystemApi
    public static final int RESULT_ENCODING_ERROR = 18;

    @SystemApi
    public static final int RESULT_ERROR_FDN_CHECK_FAILURE = 6;
    public static final int RESULT_ERROR_GENERIC_FAILURE = 1;
    public static final int RESULT_ERROR_LIMIT_EXCEEDED = 5;

    @SystemApi
    public static final int RESULT_ERROR_NONE = 0;
    public static final int RESULT_ERROR_NO_SERVICE = 4;
    public static final int RESULT_ERROR_NULL_PDU = 3;
    public static final int RESULT_ERROR_RADIO_OFF = 2;
    public static final int RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED = 8;
    public static final int RESULT_ERROR_SHORT_CODE_NOT_ALLOWED = 7;

    @SystemApi
    public static final int RESULT_INTERNAL_ERROR = 21;

    @SystemApi
    public static final int RESULT_INVALID_ARGUMENTS = 11;

    @SystemApi
    public static final int RESULT_INVALID_SMSC_ADDRESS = 19;

    @SystemApi
    public static final int RESULT_INVALID_SMS_FORMAT = 14;

    @SystemApi
    public static final int RESULT_INVALID_STATE = 12;

    @SystemApi
    public static final int RESULT_MODEM_ERROR = 16;

    @SystemApi
    public static final int RESULT_NETWORK_ERROR = 17;

    @SystemApi
    public static final int RESULT_NETWORK_REJECT = 10;

    @SystemApi
    public static final int RESULT_NO_MEMORY = 13;

    @SystemApi
    public static final int RESULT_NO_RESOURCES = 22;

    @SystemApi
    public static final int RESULT_OPERATION_NOT_ALLOWED = 20;

    @SystemApi
    public static final int RESULT_RADIO_NOT_AVAILABLE = 9;

    @SystemApi
    public static final int RESULT_REQUEST_NOT_SUPPORTED = 24;

    @SystemApi
    public static final int RESULT_SYSTEM_ERROR = 15;
    private static final int SMS_PICK = 2;
    public static final int SMS_RECORD_LENGTH = 176;
    public static final int SMS_TYPE_INCOMING = 0;
    public static final int SMS_TYPE_OUTGOING = 1;
    public static final int STATUS_ON_ICC_FREE = 0;
    public static final int STATUS_ON_ICC_READ = 1;
    public static final int STATUS_ON_ICC_SENT = 5;
    public static final int STATUS_ON_ICC_UNREAD = 3;
    public static final int STATUS_ON_ICC_UNSENT = 7;
    private static final String TAG = "SmsManager";
    private int mSubId;
    private static final SmsManager sInstance = new SmsManager(-1002);
    private static final Object sLockObject = new Object();
    private static final Map<Integer, SmsManager> sSubInstances = new ArrayMap();
    private static String DIALOG_TYPE_KEY = "dialog_type";

    public void sendTextMessage(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        sendTextMessageInternal(str, str2, str3, pendingIntent, pendingIntent2, true);
    }

    private void sendTextMessageInternal(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (TextUtils.isEmpty(str3)) {
            throw new IllegalArgumentException("Invalid message body");
        }
        try {
            getISmsServiceOrThrow().sendTextForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), str, str2, str3, pendingIntent, pendingIntent2, z);
        } catch (RemoteException e) {
        }
    }

    @SystemApi
    public void sendTextMessageWithoutPersisting(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        sendTextMessageInternal(str, str2, str3, pendingIntent, pendingIntent2, false);
    }

    public void sendTextMessageWithSelfPermissions(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (TextUtils.isEmpty(str3)) {
            throw new IllegalArgumentException("Invalid message body");
        }
        try {
            getISmsServiceOrThrow().sendTextForSubscriberWithSelfPermissions(getSubscriptionId(), ActivityThread.currentPackageName(), str, str2, str3, pendingIntent, pendingIntent2, z);
        } catch (RemoteException e) {
        }
    }

    public void sendTextMessage(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, int i, boolean z, int i2) {
        sendTextMessageInternal(str, str2, str3, pendingIntent, pendingIntent2, true, i, z, i2);
    }

    private void sendTextMessageInternal(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z, int i, boolean z2, int i2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (TextUtils.isEmpty(str3)) {
            throw new IllegalArgumentException("Invalid message body");
        }
        if (i >= 0 && i <= 3) {
            if (i2 < 5 || i2 > 635040) {
                throw new IllegalArgumentException("Invalid validity period");
            }
            try {
                ISms iSmsServiceOrThrow = getISmsServiceOrThrow();
                if (iSmsServiceOrThrow != null) {
                    iSmsServiceOrThrow.sendTextForSubscriberWithOptions(getSubscriptionId(), ActivityThread.currentPackageName(), str, str2, str3, pendingIntent, pendingIntent2, z, i, z2, i2);
                    return;
                }
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid priority");
    }

    public void sendTextMessageWithoutPersisting(String str, String str2, String str3, PendingIntent pendingIntent, PendingIntent pendingIntent2, int i, boolean z, int i2) {
        sendTextMessageInternal(str, str2, str3, pendingIntent, pendingIntent2, false, i, z, i2);
    }

    public void injectSmsPdu(byte[] bArr, String str, PendingIntent pendingIntent) {
        if (!str.equals("3gpp") && !str.equals("3gpp2")) {
            throw new IllegalArgumentException("Invalid pdu format. format must be either 3gpp or 3gpp2");
        }
        try {
            ISms iSmsAsInterface = ISms.Stub.asInterface(ServiceManager.getService("isms"));
            if (iSmsAsInterface != null) {
                iSmsAsInterface.injectSmsPduForSubscriber(getSubscriptionId(), bArr, str, pendingIntent);
            }
        } catch (RemoteException e) {
        }
    }

    public ArrayList<String> divideMessage(String str) {
        if (str == null) {
            throw new IllegalArgumentException("text is null");
        }
        return SmsMessage.fragmentText(str);
    }

    public void sendMultipartTextMessage(String str, String str2, ArrayList<String> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3) {
        sendMultipartTextMessageInternal(str, str2, arrayList, arrayList2, arrayList3, true);
    }

    private void sendMultipartTextMessageInternal(String str, String str2, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) {
        PendingIntent pendingIntent;
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (list == null || list.size() < 1) {
            throw new IllegalArgumentException("Invalid message body");
        }
        if (list.size() > 1) {
            try {
                getISmsServiceOrThrow().sendMultipartTextForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), str, str2, list, list2, list3, z);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        PendingIntent pendingIntent2 = null;
        if (list2 != null && list2.size() > 0) {
            pendingIntent = list2.get(0);
        } else {
            pendingIntent = null;
        }
        if (list3 != null && list3.size() > 0) {
            pendingIntent2 = list3.get(0);
        }
        sendTextMessage(str, str2, list.get(0), pendingIntent, pendingIntent2);
    }

    @SystemApi
    public void sendMultipartTextMessageWithoutPersisting(String str, String str2, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3) {
        sendMultipartTextMessageInternal(str, str2, list, list2, list3, false);
    }

    public void sendMultipartTextMessage(String str, String str2, ArrayList<String> arrayList, ArrayList<PendingIntent> arrayList2, ArrayList<PendingIntent> arrayList3, int i, boolean z, int i2) {
        sendMultipartTextMessageInternal(str, str2, arrayList, arrayList2, arrayList3, true);
    }

    private void sendMultipartTextMessageInternal(String str, String str2, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, boolean z, int i, boolean z2, int i2) {
        PendingIntent pendingIntent;
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (list == null || list.size() < 1) {
            throw new IllegalArgumentException("Invalid message body");
        }
        if (i >= 0 && i <= 3) {
            if (i2 < 5 || i2 > 635040) {
                throw new IllegalArgumentException("Invalid validity period");
            }
            if (list.size() > 1) {
                try {
                    ISms iSmsServiceOrThrow = getISmsServiceOrThrow();
                    if (iSmsServiceOrThrow != null) {
                        iSmsServiceOrThrow.sendMultipartTextForSubscriberWithOptions(getSubscriptionId(), ActivityThread.currentPackageName(), str, str2, list, list2, list3, z, i, z2, i2);
                        return;
                    }
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            PendingIntent pendingIntent2 = null;
            if (list2 == null || list2.size() <= 0) {
                pendingIntent = null;
            } else {
                pendingIntent = list2.get(0);
            }
            if (list3 != null && list3.size() > 0) {
                pendingIntent2 = list3.get(0);
            }
            sendTextMessageInternal(str, str2, list.get(0), pendingIntent, pendingIntent2, z, i, z2, i2);
            return;
        }
        throw new IllegalArgumentException("Invalid priority");
    }

    public void sendMultipartTextMessageWithoutPersisting(String str, String str2, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3, int i, boolean z, int i2) {
        sendMultipartTextMessageInternal(str, str2, list, list2, list3, false, i, z, i2);
    }

    public void sendDataMessage(String str, String str2, short s, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (bArr == null || bArr.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }
        try {
            getISmsServiceOrThrow().sendDataForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), str, str2, s & HostEndPoint.BROADCAST, bArr, pendingIntent, pendingIntent2);
        } catch (RemoteException e) {
        }
    }

    public void sendDataMessageWithSelfPermissions(String str, String str2, short s, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Invalid destinationAddress");
        }
        if (bArr == null || bArr.length == 0) {
            throw new IllegalArgumentException("Invalid message data");
        }
        try {
            getISmsServiceOrThrow().sendDataForSubscriberWithSelfPermissions(getSubscriptionId(), ActivityThread.currentPackageName(), str, str2, s & HostEndPoint.BROADCAST, bArr, pendingIntent, pendingIntent2);
        } catch (RemoteException e) {
        }
    }

    public static SmsManager getDefault() {
        return sInstance;
    }

    public static SmsManager getSmsManagerForSubscriptionId(int i) {
        SmsManager smsManager;
        synchronized (sLockObject) {
            smsManager = sSubInstances.get(Integer.valueOf(i));
            if (smsManager == null) {
                smsManager = new SmsManager(i);
                sSubInstances.put(Integer.valueOf(i), smsManager);
            }
        }
        return smsManager;
    }

    private SmsManager(int i) {
        this.mSubId = i;
    }

    public int getSubscriptionId() throws RemoteException {
        int defaultSmsSubscriptionId = this.mSubId == -1002 ? getDefaultSmsSubscriptionId() : this.mSubId;
        boolean zIsSmsSimPickActivityNeeded = false;
        Context applicationContext = ActivityThread.currentApplication().getApplicationContext();
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService != null) {
                zIsSmsSimPickActivityNeeded = iSmsService.isSmsSimPickActivityNeeded(defaultSmsSubscriptionId);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in getSubscriptionId");
        }
        if (checkSimPickActivityNeeded(zIsSmsSimPickActivityNeeded)) {
            Log.d(TAG, "getSubscriptionId isSmsSimPickActivityNeeded is true");
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.sim.SimDialogActivity");
            intent.addFlags(268435456);
            intent.putExtra(DIALOG_TYPE_KEY, 2);
            try {
                applicationContext.startActivity(intent);
            } catch (ActivityNotFoundException e2) {
                Log.e(TAG, "Unable to launch Settings application.");
            }
        }
        return defaultSmsSubscriptionId;
    }

    private static ISms getISmsServiceOrThrow() {
        ISms iSmsService = getISmsService();
        if (iSmsService == null) {
            throw new UnsupportedOperationException("Sms is not supported");
        }
        return iSmsService;
    }

    private static ISms getISmsService() {
        return ISms.Stub.asInterface(ServiceManager.getService("isms"));
    }

    public boolean copyMessageToIcc(byte[] bArr, byte[] bArr2, int i) {
        if (bArr2 == null) {
            throw new IllegalArgumentException("pdu is NULL");
        }
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.copyMessageToIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), i, bArr2, bArr);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean deleteMessageFromIcc(int i) {
        byte[] bArr = new byte[175];
        Arrays.fill(bArr, (byte) -1);
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.updateMessageOnIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), i, 0, bArr);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean updateMessageOnIcc(int i, int i2, byte[] bArr) {
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.updateMessageOnIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName(), i, i2, bArr);
        } catch (RemoteException e) {
            return false;
        }
    }

    public ArrayList<SmsMessage> getAllMessagesFromIcc() throws RemoteException {
        List<SmsRawData> allMessagesFromIccEfForSubscriber = null;
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService != null) {
                allMessagesFromIccEfForSubscriber = iSmsService.getAllMessagesFromIccEfForSubscriber(getSubscriptionId(), ActivityThread.currentPackageName());
            }
        } catch (RemoteException e) {
        }
        return createMessageListFromRawRecords(allMessagesFromIccEfForSubscriber);
    }

    public boolean enableCellBroadcast(int i, int i2) {
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.enableCellBroadcastForSubscriber(getSubscriptionId(), i, i2);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disableCellBroadcast(int i, int i2) {
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.disableCellBroadcastForSubscriber(getSubscriptionId(), i, i2);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean enableCellBroadcastRange(int i, int i2, int i3) {
        if (i2 < i) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.enableCellBroadcastRangeForSubscriber(getSubscriptionId(), i, i2, i3);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean disableCellBroadcastRange(int i, int i2, int i3) {
        if (i2 < i) {
            throw new IllegalArgumentException("endMessageId < startMessageId");
        }
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.disableCellBroadcastRangeForSubscriber(getSubscriptionId(), i, i2, i3);
        } catch (RemoteException e) {
            return false;
        }
    }

    private ArrayList<SmsMessage> createMessageListFromRawRecords(List<SmsRawData> list) {
        SmsMessage smsMessageCreateFromEfRecord;
        ArrayList<SmsMessage> arrayList = new ArrayList<>();
        if (list != null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                SmsRawData smsRawData = list.get(i);
                if (smsRawData != null && (smsMessageCreateFromEfRecord = SmsMessage.createFromEfRecord(i + 1, smsRawData.getBytes(), getSubscriptionId())) != null) {
                    arrayList.add(smsMessageCreateFromEfRecord);
                }
            }
        }
        return arrayList;
    }

    public boolean isImsSmsSupported() {
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return false;
            }
            return iSmsService.isImsSmsSupportedForSubscriber(getSubscriptionId());
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getImsSmsFormat() {
        try {
            ISms iSmsService = getISmsService();
            if (iSmsService == null) {
                return "unknown";
            }
            return iSmsService.getImsSmsFormatForSubscriber(getSubscriptionId());
        } catch (RemoteException e) {
            return "unknown";
        }
    }

    public static int getDefaultSmsSubscriptionId() {
        try {
            return ISms.Stub.asInterface(ServiceManager.getService("isms")).getPreferredSmsSubscription();
        } catch (RemoteException e) {
            return -1;
        } catch (NullPointerException e2) {
            return -1;
        }
    }

    public boolean isSMSPromptEnabled() {
        try {
            return ISms.Stub.asInterface(ServiceManager.getService("isms")).isSMSPromptEnabled();
        } catch (RemoteException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public void sendMultimediaMessage(Context context, Uri uri, String str, Bundle bundle, PendingIntent pendingIntent) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface == null) {
                return;
            }
            iMmsAsInterface.sendMessage(getSubscriptionId(), ActivityThread.currentPackageName(), uri, str, bundle, pendingIntent);
        } catch (RemoteException e) {
        }
    }

    public void downloadMultimediaMessage(Context context, String str, Uri uri, Bundle bundle, PendingIntent pendingIntent) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Empty MMS location URL");
        }
        if (uri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface == null) {
                return;
            }
            iMmsAsInterface.downloadMessage(getSubscriptionId(), ActivityThread.currentPackageName(), str, uri, bundle, pendingIntent);
        } catch (RemoteException e) {
        }
    }

    public Uri importTextMessage(String str, int i, String str2, long j, boolean z, boolean z2) {
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.importTextMessage(ActivityThread.currentPackageName(), str, i, str2, j, z, z2);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public Uri importMultimediaMessage(Uri uri, String str, long j, boolean z, boolean z2) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.importMultimediaMessage(ActivityThread.currentPackageName(), uri, str, j, z, z2);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean deleteStoredMessage(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.deleteStoredMessage(ActivityThread.currentPackageName(), uri);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean deleteStoredConversation(long j) {
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.deleteStoredConversation(ActivityThread.currentPackageName(), j);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean updateStoredMessageStatus(Uri uri, ContentValues contentValues) {
        if (uri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.updateStoredMessageStatus(ActivityThread.currentPackageName(), uri, contentValues);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean archiveStoredConversation(long j, boolean z) {
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.archiveStoredConversation(ActivityThread.currentPackageName(), j, z);
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public Uri addTextMessageDraft(String str, String str2) {
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.addTextMessageDraft(ActivityThread.currentPackageName(), str, str2);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public Uri addMultimediaMessageDraft(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Uri contentUri null");
        }
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.addMultimediaMessageDraft(ActivityThread.currentPackageName(), uri);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public void sendStoredTextMessage(Uri uri, String str, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (uri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            getISmsServiceOrThrow().sendStoredText(getSubscriptionId(), ActivityThread.currentPackageName(), uri, str, pendingIntent, pendingIntent2);
        } catch (RemoteException e) {
        }
    }

    public void sendStoredMultipartTextMessage(Uri uri, String str, ArrayList<PendingIntent> arrayList, ArrayList<PendingIntent> arrayList2) {
        if (uri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            getISmsServiceOrThrow().sendStoredMultipartText(getSubscriptionId(), ActivityThread.currentPackageName(), uri, str, arrayList, arrayList2);
        } catch (RemoteException e) {
        }
    }

    public void sendStoredMultimediaMessage(Uri uri, Bundle bundle, PendingIntent pendingIntent) {
        if (uri == null) {
            throw new IllegalArgumentException("Empty message URI");
        }
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                iMmsAsInterface.sendStoredMessage(getSubscriptionId(), ActivityThread.currentPackageName(), uri, bundle, pendingIntent);
            }
        } catch (RemoteException e) {
        }
    }

    public void setAutoPersisting(boolean z) {
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                iMmsAsInterface.setAutoPersisting(ActivityThread.currentPackageName(), z);
            }
        } catch (RemoteException e) {
        }
    }

    public boolean getAutoPersisting() {
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.getAutoPersisting();
            }
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public Bundle getCarrierConfigValues() {
        try {
            IMms iMmsAsInterface = IMms.Stub.asInterface(ServiceManager.getService("imms"));
            if (iMmsAsInterface != null) {
                return iMmsAsInterface.getCarrierConfigValues(getSubscriptionId());
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public String createAppSpecificSmsToken(PendingIntent pendingIntent) {
        try {
            return getISmsServiceOrThrow().createAppSpecificSmsToken(getSubscriptionId(), ActivityThread.currentPackageName(), pendingIntent);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return null;
        }
    }

    public static Bundle getMmsConfig(BaseBundle baseBundle) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("enabledTransID", baseBundle.getBoolean("enabledTransID"));
        bundle.putBoolean("enabledMMS", baseBundle.getBoolean("enabledMMS"));
        bundle.putBoolean("enableGroupMms", baseBundle.getBoolean("enableGroupMms"));
        bundle.putBoolean("enabledNotifyWapMMSC", baseBundle.getBoolean("enabledNotifyWapMMSC"));
        bundle.putBoolean("aliasEnabled", baseBundle.getBoolean("aliasEnabled"));
        bundle.putBoolean("allowAttachAudio", baseBundle.getBoolean("allowAttachAudio"));
        bundle.putBoolean("enableMultipartSMS", baseBundle.getBoolean("enableMultipartSMS"));
        bundle.putBoolean("enableSMSDeliveryReports", baseBundle.getBoolean("enableSMSDeliveryReports"));
        bundle.putBoolean("supportMmsContentDisposition", baseBundle.getBoolean("supportMmsContentDisposition"));
        bundle.putBoolean("sendMultipartSmsAsSeparateMessages", baseBundle.getBoolean("sendMultipartSmsAsSeparateMessages"));
        bundle.putBoolean("enableMMSReadReports", baseBundle.getBoolean("enableMMSReadReports"));
        bundle.putBoolean("enableMMSDeliveryReports", baseBundle.getBoolean("enableMMSDeliveryReports"));
        bundle.putBoolean("mmsCloseConnection", baseBundle.getBoolean("mmsCloseConnection"));
        bundle.putInt("maxMessageSize", baseBundle.getInt("maxMessageSize"));
        bundle.putInt("maxImageWidth", baseBundle.getInt("maxImageWidth"));
        bundle.putInt("maxImageHeight", baseBundle.getInt("maxImageHeight"));
        bundle.putInt("recipientLimit", baseBundle.getInt("recipientLimit"));
        bundle.putInt("aliasMinChars", baseBundle.getInt("aliasMinChars"));
        bundle.putInt("aliasMaxChars", baseBundle.getInt("aliasMaxChars"));
        bundle.putInt("smsToMmsTextThreshold", baseBundle.getInt("smsToMmsTextThreshold"));
        bundle.putInt("smsToMmsTextLengthThreshold", baseBundle.getInt("smsToMmsTextLengthThreshold"));
        bundle.putInt("maxMessageTextSize", baseBundle.getInt("maxMessageTextSize"));
        bundle.putInt("maxSubjectLength", baseBundle.getInt("maxSubjectLength"));
        bundle.putInt("httpSocketTimeout", baseBundle.getInt("httpSocketTimeout"));
        bundle.putString("uaProfTagName", baseBundle.getString("uaProfTagName"));
        bundle.putString("userAgent", baseBundle.getString("userAgent"));
        bundle.putString("uaProfUrl", baseBundle.getString("uaProfUrl"));
        bundle.putString("httpParams", baseBundle.getString("httpParams"));
        bundle.putString("emailGatewayNumber", baseBundle.getString("emailGatewayNumber"));
        bundle.putString("naiSuffix", baseBundle.getString("naiSuffix"));
        bundle.putBoolean("config_cellBroadcastAppLinks", baseBundle.getBoolean("config_cellBroadcastAppLinks"));
        bundle.putBoolean("supportHttpCharsetHeader", baseBundle.getBoolean("supportHttpCharsetHeader"));
        return bundle;
    }

    private boolean checkSimPickActivityNeeded(boolean z) {
        try {
            Class<?> cls = Class.forName("mediatek.telephony.MtkSmsManager");
            if (cls != null) {
                Method declaredMethod = cls.getDeclaredMethod("checkSimPickActivityNeeded", Boolean.TYPE);
                if (declaredMethod != null) {
                    z = ((Boolean) declaredMethod.invoke(null, Boolean.valueOf(z))).booleanValue();
                } else {
                    Rlog.e(TAG, "checkSimPickActivityNeeded() does not exist!");
                }
            } else {
                Rlog.e(TAG, "MtkSmsManager does not exist!");
            }
        } catch (Exception e) {
            Rlog.e(TAG, "checkSimPickActivityNeeded() does not exist! " + e);
        }
        return z;
    }
}
