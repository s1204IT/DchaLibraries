package com.android.mms.service;

import android.app.ActivityManager;
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

    public DownloadRequest(MmsRequest.RequestManager requestManager, int i, String str, Uri uri, PendingIntent pendingIntent, String str2, Bundle bundle, Context context) {
        super(requestManager, i, str2, bundle, context);
        this.mLocationUrl = str;
        this.mDownloadedIntent = pendingIntent;
        this.mContentUri = uri;
    }

    @Override
    protected byte[] doHttp(Context context, MmsNetworkManager mmsNetworkManager, ApnSettings apnSettings) throws MmsHttpException {
        String requestId = getRequestId();
        MmsHttpClient orCreateHttpClient = mmsNetworkManager.getOrCreateHttpClient();
        if (orCreateHttpClient == null) {
            LogUtil.e(requestId, "MMS network is not ready!");
            throw new MmsHttpException(0, "MMS network is not ready");
        }
        return orCreateHttpClient.execute(this.mLocationUrl, null, "GET", apnSettings.isProxySet(), apnSettings.getProxyAddress(), apnSettings.getProxyPort(), this.mMmsConfig, this.mSubId, requestId);
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
    protected Uri persistIfRequired(Context context, int i, byte[] bArr) {
        String requestId = getRequestId();
        notifyOfDownload(context);
        LogUtil.d(requestId, "persistIfRequired");
        if (bArr == null || bArr.length < 1) {
            LogUtil.e(requestId, "persistIfRequired: empty response");
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    RetrieveConf retrieveConf = new PduParser(bArr, this.mMmsConfig.getBoolean("supportMmsContentDisposition")).parse();
                    if (retrieveConf != null && (retrieveConf instanceof RetrieveConf)) {
                        RetrieveConf retrieveConf2 = retrieveConf;
                        int retrieveStatus = retrieveConf2.getRetrieveStatus();
                        LogUtil.d(requestId, "persistIfRequired: messageId = " + new String(retrieveConf2.getMessageId()));
                        if (!this.mRequestManager.getAutoPersistingPref()) {
                            LogUtil.d(requestId, "persistIfRequired, don't auto persist, return");
                            return null;
                        }
                        if (retrieveStatus != 128) {
                            LogUtil.e(requestId, "persistIfRequired: retrieve failed " + retrieveStatus);
                            ContentValues contentValues = new ContentValues(1);
                            contentValues.put("retr_st", Integer.valueOf(retrieveStatus));
                            SqliteWrapper.update(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, contentValues, "m_type=? AND ct_l =?", new String[]{Integer.toString(130), this.mLocationUrl});
                            return null;
                        }
                        Uri uriPersist = PduPersister.getPduPersister(context).persist(retrieveConf, Telephony.Mms.Inbox.CONTENT_URI, true, true, (HashMap) null);
                        if (uriPersist == null) {
                            LogUtil.e(requestId, "persistIfRequired: can not persist message");
                            return null;
                        }
                        ContentValues contentValues2 = new ContentValues();
                        contentValues2.put("date", Long.valueOf(System.currentTimeMillis() / 1000));
                        contentValues2.put("read", (Integer) 0);
                        contentValues2.put("seen", (Integer) 0);
                        if (!TextUtils.isEmpty(this.mCreator)) {
                            contentValues2.put("creator", this.mCreator);
                        }
                        contentValues2.put("sub_id", Integer.valueOf(this.mSubId));
                        if (SqliteWrapper.update(context, context.getContentResolver(), uriPersist, contentValues2, (String) null, (String[]) null) != 1) {
                            LogUtil.e(requestId, "persistIfRequired: can not update message");
                        }
                        SqliteWrapper.delete(context, context.getContentResolver(), Telephony.Mms.CONTENT_URI, "m_type=? AND ct_l =?", new String[]{Integer.toString(130), this.mLocationUrl});
                        return uriPersist;
                    }
                    LogUtil.e(requestId, "persistIfRequired: invalid parsed PDU");
                    return null;
                } catch (RuntimeException e) {
                    LogUtil.e(requestId, "persistIfRequired: can not parse response", e);
                    return null;
                }
            } catch (MmsException e2) {
                LogUtil.e(requestId, "persistIfRequired: can not persist message", e2);
                return null;
            } catch (SQLiteException e3) {
                LogUtil.e(requestId, "persistIfRequired: can not update message", e3);
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void notifyOfDownload(Context context) {
        int[] runningUserIds;
        UserInfo userInfo;
        Intent intent = new Intent("android.provider.Telephony.MMS_DOWNLOADED");
        intent.addFlags(134217728);
        try {
            runningUserIds = ActivityManager.getService().getRunningUserIds();
        } catch (RemoteException e) {
            runningUserIds = null;
        }
        if (runningUserIds == null) {
            runningUserIds = new int[]{UserHandle.ALL.getIdentifier()};
        }
        int[] iArr = runningUserIds;
        UserManager userManager = (UserManager) context.getSystemService("user");
        for (int length = iArr.length - 1; length >= 0; length--) {
            UserHandle userHandle = new UserHandle(iArr[length]);
            if (iArr[length] == 0 || (!userManager.hasUserRestriction("no_sms", userHandle) && (userInfo = userManager.getUserInfo(iArr[length])) != null && !userInfo.isManagedProfile())) {
                context.sendOrderedBroadcastAsUser(intent, userHandle, "android.permission.RECEIVE_MMS", 18, null, null, -1, null, null);
            }
        }
    }

    @Override
    protected boolean transferResponse(Intent intent, byte[] bArr) {
        return this.mRequestManager.writePduToContentUri(this.mContentUri, bArr);
    }

    @Override
    protected boolean prepareForHttpRequest() {
        return true;
    }

    public void tryDownloadingByCarrierApp(Context context, String str) {
        CarrierDownloadManager carrierDownloadManager = new CarrierDownloadManager();
        carrierDownloadManager.downloadMms(context, str, new CarrierDownloadCompleteCallback(context, carrierDownloadManager));
    }

    @Override
    protected void revokeUriPermission(Context context) {
        context.revokeUriPermission(this.mContentUri, 2);
    }

    private final class CarrierDownloadManager extends CarrierMessagingServiceManager {
        private volatile CarrierDownloadCompleteCallback mCarrierDownloadCallback;

        private CarrierDownloadManager() {
        }

        void downloadMms(Context context, String str, CarrierDownloadCompleteCallback carrierDownloadCompleteCallback) {
            this.mCarrierDownloadCallback = carrierDownloadCompleteCallback;
            if (bindToCarrierMessagingService(context, str)) {
                LogUtil.v("bindService() for carrier messaging service succeeded");
            } else {
                LogUtil.e("bindService() for carrier messaging service failed");
                carrierDownloadCompleteCallback.onDownloadMmsComplete(1);
            }
        }

        protected void onServiceReady(ICarrierMessagingService iCarrierMessagingService) {
            try {
                iCarrierMessagingService.downloadMms(DownloadRequest.this.mContentUri, DownloadRequest.this.mSubId, Uri.parse(DownloadRequest.this.mLocationUrl), this.mCarrierDownloadCallback);
            } catch (RemoteException e) {
                LogUtil.e("Exception downloading MMS using the carrier messaging service: " + e, e);
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

        public void onSendMmsComplete(int i, byte[] bArr) {
            LogUtil.e("Unexpected onSendMmsComplete call with result: " + i);
        }

        public void onDownloadMmsComplete(int i) {
            LogUtil.d("Carrier app result for download: " + i);
            this.mCarrierDownloadManager.disposeConnection(this.mContext);
            if (!DownloadRequest.this.maybeFallbackToRegularDelivery(i)) {
                DownloadRequest.this.processResult(this.mContext, MmsRequest.toSmsManagerResult(i), null, 0);
            }
        }
    }
}
