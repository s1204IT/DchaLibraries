package com.android.contacts.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountsListAdapter;
import com.android.contacts.editor.ContactEditorUtils;
import java.util.List;

public class ContactEditorAccountsChangedActivity extends Activity {
    private static final String TAG = ContactEditorAccountsChangedActivity.class.getSimpleName();
    private AccountsListAdapter mAccountListAdapter;
    private final AdapterView.OnItemClickListener mAccountListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (ContactEditorAccountsChangedActivity.this.mAccountListAdapter != null) {
                ContactEditorAccountsChangedActivity.this.saveAccountAndReturnResult(ContactEditorAccountsChangedActivity.this.mAccountListAdapter.getItem(position));
            }
        }
    };
    private final View.OnClickListener mAddAccountClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ContactEditorAccountsChangedActivity.this.startActivityForResult(ContactEditorAccountsChangedActivity.this.mEditorUtils.createAddWritableAccountIntent(), 1);
        }
    };
    private ContactEditorUtils mEditorUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mEditorUtils = ContactEditorUtils.getInstance(this);
        List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(this).getAccounts(true);
        int numAccounts = accounts.size();
        if (numAccounts < 0) {
            throw new IllegalStateException("Cannot have a negative number of accounts");
        }
        if (numAccounts >= 2) {
            setContentView(R.layout.contact_editor_accounts_changed_activity_with_picker);
            TextView textView = (TextView) findViewById(R.id.text);
            textView.setText(getString(R.string.contact_editor_prompt_multiple_accounts));
            Button button = (Button) findViewById(R.id.add_account_button);
            button.setText(getString(R.string.add_new_account));
            button.setOnClickListener(this.mAddAccountClickListener);
            ListView accountListView = (ListView) findViewById(R.id.account_list);
            this.mAccountListAdapter = new AccountsListAdapter(this, AccountsListAdapter.AccountListFilter.ACCOUNTS_CONTACT_WRITABLE);
            accountListView.setAdapter((ListAdapter) this.mAccountListAdapter);
            accountListView.setOnItemClickListener(this.mAccountListItemClickListener);
            return;
        }
        if (numAccounts == 1) {
            setContentView(R.layout.contact_editor_accounts_changed_activity_with_text);
            TextView textView2 = (TextView) findViewById(R.id.text);
            Button leftButton = (Button) findViewById(R.id.left_button);
            Button rightButton = (Button) findViewById(R.id.right_button);
            final AccountWithDataSet account = accounts.get(0);
            textView2.setText(getString(R.string.contact_editor_prompt_one_account, new Object[]{account.name}));
            leftButton.setText(getString(R.string.add_new_account));
            leftButton.setOnClickListener(this.mAddAccountClickListener);
            rightButton.setText(getString(android.R.string.ok));
            rightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContactEditorAccountsChangedActivity.this.saveAccountAndReturnResult(account);
                }
            });
            return;
        }
        setContentView(R.layout.contact_editor_accounts_changed_activity_with_text);
        TextView textView3 = (TextView) findViewById(R.id.text);
        Button leftButton2 = (Button) findViewById(R.id.left_button);
        Button rightButton2 = (Button) findViewById(R.id.right_button);
        textView3.setText(getString(R.string.contact_editor_prompt_zero_accounts));
        leftButton2.setText(getString(R.string.keep_local));
        leftButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContactEditorAccountsChangedActivity.this.mEditorUtils.saveDefaultAndAllAccounts(null);
                ContactEditorAccountsChangedActivity.this.setResult(-1);
                ContactEditorAccountsChangedActivity.this.finish();
            }
        });
        rightButton2.setText(getString(R.string.add_account));
        rightButton2.setOnClickListener(this.mAddAccountClickListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == -1) {
            AccountWithDataSet account = this.mEditorUtils.getCreatedAccount(resultCode, data);
            if (account == null) {
                setResult(resultCode);
                finish();
            } else {
                saveAccountAndReturnResult(account);
            }
        }
    }

    private void saveAccountAndReturnResult(AccountWithDataSet account) {
        this.mEditorUtils.saveDefaultAndAllAccounts(account);
        Intent intent = new Intent();
        intent.putExtra("com.android.contacts.extra.ACCOUNT", account);
        setResult(-1, intent);
        finish();
    }
}
