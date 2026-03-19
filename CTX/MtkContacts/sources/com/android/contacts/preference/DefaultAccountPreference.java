package com.android.contacts.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter;
import com.mediatek.contacts.model.account.LocalPhoneAccountType;
import com.mediatek.contacts.util.AccountTypeUtils;
import java.util.List;

public class DefaultAccountPreference extends DialogPreference {
    private List<AccountInfo> mAccounts;
    private int mChosenIndex;
    private AccountsListAdapter mListAdapter;
    private ContactsPreferences mPreferences;

    public DefaultAccountPreference(Context context) {
        super(context);
        this.mChosenIndex = -1;
        prepare();
    }

    public DefaultAccountPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mChosenIndex = -1;
        prepare();
    }

    public void setAccounts(List<AccountInfo> list) {
        this.mAccounts = list;
        if (this.mListAdapter != null) {
            this.mListAdapter.setAccounts(list, null);
            notifyChanged();
        }
    }

    @Override
    protected View onCreateDialogView() {
        prepare();
        return super.onCreateDialogView();
    }

    private void prepare() {
        this.mPreferences = new ContactsPreferences(getContext());
        this.mListAdapter = new AccountsListAdapter(getContext());
        if (this.mAccounts != null) {
            this.mListAdapter.setAccounts(this.mAccounts, null);
        }
    }

    @Override
    protected boolean shouldPersist() {
        return false;
    }

    @Override
    public CharSequence getSummary() {
        AccountWithDataSet defaultAccount = this.mPreferences.getDefaultAccount();
        if (defaultAccount == null || this.mAccounts == null || !AccountInfo.contains(this.mAccounts, defaultAccount)) {
            return null;
        }
        AccountInfo account = AccountInfo.getAccount(this.mAccounts, defaultAccount);
        String str = account.getAccount().name;
        if (AccountTypeUtils.getSubIdBySimAccountName(getContext(), str) >= 0) {
            return AccountTypeUtils.getDisplayAccountName(getContext(), str);
        }
        if (account.getType() instanceof LocalPhoneAccountType) {
            return account.getType().getDisplayLabel(getContext());
        }
        return account.getNameLabel();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNegativeButton((CharSequence) null, (DialogInterface.OnClickListener) null);
        builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
        builder.setAdapter(this.mListAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DefaultAccountPreference.this.mChosenIndex = i;
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean z) {
        AccountWithDataSet defaultAccount = this.mPreferences.getDefaultAccount();
        if (this.mChosenIndex != -1 && this.mChosenIndex < this.mListAdapter.getCount()) {
            AccountWithDataSet item = this.mListAdapter.getItem(this.mChosenIndex);
            if (!item.equals(defaultAccount)) {
                this.mPreferences.setDefaultAccount(item);
                notifyChanged();
            }
        }
    }
}
