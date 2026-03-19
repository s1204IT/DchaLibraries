package com.android.contacts.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorUtils;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.AccountsLoader;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.util.List;

public class ContactEditorAccountsChangedActivity extends Activity implements AccountsLoader.AccountsListener {
    private static final String TAG = ContactEditorAccountsChangedActivity.class.getSimpleName();
    private AccountsListAdapter mAccountListAdapter;
    private AlertDialog mDialog;
    private ContactEditorUtils mEditorUtils;
    private int mSubId = SubInfoUtils.getInvalidSubId();
    private int mCheckCount = 0;
    private final AdapterView.OnItemClickListener mAccountListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            Log.d(ContactEditorAccountsChangedActivity.TAG, "the mCheckCount = " + ContactEditorAccountsChangedActivity.this.mCheckCount);
            if (ContactEditorAccountsChangedActivity.this.mAccountListAdapter == null) {
                Log.w(ContactEditorAccountsChangedActivity.TAG, "mAccountListAdapter = " + ContactEditorAccountsChangedActivity.this.mAccountListAdapter);
                return;
            }
            if (ContactEditorAccountsChangedActivity.this.mCheckCount >= 1) {
                Log.w(ContactEditorAccountsChangedActivity.TAG, "ignore multiple click, just return.");
            } else {
                ContactEditorAccountsChangedActivity.access$108(ContactEditorAccountsChangedActivity.this);
                ContactEditorAccountsChangedActivity.this.saveAccountAndReturnResult(ContactEditorAccountsChangedActivity.this.mAccountListAdapter.getItem(i));
            }
        }
    };
    private final View.OnClickListener mAddAccountClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            ContactEditorAccountsChangedActivity.this.startActivityForResult(ImplicitIntentsUtil.getIntentForAddingAccount(), 1);
        }
    };

    static int access$108(ContactEditorAccountsChangedActivity contactEditorAccountsChangedActivity) {
        int i = contactEditorAccountsChangedActivity.mCheckCount;
        contactEditorAccountsChangedActivity.mCheckCount = i + 1;
        return i;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mDialog != null && !this.mDialog.isShowing()) {
            this.mDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (ContactsApplicationEx.isContactsApplicationBusy()) {
            Log.w(TAG, "[onCreate]contacts busy, should not edit, finish");
            finish();
        }
        this.mEditorUtils = ContactEditorUtils.create(this);
        AccountsLoader.loadAccounts(this, 0, AccountTypeManager.writableFilter());
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        Log.d(TAG, "[onActivityResult] requestCode:" + i + ",resultCode:" + i2 + ",data:" + intent);
        if (i != 1 || i2 != -1) {
            return;
        }
        AccountWithDataSet createdAccount = this.mEditorUtils.getCreatedAccount(i2, intent);
        if (createdAccount == null) {
            Log.w(TAG, "[onActivityResult] account is null...");
            setResult(i2);
            finish();
            return;
        }
        saveAccountAndReturnResult(createdAccount);
    }

    private void updateDisplayedAccounts(List<AccountInfo> list) {
        View viewInflate;
        int size = list.size();
        if (size < 0) {
            throw new IllegalStateException("Cannot have a negative number of accounts");
        }
        if (size >= 2) {
            viewInflate = View.inflate(this, R.layout.contact_editor_accounts_changed_activity_with_picker, null);
            ((TextView) viewInflate.findViewById(R.id.text)).setText(getString(R.string.store_contact_to));
            Button button = (Button) viewInflate.findViewById(R.id.add_account_button);
            button.setText(getString(R.string.add_new_account));
            button.setOnClickListener(this.mAddAccountClickListener);
            ListView listView = (ListView) viewInflate.findViewById(R.id.account_list);
            this.mAccountListAdapter = new AccountsListAdapter(this, list);
            listView.setAdapter((ListAdapter) this.mAccountListAdapter);
            listView.setOnItemClickListener(this.mAccountListItemClickListener);
        } else if (size == 1 && !list.get(0).getAccount().isNullAccount()) {
            View viewInflate2 = View.inflate(this, R.layout.contact_editor_accounts_changed_activity_with_text, null);
            TextView textView = (TextView) viewInflate2.findViewById(R.id.text);
            Button button2 = (Button) viewInflate2.findViewById(R.id.left_button);
            Button button3 = (Button) viewInflate2.findViewById(R.id.right_button);
            final AccountInfo accountInfo = list.get(0);
            textView.setText(getString(R.string.contact_editor_prompt_one_account, new Object[]{accountInfo.getNameLabel()}));
            button2.setText(getString(R.string.add_new_account));
            button2.setOnClickListener(this.mAddAccountClickListener);
            button3.setText(getString(android.R.string.ok));
            button3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContactEditorAccountsChangedActivity.this.saveAccountAndReturnResult(accountInfo.getAccount());
                }
            });
            viewInflate = viewInflate2;
        } else {
            viewInflate = View.inflate(this, R.layout.contact_editor_accounts_changed_activity_with_text, null);
            TextView textView2 = (TextView) viewInflate.findViewById(R.id.text);
            Button button4 = (Button) viewInflate.findViewById(R.id.left_button);
            Button button5 = (Button) viewInflate.findViewById(R.id.right_button);
            textView2.setText(getString(R.string.contact_editor_prompt_zero_accounts));
            button4.setText(getString(android.R.string.cancel));
            button4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContactEditorAccountsChangedActivity.this.saveAccountAndReturnResult(AccountWithDataSet.getNullAccount());
                    ContactEditorAccountsChangedActivity.this.finish();
                }
            });
            button5.setText(getString(R.string.add_account));
            button5.setOnClickListener(this.mAddAccountClickListener);
        }
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        this.mDialog = new AlertDialog.Builder(this).setView(viewInflate).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ContactEditorAccountsChangedActivity.this.finish();
            }
        }).create();
        this.mDialog.show();
    }

    private void saveAccountAndReturnResult(AccountWithDataSet accountWithDataSet) {
        if (accountWithDataSet == null || accountWithDataSet.type == null) {
            if (accountWithDataSet != null && accountWithDataSet.isNullAccount()) {
                Log.d(TAG, "[saveAccountAndReturnResult] MTK not support null account!");
                return;
            }
        } else if (AccountTypeUtils.isAccountTypeIccCard(accountWithDataSet.type.toString())) {
            if (accountWithDataSet instanceof AccountWithDataSetEx) {
                this.mSubId = ((AccountWithDataSetEx) accountWithDataSet).getSubId();
            }
            if (!SimCardUtils.checkPHBStateAndSimStorage(this, this.mSubId)) {
                Log.d(TAG, "[saveAccountAndReturnResult] finish for PHB not ready");
                finish();
                return;
            }
        }
        this.mEditorUtils.saveDefaultAccount(accountWithDataSet);
        Intent intent = new Intent();
        intent.putExtra("android.provider.extra.ACCOUNT", accountWithDataSet);
        intent.putExtra("mSubId", this.mSubId);
        Log.d(TAG, "[saveAccountAndReturnResult] account is " + accountWithDataSet + ", mSubId is " + this.mSubId);
        setResult(-1, intent);
        finish();
    }

    @Override
    public void onAccountsLoaded(List<AccountInfo> list) {
        updateDisplayedAccounts(list);
    }
}
