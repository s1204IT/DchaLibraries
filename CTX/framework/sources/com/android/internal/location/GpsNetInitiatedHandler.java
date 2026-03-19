package com.android.internal.location;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.INetInitiatedListener;
import android.location.LocationManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.app.NetInitiatedActivity;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.telephony.GsmAlphabet;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

public class GpsNetInitiatedHandler {
    public static final String ACTION_NI_VERIFY = "android.intent.action.NETWORK_INITIATED_VERIFY";
    public static final int GPS_ENC_NONE = 0;
    public static final int GPS_ENC_SUPL_GSM_DEFAULT = 1;
    public static final int GPS_ENC_SUPL_UCS2 = 3;
    public static final int GPS_ENC_SUPL_UTF8 = 2;
    public static final int GPS_ENC_UNKNOWN = -1;
    public static final int GPS_NI_NEED_NOTIFY = 1;
    public static final int GPS_NI_NEED_VERIFY = 2;
    public static final int GPS_NI_PRIVACY_OVERRIDE = 4;
    public static final int GPS_NI_RESPONSE_ACCEPT = 1;
    public static final int GPS_NI_RESPONSE_DENY = 2;
    public static final int GPS_NI_RESPONSE_IGNORE = 4;
    public static final int GPS_NI_RESPONSE_NORESP = 3;
    public static final int GPS_NI_TYPE_EMERGENCY_SUPL = 4;
    public static final int GPS_NI_TYPE_UMTS_CTRL_PLANE = 3;
    public static final int GPS_NI_TYPE_UMTS_SUPL = 2;
    public static final int GPS_NI_TYPE_VOICE = 1;
    private static final int MAX_EMERGENCY_MODE_EXTENSION_SECONDS = 300;
    public static final String NI_EXTRA_CMD_NOTIF_ID = "notif_id";
    public static final String NI_EXTRA_CMD_RESPONSE = "response";
    public static final String NI_INTENT_KEY_DEFAULT_RESPONSE = "default_resp";
    public static final String NI_INTENT_KEY_MESSAGE = "message";
    public static final String NI_INTENT_KEY_NOTIF_ID = "notif_id";
    public static final String NI_INTENT_KEY_TIMEOUT = "timeout";
    public static final String NI_INTENT_KEY_TITLE = "title";
    public static final String NI_RESPONSE_EXTRA_CMD = "send_ni_response";
    private final Context mContext;
    private volatile boolean mIsInEmergencyCall;
    private volatile boolean mIsSuplEsEnabled;
    private final LocationManager mLocationManager;
    private final INetInitiatedListener mNetInitiatedListener;
    private Notification.Builder mNiNotificationBuilder;
    private final PhoneStateListener mPhoneStateListener;
    private final TelephonyManager mTelephonyManager;
    private static final String TAG = "GpsNetInitiatedHandler";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static boolean mIsHexInput = true;
    private boolean mPlaySounds = false;
    private boolean mPopupImmediately = true;
    private volatile boolean mIsLocationEnabled = false;
    private volatile long mCallEndElapsedRealtimeMillis = 0;
    private volatile long mEmergencyExtensionMillis = 0;
    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                GpsNetInitiatedHandler.this.mIsInEmergencyCall = PhoneNumberUtils.isEmergencyNumber(intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
                if (GpsNetInitiatedHandler.DEBUG) {
                    Log.v(GpsNetInitiatedHandler.TAG, "ACTION_NEW_OUTGOING_CALL - " + GpsNetInitiatedHandler.this.getInEmergency());
                    return;
                }
                return;
            }
            if (action.equals(LocationManager.MODE_CHANGED_ACTION)) {
                GpsNetInitiatedHandler.this.updateLocationMode();
                if (GpsNetInitiatedHandler.DEBUG) {
                    Log.d(GpsNetInitiatedHandler.TAG, "location enabled :" + GpsNetInitiatedHandler.this.getLocationEnabled());
                }
            }
        }
    };

    public static class GpsNiNotification {
        public int defaultResponse;
        public boolean needNotify;
        public boolean needVerify;
        public int niType;
        public int notificationId;
        public boolean privacyOverride;
        public String requestorId;
        public int requestorIdEncoding;
        public String text;
        public int textEncoding;
        public int timeout;
    }

    public static class GpsNiResponse {
        int userResponse;
    }

    public GpsNetInitiatedHandler(Context context, INetInitiatedListener iNetInitiatedListener, boolean z) {
        this.mContext = context;
        if (iNetInitiatedListener == null) {
            throw new IllegalArgumentException("netInitiatedListener is null");
        }
        this.mNetInitiatedListener = iNetInitiatedListener;
        setSuplEsEnabled(z);
        this.mLocationManager = (LocationManager) context.getSystemService("location");
        updateLocationMode();
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int i, String str) {
                if (GpsNetInitiatedHandler.DEBUG) {
                    Log.d(GpsNetInitiatedHandler.TAG, "onCallStateChanged(): state is " + i);
                }
                if (i == 0 && GpsNetInitiatedHandler.this.mIsInEmergencyCall) {
                    GpsNetInitiatedHandler.this.mCallEndElapsedRealtimeMillis = SystemClock.elapsedRealtime();
                    GpsNetInitiatedHandler.this.mIsInEmergencyCall = false;
                }
            }
        };
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        intentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        this.mContext.registerReceiver(this.mBroadcastReciever, intentFilter);
    }

    public void setSuplEsEnabled(boolean z) {
        this.mIsSuplEsEnabled = z;
    }

    public boolean getSuplEsEnabled() {
        return this.mIsSuplEsEnabled;
    }

    public void updateLocationMode() {
        this.mIsLocationEnabled = this.mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean getLocationEnabled() {
        return this.mIsLocationEnabled;
    }

    public boolean getInEmergency() {
        return this.mIsInEmergencyCall || this.mTelephonyManager.getEmergencyCallbackMode() || ((this.mCallEndElapsedRealtimeMillis > 0L ? 1 : (this.mCallEndElapsedRealtimeMillis == 0L ? 0 : -1)) > 0 && ((SystemClock.elapsedRealtime() - this.mCallEndElapsedRealtimeMillis) > this.mEmergencyExtensionMillis ? 1 : ((SystemClock.elapsedRealtime() - this.mCallEndElapsedRealtimeMillis) == this.mEmergencyExtensionMillis ? 0 : -1)) < 0);
    }

    public void setEmergencyExtensionSeconds(int i) {
        if (i > 300) {
            Log.w(TAG, "emergencyExtensionSeconds " + i + " too high, reset to 300");
            i = 300;
        } else if (i < 0) {
            Log.w(TAG, "emergencyExtensionSeconds " + i + " is negative, reset to zero.");
            i = 0;
        }
        this.mEmergencyExtensionMillis = TimeUnit.SECONDS.toMillis(i);
    }

    public void handleNiNotification(GpsNiNotification gpsNiNotification) {
        if (DEBUG) {
            Log.d(TAG, "in handleNiNotification () : notificationId: " + gpsNiNotification.notificationId + " requestorId: " + gpsNiNotification.requestorId + " text: " + gpsNiNotification.text + " mIsSuplEsEnabled" + getSuplEsEnabled() + " mIsLocationEnabled" + getLocationEnabled());
        }
        if (getSuplEsEnabled()) {
            handleNiInEs(gpsNiNotification);
        } else {
            handleNi(gpsNiNotification);
        }
    }

    private void handleNi(GpsNiNotification gpsNiNotification) {
        if (DEBUG) {
            Log.d(TAG, "in handleNi () : needNotify: " + gpsNiNotification.needNotify + " needVerify: " + gpsNiNotification.needVerify + " privacyOverride: " + gpsNiNotification.privacyOverride + " mPopupImmediately: " + this.mPopupImmediately + " mInEmergency: " + getInEmergency());
        }
        if (!getLocationEnabled() && !getInEmergency()) {
            try {
                this.mNetInitiatedListener.sendNiResponse(gpsNiNotification.notificationId, 4);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in sendNiResponse");
            }
        }
        if (gpsNiNotification.needNotify) {
            if (gpsNiNotification.needVerify && this.mPopupImmediately) {
                openNiDialog(gpsNiNotification);
            } else {
                setNiNotification(gpsNiNotification);
            }
        }
        if (!gpsNiNotification.needVerify || gpsNiNotification.privacyOverride) {
            try {
                this.mNetInitiatedListener.sendNiResponse(gpsNiNotification.notificationId, 1);
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException in sendNiResponse");
            }
        }
    }

    private void handleNiInEs(GpsNiNotification gpsNiNotification) {
        if (DEBUG) {
            Log.d(TAG, "in handleNiInEs () : niType: " + gpsNiNotification.niType + " notificationId: " + gpsNiNotification.notificationId);
        }
        if ((gpsNiNotification.niType == 4) != getInEmergency()) {
            try {
                this.mNetInitiatedListener.sendNiResponse(gpsNiNotification.notificationId, 4);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in sendNiResponse");
                return;
            }
        }
        handleNi(gpsNiNotification);
    }

    private synchronized void setNiNotification(GpsNiNotification gpsNiNotification) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        String notifTitle = getNotifTitle(gpsNiNotification, this.mContext);
        String notifMessage = getNotifMessage(gpsNiNotification, this.mContext);
        if (DEBUG) {
            Log.d(TAG, "setNiNotification, notifyId: " + gpsNiNotification.notificationId + ", title: " + notifTitle + ", message: " + notifMessage);
        }
        if (this.mNiNotificationBuilder == null) {
            this.mNiNotificationBuilder = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_ALERTS).setSmallIcon(R.drawable.stat_sys_gps_on).setWhen(0L).setOngoing(true).setAutoCancel(true).setColor(this.mContext.getColor(R.color.system_notification_accent_color));
        }
        if (this.mPlaySounds) {
            this.mNiNotificationBuilder.setDefaults(1);
        } else {
            this.mNiNotificationBuilder.setDefaults(0);
        }
        this.mNiNotificationBuilder.setTicker(getNotifTicker(gpsNiNotification, this.mContext)).setContentTitle(notifTitle).setContentText(notifMessage);
        notificationManager.notifyAsUser(null, gpsNiNotification.notificationId, this.mNiNotificationBuilder.build(), UserHandle.ALL);
    }

    private void openNiDialog(GpsNiNotification gpsNiNotification) {
        Intent dlgIntent = getDlgIntent(gpsNiNotification);
        if (DEBUG) {
            Log.d(TAG, "openNiDialog, notifyId: " + gpsNiNotification.notificationId + ", requestorId: " + gpsNiNotification.requestorId + ", text: " + gpsNiNotification.text);
        }
        this.mContext.startActivity(dlgIntent);
    }

    private Intent getDlgIntent(GpsNiNotification gpsNiNotification) {
        Intent intent = new Intent();
        String dialogTitle = getDialogTitle(gpsNiNotification, this.mContext);
        String dialogMessage = getDialogMessage(gpsNiNotification, this.mContext);
        intent.setFlags(268468224);
        intent.setClass(this.mContext, NetInitiatedActivity.class);
        intent.putExtra("notif_id", gpsNiNotification.notificationId);
        intent.putExtra("title", dialogTitle);
        intent.putExtra("message", dialogMessage);
        intent.putExtra(NI_INTENT_KEY_TIMEOUT, gpsNiNotification.timeout);
        intent.putExtra(NI_INTENT_KEY_DEFAULT_RESPONSE, gpsNiNotification.defaultResponse);
        if (DEBUG) {
            Log.d(TAG, "generateIntent, title: " + dialogTitle + ", message: " + dialogMessage + ", timeout: " + gpsNiNotification.timeout);
        }
        return intent;
    }

    static byte[] stringToByteArray(String str, boolean z) {
        int length = z ? str.length() / 2 : str.length();
        byte[] bArr = new byte[length];
        int i = 0;
        if (z) {
            while (i < length) {
                int i2 = i * 2;
                bArr[i] = (byte) Integer.parseInt(str.substring(i2, i2 + 2), 16);
                i++;
            }
        } else {
            while (i < length) {
                bArr[i] = (byte) str.charAt(i);
                i++;
            }
        }
        return bArr;
    }

    static String decodeGSMPackedString(byte[] bArr) {
        int length = bArr.length;
        int i = (length * 8) / 7;
        if (length % 7 == 0 && length > 0 && (bArr[length - 1] >> 1) == 0) {
            i--;
        }
        String strGsm7BitPackedToString = GsmAlphabet.gsm7BitPackedToString(bArr, 0, i);
        if (strGsm7BitPackedToString == null) {
            Log.e(TAG, "Decoding of GSM packed string failed");
            return "";
        }
        return strGsm7BitPackedToString;
    }

    static String decodeUTF8String(byte[] bArr) {
        try {
            return new String(bArr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    static String decodeUCS2String(byte[] bArr) {
        try {
            return new String(bArr, "UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
    }

    private static String decodeString(String str, boolean z, int i) {
        byte[] bArrStringToByteArray = stringToByteArray(str, z);
        switch (i) {
            case -1:
            case 0:
                return str;
            case 1:
                return decodeGSMPackedString(bArrStringToByteArray);
            case 2:
                return decodeUTF8String(bArrStringToByteArray);
            case 3:
                return decodeUCS2String(bArrStringToByteArray);
            default:
                Log.e(TAG, "Unknown encoding " + i + " for NI text " + str);
                return str;
        }
    }

    private static String getNotifTicker(GpsNiNotification gpsNiNotification, Context context) {
        return String.format(context.getString(R.string.gpsNotifTicker), decodeString(gpsNiNotification.requestorId, mIsHexInput, gpsNiNotification.requestorIdEncoding), decodeString(gpsNiNotification.text, mIsHexInput, gpsNiNotification.textEncoding));
    }

    private static String getNotifTitle(GpsNiNotification gpsNiNotification, Context context) {
        return String.format(context.getString(R.string.gpsNotifTitle), new Object[0]);
    }

    private static String getNotifMessage(GpsNiNotification gpsNiNotification, Context context) {
        return String.format(context.getString(R.string.gpsNotifMessage), decodeString(gpsNiNotification.requestorId, mIsHexInput, gpsNiNotification.requestorIdEncoding), decodeString(gpsNiNotification.text, mIsHexInput, gpsNiNotification.textEncoding));
    }

    public static String getDialogTitle(GpsNiNotification gpsNiNotification, Context context) {
        return getNotifTitle(gpsNiNotification, context);
    }

    private static String getDialogMessage(GpsNiNotification gpsNiNotification, Context context) {
        return getNotifMessage(gpsNiNotification, context);
    }
}
