package com.android.mms.service;

import android.app.ActivityManagerNative;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.service.carrier.ICarrierMessagingService;
import android.telephony.CarrierMessagingServiceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.mms.service.MmsRequest;
import com.android.mms.service.exception.MmsHttpException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.util.SqliteWrapper;
import java.util.HashMap;

public class DownloadRequest extends MmsRequest {
    private final Uri mContentUri;
    private final PendingIntent mDownloadedIntent;
    private final String mLocationUrl;

    public DownloadRequest(MmsRequest.RequestManager manager, int subId, String locationUrl, Uri contentUri, PendingIntent downloadedIntent, String creator, Bundle configOverrides) {
        super(manager, subId, creator, configOverrides);
        this.mLocationUrl = locationUrl;
        this.mDownloadedIntent = downloadedIntent;
        this.mContentUri = contentUri;
    }

    @Override
    protected byte[] doHttp(Context context, MmsNetworkManager netMgr, ApnSettings apn) throws MmsHttpException {
        MmsHttpClient mmsHttpClient = netMgr.getOrCreateHttpClient();
        if (mmsHttpClient == null) {
            Log.e("MmsService", "MMS network is not ready!");
            throw new MmsHttpException(0, "MMS network is not ready");
        }
        return mmsHttpClient.execute(this.mLocationUrl, null, "GET", apn.isProxySet(), apn.getProxyAddress(), apn.getProxyPort(), this.mMmsConfig);
    }

    @Override
    protected PendingIntent getPendingIntent() {
        return this.mDownloadedIntent;
    }

    @Override
    protected int getQueueType() {
        return 1;
    }

