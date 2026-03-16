package com.android.contacts.list;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.list.JoinContactLoader;

public class JoinContactListFragment extends ContactEntryListFragment<JoinContactListAdapter> {
    private OnContactPickerActionListener mListener;
    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case -2:
                    return new CursorLoader(JoinContactListFragment.this.getActivity(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, JoinContactListFragment.this.mTargetContactId), new String[]{"display_name"}, null, null, null);
                case -1:
                case 0:
                default:
                    throw new IllegalArgumentException("No loader for ID=" + id);
                case 1:
                    JoinContactLoader loader = new JoinContactLoader(JoinContactListFragment.this.getActivity());
                    JoinContactListAdapter adapter = JoinContactListFragment.this.getAdapter();
                    if (adapter != null) {
                        adapter.configureLoader(loader, 0L);
                    }
                    return loader;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            switch (loader.getId()) {
                case -2:
                    if (data != null && data.moveToFirst()) {
                        JoinContactListFragment.this.showTargetContactName(data.getString(0));
                        break;
                    }
                    break;
                case 1:
                    if (data != null) {
                        Cursor suggestionsCursor = ((JoinContactLoader.JoinContactLoaderResult) data).suggestionCursor;
                        JoinContactListFragment.this.onContactListLoaded(suggestionsCursor, data);
                    }
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private long mTargetContactId;

    public JoinContactListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(false);
        setQuickContactEnabled(false);
    }

    public void setOnContactPickerActionListener(OnContactPickerActionListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void startLoading() {
        configureAdapter();
        getLoaderManager().initLoader(-2, null, this.mLoaderCallbacks);
        getLoaderManager().restartLoader(1, null, this.mLoaderCallbacks);
    }

    private void onContactListLoaded(Cursor suggestionsCursor, Cursor allContactsCursor) {
        JoinContactListAdapter adapter = getAdapter();
        adapter.setSuggestionsCursor(suggestionsCursor);
        setVisibleScrollbarEnabled(true);
        onPartitionLoaded(1, allContactsCursor);
    }

    private void showTargetContactName(String displayName) {
        Activity activity = getActivity();
        TextView blurbView = (TextView) activity.findViewById(R.id.join_contact_blurb);
        String name = !TextUtils.isEmpty(displayName) ? displayName : activity.getString(R.string.missing_name);
        String blurb = activity.getString(R.string.blurbJoinContactDataWith, new Object[]{name});
        blurbView.setText(blurb);
    }

    public void setTargetContactId(long targetContactId) {
        this.mTargetContactId = targetContactId;
    }

    @Override
    public JoinContactListAdapter createListAdapter() {
        JoinContactListAdapter adapter = new JoinContactListAdapter(getActivity());
        adapter.setPhotoPosition(ContactListItemView.getDefaultPhotoPosition(true));
        return adapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        JoinContactListAdapter adapter = getAdapter();
        adapter.setTargetContactId(this.mTargetContactId);
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.join_contact_picker_list_content, (ViewGroup) null);
    }

    @Override
    protected void onItemClick(int position, long id) {
        Uri contactUri = getAdapter().getContactUri(position);
        if (contactUri != null) {
            this.mListener.onPickContactAction(contactUri);
        }
    }

    @Override
    public void onPickerResult(Intent data) {
        Uri contactUri = data.getData();
        if (contactUri != null) {
            this.mListener.onPickContactAction(contactUri);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("targetContactId", this.mTargetContactId);
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);
        if (savedState != null) {
            this.mTargetContactId = savedState.getLong("targetContactId");
        }
    }

    @Override
    public void setQueryString(String queryString, boolean delaySelection) {
        super.setQueryString(queryString, delaySelection);
        setSearchMode(!TextUtils.isEmpty(queryString));
    }
}
