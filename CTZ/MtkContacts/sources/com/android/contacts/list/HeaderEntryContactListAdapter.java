package com.android.contacts.list;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;

public class HeaderEntryContactListAdapter extends DefaultContactListAdapter {
    private boolean mShowCreateContact;

    public HeaderEntryContactListAdapter(Context context) {
        super(context);
    }

    private int getHeaderEntryCount() {
        return (isSearchMode() || !this.mShowCreateContact) ? 0 : 1;
    }

    public void setShowCreateContact(boolean z) {
        this.mShowCreateContact = z;
        invalidate();
    }

    @Override
    public int getCount() {
        return super.getCount() + getHeaderEntryCount();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ContactListItemView contactListItemViewNewView;
        if (i == 0 && getHeaderEntryCount() > 0) {
            if (view == null) {
                contactListItemViewNewView = newView(getContext(), 0, getCursor(0), 0, viewGroup);
            } else {
                contactListItemViewNewView = (ContactListItemView) view;
            }
            contactListItemViewNewView.setDrawableResource(R.drawable.quantum_ic_person_add_vd_theme_24);
            contactListItemViewNewView.setDisplayName(getContext().getResources().getString(R.string.header_entry_contact_list_adapter_header_title));
            return contactListItemViewNewView;
        }
        return super.getView(i - getHeaderEntryCount(), view, viewGroup);
    }

    @Override
    public Object getItem(int i) {
        return super.getItem(i - getHeaderEntryCount());
    }

    @Override
    public boolean isEnabled(int i) {
        return i < getHeaderEntryCount() || super.isEnabled(i - getHeaderEntryCount());
    }

    @Override
    public int getPartitionForPosition(int i) {
        return super.getPartitionForPosition(i - getHeaderEntryCount());
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2 + getHeaderEntryCount());
    }

    @Override
    public int getItemViewType(int i) {
        if (i == 0 && getHeaderEntryCount() > 0) {
            return getViewTypeCount() - 1;
        }
        return super.getItemViewType(i - getHeaderEntryCount());
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
