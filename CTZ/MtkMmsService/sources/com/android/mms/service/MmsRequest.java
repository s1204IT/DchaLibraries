package com.android.mms.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.service.carrier.ICarrierMessagingCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.mms.service.exception.ApnException;
import com.android.mms.service.exception.MmsHttpException;
import com.android.mms.service.exception.MmsNetworkException;

public abstract class MmsRequest {
    protected Context mContext;
    protected String mCreator;
    protected Bundle mMmsConfig = null;
    protected Bundle mMmsConfigOverrides;
    protected RequestManager mRequestManager;
    protected int mSubId;

    public interface RequestManager {
        void addSimRequest(MmsRequest mmsRequest);

        boolean getAutoPersistingPref();

        byte[] readPduFromContentUri(Uri uri, int i);

        boolean writePduToContentUri(Uri uri, byte[] bArr);
    }

    protected abstract byte[] doHttp(Context context, MmsNetworkManager mmsNetworkManager, ApnSettings apnSettings) throws MmsHttpException;

    protected abstract PendingIntent getPendingIntent();

    protected abstract int getQueueType();

    protected abstract Uri persistIfRequired(Context context, int i, byte[] bArr);

    protected abstract boolean prepareForHttpRequest();

    protected abstract void revokeUriPermission(Context context);

    protected abstract boolean transferResponse(Intent intent, byte[] bArr);

    public MmsRequest(RequestManager requestManager, int i, String str, Bundle bundle, Context context) {
        this.mRequestManager = requestManager;
        this.mSubId = i;
        this.mCreator = str;
        this.mMmsConfigOverrides = bundle;
        this.mContext = context;
    }

    public int getSubId() {
        return this.mSubId;
    }

    private boolean ensureMmsConfigLoaded() {
        Bundle mmsConfigBySubId;
        if (this.mMmsConfig == null && (mmsConfigBySubId = MmsConfigManager.getInstance().getMmsConfigBySubId(this.mSubId)) != null) {
            this.mMmsConfig = mmsConfigBySubId;
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            String mmsUserAgent = telephonyManager.getMmsUserAgent();
            if (!TextUtils.isEmpty(mmsUserAgent)) {
                mmsConfigBySubId.putString("userAgent", mmsUserAgent);
            }
            String mmsUAProfUrl = telephonyManager.getMmsUAProfUrl();
            if (!TextUtils.isEmpty(mmsUAProfUrl)) {
                mmsConfigBySubId.putString("uaProfUrl", mmsUAProfUrl);
            }
            if (this.mMmsConfigOverrides != null) {
                this.mMmsConfig.putAll(this.mMmsConfigOverrides);
            }
        }
        return this.mMmsConfig != null;
    }

