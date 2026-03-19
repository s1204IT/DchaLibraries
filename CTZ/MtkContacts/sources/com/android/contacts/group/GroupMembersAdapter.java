package com.android.contacts.group;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class GroupMembersAdapter extends MultiSelectEntryContactListAdapter {
    private boolean mDisplayDeleteButtons;
    private long mGroupId;
    private final CharSequence mUnknownNameText;

    public static class GroupMembersQuery {
        private static final String[] PROJECTION_ALTERNATIVE;
        private static final String[] PROJECTION_ALTERNATIVE_INTERNAL;
        private static final String[] PROJECTION_PRIMARY;
        private static final String[] PROJECTION_PRIMARY_INTERNAL = {"contact_id", "raw_contact_id", "photo_id", "lookup", "contact_presence", "contact_status", "display_name"};

        static {
            ArrayList arrayListNewArrayList = Lists.newArrayList(PROJECTION_PRIMARY_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList.add("index_in_sim");
                arrayListNewArrayList.add("is_sdn_contact");
            }
            PROJECTION_PRIMARY = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
            PROJECTION_ALTERNATIVE_INTERNAL = new String[]{"contact_id", "raw_contact_id", "photo_id", "lookup", "contact_presence", "contact_status", "display_name_alt"};
            ArrayList arrayListNewArrayList2 = Lists.newArrayList(PROJECTION_ALTERNATIVE_INTERNAL);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList2.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList2.add("index_in_sim");
                arrayListNewArrayList2.add("is_sdn_contact");
            }
            PROJECTION_ALTERNATIVE = (String[]) arrayListNewArrayList2.toArray(new String[arrayListNewArrayList2.size()]);
        }
    }

    public GroupMembersAdapter(Context context) {
        super(context, 0);
        this.mUnknownNameText = context.getText(R.string.missing_name);
    }

    public void setGroupId(long j) {
        this.mGroupId = j;
    }

    public long getGroupId() {
        Log.d("GroupMembersAdapter", "[getGroupId] mGroupId = " + this.mGroupId);
        return this.mGroupId;
    }

    public Uri getContactUri(int i) {
        Cursor cursor = (Cursor) getItem(i);
        return ContactsContract.Contacts.getLookupUri(cursor.getLong(0), cursor.getString(3));
    }

    public long getContactId(int i) {
        return ((Cursor) getItem(i)).getLong(0);
    }

    public void setDisplayDeleteButtons(boolean z) {
        this.mDisplayDeleteButtons = z;
        notifyDataSetChanged();
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long j) {
        cursorLoader.setUri(ContactsContract.Data.CONTENT_URI.buildUpon().appendQueryParameter("directory", String.valueOf(0L)).appendQueryParameter("android.provider.extra.ADDRESS_BOOK_INDEX", "true").build());
        cursorLoader.setSelection("mimetype=? AND data1=?");
        cursorLoader.setSelectionArgs(new String[]{"vnd.android.cursor.item/group_membership", String.valueOf(this.mGroupId)});
        cursorLoader.setProjection(getContactNameDisplayOrder() == 1 ? GroupMembersQuery.PROJECTION_PRIMARY : GroupMembersQuery.PROJECTION_ALTERNATIVE);
        cursorLoader.setSortOrder(getSortOrder() == 1 ? "sort_key" : "sort_key_alt");
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        ContactListItemView contactListItemViewNewView = super.newView(context, i, cursor, i2, viewGroup);
        contactListItemViewNewView.setUnknownNameText(this.mUnknownNameText);
        return contactListItemViewNewView;
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2);
        ContactListItemView contactListItemView = (ContactListItemView) view;
        bindSectionHeaderAndDivider(contactListItemView, i2);
        bindName(contactListItemView, cursor);
        bindPhoto(contactListItemView, cursor, 2, 3, 6);
        bindDeleteButton(contactListItemView, i2);
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView contactListItemView, int i) {
        contactListItemView.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
        if (isSectionHeaderDisplayEnabled()) {
            contactListItemView.setSectionHeader(getItemPlacementInSection(i).sectionHeader);
        } else {
            contactListItemView.setSectionHeader(null);
        }
    }

    private void bindName(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.showDisplayName(cursor, 6, getContactNameDisplayOrder());
    }

    private void bindDeleteButton(ContactListItemView contactListItemView, int i) {
        if (this.mDisplayDeleteButtons) {
            contactListItemView.getDeleteImageButton(getDeleteContactListener(), i);
        } else {
            contactListItemView.hideDeleteImageButton();
        }
    }
}
