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
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListItemView;

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

    public void setTargetContactId(long targetContactId) {
        this.mTargetContactId = targetContactId;
    }

    @Override
    public void configureLoader(CursorLoader cursorLoader, long directoryId) {
        Uri allContactsUri;
        JoinContactLoader loader = (JoinContactLoader) cursorLoader;
        Uri.Builder builder = ContactsContract.Contacts.CONTENT_URI.buildUpon();
        builder.appendEncodedPath(String.valueOf(this.mTargetContactId));
        builder.appendEncodedPath("suggestions");
        String filter = getQueryString();
        if (!TextUtils.isEmpty(filter)) {
            builder.appendEncodedPath(Uri.encode(filter));
        }
        builder.appendQueryParameter("limit", String.valueOf(4));
        loader.setSuggestionUri(builder.build());
        loader.setProjection(getProjection(false));
        if (!TextUtils.isEmpty(filter)) {
            allContactsUri = buildSectionIndexerUri(ContactsContract.Contacts.CONTENT_FILTER_URI).buildUpon().appendEncodedPath(Uri.encode(filter)).appendQueryParameter("directory", String.valueOf(0L)).build();
        } else {
            allContactsUri = buildSectionIndexerUri(ContactsContract.Contacts.CONTENT_URI).buildUpon().appendQueryParameter("directory", String.valueOf(0L)).build();
        }
        loader.setUri(allContactsUri);
        loader.setSelection("_id!=?");
        loader.setSelectionArgs(new String[]{String.valueOf(this.mTargetContactId)});
        if (getSortOrder() == 1) {
            loader.setSortOrder("sort_key");
        } else {
            loader.setSortOrder("sort_key_alt");
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
    public void configureDefaultPartition(boolean showIfEmpty, boolean hasHeader) {
        super.configureDefaultPartition(false, true);
    }

    @Override
    public int getViewTypeCount() {
        return super.getViewTypeCount();
    }

    @Override
    public int getItemViewType(int partition, int position) {
        return super.getItemViewType(partition, position);
    }

    @Override
    protected View newHeaderView(Context context, int partition, Cursor cursor, ViewGroup parent) {
        switch (partition) {
            case 0:
                View view = inflate(R.layout.join_contact_picker_section_header, parent);
                ((TextView) view.findViewById(R.id.text)).setText(R.string.separatorJoinAggregateSuggestions);
                return view;
            case 1:
                View view2 = inflate(R.layout.join_contact_picker_section_header, parent);
                ((TextView) view2.findViewById(R.id.text)).setText(R.string.separatorJoinAggregateAll);
                return view2;
            default:
                return null;
        }
    }

    @Override
    protected void bindHeaderView(View view, int partitionIndex, Cursor cursor) {
    }

    @Override
    protected ContactListItemView newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        switch (partition) {
            case 0:
            case 1:
                return super.newView(context, partition, cursor, position, parent);
            default:
                return null;
        }
    }

    private View inflate(int layoutId, ViewGroup parent) {
        return LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        switch (partition) {
            case 0:
                ContactListItemView view = (ContactListItemView) itemView;
                view.setSectionHeader(null);
                bindPhoto(view, partition, cursor);
                bindNameAndViewId(view, cursor);
                break;
            case 1:
                ContactListItemView view2 = (ContactListItemView) itemView;
                bindSectionHeaderAndDivider(view2, position, cursor);
                bindPhoto(view2, partition, cursor);
                bindNameAndViewId(view2, cursor);
                break;
        }
    }

    @Override
    public Uri getContactUri(int partitionIndex, Cursor cursor) {
        long contactId = cursor.getLong(0);
        String lookupKey = cursor.getString(6);
        return ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
    }
}
