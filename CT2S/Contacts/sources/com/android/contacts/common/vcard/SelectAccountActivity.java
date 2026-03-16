package com.android.contacts.common.vcard;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountSelectionUtil;
import java.util.List;

public class SelectAccountActivity extends Activity {
    private AccountSelectionUtil.AccountSelectedListener mAccountSelectionListener;

    private class CancelListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private CancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            SelectAccountActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            SelectAccountActivity.this.finish();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        int i = R.string.import_from_sdcard;
        super.onCreate(bundle);
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
        if (accountList.size() == 0) {
            Log.w("SelectAccountActivity", "Account does not exist");
            finish();
            return;
        }
        if (accountList.size() == 1) {
            AccountWithDataSet account = accountList.get(0);
            Intent intent = new Intent();
            intent.putExtra("account_name", account.name);
            intent.putExtra("account_type", account.type);
            intent.putExtra("data_set", account.dataSet);
            setResult(-1, intent);
            finish();
            return;
        }
        Log.i("SelectAccountActivity", "The number of available accounts: " + accountList.size());
        this.mAccountSelectionListener = new AccountSelectionUtil.AccountSelectedListener(this, accountList, i) {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                AccountWithDataSet account2 = this.mAccountList.get(which);
                Intent intent2 = new Intent();
                intent2.putExtra("account_name", account2.name);
                intent2.putExtra("account_type", account2.type);
                intent2.putExtra("data_set", account2.dataSet);
                SelectAccountActivity.this.setResult(-1, intent2);
                SelectAccountActivity.this.finish();
            }
        };
        showDialog(R.string.import_from_sdcard);
    }

    @Override
    protected Dialog onCreateDialog(int resId, Bundle bundle) {
        switch (resId) {
            case R.string.import_from_sdcard:
                if (this.mAccountSelectionListener == null) {
                    throw new NullPointerException("mAccountSelectionListener must not be null.");
                }
                return AccountSelectionUtil.getSelectAccountDialog(this, resId, this.mAccountSelectionListener, new CancelListener());
            default:
                return super.onCreateDialog(resId, bundle);
        }
    }
}
