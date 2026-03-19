package com.android.server.telecom;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telecom.Log;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CallerInfo;

@VisibleForTesting
public class RingtoneFactory {
    private final CallsManager mCallsManager;
    private final Context mContext;

    public RingtoneFactory(CallsManager callsManager, Context context) {
        this.mContext = context;
        this.mCallsManager = callsManager;
    }

    public Ringtone getRingtone(Call call) {
        Context contextForUserHandle;
        Ringtone ringtone;
        Uri actualDefaultRingtoneUri;
        if (isWorkContact(call)) {
            contextForUserHandle = getWorkProfileContextForUser(this.mCallsManager.getCurrentUserHandle());
        } else {
            contextForUserHandle = getContextForUserHandle(this.mCallsManager.getCurrentUserHandle());
        }
        Uri ringtone2 = call.getRingtone();
        if (ringtone2 != null && contextForUserHandle != null) {
            ringtone = RingtoneManager.getRingtone(contextForUserHandle, ringtone2);
        } else {
            ringtone = null;
        }
        if (ringtone == null) {
            if (!hasDefaultRingtoneForUser(contextForUserHandle)) {
                contextForUserHandle = this.mContext;
            }
            if (UserManager.get(contextForUserHandle).isUserUnlocked(contextForUserHandle.getUserId())) {
                actualDefaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(contextForUserHandle, 1);
            } else {
                actualDefaultRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
            }
            if (actualDefaultRingtoneUri == null) {
                return null;
            }
            ringtone = RingtoneManager.getRingtone(contextForUserHandle, actualDefaultRingtoneUri);
        }
        if (ringtone != null) {
            ringtone.setStreamType(2);
        }
        return ringtone;
    }

    private Context getWorkProfileContextForUser(UserHandle userHandle) {
        int i = 0;
        UserInfo userInfo = null;
        for (UserInfo userInfo2 : UserManager.get(this.mContext).getEnabledProfiles(userHandle.getIdentifier())) {
            if (userInfo2.getUserHandle() != userHandle && userInfo2.isManagedProfile()) {
                i++;
                userInfo = userInfo2;
            }
        }
        if (i == 1) {
            return getContextForUserHandle(userInfo.getUserHandle());
        }
        return null;
    }

    private Context getContextForUserHandle(UserHandle userHandle) {
        if (userHandle == null) {
            return null;
        }
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("RingtoneFactory", "Package name not found: " + e.getMessage(), new Object[0]);
            return null;
        }
    }

    private boolean hasDefaultRingtoneForUser(Context context) {
        if (context == null) {
            return false;
        }
        return !TextUtils.isEmpty(Settings.System.getStringForUser(context.getContentResolver(), "ringtone", context.getUserId()));
    }

    private boolean isWorkContact(Call call) {
        CallerInfo callerInfo = call.getCallerInfo();
        return callerInfo != null && callerInfo.userType == 1;
    }
}
