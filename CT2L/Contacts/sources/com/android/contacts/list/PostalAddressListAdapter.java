package com.android.contacts.list;

import android.R;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactListItemView;

public class PostalAddressListAdapter extends ContactEntryListAdapter {
    private final CharSequence mUnknownNameText;

    protected static class PostalQuery {
        private static final String[] PROJECTION_PRIMARY = {"_id", "data2", "data3", "data1", "photo_id", "lookup", "display_name"};
        private static final String[] PROJECTION_ALTERNATIVE = {"_id", "data2", "data3", "data1", "photo_id", "lookup", "display_name_alt"};
    }

    public PostalAddressListAdapter(Context context) {
        super(context);
        this.mUnknownNameText = context.getText(R.string.unknownName);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        Uri.Builder builder = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI.buildUpon().appendQueryParameter("remove_duplicate_entries", "true");
        if (isSectionHeaderDisplayEnabled()) {
            builder.appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true");
        }
        loader.setUri(builder.build());
        if (getContactNameDisplayOrder() == 1) {
            loader.setProjection(PostalQuery.PROJECTION_PRIMARY);
        } else {
            loader.setProjection(PostalQuery.PROJECTION_ALTERNATIVE);
        }
        if (getSortOrder() == 1) {
            loader.setSortOrder("sort_key");
        } else {
            loader.setSortOrder("sort_key_alt");
        }
    }

    public Uri getDataUri(int position) {
        long id = ((Cursor) getItem(position)).getLong(0);
        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
    }

    @Override
    protected ContactListItemView newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        ContactListItemView view = super.newView(context, partition, cursor, position, parent);
        view.setUnknownNameText(this.mUnknownNameText);
        view.setQuickContactEnabled(isQuickContactEnabled());
        view.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        return view;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        ContactListItemView view = (ContactListItemView) itemView;
        bindSectionHeaderAndDivider(view, position);
        bindName(view, cursor);
        bindViewId(view, cursor, 0);
        bindPhoto(view, cursor);
        bindPostalAddress(view, cursor);
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

    protected void bindSectionHeaderAndDivider(ContactListItemView view, int position) {
        int section = getSectionForPosition(position);
        if (getPositionForSection(section) == position) {
            String title = (String) getSections()[section];
            view.setSectionHeader(title);
        } else {
            view.setSectionHeader(null);
        }
    }

    protected void bindName(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, 6, getContactNameDisplayOrder());
    }

    protected void bindPhoto(ContactListItemView view, Cursor cursor) {
        long photoId = 0;
        if (!cursor.isNull(4)) {
            photoId = cursor.getLong(4);
        }
        ContactPhotoManager.DefaultImageRequest request = null;
        if (photoId == 0) {
            request = getDefaultImageRequestFromCursor(cursor, 6, 5);
        }
        getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false, getCircularPhotos(), request);
    }
}
