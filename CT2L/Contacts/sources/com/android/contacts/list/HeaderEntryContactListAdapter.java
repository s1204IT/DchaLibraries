package com.android.contacts.list;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.DefaultContactListAdapter;

public class HeaderEntryContactListAdapter extends DefaultContactListAdapter {
    private boolean mShowCreateContact;

    public HeaderEntryContactListAdapter(Context context) {
        super(context);
    }

    private int getHeaderEntryCount() {
        return (isSearchMode() || !this.mShowCreateContact) ? 0 : 1;
    }

    public void setShowCreateContact(boolean showCreateContact) {
        this.mShowCreateContact = showCreateContact;
        invalidate();
    }

    @Override
    public int getCount() {
        return super.getCount() + getHeaderEntryCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ContactListItemView itemView;
        if (position == 0 && getHeaderEntryCount() > 0) {
            if (convertView == null) {
                itemView = newView(getContext(), 0, getCursor(0), 0, parent);
            } else {
                itemView = (ContactListItemView) convertView;
            }
            itemView.setDrawableResource(R.drawable.search_shortcut_background, R.drawable.ic_search_add_contact);
            itemView.setDisplayName(getContext().getResources().getString(R.string.header_entry_contact_list_adapter_header_title));
            return itemView;
        }
        return super.getView(position - getHeaderEntryCount(), convertView, parent);
    }

    @Override
    public Object getItem(int position) {
        return super.getItem(position - getHeaderEntryCount());
    }

    @Override
    public boolean isEnabled(int position) {
        return position < getHeaderEntryCount() || super.isEnabled(position - getHeaderEntryCount());
    }

    @Override
    public int getPartitionForPosition(int position) {
        return super.getPartitionForPosition(position - getHeaderEntryCount());
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, getHeaderEntryCount() + position);
    }

    @Override
    public int getItemViewType(int position) {
        return (position != 0 || getHeaderEntryCount() <= 0) ? super.getItemViewType(position - getHeaderEntryCount()) : getViewTypeCount() - 1;
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount() + 1;
    }

    @Override
    protected boolean getExtraStartingSection() {
        return getHeaderEntryCount() > 0;
    }
}
