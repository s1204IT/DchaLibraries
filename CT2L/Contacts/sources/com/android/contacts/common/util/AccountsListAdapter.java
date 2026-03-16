package com.android.contacts.common.util;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import java.util.ArrayList;
import java.util.List;

public final class AccountsListAdapter extends BaseAdapter {
    private final AccountTypeManager mAccountTypes;
    private final List<AccountWithDataSet> mAccounts;
    private final Context mContext;
    private final LayoutInflater mInflater;

    public enum AccountListFilter {
        ALL_ACCOUNTS,
        ACCOUNTS_CONTACT_WRITABLE,
        ACCOUNTS_GROUP_WRITABLE,
        ACCOUNTS_CONTACT_WRITABLE_WITHOUT_SIM
    }

    public AccountsListAdapter(Context context, AccountListFilter accountListFilter) {
        this(context, accountListFilter, null);
    }

    public AccountsListAdapter(Context context, AccountListFilter accountListFilter, AccountWithDataSet currentAccount) {
        this.mContext = context;
        this.mAccountTypes = AccountTypeManager.getInstance(context);
        this.mAccounts = getAccounts(accountListFilter);
        if (currentAccount != null && !this.mAccounts.isEmpty() && !this.mAccounts.get(0).equals(currentAccount) && this.mAccounts.remove(currentAccount)) {
            this.mAccounts.add(0, currentAccount);
        }
        this.mInflater = LayoutInflater.from(context);
    }

    private List<AccountWithDataSet> getAccounts(AccountListFilter accountListFilter) {
        if (accountListFilter == AccountListFilter.ACCOUNTS_GROUP_WRITABLE) {
            return new ArrayList(this.mAccountTypes.getGroupWritableAccounts());
        }
        if (accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_WITHOUT_SIM) {
            return new ArrayList(this.mAccountTypes.getWritableAccountsWithoutSim());
        }
        return new ArrayList(this.mAccountTypes.getAccounts(accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View resultView = convertView != null ? convertView : this.mInflater.inflate(R.layout.account_selector_list_item, parent, false);
        TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
        ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);
        AccountWithDataSet account = this.mAccounts.get(position);
        AccountType accountType = this.mAccountTypes.getAccountType(account.type, account.dataSet);
        text1.setText(accountType.getDisplayLabel(this.mContext));
        text2.setText(account.name);
        text2.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        icon.setImageDrawable(accountType.getDisplayIcon(this.mContext));
        return resultView;
    }

    @Override
    public int getCount() {
        return this.mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int position) {
        return this.mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
