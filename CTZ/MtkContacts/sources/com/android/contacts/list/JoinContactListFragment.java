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
import com.android.contacts.list.JoinContactLoader;
import com.mediatek.contacts.util.ContactsListUtils;

public class JoinContactListFragment extends ContactEntryListFragment<JoinContactListAdapter> {
    private OnContactPickerActionListener mListener;
    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            if (i == -2) {
                return new CursorLoader(JoinContactListFragment.this.getActivity(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, JoinContactListFragment.this.mTargetContactId), new String[]{"display_name"}, null, null, null);
            }
            if (i == 1) {
                JoinContactLoader joinContactLoader = new JoinContactLoader(JoinContactListFragment.this.getActivity());
                JoinContactListAdapter adapter = JoinContactListFragment.this.getAdapter();
                if (adapter != null) {
                    adapter.configureLoader(joinContactLoader, 0L);
                }
                return joinContactLoader;
            }
            throw new IllegalArgumentException("No loader for ID=" + i);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            int id = loader.getId();
            if (id == -2) {
                if (cursor != null && cursor.moveToFirst()) {
                    JoinContactListFragment.this.showTargetContactName(cursor.getString(0));
                    return;
                }
                return;
            }
            if (id == 1 && cursor != null) {
                JoinContactListFragment.this.onContactListLoaded(((JoinContactLoader.JoinContactLoaderResult) cursor).suggestionCursor, cursor);
                JoinContactListFragment.this.maybeLogListEvent();
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
        setListType(15);
        setLogListEvents(true);
    }

    public void setOnContactPickerActionListener(OnContactPickerActionListener onContactPickerActionListener) {
        this.mListener = onContactPickerActionListener;
    }

    @Override
    protected void startLoading() {
        configureAdapter();
        getLoaderManager().initLoader(-2, null, this.mLoaderCallbacks);
        getLoaderManager().restartLoader(1, null, this.mLoaderCallbacks);
    }

    private void onContactListLoaded(Cursor cursor, Cursor cursor2) {
        getAdapter().setSuggestionsCursor(cursor);
        setVisibleScrollbarEnabled(true);
        onPartitionLoaded(1, cursor2);
    }

    private void showTargetContactName(String str) {
        Activity activity = getActivity();
        TextView textView = (TextView) activity.findViewById(R.id.join_contact_blurb);
        if (TextUtils.isEmpty(str)) {
            activity.getString(R.string.missing_name);
        }
        textView.setText(ContactsListUtils.getBlurb(getActivity(), str));
    }

    public void setTargetContactId(long j) {
        this.mTargetContactId = j;
    }

    @Override
    public JoinContactListAdapter createListAdapter() {
        JoinContactListAdapter joinContactListAdapter = new JoinContactListAdapter(getActivity());
        joinContactListAdapter.setPhotoPosition(ContactListItemView.getDefaultPhotoPosition(false));
        return joinContactListAdapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        getAdapter().setTargetContactId(this.mTargetContactId);
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.join_contact_picker_list_content, (ViewGroup) null);
    }

    @Override
    protected void onItemClick(int i, long j) {
        Uri contactUri = getAdapter().getContactUri(i);
        if (contactUri != null) {
            this.mListener.onPickContactAction(contactUri);
        }
    }

    @Override
    public void onPickerResult(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            this.mListener.onPickContactAction(data);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putLong("targetContactId", this.mTargetContactId);
    }

    @Override
    public void restoreSavedState(Bundle bundle) {
        super.restoreSavedState(bundle);
        if (bundle != null) {
            this.mTargetContactId = bundle.getLong("targetContactId");
        }
    }

    @Override
    public void setQueryString(String str, boolean z) {
        super.setQueryString(str, z);
        setSearchMode(!TextUtils.isEmpty(str));
    }
}
