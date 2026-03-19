package com.android.providers.contacts;

import android.content.Context;
import android.os.Binder;
import android.telecom.DefaultDialerManager;
import android.telephony.TelephonyManager;
import com.android.providers.contacts.util.ContactsPermissions;

public class VoicemailPermissions {
    private final Context mContext;

    public VoicemailPermissions(Context context) {
        this.mContext = context;
    }

    public boolean callerHasOwnVoicemailAccess() {
        return callerHasPermission("com.android.voicemail.permission.ADD_VOICEMAIL") || callerHasCarrierPrivileges();
    }

    public boolean callerHasReadAccess(String str) {
        if (DefaultDialerManager.isDefaultOrSystemDialer(this.mContext, str)) {
            return true;
        }
        return callerHasPermission("com.android.voicemail.permission.READ_VOICEMAIL");
    }

    public boolean callerHasWriteAccess(String str) {
        if (DefaultDialerManager.isDefaultOrSystemDialer(this.mContext, str)) {
            return true;
        }
        return callerHasPermission("com.android.voicemail.permission.WRITE_VOICEMAIL");
    }

    public void checkCallerHasOwnVoicemailAccess() {
        if (!callerHasOwnVoicemailAccess()) {
            throw new SecurityException("The caller must have permission: com.android.voicemail.permission.ADD_VOICEMAIL or carrier privileges");
        }
    }

    public void checkCallerHasReadAccess(String str) {
        if (!callerHasReadAccess(str)) {
            throw new SecurityException(String.format("The caller must be the default or system dialer, or have the system-only %s permission: ", "com.android.voicemail.permission.READ_VOICEMAIL"));
        }
    }

    public void checkCallerHasWriteAccess(String str) {
        if (!callerHasWriteAccess(str)) {
            throw new SecurityException(String.format("The caller must be the default or system dialer, or have the system-only %s permission: ", "com.android.voicemail.permission.WRITE_VOICEMAIL"));
        }
    }

    public boolean packageHasOwnVoicemailAccess(String str) {
        return packageHasPermission(str, "com.android.voicemail.permission.ADD_VOICEMAIL") || packageHasCarrierPrivileges(str);
    }

    public boolean packageHasReadAccess(String str) {
        return packageHasPermission(str, "com.android.voicemail.permission.READ_VOICEMAIL");
    }

    private boolean packageHasPermission(String str, String str2) {
        return ContactsPermissions.hasPackagePermission(this.mContext, str2, str);
    }

    private boolean callerHasPermission(String str) {
        return ContactsPermissions.hasCallerOrSelfPermission(this.mContext, str);
    }

    public boolean callerHasCarrierPrivileges() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        for (String str : this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid())) {
            if (telephonyManager.checkCarrierPrivilegesForPackageAnyPhone(str) == 1) {
                return true;
            }
        }
        return false;
    }

    private boolean packageHasCarrierPrivileges(String str) {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).getPackagesWithCarrierPrivileges().contains(str);
    }
}
