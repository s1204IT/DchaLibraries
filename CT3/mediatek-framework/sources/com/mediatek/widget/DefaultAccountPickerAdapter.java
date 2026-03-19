package com.mediatek.widget;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import com.mediatek.widget.CustomAccountRemoteViews;
import java.util.List;

public class DefaultAccountPickerAdapter extends BaseAdapter {
    private static String TAG = "DefaultAccountPickerAdapter";
    private Context mContext;
    private List<CustomAccountRemoteViews.AccountInfo> mData;

    public DefaultAccountPickerAdapter(Context context) {
        this.mContext = context;
    }

    void setItemData(List<CustomAccountRemoteViews.AccountInfo> data) {
        this.mData = data;
    }

    void setActiveStatus(int position) {
        for (CustomAccountRemoteViews.AccountInfo accountInfo : this.mData) {
            accountInfo.setActiveStatus(false);
        }
        if (position > -1 && position < this.mData.size()) {
            this.mData.get(position).setActiveStatus(true);
        } else {
            Log.d(TAG, "set the wrong active position: " + position);
        }
    }

    int getActivePosition() {
        for (int i = 0; i < this.mData.size(); i++) {
            if (this.mData.get(i).isActive()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getCount() {
        return this.mData.size();
    }

    @Override
    public CustomAccountRemoteViews.AccountInfo getItem(int position) {
        return this.mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        ViewHolder viewHolder2 = null;
        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            convertView = inflator.inflate(134676498, (ViewGroup) null);
            viewHolder = new ViewHolder(this, viewHolder2);
            viewHolder.accountItem = (AccountItemView) convertView.findViewById(135331920);
            viewHolder.radioButton = (RadioButton) convertView.findViewById(135331919);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        CustomAccountRemoteViews.AccountInfo accountInfo = getItem(position);
        viewHolder.accountItem.setAccountIcon(accountInfo.getIcon());
        viewHolder.accountItem.setAccountName(accountInfo.getLabel());
        viewHolder.accountItem.setAccountNumber(accountInfo.getNumber());
        viewHolder.radioButton.setChecked(accountInfo.isActive());
        return convertView;
    }

    private class ViewHolder {
        AccountItemView accountItem;
        RadioButton radioButton;

        ViewHolder(DefaultAccountPickerAdapter this$0, ViewHolder viewHolder) {
            this();
        }

        private ViewHolder() {
        }
    }
}
