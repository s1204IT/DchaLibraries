package com.android.contacts.list;

import android.R;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactListItemView;

public class LegacyContactListAdapter extends ContactEntryListAdapter {
    static final String[] PEOPLE_PROJECTION = {"_id", "display_name", "phonetic_name", "starred", "mode"};
    private CharSequence mUnknownNameText;

    public LegacyContactListAdapter(Context context) {
        super(context);
        this.mUnknownNameText = context.getText(R.string.unknownName);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        loader.setUri(Contacts.People.CONTENT_URI);
        loader.setProjection(PEOPLE_PROJECTION);
        loader.setSortOrder("display_name");
    }

    public Uri getPersonUri(int position) {
        Cursor cursor = (Cursor) getItem(position);
        long personId = cursor.getLong(0);
        return ContentUris.withAppendedId(Contacts.People.CONTENT_URI, personId);
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
        bindPresence(view, cursor);
    }

    protected void bindName(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, 1, getContactNameDisplayOrder());
        view.showPhoneticName(cursor, 2);
    }

    protected void bindPresence(ContactListItemView view, Cursor cursor) {
        view.showPresenceAndStatusMessage(cursor, 4, 0);
    }
}