    public void execute(Context context, MmsNetworkManager mmsNetworkManager) {
        int i;
        int i2;
        byte[] bArr;
        int statusCode;
        int i3;
        ApnSettings apnSettingsLoad;
        byte[] bArrDoHttp;
        String string = toString();
        LogUtil.i(string, "Executing...");
        String str = null;
        byte[] bArr2 = null;
        if (!ensureMmsConfigLoaded()) {
            LogUtil.e(string, "mms config is not loaded yet");
            i2 = 7;
        } else if (prepareForHttpRequest()) {
            byte[] bArr3 = null;
            i = 0;
            i2 = 1;
            long j = 2;
            int i4 = 0;
            while (true) {
                if (i4 >= 3) {
                    bArr2 = bArr3;
                    break;
                }
                try {
                    mmsNetworkManager.acquireNetwork(string);
                    String apnName = mmsNetworkManager.getApnName();
                    LogUtil.d(string, "APN name is " + apnName);
                    try {
                        try {
                            apnSettingsLoad = ApnSettings.load(context, apnName, this.mSubId, string);
                        } catch (ApnException e) {
                            if (apnName == null) {
                                throw e;
                            }
                            LogUtil.i(string, "No match with APN name: " + apnName + ", try with no name");
                            apnSettingsLoad = ApnSettings.load(context, str, this.mSubId, string);
                        }
                        LogUtil.i(string, "Using " + apnSettingsLoad.toString());
                        bArrDoHttp = doHttp(context, mmsNetworkManager, apnSettingsLoad);
                    } catch (Throwable th) {
                        mmsNetworkManager.releaseNetwork(string, this instanceof DownloadRequest);
                        throw th;
                    }
                } catch (ApnException e2) {
                    e = e2;
                    bArr2 = bArr3;
                } catch (MmsHttpException e3) {
                    e = e3;
                } catch (MmsNetworkException e4) {
                    e = e4;
                } catch (Exception e5) {
                    e = e5;
                    bArr2 = bArr3;
                }
                try {
                    mmsNetworkManager.releaseNetwork(string, this instanceof DownloadRequest);
                    bArr2 = bArrDoHttp;
                    i2 = -1;
                    break;
                } catch (ApnException e6) {
                    e = e6;
                    bArr2 = bArrDoHttp;
                    LogUtil.e(string, "APN failure", e);
                    i2 = 2;
                } catch (MmsHttpException e7) {
                    e = e7;
                    bArr3 = bArrDoHttp;
                    LogUtil.e(string, "HTTP or network I/O failure", e);
                    i3 = 4;
                    bArr = bArr3;
                    statusCode = e.getStatusCode();
                    try {
                        StringBuilder sb = new StringBuilder();
                        sb.append("sleep: ");
                        long j2 = j * 1000;
                        sb.append(j2);
                        LogUtil.d(string, sb.toString());
                        Thread.sleep(j2, 0);
                    } catch (InterruptedException e8) {
                    }
                    j <<= 1;
                    i4++;
                    i2 = i3;
                    i = statusCode;
                    bArr3 = bArr;
                    str = null;
                } catch (MmsNetworkException e9) {
                    e = e9;
                    bArr3 = bArrDoHttp;
                    LogUtil.e(string, "MMS network acquiring failure", e);
                    bArr = bArr3;
                    statusCode = i;
                    i3 = 3;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("sleep: ");
                    long j22 = j * 1000;
                    sb2.append(j22);
                    LogUtil.d(string, sb2.toString());
                    Thread.sleep(j22, 0);
                    j <<= 1;
                    i4++;
                    i2 = i3;
                    i = statusCode;
                    bArr3 = bArr;
                    str = null;
                } catch (Exception e10) {
                    e = e10;
                    bArr2 = bArrDoHttp;
                    LogUtil.e(string, "Unexpected failure", e);
                    i2 = 1;
                }
                j <<= 1;
                i4++;
                i2 = i3;
                i = statusCode;
                bArr3 = bArr;
                str = null;
            }
        } else {
            LogUtil.e(string, "Failed to prepare for request");
            i2 = 5;
        }
        i = 0;
        if (i2 != -1) {
            LogUtil.d(string, "pdp connect FAIL. result = " + i2);
        }
        processResult(context, i2, bArr2, i);
    }

    public void processResult(Context context, int i, byte[] bArr, int i2) {
        Uri uriPersistIfRequired = persistIfRequired(context, i, bArr);
        LogUtil.d(toString(), "processResult: prepare");
        PendingIntent pendingIntent = getPendingIntent();
        if (pendingIntent != null) {
            boolean zTransferResponse = true;
            Intent intent = new Intent();
            if (bArr != null) {
                zTransferResponse = transferResponse(intent, bArr);
            }
            if (uriPersistIfRequired != null) {
                intent.putExtra("uri", uriPersistIfRequired.toString());
            }
            if (i == 4 && i2 != 0) {
                intent.putExtra("android.telephony.extra.MMS_HTTP_STATUS", i2);
            }
            if (!zTransferResponse) {
                i = 5;
            }
            try {
                pendingIntent.send(context, i, intent);
            } catch (PendingIntent.CanceledException e) {
                LogUtil.e(toString(), "Sending pending intent canceled", e);
            }
        }
        revokeUriPermission(context);
    }

    protected boolean maybeFallbackToRegularDelivery(int i) {
        if (i == 1 || i == 1) {
            LogUtil.d(toString(), "Sending/downloading MMS by IP failed.");
            this.mRequestManager.addSimRequest(this);
            return true;
        }
        return false;
    }

    protected static int toSmsManagerResult(int i) {
        switch (i) {
            case 0:
                return -1;
            case 1:
                return 6;
            default:
                return 1;
        }
    }

    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(hashCode());
    }

    protected String getRequestId() {
        return toString();
    }

    protected abstract class CarrierMmsActionCallback extends ICarrierMessagingCallback.Stub {
        protected CarrierMmsActionCallback() {
        }

        public void onSendSmsComplete(int i, int i2) {
            LogUtil.e("Unexpected onSendSmsComplete call with result: " + i);
        }

        public void onSendMultipartSmsComplete(int i, int[] iArr) {
            LogUtil.e("Unexpected onSendMultipartSmsComplete call with result: " + i);
        }

        public void onFilterComplete(int i) {
            LogUtil.e("Unexpected onFilterComplete call with result: " + i);
        }
    }
}
