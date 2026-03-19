package com.android.mms.service;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Telephony;
import android.service.carrier.ICarrierMessagingService;
import android.telephony.CarrierMessagingServiceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.AsyncEmergencyContactNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsNumberUtils;
import com.android.mms.service.MmsRequest;
import com.android.mms.service.exception.MmsHttpException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendConf;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.util.SqliteWrapper;
import java.util.HashMap;

public class SendRequest extends MmsRequest {
    private final String mLocationUrl;
    private byte[] mPduData;
    private final Uri mPduUri;
    private final PendingIntent mSentIntent;

    public SendRequest(MmsRequest.RequestManager requestManager, int i, Uri uri, String str, PendingIntent pendingIntent, String str2, Bundle bundle, Context context) {
        super(requestManager, i, str2, bundle, context);
        this.mPduUri = uri;
        this.mPduData = null;
        this.mLocationUrl = str;
        this.mSentIntent = pendingIntent;
    }

    @Override
    protected byte[] doHttp(Context context, MmsNetworkManager mmsNetworkManager, ApnSettings apnSettings) throws MmsHttpException {
        String requestId = getRequestId();
        MmsHttpClient orCreateHttpClient = mmsNetworkManager.getOrCreateHttpClient();
        if (orCreateHttpClient == null) {
            LogUtil.e(requestId, "MMS network is not ready!");
            throw new MmsHttpException(0, "MMS network is not ready");
        }
        GenericPdu pdu = parsePdu();
        notifyIfEmergencyContactNoThrow(pdu);
        updateSenderAddress(pdu);
        updateDestinationAddress(pdu);
        return orCreateHttpClient.execute(this.mLocationUrl != null ? this.mLocationUrl : apnSettings.getMmscUrl(), this.mPduData, "POST", apnSettings.isProxySet(), apnSettings.getProxyAddress(), apnSettings.getProxyPort(), this.mMmsConfig, this.mSubId, requestId);
    }

    private GenericPdu parsePdu() {
        String requestId = getRequestId();
        try {
            if (this.mPduData == null) {
                LogUtil.w(requestId, "Empty PDU raw data");
                return null;
            }
            return new PduParser(this.mPduData, this.mMmsConfig.getBoolean("supportMmsContentDisposition")).parse();
        } catch (Exception e) {
            LogUtil.w(requestId, "Failed to parse PDU raw data");
            return null;
        }
    }

    private void notifyIfEmergencyContactNoThrow(GenericPdu genericPdu) {
        try {
            notifyIfEmergencyContact(genericPdu);
        } catch (Exception e) {
            LogUtil.w(getRequestId(), "Error in notifyIfEmergencyContact", e);
        }
    }

    private void notifyIfEmergencyContact(GenericPdu genericPdu) {
        if (genericPdu != null && genericPdu.getMessageType() == 128) {
            for (EncodedStringValue encodedStringValue : ((SendReq) genericPdu).getTo()) {
                if (isEmergencyNumber(encodedStringValue.getString())) {
                    LogUtil.i(getRequestId(), "Notifying emergency contact");
                    new AsyncEmergencyContactNotifier(this.mContext).execute(new Void[0]);
                    return;
                }
            }
        }
    }

    private boolean isEmergencyNumber(String str) {
        return !TextUtils.isEmpty(str) && PhoneNumberUtils.isEmergencyNumber(this.mSubId, str);
    }

    @Override
    protected PendingIntent getPendingIntent() {
        return this.mSentIntent;
    }

    @Override
    protected int getQueueType() {
        return 0;
    }

