package com.android.contacts.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.util.AccountsListAdapterUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AccountsListAdapter extends BaseAdapter {
    private List<AccountInfo> mAccounts;
    private final Context mContext;
    private int mCustomLayout;
    private final LayoutInflater mInflater;

    public AccountsListAdapter(Context context) {
        this(context, Collections.emptyList(), null);
    }

    public AccountsListAdapter(Context context, List<AccountInfo> list) {
        this(context, list, null);
    }

    public AccountsListAdapter(Context context, List<AccountInfo> list, AccountWithDataSet accountWithDataSet) {
        this.mCustomLayout = -1;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mAccounts = new ArrayList(list.size());
        setAccounts(list, accountWithDataSet);
    }

    public void setAccounts(List<AccountInfo> list, AccountWithDataSet accountWithDataSet) {
        AccountInfo account;
        if (this.mAccounts.isEmpty()) {
            account = AccountInfo.getAccount(list, accountWithDataSet);
        } else {
            account = AccountInfo.getAccount(list, this.mAccounts.get(0).getAccount());
        }
        this.mAccounts.clear();
        this.mAccounts.addAll(list);
        if (account != null && !this.mAccounts.isEmpty() && !this.mAccounts.get(0).sameAccount(accountWithDataSet) && this.mAccounts.remove(account)) {
            this.mAccounts.add(0, account);
        }
        notifyDataSetChanged();
    }

    public void setCustomLayout(int i) {
        this.mCustomLayout = i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.mInflater.inflate(this.mCustomLayout > 0 ? this.mCustomLayout : R.layout.account_selector_list_item_condensed, viewGroup, false);
        }
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        textView.setText(this.mAccounts.get(i).getType().getDisplayLabel(this.mContext));
        AccountsListAdapterUtils.getViewForName(this.mContext, this.mAccounts.get(i).getAccount(), this.mAccounts.get(i).getType(), textView2, imageView);
        return view;
    }

    @Override
    public int getCount() {
        return this.mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int i) {
        return this.mAccounts.get(i).getAccount();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
}
