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
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.SmsApplication;
import com.android.mms.service.MmsRequest;
import com.android.mms.service.exception.MmsHttpException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
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

    public SendRequest(MmsRequest.RequestManager manager, int subId, Uri contentUri, String locationUrl, PendingIntent sentIntent, String creator, Bundle configOverrides) {
        super(manager, subId, creator, configOverrides);
        this.mPduUri = contentUri;
        this.mPduData = null;
        this.mLocationUrl = locationUrl;
        this.mSentIntent = sentIntent;
    }

    @Override
    protected byte[] doHttp(Context context, MmsNetworkManager netMgr, ApnSettings apn) throws MmsHttpException {
        MmsHttpClient mmsHttpClient = netMgr.getOrCreateHttpClient();
        if (mmsHttpClient != null) {
            return mmsHttpClient.execute(this.mLocationUrl != null ? this.mLocationUrl : apn.getMmscUrl(), this.mPduData, "POST", apn.isProxySet(), apn.getProxyAddress(), apn.getProxyPort(), this.mMmsConfig);
        }
        Log.e("MmsService", "MMS network is not ready!");
        throw new MmsHttpException(0, "MMS network is not ready");
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
    protected Uri persistIfRequired(Context context, int result, byte[] response) {
        GenericPdu pdu;
        if (!SmsApplication.shouldWriteMessageForPackage(this.mCreator, context)) {
            return null;
        }
        Log.d("MmsService", "SendRequest.persistIfRequired");
        if (this.mPduData == null) {
            Log.e("MmsService", "SendRequest.persistIfRequired: empty PDU");
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            boolean supportContentDisposition = this.mMmsConfig.getSupportMmsContentDisposition();
            GenericPdu pdu2 = new PduParser(this.mPduData, supportContentDisposition).parse();
            if (pdu2 == null) {
                Log.e("MmsService", "SendRequest.persistIfRequired: can't parse input PDU");
                return null;
            }
            if (!(pdu2 instanceof SendReq)) {
                Log.d("MmsService", "SendRequest.persistIfRequired: not SendReq");
                return null;
            }
            PduPersister persister = PduPersister.getPduPersister(context);
            Uri messageUri = persister.persist(pdu2, Telephony.Mms.Sent.CONTENT_URI, true, true, (HashMap) null);
            if (messageUri == null) {
                Log.e("MmsService", "SendRequest.persistIfRequired: can not persist message");
                return null;
            }
            ContentValues values = new ContentValues();
            SendConf sendConf = null;
            if (response != null && response.length > 0 && (pdu = new PduParser(response, supportContentDisposition).parse()) != null && (pdu instanceof SendConf)) {
                sendConf = (SendConf) pdu;
            }
            if (result != -1 || sendConf == null || sendConf.getResponseStatus() != 128) {
                values.put("msg_box", (Integer) 5);
            }
            if (sendConf != null) {
                values.put("resp_st", Integer.valueOf(sendConf.getResponseStatus()));
                values.put("m_id", PduPersister.toIsoString(sendConf.getMessageId()));
            }
            values.put("date", Long.valueOf(System.currentTimeMillis() / 1000));
            values.put("read", (Integer) 1);
            values.put("seen", (Integer) 1);
            if (!TextUtils.isEmpty(this.mCreator)) {
                values.put("creator", this.mCreator);
            }
            values.put("sub_id", Integer.valueOf(this.mSubId));
            if (SqliteWrapper.update(context, context.getContentResolver(), messageUri, values, (String) null, (String[]) null) != 1) {
                Log.e("MmsService", "SendRequest.persistIfRequired: failed to update message");
            }
            return messageUri;
        } catch (MmsException e) {
            Log.e("MmsService", "SendRequest.persistIfRequired: can not persist message", e);
            return null;
        } catch (RuntimeException e2) {
            Log.e("MmsService", "SendRequest.persistIfRequired: unexpected parsing failure", e2);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean readPduFromContentUri() {
        if (this.mPduData != null) {
            return true;
        }
        int bytesTobeRead = this.mMmsConfig.getMaxMessageSize();
        this.mPduData = this.mRequestManager.readPduFromContentUri(this.mPduUri, bytesTobeRead);
        return this.mPduData != null;
    }

    @Override
    protected boolean transferResponse(Intent fillIn, byte[] response) {
        if (response != null) {
            fillIn.putExtra("android.telephony.extra.MMS_DATA", response);
            return true;
        }
        return true;
    }

    @Override
    protected boolean prepareForHttpRequest() {
        return readPduFromContentUri();
    }

    public void trySendingByCarrierApp(Context context, String carrierMessagingServicePackage) {
        CarrierSendManager carrierSendManger = new CarrierSendManager();
        CarrierSendCompleteCallback sendCallback = new CarrierSendCompleteCallback(context, carrierSendManger);
        carrierSendManger.sendMms(context, carrierMessagingServicePackage, sendCallback);
    }

    @Override
    protected void revokeUriPermission(Context context) {
        context.revokeUriPermission(this.mPduUri, 1);
    }

    private final class CarrierSendManager extends CarrierMessagingServiceManager {
        private volatile CarrierSendCompleteCallback mCarrierSendCompleteCallback;

        private CarrierSendManager() {
        }

        void sendMms(Context context, String carrierMessagingServicePackage, CarrierSendCompleteCallback carrierSendCompleteCallback) {
            this.mCarrierSendCompleteCallback = carrierSendCompleteCallback;
            if (bindToCarrierMessagingService(context, carrierMessagingServicePackage)) {
                Log.v("MmsService", "bindService() for carrier messaging service succeeded");
            } else {
                Log.e("MmsService", "bindService() for carrier messaging service failed");
                carrierSendCompleteCallback.onSendMmsComplete(1, null);
            }
        }

        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            Uri locationUri = null;
            try {
                if (SendRequest.this.mLocationUrl != null) {
                    locationUri = Uri.parse(SendRequest.this.mLocationUrl);
                }
                carrierMessagingService.sendMms(SendRequest.this.mPduUri, SendRequest.this.mSubId, locationUri, this.mCarrierSendCompleteCallback);
            } catch (RemoteException e) {
                Log.e("MmsService", "Exception sending MMS using the carrier messaging service: " + e);
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

        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            Log.d("MmsService", "Carrier app result for send: " + result);
            this.mCarrierSendManager.disposeConnection(this.mContext);
            if (!SendRequest.this.maybeFallbackToRegularDelivery(result)) {
                SendRequest.this.processResult(this.mContext, MmsRequest.toSmsManagerResult(result), sendConfPdu, 0);
            }
        }

        public void onDownloadMmsComplete(int result) {
            Log.e("MmsService", "Unexpected onDownloadMmsComplete call with result: " + result);
        }
    }
}
