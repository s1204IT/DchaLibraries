package com.android.bluetooth.map;

import android.R;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.mediatek.widget.AccountItemView;
import java.util.List;

public class BluetoothMapSubAdapter extends BaseAdapter {
    Context mContext;
    List<SubscriptionInfo> mList;

    public BluetoothMapSubAdapter(Context context, List<SubscriptionInfo> list) {
        this.mContext = context;
        this.mList = list;
    }

    @Override
    public int getCount() {
        return this.mList.size();
    }

    @Override
    public Object getItem(int i) {
        return this.mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        AccountItemView accountItemView;
        String string;
        if (view == null) {
            accountItemView = new AccountItemView(this.mContext);
        } else {
            accountItemView = (AccountItemView) view;
        }
        SubscriptionInfo subscriptionInfo = this.mList.get(i);
        if (subscriptionInfo.getSimSlotIndex() == -1) {
            if (subscriptionInfo.getDisplayName() == null) {
                string = null;
            } else {
                string = subscriptionInfo.getDisplayName().toString();
            }
            accountItemView.setAccountName(string);
            accountItemView.setAccountNumber((String) null);
            accountItemView.findViewById(R.id.icon).setVisibility(8);
            accountItemView.setClickable(true);
        } else {
            accountItemView.setClickable(false);
            accountItemView.setAccountName(subscriptionInfo.getDisplayName() != null ? subscriptionInfo.getDisplayName().toString() : null);
            accountItemView.setAccountNumber(subscriptionInfo.getNumber());
            accountItemView.findViewById(R.id.icon).setVisibility(0);
        }
        return accountItemView;
    }

    public void setAdapterData(List<SubscriptionInfo> list) {
        this.mList = list;
    }
}
