package com.android.browser;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import com.android.browser.provider.BrowserContract;

public class AccountsChangedReceiver extends BroadcastReceiver {
    private static final String[] PROJECTION = {"account_name", "account_type"};

    static class DeleteRemovedAccounts extends Thread {
        Context mContext;

        public DeleteRemovedAccounts(Context context) {
            this.mContext = context.getApplicationContext();
        }

        boolean contains(Account[] accountArr, String str, String str2) {
            for (Account account : accountArr) {
                if (TextUtils.equals(account.name, str) && TextUtils.equals(account.type, str2)) {
                    return true;
                }
            }
            return false;
        }

        void delete(ContentResolver contentResolver, String str, String str2) {
            contentResolver.delete(BrowserContract.Bookmarks.CONTENT_URI.buildUpon().appendQueryParameter("caller_is_syncadapter", "true").build(), "account_name=? AND account_type=?", new String[]{str, str2});
        }

        @Override
        public void run() {
            Account[] accounts = AccountManager.get(this.mContext).getAccounts();
            ContentResolver contentResolver = this.mContext.getContentResolver();
            Cursor cursorQuery = contentResolver.query(BrowserContract.Accounts.CONTENT_URI, AccountsChangedReceiver.PROJECTION, "account_name IS NOT NULL", null, null);
            while (cursorQuery.moveToNext()) {
                String string = cursorQuery.getString(0);
                String string2 = cursorQuery.getString(1);
                if (!contains(accounts, string, string2)) {
                    delete(contentResolver, string, string2);
                }
            }
            try {
                contentResolver.update(BrowserContract.Accounts.CONTENT_URI, null, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cursorQuery.close();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        new DeleteRemovedAccounts(context).start();
    }
}
