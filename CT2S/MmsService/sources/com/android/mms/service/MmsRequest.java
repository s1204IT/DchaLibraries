package com.android.mms.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.service.carrier.ICarrierMessagingCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.mms.service.MmsConfig;
import com.android.mms.service.exception.ApnException;
import com.android.mms.service.exception.MmsHttpException;
import com.android.mms.service.exception.MmsNetworkException;

public abstract class MmsRequest {
    protected String mCreator;
    protected MmsConfig.Overridden mMmsConfig = null;
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

    public MmsRequest(RequestManager requestManager, int subId, String creator, Bundle configOverrides) {
        this.mRequestManager = requestManager;
        this.mSubId = subId;
        this.mCreator = creator;
        this.mMmsConfigOverrides = configOverrides;
    }

    public int getSubId() {
        return this.mSubId;
    }

    private boolean ensureMmsConfigLoaded() {
        MmsConfig config;
        if (this.mMmsConfig == null && (config = MmsConfigManager.getInstance().getMmsConfigBySubId(this.mSubId)) != null) {
            this.mMmsConfig = new MmsConfig.Overridden(config, this.mMmsConfigOverrides);
        }
        return this.mMmsConfig != null;
    }

    private static boolean inAirplaneMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private static boolean isMobileDataEnabled(Context context, int subId) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        return telephonyManager.getDataEnabled(subId);
    }

    private static boolean isDataNetworkAvailable(Context context, int subId) {
        return !inAirplaneMode(context) && isMobileDataEnabled(context, subId);
    }

    public void execute(Context context, MmsNetworkManager networkManager) {
        ApnSettings apn;
        int result = 1;
        int httpStatusCode = 0;
        byte[] response = null;
        if (!ensureMmsConfigLoaded()) {
            Log.e("MmsService", "MmsRequest: mms config is not loaded yet");
            result = 7;
        } else if (!prepareForHttpRequest()) {
            Log.e("MmsService", "MmsRequest: failed to prepare for request");
            result = 5;
        } else if (!isDataNetworkAvailable(context, this.mSubId)) {
            Log.e("MmsService", "MmsRequest: in airplane mode or mobile data disabled");
            result = 8;
        } else {
            long retryDelaySecs = 2;
            int i = 0;
            while (true) {
                if (i >= 3) {
                    break;
                }
                try {
                    networkManager.acquireNetwork();
                    String apnName = networkManager.getApnName();
                    try {
                        try {
                            apn = ApnSettings.load(context, apnName, this.mSubId);
                        } catch (ApnException e) {
                            if (apnName == null) {
                                throw e;
                            }
                            Log.i("MmsService", "MmsRequest: No match with APN name:" + apnName + ", try with no name");
                            apn = ApnSettings.load(context, null, this.mSubId);
                        }
                        Log.i("MmsService", "MmsRequest: using " + apn.toString());
                        response = doHttp(context, networkManager, apn);
                        result = -1;
                        break;
                    } finally {
                        networkManager.releaseNetwork();
                    }
                } catch (ApnException e2) {
                    Log.e("MmsService", "MmsRequest: APN failure", e2);
                    result = 2;
                } catch (MmsHttpException e3) {
                    Log.e("MmsService", "MmsRequest: HTTP or network I/O failure", e3);
                    result = 4;
                    httpStatusCode = e3.getStatusCode();
                    try {
                        Thread.sleep(1000 * retryDelaySecs, 0);
                    } catch (InterruptedException e4) {
                    }
                    retryDelaySecs <<= 1;
                    i++;
                } catch (MmsNetworkException e5) {
                    Log.e("MmsService", "MmsRequest: MMS network acquiring failure", e5);
                    result = 3;
                    Thread.sleep(1000 * retryDelaySecs, 0);
                    retryDelaySecs <<= 1;
                    i++;
                } catch (Exception e6) {
                    Log.e("MmsService", "MmsRequest: unexpected failure", e6);
                    result = 1;
                }
                retryDelaySecs <<= 1;
                i++;
            }
        }
        processResult(context, result, response, httpStatusCode);
    }

    public void processResult(Context context, int result, byte[] response, int httpStatusCode) {
        Uri messageUri = persistIfRequired(context, result, response);
        PendingIntent pendingIntent = getPendingIntent();
        if (pendingIntent != null) {
            boolean succeeded = true;
            Intent fillIn = new Intent();
            if (response != null) {
                succeeded = transferResponse(fillIn, response);
            }
            if (messageUri != null) {
                fillIn.putExtra("uri", messageUri.toString());
            }
            if (result == 4 && httpStatusCode != 0) {
                fillIn.putExtra("android.telephony.extra.MMS_HTTP_STATUS", httpStatusCode);
            }
            if (!succeeded) {
                result = 5;
            }
            try {
                pendingIntent.send(context, result, fillIn);
            } catch (PendingIntent.CanceledException e) {
                Log.e("MmsService", "MmsRequest: sending pending intent canceled", e);
            }
        }
        revokeUriPermission(context);
    }

    protected boolean maybeFallbackToRegularDelivery(int carrierMessagingAppResult) {
        if (carrierMessagingAppResult != 1 && carrierMessagingAppResult != 1) {
            return false;
        }
        Log.d("MmsService", "Sending/downloading MMS by IP failed.");
        this.mRequestManager.addSimRequest(this);
        return true;
    }

    protected static int toSmsManagerResult(int carrierMessagingAppResult) {
        switch (carrierMessagingAppResult) {
            case 0:
                return -1;
            case 1:
                return 6;
            default:
                return 1;
        }
    }

    protected abstract class CarrierMmsActionCallback extends ICarrierMessagingCallback.Stub {
        protected CarrierMmsActionCallback() {
        }

        public void onSendSmsComplete(int result, int messageRef) {
            Log.e("MmsService", "Unexpected onSendSmsComplete call with result: " + result);
        }

        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            Log.e("MmsService", "Unexpected onSendMultipartSmsComplete call with result: " + result);
        }

        public void onFilterComplete(boolean keepMessage) {
            Log.e("MmsService", "Unexpected onFilterComplete call with result: " + keepMessage);
        }
    }
}
