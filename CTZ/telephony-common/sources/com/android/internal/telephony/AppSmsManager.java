package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.security.SecureRandom;
import java.util.Map;

public class AppSmsManager {
    private static final String LOG_TAG = "AppSmsManager";
    private final Context mContext;
    private final Object mLock = new Object();
    private final SecureRandom mRandom = new SecureRandom();

    @GuardedBy("mLock")
    private final Map<String, AppRequestInfo> mTokenMap = new ArrayMap();

    @GuardedBy("mLock")
    private final Map<String, AppRequestInfo> mPackageMap = new ArrayMap();

    public AppSmsManager(Context context) {
        this.mContext = context;
    }

    public String createAppSpecificSmsToken(String str, PendingIntent pendingIntent) {
        ((AppOpsManager) this.mContext.getSystemService("appops")).checkPackage(Binder.getCallingUid(), str);
        String strGenerateNonce = generateNonce();
        synchronized (this.mLock) {
            if (this.mPackageMap.containsKey(str)) {
                removeRequestLocked(this.mPackageMap.get(str));
            }
            addRequestLocked(new AppRequestInfo(str, pendingIntent, strGenerateNonce));
        }
        return strGenerateNonce;
    }

    public boolean handleSmsReceivedIntent(Intent intent) {
        if (intent.getAction() != "android.provider.Telephony.SMS_DELIVER") {
            Log.wtf(LOG_TAG, "Got intent with incorrect action: " + intent.getAction());
            return false;
        }
        synchronized (this.mLock) {
            AppRequestInfo appRequestInfoFindAppRequestInfoSmsIntentLocked = findAppRequestInfoSmsIntentLocked(intent);
            if (appRequestInfoFindAppRequestInfoSmsIntentLocked == null) {
                return false;
            }
            try {
                Intent intent2 = new Intent();
                intent2.putExtras(intent.getExtras());
                appRequestInfoFindAppRequestInfoSmsIntentLocked.pendingIntent.send(this.mContext, 0, intent2);
                removeRequestLocked(appRequestInfoFindAppRequestInfoSmsIntentLocked);
                return true;
            } catch (PendingIntent.CanceledException e) {
                removeRequestLocked(appRequestInfoFindAppRequestInfoSmsIntentLocked);
                return false;
            }
        }
    }

    private AppRequestInfo findAppRequestInfoSmsIntentLocked(Intent intent) {
        SmsMessage[] messagesFromIntent = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messagesFromIntent == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (SmsMessage smsMessage : messagesFromIntent) {
            if (smsMessage != null && smsMessage.getMessageBody() != null) {
                sb.append(smsMessage.getMessageBody());
            }
        }
        String string = sb.toString();
        for (String str : this.mTokenMap.keySet()) {
            if (string.contains(str)) {
                return this.mTokenMap.get(str);
            }
        }
        return null;
    }

    private String generateNonce() {
        byte[] bArr = new byte[8];
        this.mRandom.nextBytes(bArr);
        return Base64.encodeToString(bArr, 11);
    }

    private void removeRequestLocked(AppRequestInfo appRequestInfo) {
        this.mTokenMap.remove(appRequestInfo.token);
        this.mPackageMap.remove(appRequestInfo.packageName);
    }

    private void addRequestLocked(AppRequestInfo appRequestInfo) {
        this.mTokenMap.put(appRequestInfo.token, appRequestInfo);
        this.mPackageMap.put(appRequestInfo.packageName, appRequestInfo);
    }

    private final class AppRequestInfo {
        public final String packageName;
        public final PendingIntent pendingIntent;
        public final String token;

        AppRequestInfo(String str, PendingIntent pendingIntent, String str2) {
            this.packageName = str;
            this.pendingIntent = pendingIntent;
            this.token = str2;
        }
    }
}
