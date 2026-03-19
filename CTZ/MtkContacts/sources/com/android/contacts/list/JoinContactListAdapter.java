package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.contacts.R;
import com.mediatek.contacts.util.ContactsPortableUtils;

public class JoinContactListAdapter extends ContactListAdapter {
    private long mTargetContactId;

    public JoinContactListAdapter(Context context) {
        super(context);
        setPinnedPartitionHeadersEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setIndexedPartition(1);
        setDirectorySearchMode(0);
    }

    @Override
    protected void addPartitions() {
        addPartition(false, true);
        addPartition(createDefaultDirectoryPartition());
    }

    public void setTargetContactId(long j) {
        this.mTargetContactId = j;
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long j) {
        Uri uriBuild;
        JoinContactLoader joinContactLoader = (JoinContactLoader) cursorLoader;
        Uri.Builder builderBuildUpon = ContactsContract.Contacts.CONTENT_URI.buildUpon();
        builderBuildUpon.appendEncodedPath(String.valueOf(this.mTargetContactId));
        builderBuildUpon.appendEncodedPath("suggestions");
        String queryString = getQueryString();
        if (!TextUtils.isEmpty(queryString)) {
            builderBuildUpon.appendEncodedPath(Uri.encode(queryString));
        }
        builderBuildUpon.appendQueryParameter("limit", String.valueOf(4));
        joinContactLoader.setSuggestionUri(builderBuildUpon.build());
        joinContactLoader.setProjection(getProjection(false));
        if (!TextUtils.isEmpty(queryString)) {
            uriBuild = buildSectionIndexerUri(ContactsContract.Contacts.CONTENT_FILTER_URI).buildUpon().appendEncodedPath(Uri.encode(queryString)).appendQueryParameter("directory", String.valueOf(0L)).build();
        } else {
            uriBuild = buildSectionIndexerUri(ContactsContract.Contacts.CONTENT_URI).buildUpon().appendQueryParameter("directory", String.valueOf(0L)).build();
        }
        joinContactLoader.setUri(uriBuild);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            joinContactLoader.setSelection("_id!=? AND indicate_phone_or_sim_contact=-1");
        } else {
            joinContactLoader.setSelection("_id!=?");
        }
        joinContactLoader.setSelectionArgs(new String[]{String.valueOf(this.mTargetContactId)});
        if (getSortOrder() == 1) {
            joinContactLoader.setSortOrder("sort_key");
        } else {
            joinContactLoader.setSortOrder("sort_key_alt");
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public void setSuggestionsCursor(Cursor cursor) {
        changeCursor(0, cursor);
    }

    @Override
    public void configureDefaultPartition(boolean z, boolean z2) {
        super.configureDefaultPartition(false, true);
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    @Override
    public int getItemViewType(int i, int i2) {
        return super.getItemViewType(i, i2);
    }

    @Override
    protected View newHeaderView(Context context, int i, Cursor cursor, ViewGroup viewGroup) {
        switch (i) {
            case 0:
                View viewInflate = inflate(R.layout.join_contact_picker_section_header, viewGroup);
                ((TextView) viewInflate.findViewById(R.id.text)).setText(R.string.separatorJoinAggregateSuggestions);
                return viewInflate;
            case 1:
                View viewInflate2 = inflate(R.layout.join_contact_picker_section_header, viewGroup);
                ((TextView) viewInflate2.findViewById(R.id.text)).setText(R.string.separatorJoinAggregateAll);
                return viewInflate2;
            default:
                return null;
        }
    }

    @Override
    protected void bindHeaderView(View view, int i, Cursor cursor) {
    }

    @Override
    protected ContactListItemView newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        switch (i) {
            case 0:
            case 1:
                return super.newView(context, i, cursor, i2, viewGroup);
            default:
                return null;
        }
    }

    private View inflate(int i, ViewGroup viewGroup) {
        return LayoutInflater.from(getContext()).inflate(i, viewGroup, false);
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        super.bindView(view, i, cursor, i2);
        switch (i) {
            case 0:
                ContactListItemView contactListItemView = (ContactListItemView) view;
                contactListItemView.setSectionHeader(null);
                bindPhoto(contactListItemView, i, cursor);
                bindNameAndViewId(contactListItemView, cursor);
                break;
            case 1:
                ContactListItemView contactListItemView2 = (ContactListItemView) view;
                bindSectionHeaderAndDivider(contactListItemView2, i2, cursor);
                bindPhoto(contactListItemView2, i, cursor);
                bindNameAndViewId(contactListItemView2, cursor);
                break;
        }
    }

    @Override
    public Uri getContactUri(int i, Cursor cursor) {
        return ContactsContract.Contacts.getLookupUri(cursor.getLong(0), cursor.getString(6));
    }
}
