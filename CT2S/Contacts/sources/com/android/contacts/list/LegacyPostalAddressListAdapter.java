package com.android.contacts.list;

import android.R;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactListItemView;

public class LegacyPostalAddressListAdapter extends ContactEntryListAdapter {
    static final String[] POSTALS_PROJECTION = {"_id", "type", "label", "data", "display_name", "phonetic_name"};
    private CharSequence mUnknownNameText;

    public LegacyPostalAddressListAdapter(Context context) {
        super(context);
        this.mUnknownNameText = context.getText(R.string.unknownName);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        loader.setUri(Contacts.ContactMethods.CONTENT_URI);
        loader.setProjection(POSTALS_PROJECTION);
        loader.setSortOrder("display_name");
        loader.setSelection("kind=2");
    }

    public Uri getContactMethodUri(int position) {
        Cursor cursor = (Cursor) getItem(position);
        long id = cursor.getLong(0);
        return ContentUris.withAppendedId(Contacts.ContactMethods.CONTENT_URI, id);
    }

    @Override
    protected ContactListItemView newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        ContactListItemView view = new ContactListItemView(context, null);
        view.setUnknownNameText(this.mUnknownNameText);
        return view;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        ContactListItemView view = (ContactListItemView) itemView;
        bindName(view, cursor);
        bindViewId(view, cursor, 0);
        bindPostalAddress(view, cursor);
    }

    protected void bindName(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, 4, getContactNameDisplayOrder());
        view.showPhoneticName(cursor, 5);
    }

    protected void bindPostalAddress(ContactListItemView view, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(1)) {
            int type = cursor.getInt(1);
            String customLabel = cursor.getString(2);
            label = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(getContext().getResources(), type, customLabel);
        }
        view.setLabel(label);
        view.showData(cursor, 3);
    }
}