    @Override
    protected Uri persistIfRequired(Context context, int i, byte[] bArr) {
        GenericPdu genericPdu;
        SendConf sendConf;
        String requestId = getRequestId();
        if (bArr != null && bArr.length > 0 && (sendConf = new PduParser(bArr, false).parse()) != null && (sendConf instanceof SendConf)) {
            SendConf sendConf2 = sendConf;
            if (sendConf2.getMessageId() != null) {
                LogUtil.d(requestId, "persistIfRequired: messageId = " + PduPersister.toIsoString(sendConf2.getMessageId()));
            }
        }
        if (!SmsApplication.shouldWriteMessageForPackage(this.mCreator, context) || TextUtils.equals("com.android.phone", this.mCreator)) {
            return null;
        }
        LogUtil.d(requestId, "persistIfRequired");
        if (this.mPduData == null) {
            LogUtil.e(requestId, "persistIfRequired: empty PDU");
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                boolean z = this.mMmsConfig.getBoolean("supportMmsContentDisposition");
                GenericPdu genericPdu2 = new PduParser(this.mPduData, z).parse();
                if (genericPdu2 == null) {
                    LogUtil.e(requestId, "persistIfRequired: can't parse input PDU");
                    return null;
                }
                if (!(genericPdu2 instanceof SendReq)) {
                    LogUtil.d(requestId, "persistIfRequired: not SendReq");
                    return null;
                }
                Uri uriPersist = PduPersister.getPduPersister(context).persist(genericPdu2, Telephony.Mms.Sent.CONTENT_URI, true, true, (HashMap) null);
                if (uriPersist == null) {
                    LogUtil.e(requestId, "persistIfRequired: can not persist message");
                    return null;
                }
                ContentValues contentValues = new ContentValues();
                SendConf sendConf3 = (bArr == null || bArr.length <= 0 || (genericPdu = new PduParser(bArr, z).parse()) == null || !(genericPdu instanceof SendConf)) ? null : (SendConf) genericPdu;
                if (i != -1 || sendConf3 == null || sendConf3.getResponseStatus() != 128) {
                    contentValues.put("msg_box", (Integer) 5);
                }
                if (sendConf3 != null) {
                    contentValues.put("resp_st", Integer.valueOf(sendConf3.getResponseStatus()));
                    byte[] messageId = sendConf3.getMessageId();
                    if (messageId != null) {
                        contentValues.put("m_id", PduPersister.toIsoString(messageId));
                    }
                }
                contentValues.put("date", Long.valueOf(System.currentTimeMillis() / 1000));
                contentValues.put("read", (Integer) 1);
                contentValues.put("seen", (Integer) 1);
                if (!TextUtils.isEmpty(this.mCreator)) {
                    contentValues.put("creator", this.mCreator);
                }
                contentValues.put("sub_id", Integer.valueOf(this.mSubId));
                if (SqliteWrapper.update(context, context.getContentResolver(), uriPersist, contentValues, (String) null, (String[]) null) != 1) {
                    LogUtil.e(requestId, "persistIfRequired: failed to update message");
                }
                return uriPersist;
            } catch (MmsException e) {
                LogUtil.e(requestId, "persistIfRequired: can not persist message", e);
                return null;
            } catch (RuntimeException e2) {
                LogUtil.e(requestId, "persistIfRequired: unexpected parsing failure", e2);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void updateSenderAddress(GenericPdu genericPdu) {
        String line1Number;
        String requestId = getRequestId();
        if (genericPdu == null) {
            LogUtil.e(requestId, "updateSenderAddress: can't parse input PDU");
            return;
        }
        if (!(genericPdu instanceof SendReq)) {
            LogUtil.i(requestId, "updateSenderAddress: not SendReq");
            return;
        }
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            if ((genericPdu instanceof SendReq) && (line1Number = telephonyManager.getLine1Number(this.mSubId)) != null && !line1Number.isEmpty()) {
                EncodedStringValue encodedStringValue = new EncodedStringValue(line1Number);
                LogUtil.w(requestId, "updateSenderAddress set from: " + line1Number);
                genericPdu.setFrom(encodedStringValue);
                this.mPduData = new PduComposer(this.mContext, (SendReq) genericPdu).make();
            }
        } catch (Exception e) {
            LogUtil.w(requestId, "updateSenderAddress Failed to parse PDU raw data");
        }
    }

    private void updateDestinationAddress(GenericPdu genericPdu) {
        String requestId = getRequestId();
        if (genericPdu == null) {
            LogUtil.e(requestId, "updateDestinationAddress: can't parse input PDU");
            return;
        }
        if (!(genericPdu instanceof SendReq)) {
            LogUtil.i(requestId, "updateDestinationAddress: not SendReq");
            return;
        }
        SendReq sendReq = (SendReq) genericPdu;
        boolean z = true;
        boolean z2 = updateDestinationAddressPerType(sendReq, 130) || updateDestinationAddressPerType(sendReq, 151);
        if (!updateDestinationAddressPerType(sendReq, 129) && !z2) {
            z = false;
        }
        if (z) {
            this.mPduData = new PduComposer(this.mContext, sendReq).make();
        }
    }

    private boolean updateDestinationAddressPerType(SendReq sendReq, int i) {
        EncodedStringValue[] to;
        int length;
        if (i == 151) {
            to = sendReq.getTo();
        } else {
            switch (i) {
                case 129:
                    to = sendReq.getBcc();
                    break;
                case 130:
                    to = sendReq.getCc();
                    break;
                default:
                    return false;
            }
        }
        if (to == null || (length = to.length) <= 0) {
            return false;
        }
        Phone defaultPhone = PhoneFactory.getDefaultPhone();
        EncodedStringValue[] encodedStringValueArr = new EncodedStringValue[length];
        boolean z = false;
        for (int i2 = 0; i2 < length; i2++) {
            String string = to[i2].getString();
            String strFilterDestAddr = SmsNumberUtils.filterDestAddr(defaultPhone, string);
            if (!TextUtils.equals(string, strFilterDestAddr)) {
                encodedStringValueArr[i2] = new EncodedStringValue(strFilterDestAddr);
                z = true;
            } else {
                encodedStringValueArr[i2] = to[i2];
            }
        }
        if (i == 151) {
            sendReq.setTo(encodedStringValueArr);
        } else {
            switch (i) {
                case 129:
                    sendReq.setBcc(encodedStringValueArr);
                    break;
                case 130:
                    sendReq.setCc(encodedStringValueArr);
                    break;
            }
        }
        return z;
    }

    private boolean readPduFromContentUri() {
        if (this.mPduData != null) {
            return true;
        }
        this.mPduData = this.mRequestManager.readPduFromContentUri(this.mPduUri, this.mMmsConfig.getInt("maxMessageSize"));
        return this.mPduData != null;
    }

    @Override
    protected boolean transferResponse(Intent intent, byte[] bArr) {
        if (bArr != null) {
            intent.putExtra("android.telephony.extra.MMS_DATA", bArr);
            return true;
        }
        return true;
    }

    @Override
    protected boolean prepareForHttpRequest() {
        return readPduFromContentUri();
    }

    public void trySendingByCarrierApp(Context context, String str) {
        CarrierSendManager carrierSendManager = new CarrierSendManager();
        carrierSendManager.sendMms(context, str, new CarrierSendCompleteCallback(context, carrierSendManager));
    }

    @Override
    protected void revokeUriPermission(Context context) {
        if (this.mPduUri != null) {
            context.revokeUriPermission(this.mPduUri, 1);
        }
    }

    private final class CarrierSendManager extends CarrierMessagingServiceManager {
        private volatile CarrierSendCompleteCallback mCarrierSendCompleteCallback;

        private CarrierSendManager() {
        }

        void sendMms(Context context, String str, CarrierSendCompleteCallback carrierSendCompleteCallback) {
            this.mCarrierSendCompleteCallback = carrierSendCompleteCallback;
            if (bindToCarrierMessagingService(context, str)) {
                LogUtil.v("bindService() for carrier messaging service succeeded");
            } else {
                LogUtil.e("bindService() for carrier messaging service failed");
                carrierSendCompleteCallback.onSendMmsComplete(1, null);
            }
        }

        protected void onServiceReady(ICarrierMessagingService iCarrierMessagingService) {
            Uri uri;
            try {
                if (SendRequest.this.mLocationUrl != null) {
                    uri = Uri.parse(SendRequest.this.mLocationUrl);
                } else {
                    uri = null;
                }
                iCarrierMessagingService.sendMms(SendRequest.this.mPduUri, SendRequest.this.mSubId, uri, this.mCarrierSendCompleteCallback);
            } catch (RemoteException e) {
                LogUtil.e("Exception sending MMS using the carrier messaging service: " + e, e);
                this.mCarrierSendCompleteCallback.onSendMmsComplete(1, null);
            }
        }
    }

    private final class CarrierSendCompleteCallback extends MmsRequest.CarrierMmsActionCallback {
        private final CarrierSendManager mCarrierSendManager;
        private final Context mContext;

        public CarrierSendCompleteCallback(Context context, CarrierSendManager carrierSendManager) {
            super();
            this.mContext = context;
            this.mCarrierSendManager = carrierSendManager;
        }

        public void onSendMmsComplete(int i, byte[] bArr) {
            LogUtil.d("Carrier app result for send: " + i);
            this.mCarrierSendManager.disposeConnection(this.mContext);
            if (!SendRequest.this.maybeFallbackToRegularDelivery(i)) {
                SendRequest.this.processResult(this.mContext, MmsRequest.toSmsManagerResult(i), bArr, 0);
            }
        }

        public void onDownloadMmsComplete(int i) {
            LogUtil.e("Unexpected onDownloadMmsComplete call with result: " + i);
        }
    }
}
