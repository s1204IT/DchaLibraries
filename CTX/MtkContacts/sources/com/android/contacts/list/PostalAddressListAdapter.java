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
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.ContactsPortableUtils;
import java.util.ArrayList;

public class PostalAddressListAdapter extends ContactEntryListAdapter {
    private final CharSequence mUnknownNameText;

    protected static class PostalQuery {
        private static final String[] PROJECTION_ALTERNATIVE;
        private static final String[] PROJECTION_ALTERNATIVE_INTERNAL;
        private static final String[] PROJECTION_PRIMARY;
        private static final String[] PROJECTION_PRIMARY_INTERNAL = {"_id", "data2", "data3", "data1", "photo_id", "lookup", "display_name"};

        static {
            ArrayList arrayListNewArrayList = Lists.newArrayList(PROJECTION_PRIMARY_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList.add("indicate_phone_or_sim_contact");
            }
            PROJECTION_PRIMARY = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
            PROJECTION_ALTERNATIVE_INTERNAL = new String[]{"_id", "data2", "data3", "data1", "photo_id", "lookup", "display_name_alt"};
            ArrayList arrayListNewArrayList2 = Lists.newArrayList(PROJECTION_ALTERNATIVE_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList2.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList2.add("is_sdn_contact");
            }
            PROJECTION_ALTERNATIVE = (String[]) arrayListNewArrayList2.toArray(new String[arrayListNewArrayList2.size()]);
        }
    }

    public PostalAddressListAdapter(Context context) {
        super(context);
        this.mUnknownNameText = context.getText(R.string.unknownName);
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long j) {
        Uri.Builder builderAppendQueryParameter = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI.buildUpon().appendQueryParameter("remove_duplicate_entries", "true");
        if (isSectionHeaderDisplayEnabled()) {
            builderAppendQueryParameter.appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true");
        }
        cursorLoader.setUri(builderAppendQueryParameter.build());
        if (getContactNameDisplayOrder() == 1) {
            cursorLoader.setProjection(PostalQuery.PROJECTION_PRIMARY);
        } else {
            cursorLoader.setProjection(PostalQuery.PROJECTION_ALTERNATIVE);
        }
        if (getSortOrder() == 1) {
            cursorLoader.setSortOrder("sort_key");
        } else {
            cursorLoader.setSortOrder("sort_key_alt");
        }
    }

    public Uri getDataUri(int i) {
        return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, ((Cursor) getItem(i)).getLong(0));
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        ContactListItemView contactListItemViewNewView = super.newView(context, i, cursor, i2, viewGroup);
        contactListItemViewNewView.setUnknownNameText(this.mUnknownNameText);
        contactListItemViewNewView.setQuickContactEnabled(isQuickContactEnabled());
        contactListItemViewNewView.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        return contactListItemViewNewView;
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        bindSectionHeaderAndDivider(contactListItemView, i2);
        bindName(contactListItemView, cursor);
        bindViewId(contactListItemView, cursor, 0);
        bindPhoto(contactListItemView, cursor);
        bindPostalAddress(contactListItemView, cursor);
    }

    protected void bindPostalAddress(ContactListItemView contactListItemView, Cursor cursor) {
        CharSequence typeLabel;
        if (!cursor.isNull(1)) {
            typeLabel = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(getContext().getResources(), cursor.getInt(1), cursor.getString(2));
        } else {
            typeLabel = null;
        }
        contactListItemView.setLabel(typeLabel);
        contactListItemView.showData(cursor, 3);
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView contactListItemView, int i) {
        int sectionForPosition = getSectionForPosition(i);
        if (getPositionForSection(sectionForPosition) == i) {
            contactListItemView.setSectionHeader((String) getSections()[sectionForPosition]);
        } else {
            contactListItemView.setSectionHeader(null);
        }
    }

    protected void bindName(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showDisplayName(cursor, 6, getContactNameDisplayOrder());
    }

    protected void bindPhoto(ContactListItemView contactListItemView, Cursor cursor) {
        long j = !cursor.isNull(4) ? cursor.getLong(4) : 0L;
        getPhotoLoader().loadThumbnail(contactListItemView.getPhotoView(), j, false, getCircularPhotos(), j == 0 ? getDefaultImageRequestFromCursor(cursor, 6, 5) : null);
    }
}
