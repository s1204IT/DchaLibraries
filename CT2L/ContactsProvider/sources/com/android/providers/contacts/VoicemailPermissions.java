package com.android.providers.contacts;

import android.content.Context;

public class VoicemailPermissions {
    private final Context mContext;

    public VoicemailPermissions(Context context) {
        this.mContext = context;
    }

    public boolean callerHasOwnVoicemailAccess() {
        return callerHasPermission("com.android.voicemail.permission.ADD_VOICEMAIL");
    }

    public boolean callerHasReadAccess() {
        return callerHasPermission("com.android.voicemail.permission.READ_VOICEMAIL");
    }

    public boolean callerHasWriteAccess() {
        return callerHasPermission("com.android.voicemail.permission.WRITE_VOICEMAIL");
    }

    public void checkCallerHasOwnVoicemailAccess() {
        if (!callerHasOwnVoicemailAccess()) {
            throw new SecurityException("The caller must have permission: com.android.voicemail.permission.ADD_VOICEMAIL");
        }
    }

    public void checkCallerHasReadAccess() {
        if (!callerHasReadAccess()) {
            throw new SecurityException(String.format("The caller must have %s permission: ", "com.android.voicemail.permission.READ_VOICEMAIL"));
        }
    }

    public void checkCallerHasWriteAccess() {
        if (!callerHasWriteAccess()) {
            throw new SecurityException(String.format("The caller must have %s permission: ", "com.android.voicemail.permission.WRITE_VOICEMAIL"));
        }
    }

    public boolean packageHasOwnVoicemailAccess(String packageName) {
        return packageHasPermission(packageName, "com.android.voicemail.permission.ADD_VOICEMAIL");
    }

    public boolean packageHasReadAccess(String packageName) {
        return packageHasPermission(packageName, "com.android.voicemail.permission.READ_VOICEMAIL");
    }

    public boolean packageHasWriteAccess(String packageName) {
        return packageHasPermission(packageName, "com.android.voicemail.permission.WRITE_VOICEMAIL");
    }

    private boolean packageHasPermission(String packageName, String permission) {
        return this.mContext.getPackageManager().checkPermission(permission, packageName) == 0;
    }

    private boolean callerHasPermission(String permission) {
        return this.mContext.checkCallingOrSelfPermission(permission) == 0;
    }
}
