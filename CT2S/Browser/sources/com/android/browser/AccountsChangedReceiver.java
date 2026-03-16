package com.android.browser;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BrowserContract;
import android.text.TextUtils;

public class AccountsChangedReceiver extends BroadcastReceiver {
    private static final String[] PROJECTION = {"account_name", "account_type"};

    @Override
    public void onReceive(Context context, Intent intent) {
        new DeleteRemovedAccounts(context).start();
    }

    static class DeleteRemovedAccounts extends Thread {
        Context mContext;

        public DeleteRemovedAccounts(Context context) {
            this.mContext = context.getApplicationContext();
        }

        @Override
        public void run() {
            Account[] accounts = AccountManager.get(this.mContext).getAccounts();
            ContentResolver cr = this.mContext.getContentResolver();
            Cursor c = cr.query(BrowserContract.Accounts.CONTENT_URI, AccountsChangedReceiver.PROJECTION, "account_name IS NOT NULL", null, null);
            while (c.moveToNext()) {
                String name = c.getString(0);
                String type = c.getString(1);
                if (!contains(accounts, name, type)) {
                    delete(cr, name, type);
                }
            }
            cr.update(BrowserContract.Accounts.CONTENT_URI, null, null, null);
            c.close();
        }

        void delete(ContentResolver cr, String name, String type) {
            Uri uri = BrowserContract.Bookmarks.CONTENT_URI.buildUpon().appendQueryParameter("caller_is_syncadapter", "true").build();
            cr.delete(uri, "account_name=? AND account_type=?", new String[]{name, type});
        }

        boolean contains(Account[] accounts, String name, String type) {
            for (Account a : accounts) {
                if (TextUtils.equals(a.name, name) && TextUtils.equals(a.type, type)) {
                    return true;
                }
            }
            return false;
        }
    }
}
