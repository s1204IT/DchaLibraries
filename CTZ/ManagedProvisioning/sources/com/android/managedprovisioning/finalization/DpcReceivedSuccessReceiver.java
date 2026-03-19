package com.android.managedprovisioning.finalization;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.UserHandle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;

public class DpcReceivedSuccessReceiver extends BroadcastReceiver {
    private final Callback mCallback;
    private final boolean mKeepAccountMigrated;
    private final UserHandle mManagedUserHandle;
    private final String mMdmPackageName;
    private final Account mMigratedAccount;
    private final Utils mUtils;

    interface Callback {
        void cleanup();
    }

    public DpcReceivedSuccessReceiver(Account account, boolean z, UserHandle userHandle, String str, Callback callback) {
        this(account, z, userHandle, str, new Utils(), callback);
    }

    @VisibleForTesting
    DpcReceivedSuccessReceiver(Account account, boolean z, UserHandle userHandle, String str, Utils utils, Callback callback) {
        this.mMigratedAccount = account;
        this.mKeepAccountMigrated = z;
        this.mMdmPackageName = (String) Preconditions.checkNotNull(str);
        this.mManagedUserHandle = (UserHandle) Preconditions.checkNotNull(userHandle);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ProvisionLogger.logd("ACTION_PROFILE_PROVISIONING_COMPLETE broadcast received by mdm");
        Intent intent2 = new Intent("android.app.action.MANAGED_PROFILE_PROVISIONED");
        intent2.setPackage(this.mMdmPackageName);
        intent2.addFlags(268435488);
        intent2.putExtra("android.intent.extra.USER", this.mManagedUserHandle);
        if (this.mMigratedAccount != null) {
            intent2.putExtra("android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE", this.mMigratedAccount);
            finishAccountMigration(context, intent2);
        } else {
            context.sendBroadcast(intent2);
            if (this.mCallback != null) {
                this.mCallback.cleanup();
            }
        }
    }

    private void finishAccountMigration(final Context context, final Intent intent) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                if (!DpcReceivedSuccessReceiver.this.mKeepAccountMigrated) {
                    DpcReceivedSuccessReceiver.this.mUtils.removeAccount(context, DpcReceivedSuccessReceiver.this.mMigratedAccount);
                }
                context.sendBroadcast(intent);
                return null;
            }

            @Override
            protected void onPostExecute(Void r1) {
                if (DpcReceivedSuccessReceiver.this.mCallback != null) {
                    DpcReceivedSuccessReceiver.this.mCallback.cleanup();
                }
            }
        }.execute(new Void[0]);
    }
}
