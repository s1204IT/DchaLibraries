package com.android.server.timezone;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.UserHandle;
import com.android.internal.util.DumpUtils;
import com.android.server.pm.DumpState;
import java.io.PrintWriter;
import java.util.concurrent.Executor;

final class RulesManagerServiceHelperImpl implements PermissionHelper, Executor, RulesManagerIntentHelper {
    private final Context mContext;

    RulesManagerServiceHelperImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public void enforceCallerHasPermission(String str) {
        this.mContext.enforceCallingPermission(str, null);
    }

    @Override
    public boolean checkDumpPermission(String str, PrintWriter printWriter) {
        return DumpUtils.checkDumpPermission(this.mContext, str, printWriter);
    }

    @Override
    public void execute(Runnable runnable) {
        AsyncTask.execute(runnable);
    }

    @Override
    public void sendTimeZoneOperationStaged() {
        sendOperationIntent(true);
    }

    @Override
    public void sendTimeZoneOperationUnstaged() {
        sendOperationIntent(false);
    }

    private void sendOperationIntent(boolean z) {
        Intent intent = new Intent("com.android.intent.action.timezone.RULES_UPDATE_OPERATION");
        intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.putExtra("staged", z);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }
}