    @Override
    protected Uri persistIfRequired(Context context, int result, byte[] response) {
        notifyOfDownload(context);
        if (!this.mRequestManager.getAutoPersistingPref()) {
            return null;
        }
        Log.d("MmsService", "DownloadRequest.persistIfRequired");
        if (response == null || response.length < 1) {
            Log.e("MmsService", "DownloadRequest.persistIfRequired: empty response");
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            RetrieveConf retrieveConf = new PduParser(response, this.mMmsConfig.getSupportMmsContentDisposition()).parse();
            if (retrieveConf == null || !(retrieveConf instanceof RetrieveConf)) {
                Log.e("MmsService", "DownloadRequest.persistIfRequired: invalid parsed PDU");
                return null;
            }
            RetrieveConf retrieveConf2 = retrieveConf;
            int status = retrieveConf2.getRetrieveStatus();
            if (status != 128) {
                Log.e("MmsService", "DownloadRequest.persistIfRequired: retrieve failed " + status);
                ContentValues values = new ContentValues(1);
                values.put("retr_st", Integer.valueOf(status));
                SqliteWrapper.update(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, values, "m_type=? AND ct_l =?", new String[]{Integer.toString(130), this.mLocationUrl});
                return null;
            }
            PduPersister persister = PduPersister.getPduPersister(context);
            Uri messageUri = persister.persist(retrieveConf, Telephony.Mms.Inbox.CONTENT_URI, true, true, (HashMap) null);
            if (messageUri == null) {
                Log.e("MmsService", "DownloadRequest.persistIfRequired: can not persist message");
                return null;
            }
            ContentValues values2 = new ContentValues();
            values2.put("date", Long.valueOf(System.currentTimeMillis() / 1000));
            values2.put("read", (Integer) 0);
            values2.put("seen", (Integer) 0);
            if (!TextUtils.isEmpty(this.mCreator)) {
                values2.put("creator", this.mCreator);
            }
            values2.put("sub_id", Integer.valueOf(this.mSubId));
            if (SqliteWrapper.update(context, context.getContentResolver(), messageUri, values2, (String) null, (String[]) null) != 1) {
                Log.e("MmsService", "DownloadRequest.persistIfRequired: can not update message");
            }
            SqliteWrapper.delete(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, "m_type=? AND ct_l =?", new String[]{Integer.toString(130), this.mLocationUrl});
            return messageUri;
        } catch (SQLiteException e) {
            Log.e("MmsService", "DownloadRequest.persistIfRequired: can not update message", e);
            return null;
        } catch (RuntimeException e2) {
            Log.e("MmsService", "DownloadRequest.persistIfRequired: can not parse response", e2);
            return null;
        } catch (MmsException e3) {
            Log.e("MmsService", "DownloadRequest.persistIfRequired: can not persist message", e3);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void notifyOfDownload(Context context) {
        UserInfo info;
        Intent intent = new Intent("android.provider.Telephony.MMS_DOWNLOADED");
        intent.addFlags(134217728);
        int[] users = null;
        try {
            users = ActivityManagerNative.getDefault().getRunningUserIds();
        } catch (RemoteException e) {
        }
        if (users == null) {
            users = new int[]{UserHandle.ALL.getIdentifier()};
        }
        UserManager userManager = (UserManager) context.getSystemService("user");
        for (int i = users.length - 1; i >= 0; i--) {
            UserHandle targetUser = new UserHandle(users[i]);
            if (users[i] == 0 || (!userManager.hasUserRestriction("no_sms", targetUser) && (info = userManager.getUserInfo(users[i])) != null && !info.isManagedProfile())) {
                context.sendOrderedBroadcastAsUser(intent, targetUser, "android.permission.RECEIVE_MMS", 18, null, null, -1, null, null);
            }
        }
    }

    @Override
    protected boolean transferResponse(Intent fillIn, byte[] response) {
        return this.mRequestManager.writePduToContentUri(this.mContentUri, response);
    }

    @Override
    protected boolean prepareForHttpRequest() {
        return true;
    }

    public void tryDownloadingByCarrierApp(Context context, String carrierMessagingServicePackage) {
        CarrierDownloadManager carrierDownloadManger = new CarrierDownloadManager();
        CarrierDownloadCompleteCallback downloadCallback = new CarrierDownloadCompleteCallback(context, carrierDownloadManger);
        carrierDownloadManger.downloadMms(context, carrierMessagingServicePackage, downloadCallback);
    }

    @Override
    protected void revokeUriPermission(Context context) {
        context.revokeUriPermission(this.mContentUri, 2);
    }

    private final class CarrierDownloadManager extends CarrierMessagingServiceManager {
        private volatile CarrierDownloadCompleteCallback mCarrierDownloadCallback;

        private CarrierDownloadManager() {
        }

        void downloadMms(Context context, String carrierMessagingServicePackage, CarrierDownloadCompleteCallback carrierDownloadCallback) {
            this.mCarrierDownloadCallback = carrierDownloadCallback;
            if (bindToCarrierMessagingService(context, carrierMessagingServicePackage)) {
                Log.v("MmsService", "bindService() for carrier messaging service succeeded");
            } else {
                Log.e("MmsService", "bindService() for carrier messaging service failed");
                carrierDownloadCallback.onDownloadMmsComplete(1);
            }
        }

        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            try {
                carrierMessagingService.downloadMms(DownloadRequest.this.mContentUri, DownloadRequest.this.mSubId, Uri.parse(DownloadRequest.this.mLocationUrl), this.mCarrierDownloadCallback);
            } catch (RemoteException e) {
                Log.e("MmsService", "Exception downloading MMS using the carrier messaging service: " + e);
                this.mCarrierDownloadCallback.onDownloadMmsComplete(1);
            }
        }
    }

    private final class CarrierDownloadCompleteCallback extends MmsRequest.CarrierMmsActionCallback {
        private final CarrierDownloadManager mCarrierDownloadManager;
        private final Context mContext;

        public CarrierDownloadCompleteCallback(Context context, CarrierDownloadManager carrierDownloadManager) {
            super();
            this.mContext = context;
            this.mCarrierDownloadManager = carrierDownloadManager;
        }

        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            Log.e("MmsService", "Unexpected onSendMmsComplete call with result: " + result);
        }

        public void onDownloadMmsComplete(int result) {
            Log.d("MmsService", "Carrier app result for download: " + result);
            this.mCarrierDownloadManager.disposeConnection(this.mContext);
            if (!DownloadRequest.this.maybeFallbackToRegularDelivery(result)) {
                DownloadRequest.this.processResult(this.mContext, MmsRequest.toSmsManagerResult(result), null, 0);
            }
        }
    }
}
