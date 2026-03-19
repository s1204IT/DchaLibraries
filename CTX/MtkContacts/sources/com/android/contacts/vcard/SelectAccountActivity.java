package com.android.contacts.vcard;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountSelectionUtil;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.VcardUtils;
import java.util.List;

public class SelectAccountActivity extends Activity {
    private AccountSelectionUtil.AccountSelectedListener mAccountSelectionListener;

    private class CancelListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private CancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            SelectAccountActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            SelectAccountActivity.this.finish();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        List<AccountWithDataSet> listBlockForWritableAccounts = AccountTypeManager.getInstance(this).blockForWritableAccounts();
        if (listBlockForWritableAccounts.size() == 0) {
            Log.w("SelectAccountActivity", "Account does not exist");
            finish();
            return;
        }
        if (listBlockForWritableAccounts.size() == 1) {
            AccountWithDataSet accountWithDataSet = listBlockForWritableAccounts.get(0);
            Intent intent = new Intent();
            intent.putExtra("account_name", accountWithDataSet.name);
            intent.putExtra("account_type", accountWithDataSet.type);
            intent.putExtra("data_set", accountWithDataSet.dataSet);
            setResult(-1, intent);
            finish();
            return;
        }
        Log.i("SelectAccountActivity", "The number of available accounts: " + listBlockForWritableAccounts.size());
        this.mAccountSelectionListener = new AccountSelectionUtil.AccountSelectedListener(this, VcardUtils.addNonSimAccount(listBlockForWritableAccounts), R.string.import_from_vcf_file) {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                Log.d("SelectAccountActivity", "mAccountList.size() : " + this.mAccountList.size());
                AccountWithDataSet accountWithDataSet2 = this.mAccountList.get(i);
                Intent intent2 = new Intent();
                intent2.putExtra("account_name", accountWithDataSet2.name);
                intent2.putExtra("account_type", accountWithDataSet2.type);
                intent2.putExtra("data_set", accountWithDataSet2.dataSet);
                SelectAccountActivity.this.setResult(-1, intent2);
                SelectAccountActivity.this.finish();
            }
        };
        showDialog(R.string.import_from_vcf_file);
    }

    @Override
    protected Dialog onCreateDialog(int i, Bundle bundle) {
        if (i == R.string.import_from_vcf_file) {
            if (this.mAccountSelectionListener == null) {
                throw new NullPointerException("mAccountSelectionListener must not be null.");
            }
            return VcardUtils.getSelectAccountDialog(this, i, this.mAccountSelectionListener, new CancelListener());
        }
        return super.onCreateDialog(i, bundle);
    }
}
