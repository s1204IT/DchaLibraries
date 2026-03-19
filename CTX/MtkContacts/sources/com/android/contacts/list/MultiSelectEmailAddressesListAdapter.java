package com.android.contacts.list;

import android.R;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.group.GroupUtil;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.ContactsPortableUtils;
import java.util.ArrayList;

public class MultiSelectEmailAddressesListAdapter extends MultiSelectEntryContactListAdapter {
    private long[] mContactIdsFilter;
    private final CharSequence mUnknownNameText;

    protected static class EmailQuery {
        private static final String[] PROJECTION_ALTERNATIVE;
        public static final String[] PROJECTION_ALTERNATIVE_INTERNAL;
        private static final String[] PROJECTION_PRIMARY;
        public static final String[] PROJECTION_PRIMARY_INTERNAL = {"_id", "data2", "data3", "data1", "contact_id", "lookup", "photo_id", "display_name", "photo_thumb_uri"};

        static {
            ArrayList arrayListNewArrayList = Lists.newArrayList(PROJECTION_PRIMARY_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList.add("is_sdn_contact");
            }
            PROJECTION_PRIMARY = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
            PROJECTION_ALTERNATIVE_INTERNAL = new String[]{"_id", "data2", "data3", "data1", "contact_id", "lookup", "photo_id", "display_name_alt", "photo_thumb_uri"};
            ArrayList arrayListNewArrayList2 = Lists.newArrayList(PROJECTION_ALTERNATIVE_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList2.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList2.add("is_sdn_contact");
            }
            PROJECTION_ALTERNATIVE = (String[]) arrayListNewArrayList2.toArray(new String[arrayListNewArrayList2.size()]);
        }
    }

    public MultiSelectEmailAddressesListAdapter(Context context) {
        super(context, 0);
        this.mContactIdsFilter = null;
        this.mUnknownNameText = context.getText(R.string.unknownName);
    }

    public void setArguments(Bundle bundle) {
        this.mContactIdsFilter = bundle.getLongArray("com.android.contacts.extra.SELECTION_ITEM_LIST");
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long j) {
        Uri.Builder builderBuildUpon;
        if (isSearchMode()) {
            builderBuildUpon = ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon();
            String queryString = getQueryString();
            if (TextUtils.isEmpty(queryString)) {
                queryString = "";
            }
            builderBuildUpon.appendPath(queryString);
        } else {
            builderBuildUpon = ContactsContract.CommonDataKinds.Email.CONTENT_URI.buildUpon();
            if (isSectionHeaderDisplayEnabled()) {
                builderBuildUpon.appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true");
            }
        }
        builderBuildUpon.appendQueryParameter("directory", String.valueOf(j));
        cursorLoader.setUri(builderBuildUpon.build());
        if (this.mContactIdsFilter != null) {
            cursorLoader.setSelection("contact_id IN (" + GroupUtil.convertArrayToString(this.mContactIdsFilter) + ")");
        }
        if (getContactNameDisplayOrder() == 1) {
            cursorLoader.setProjection(EmailQuery.PROJECTION_PRIMARY);
        } else {
            cursorLoader.setProjection(EmailQuery.PROJECTION_ALTERNATIVE);
        }
        if (getSortOrder() == 1) {
            cursorLoader.setSortOrder("sort_key");
        } else {
            cursorLoader.setSortOrder("sort_key_alt");
        }
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        ContactListItemView contactListItemViewNewView = super.newView(context, i, cursor, i2, viewGroup);
        contactListItemViewNewView.setUnknownNameText(this.mUnknownNameText);
        contactListItemViewNewView.setQuickContactEnabled(isQuickContactEnabled());
        return contactListItemViewNewView;
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        cursor.moveToPosition(i2);
        boolean z = (cursor.moveToPrevious() && !cursor.isBeforeFirst() && cursor.getLong(4) == cursor.getLong(4)) ? false : true;
        cursor.moveToPosition(i2);
        bindViewId(contactListItemView, cursor, 0);
        if (z) {
            bindName(contactListItemView, cursor);
            bindPhoto(contactListItemView, cursor, 6, 5, 7);
        } else {
            unbindName(contactListItemView);
            contactListItemView.removePhotoView(true, false);
        }
        bindEmailAddress(contactListItemView, cursor);
    }

    protected void unbindName(ContactListItemView contactListItemView) {
        contactListItemView.hideDisplayName();
    }

    protected void bindEmailAddress(ContactListItemView contactListItemView, Cursor cursor) {
        CharSequence typeLabel;
        if (!cursor.isNull(1)) {
            typeLabel = ContactsContract.CommonDataKinds.Email.getTypeLabel(getContext().getResources(), cursor.getInt(1), cursor.getString(2));
        } else {
            typeLabel = null;
        }
        contactListItemView.setLabel(typeLabel);
        contactListItemView.showData(cursor, 3);
    }

    protected void bindName(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showDisplayName(cursor, 7, getContactNameDisplayOrder());
    }
}
